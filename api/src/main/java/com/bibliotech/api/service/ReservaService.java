package com.bibliotech.api.service;

import com.bibliotech.api.model.Livro;
import com.bibliotech.api.model.Reserva;
import com.bibliotech.api.model.Usuario;
import com.bibliotech.api.repository.LivroRepository;
import com.bibliotech.api.repository.ReservaRepository;
import com.bibliotech.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;

    public ReservaService(ReservaRepository reservaRepository, LivroRepository livroRepository, UsuarioRepository usuarioRepository) {
        this.reservaRepository = reservaRepository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Reserva entrarNaFila(Long usuarioId, Long livroId) {
        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Livro não encontrado."));
                
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Usuário não encontrado."));

        // Regra de Negócio: Se o livro tem estoque, o sistema bloqueia a reserva e manda o usuário fazer o empréstimo normal
        if (livro.getQuantidadeDisponivel() > 0) {
            throw new RuntimeException("Operação negada: O livro está disponível nas prateleiras. Faça o empréstimo diretamente.");
        }

        // Regra de Negócio: O usuário não pode entrar duas vezes na mesma fila
        if (reservaRepository.existsByUsuarioIdAndLivroIdAndStatus(usuarioId, livroId, com.bibliotech.api.model.ReservaStatus.AGUARDANDO)) {
            throw new RuntimeException("Operação negada: Você já está na fila de espera para este livro.");
        }

        // Cria a reserva
        Reserva novaReserva = new Reserva();
        novaReserva.setUsuario(usuario);
        novaReserva.setLivro(livro);
        novaReserva.setDataSolicitacao(LocalDateTime.now());
        novaReserva.setStatus(com.bibliotech.api.model.ReservaStatus.AGUARDANDO);

        return reservaRepository.save(novaReserva);
    }

    public List<Reserva> consultarFilaDoLivro(Long livroId) {
        return reservaRepository.findByLivroIdAndStatusOrderByDataSolicitacaoAsc(livroId, com.bibliotech.api.model.ReservaStatus.AGUARDANDO);
    }

    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    public List<Reserva> listarPorUsuario(Long usuarioId) {
        List<Reserva> reservas = reservaRepository.findByUsuarioIdOrderByDataSolicitacaoDesc(usuarioId);
        for (Reserva r : reservas) {
            if (com.bibliotech.api.model.ReservaStatus.AGUARDANDO.equals(r.getStatus())) {
                int count = reservaRepository.countByLivroIdAndStatusAndDataSolicitacaoBefore(
                    r.getLivro().getId(), 
                    com.bibliotech.api.model.ReservaStatus.AGUARDANDO, 
                    r.getDataSolicitacao()
                );
                r.setPosicaoFila(count + 1);
            }
        }
        return reservas;
    }
}



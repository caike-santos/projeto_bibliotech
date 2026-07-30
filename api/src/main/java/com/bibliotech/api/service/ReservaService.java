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
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Livro nÃ£o encontrado."));
                
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("UsuÃ¡rio nÃ£o encontrado."));

        // Regra de NegÃ³cio: Se o livro tem estoque, o sistema bloqueia a reserva e manda o usuÃ¡rio fazer o emprÃ©stimo normal
        if (livro.getQuantidadeDisponivel() > 0) {
            throw new RuntimeException("OperaÃ§Ã£o negada: O livro estÃ¡ disponÃ­vel nas prateleiras. FaÃ§a o emprÃ©stimo diretamente.");
        }

        // Regra de NegÃ³cio: O usuÃ¡rio nÃ£o pode entrar duas vezes na mesma fila
        if (reservaRepository.existsByUsuarioIdAndLivroIdAndStatus(usuarioId, livroId, com.bibliotech.api.model.ReservaStatus.AGUARDANDO)) {
            throw new RuntimeException("OperaÃ§Ã£o negada: VocÃª jÃ¡ estÃ¡ na fila de espera para este livro.");
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



package com.bibliotech.api.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bibliotech.api.model.Emprestimo;
import com.bibliotech.api.model.Livro;
import com.bibliotech.api.model.Usuario;
import com.bibliotech.api.model.Reserva;
import com.bibliotech.api.model.Notificacao;
import com.bibliotech.api.repository.EmprestimoRepository;
import com.bibliotech.api.repository.LivroRepository;
import com.bibliotech.api.repository.UsuarioRepository;
import com.bibliotech.api.repository.ReservaRepository;
import com.bibliotech.api.repository.NotificacaoRepository;

@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LivroRepository livroRepository;
    private final ReservaRepository reservaRepository;
    private final NotificacaoRepository notificacaoRepository;

    public EmprestimoService(EmprestimoRepository emprestimoRepository, UsuarioRepository usuarioRepository, 
                             LivroRepository livroRepository, ReservaRepository reservaRepository, 
                             NotificacaoRepository notificacaoRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.usuarioRepository = usuarioRepository;
        this.livroRepository = livroRepository;
        this.reservaRepository = reservaRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    @Transactional
    public Emprestimo realizarEmprestimo(Emprestimo novoEmprestimo) {
        Long usuarioId = novoEmprestimo.getUsuario().getId();

        int quantidadeAtivos = emprestimoRepository.countByUsuarioIdAndStatus(usuarioId, "ATIVO");
        if (quantidadeAtivos >= 3) {
            throw new RuntimeException("Limite excedido: O leitor já possui 3 livros emprestados.");
        }

        if (emprestimoRepository.existsByUsuarioIdAndStatus(usuarioId, "ATRASADO")) {
            throw new RuntimeException("Bloqueado: O leitor possui livros em atraso e precisa regularizar sua situação.");
        }

        Livro livro = livroRepository.findById(novoEmprestimo.getLivro().getId())
                .orElseThrow(() -> new RuntimeException("Livro não encontrado no sistema."));

        if (emprestimoRepository.existsByUsuarioIdAndLivroIdAndStatusIn(usuarioId, livro.getId(), java.util.Arrays.asList("ATIVO", "ATRASADO"))) {
            throw new RuntimeException("Operação negada: Você já possui um exemplar deste livro em seus empréstimos ativos.");
        }

        // --- NOVA REGRA: VERIFICA SE O USUÁRIO É O DONO DA RESERVA ---
        java.util.Optional<Reserva> reservaNotificada = reservaRepository.findFirstByUsuarioIdAndLivroIdAndStatus(usuarioId, livro.getId(), "NOTIFICADO");
        java.util.Optional<Reserva> reservaAguardando = reservaRepository.findFirstByUsuarioIdAndLivroIdAndStatus(usuarioId, livro.getId(), "AGUARDANDO");

        if (reservaNotificada.isPresent()) {
            // É a pessoa da fila! Libera o empréstimo e conclui a reserva.
            Reserva reserva = reservaNotificada.get();
            reserva.setStatus("CONCLUIDA");
            reservaRepository.save(reserva);
            
            // Marca a notificação como lida automaticamente
            List<Notificacao> notifs = notificacaoRepository.findByUsuarioIdOrderByDataEnvioDesc(usuarioId);
            for (Notificacao n : notifs) {
                if (!n.isLida() && n.getMensagem().contains(livro.getTitulo())) {
                    n.setLida(true);
                    notificacaoRepository.save(n);
                }
            }
            // Atenção: Não diminuímos o estoque aqui porque ele já foi "congelado" na devolução!
        } else {
            // É um usuário comum tentando pegar o livro
            if (livro.getQuantidadeDisponivel() <= 0) {
                throw new RuntimeException("Operação negada: O livro '" + livro.getTitulo() + "' está sem estoque no momento.");
            }
            // Se ele estava só aguardando, mas pegou o livro (alguém devolveu outra cópia, por ex), resolvemos a fila.
            if (reservaAguardando.isPresent()) {
                Reserva r = reservaAguardando.get();
                r.setStatus("CONCLUIDA");
                reservaRepository.save(r);
            }
            // Fluxo normal: diminui 1 do estoque e salva
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() - 1);
            livroRepository.save(livro);
        }

        novoEmprestimo.setDataRetirada(LocalDate.now());
        novoEmprestimo.setDataDevolucaoPrevista(LocalDate.now().plusDays(14));
        novoEmprestimo.setStatus("ATIVO");
        novoEmprestimo.setRenovacoesFeitas(0);
        novoEmprestimo.setLivro(livro); 

        return emprestimoRepository.save(novoEmprestimo);
    }

    public Emprestimo renovarEmprestimo(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        if (emprestimoRepository.existsByUsuarioIdAndStatus(emprestimo.getUsuario().getId(), "ATRASADO")) {
            throw new RuntimeException("Bloqueado: O leitor não pode fazer renovações pois possui livros em atraso.");
        }

        if (emprestimo.getRenovacoesFeitas() >= 1) {
            throw new RuntimeException("Limite de renovação: Este livro já foi renovado o limite máximo de vezes permitidas.");
        }
        
        // RN07 - Bloqueio de renovação se houver fila de espera
        boolean temFilaDeEspera = reservaRepository.existsByLivroIdAndStatus(emprestimo.getLivro().getId(), "AGUARDANDO");
        if (temFilaDeEspera) {
            throw new RuntimeException("Renovação negada: Há leitores na fila de espera aguardando este livro.");
        }

        emprestimo.setRenovacoesFeitas(emprestimo.getRenovacoesFeitas() + 1);
        emprestimo.setDataDevolucaoPrevista(emprestimo.getDataDevolucaoPrevista().plusDays(14));

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo devolverLivro(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado com o ID: " + emprestimoId));

        if ("DEVOLVIDO".equals(emprestimo.getStatus())) {
            throw new RuntimeException("Este livro já consta como devolvido no sistema.");
        }

        emprestimo.setDataDevolucaoReal(LocalDate.now());
        emprestimo.setStatus("DEVOLVIDO");

        Livro livro = emprestimo.getLivro();
        livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() + 1);
        
        // --- NOVA REGRA: AVISAR A FILA DE ESPERA ---
        List<Reserva> fila = reservaRepository.findByLivroIdAndStatusOrderByDataSolicitacaoAsc(livro.getId(), "AGUARDANDO");
        
        if (!fila.isEmpty()) {
            // Pega quem pediu primeiro
            Reserva proximoDaFila = fila.get(0);
            proximoDaFila.setStatus("NOTIFICADO");
            reservaRepository.save(proximoDaFila);

            // Gera o alerta no sistema
            Notificacao aviso = new Notificacao();
            aviso.setUsuario(proximoDaFila.getUsuario());
            aviso.setMensagem("Boas notícias! O livro '" + livro.getTitulo() + "' que você reservou acabou de ser devolvido. Ele está reservado para você no balcão por 48 horas.");
            aviso.setDataEnvio(java.time.LocalDateTime.now());
            aviso.setLida(false);
            notificacaoRepository.save(aviso);
            
            // "Congela" o livro para o próximo da fila, impedindo que outro leitor pegue antes
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() - 1);
        }

        livroRepository.save(livro);

        if (!emprestimo.getDataDevolucaoReal().isAfter(emprestimo.getDataDevolucaoPrevista())) {
            Usuario usuario = emprestimo.getUsuario();
            int pontosAtuais = usuario.getPontosGamificacao();
            usuario.setPontosGamificacao(pontosAtuais + 10);
            usuarioRepository.save(usuario);
            emprestimo.setValorMulta(0.0); 
        } else {
            long diasAtraso = ChronoUnit.DAYS.between(emprestimo.getDataDevolucaoPrevista(), emprestimo.getDataDevolucaoReal());
            double valorDaMulta = diasAtraso * 2.00;
            emprestimo.setValorMulta(valorDaMulta);
            System.out.println("Alerta de Atraso: " + diasAtraso + " dias. Multa gerada: R$ " + valorDaMulta);
        }

        return emprestimoRepository.save(emprestimo);
    }
}
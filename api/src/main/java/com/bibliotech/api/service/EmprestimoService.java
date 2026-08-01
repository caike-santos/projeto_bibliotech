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

    public EmprestimoService(EmprestimoRepository emprestimoRepository, UsuarioRepository usuarioRepository, LivroRepository livroRepository, ReservaRepository reservaRepository, 
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

        int quantidadeAtivos = emprestimoRepository.countByUsuarioIdAndStatus(usuarioId, com.bibliotech.api.model.EmprestimoStatus.ATIVO);
        if (quantidadeAtivos >= 3) {
            throw new RuntimeException("Limite excedido: O leitor já possui 3 livros emprestados.");
        }

        if (emprestimoRepository.existsByUsuarioIdAndStatus(usuarioId, com.bibliotech.api.model.EmprestimoStatus.ATRASADO)) {
            throw new RuntimeException("Bloqueado: O leitor possui livros em atraso e precisa regularizar sua situação.");
        }

        Livro livro = livroRepository.findById(novoEmprestimo.getLivro().getId())
                .orElseThrow(() -> new RuntimeException("Livro não encontrado no sistema."));

        if (emprestimoRepository.existsByUsuarioIdAndLivroIdAndStatusIn(usuarioId, livro.getId(), java.util.Arrays.asList(com.bibliotech.api.model.EmprestimoStatus.ATIVO, com.bibliotech.api.model.EmprestimoStatus.ATRASADO, com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA))) {
            throw new RuntimeException("Operação negada: Você já possui (ou solicitou) um exemplar deste livro.");
        }

        //VERIFICA SE O USUÁRIO É O DONO DA RESERVA ---
        //Se a reserva estiver NOTIFICADO, ele é o primeiro da fila. 
        java.util.Optional<Reserva> reservaNotificada = reservaRepository.findFirstByUsuarioIdAndLivroIdAndStatus(usuarioId, livro.getId(), com.bibliotech.api.model.ReservaStatus.NOTIFICADO);
        //Se a reserva estiver AGUARDANDO, ele está na fila, mas não é o primeiro.
        java.util.Optional<Reserva> reservaAguardando = reservaRepository.findFirstByUsuarioIdAndLivroIdAndStatus(usuarioId, livro.getId(), com.bibliotech.api.model.ReservaStatus.AGUARDANDO);

        if (reservaNotificada.isPresent()) {
            // É a primeira pessoa da fila! Libera o empréstimo e conclui a reserva.
            Reserva reserva = reservaNotificada.get();
            reserva.setStatus(com.bibliotech.api.model.ReservaStatus.CONCLUIDA);
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
                r.setStatus(com.bibliotech.api.model.ReservaStatus.CONCLUIDA);
                reservaRepository.save(r);
            }
            // Fluxo normal: diminui 1 do estoque e salva
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() - 1);
            livroRepository.save(livro);
        }

        novoEmprestimo.setDataRetirada(LocalDate.now()); // Data do pedido
        novoEmprestimo.setDataDevolucaoPrevista(LocalDate.now().plusDays(2)); // Prazo de 48h para buscar
        novoEmprestimo.setStatus(com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA);
        novoEmprestimo.setRenovacoesFeitas(0);
        novoEmprestimo.setLivro(livro); 

        return emprestimoRepository.save(novoEmprestimo);
    }

    public Emprestimo renovarEmprestimo(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        if (emprestimoRepository.existsByUsuarioIdAndStatus(emprestimo.getUsuario().getId(), com.bibliotech.api.model.EmprestimoStatus.ATRASADO)) {
            throw new RuntimeException("Bloqueado: O leitor não pode fazer renovações pois possui livros em atraso.");
        }

        if (emprestimo.getRenovacoesFeitas() >= 1) {
            throw new RuntimeException("Limite de renovação: Este livro já foi renovado o limite máximo de vezes permitidas.");
        }
        
        // RN07 - Bloqueio de renovação se houver fila de espera
        boolean temFilaDeEspera = reservaRepository.existsByLivroIdAndStatus(emprestimo.getLivro().getId(), com.bibliotech.api.model.ReservaStatus.AGUARDANDO);
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

        if (com.bibliotech.api.model.EmprestimoStatus.DEVOLVIDO.equals(emprestimo.getStatus())) {
            throw new RuntimeException("Este livro já consta como devolvido no sistema.");
        }

        boolean oLivroFoiRetirado = com.bibliotech.api.model.EmprestimoStatus.ATIVO.equals(emprestimo.getStatus()) || com.bibliotech.api.model.EmprestimoStatus.ATRASADO.equals(emprestimo.getStatus());

        emprestimo.setDataDevolucaoReal(LocalDate.now());
        emprestimo.setStatus(com.bibliotech.api.model.EmprestimoStatus.DEVOLVIDO);

        Livro livro = emprestimo.getLivro();
        livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() + 1);
        
        // --- AVISAR A FILA DE ESPERA ---
        List<Reserva> fila = reservaRepository.findByLivroIdAndStatusOrderByDataSolicitacaoAsc(livro.getId(), com.bibliotech.api.model.ReservaStatus.AGUARDANDO);
        
        if (!fila.isEmpty()) {
            // Pega quem pediu primeiro
            Reserva proximoDaFila = fila.get(0);
            proximoDaFila.setStatus(com.bibliotech.api.model.ReservaStatus.NOTIFICADO);
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

        if (oLivroFoiRetirado) {
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
        } else {
            emprestimo.setValorMulta(0.0);
        }

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo confirmarRetirada(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        if (!com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA.equals(emprestimo.getStatus())) {
            throw new RuntimeException("Este empréstimo não está aguardando retirada.");
        }

        emprestimo.setStatus(com.bibliotech.api.model.EmprestimoStatus.ATIVO);
        emprestimo.setDataRetirada(LocalDate.now());
        emprestimo.setDataDevolucaoPrevista(LocalDate.now().plusDays(14));
        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo cancelarEmprestimo(Long emprestimoId, Long usuarioLogadoId, boolean isAdmin) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        if (!isAdmin && !emprestimo.getUsuario().getId().equals(usuarioLogadoId)) {
            throw new RuntimeException("Acesso negado: você só pode cancelar os seus próprios pedidos.");
        }

        if (!com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA.equals(emprestimo.getStatus())) {
            throw new RuntimeException("Apenas empréstimos aguardando retirada podem ser cancelados.");
        }

        emprestimo.setStatus(com.bibliotech.api.model.EmprestimoStatus.CANCELADO);
        
        // Devolve livro pro acervo
        Livro livro = emprestimo.getLivro();
        livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() + 1);
        livroRepository.save(livro);

        return emprestimoRepository.save(emprestimo);
    }
}







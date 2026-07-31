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

        int quantidadeAtivos = emprestimoRepository.countByUsuarioIdAndStatus(usuarioId, com.bibliotech.api.model.EmprestimoStatus.ATIVO);
        if (quantidadeAtivos >= 3) {
            throw new RuntimeException("Limite excedido: O leitor jÃ¡ possui 3 livros emprestados.");
        }

        if (emprestimoRepository.existsByUsuarioIdAndStatus(usuarioId, com.bibliotech.api.model.EmprestimoStatus.ATRASADO)) {
            throw new RuntimeException("Bloqueado: O leitor possui livros em atraso e precisa regularizar sua situaÃ§Ã£o.");
        }

        Livro livro = livroRepository.findById(novoEmprestimo.getLivro().getId())
                .orElseThrow(() -> new RuntimeException("Livro nÃ£o encontrado no sistema."));

        if (emprestimoRepository.existsByUsuarioIdAndLivroIdAndStatusIn(usuarioId, livro.getId(), java.util.Arrays.asList(com.bibliotech.api.model.EmprestimoStatus.ATIVO, com.bibliotech.api.model.EmprestimoStatus.ATRASADO, com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA))) {
            throw new RuntimeException("OperaÃ§Ã£o negada: VocÃª jÃ¡ possui (ou solicitou) um exemplar deste livro.");
        }

        // --- NOVA REGRA: VERIFICA SE O USUÃRIO Ã‰ O DONO DA RESERVA ---
        java.util.Optional<Reserva> reservaNotificada = reservaRepository.findFirstByUsuarioIdAndLivroIdAndStatus(usuarioId, livro.getId(), com.bibliotech.api.model.ReservaStatus.NOTIFICADO);
        java.util.Optional<Reserva> reservaAguardando = reservaRepository.findFirstByUsuarioIdAndLivroIdAndStatus(usuarioId, livro.getId(), com.bibliotech.api.model.ReservaStatus.AGUARDANDO);

        if (reservaNotificada.isPresent()) {
            // Ã‰ a pessoa da fila! Libera o emprÃ©stimo e conclui a reserva.
            Reserva reserva = reservaNotificada.get();
            reserva.setStatus(com.bibliotech.api.model.ReservaStatus.CONCLUIDA);
            reservaRepository.save(reserva);
            
            // Marca a notificaÃ§Ã£o como lida automaticamente
            List<Notificacao> notifs = notificacaoRepository.findByUsuarioIdOrderByDataEnvioDesc(usuarioId);
            for (Notificacao n : notifs) {
                if (!n.isLida() && n.getMensagem().contains(livro.getTitulo())) {
                    n.setLida(true);
                    notificacaoRepository.save(n);
                }
            }
            // AtenÃ§Ã£o: NÃ£o diminuÃ­mos o estoque aqui porque ele jÃ¡ foi "congelado" na devoluÃ§Ã£o!
        } else {
            // Ã‰ um usuÃ¡rio comum tentando pegar o livro
            if (livro.getQuantidadeDisponivel() <= 0) {
                throw new RuntimeException("OperaÃ§Ã£o negada: O livro '" + livro.getTitulo() + "' estÃ¡ sem estoque no momento.");
            }
            // Se ele estava sÃ³ aguardando, mas pegou o livro (alguÃ©m devolveu outra cÃ³pia, por ex), resolvemos a fila.
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
                .orElseThrow(() -> new RuntimeException("EmprÃ©stimo nÃ£o encontrado."));

        if (emprestimoRepository.existsByUsuarioIdAndStatus(emprestimo.getUsuario().getId(), com.bibliotech.api.model.EmprestimoStatus.ATRASADO)) {
            throw new RuntimeException("Bloqueado: O leitor nÃ£o pode fazer renovaÃ§Ãµes pois possui livros em atraso.");
        }

        if (emprestimo.getRenovacoesFeitas() >= 1) {
            throw new RuntimeException("Limite de renovaÃ§Ã£o: Este livro jÃ¡ foi renovado o limite mÃ¡ximo de vezes permitidas.");
        }
        
        // RN07 - Bloqueio de renovaÃ§Ã£o se houver fila de espera
        boolean temFilaDeEspera = reservaRepository.existsByLivroIdAndStatus(emprestimo.getLivro().getId(), com.bibliotech.api.model.ReservaStatus.AGUARDANDO);
        if (temFilaDeEspera) {
            throw new RuntimeException("RenovaÃ§Ã£o negada: HÃ¡ leitores na fila de espera aguardando este livro.");
        }

        emprestimo.setRenovacoesFeitas(emprestimo.getRenovacoesFeitas() + 1);
        emprestimo.setDataDevolucaoPrevista(emprestimo.getDataDevolucaoPrevista().plusDays(14));

        return emprestimoRepository.save(emprestimo);
    }

    @Transactional
    public Emprestimo devolverLivro(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new RuntimeException("EmprÃ©stimo nÃ£o encontrado com o ID: " + emprestimoId));

        if (com.bibliotech.api.model.EmprestimoStatus.DEVOLVIDO.equals(emprestimo.getStatus())) {
            throw new RuntimeException("Este livro jÃ¡ consta como devolvido no sistema.");
        }

        boolean oLivroFoiRetirado = com.bibliotech.api.model.EmprestimoStatus.ATIVO.equals(emprestimo.getStatus()) || 
                                    com.bibliotech.api.model.EmprestimoStatus.ATRASADO.equals(emprestimo.getStatus());

        emprestimo.setDataDevolucaoReal(LocalDate.now());
        emprestimo.setStatus(com.bibliotech.api.model.EmprestimoStatus.DEVOLVIDO);

        Livro livro = emprestimo.getLivro();
        livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() + 1);
        
        // --- NOVA REGRA: AVISAR A FILA DE ESPERA ---
        List<Reserva> fila = reservaRepository.findByLivroIdAndStatusOrderByDataSolicitacaoAsc(livro.getId(), com.bibliotech.api.model.ReservaStatus.AGUARDANDO);
        
        if (!fila.isEmpty()) {
            // Pega quem pediu primeiro
            Reserva proximoDaFila = fila.get(0);
            proximoDaFila.setStatus(com.bibliotech.api.model.ReservaStatus.NOTIFICADO);
            reservaRepository.save(proximoDaFila);

            // Gera o alerta no sistema
            Notificacao aviso = new Notificacao();
            aviso.setUsuario(proximoDaFila.getUsuario());
            aviso.setMensagem("Boas notÃ­cias! O livro '" + livro.getTitulo() + "' que vocÃª reservou acabou de ser devolvido. Ele estÃ¡ reservado para vocÃª no balcÃ£o por 48 horas.");
            aviso.setDataEnvio(java.time.LocalDateTime.now());
            aviso.setLida(false);
            notificacaoRepository.save(aviso);
            
            // "Congela" o livro para o prÃ³ximo da fila, impedindo que outro leitor pegue antes
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
                .orElseThrow(() -> new RuntimeException("EmprÃ©stimo nÃ£o encontrado."));

        if (!com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA.equals(emprestimo.getStatus())) {
            throw new RuntimeException("Este emprÃ©stimo nÃ£o estÃ¡ aguardando retirada.");
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







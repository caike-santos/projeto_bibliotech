package com.bibliotech.api.task;

import com.bibliotech.api.model.Emprestimo;
import com.bibliotech.api.model.Notificacao;
import com.bibliotech.api.repository.EmprestimoRepository;
import com.bibliotech.api.repository.NotificacaoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class NotificacaoTask {

    private final EmprestimoRepository emprestimoRepository;
    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoTask(EmprestimoRepository emprestimoRepository, NotificacaoRepository notificacaoRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    // Roda todos os dias à meia-noite
    @Scheduled(cron = "0 0 0 * * ?")
    public void gerarAlertasVencimentoProximo() {
        // Data alvo: Daqui a exatos 2 dias
        LocalDate dataAlvo = LocalDate.now().plusDays(2);
        
        System.out.println("Iniciando rotina de cobrança: Buscando empréstimos que vencem em " + dataAlvo);

        // Busca empréstimos que estão ATIVOS e com a data de devolução prevista para a data alvo
        List<Emprestimo> emprestimosPertoDoVencimento = emprestimoRepository.findByDataDevolucaoPrevistaAndStatus(dataAlvo, com.bibliotech.api.model.EmprestimoStatus.ATIVO);

        for (Emprestimo emprestimo : emprestimosPertoDoVencimento) {
            String tituloLivro = emprestimo.getLivro().getTitulo();
            
            Notificacao aviso = new Notificacao();
            aviso.setUsuario(emprestimo.getUsuario());
            aviso.setMensagem("Aviso: Seu empréstimo do livro '" + tituloLivro + "' vence em 2 dias! Não esqueça de devolver no balcão.");
            aviso.setDataEnvio(LocalDateTime.now());
            aviso.setLida(false);
            
            notificacaoRepository.save(aviso);
            System.out.println("Alerta gerado para o usuário " + emprestimo.getUsuario().getEmail() + " referente ao livro " + tituloLivro);
        }
        
        System.out.println("Rotina de cobrança finalizada. Foram gerados " + emprestimosPertoDoVencimento.size() + " alertas.");
    }
}


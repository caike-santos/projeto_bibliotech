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

    // Roda todos os dias Ã  meia-noite
    @Scheduled(cron = "0 0 0 * * ?")
    public void gerarAlertasVencimentoProximo() {
        // Data alvo: Daqui a exatos 2 dias
        LocalDate dataAlvo = LocalDate.now().plusDays(2);
        
        System.out.println("Iniciando rotina de cobranÃ§a: Buscando emprÃ©stimos que vencem em " + dataAlvo);

        // Busca emprÃ©stimos que estÃ£o ATIVOS e com a data de devoluÃ§Ã£o prevista para a data alvo
        List<Emprestimo> emprestimosPertoDoVencimento = emprestimoRepository.findByDataDevolucaoPrevistaAndStatus(dataAlvo, com.bibliotech.api.model.EmprestimoStatus.ATIVO);

        for (Emprestimo emprestimo : emprestimosPertoDoVencimento) {
            String tituloLivro = emprestimo.getLivro().getTitulo();
            
            Notificacao aviso = new Notificacao();
            aviso.setUsuario(emprestimo.getUsuario());
            aviso.setMensagem("Aviso: Seu emprÃ©stimo do livro '" + tituloLivro + "' vence em 2 dias! NÃ£o esqueÃ§a de devolver no balcÃ£o.");
            aviso.setDataEnvio(LocalDateTime.now());
            aviso.setLida(false);
            
            notificacaoRepository.save(aviso);
            System.out.println("Alerta gerado para o usuÃ¡rio " + emprestimo.getUsuario().getEmail() + " referente ao livro " + tituloLivro);
        }
        
        System.out.println("Rotina de cobranÃ§a finalizada. Foram gerados " + emprestimosPertoDoVencimento.size() + " alertas.");
    }
}


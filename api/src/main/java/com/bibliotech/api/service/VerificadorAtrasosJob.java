package com.bibliotech.api.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bibliotech.api.model.Emprestimo;
import com.bibliotech.api.model.Notificacao;
import com.bibliotech.api.repository.EmprestimoRepository;
import com.bibliotech.api.repository.NotificacaoRepository;

@Component
public class VerificadorAtrasosJob {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    // Roda todos os dias à meia-noite (0 0 0 * * *)
    @Scheduled(cron = "0 0 0 * * *")
    public void verificarAtrasos() {
        System.out.println("Iniciando verificação diária de atrasos...");
        
        LocalDate hoje = LocalDate.now();
        List<Emprestimo> emprestimosVencidos = emprestimoRepository.findByStatusAndDataDevolucaoPrevistaBefore("ATIVO", hoje);

        int count = 0;
        for (Emprestimo e : emprestimosVencidos) {
            e.setStatus("ATRASADO");
            emprestimoRepository.save(e);
            
            Notificacao notificacao = new Notificacao();
            notificacao.setUsuario(e.getUsuario());
            notificacao.setTitulo("Atraso Detectado!");
            notificacao.setMensagem("O livro '" + e.getLivro().getTitulo() + "' passou do prazo de devolução. Sua conta está bloqueada para novos empréstimos até a regularização.");
            notificacao.setLida(false);
            notificacaoRepository.save(notificacao);
            
            count++;
        }
        
        System.out.println("Verificação concluída. " + count + " empréstimos atualizados para ATRASADO.");
    }
}

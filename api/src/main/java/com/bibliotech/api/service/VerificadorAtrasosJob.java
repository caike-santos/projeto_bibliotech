package com.bibliotech.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bibliotech.api.model.Emprestimo;
import com.bibliotech.api.model.Livro;
import com.bibliotech.api.model.Notificacao;
import com.bibliotech.api.repository.EmprestimoRepository;
import com.bibliotech.api.repository.LivroRepository;
import com.bibliotech.api.repository.NotificacaoRepository;

@Component
public class VerificadorAtrasosJob {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private LivroRepository livroRepository;

    // Roda 10 segundos apÃ³s o servidor iniciar e depois a cada 1 hora (3600000 ms)
    @Scheduled(initialDelay = 10000, fixedRate = 3600000)
    public void verificarAtrasos() {
        System.out.println("Iniciando verificaÃ§Ã£o diÃ¡ria de atrasos...");
        
        LocalDate hoje = LocalDate.now();
        List<Emprestimo> emprestimosVencidos = emprestimoRepository.findByStatusAndDataDevolucaoPrevistaBefore(com.bibliotech.api.model.EmprestimoStatus.ATIVO, hoje);

        int count = 0;
        List<Notificacao> notificacoesAtraso = new ArrayList<>();
        for (Emprestimo e : emprestimosVencidos) {
            e.setStatus(com.bibliotech.api.model.EmprestimoStatus.ATRASADO);
            
            Notificacao notificacao = new Notificacao();
            notificacao.setUsuario(e.getUsuario());
            notificacao.setMensagem("O livro '" + e.getLivro().getTitulo() + "' passou do prazo de devoluÃ§Ã£o. Sua conta estÃ¡ bloqueada para novos emprÃ©stimos atÃ© a regularizaÃ§Ã£o.");
            notificacao.setLida(false);
            notificacao.setDataEnvio(java.time.LocalDateTime.now());
            notificacoesAtraso.add(notificacao);
            
            count++;
        }
        
        if (!emprestimosVencidos.isEmpty()) {
            emprestimoRepository.saveAll(emprestimosVencidos);
            notificacaoRepository.saveAll(notificacoesAtraso);
        }
        
        System.out.println("VerificaÃ§Ã£o concluÃ­da. " + count + " emprÃ©stimos atualizados para ATRASADO.");
        
        System.out.println("Iniciando verificaÃ§Ã£o de pendÃªncias de retirada expiradas...");
        List<Emprestimo> retiradasExpiradas = emprestimoRepository.findByStatusAndDataDevolucaoPrevistaBefore(com.bibliotech.api.model.EmprestimoStatus.AGUARDANDO_RETIRADA, hoje);
        int countCancelados = 0;
        List<Notificacao> notificacoesCancelamento = new ArrayList<>();
        List<Livro> livrosAtualizados = new ArrayList<>();
        
        for (Emprestimo e : retiradasExpiradas) {
            e.setStatus(com.bibliotech.api.model.EmprestimoStatus.CANCELADO);
            
            Livro livro = e.getLivro();
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() + 1);
            livrosAtualizados.add(livro);
            
            Notificacao notificacao = new Notificacao();
            notificacao.setUsuario(e.getUsuario());
            notificacao.setMensagem("Sua solicitaÃ§Ã£o do livro '" + livro.getTitulo() + "' foi cancelada, pois vocÃª nÃ£o o retirou no prazo de 48 horas.");
            notificacao.setLida(false);
            notificacao.setDataEnvio(java.time.LocalDateTime.now());
            notificacoesCancelamento.add(notificacao);
            
            countCancelados++;
        }
        
        if (!retiradasExpiradas.isEmpty()) {
            emprestimoRepository.saveAll(retiradasExpiradas);
            livroRepository.saveAll(livrosAtualizados);
            notificacaoRepository.saveAll(notificacoesCancelamento);
        }
        
        System.out.println("VerificaÃ§Ã£o concluÃ­da. " + countCancelados + " solicitaÃ§Ãµes CANCELADAS.");
    }
}



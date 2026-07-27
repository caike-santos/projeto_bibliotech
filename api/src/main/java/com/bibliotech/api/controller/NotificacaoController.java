package com.bibliotech.api.controller;

import com.bibliotech.api.model.Notificacao;
import com.bibliotech.api.repository.NotificacaoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoRepository repository;

    public NotificacaoController(NotificacaoRepository repository) {
        this.repository = repository;
    }

    // Busca todas as notificações de um leitor
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Notificacao>> buscarPorUsuario(@PathVariable Long usuarioId) {
        List<Notificacao> notificacoes = repository.findByUsuarioIdOrderByDataEnvioDesc(usuarioId);
        return ResponseEntity.ok(notificacoes);
    }

    // O frontend chama essa rota quando o leitor clica na mensagem para marcá-la como lida
    @PutMapping("/{id}/ler")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id) {
        Notificacao notificacao = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Notificação não encontrada."));
        
        notificacao.setLida(true);
        repository.save(notificacao);
        
        return ResponseEntity.noContent().build(); // Retorna código 204 (Sucesso, sem conteúdo na resposta)
    }
}
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
        verificarPermissaoUsuario(usuarioId);
        List<Notificacao> notificacoes = repository.findByUsuarioIdOrderByDataEnvioDesc(usuarioId);
        return ResponseEntity.ok(notificacoes);
    }

    // O frontend chama essa rota quando o leitor clica na mensagem para marcá-la como lida
    @PutMapping("/{id}/ler")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id) {
        Notificacao notificacao = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Notificação não encontrada."));
        
        verificarPermissaoUsuario(notificacao.getUsuario().getId());
        
        notificacao.setLida(true);
        repository.save(notificacao);
        
        return ResponseEntity.noContent().build(); // Retorna código 204 (Sucesso, sem conteúdo na resposta)
    }

    private void verificarPermissaoUsuario(Long usuarioIdAlvo) {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof com.bibliotech.api.model.Usuario)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        com.bibliotech.api.model.Usuario usuarioLogado = (com.bibliotech.api.model.Usuario) principal;
        if (usuarioLogado.getTipo().equalsIgnoreCase("LEITOR") && !usuarioLogado.getId().equals(usuarioIdAlvo)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Acesso negado: Você não tem permissão para acessar os dados deste usuário.");
        }
    }
}
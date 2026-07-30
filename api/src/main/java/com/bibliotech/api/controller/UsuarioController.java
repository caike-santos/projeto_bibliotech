package com.bibliotech.api.controller;

import com.bibliotech.api.model.Notificacao;
import com.bibliotech.api.model.Usuario;
import com.bibliotech.api.repository.NotificacaoRepository;
import com.bibliotech.api.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.bibliotech.api.repository.EmprestimoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/usuarios") // Todas as URLs dessa classe vÃ£o comeÃ§ar com /usuarios
public class UsuarioController {

    // Injeta o repository automaticamente (o Spring instancia para nÃ³s)
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmprestimoRepository emprestimoRepository;
    private final NotificacaoRepository notificacaoRepository;

    UsuarioController(PasswordEncoder passwordEncoder, UsuarioRepository repository, EmprestimoRepository emprestimoRepository, NotificacaoRepository notificacaoRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emprestimoRepository = emprestimoRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    // Rota para CADASTRAR um usuÃ¡rio (MÃ©todo POST)
    @PostMapping
    public Usuario cadastrarUsuario(@RequestBody Usuario novoUsuario) {
        String senhaCriptografada = passwordEncoder.encode(novoUsuario.getSenha());
        novoUsuario.setSenha(senhaCriptografada);
        Usuario usuarioSalvo = repository.save(novoUsuario);

        // Gera notificaÃ§Ã£o de Boas-vindas automÃ¡tica
        if ("LEITOR".equalsIgnoreCase(usuarioSalvo.getTipo())) {
            Notificacao boasVindas = new Notificacao();
            boasVindas.setUsuario(usuarioSalvo);
            boasVindas.setMensagem("Bem-vindo ao BiblioTech AI! Explore nosso catÃ¡logo e converse com a Lumina para receber recomendaÃ§Ãµes personalizadas.");
            boasVindas.setDataEnvio(LocalDateTime.now());
            boasVindas.setLida(false);
            notificacaoRepository.save(boasVindas);
        }

        return usuarioSalvo;
    }

    // Rota para LISTAR TODOS (GET)
    @GetMapping
    public List<Usuario> listarUsuarios() {
        // O findByEnabledTrue() vai no banco, pega todos os registros com enabled=true e converte para uma lista JSON!
        return repository.findByEnabledTrue();
    }
    // Rota para LISTAR UM USUÃRIO ESPECÃFICO (GET) com tratamento de erro 404
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarUsuarioPorId(@PathVariable Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());
        
        return ResponseEntity.ok(usuario);
    }

    // Rota para o usuÃ¡rio logado pegar seus prÃ³prios dados
    @GetMapping("/me")
    public ResponseEntity<Usuario> buscarUsuarioLogado() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UsuÃ¡rio nÃ£o autenticado.");
        }
        return ResponseEntity.ok((Usuario) principal);
    }

    // Rota PUT para atualizar dados do usuÃ¡rio
    @PutMapping("/{id}")
    public Usuario atualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioAtualizado) {
        
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UsuÃ¡rio nÃ£o autenticado.");
        }
        Usuario usuarioLogado = (Usuario) principal;
        
        if (!usuarioLogado.getTipo().equalsIgnoreCase("ADMIN") && !usuarioLogado.getId().equals(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, 
                "VocÃª nÃ£o tem permissÃ£o para alterar os dados de outro usuÃ¡rio."
            );
        }

        Usuario usuarioExistente = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());

        // ProteÃ§Ã£o: se nÃ£o for admin, nÃ£o pode alterar o prÃ³prio tipo ou status
        if (!usuarioLogado.getTipo().equalsIgnoreCase("ADMIN")) {
            usuarioAtualizado.setTipo(usuarioExistente.getTipo());
            usuarioAtualizado.setStatus(usuarioExistente.getStatus());
        }

        // Atualiza os dados bÃ¡sicos
        usuarioExistente.setNome(usuarioAtualizado.getNome());
        usuarioExistente.setEmail(usuarioAtualizado.getEmail());
        usuarioExistente.setTipo(usuarioAtualizado.getTipo());
        usuarioExistente.setStatus(usuarioAtualizado.getStatus());

        // Se o usuÃ¡rio mandou uma senha na requisiÃ§Ã£o (nÃ£o estÃ¡ nula nem vazia), atualiza o Hash
        if (usuarioAtualizado.getSenha() != null && !usuarioAtualizado.getSenha().trim().isEmpty()) {
            String senhaCriptografada = passwordEncoder.encode(usuarioAtualizado.getSenha());
            usuarioExistente.setSenha(senhaCriptografada);
        }

        return repository.save(usuarioExistente);
    }

    // Rota DELETE (Soft Delete) para inativar o usuÃ¡rio sem perder o histÃ³rico
    @DeleteMapping("/{id}")
    public void inativarUsuario(@PathVariable Long id) {
        
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UsuÃ¡rio nÃ£o autenticado.");
        }
        Usuario usuarioLogado = (Usuario) principal;
        
        if (!usuarioLogado.getTipo().equalsIgnoreCase("ADMIN") && !usuarioLogado.getId().equals(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, 
                "VocÃª nÃ£o tem permissÃ£o para inativar outro usuÃ¡rio."
            );
        }

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());

        // Muda o status visual e bloqueia o acesso no Spring Security
        usuario.setStatus(com.bibliotech.api.model.UsuarioStatus.INATIVO);
        usuario.setEnabled(false);

        repository.save(usuario);
    }

    // Rota de GamificaÃ§Ã£o (Calcula o nÃ­vel do usuÃ¡rio baseado em emprÃ©stimos)
    @GetMapping("/{id}/gamificacao")
    public ResponseEntity<Map<String, Object>> obterGamificacao(@PathVariable Long id) {
        // Verifica se o usuÃ¡rio existe
        repository.findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());

        // Busca o nÃºmero de emprÃ©stimos vÃ¡lidos (nÃ£o conta os cancelados)
        int totalEmprestimos = emprestimoRepository.countByUsuarioIdAndStatusIn(
            id, 
            java.util.List.of(
                com.bibliotech.api.model.EmprestimoStatus.ATIVO,
                com.bibliotech.api.model.EmprestimoStatus.ATRASADO,
                com.bibliotech.api.model.EmprestimoStatus.DEVOLVIDO
            )
        );

        String nivel;
        String selo;

        if (totalEmprestimos >= 10) {
            nivel = "Mestre da Leitura";
            selo = "\uD83C\uDFC6"; // 🏆
        } else if (totalEmprestimos >= 5) {
            nivel = "Leitor Ass\u00EDduo";
            selo = "\uD83E\uDD47"; // 🥇
        } else if (totalEmprestimos >= 1) {
            nivel = "Leitor Iniciante";
            selo = "\uD83E\uDD48"; // 🥈
        } else {
            nivel = "Visitante";
            selo = "\uD83D\uDCDA"; // 📚
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("totalEmprestimos", totalEmprestimos);
        resultado.put("nivel", nivel);
        resultado.put("selo", selo);

        return ResponseEntity.ok(resultado);
    }
}

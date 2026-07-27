package com.bibliotech.api.controller;

import com.bibliotech.api.model.Usuario;
import com.bibliotech.api.repository.UsuarioRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios") // Todas as URLs dessa classe vão começar com /usuarios
public class UsuarioController {

    // Injeta o repository automaticamente (o Spring instancia para nós)
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    UsuarioController(PasswordEncoder passwordEncoder, UsuarioRepository repository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // Rota para CADASTRAR um usuário (Método POST)
    @PostMapping
    public Usuario cadastrarUsuario(@RequestBody Usuario novoUsuario) {
        String senhaCriptografada = passwordEncoder.encode(novoUsuario.getSenha());
        novoUsuario.setSenha(senhaCriptografada);
        return repository.save(novoUsuario);
    }

    // Rota para LISTAR TODOS (GET)
    @GetMapping
    public List<Usuario> listarUsuarios() {
        // O findByEnabledTrue() vai no banco, pega todos os registros com enabled=true e converte para uma lista JSON!
        return repository.findByEnabledTrue();
    }
    // Rota para LISTAR UM USUÁRIO ESPECÍFICO (GET) com tratamento de erro 404
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarUsuarioPorId(@PathVariable Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());
        
        return ResponseEntity.ok(usuario);
    }

    // Rota PUT para atualizar dados do usuário
    @PutMapping("/{id}")
    public Usuario atualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioAtualizado) {
        
        Usuario usuarioExistente = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());

        // Atualiza os dados básicos
        usuarioExistente.setNome(usuarioAtualizado.getNome());
        usuarioExistente.setEmail(usuarioAtualizado.getEmail());
        usuarioExistente.setTipo(usuarioAtualizado.getTipo());
        usuarioExistente.setStatus(usuarioAtualizado.getStatus());

        // Se o usuário mandou uma senha na requisição (não está nula nem vazia), atualiza o Hash
        if (usuarioAtualizado.getSenha() != null && !usuarioAtualizado.getSenha().trim().isEmpty()) {
            String senhaCriptografada = passwordEncoder.encode(usuarioAtualizado.getSenha());
            usuarioExistente.setSenha(senhaCriptografada);
        }

        return repository.save(usuarioExistente);
    }

    // Rota DELETE (Soft Delete) para inativar o usuário sem perder o histórico
    @DeleteMapping("/{id}")
    public void inativarUsuario(@PathVariable Long id) {
        
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());

        // Muda o status visual e bloqueia o acesso no Spring Security
        usuario.setStatus("INATIVO");
        usuario.setEnabled(false);

        // Salva a alteração
        repository.save(usuario);
    }
}
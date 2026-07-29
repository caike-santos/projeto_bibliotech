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
@RequestMapping("/usuarios") // Todas as URLs dessa classe vão começar com /usuarios
public class UsuarioController {

    // Injeta o repository automaticamente (o Spring instancia para nós)
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

    // Rota para CADASTRAR um usuário (Método POST)
    @PostMapping
    public Usuario cadastrarUsuario(@RequestBody Usuario novoUsuario) {
        String senhaCriptografada = passwordEncoder.encode(novoUsuario.getSenha());
        novoUsuario.setSenha(senhaCriptografada);
        Usuario usuarioSalvo = repository.save(novoUsuario);

        // Gera notificação de Boas-vindas automática
        if ("LEITOR".equalsIgnoreCase(usuarioSalvo.getTipo())) {
            Notificacao boasVindas = new Notificacao();
            boasVindas.setUsuario(usuarioSalvo);
            boasVindas.setMensagem("Bem-vindo ao BiblioTech AI! Explore nosso catálogo e converse com a Lumina para receber recomendações personalizadas.");
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

        repository.save(usuario);
    }

    // Rota de Gamificação (Calcula o nível do usuário baseado em empréstimos)
    @GetMapping("/{id}/gamificacao")
    public ResponseEntity<Map<String, Object>> obterGamificacao(@PathVariable Long id) {
        // Verifica se o usuário existe
        repository.findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());

        // Busca o número de empréstimos
        int totalEmprestimos = emprestimoRepository.findByUsuarioId(id).size();

        String nivel;
        String selo;

        if (totalEmprestimos >= 10) {
            nivel = "Mestre da Leitura";
            selo = "🏆";
        } else if (totalEmprestimos >= 5) {
            nivel = "Leitor Assíduo";
            selo = "🥇";
        } else if (totalEmprestimos >= 1) {
            nivel = "Leitor Iniciante";
            selo = "🥈";
        } else {
            nivel = "Visitante";
            selo = "📚";
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("totalEmprestimos", totalEmprestimos);
        resultado.put("nivel", nivel);
        resultado.put("selo", selo);

        return ResponseEntity.ok(resultado);
    }
}
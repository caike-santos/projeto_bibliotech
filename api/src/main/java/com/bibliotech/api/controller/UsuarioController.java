package com.bibliotech.api.controller;

import com.bibliotech.api.model.Usuario;
import com.bibliotech.api.repository.UsuarioRepository;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios") // Todas as URLs dessa classe vão começar com /usuarios
public class UsuarioController {

    // Injeta o repository automaticamente (o Spring instancia para nós)
    private final UsuarioRepository repository;

    UsuarioController(UsuarioRepository repository) {
        this.repository = repository;
    }

    // Rota para CADASTRAR um usuário (Método POST)
    @PostMapping
    public Usuario cadastrarUsuario(@RequestBody Usuario novoUsuario) {
        // O @RequestBody converte o JSON que vem da internet em um objeto Java
        return repository.save(novoUsuario); // Salva no banco e devolve os dados com o ID gerado
    }

    // Rota para LISTAR TODOS (GET)
    @GetMapping
    public List<Usuario> listarUsuarios() {
        // O findAll() vai no banco, pega todos os registros e converte para uma lista JSON!
        return repository.findAll();
    }
}
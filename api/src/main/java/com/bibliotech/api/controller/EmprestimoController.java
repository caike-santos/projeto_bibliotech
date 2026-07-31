package com.bibliotech.api.controller;

import com.bibliotech.api.model.Emprestimo;
import com.bibliotech.api.repository.EmprestimoRepository;
import com.bibliotech.api.service.EmprestimoService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/emprestimos")
public class EmprestimoController {
    private final EmprestimoRepository repository;
    private final EmprestimoService service;

    EmprestimoController(EmprestimoRepository repository, EmprestimoService service) {
        this.repository = repository;
        this.service = service;
    }

    @PostMapping
    public Emprestimo cadastrarEmprestimo(@RequestBody Emprestimo novoEmprestimo, @RequestParam(defaultValue = "false") boolean balcao) {
       verificarPermissaoUsuario(novoEmprestimo.getUsuario().getId());
       // Agora quem cuida de salvar é o Service, que vai rodar as regras antes!
       Emprestimo emprestimo = service.realizarEmprestimo(novoEmprestimo);
       if (balcao) {
           return service.confirmarRetirada(emprestimo.getId());
       }
       return emprestimo;
    }

    @GetMapping
    public List<Emprestimo> listarEmprestimos() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Emprestimo> buscarEmprestimoPorId(@PathVariable Long id) {
        Emprestimo emprestimo = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());
        
        return ResponseEntity.ok(emprestimo);
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Emprestimo>> buscarEmprestimosDoUsuario(@PathVariable Long usuarioId) {
        verificarPermissaoUsuario(usuarioId);
        List<Emprestimo> historico = repository.findByUsuarioId(usuarioId);
        
        return ResponseEntity.ok(historico);
    }

    @PutMapping("/{id}/renovar")
    public Emprestimo renovarEmprestimo(@PathVariable Long id) {
        return service.renovarEmprestimo(id);
    }

    // A bibliotecária acessa essa rota enviando o ID do empréstimo na URL
    @PutMapping("/{id}/devolver")
    public Emprestimo realizarDevolucao(@PathVariable Long id) {
        return service.devolverLivro(id);
    }

    @PutMapping("/{id}/confirmar-retirada")
    public Emprestimo confirmarRetirada(@PathVariable Long id) {
        return service.confirmarRetirada(id);
    }

    @PutMapping("/{id}/cancelar")
    public Emprestimo cancelarEmprestimo(@PathVariable Long id) {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof com.bibliotech.api.model.Usuario)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        com.bibliotech.api.model.Usuario usuarioLogado = (com.bibliotech.api.model.Usuario) principal;
        boolean isAdmin = usuarioLogado.getTipo().equalsIgnoreCase("ADMIN") || usuarioLogado.getTipo().equalsIgnoreCase("BIBLIOTECARIO");
        
        return service.cancelarEmprestimo(id, usuarioLogado.getId(), isAdmin);
    }

    private void verificarPermissaoUsuario(Long usuarioIdAlvo) {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof com.bibliotech.api.model.Usuario)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UsuÃ¡rio nÃ£o autenticado.");
        }
        com.bibliotech.api.model.Usuario usuarioLogado = (com.bibliotech.api.model.Usuario) principal;
        if (usuarioLogado.getTipo().equalsIgnoreCase("LEITOR") && !usuarioLogado.getId().equals(usuarioIdAlvo)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Acesso negado: VocÃª nÃ£o tem permissÃ£o para acessar os dados deste usuÃ¡rio.");
        }
    }
}

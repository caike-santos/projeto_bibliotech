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
    public Emprestimo cadastrarEmprestimo(@RequestBody Emprestimo novoEmprestimo) {
       // Agora quem cuida de salvar é o Service, que vai rodar as regras antes!
        return service.realizarEmprestimo(novoEmprestimo);
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
    
}

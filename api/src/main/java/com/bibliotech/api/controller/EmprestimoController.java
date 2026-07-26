package com.bibliotech.api.controller;

import com.bibliotech.api.model.Emprestimo;
import com.bibliotech.api.repository.EmprestimoRepository;
import com.bibliotech.api.service.EmprestimoService;

import java.util.List;

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

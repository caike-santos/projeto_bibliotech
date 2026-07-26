package com.bibliotech.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import com.bibliotech.api.model.Livro;
import com.bibliotech.api.service.LivroApiService;
import com.bibliotech.api.service.IaService;
import com.bibliotech.api.repository.LivroRepository; // Importante para SALVAR no final

@RestController
@RequestMapping("/livros")
public class LivroController {

    private final LivroApiService livroApiService;
    private final IaService iaService;
    private final LivroRepository livroRepository;

    public LivroController(LivroApiService livroApiService, IaService iaService, LivroRepository livroRepository) {
        this.livroApiService = livroApiService;
        this.iaService = iaService;
        this.livroRepository = livroRepository;
    }

    // A mágica completa acontece aqui!
    @PostMapping("/cadastrar-por-isbn/{isbn}")
    public Livro cadastrarLivroAutomaticamente(@PathVariable String isbn) {
        
        // 1. Busca os dados brutos na Brasil API (O que você acabou de fazer)
        Livro livro = livroApiService.buscarDadosDoLivroPorIsbn(isbn);

        // 2. Passa o livro pela Inteligência Artificial para descobrir o Gênero e preencher o resto
        iaService.enriquecerDadosDoLivro(livro);

        // 3. Salva no banco de dados
        return livroRepository.save(livro);
    }

    // 1. Rota clássica para listar TODOS os livros do banco
    @GetMapping
    public List<Livro> listarTodos() {
        return livroRepository.findAll();
    }

    // 2. Rota para filtrar pelo gênero literário gerado pela IA
    @GetMapping("/busca/genero")
    public List<Livro> buscarPorGenero(@RequestParam String nome) {
        return livroRepository.findByGeneroPrincipalIgnoreCase(nome);
    }

    // 3. Rota para filtrar por trechos do nome do autor
    @GetMapping("/busca/autor")
    public List<Livro> buscarPorAutor(@RequestParam String nome) {
        return livroRepository.findByAutorContainingIgnoreCase(nome);
    }
}
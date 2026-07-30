package com.bibliotech.api.controller;

import java.util.List;


import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bibliotech.api.model.Livro;
import com.bibliotech.api.service.IaService;
import com.bibliotech.api.repository.EmprestimoRepository;
import com.bibliotech.api.repository.LivroRepository; // Importante para SALVAR no final
import com.bibliotech.api.repository.TagRepository;
import com.bibliotech.api.model.Tag;
import java.util.Optional;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/livros")
public class LivroController {

    private final IaService iaService;
    private final LivroRepository livroRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final TagRepository tagRepository;

    public LivroController(IaService iaService, LivroRepository livroRepository, EmprestimoRepository emprestimoRepository, TagRepository tagRepository) {
        this.iaService = iaService;
        this.livroRepository = livroRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.tagRepository = tagRepository;
    }

    // A mágica completa acontece aqui!
   @PostMapping("/cadastrar-por-isbn/{isbn}")
    public Livro cadastrarLivroAutomaticamente(
            @PathVariable String isbn, 
            @RequestParam(defaultValue = "1") Integer quantidade,
            @RequestParam(required = false) String tituloHint,
            @RequestParam(required = false) String autorHint) { 
        
        // 1. Busca todos os dados do livro, gênero e capa usando EXCLUSIVAMENTE a Inteligência Artificial Gemini
        Livro livro = iaService.buscarLivroCompletoPorIsbn(isbn, tituloHint, autorHint);

        // 2. Aplica a sua regra de negócio de estoque
        livro.setQuantidadeTotal(quantidade);
        livro.setQuantidadeDisponivel(quantidade);

        // 3. Salva no banco de dados
        return livroRepository.save(livro);
    }

    // Rota POST (Manual) para cadastrar livro sem passar pela IA
    @PostMapping
    public Livro cadastrarLivroManual(@RequestBody Livro livroManual) {
        // Validação de coerência do estoque
        if (livroManual.getQuantidadeDisponivel() != null && livroManual.getQuantidadeTotal() != null) {
            if (livroManual.getQuantidadeDisponivel() > livroManual.getQuantidadeTotal()) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, 
                    "A quantidade disponível não pode ser maior que a quantidade total."
                );
            }
        }

        // Processamento de Tags
        List<Tag> tagsProcessadas = new ArrayList<>();
        if (livroManual.getTagsSecundarias() != null) {
            for (Tag tagEnviada : livroManual.getTagsSecundarias()) {
                if (tagEnviada.getNome() != null && !tagEnviada.getNome().trim().isEmpty()) {
                    String nomeTag = tagEnviada.getNome().trim();
                    Optional<Tag> tagExistente = tagRepository.findByNomeIgnoreCase(nomeTag);
                    if (tagExistente.isPresent()) {
                        tagsProcessadas.add(tagExistente.get());
                    } else {
                        Tag novaTag = new Tag(nomeTag);
                        novaTag = tagRepository.save(novaTag);
                        tagsProcessadas.add(novaTag);
                    }
                }
            }
        }
        livroManual.setTagsSecundarias(tagsProcessadas);

        return livroRepository.save(livroManual);
    }

    // 1. Rota clássica para listar TODOS os livros do banco
   
    @GetMapping
    public Page<Livro> listarLivros(@ParameterObject @PageableDefault(size = 100, sort = {"titulo"}) Pageable paginacao, 
                                    @RequestParam(required = false, defaultValue = "false") boolean todos) {
        if (todos) {
            return livroRepository.findAll(paginacao);
        }
        return livroRepository.findByAtivoTrue(paginacao);
    }

    // 2. Rota para filtrar pelo gênero literário gerado pela IA
    @GetMapping("/busca/genero")
    public List<Livro> buscarPorGenero(@RequestParam String nome) {
        return livroRepository.findByGeneroPrincipalIgnoreCaseAndAtivoTrue(nome);
    }

    // 3. Rota para filtrar por trechos do nome do autor
    @GetMapping("/busca/autor")
    public List<Livro> buscarPorAutor(@RequestParam String nome) {
        return livroRepository.findByAutorContainingIgnoreCaseAndAtivoTrue(nome);
    }

    // Rota para buscar livro por ID com tratamento 404
    @GetMapping("/{id}")
    public Livro buscarPorId(@PathVariable Long id) {
        return livroRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());
    }

    // Rota PUT para atualizar dados de um livro existente (ex: arrumar campos nulos)
    @PutMapping("/{id}")
    public Livro atualizarLivro(@PathVariable Long id, @RequestBody Livro livroAtualizado) {
        
        // 1. Busca o livro existente ou devolve Erro 404
        Livro livroExistente = livroRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());

        // 2. Atualiza apenas os campos cadastrais
        livroExistente.setTitulo(livroAtualizado.getTitulo());
        livroExistente.setAutor(livroAtualizado.getAutor());
        livroExistente.setEditora(livroAtualizado.getEditora());
        livroExistente.setAno(livroAtualizado.getAno());
        livroExistente.setCapaUrl(livroAtualizado.getCapaUrl());
        livroExistente.setSinopse(livroAtualizado.getSinopse());
        livroExistente.setGeneroPrincipal(livroAtualizado.getGeneroPrincipal());
        
        if (livroAtualizado.getQuantidadeTotal() != null) {
            livroExistente.setQuantidadeTotal(livroAtualizado.getQuantidadeTotal());
        }
        if (livroAtualizado.getQuantidadeDisponivel() != null) {
            livroExistente.setQuantidadeDisponivel(livroAtualizado.getQuantidadeDisponivel());
        }

        // Validação de coerência do estoque
        if (livroExistente.getQuantidadeDisponivel() != null && livroExistente.getQuantidadeTotal() != null) {
            if (livroExistente.getQuantidadeDisponivel() > livroExistente.getQuantidadeTotal()) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, 
                    "A quantidade disponível não pode ser maior que a quantidade total."
                );
            }
        }
        
        List<Tag> tagsProcessadas = new ArrayList<>();
        if (livroAtualizado.getTagsSecundarias() != null) {
            for (Tag tagEnviada : livroAtualizado.getTagsSecundarias()) {
                if (tagEnviada.getNome() != null && !tagEnviada.getNome().trim().isEmpty()) {
                    String nomeTag = tagEnviada.getNome().trim();
                    Optional<Tag> tagExistente = tagRepository.findByNomeIgnoreCase(nomeTag);
                    if (tagExistente.isPresent()) {
                        tagsProcessadas.add(tagExistente.get());
                    } else {
                        Tag novaTag = new Tag(nomeTag);
                        novaTag = tagRepository.save(novaTag);
                        tagsProcessadas.add(novaTag);
                    }
                }
            }
        }
        livroExistente.setTagsSecundarias(tagsProcessadas);

        // 3. Salva a versão atualizada por cima da antiga
        return livroRepository.save(livroExistente);
    }

    // Rota DELETE (Soft Delete) para inativar o livro do catálogo
    @DeleteMapping("/{id}")
    public void inativarLivro(@PathVariable Long id) {
        
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException());

        // Inativa o livro
        livro.setAtivo(false);

        // Opcional: Você pode zerar a quantidade disponível aqui se a regra de negócio exigir
        // livro.setQuantidadeDisponivel(0);

        livroRepository.save(livro);
    }

    // Rota DELETE (Hard Delete) para excluir totalmente o livro do banco de dados
    @DeleteMapping("/hard/{id}")
    public ResponseEntity<?> excluirLivroDefinitivo(@PathVariable Long id) {
        // 1. Busca o livro
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Livro não encontrado."));

        // 2. Valida se o livro tem algum histórico de empréstimo
        boolean possuiEmprestimo = emprestimoRepository.existsByLivroId(id);
        if (possuiEmprestimo) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, 
                "Não é possível excluir definitivamente um livro que possui histórico de empréstimos. Use a opção de Inativar."
            );
        }

        // 3. Limpa associações para evitar erro de Foreign Key (ex: na tabela livro_tags)
        livro.getTagsSecundarias().clear();
        livroRepository.save(livro);

        // 4. Exclui permanentemente
        livroRepository.delete(livro);

        return ResponseEntity.ok().build();
    }
    // Rota de IA: Gera recomendações baseadas no histórico do leitor e no acervo real
    @GetMapping("/recomendacoes/usuario/{usuarioId}")
    public ResponseEntity<String> recomendarLivros(@PathVariable Long usuarioId) {
        
        // 1. Busca todo o histórico de empréstimos desse usuário
        List<com.bibliotech.api.model.Emprestimo> historico = emprestimoRepository.findByUsuarioId(usuarioId);
        
        // 2. Extrai apenas os nomes dos livros que ele já pegou
        List<String> livrosLidos = historico.stream()
                .map(emprestimo -> emprestimo.getLivro().getTitulo())
                .distinct()
                .toList();
                
        // 3. Busca o catálogo atual do sistema (Apenas livros ativos)
        // Usamos findAll() e filtramos para simplificar, mapeando Título e Gênero para ajudar a IA
        String catalogoDisponivel = livroRepository.findAll().stream()
                .filter(com.bibliotech.api.model.Livro::isAtivo)
                .map(livro -> livro.getTitulo() + " (" + livro.getGeneroPrincipal() + ")")
                .collect(java.util.stream.Collectors.joining(", "));
                
        // 4. Manda o histórico e o catálogo real para o Gemini trabalhar
        String recomendacao = iaService.gerarRecomendacoesParaUsuario(livrosLidos, catalogoDisponivel);
        
        return ResponseEntity.ok(recomendacao);
    }

    // Rota de IA: Gera recomendações usando Clustering (Agrupamento de Leitores Similares)
    @GetMapping("/clustering/usuario/{usuarioId}")
    public ResponseEntity<String> recomendacaoClustering(@PathVariable Long usuarioId) {
        // 1. Busca os livros lidos do usuário atual
        List<com.bibliotech.api.model.Emprestimo> historicoUsuario = emprestimoRepository.findByUsuarioId(usuarioId);
        List<String> livrosLidos = historicoUsuario.stream()
                .map(emprestimo -> emprestimo.getLivro().getTitulo())
                .distinct()
                .toList();

        // 2. Busca histórico de todos os outros usuários
        List<com.bibliotech.api.model.Emprestimo> todosEmprestimos = emprestimoRepository.findAll();
        
        java.util.Map<Long, java.util.List<String>> agrupamentoLeitores = todosEmprestimos.stream()
            .filter(e -> !e.getUsuario().getId().equals(usuarioId))
            .collect(
                java.util.stream.Collectors.groupingBy(
                    e -> e.getUsuario().getId(),
                    java.util.stream.Collectors.mapping(e -> e.getLivro().getTitulo(), java.util.stream.Collectors.toList())
                )
            );
            
        // Formata string de outros leitores: "Leitor 2: [Livro A, Livro B]; Leitor 3: [Livro C]"
        String outrosLeitores = agrupamentoLeitores.entrySet().stream()
            .map(entry -> "Leitor " + entry.getKey() + ": " + entry.getValue())
            .collect(java.util.stream.Collectors.joining("; "));

        // 3. Busca catálogo disponível
        String catalogoDisponivel = livroRepository.findAll().stream()
                .filter(com.bibliotech.api.model.Livro::isAtivo)
                .map(livro -> livro.getTitulo() + " (" + livro.getGeneroPrincipal() + ")")
                .collect(java.util.stream.Collectors.joining(", "));

        // 4. Manda pra IA gerar o clustering
        String recomendacao = iaService.gerarRecomendacoesDeClustering(livrosLidos, outrosLeitores, catalogoDisponivel);
        
        return ResponseEntity.ok(recomendacao);
    }

}
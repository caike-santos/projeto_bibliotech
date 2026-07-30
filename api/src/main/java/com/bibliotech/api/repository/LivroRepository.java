package com.bibliotech.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bibliotech.api.model.Livro;

@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {
    
    // Substitui o findAll() padrão, trazendo apenas os ativos com paginação
    Page<Livro> findByAtivoTrue(Pageable paginacao);

    // Verifica se um ISBN já existe no banco de dados
    boolean existsByIsbn(String isbn);

    // Busca exata pelo gênero ignorando maiúsculas/minúsculas APENAS DE LIVROS ATIVOS
    List<Livro> findByGeneroPrincipalIgnoreCaseAndAtivoTrue(String generoPrincipal);
    
    // Busca livros cujo autor contenha o termo pesquisado APENAS DE LIVROS ATIVOS
    List<Livro> findByAutorContainingIgnoreCaseAndAtivoTrue(String autor);
    List<Livro> findByAtivoTrue();
}
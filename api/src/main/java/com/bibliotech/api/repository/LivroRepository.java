package com.bibliotech.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bibliotech.api.model.Livro;

@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {
    // Busca exata pelo gênero ignorando letras maiúsculas/minúsculas
    List<Livro> findByGeneroPrincipalIgnoreCase(String generoPrincipal);
    
    // Busca livros cujo autor contenha o termo pesquisado (como um operador LIKE % % no SQL)
    List<Livro> findByAutorContainingIgnoreCase(String autor);
}

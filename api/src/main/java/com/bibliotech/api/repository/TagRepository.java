package com.bibliotech.api.repository;

import com.bibliotech.api.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    // Busca exata pelo nome ignorando maiúsculas e minúsculas (evita duplicação)
    Optional<Tag> findByNomeIgnoreCase(String nome);
}
package com.bibliotech.api.repository;

import com.bibliotech.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Só de estender o JpaRepository, você já ganha de brinde os métodos:
    // save(), findAll(), findById(), deleteById() etc.
}
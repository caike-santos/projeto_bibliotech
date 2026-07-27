package com.bibliotech.api.repository;

import java.util.List;
import com.bibliotech.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    UserDetails findByEmail(String username);
    
    // Substitui o findAll() padrão para listar apenas os usuários ativos
    List<Usuario> findByEnabledTrue();
}
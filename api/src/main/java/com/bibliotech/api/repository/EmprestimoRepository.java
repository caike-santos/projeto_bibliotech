package com.bibliotech.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bibliotech.api.model.Emprestimo;
@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long>{
    // O Spring Boot cria a query SQL de contagem automaticamente só lendo o nome desse método!
    int countByUsuarioIdAndStatus(Long usuarioId, String status);

    // Regra 3 (Novo): Verifica se existe ALGUM empréstimo com um status específico para o usuário
    boolean existsByUsuarioIdAndStatus(Long usuarioId, String status);
}

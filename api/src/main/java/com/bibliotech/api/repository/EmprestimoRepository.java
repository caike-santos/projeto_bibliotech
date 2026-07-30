package com.bibliotech.api.repository;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bibliotech.api.model.Emprestimo;
import com.bibliotech.api.model.EmprestimoStatus;

@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long>{
    // O Spring Boot cria a query SQL de contagem automaticamente só lendo o nome desse método!
    int countByUsuarioIdAndStatus(Long usuarioId, EmprestimoStatus status);

    // Regra 3 (Novo): Verifica se existe ALGUM empréstimo com um status específico para o usuário
    boolean existsByUsuarioIdAndStatus(Long usuarioId, EmprestimoStatus status);

    // Verifica se o usuário já possui um empréstimo ativo ou atrasado de um livro específico
    boolean existsByUsuarioIdAndLivroIdAndStatusIn(Long usuarioId, Long livroId, List<EmprestimoStatus> statuses);

    // Busca todo o histórico de empréstimos de um usuário específico
    List<Emprestimo> findByUsuarioId(Long usuarioId);

    // Conta quantos empréstimos um usuário possui considerando apenas os status informados
    int countByUsuarioIdAndStatusIn(Long usuarioId, List<EmprestimoStatus> statuses);

    // Encontra todos os empréstimos previstos para uma data e status específicos
    List<Emprestimo> findByDataDevolucaoPrevistaAndStatus(java.time.LocalDate data, EmprestimoStatus status);

    // Encontra todos os empréstimos vencidos (data prevista antes de hoje) e com um status específico
    List<Emprestimo> findByStatusAndDataDevolucaoPrevistaBefore(EmprestimoStatus status, java.time.LocalDate data);

    // Verifica se existe qualquer empréstimo para um livro específico (usado na exclusão definitiva)
    boolean existsByLivroId(Long livroId);

    // Encontra empréstimos de todos os outros usuários (usado na IA de Clustering)
    List<Emprestimo> findByUsuarioIdNot(Long usuarioId);
}

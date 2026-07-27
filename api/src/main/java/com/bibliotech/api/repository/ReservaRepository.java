package com.bibliotech.api.repository;

import com.bibliotech.api.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    // Verifica se já existe alguma reserva aguardando para aquele livro (usado no EmprestimoService para bloquear renovação)
    boolean existsByLivroIdAndStatus(Long livroId, String status);
    
    // Evita que o mesmo usuário entre na fila do mesmo livro duas vezes
    boolean existsByUsuarioIdAndLivroIdAndStatus(Long usuarioId, Long livroId, String status);
    
    // Traz a fila de espera do livro ordenada pela data (do mais antigo para o mais novo)
    List<Reserva> findByLivroIdAndStatusOrderByDataSolicitacaoAsc(Long livroId, String status);

    // Busca uma reserva específica de um usuário para um livro com um determinado status
    java.util.Optional<Reserva> findFirstByUsuarioIdAndLivroIdAndStatus(Long usuarioId, Long livroId, String status);
}
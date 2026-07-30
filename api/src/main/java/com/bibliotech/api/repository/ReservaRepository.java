package com.bibliotech.api.repository;

import com.bibliotech.api.model.Reserva;
import com.bibliotech.api.model.ReservaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    // Verifica se jÃ¡ existe alguma reserva aguardando para aquele livro (usado no EmprestimoService para bloquear renovaÃ§Ã£o)
    boolean existsByLivroIdAndStatus(Long livroId, ReservaStatus status);
    
    // Evita que o mesmo usuÃ¡rio entre na fila do mesmo livro duas vezes
    boolean existsByUsuarioIdAndLivroIdAndStatus(Long usuarioId, Long livroId, ReservaStatus status);
    
    // Traz a fila de espera do livro ordenada pela data (do mais antigo para o mais novo)
    List<Reserva> findByLivroIdAndStatusOrderByDataSolicitacaoAsc(Long livroId, ReservaStatus status);

    // Busca uma reserva especÃ­fica de um usuÃ¡rio para um livro com um determinado status
    Optional<Reserva> findFirstByUsuarioIdAndLivroIdAndStatus(Long usuarioId, Long livroId, ReservaStatus status);

    // Busca o histÃ³rico de reservas de um usuÃ¡rio, da mais recente para a mais antiga
    List<Reserva> findByUsuarioIdOrderByDataSolicitacaoDesc(Long usuarioId);

    // Conta quantas reservas existem para um livro com status AGUARDANDO antes da data solicitada
    int countByLivroIdAndStatusAndDataSolicitacaoBefore(Long livroId, ReservaStatus status, java.time.LocalDateTime data);
}

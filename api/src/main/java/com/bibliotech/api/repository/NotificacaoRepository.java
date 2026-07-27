package com.bibliotech.api.repository;

import com.bibliotech.api.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    
    // Traz as notificações de um usuário específico, ordenadas da mais recente para a mais antiga
    List<Notificacao> findByUsuarioIdOrderByDataEnvioDesc(Long usuarioId);
    
    // Conta quantas notificações não lidas o usuário tem (perfeito para fazer um ícone de sininho com notificação)
    int countByUsuarioIdAndLidaFalse(Long usuarioId);
}
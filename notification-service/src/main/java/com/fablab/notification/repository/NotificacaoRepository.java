package com.fablab.notification.repository;

import com.fablab.notification.entity.Notificacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code notificacao}.
 */
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    long countByIdUsuarioAndLidaFalse(Long idUsuario);

    List<Notificacao> findTop20ByIdUsuarioOrderByCriadaEmDesc(Long idUsuario);
}
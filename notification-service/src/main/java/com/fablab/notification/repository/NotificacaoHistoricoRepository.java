package com.fablab.notification.repository;

import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.NotificacaoHistorico;
import com.fablab.notification.entity.TipoEvento;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do histórico de notificações revisadas. */
public interface NotificacaoHistoricoRepository extends JpaRepository<NotificacaoHistorico, Long> {

    List<NotificacaoHistorico> findAllByOrderByDataRevisaoAdminDesc();

    List<NotificacaoHistorico> findByCanalOrderByDataRevisaoAdminDesc(CanalNotificacao canal);

    List<NotificacaoHistorico> findByTipoEventoOrderByDataRevisaoAdminDesc(TipoEvento tipoEvento);

    List<NotificacaoHistorico> findByCanalAndTipoEventoOrderByDataRevisaoAdminDesc(CanalNotificacao canal,
                                                                                   TipoEvento tipoEvento);

    long deleteByDataRevisaoAdminBefore(LocalDateTime limite);
}

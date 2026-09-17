package com.fablab.notification.repository;

import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.entity.StatusNotificacao;
import com.fablab.notification.entity.TipoEvento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório das notificações ativas. */
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByIdDestinatarioOrderByDataCriacaoDesc(Long idDestinatario);

    List<Notificacao> findByIdDestinatarioAndStatusOrderByDataCriacaoDesc(Long idDestinatario,
                                                                          StatusNotificacao status);

    List<Notificacao> findByIdDestinatarioAndTipoEventoOrderByDataCriacaoDesc(Long idDestinatario,
                                                                              TipoEvento tipoEvento);

    List<Notificacao> findByIdDestinatarioAndStatusAndTipoEventoOrderByDataCriacaoDesc(
            Long idDestinatario, StatusNotificacao status, TipoEvento tipoEvento);

    List<Notificacao> findAllByOrderByDataCriacaoDesc();

    List<Notificacao> findByStatusOrderByDataCriacaoDesc(StatusNotificacao status);

    List<Notificacao> findByTipoEventoOrderByDataCriacaoDesc(TipoEvento tipoEvento);

    List<Notificacao> findByStatusAndTipoEventoOrderByDataCriacaoDesc(StatusNotificacao status,
                                                                      TipoEvento tipoEvento);
}

package com.fablab.notification.dto;

import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.entity.StatusNotificacao;
import com.fablab.notification.entity.TipoEvento;
import java.time.LocalDateTime;

/** Representação de uma notificação ativa. */
public record NotificacaoResponse(
        Long idNotificacao,
        Long idDestinatario,
        CanalNotificacao canal,
        TipoEvento tipoEvento,
        String assunto,
        String mensagem,
        Long idReferencia,
        StatusNotificacao status,
        LocalDateTime dataCriacao,
        LocalDateTime dataEnvio,
        LocalDateTime dataLeitura) {

    public static NotificacaoResponse from(Notificacao n) {
        return new NotificacaoResponse(
                n.getIdNotificacao(),
                n.getIdDestinatario(),
                n.getCanal(),
                n.getTipoEvento(),
                n.getAssunto(),
                n.getMensagem(),
                n.getIdReferencia(),
                n.getStatus(),
                n.getDataCriacao(),
                n.getDataEnvio(),
                n.getDataLeitura());
    }
}

package com.fablab.notification.dto;

import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.NotificacaoHistorico;
import com.fablab.notification.entity.TipoEvento;
import java.time.LocalDateTime;

/** Representação de uma notificação no histórico. */
public record NotificacaoHistoricoResponse(
        Long idHistorico,
        Long idNotificacaoOriginal,
        Long idDestinatario,
        CanalNotificacao canal,
        TipoEvento tipoEvento,
        String assunto,
        String mensagem,
        Long idReferencia,
        LocalDateTime dataCriacao,
        LocalDateTime dataEnvio,
        LocalDateTime dataLeitura,
        LocalDateTime dataRevisaoAdmin,
        Long idAdminRevisor) {

    public static NotificacaoHistoricoResponse from(NotificacaoHistorico h) {
        return new NotificacaoHistoricoResponse(
                h.getIdHistorico(),
                h.getIdNotificacaoOriginal(),
                h.getIdDestinatario(),
                h.getCanal(),
                h.getTipoEvento(),
                h.getAssunto(),
                h.getMensagem(),
                h.getIdReferencia(),
                h.getDataCriacao(),
                h.getDataEnvio(),
                h.getDataLeitura(),
                h.getDataRevisaoAdmin(),
                h.getIdAdminRevisor());
    }
}

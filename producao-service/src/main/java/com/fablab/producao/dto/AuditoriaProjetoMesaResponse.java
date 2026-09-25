package com.fablab.producao.dto;

import com.fablab.producao.entity.AuditoriaProjetoMesa;
import com.fablab.producao.entity.StatusProjetoMesa;
import java.time.LocalDate;

/** Representação de uma auditoria de projeto de mesa. */
public record AuditoriaProjetoMesaResponse(
        Long idAuditoria,
        Long idProjetoMesa,
        LocalDate dataAuditoria,
        StatusProjetoMesa resultado,
        String acaoTomada,
        Long idAdminResponsavel) {

    public static AuditoriaProjetoMesaResponse from(AuditoriaProjetoMesa auditoria) {
        return new AuditoriaProjetoMesaResponse(
                auditoria.getIdAuditoria(),
                auditoria.getProjetoMesa().getIdProjetoMesa(),
                auditoria.getDataAuditoria(),
                auditoria.getResultado(),
                auditoria.getAcaoTomada(),
                auditoria.getIdAdminResponsavel());
    }
}
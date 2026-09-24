package com.fablab.rh.mapper;

import com.fablab.rh.dto.CertificadoAprovadoEvent;
import com.fablab.rh.dto.CertificadoEmitidoResponse;
import com.fablab.rh.dto.CertificadoRejeitadoEvent;
import com.fablab.rh.dto.CertificadoSolicitadoEvent;
import com.fablab.rh.dto.HoraConsolidadaResponse;
import com.fablab.rh.dto.SolicitacaoCertificadoResponse;
import com.fablab.rh.entity.CertificadoEmitido;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.HoraConsolidada;
import com.fablab.rh.entity.SolicitacaoCertificado;
import java.util.List;

/**
 * Mapeia solicitações e certificados de horas para DTOs e eventos.
 */
public final class CertificadoMapper {

    private CertificadoMapper() {
    }

    public static SolicitacaoCertificadoResponse toResponse(SolicitacaoCertificado solicitacao) {
        Funcionario admin = solicitacao.getIdAdminAprovador();
        return new SolicitacaoCertificadoResponse(
                solicitacao.getIdSolicitacao(),
                solicitacao.getFuncionario().getId(),
                solicitacao.getFuncionario().getPessoa().getNomeCompleto(),
                solicitacao.getTipoCertificado(),
                solicitacao.getDataSolicitacao(),
                solicitacao.getHorasSolicitadas(),
                solicitacao.getStatus(),
                admin == null ? null : admin.getId(),
                solicitacao.getDataDecisao(),
                solicitacao.getObservacao());
    }

    public static CertificadoEmitidoResponse toResponse(CertificadoEmitido certificado,
                                                        List<HoraConsolidada> horas) {
        List<HoraConsolidadaResponse> consolidacoes = horas.stream()
                .map(CertificadoMapper::toResponse)
                .toList();
        return new CertificadoEmitidoResponse(
                certificado.getIdCertificado(),
                certificado.getSolicitacao().getIdSolicitacao(),
                certificado.getFuncionario().getId(),
                certificado.getFuncionario().getPessoa().getNomeCompleto(),
                certificado.getTipoCertificado(),
                certificado.getHorasCertificadas(),
                certificado.getDataEmissao(),
                certificado.getCodigoVerificacao(),
                consolidacoes);
    }

    public static HoraConsolidadaResponse toResponse(HoraConsolidada consolidada) {
        return new HoraConsolidadaResponse(
                consolidada.getIdConsolidacao(),
                consolidada.getApontamento().getId(),
                consolidada.getApontamento().getData(),
                consolidada.getApontamento().getTipo(),
                consolidada.getHoras(),
                consolidada.getDataConsolidacao());
    }

    public static CertificadoSolicitadoEvent toSolicitadoEvent(SolicitacaoCertificado solicitacao) {
        return new CertificadoSolicitadoEvent(
                solicitacao.getIdSolicitacao(),
                solicitacao.getFuncionario().getId(),
                solicitacao.getTipoCertificado(),
                solicitacao.getHorasSolicitadas(),
                solicitacao.getDataSolicitacao());
    }

    public static CertificadoAprovadoEvent toAprovadoEvent(CertificadoEmitido certificado) {
        return new CertificadoAprovadoEvent(
                certificado.getSolicitacao().getIdSolicitacao(),
                certificado.getIdCertificado(),
                certificado.getFuncionario().getId(),
                certificado.getFuncionario().getPessoa().getNomeCompleto(),
                certificado.getTipoCertificado(),
                certificado.getHorasCertificadas(),
                certificado.getDataEmissao());
    }

    public static CertificadoRejeitadoEvent toRejeitadoEvent(SolicitacaoCertificado solicitacao) {
        return new CertificadoRejeitadoEvent(
                solicitacao.getIdSolicitacao(),
                solicitacao.getFuncionario().getId(),
                solicitacao.getFuncionario().getPessoa().getNomeCompleto(),
                solicitacao.getTipoCertificado(),
                solicitacao.getObservacao(),
                solicitacao.getDataDecisao());
    }
}
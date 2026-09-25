package com.fablab.rh.mapper;

import com.fablab.rh.dto.ApontamentoHorasRequest;
import com.fablab.rh.dto.ApontamentoHorasResponse;
import com.fablab.rh.dto.HorasValidadasEvent;
import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.StatusApontamento;

/**
 * Mapeia apontamentos de horas para DTOs e eventos.
 */
public final class ApontamentoHorasMapper {

    private ApontamentoHorasMapper() {
    }

    public static ApontamentoHoras toEntity(Funcionario funcionario, ApontamentoHorasRequest request,
                                            java.math.BigDecimal horasCalculadas) {
        ApontamentoHoras apontamento = new ApontamentoHoras();
        apontamento.setFuncionario(funcionario);
        apontamento.setTipo(request.tipo());
        apontamento.setIdReferencia(request.idReferencia());
        apontamento.setData(request.data());
        apontamento.setHorasTrabalhadas(horasCalculadas);
        apontamento.setHoraInicio(request.horaInicio());
        apontamento.setHoraFim(request.horaFim());
        apontamento.setDescricaoAtividade(request.descricaoAtividade());
        apontamento.setStatus(StatusApontamento.PENDENTE);
        return apontamento;
    }

    public static ApontamentoHorasResponse toResponse(ApontamentoHoras apontamento) {
        Funcionario validador = apontamento.getIdAdminValidador();
        return new ApontamentoHorasResponse(
                apontamento.getId(),
                apontamento.getFuncionario().getId(),
                apontamento.getTipo(),
                apontamento.getIdReferencia(),
                apontamento.getData(),
                apontamento.getHorasTrabalhadas(),
                apontamento.getHoraInicio(),
                apontamento.getHoraFim(),
                apontamento.getDescricaoAtividade(),
                apontamento.getStatus(),
                apontamento.getMotivoRejeicao(),
                validador == null ? null : validador.getId(),
                apontamento.getDataValidacao());
    }

    public static HorasValidadasEvent toValidatedEvent(ApontamentoHoras apontamento) {
        return new HorasValidadasEvent(
                apontamento.getFuncionario().getId(),
                apontamento.getTipo().name(),
                apontamento.getIdReferencia(),
                apontamento.getHorasTrabalhadas(),
                apontamento.getData());
    }
}
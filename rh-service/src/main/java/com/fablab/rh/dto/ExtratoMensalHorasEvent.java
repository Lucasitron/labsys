package com.fablab.rh.dto;

import java.math.BigDecimal;

/**
 * Evento emitido mensalmente com o extrato de horas de cada funcionário ativo —
 * consumido pelo Notification Service para enviar o e-mail com o resumo.
 *
 * @param idFuncionario     funcionário do extrato
 * @param nome              nome do funcionário (para o e-mail)
 * @param mesReferencia     mês de referência no formato {@code MM/yyyy}
 * @param horasPresenca     horas de presença do mês
 * @param horasEncomenda    horas validadas em encomendas no mês
 * @param horasProjeto      horas validadas em projetos no mês
 * @param horasDisponiveis  total de horas não consolidadas (disponíveis para certificado)
 */
public record ExtratoMensalHorasEvent(
        Long idFuncionario,
        String nome,
        String mesReferencia,
        BigDecimal horasPresenca,
        BigDecimal horasEncomenda,
        BigDecimal horasProjeto,
        BigDecimal horasDisponiveis) {
}
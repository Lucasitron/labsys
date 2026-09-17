package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.CustoEncomenda;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão do custo calculado de uma encomenda (Job Order Costing).
 */
public record CustoEncomendaResponse(
        Long idCusto,
        Long idEncomenda,
        BigDecimal custoMateriais,
        BigDecimal custoMaoObra,
        BigDecimal custoOverhead,
        BigDecimal custoTotal,
        BigDecimal valorVenda,
        BigDecimal margemLucro,
        LocalDate dataCalculo) {

    public static CustoEncomendaResponse of(CustoEncomenda custo) {
        return new CustoEncomendaResponse(
                custo.getIdCusto(),
                custo.getIdEncomenda(),
                custo.getCustoMateriais(),
                custo.getCustoMaoObra(),
                custo.getCustoOverhead(),
                custo.getCustoTotal(),
                custo.getValorVenda(),
                custo.getMargemLucro(),
                custo.getDataCalculo());
    }
}
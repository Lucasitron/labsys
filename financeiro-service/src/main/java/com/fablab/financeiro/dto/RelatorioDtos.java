package com.fablab.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTOs dos 6 relatórios de saúde financeira (shapes servidos — D-2). */
public final class RelatorioDtos {

    private RelatorioDtos() {
    }

    public record FluxoCaixaResponse(
            BigDecimal entradas,
            BigDecimal saidas,
            BigDecimal saldo,
            List<FluxoCaixaSemana> semanas) {
    }

    public record FluxoCaixaSemana(
            String semana,
            BigDecimal entradas,
            BigDecimal saidas,
            BigDecimal liquido) {
    }

    public record DreResponse(
            BigDecimal receitaOperacional,
            BigDecimal custosDiretos,
            BigDecimal maoDeObra,
            BigDecimal overhead,
            BigDecimal despesasOperacionais,
            BigDecimal doacoesRecursos,
            BigDecimal resultado) {
    }

    public record LucratividadeItemResponse(
            Integer idEncomenda,
            BigDecimal valorVenda,
            BigDecimal custoTotal,
            BigDecimal margem,
            BigDecimal margemPercentual) {
    }

    public record InadimplenciaItemResponse(
            Long idLancamento,
            String referencia,
            BigDecimal valor,
            LocalDate dataVencimento,
            long diasEmAtraso) {
    }

    public record DoacoesDespesasResponse(
            BigDecimal doacoes,
            BigDecimal recursosProjeto,
            BigDecimal despesas,
            BigDecimal saldo,
            BigDecimal coberturaPercentual,
            List<DoacoesDespesasMes> meses) {
    }

    public record DoacoesDespesasMes(
            String mes,
            BigDecimal doacoes,
            BigDecimal despesas,
            BigDecimal saldo) {
    }

    public record CustoMaquinaItemResponse(
            String idMaquina,
            BigDecimal custo,
            BigDecimal percentualTotal) {
    }
}

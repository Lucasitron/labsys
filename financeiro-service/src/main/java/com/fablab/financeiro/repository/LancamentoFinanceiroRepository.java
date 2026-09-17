package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de lançamentos financeiros (contas a pagar/receber).
 */
public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, Long> {

    List<LancamentoFinanceiro> findByStatus(StatusLancamento status);

    List<LancamentoFinanceiro> findByDataVencimentoBetween(LocalDate inicio, LocalDate fim);

    List<LancamentoFinanceiro> findByCategoria_IdCategoria(Long idCategoria);

    /**
     * Lançamentos vencidos e ainda não liquidados (Pendente ou em atraso).
     */
    List<LancamentoFinanceiro> findByStatusInAndDataVencimentoBefore(
            Collection<StatusLancamento> status, LocalDate data);

    /**
     * Lançamentos utilizados para compor o custo de materiais de uma encomenda
     * (saídas com {@code idReferenciaExterna} igual ao id da encomenda).
     */
    List<LancamentoFinanceiro> findByIdReferenciaExternaAndTipo(String idReferenciaExterna, TipoLancamento tipo);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM LancamentoFinanceiro l
            WHERE l.tipo = :tipo
              AND l.status <> com.fablab.financeiro.entity.StatusLancamento.CANCELADO
              AND (:inicio IS NULL OR l.dataVencimento >= :inicio)
              AND (:fim IS NULL OR l.dataVencimento <= :fim)
            """)
    BigDecimal somaValoresPorTipo(
            @Param("tipo") TipoLancamento tipo,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("""
            SELECT COALESCE(SUM(l.valor), 0)
            FROM LancamentoFinanceiro l
            WHERE l.tipo = :tipo
              AND l.idReferenciaExterna = :referencia
              AND l.status <> com.fablab.financeiro.entity.StatusLancamento.CANCELADO
            """)
    BigDecimal somaValoresPorTipoEReferencia(
            @Param("tipo") TipoLancamento tipo,
            @Param("referencia") String referencia);

    Optional<LancamentoFinanceiro> findFirstByIdReferenciaExternaAndTipo(
            String idReferenciaExterna, TipoLancamento tipo);
}
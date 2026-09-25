package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, Long> {

    /** Vencidos não liquidados: PENDENTE/ATRASADO com vencimento anterior à data. */
    List<LancamentoFinanceiro> findByStatusInAndDataVencimentoBefore(
            Collection<StatusLancamento> status, LocalDate data);

    /** Soma de saídas não canceladas por referência externa (materiais do custeio). */
    @Query("""
            SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFinanceiro l
            WHERE l.tipo = :tipo AND l.status <> 'CANCELADO'
              AND l.idReferenciaExterna = :referencia
            """)
    BigDecimal somarPorTipoEReferencia(@Param("tipo") TipoLancamento tipo,
                                       @Param("referencia") String referencia);

    /** Soma de valores por tipo no período de vencimento (relatórios). */
    @Query("""
            SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFinanceiro l
            WHERE l.tipo = :tipo AND l.status <> 'CANCELADO'
              AND (:inicio IS NULL OR l.dataVencimento >= :inicio)
              AND (:fim IS NULL OR l.dataVencimento <= :fim)
            """)
    BigDecimal somarPorTipoEPeriodo(@Param("tipo") TipoLancamento tipo,
                                    @Param("inicio") LocalDate inicio,
                                    @Param("fim") LocalDate fim);

    List<LancamentoFinanceiro> findByStatus(StatusLancamento status);

    List<LancamentoFinanceiro> findByIdCategoria(Long idCategoria);

    List<LancamentoFinanceiro> findByTipoAndStatus(TipoLancamento tipo, StatusLancamento status);
}

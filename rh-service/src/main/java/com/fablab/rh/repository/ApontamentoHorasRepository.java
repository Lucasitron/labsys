package com.fablab.rh.repository;

import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.entity.TipoApontamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso aos dados da tabela {@code apontamento_horas}.
 */
public interface ApontamentoHorasRepository extends JpaRepository<ApontamentoHoras, Long> {

    List<ApontamentoHoras> findByFuncionarioId(Long funcionarioId);

    List<ApontamentoHoras> findByFuncionarioIdAndStatusAndConsolidadoFalse(Long funcionarioId,
                                                                           StatusApontamento status);

    long countByFuncionarioIdAndStatus(Long funcionarioId, StatusApontamento status);

    /** Lista com filtros opcionais de funcionário, status e período. */
    @Query("""
            select a from ApontamentoHoras a
            where (:funcionarioId is null or a.funcionario.id = :funcionarioId)
              and (:status is null or a.status = :status)
              and (:inicio is null or a.data between :inicio and :fim)
            order by a.data desc, a.id desc
            """)
    List<ApontamentoHoras> buscarComFiltros(@Param("funcionarioId") Long funcionarioId,
                                            @Param("status") StatusApontamento status,
                                            @Param("inicio") LocalDate inicio,
                                            @Param("fim") LocalDate fim);

    @Query("""
            select coalesce(sum(a.horasTrabalhadas), 0)
            from ApontamentoHoras a
            where a.funcionario.id = :funcionarioId
              and a.data = :data
              and a.status in (com.fablab.rh.entity.StatusApontamento.PENDENTE,
                               com.fablab.rh.entity.StatusApontamento.VALIDADO)
            """)
    BigDecimal sumHorasApontadasPorDia(@Param("funcionarioId") Long funcionarioId, @Param("data") LocalDate data);

    @Query("""
            select coalesce(sum(a.horasTrabalhadas), 0)
            from ApontamentoHoras a
            where a.funcionario.id = :funcionarioId
              and a.status = com.fablab.rh.entity.StatusApontamento.VALIDADO
              and a.consolidado = false
            """)
    BigDecimal sumHorasDisponiveis(@Param("funcionarioId") Long funcionarioId);

    @Query("""
            select coalesce(sum(a.horasTrabalhadas), 0)
            from ApontamentoHoras a
            where a.funcionario.id = :funcionarioId
              and a.status = com.fablab.rh.entity.StatusApontamento.VALIDADO
              and a.tipo = :tipo
              and a.data between :inicio and :fim
            """)
    BigDecimal sumHorasValidadasPorTipoEPeriodo(@Param("funcionarioId") Long funcionarioId,
                                                @Param("tipo") TipoApontamento tipo,
                                                @Param("inicio") LocalDate inicio,
                                                @Param("fim") LocalDate fim);
}
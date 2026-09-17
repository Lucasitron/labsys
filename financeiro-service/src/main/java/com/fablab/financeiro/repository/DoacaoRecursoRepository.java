package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.TipoDoacaoRecurso;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de doações e recursos de projetos.
 */
public interface DoacaoRecursoRepository extends JpaRepository<DoacaoRecurso, Long> {

    List<DoacaoRecurso> findByDataRecebimentoBetween(LocalDate inicio, LocalDate fim);

    @Query("""
            SELECT COALESCE(SUM(d.valor), 0)
            FROM DoacaoRecurso d
            WHERE (:tipo IS NULL OR d.tipo = :tipo)
              AND (:inicio IS NULL OR d.dataRecebimento >= :inicio)
              AND (:fim IS NULL OR d.dataRecebimento <= :fim)
            """)
    BigDecimal somarValores(
            @Param("tipo") TipoDoacaoRecurso tipo,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);
}
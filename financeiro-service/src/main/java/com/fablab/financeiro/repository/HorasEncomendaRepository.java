package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.HorasEncomenda;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de horas validadas acumuladas por encomenda.
 */
public interface HorasEncomendaRepository extends JpaRepository<HorasEncomenda, Long> {

    List<HorasEncomenda> findAllByIdEncomenda(Long idEncomenda);

    @Query("""
            SELECT COALESCE(SUM(h.horas), 0)
            FROM HorasEncomenda h
            WHERE h.idEncomenda = :idEncomenda
            """)
    Optional<BigDecimal> somaHorasPorEncomenda(@Param("idEncomenda") Long idEncomenda);

    Optional<HorasEncomenda> findFirstByIdEncomendaAndIdFuncionarioAndDataRegistro(
            Long idEncomenda, Long idFuncionario, java.time.LocalDate dataRegistro);

    boolean existsByIdEncomendaAndIdFuncionarioAndDataRegistro(Long idEncomenda, Long idFuncionario,
                                                              java.time.LocalDate dataRegistro);
}
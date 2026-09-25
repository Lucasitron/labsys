package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.HorasEncomenda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HorasEncomendaRepository extends JpaRepository<HorasEncomenda, Long> {

    Optional<HorasEncomenda> findByIdEncomendaAndIdFuncionarioAndDataRegistro(
            Integer idEncomenda, Integer idFuncionario, LocalDate dataRegistro);

    List<HorasEncomenda> findByIdEncomenda(Integer idEncomenda);

    /** Soma de horas validadas da encomenda. */
    @Query("""
            SELECT COALESCE(SUM(h.horas), 0) FROM HorasEncomenda h
            WHERE h.idEncomenda = :idEncomenda
            """)
    BigDecimal somarHorasPorEncomenda(@Param("idEncomenda") Integer idEncomenda);
}

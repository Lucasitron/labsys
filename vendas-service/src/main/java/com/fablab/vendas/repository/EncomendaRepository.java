package com.fablab.vendas.repository;

import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.StatusKanban;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de encomendas.
 */
public interface EncomendaRepository extends JpaRepository<Encomenda, Long> {

    List<Encomenda> findByCliente_Id(Long idCliente);

    List<Encomenda> findByStatusKanban(StatusKanban statusKanban);

    List<Encomenda> findByDataCriacaoBetween(LocalDate inicio, LocalDate fim);
}
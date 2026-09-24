package com.fablab.vendas.repository;

import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.StatusKanban;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório de encomendas com filtros por status, cliente e orçamento. */
public interface EncomendaRepository extends JpaRepository<Encomenda, Long> {

    List<Encomenda> findByStatusKanban(StatusKanban statusKanban);

    List<Encomenda> findByIdCliente(Long idCliente);

    Optional<Encomenda> findByIdOrcamento(Long idOrcamento);

    boolean existsByIdOrcamento(Long idOrcamento);

    boolean existsByIdCliente(Long idCliente);

    long countByStatusKanban(StatusKanban statusKanban);

    /** Leitura com lock pessimista para movimentações críticas do Kanban. */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Encomenda e WHERE e.id = :id")
    Optional<Encomenda> findByIdWithLock(@Param("id") Long id);
}

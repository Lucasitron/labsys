package com.fablab.vendas.repository;

import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusOrcamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório de orçamentos com filtros por cliente e status. */
public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findByIdCliente(Long idCliente);

    List<Orcamento> findByStatus(StatusOrcamento status);

    List<Orcamento> findByIdClienteAndStatus(Long idCliente, StatusOrcamento status);

    long countByStatus(StatusOrcamento status);
}

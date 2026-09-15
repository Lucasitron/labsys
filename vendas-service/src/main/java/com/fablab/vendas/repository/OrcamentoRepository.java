package com.fablab.vendas.repository;

import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusOrcamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de orçamentos.
 */
public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findByCliente_Id(Long idCliente);

    List<Orcamento> findByStatus(StatusOrcamento status);
}
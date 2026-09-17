package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.SolicitacaoCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de solicitações de compra.
 */
public interface SolicitacaoCompraRepository extends JpaRepository<SolicitacaoCompra, Long> {

    List<SolicitacaoCompra> findAllByOrderByDataSolicitacaoDesc();
}
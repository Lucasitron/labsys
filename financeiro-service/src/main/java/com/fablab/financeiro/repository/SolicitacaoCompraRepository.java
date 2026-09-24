package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.SolicitacaoCompra;
import com.fablab.financeiro.entity.StatusCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoCompraRepository extends JpaRepository<SolicitacaoCompra, Long> {

    List<SolicitacaoCompra> findByStatus(StatusCompra status);
}

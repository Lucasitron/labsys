package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.CustoEncomenda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de custo calculado de encomendas.
 */
public interface CustoEncomendaRepository extends JpaRepository<CustoEncomenda, Long> {

    List<CustoEncomenda> findAllByIdEncomendaOrderByDataCalculoDesc(Long idEncomenda);

    Optional<CustoEncomenda> findFirstByIdEncomendaOrderByDataCalculoDesc(Long idEncomenda);

    List<CustoEncomenda> findAllByOrderByDataCalculoDesc();
}
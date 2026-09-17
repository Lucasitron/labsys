package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.FechamentoEncomenda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de fechamentos de encomenda.
 */
public interface FechamentoEncomendaRepository extends JpaRepository<FechamentoEncomenda, Long> {

    Optional<FechamentoEncomenda> findByIdEncomenda(Long idEncomenda);

    boolean existsByIdEncomenda(Long idEncomenda);

    List<FechamentoEncomenda> findAllByStatus(com.fablab.financeiro.entity.StatusFechamentoEncomenda status);
}
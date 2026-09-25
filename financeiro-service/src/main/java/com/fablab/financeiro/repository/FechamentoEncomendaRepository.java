package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.FechamentoEncomenda;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FechamentoEncomendaRepository extends JpaRepository<FechamentoEncomenda, Long> {

    Optional<FechamentoEncomenda> findByIdEncomenda(Integer idEncomenda);

    boolean existsByIdEncomenda(Integer idEncomenda);
}

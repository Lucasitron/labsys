package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.CustoEncomenda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustoEncomendaRepository extends JpaRepository<CustoEncomenda, Long> {

    /** Último cálculo da encomenda (mais recente). */
    Optional<CustoEncomenda> findFirstByIdEncomendaOrderByDataCalculoDescIdDesc(Integer idEncomenda);

    List<CustoEncomenda> findByIdEncomendaOrderByDataCalculoDesc(Integer idEncomenda);
}

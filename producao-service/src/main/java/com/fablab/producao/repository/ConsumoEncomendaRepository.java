package com.fablab.producao.repository;

import com.fablab.producao.entity.ConsumoEncomenda;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumoEncomendaRepository extends JpaRepository<ConsumoEncomenda, Long> {

    List<ConsumoEncomenda> findByIdEncomenda(Long idEncomenda);
}
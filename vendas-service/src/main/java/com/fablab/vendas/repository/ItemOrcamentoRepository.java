package com.fablab.vendas.repository;

import com.fablab.vendas.entity.ItemOrcamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório de itens de orçamento. */
public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {

    List<ItemOrcamento> findByIdOrcamento(Long idOrcamento);

    void deleteByIdOrcamento(Long idOrcamento);
}

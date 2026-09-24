package com.fablab.vendas.repository;

import com.fablab.vendas.entity.HistoricoStatusEncomenda;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do histórico de movimentações do Kanban. */
public interface HistoricoStatusEncomendaRepository extends JpaRepository<HistoricoStatusEncomenda, Long> {

    List<HistoricoStatusEncomenda> findByIdEncomendaOrderByDataAlteracaoAsc(Long idEncomenda);
}

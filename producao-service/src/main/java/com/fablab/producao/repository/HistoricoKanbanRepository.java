package com.fablab.producao.repository;

import com.fablab.producao.entity.HistoricoKanban;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoKanbanRepository extends JpaRepository<HistoricoKanban, Long> {

    List<HistoricoKanban> findByIdEncomendaOrderByDataAlteracaoDesc(Long idEncomenda);
}
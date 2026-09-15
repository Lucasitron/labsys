package com.fablab.vendas.repository;

import com.fablab.vendas.entity.StatusTarefa;
import com.fablab.vendas.entity.TarefaMarketing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de tarefas de marketing.
 */
public interface TarefaMarketingRepository extends JpaRepository<TarefaMarketing, Long> {

    List<TarefaMarketing> findByIdResponsavel(Long idResponsavel);

    List<TarefaMarketing> findByStatus(StatusTarefa status);
}
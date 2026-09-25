package com.fablab.vendas.repository;

import com.fablab.vendas.entity.TarefaMarketing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório de tarefas de marketing com filtros por responsável e status. */
public interface TarefaMarketingRepository extends JpaRepository<TarefaMarketing, Long> {

    List<TarefaMarketing> findByIdResponsavel(Long idResponsavel);

    List<TarefaMarketing> findByStatus(String status);

    List<TarefaMarketing> findByIdResponsavelAndStatus(Long idResponsavel, String status);

    long countByStatus(String status);
}

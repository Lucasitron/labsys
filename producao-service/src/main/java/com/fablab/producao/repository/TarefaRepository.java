package com.fablab.producao.repository;

import com.fablab.producao.entity.Tarefa;
import com.fablab.producao.entity.TarefaStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findByProjeto_IdProjeto(Long idProjeto);

    List<Tarefa> findByIdResponsavel(Long idResponsavel);

    List<Tarefa> findByStatus(TarefaStatus status);
}
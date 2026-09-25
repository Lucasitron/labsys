package com.fablab.producao.repository;

import com.fablab.producao.entity.EncomendaKanban;
import com.fablab.producao.entity.KanbanStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncomendaKanbanRepository extends JpaRepository<EncomendaKanban, Long> {

    Optional<EncomendaKanban> findByIdEncomenda(Long idEncomenda);

    boolean existsByIdEncomenda(Long idEncomenda);

    List<EncomendaKanban> findByStatusOrderByOrdemAsc(KanbanStatus status);

    List<EncomendaKanban> findAllByOrderByStatusAscOrdemAsc();
}
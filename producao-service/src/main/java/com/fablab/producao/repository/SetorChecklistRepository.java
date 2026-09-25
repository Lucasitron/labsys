package com.fablab.producao.repository;

import com.fablab.producao.entity.SetorChecklist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorChecklistRepository extends JpaRepository<SetorChecklist, Long> {

    List<SetorChecklist> findBySetor_IdSetor(Long idSetor);

    List<SetorChecklist> findBySetor_IdSetorAndAtivoTrue(Long idSetor);
}
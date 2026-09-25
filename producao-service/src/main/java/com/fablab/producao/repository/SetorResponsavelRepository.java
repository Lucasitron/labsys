package com.fablab.producao.repository;

import com.fablab.producao.entity.SetorResponsavel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorResponsavelRepository extends JpaRepository<SetorResponsavel, Long> {

    List<SetorResponsavel> findBySetor_IdSetor(Long idSetor);

    Optional<SetorResponsavel> findFirstBySetor_IdSetorAndAtivoTrue(Long idSetor);
}
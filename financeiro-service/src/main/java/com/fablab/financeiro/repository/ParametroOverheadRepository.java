package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.ParametroOverhead;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroOverheadRepository extends JpaRepository<ParametroOverhead, Long> {

    /** Taxa vigente: registro com maior data de vigência. */
    Optional<ParametroOverhead> findFirstByOrderByDataVigenciaDesc();
}

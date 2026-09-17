package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.ParametroOverhead;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de parâmetros de overhead.
 */
public interface ParametroOverheadRepository extends JpaRepository<ParametroOverhead, Long> {

    Optional<ParametroOverhead> findTopByOrderByDataVigenciaDesc();
}
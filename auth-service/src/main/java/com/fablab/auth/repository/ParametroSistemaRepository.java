package com.fablab.auth.repository;

import com.fablab.auth.entity.ParametroSistema;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * KV de parâmetros globais do sistema.
 */
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, String> {
}

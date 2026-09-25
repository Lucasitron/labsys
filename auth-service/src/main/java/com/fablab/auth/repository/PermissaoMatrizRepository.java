package com.fablab.auth.repository;

import com.fablab.auth.entity.PermissaoMatriz;
import com.fablab.auth.entity.PermissaoMatrizId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code permissao_matriz}.
 */
public interface PermissaoMatrizRepository extends JpaRepository<PermissaoMatriz, PermissaoMatrizId> {
}

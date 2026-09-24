package com.fablab.auth.repository;

import com.fablab.auth.entity.Login;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code login}.
 */
public interface LoginRepository extends JpaRepository<Login, Long> {

    Optional<Login> findByEmail(String email);

    Optional<Login> findByNomeUsuario(String nomeUsuario);

    Optional<Login> findByUuid(String uuid);

    boolean existsByEmail(String email);

    boolean existsByNomeUsuario(String nomeUsuario);
}
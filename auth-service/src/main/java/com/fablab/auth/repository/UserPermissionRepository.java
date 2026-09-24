package com.fablab.auth.repository;

import com.fablab.auth.entity.UserPermission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code user_permissions}.
 */
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByIdUser(Long idUser);

    Optional<UserPermission> findFirstByIdUserAndActiveTrue(Long idUser);
}
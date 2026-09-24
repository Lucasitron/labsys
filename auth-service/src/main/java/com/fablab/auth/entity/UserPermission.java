package com.fablab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Permissão (papel RBAC) atribuída a um usuário.
 *
 * <p>Corresponde à tabela {@code user_permissions}. O campo {@code role} é persistido
 * como inteiro (0-4) conforme {@link RoleConverter}.</p>
 */
@Entity
@Table(name = "user_permissions", indexes = {
        @Index(name = "ix_user_permissions_id_user", columnList = "id_user")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "id_user", nullable = false)
    private Long idUser;

    @Convert(converter = RoleConverter.class)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active;
}
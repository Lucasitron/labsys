package com.fablab.auth.dto;

/**
 * Permissão (papel RBAC) de um usuário.
 */
public record PermissionDto(
        String role,
        String label,
        boolean active) {
}
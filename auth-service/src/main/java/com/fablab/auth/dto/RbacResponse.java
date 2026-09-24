package com.fablab.auth.dto;

import java.util.List;

/**
 * Matriz de permissões RBAC para um papel (consumida pelos demais serviços).
 */
public record RbacResponse(
        String role,
        int code,
        String label,
        List<String> permissions) {
}
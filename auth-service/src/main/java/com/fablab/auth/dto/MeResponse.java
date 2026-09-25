package com.fablab.auth.dto;

import java.util.List;

/**
 * Dados do usuário autenticado e suas permissões.
 */
public record MeResponse(
        Long id,
        Long idUser,
        String email,
        String nomeUsuario,
        String setor,
        List<PermissionDto> permissions) {
}
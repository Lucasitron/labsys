package com.fablab.auth.mapper;

import com.fablab.auth.dto.MeResponse;
import com.fablab.auth.dto.PermissionDto;
import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.UserPermission;
import java.util.List;

/**
 * Mapeia entidades do domínio para DTOs de resposta.
 */
public final class LoginMapper {

    private LoginMapper() {
    }

    public static MeResponse toMeResponse(Login login, List<UserPermission> permissions) {
        List<PermissionDto> perms = permissions.stream()
                .map(p -> new PermissionDto(p.getRole().name(), p.getRole().getLabel(), p.isActive()))
                .toList();
        return new MeResponse(
                login.getId(),
                login.getIdUser(),
                login.getEmail(),
                login.getNomeUsuario(),
                login.getSetor(),
                perms);
    }
}
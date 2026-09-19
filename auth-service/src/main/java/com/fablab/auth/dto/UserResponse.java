package com.fablab.auth.dto;

import com.fablab.auth.entity.Usuario;
import com.fablab.auth.service.AcessoHelper;
import java.util.List;
import java.util.Map;

/**
 * Perfil do usuário autenticado devolvido para o front.
 *
 * @param id               id do usuário (carregado no claim {@code id_user})
 * @param username         usuário de acesso
 * @param name             nome completo
 * @param email            e-mail
 * @param role             nível de acesso (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário, 4-Recrutando)
 * @param roles            acesso por módulo ({@code view}/{@code edit}/{@code null})
 * @param responsibilities recursos (ex.: equipamentos) dos quais o usuário é responsável, por módulo
 */
public record UserResponse(
        Long id,
        String username,
        String name,
        String email,
        Integer role,
        Map<String, String> roles,
        Map<String, List<String>> responsibilities) {

    public static UserResponse of(Usuario usuario, Map<String, List<String>> responsibilities) {
        return new UserResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNomeCompleto(),
                usuario.getEmail(),
                usuario.getRole(),
                AcessoHelper.acessosPorRole(usuario.getRole()),
                responsibilities);
    }
}
package com.fablab.rh.dto;

import java.util.List;

/**
 * Matriz de níveis de acesso (tela Níveis & acesso).
 *
 * @param niveis     níveis com contagem de membros
 * @param permissoes capacidades por recurso e nível
 */
public record NiveisResponse(
        List<NivelResponse> niveis,
        List<PermissaoNivelResponse> permissoes) {
}

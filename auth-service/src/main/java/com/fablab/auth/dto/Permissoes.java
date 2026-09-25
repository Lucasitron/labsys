package com.fablab.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Contratos da matriz RBAC ({@code /api/permissoes}).
 */
public final class Permissoes {

    private Permissoes() {
    }

    public record CelulaPermissao(
            String modulo,
            String nivel,
            Integer nivelCodigo,
            String valor) {
    }

    public record MatrizPermissoesResponse(
            /** Matriz por papel reaproveitada do RbacService (sem recalcular). */
            List<RbacResponse> roles,
            /** Matriz célula (módulo × nível) editável. */
            List<CelulaPermissao> matriz,
            /** Módulos e valores válidos (para validação no client). */
            Map<String, List<String>> enums) {
    }

    public record AtualizarPermissaoRequest(

            @NotBlank(message = "valor da permissão é obrigatório (Ver, Editar ou Nenhum)")
            String valor) {
    }
}

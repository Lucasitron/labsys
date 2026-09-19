package com.fablab.auth.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Derivado do RBAC do Pessoas &amp; RH: a partir do código de nível do usuário
 * calcula o acesso por módulo ({@code view}/{@code edit}/{@code null}), seguindo
 * as mesmas regras aplicadas no frontend (não duplica lógica em runtime, apenas
 * espelha o contrato no MVP).
 */
public final class AcessoHelper {

    private static final List<String> MODULES = List.of(
            "dashboard", "rh", "estoque", "vendas", "financeiro", "producao", "notificacoes", "configuracoes");

    private AcessoHelper() {
    }

    /**
     * Acessos por módulo derivados do nível.
     *
     * <ul>
     *   <li>Admin (0): acesso de edição em todos os módulos.</li>
     *   <li>Bolsista (1)/Voluntário (2): edição em RH, visualização nos demais.</li>
     *   <li>Estagiário (3): visualização em RH/estoque/vendas/produção.</li>
     *   <li>Recrutando (4): apenas visualização de módulos de leitura.</li>
     *   <li>Financeiro: somente Admin.</li>
     * </ul>
     */
    public static Map<String, String> acessosPorRole(Integer role) {
        Map<String, String> acessos = new LinkedHashMap<>();
        for (String modulo : MODULES) {
            acessos.put(modulo, acesso(modulo, role));
        }
        return acessos;
    }

    private static String acesso(String modulo, Integer role) {
        if (role == null) {
            return null;
        }
        if (role == 0) {
            return "edit";
        }
        return switch (modulo) {
            case "rh" -> role <= 2 ? "edit" : "view";
            case "estoque", "vendas", "producao" -> role <= 3 ? "view" : null;
            case "financeiro" -> null;
            default -> "view";
        };
    }
}
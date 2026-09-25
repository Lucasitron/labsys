package com.fablab.auth.entity;

/**
 * Módulos do sistema na matriz RBAC de Configurações/Permissões.
 *
 * <p>Espelha {@code Module} do frontend ({@code src/lib/types/auth.ts}):
 * dashboard, rh, estoque, vendas, financeiro, producao, notificacoes,
 * configuracoes.</p>
 */
public enum Modulo {

    DASHBOARD("dashboard"),
    RH("rh"),
    ESTOQUE("estoque"),
    VENDAS("vendas"),
    FINANCEIRO("financeiro"),
    PRODUCAO("producao"),
    NOTIFICACOES("notificacoes"),
    CONFIGURACOES("configuracoes");

    private final String code;

    Modulo(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * Converte texto (nome ou código) em módulo.
     *
     * @throws IllegalArgumentException com mensagem PT se inválido
     */
    public static Modulo parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Módulo inválido: informe dashboard, rh, estoque, vendas, financeiro, producao, notificacoes ou configuracoes");
        }
        String normalized = value.trim();
        for (Modulo modulo : values()) {
            if (modulo.name().equalsIgnoreCase(normalized) || modulo.code.equalsIgnoreCase(normalized)) {
                return modulo;
            }
        }
        throw new IllegalArgumentException(
                "Módulo inválido: informe dashboard, rh, estoque, vendas, financeiro, producao, notificacoes ou configuracoes");
    }
}

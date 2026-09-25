package com.fablab.auth.entity;

/**
 * Nível de acesso de uma célula da matriz RBAC (módulo × nível).
 *
 * <p>Espelha {@code PermissaoNivel} do frontend ({@code 'Ver'|'Editar'|'Nenhum'}).</p>
 */
public enum PermissaoNivel {

    VER("Ver"),
    EDITAR("Editar"),
    NENHUM("Nenhum");

    private final String label;

    PermissaoNivel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Converte texto (nome ou rótulo PT) em nível de permissão.
     *
     * @throws IllegalArgumentException com mensagem PT se inválido
     */
    public static PermissaoNivel parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Permissão inválida: informe Ver, Editar ou Nenhum");
        }
        String normalized = value.trim();
        for (PermissaoNivel nivel : values()) {
            if (nivel.name().equalsIgnoreCase(normalized) || nivel.label.equalsIgnoreCase(normalized)) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("Permissão inválida: informe Ver, Editar ou Nenhum");
    }
}

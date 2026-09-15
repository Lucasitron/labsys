package com.fablab.rh.entity;

/**
 * Nível de acesso do funcionário (RBAC) — espelha os códigos do Auth Service
 * (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário, 4-Recrutando).
 */
public enum NivelAcesso {

    ADMIN(0, "Admin"),
    BOLSISTA(1, "Bolsista"),
    VOLUNTARIO(2, "Voluntário"),
    ESTAGIARIO(3, "Estagiário"),
    RECRUTANDO(4, "Recrutando");

    private final int code;
    private final String label;

    NivelAcesso(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static NivelAcesso fromCode(int code) {
        for (NivelAcesso nivel : values()) {
            if (nivel.code == code) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("Nível de acesso inválido: " + code);
    }
}
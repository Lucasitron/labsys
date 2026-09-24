package com.fablab.auth.entity;

/**
 * Níveis de acesso do sistema (RBAC).
 *
 * <p>Os códigos seguem a documentação técnica: 0-Admin, 1-Bolsista, 2-Voluntário,
 * 3-Estagiário, 4-Recrutando.</p>
 */
public enum Role {

    ADMIN(0, "Admin"),
    BOLSISTA(1, "Bolsista"),
    VOLUNTARIO(2, "Voluntário"),
    ESTAGIARIO(3, "Estagiário"),
    RECRUTANDO(4, "Recrutando");

    private final int code;
    private final String label;

    Role(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static Role fromCode(int code) {
        for (Role role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("Papel inválido: " + code);
    }
}
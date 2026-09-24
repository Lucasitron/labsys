package com.fablab.auth.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Chave composta da matriz de permissões (módulo × papel).
 */
public class PermissaoMatrizId implements Serializable {

    private String modulo;
    private Role role;

    public PermissaoMatrizId() {
    }

    public PermissaoMatrizId(String modulo, Role role) {
        this.modulo = modulo;
        this.role = role;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PermissaoMatrizId that)) {
            return false;
        }
        return Objects.equals(modulo, that.modulo) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(modulo, role);
    }
}

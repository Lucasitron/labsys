package com.fablab.rh.entity;

/**
 * Status da pessoa no laboratório (0-Ativo, 1-Inativo, 2-Recrutando).
 */
public enum PessoaStatus {

    ATIVO(0),
    INATIVO(1),
    RECRUTANDO(2);

    private final int code;

    PessoaStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
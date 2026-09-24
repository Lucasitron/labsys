package com.fablab.producao.entity;

/** Níveis de acesso do Fab Lab (Matriz de Permissões). */
public enum NivelAcesso {
    ADMIN(0),
    BOLSISTA(1),
    VOLUNTARIO(2),
    ESTAGIARIO(3),
    RECRUTANDO(4);

    private final int codigo;

    NivelAcesso(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
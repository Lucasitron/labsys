package com.fablab.financeiro.entity;

/**
 * Níveis de acesso do Fab Lab (Matriz de Permissões).
 *
 * <p>Ordem/valor numérico: Admin (0) &gt; Bolsista (1) &gt; Voluntário (2)
 * &gt; Estagiário (3) &gt; Recrutando (4). Os nomes são usados como claims
 * {@code role} dos tokens emitidos pelo Auth &amp; Identity Service.</p>
 */
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

    public static NivelAcesso doCodigo(Integer codigo) {
        if (codigo == null) {
            return null;
        }
        for (NivelAcesso nivel : values()) {
            if (nivel.codigo == codigo) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("Nível de acesso inválido: " + codigo);
    }
}

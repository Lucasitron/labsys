package com.fablab.auth.entity;

/**
 * Situação da conta de acesso (gestão de contas em Configurações/Usuários).
 *
 * <p>Não confundir com cadastro físico de pessoas (domínio do
 * {@code rh-service}): aqui opera-se apenas conta/nível/situação.</p>
 */
public enum SituacaoUsuario {

    ATIVO("Ativo"),
    PENDENTE("Pendente"),
    DESATIVADO("Desativado");

    private final String label;

    SituacaoUsuario(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Converte texto (nome ou rótulo PT) em situação.
     *
     * @throws IllegalArgumentException com mensagem PT se inválido
     */
    public static SituacaoUsuario parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Situação inválida: informe Ativo, Pendente ou Desativado");
        }
        String normalized = value.trim();
        for (SituacaoUsuario situacao : values()) {
            if (situacao.name().equalsIgnoreCase(normalized) || situacao.label.equalsIgnoreCase(normalized)) {
                return situacao;
            }
        }
        throw new IllegalArgumentException("Situação inválida: informe Ativo, Pendente ou Desativado");
    }
}

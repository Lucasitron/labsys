package com.fablab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Célula persistente da matriz de permissões (módulo × papel).
 *
 * <p>Corresponde à tabela {@code permissao_matriz}. Substitui o mapa em memória do
 * {@code RbacService}: {@code PUT /api/permissoes} agora sobrevive ao restart.</p>
 */
@Entity
@Table(name = "permissao_matriz")
@IdClass(PermissaoMatrizId.class)
@Getter
@Setter
@NoArgsConstructor
public class PermissaoMatriz {

    @Id
    @Column(name = "modulo", nullable = false, length = 64, columnDefinition = "VARCHAR(64)")
    private String modulo;

    @Id
    @Convert(converter = RoleConverter.class)
    @Column(name = "role", nullable = false, columnDefinition = "INTEGER")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 16, columnDefinition = "VARCHAR(16)")
    private PermissaoNivel nivel;

    public PermissaoMatriz(String modulo, Role role, PermissaoNivel nivel) {
        this.modulo = modulo;
        this.role = role;
        this.nivel = nivel;
    }
}

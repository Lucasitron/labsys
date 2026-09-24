package com.fablab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credenciais de acesso e identidade do usuário.
 *
 * <p>Corresponde à tabela {@code login}. O campo {@code idUser} referencia o usuário
 * no Pessoas &amp; RH Service, enquanto {@code uuid} representa o cartão RFID.</p>
 */
@Entity
@Table(name = "login", uniqueConstraints = {
        @UniqueConstraint(name = "uk_login_uuid", columnNames = "uuid"),
        @UniqueConstraint(name = "uk_login_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_login_nome_usuario", columnNames = "nome_usuario")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Login {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "id_user", nullable = false)
    private Long idUser;

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "nome_usuario", nullable = false, unique = true)
    private String nomeUsuario;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "setor")
    private String setor;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private SituacaoUsuario situacao = SituacaoUsuario.ATIVO;
}
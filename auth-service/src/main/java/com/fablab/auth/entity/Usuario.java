package com.fablab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuário autenticável do sistema.
 *
 * <p>O {@code id} é um Long e o JWT carrega o claim {@code id_user} com esse
 * valor, mantendo compatibilidade com o restante da malha (os microsserviços
 * interpretam {@code id_user} como um longo).</p>
 *
 * <p>Corresponde à tabela {@code usuario}. O {@code role} espelha o código do
 * nível de acesso do Pessoas &amp; RH (0-Admin, 1-Bolsista, 2-Voluntário,
 * 3-Estagiário, 4-Recrutando).</p>
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nome_completo", nullable = false, length = 160)
    private String nomeCompleto;

    @Column(name = "email", nullable = false, length = 160)
    private String email;

    @Column(name = "role", nullable = false)
    private Integer role;

    @Column(name = "setor", length = 64)
    private String setor;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}
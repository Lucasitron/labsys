package com.fablab.producao.entity;

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

/** Parâmetro configurável do sistema 5S. */
@Entity
@Table(name = "parametro_5s")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Parametro5S {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idParametro;

    @Column(name = "chave", nullable = false, unique = true, length = 100)
    private String chave;

    @Column(name = "valor", nullable = false, length = 255)
    private String valor;

    @Column(name = "descricao", length = 500)
    private String descricao;
}
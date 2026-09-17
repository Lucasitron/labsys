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

/** Setor avaliado pelo programa 5S. */
@Entity
@Table(name = "setor")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idSetor;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "descricao", length = 1000)
    private String descricao;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "foto_correto_url", length = 500)
    private String fotoCorretoUrl;

    @Column(name = "foto_incorreto_url", length = 500)
    private String fotoIncorretoUrl;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}
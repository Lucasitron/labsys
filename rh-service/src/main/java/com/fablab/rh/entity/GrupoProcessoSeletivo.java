package com.fablab.rh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Grupo de candidatos do processo seletivo (ex.: "Eletrônica"), liderado por
 * um tutor responsável.
 *
 * <p>Corresponde à tabela {@code grupo_processo_seletivo}.</p>
 */
@Entity
@Table(name = "grupo_processo_seletivo")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GrupoProcessoSeletivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tutor_lider", nullable = false)
    private Funcionario lider;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa", nullable = false, length = 32)
    private StatusProcesso etapa = StatusProcesso.INSCRITO;
}

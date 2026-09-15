package com.fablab.rh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Professor/monitor responsável por tutorar candidatos e treinamentos.
 *
 * <p>Corresponde à tabela {@code tutor}. {@code qualificacao} varia de 0 a 6.</p>
 */
@Entity
@Table(name = "tutor", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tutor_funcionario", columnNames = "id_funcionario")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Column(name = "turno")
    private String turno;

    @Column(name = "qualificacao")
    private Integer qualificacao;
}
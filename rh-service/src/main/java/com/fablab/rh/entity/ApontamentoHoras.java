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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Apontamento de horas dedicadas a uma encomenda ou projeto.
 *
 * <p>Corresponde à tabela {@code apontamento_horas}.</p>
 */
@Entity
@Table(name = "apontamento_horas")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApontamentoHoras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 32)
    private TipoApontamento tipo;

    @Column(name = "id_referencia", nullable = false)
    private Long idReferencia;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "horas_trabalhadas", nullable = false, precision = 5, scale = 2)
    private BigDecimal horasTrabalhadas;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Column(name = "motivo_rejeicao")
    private String motivoRejeicao;

    @Column(name = "descricao_atividade")
    private String descricaoAtividade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private StatusApontamento status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_admin_validador")
    private Funcionario idAdminValidador;

    @Column(name = "data_validacao")
    private Instant dataValidacao;

    @Column(name = "consolidado", nullable = false)
    private Boolean consolidado = false;
}
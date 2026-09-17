package com.fablab.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Valor da hora de trabalho por nível de acesso (0-3), com vigência.
 *
 * <p>Corresponde à tabela {@code valor_hora_nivel}. O custo de mão de obra do
 * custeio por ordem usa o valor vigente do nível do executante.</p>
 */
@Entity
@Table(name = "valor_hora_nivel")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ValorHoraNivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idValorHora;

    @Column(name = "nivel_acesso", nullable = false)
    private Integer nivelAcesso;

    @Column(name = "valor_hora", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorHora;

    @Column(name = "data_vigencia", nullable = false)
    private LocalDate dataVigencia;
}
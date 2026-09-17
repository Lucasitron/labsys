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
 * Parâmetro de taxa de overhead sobre horas (taxa-hora aplicada à mão de obra).
 *
 * <p>Corresponde à tabela {@code parametro_overhead}. Suporta vigência para
 * permitir atualizações periódicas da taxa.</p>
 */
@Entity
@Table(name = "parametro_overhead")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ParametroOverhead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idParametro;

    @Column(name = "valor_taxa_hora", nullable = false, precision = 12, scale = 4)
    private BigDecimal valorTaxaHora;

    @Column(name = "data_vigencia", nullable = false)
    private LocalDate dataVigencia;
}
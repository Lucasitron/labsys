package com.fablab.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Taxa de overhead (custos indiretos) por hora com vigência. Corresponde à tabela {@code parametro_overhead}. */
@Entity
@Table(name = "parametro_overhead")
public class ParametroOverhead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Long id;

    @Column(name = "valor_taxa_hora", nullable = false, precision = 12, scale = 4)
    private BigDecimal valorTaxaHora;

    @Column(name = "data_vigencia", nullable = false)
    private LocalDate dataVigencia;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getValorTaxaHora() {
        return valorTaxaHora;
    }

    public void setValorTaxaHora(BigDecimal valorTaxaHora) {
        this.valorTaxaHora = valorTaxaHora;
    }

    public LocalDate getDataVigencia() {
        return dataVigencia;
    }

    public void setDataVigencia(LocalDate dataVigencia) {
        this.dataVigencia = dataVigencia;
    }
}

package com.fablab.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fechamento de encomenda: congela horas estimadas e valor acordado antes da
 * produção (D-5). Alterações geram nova ordem, nunca edição do congelado.
 * Corresponde à tabela {@code fechamento_encomenda}.
 */
@Entity
@Table(name = "fechamento_encomenda")
public class FechamentoEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fechamento")
    private Long id;

    @Column(name = "id_encomenda", nullable = false, unique = true)
    private Integer idEncomenda;

    @Column(name = "horas_estimadas", nullable = false, precision = 14, scale = 2)
    private BigDecimal horasEstimadas;

    @Column(name = "valor_fechado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorFechado;

    @Column(name = "data_fechamento", nullable = false)
    private LocalDate dataFechamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StatusFechamento status;

    @Column(name = "horas_validadas", nullable = false, precision = 14, scale = 2)
    private BigDecimal horasValidadas = BigDecimal.ZERO;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdEncomenda() {
        return idEncomenda;
    }

    public void setIdEncomenda(Integer idEncomenda) {
        this.idEncomenda = idEncomenda;
    }

    public BigDecimal getHorasEstimadas() {
        return horasEstimadas;
    }

    public void setHorasEstimadas(BigDecimal horasEstimadas) {
        this.horasEstimadas = horasEstimadas;
    }

    public BigDecimal getValorFechado() {
        return valorFechado;
    }

    public void setValorFechado(BigDecimal valorFechado) {
        this.valorFechado = valorFechado;
    }

    public LocalDate getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDate dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public StatusFechamento getStatus() {
        return status;
    }

    public void setStatus(StatusFechamento status) {
        this.status = status;
    }

    public BigDecimal getHorasValidadas() {
        return horasValidadas;
    }

    public void setHorasValidadas(BigDecimal horasValidadas) {
        this.horasValidadas = horasValidadas;
    }
}

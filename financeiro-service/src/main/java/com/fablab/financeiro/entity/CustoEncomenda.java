package com.fablab.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Custo calculado por ordem (Job Order Costing), congelado na entrega.
 * Corresponde à tabela {@code custo_encomenda}.
 */
@Entity
@Table(name = "custo_encomenda")
public class CustoEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_custo")
    private Long id;

    @Column(name = "id_encomenda", nullable = false)
    private Integer idEncomenda;

    @Column(name = "custo_materiais", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoMateriais;

    @Column(name = "custo_mao_obra", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoMaoObra;

    @Column(name = "custo_overhead", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoOverhead;

    @Column(name = "custo_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal custoTotal;

    @Column(name = "valor_venda", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorVenda;

    @Column(name = "margem_lucro", nullable = false, precision = 14, scale = 2)
    private BigDecimal margemLucro;

    @Column(name = "data_calculo", nullable = false)
    private LocalDate dataCalculo;

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

    public BigDecimal getCustoMateriais() {
        return custoMateriais;
    }

    public void setCustoMateriais(BigDecimal custoMateriais) {
        this.custoMateriais = custoMateriais;
    }

    public BigDecimal getCustoMaoObra() {
        return custoMaoObra;
    }

    public void setCustoMaoObra(BigDecimal custoMaoObra) {
        this.custoMaoObra = custoMaoObra;
    }

    public BigDecimal getCustoOverhead() {
        return custoOverhead;
    }

    public void setCustoOverhead(BigDecimal custoOverhead) {
        this.custoOverhead = custoOverhead;
    }

    public BigDecimal getCustoTotal() {
        return custoTotal;
    }

    public void setCustoTotal(BigDecimal custoTotal) {
        this.custoTotal = custoTotal;
    }

    public BigDecimal getValorVenda() {
        return valorVenda;
    }

    public void setValorVenda(BigDecimal valorVenda) {
        this.valorVenda = valorVenda;
    }

    public BigDecimal getMargemLucro() {
        return margemLucro;
    }

    public void setMargemLucro(BigDecimal margemLucro) {
        this.margemLucro = margemLucro;
    }

    public LocalDate getDataCalculo() {
        return dataCalculo;
    }

    public void setDataCalculo(LocalDate dataCalculo) {
        this.dataCalculo = dataCalculo;
    }
}

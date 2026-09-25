package com.fablab.vendas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Item de orçamento com extensão de composição de custo (D-5): material
 * (tipo/quantidade/unidade) e horas alimentam a estimativa de custo sem
 * alterar o preço de venda ({@code valorUnitario}).
 *
 * <p>Corresponde à tabela {@code item_orcamento}.</p>
 */
@Entity
@Table(name = "item_orcamento")
public class ItemOrcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_orcamento")
    private Long id;

    @Column(name = "id_orcamento", nullable = false)
    private Long idOrcamento;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "quantidade", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;

    /** Tipo de material da composição de custo (ex.: PLA) — extensão D-5. */
    @Column(name = "material_tipo", length = 64)
    private String materialTipo;

    /** Quantidade de material da composição de custo — extensão D-5. */
    @Column(name = "material_quantidade", precision = 12, scale = 3)
    private BigDecimal materialQuantidade;

    /** Unidade do material (ex.: kg) — extensão D-5. */
    @Column(name = "material_unidade", length = 16)
    private String materialUnidade;

    /** Horas estimadas da composição de custo — extensão D-5. */
    @Column(name = "horas", precision = 10, scale = 2)
    private BigDecimal horas;

    /** Item fora do estoque: precisa ser comprado — extensão D-5. */
    @Column(name = "compra", nullable = false)
    private boolean compra;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdOrcamento() {
        return idOrcamento;
    }

    public void setIdOrcamento(Long idOrcamento) {
        this.idOrcamento = idOrcamento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public String getMaterialTipo() {
        return materialTipo;
    }

    public void setMaterialTipo(String materialTipo) {
        this.materialTipo = materialTipo;
    }

    public BigDecimal getMaterialQuantidade() {
        return materialQuantidade;
    }

    public void setMaterialQuantidade(BigDecimal materialQuantidade) {
        this.materialQuantidade = materialQuantidade;
    }

    public String getMaterialUnidade() {
        return materialUnidade;
    }

    public void setMaterialUnidade(String materialUnidade) {
        this.materialUnidade = materialUnidade;
    }

    public BigDecimal getHoras() {
        return horas;
    }

    public void setHoras(BigDecimal horas) {
        this.horas = horas;
    }

    public boolean isCompra() {
        return compra;
    }

    public void setCompra(boolean compra) {
        this.compra = compra;
    }
}

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
 * Solicitação de compra (fluxo informativo: o Admin visualiza, não bloqueia).
 * Corresponde à tabela {@code solicitacao_compra}.
 */
@Entity
@Table(name = "solicitacao_compra")
public class SolicitacaoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitacao")
    private Long id;

    @Column(name = "id_item_estoque", nullable = false)
    private Integer idItemEstoque;

    @Column(name = "quantidade", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "valor_estimado", precision = 14, scale = 2)
    private BigDecimal valorEstimado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StatusCompra status;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDate dataSolicitacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdItemEstoque() {
        return idItemEstoque;
    }

    public void setIdItemEstoque(Integer idItemEstoque) {
        this.idItemEstoque = idItemEstoque;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorEstimado() {
        return valorEstimado;
    }

    public void setValorEstimado(BigDecimal valorEstimado) {
        this.valorEstimado = valorEstimado;
    }

    public StatusCompra getStatus() {
        return status;
    }

    public void setStatus(StatusCompra status) {
        this.status = status;
    }

    public LocalDate getDataSolicitacao() {
        return dataSolicitacao;
    }

    public void setDataSolicitacao(LocalDate dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }
}

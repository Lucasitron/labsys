package com.fablab.vendas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Encomenda: nasce na Fila a partir de orçamento aprovado (ou venda direta).
 * Corresponde à tabela {@code encomenda}.
 */
@Entity
@Table(name = "encomenda")
public class Encomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_encomenda")
    private Long id;

    /** Versão para lock otimista de concorrência no Kanban (D-9). */
    @Version
    @Column(name = "versao")
    private Long versao;

    @Column(name = "id_orcamento")
    private Long idOrcamento;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(name = "data_criacao", nullable = false)
    private LocalDate dataCriacao;

    @Column(name = "data_previsao_entrega")
    private LocalDate dataPrevisaoEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_kanban", nullable = false, length = 16)
    private StatusKanban statusKanban;

    @Column(name = "valor_final", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorFinal;

    @Column(name = "observacoes")
    private String observacoes;

    /** Id do usuário que criou o registro (D-3: mover/editar só criador+Admin). */
    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    /** Referência legada: encomenda anterior que originou esta nova ordem. */
    @Column(name = "encomenda_origem_id")
    private Long encomendaOrigemId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

    public Long getIdOrcamento() {
        return idOrcamento;
    }

    public void setIdOrcamento(Long idOrcamento) {
        this.idOrcamento = idOrcamento;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public LocalDate getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDate dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDate getDataPrevisaoEntrega() {
        return dataPrevisaoEntrega;
    }

    public void setDataPrevisaoEntrega(LocalDate dataPrevisaoEntrega) {
        this.dataPrevisaoEntrega = dataPrevisaoEntrega;
    }

    public StatusKanban getStatusKanban() {
        return statusKanban;
    }

    public void setStatusKanban(StatusKanban statusKanban) {
        this.statusKanban = statusKanban;
    }

    public BigDecimal getValorFinal() {
        return valorFinal;
    }

    public void setValorFinal(BigDecimal valorFinal) {
        this.valorFinal = valorFinal;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Long getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(Long criadoPor) {
        this.criadoPor = criadoPor;
    }

    public Long getEncomendaOrigemId() {
        return encomendaOrigemId;
    }

    public void setEncomendaOrigemId(Long encomendaOrigemId) {
        this.encomendaOrigemId = encomendaOrigemId;
    }
}

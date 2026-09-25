package com.fablab.vendas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Solicitação de edição feita por quem não pode editar o registro (tela
 * {@code /vendas/solicitacoes}). Corresponde à tabela
 * {@code solicitacao_edicao}.
 *
 * <p>Contrato mínimo (D-4 REPORTADO): solicitar, listar e decidir
 * (aprovar/rejeitar). A aplicação da edição aprovada é manual — não há
 * aplicação automática para não improvisar regra sem lastro.</p>
 */
@Entity
@Table(name = "solicitacao_edicao")
public class SolicitacaoEdicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitacao")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 32)
    private TipoSolicitacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "alvo_tipo", nullable = false, length = 16)
    private TipoAlvoSolicitacao alvoTipo;

    @Column(name = "alvo_id", nullable = false)
    private Long alvoId;

    @Column(name = "campo", nullable = false)
    private String campo;

    @Column(name = "valor_atual")
    private String valorAtual;

    @Column(name = "valor_proposto", nullable = false)
    private String valorProposto;

    @Column(name = "justificativa", nullable = false)
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StatusSolicitacao status;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Column(name = "decidido_por")
    private Long decididoPor;

    @Column(name = "motivo_decisao")
    private String motivoDecisao;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_decisao")
    private LocalDateTime dataDecisao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoSolicitacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitacao tipo) {
        this.tipo = tipo;
    }

    public TipoAlvoSolicitacao getAlvoTipo() {
        return alvoTipo;
    }

    public void setAlvoTipo(TipoAlvoSolicitacao alvoTipo) {
        this.alvoTipo = alvoTipo;
    }

    public Long getAlvoId() {
        return alvoId;
    }

    public void setAlvoId(Long alvoId) {
        this.alvoId = alvoId;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public String getValorAtual() {
        return valorAtual;
    }

    public void setValorAtual(String valorAtual) {
        this.valorAtual = valorAtual;
    }

    public String getValorProposto() {
        return valorProposto;
    }

    public void setValorProposto(String valorProposto) {
        this.valorProposto = valorProposto;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public void setStatus(StatusSolicitacao status) {
        this.status = status;
    }

    public Long getSolicitanteId() {
        return solicitanteId;
    }

    public void setSolicitanteId(Long solicitanteId) {
        this.solicitanteId = solicitanteId;
    }

    public Long getDecididoPor() {
        return decididoPor;
    }

    public void setDecididoPor(Long decididoPor) {
        this.decididoPor = decididoPor;
    }

    public String getMotivoDecisao() {
        return motivoDecisao;
    }

    public void setMotivoDecisao(String motivoDecisao) {
        this.motivoDecisao = motivoDecisao;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataDecisao() {
        return dataDecisao;
    }

    public void setDataDecisao(LocalDateTime dataDecisao) {
        this.dataDecisao = dataDecisao;
    }
}

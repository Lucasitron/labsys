package com.fablab.vendas.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Encomenda (ordem de produção) vinculada a um cliente.
 *
 * <p>Corresponde à tabela {@code encomenda}. O campo {@code version} é usado
 * para controle otimista de concorrência nas movimentações do Kanban.</p>
 */
@Entity
@Table(name = "encomenda")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Encomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orcamento")
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "data_criacao", nullable = false)
    private LocalDate dataCriacao;

    @Column(name = "data_previsao_entrega")
    private LocalDate dataPrevisaoEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_kanban", nullable = false, length = 20)
    private StatusKanban statusKanban;

    @Column(name = "valor_final", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorFinal;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoricoStatusEncomenda> historico = new ArrayList<>();

    /** Adiciona um registro de histórico de status. */
    public void adicionarHistorico(HistoricoStatusEncomenda historicoStatus) {
        historicoStatus.setEncomenda(this);
        historico.add(historicoStatus);
    }
}
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitação de compra (fluxo informativo para o Estoque &amp; Suprimentos).
 *
 * <p>Corresponde à tabela {@code solicitacao_compra}. Ao registrar, publica
 * {@code compra.solicitada.event} informando o Estoque.</p>
 */
@Entity
@Table(name = "solicitacao_compra")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SolicitacaoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idSolicitacao;

    @Column(name = "id_item_estoque", nullable = false)
    private Long idItemEstoque;

    @Column(name = "quantidade", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "valor_estimado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorEstimado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusSolicitacaoCompra status;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDate dataSolicitacao;
}
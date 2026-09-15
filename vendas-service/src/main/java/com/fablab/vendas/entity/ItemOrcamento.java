package com.fablab.vendas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Item de um orçamento (descrição, quantidade e valor unitário).
 *
 * <p>Corresponde à tabela {@code item_orcamento}.</p>
 */
@Entity
@Table(name = "item_orcamento")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemOrcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orcamento", nullable = false)
    private Orcamento orcamento;

    @Column(name = "descricao", nullable = false, length = 300)
    private String descricao;

    @Column(name = "quantidade", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorUnitario;

    /** Subtotal do item (quantidade × valor unitário). */
    public BigDecimal getSubtotal() {
        return quantidade.multiply(valorUnitario);
    }
}
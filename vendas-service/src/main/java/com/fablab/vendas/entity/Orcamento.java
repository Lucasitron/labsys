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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Orçamento enviado a um cliente.
 *
 * <p>Corresponde à tabela {@code orcamento}. O {@code valorTotal} é a soma dos
 * itens registrados em {@code ItemOrcamento}.</p>
 */
@Entity
@Table(name = "orcamento")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "data_criacao", nullable = false)
    private LocalDate dataCriacao;

    @Column(name = "validade", nullable = false)
    private LocalDate validade;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusOrcamento status;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrcamento> itens = new ArrayList<>();

    /** Recalcula o {@code valorTotal} a partir dos itens atuais. */
    public void recalcularValorTotal() {
        this.valorTotal = itens.stream()
                .map(ItemOrcamento::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Adiciona um item ao orçamento vinculando a referência inversa. */
    public void adicionarItem(ItemOrcamento item) {
        item.setOrcamento(this);
        itens.add(item);
    }
}
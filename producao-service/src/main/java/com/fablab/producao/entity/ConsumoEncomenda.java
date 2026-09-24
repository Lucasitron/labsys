package com.fablab.producao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Acúmulo interno do consumo real de itens por encomenda.
 *
 * <p>Alimentado por {@code estoque.consumo.realizado.event} e reemitido em
 * {@code producao.concluida.event} para a baixa de estoque e o custeio.</p>
 */
@Entity
@Table(name = "consumo_encomenda")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConsumoEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idConsumo;

    @Column(name = "id_encomenda", nullable = false)
    private Long idEncomenda;

    @Column(name = "id_item", nullable = false)
    private Long idItem;

    @Column(name = "quantidade_consumida", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidadeConsumida;
}
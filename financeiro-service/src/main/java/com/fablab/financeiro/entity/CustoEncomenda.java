package com.fablab.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Custo calculado de uma encomenda (Job Order Costing).
 *
 * <p>Corresponde à tabela {@code custo_encomenda}. {@code margemLucro} é
 * calculada como {@code valorVenda - custoTotal}. Publica
 * {@code custo.calculado.event} após o cálculo.</p>
 */
@Entity
@Table(name = "custo_encomenda")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CustoEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idCusto;

    @Column(name = "id_encomenda", nullable = false)
    private Long idEncomenda;

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
}
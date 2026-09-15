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
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro manual de venda em um marketplace externo.
 *
 * <p>Corresponde à tabela {@code registro_marketplace}.</p>
 */
@Entity
@Table(name = "registro_marketplace")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RegistroMarketplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_encomenda", nullable = false)
    private Encomenda encomenda;

    @Column(name = "plataforma", nullable = false, length = 100)
    private String plataforma;

    @Column(name = "codigo_externo", length = 100)
    private String codigoExterno;

    @Column(name = "data_venda", nullable = false)
    private LocalDate dataVenda;

    @Column(name = "valor_taxa", precision = 14, scale = 2)
    private BigDecimal valorTaxa;
}
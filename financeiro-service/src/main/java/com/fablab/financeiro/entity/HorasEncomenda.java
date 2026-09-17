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
 * Acúmulo interno de horas validadas por encomenda para o custeio por ordem.
 *
 * <p>Corresponde à tabela {@code horas_encomenda}. Cada linha agrupa as horas
 * validadas de um funcionário (nível informativo) recebidas via
 * {@code horas.validadas.event} para alimentar o Job Order Costing.</p>
 */
@Entity
@Table(name = "horas_encomenda")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HorasEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idHoras;

    @Column(name = "id_encomenda", nullable = false)
    private Long idEncomenda;

    @Column(name = "id_funcionario", nullable = false)
    private Long idFuncionario;

    @Column(name = "nivel_acesso")
    private Integer nivelAcesso;

    @Column(name = "horas", nullable = false, precision = 12, scale = 2)
    private BigDecimal horas;

    @Column(name = "data_registro", nullable = false)
    private LocalDate dataRegistro;
}
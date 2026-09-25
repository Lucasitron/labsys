package com.fablab.rh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de ponto diário consolidado a partir dos eventos RFID.
 *
 * <p>Corresponde à tabela {@code registro_ponto_diario}. Um registro por
 * funcionário e data.</p>
 */
@Entity
@Table(name = "registro_ponto_diario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ponto_funcionario_data", columnNames = {"id_funcionario", "data"})
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RegistroPontoDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "hora_entrada")
    private Instant horaEntrada;

    @Column(name = "hora_saida")
    private Instant horaSaida;

    @Column(name = "total_horas", precision = 5, scale = 2)
    private BigDecimal totalHoras;
}
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vínculo entre um apontamento de horas e o certificado que o consolidou.
 *
 * <p>Corresponde à tabela {@code hora_consolidada}.</p>
 */
@Entity
@Table(name = "hora_consolidada")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HoraConsolidada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long idConsolidacao;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_certificado", nullable = false)
    private CertificadoEmitido certificado;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_apontamento", nullable = false)
    private ApontamentoHoras apontamento;

    @Column(name = "horas", nullable = false, precision = 5, scale = 2)
    private BigDecimal horas;

    @Column(name = "data_consolidacao", nullable = false)
    private LocalDateTime dataConsolidacao;
}

package com.fablab.rh.entity;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitação de certificado de horas feita por um funcionário e decidida por
 * um Admin.
 *
 * <p>Corresponde à tabela {@code solicitacao_certificado}.</p>
 */
@Entity
@Table(name = "solicitacao_certificado")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SolicitacaoCertificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long idSolicitacao;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_certificado", nullable = false, length = 32)
    private TipoCertificado tipoCertificado;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDateTime dataSolicitacao;

    @Column(name = "horas_solicitadas", nullable = false, precision = 7, scale = 2)
    private BigDecimal horasSolicitadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private StatusSolicitacao status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_admin_aprovador")
    private Funcionario idAdminAprovador;

    @Column(name = "data_decisao")
    private LocalDateTime dataDecisao;

    @Column(name = "observacao")
    private String observacao;
}

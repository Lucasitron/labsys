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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Certificado de horas efetivamente emitido a partir da aprovação de uma
 * solicitação.
 *
 * <p>Corresponde à tabela {@code certificado_emitido}. O
 * {@code codigoVerificacao} é um UUID usado para validação pública do
 * certificado.</p>
 */
@Entity
@Table(name = "certificado_emitido")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CertificadoEmitido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long idCertificado;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_solicitacao", nullable = false, unique = true)
    private SolicitacaoCertificado solicitacao;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_certificado", nullable = false, length = 32)
    private TipoCertificado tipoCertificado;

    @Column(name = "horas_certificadas", nullable = false, precision = 7, scale = 2)
    private BigDecimal horasCertificadas;

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao;

    @Column(name = "codigo_verificacao", nullable = false, unique = true)
    private UUID codigoVerificacao;
}

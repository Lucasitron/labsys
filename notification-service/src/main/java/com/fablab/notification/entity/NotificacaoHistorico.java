package com.fablab.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cópia histórica de uma notificação revisada pelo Admin. Mantida por
 * {@code retencao-dias} dias após a revisão e então expurgada.
 */
@Entity
@Table(name = "notificacao_historico")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificacaoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idHistorico;

    @Column(name = "id_notificacao_original")
    private Long idNotificacaoOriginal;

    @Column(name = "id_destinatario")
    private Long idDestinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalNotificacao canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEvento tipoEvento;

    @Column(name = "assunto", nullable = false, length = 255)
    private String assunto;

    @Column(name = "mensagem", nullable = false, length = 2000)
    private String mensagem;

    @Column(name = "id_referencia")
    private Long idReferencia;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(name = "data_leitura")
    private LocalDateTime dataLeitura;

    @Column(name = "data_revisao_admin", nullable = false)
    private LocalDateTime dataRevisaoAdmin;

    @Column(name = "id_admin_revisor")
    private Long idAdminRevisor;
}

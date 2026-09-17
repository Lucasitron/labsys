package com.fablab.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configuração de um canal de notificação. */
@Entity
@Table(name = "configuracao_canal")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConfiguracaoCanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idConfiguracao;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, unique = true, length = 20)
    private CanalNotificacao canal;

    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado;

    @Column(name = "parametros", length = 2000)
    private String parametros;
}

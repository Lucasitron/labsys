package com.fablab.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Notificação endereçada a um usuário (id do Auth Service).
 *
 * <p>Corresponde à tabela {@code notificacao}. No MVP as notificações nascem
 * dos eventos RabbitMQ da malha (RFID, horas validadas, certificados) — a
 * contagem de não lidas alimenta o App Shell.</p>
 */
@Entity
@Table(name = "notificacao")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "titulo", nullable = false, length = 160)
    private String titulo;

    @Column(name = "mensagem")
    private String mensagem;

    @Column(name = "lida", nullable = false)
    private Boolean lida = false;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;
}
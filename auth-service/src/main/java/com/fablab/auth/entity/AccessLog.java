package com.fablab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de acesso físico via RFID (ponto eletrônico e controle de acesso).
 *
 * <p>Corresponde à tabela {@code access_log}. Para cartões não reconhecidos,
 * {@code idUser} fica {@code null} e o tipo é {@link AccessLogType#ACESSO_NEGADO}.</p>
 */
@Entity
@Table(name = "access_log", indexes = {
        @Index(name = "ix_access_log_uuid", columnList = "uuid_rfid, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "id_user")
    private Long idUser;

    @Column(name = "uuid_rfid", nullable = false)
    private String uuidRfid;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private AccessLogType type;
}
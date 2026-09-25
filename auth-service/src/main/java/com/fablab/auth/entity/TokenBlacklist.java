package com.fablab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Token JWT invalidado (logout) até sua data de expiração.
 *
 * <p>Corresponde à tabela {@code token_blacklist}.</p>
 */
@Entity
@Table(name = "token_blacklist", indexes = {
        @Index(name = "ix_token_blacklist_token", columnList = "token", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 2048)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;
}
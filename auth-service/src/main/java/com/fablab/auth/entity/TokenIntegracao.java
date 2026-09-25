package com.fablab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token de integração (C-5): armazena SOMENTE hash SHA-256 + prefixo.
 *
 * <p>A chave em claro é exibida UMA vez na resposta de criação e NUNCA
 * persistida, logada ou reexposta. Revogação é soft
 * ({@code revogado=true}).</p>
 */
@Entity
@Table(name = "token_integracao", indexes = {
        @Index(name = "ix_token_integracao_hash", columnList = "hash", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class TokenIntegracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 128)
    private String nome;

    @Column(name = "prefixo", nullable = false, length = 32)
    private String prefixo;

    /** Hash SHA-256 hexadecimal da chave. NUNCA retornar ao client. */
    @Column(name = "hash", nullable = false, unique = true, length = 64)
    private String hash;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "ultimo_uso")
    private Instant ultimoUso;

    @Column(name = "revogado", nullable = false)
    private boolean revogado;
}

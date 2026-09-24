package com.fablab.auth.service;

import com.fablab.auth.entity.TokenBlacklist;
import com.fablab.auth.repository.TokenBlacklistRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Gerenciamento da blacklist de tokens JWT (logout).
 */
@Service
public class TokenBlacklistService {

    private final TokenBlacklistRepository repository;

    public TokenBlacklistService(TokenBlacklistRepository repository) {
        this.repository = repository;
    }

    /**
     * Verifica se um token está revogado.
     */
    public boolean isBlacklisted(String token) {
        return repository.existsByToken(token);
    }

    /**
     * Adiciona um token à blacklist até sua data de expiração.
     *
     * <p>A operação é idempotente.</p>
     */
    public void blacklist(String token, Claims claims) {
        Instant expiry = claims.getExpiration() != null
                ? claims.getExpiration().toInstant()
                : Instant.now().plusSeconds(60);
        if (!repository.existsByToken(token)) {
            TokenBlacklist entry = new TokenBlacklist();
            entry.setToken(token);
            entry.setExpiryDate(expiry);
            repository.save(entry);
        }
    }

    /**
     * Remove tokens expirados da blacklist. Invocado diariamente via agendamento.
     */
    public long cleanupExpired() {
        return repository.deleteByExpiryDateBefore(Instant.now());
    }
}
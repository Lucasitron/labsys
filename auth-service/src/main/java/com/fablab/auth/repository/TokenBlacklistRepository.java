package com.fablab.auth.repository;

import com.fablab.auth.entity.TokenBlacklist;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code token_blacklist}.
 */
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByToken(String token);

    long deleteByExpiryDateBefore(Instant now);
}
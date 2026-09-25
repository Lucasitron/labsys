package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fablab.auth.entity.TokenBlacklist;
import com.fablab.auth.repository.TokenBlacklistRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class TokenBlacklistRepositoryTest {

    @Autowired
    private TokenBlacklistRepository repository;

    private TokenBlacklist entry(String token, Instant expiry) {
        TokenBlacklist entry = new TokenBlacklist();
        entry.setToken(token);
        entry.setExpiryDate(expiry);
        return repository.save(entry);
    }

    @Test
    void existsByToken() {
        entry("token-a", Instant.now().plusSeconds(900));
        assertThat(repository.existsByToken("token-a")).isTrue();
        assertThat(repository.existsByToken("token-z")).isFalse();
    }

    @Test
    void deletesOnlyExpiredTokens() {
        entry("expired", Instant.now().minusSeconds(60));
        entry("active", Instant.now().plusSeconds(900));

        long removed = repository.deleteByExpiryDateBefore(Instant.now());

        assertThat(removed).isEqualTo(1);
        assertThat(repository.existsByToken("expired")).isFalse();
        assertThat(repository.existsByToken("active")).isTrue();
    }
}
package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.auth.entity.TokenBlacklist;
import com.fablab.auth.repository.TokenBlacklistRepository;
import com.fablab.auth.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenBlacklistServiceTest {

    @Mock
    private TokenBlacklistRepository repository;

    private TokenBlacklistService createService() {
        return new TokenBlacklistService(repository);
    }

    @Test
    void blacklistsTokenUntilItsExpiration() {
        when(repository.existsByToken("token-a")).thenReturn(false);
        Date expiresAt = Date.from(Instant.now().plusSeconds(900));
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.getExpiration()).thenReturn(expiresAt);

        createService().blacklist("token-a", claims);

        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("token-a");
        assertThat(captor.getValue().getExpiryDate()).isEqualTo(expiresAt.toInstant());
    }

    @Test
    void blacklistIsIdempotent() {
        when(repository.existsByToken("token-a")).thenReturn(true);
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        createService().blacklist("token-a", claims);

        verify(repository, never()).save(any());
    }

    @Test
    void fallsBackToShortExpiryWhenClaimMissing() {
        when(repository.existsByToken("token-a")).thenReturn(false);
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.getExpiration()).thenReturn(null);

        createService().blacklist("token-a", claims);

        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void isBlacklistedDelegatesToRepository() {
        when(repository.existsByToken("token-a")).thenReturn(true);
        assertThat(createService().isBlacklisted("token-a")).isTrue();
        when(repository.existsByToken("token-b")).thenReturn(false);
        assertThat(createService().isBlacklisted("token-b")).isFalse();
    }

    @Test
    void cleanupDeletesExpiredTokens() {
        when(repository.deleteByExpiryDateBefore(any(Instant.class))).thenReturn(3L);
        assertThat(createService().cleanupExpired()).isEqualTo(3L);
    }
}
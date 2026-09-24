package com.fablab.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Garante que as senhas sejam armazenadas apenas como hash BCrypt.
 */
class PasswordHashingTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void rawPasswordIsNeverReturnedByEncoder() {
        String raw = "Senha@123";
        String hash = encoder.encode(raw);

        assertThat(hash).isNotEqualTo(raw);
        assertThat(hash).startsWith("$2a$");
        assertThat(hash).doesNotContain(raw);
    }

    @Test
    void encodedPasswordMatchesRawPassword() {
        String hash = encoder.encode("Senha@123");
        assertThat(encoder.matches("Senha@123", hash)).isTrue();
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        String hash = encoder.encode("Senha@123");
        assertThat(encoder.matches("SenhaErrada", hash)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "Forte#2026", "a b c d e f g h i j k l m"})
    void typicalPasswordsHashAndVerify(String password) {
        String hash = encoder.encode(password);
        assertThat(encoder.matches(password, hash)).isTrue();
    }
}
package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fablab.auth.entity.Login;
import com.fablab.auth.repository.LoginRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class LoginRepositoryTest {

    @Autowired
    private LoginRepository repository;

    @BeforeEach
    void seed() {
        Login login = new Login();
        login.setIdUser(7L);
        login.setUuid("CARD-001");
        login.setEmail("admin@fablab.io");
        login.setNomeUsuario("admin");
        login.setSenhaHash("$2a$10$hashed");
        login.setSetor("Direção");
        repository.save(login);
    }

    @Test
    void findsByEmail() {
        assertThat(repository.findByEmail("admin@fablab.io")).isPresent();
        assertThat(repository.findByEmail("ghost@fablab.io")).isEmpty();
    }

    @Test
    void findsByNomeUsuario() {
        assertThat(repository.findByNomeUsuario("admin")).isPresent();
        assertThat(repository.findByNomeUsuario("ghost")).isEmpty();
    }

    @Test
    void findsByUuid() {
        assertThat(repository.findByUuid("CARD-001")).isPresent();
        assertThat(repository.findByUuid("CARD-999")).isEmpty();
    }

    @Test
    void existsByEmail() {
        assertThat(repository.existsByEmail("admin@fablab.io")).isTrue();
        assertThat(repository.existsByEmail("ghost@fablab.io")).isFalse();
    }

    @Test
    void enforcesUniqueEmail() {
        Login duplicate = new Login();
        duplicate.setIdUser(8L);
        duplicate.setUuid("CARD-002");
        duplicate.setEmail("admin@fablab.io");
        duplicate.setNomeUsuario("outro");
        duplicate.setSenhaHash("$2a$10$hashed");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
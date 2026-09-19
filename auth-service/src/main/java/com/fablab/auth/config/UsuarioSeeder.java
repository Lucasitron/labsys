package com.fablab.auth.config;

import com.fablab.auth.entity.Usuario;
import com.fablab.auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds minimais para o MVP: quando a tabela de usuários está vazia, cria o
 * acesso raiz (Admin) a partir de variáveis de ambiente (ou valores padrão),
 * mantendo a humanização simples de subir a malha. Vinculação com o Pessoas
 * &amp; RH é feita manualmente (ver {@code contract.gap.md} §8).
 */
@Component
public class UsuarioSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties properties;

    public UsuarioSeeder(UsuarioRepository usuarioRepository,
                         PasswordEncoder passwordEncoder,
                         SeedProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario raiz = new Usuario();
        raiz.setUsername(properties.username());
        raiz.setPasswordHash(passwordEncoder.encode(properties.password()));
        raiz.setNomeCompleto(properties.name());
        raiz.setEmail(properties.email());
        raiz.setRole(0);
        raiz.setSetor(null);
        raiz.setAtivo(true);
        usuarioRepository.save(raiz);

        log.info("Seed de acesso raiz criado para '{}' (nível Admin)", properties.username());
    }
}
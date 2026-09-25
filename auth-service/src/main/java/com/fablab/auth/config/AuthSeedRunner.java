package com.fablab.auth.config;

import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import com.fablab.auth.entity.UserPermission;
import com.fablab.auth.repository.LoginRepository;
import com.fablab.auth.repository.UserPermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed de acesso admin para homologação do frontend (§6.0, dev-only).
 *
 * <p>Só executa com {@code auth.seed.enabled=true} (perfil dev) e somente se a
 * tabela {@code login} estiver vazia (idempotente). Cria
 * {@code Login{email=admin@fablab.org, nomeUsuario=admin, senhaHash=BCrypt(admin123)}}
 * + {@code UserPermission{role=ADMIN(0), active=true}}. Nunca loga credenciais e
 * nunca deve ser habilitado em produção.</p>
 */
@Component
@Order(Integer.MAX_VALUE)
@ConditionalOnProperty(prefix = "auth.seed", name = "enabled", havingValue = "true")
public class AuthSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthSeedRunner.class);

    private final LoginRepository loginRepository;
    private final UserPermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties properties;

    public AuthSeedRunner(LoginRepository loginRepository,
                          UserPermissionRepository permissionRepository,
                          PasswordEncoder passwordEncoder,
                          SeedProperties properties) {
        this.loginRepository = loginRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (loginRepository.count() > 0) {
            return;
        }
        if (isBlank(properties.senha()) || isBlank(properties.email())
                || isBlank(properties.nomeUsuario())) {
            log.warn("Seed admin ignorado: propriedades auth.seed incompletas");
            return;
        }

        Login login = new Login();
        login.setIdUser(properties.idUser() != null ? properties.idUser() : 1L);
        login.setUuid(blankToDefault(properties.uuid(), "admin-root-rfid"));
        login.setEmail(properties.email().trim());
        login.setNomeUsuario(properties.nomeUsuario().trim());
        login.setSenhaHash(passwordEncoder.encode(properties.senha()));
        login.setSetor(properties.setor());
        login = loginRepository.save(login);

        UserPermission permission = new UserPermission();
        permission.setIdUser(login.getIdUser());
        permission.setRole(Role.ADMIN);
        permission.setActive(true);
        permissionRepository.save(permission);

        log.info("Seed de acesso admin criado (dev-only, tabela login estava vazia)");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String blankToDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }
}

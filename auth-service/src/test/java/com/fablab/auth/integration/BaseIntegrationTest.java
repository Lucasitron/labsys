package com.fablab.auth.integration;

import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import com.fablab.auth.entity.UserPermission;
import com.fablab.auth.repository.AccessLogRepository;
import com.fablab.auth.repository.LoginRepository;
import com.fablab.auth.repository.TokenBlacklistRepository;
import com.fablab.auth.repository.UserPermissionRepository;
import com.fablab.auth.service.JwtService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base para testes de integração: contexto completo, banco H2 (rollback por
 * teste) e RabbitMQ substituído por mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
@MockBean(RabbitTemplate.class)
public abstract class BaseIntegrationTest {

    protected static final String SENHA = "Senha@123";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected LoginRepository loginRepository;
    @Autowired
    protected UserPermissionRepository permissionRepository;
    @Autowired
    protected AccessLogRepository accessLogRepository;
    @Autowired
    protected TokenBlacklistRepository blacklistRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtService jwtService;
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected Login seedUser(long idUser, Role role, String uuid, String email, String nome, String setor) {
        Login login = new Login();
        login.setIdUser(idUser);
        login.setUuid(uuid);
        login.setEmail(email);
        login.setNomeUsuario(nome);
        login.setSenhaHash(passwordEncoder.encode(SENHA));
        login.setSetor(setor);
        login = loginRepository.save(login);

        UserPermission permission = new UserPermission();
        permission.setIdUser(idUser);
        permission.setRole(role);
        permission.setActive(true);
        permissionRepository.save(permission);

        return login;
    }

    protected String accessToken(Login login, Role role) {
        return jwtService.generateAccessToken(login, role);
    }

    protected String refreshToken(Login login, Role role) {
        return jwtService.generateRefreshToken(login, role);
    }
}
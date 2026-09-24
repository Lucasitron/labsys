package com.fablab.auth;

import com.fablab.auth.entity.AccessLog;
import com.fablab.auth.entity.AccessLogType;
import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import com.fablab.auth.entity.UserPermission;
import java.time.Instant;

/**
 * Fábrica de entidades para testes.
 */
public final class TestData {

    public static final String SECRET = "fablab-auth-test-secret-key-min-32-bytes-1234567890";

    private TestData() {
    }

    public static Login login(long id, long idUser, String uuid, String email, String nome, String setor) {
        Login login = new Login();
        login.setId(id);
        login.setIdUser(idUser);
        login.setUuid(uuid);
        login.setEmail(email);
        login.setNomeUsuario(nome);
        login.setSenhaHash("$2a$10$hash");
        login.setSetor(setor);
        return login;
    }

    public static UserPermission permission(long idUser, Role role, boolean active) {
        UserPermission permission = new UserPermission();
        permission.setIdUser(idUser);
        permission.setRole(role);
        permission.setActive(active);
        return permission;
    }

    public static AccessLog accessLog(long id, Long idUser, String uuid, AccessLogType type, Instant timestamp) {
        AccessLog log = new AccessLog();
        log.setId(id);
        log.setIdUser(idUser);
        log.setUuidRfid(uuid);
        log.setType(type);
        log.setTimestamp(timestamp);
        return log;
    }
}
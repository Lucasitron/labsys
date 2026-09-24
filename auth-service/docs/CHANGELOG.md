# Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e este
projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [0.1.0] - 2026-09-15

### Adicionado

- Projeto Spring Boot do Auth & Identity Service (Java 21, Spring Boot 3.4.x).
- Entidades JPA: `Login`, `UserPermission`, `TokenBlacklist`, `AccessLog` e enums
  `Role` (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário, 4-Recrutando) e `AccessLogType`.
- Migrations Flyway (`V1` a `V4`) com as tabelas do schema de autenticação.
- Repositórios JPA com consultas derivadas.
- Autenticação via e-mail/nome de usuário e senha (`POST /auth/login`).
- Emissão e validação de tokens JWT (HMAC SHA-256) com claims `id_user`, `role` e
  `setor`; tokens de acesso (15 min) e refresh tokens (7 dias).
- Renovação de tokens com rotação (`POST /auth/refresh`).
- Logout seguro com blacklist (`POST /auth/logout`).
- Validação de RFID com registro de acesso e alternância ENTRADA/SAIDA
  (`POST /auth/validate-rfid`).
- Publicação do evento `access.rfid.event` no RabbitMQ.
- Perfil do usuário autenticado (`GET /auth/me`).
- Matriz de permissões RBAC (`GET /auth/permissions`).
- Spring Security com `SecurityFilterChain`, filtro JWT, entry point e handler de
  acesso negado com respostas JSON.
- Senhas armazenadas apenas como hash BCrypt.
- Tratamento centralizado de exceções (`@RestControllerAdvice`).
- Testes unitários, de integração e de segurança.
- Cobertura de código via JaCoCo (mínimo de 80%).
- Documentação: `README.md`, `API.md`, `BUGS.md`, `ACTION_PLAN.md`.
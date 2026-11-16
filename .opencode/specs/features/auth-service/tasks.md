# Tasks: Auth & Identity Service

> Módulo: `auth-service/` · pacote raiz `src/main/java/com/fablab/auth` · recursos `src/main/resources` · testes `src/test/java/com/fablab/auth`.
> Validado contra o código real da branch `feature/auth-service`.

---

## task-001 — Setup do módulo e bootstrap

- **Escopo:** `auth-service/pom.xml`, `auth-service/Dockerfile`, `auth-service/src/main/resources/application.yml`, `application-dev.yml`, `application-testing.yml`, `auth-service/src/main/java/com/fablab/auth/AuthApplication.java`
- **Entradas:** padrão de módulos do monorepo (ex.: discovery-service), stack Java 21 + Spring Boot 3.x, portas canônicas (auth 8081).
- **Saídas:** módulo Maven espelhável com `mvn verify`, bootstrap com Flyway/JPA/Eureka/Rabbit, config por env (`PORT`, `DB_URL`, `JWT_SECRET`, `EUREKA_URI`, `RABBITMQ_*`).

- **Critérios de aceite:**
  - [ ] `spring.application.name=auth-service` e `server.port=${PORT:8081}`
  - [ ] `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/fablab_auth}`, user `fablab`
  - [ ] `jwt.secret=${JWT_SECRET}`, `expiration-seconds=900`, `refresh-expiration-seconds=604800`, `issuer=fablab`
  - [ ] `flyway.enabled=true`, `jpa.hibernate.ddl-auto=validate`
  - [ ] Eureka `defaultZone=${EUREKA_URI:http://localhost:8761/eureka}`, `prefer-ip-address=true`
  - [ ] RabbitMQ com credenciais `fablab` e Actuator `health,info,metrics`
  - [ ] `mvn verify` compila; Dockerfile publicado

- **Dependências:** — (inicial)

---

## task-002 — Entity `Login` + migration V1

- **Escopo:** `src/main/resources/db/migration/V1__create_login_table.sql`, `src/main/java/com/fablab/auth/entity/Login.java`
- **Entradas:** spec §3/V1 (campos exatos de `login`), convenção Lombok + JPA do projeto.
- **Saídas:** tabela `login` criada via Flyway e entidade mapeada com constraints `uk_login_uuid`, `uk_login_email`, `uk_login_nome_usuario`.

- **Critérios de aceite:**
  - [ ] Colunas `id`, `id_user`, `uuid`, `email`, `nome_usuario`, `senha_hash`, `setor`
  - [ ] PK `id` BIGSERIAL; NOT NULL em `id_user`, `uuid`, `email`, `nome_usuario`, `senha_hash`
  - [ ] UNIQUE em `uuid`, `email`, `nome_usuario` (UK nomeadas)
  - [ ] `Login` com `@Getter/@Setter/@NoArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` em `id`
  - [ ] Migração aplica em DB limpo e `ddl-auto=validate` valida

- **Dependências:** `task-001`

---

## task-003 — Entity `UserPermission` + migration V2 + `Role`

- **Escopo:** `src/main/resources/db/migration/V2__create_user_permissions_table.sql`, `src/main/java/com/fablab/auth/entity/UserPermission.java`, `Role.java`, `RoleConverter.java`
- **Entradas:** spec §3/V2, níveis 0-4 (`ADMIN=0, BOLSISTA=1, VOLUNTARIO=2, ESTAGIARIO=3, RECRUTANDO=4`).
- **Saídas:** tabela `user_permissions` com `role` inteiro 0-4 + `active`; enum `Role` e conversor JPA int↔enum.

- **Critérios de aceite:**
  - [ ] Colunas `id`, `id_user`, `role` (CHECK 0-4), `active` (default TRUE)
  - [ ] Índice `ix_user_permissions_id_user`
  - [ ] Sem campo `responsabilidade` (conceito do RH, não do Auth)
  - [ ] `Role.fromCode(int)` presente; `RoleConverter` registrado via `@Convert`
  - [ ] `UserPermission.active` Boolean mapeado como `boolean`

- **Dependências:** `task-002`

---

## task-004 — Entity `TokenBlacklist` + migration V3

- **Escopo:** `src/main/resources/db/migration/V3__create_token_blacklist_table.sql`, `src/main/java/com/fablab/auth/entity/TokenBlacklist.java`
- **Entradas:** spec §3/V3.
- **Saídas:** tabela `token_blacklist` com UNIQUE `token` e `expiry_date`.

- **Critérios de aceite:**
  - [ ] Colunas `id`, `token` (TEXT NOT NULL UNIQUE), `expiry_date` (TIMESTAMPTZ NOT NULL)
  - [ ] Constraint nomeada `uk_token_blacklist_token`
  - [ ] Entidade com índice JPA único em `token`

- **Dependências:** `task-002`

---

## task-005 — Entity `AccessLog` + migration V4 + `AccessLogType`

- **Escopo:** `src/main/resources/db/migration/V4__create_access_log_table.sql`, `src/main/java/com/fablab/auth/entity/AccessLog.java`, `AccessLogType.java`
- **Entradas:** spec §3/V4, tipos `ENTRADA`, `SAIDA`, `ACESSO_NEGADO`.
- **Saídas:** tabela `access_log` com `ix_access_log_uuid` (uuid, timestamp DESC); enum de tipos.

- **Critérios de aceite:**
  - [ ] Colunas `id`, `id_user` (NULL p/ negado), `uuid_rfid` NOT NULL, `timestamp` NOT NULL, `type` VARCHAR(32) NOT NULL
  - [ ] Índice `ix_access_log_uuid` em (`uuid_rfid`, `timestamp`)
  - [ ] `@Enumerated(EnumType.STRING)` no campo `type`
  - [ ] Sem migração de `emailVer` (fora do MVP)

- **Dependências:** `task-002`

---

## task-006 — Repositórios Spring Data JPA

- **Escopo:** `src/main/java/com/fablab/auth/repository/LoginRepository.java`, `UserPermissionRepository.java`, `TokenBlacklistRepository.java`, `AccessLogRepository.java`
- **Entradas:** entidades das tasks 002-005.
- **Saídas:** repositórios com as queries usadas pelos services.

- **Critérios de aceite:**
  - [ ] `LoginRepository.findByEmail`, `findByNomeUsuario`, `findByUuid`, `findById`
  - [ ] `UserPermissionRepository.findByIdUser`, `findFirstByIdUserAndActiveTrue`
  - [ ] `TokenBlacklistRepository` (find/insert por token, delete expirados)
  - [ ] `AccessLogRepository.findTopByUuidRfidOrderByTimestampDesc`
  - [ ] Testes de repositório (slice) passando

- **Dependências:** `task-002`, `task-003`, `task-004`, `task-005`

---

## task-007 — `JwtService`, `JwtProperties` e `JwtAuthenticationFilter`

- **Escopo:** `src/main/java/com/fablab/auth/service/JwtService.java`, `config/JwtProperties.java`, `config/JwtAuthenticationFilter.java`
- **Entradas:** secreta JWT (`≥ 32 chars`), spec §2/F4 e §8/NFR-2/3.
- **Saídas:** geração/validação de access (900s) e refresh (604800s) com claims `id_user`, `role`, `setor`, `type`; filtro que popula `LoginPrincipal` no contexto.

- **Critérios de aceite:**
  - [ ] HMAC SHA-256; `Keys.hmacShaKeyFor`; falha cuidada se secret < 32 chars
  - [ ] Claim `sub` = id de `login`; `issuer = fablab`; clock skew 5s; `requireIssuer`
  - [ ] `generateAccessToken` / `generateRefreshToken` / `parseClaims` / `isTokenValid` / extrações
  - [ ] Distinção `type=access`/`type=refresh`; `isRefreshToken`
  - [ ] Filtro ignora rotas públicas, valida assinatura/exp e monta `Authentication` (ou deixa anônimo)
  - [ ] `JwtServiceTest` e `SecurityConfigTest` verdes

- **Dependências:** `task-006`

---

## task-008 — `SecurityConfig`, handlers REST e RBAC base

- **Escopo:** `src/main/java/com/fablab/auth/config/SecurityConfig.java`, `RestAuthenticationEntryPoint.java`, `RestAccessDeniedHandler.java`
- **Entradas:** rotas do AuthController (task-011), NFR-8.
- **Saídas:** cadeia de filtros stateless com públicas/protegidas e respostas JSON de erro 401/403.

- **Critérios de aceite:**
  - [ ] `@EnableWebSecurity` + `@EnableMethodSecurity`; CSRF off; `SessionCreationPolicy.STATELESS`
  - [ ] Públicas: `POST /auth/login`, `/auth/validate-rfid`, `/auth/refresh`, `GET /actuator/health`
  - [ ] `GET /auth/permissions` com `hasRole("ADMIN")`; demais autenticadas
  - [ ] `RestAuthenticationEntryPoint` (401) e `RestAccessDeniedHandler` (403) em JSON
  - [ ] `PasswordEncoder = BCryptPasswordEncoder`
  - [ ] Filtro adicionado antes de `UsernamePasswordAuthenticationFilter`

- **Dependências:** `task-007`

---

## task-009 — `AuthService` (login/logout/refresh/me) + `TokenBlacklistService` + cleanup

- **Escopo:** `src/main/java/com/fablab/auth/service/AuthService.java`, `TokenBlacklistService.java`, `TokenBlacklistCleanupTask.java`
- **Entradas:** repos (006), JWT (007), `LoginRequest/LoginResponse/RefreshRequest/RefreshResponse/LogoutRequest/LogoutResponse`.
- **Saídas:** fluxos F1/F2/F5/F6 completos (login com BCrypt + role ativa; logout via header/body; refresh rotacionado; `me` com permissões).

- **Critérios de aceite:**
  - [ ] Login por `email` ou `nomeUsuario`; falha genérica = `InvalidCredentialsException`
  - [ ] Sem permissão ativa → `ForbiddenException` (403)
  - [ ] Logout: aceita token do header `Authorization` ou body; blacklist até `expiry_date`
  - [ ] Refresh: rejeita tipo errado/blacklisted; rotaciona e blacklist antigo
  - [ ] `me`: monta `MeResponse` com `PermissionDto` via `LoginMapper`
  - [ ] `TokenBlacklistCleanupTask` diário às 03h remove expirados
  - [ ] `AuthServiceTest`, `TokenBlacklistServiceTest` verdes

- **Dependências:** `task-006`, `task-007`, `task-008`

---

## task-010 — `AccessLogService` + `RfidEventPublisher` (validação RFID)

- **Escopo:** `src/main/java/com/fablab/auth/service/AccessLogService.java`, `RfidEventPublisher.java`, `src/main/java/com/fablab/auth/dto/RfidAccessEvent.java`
- **Entradas:** `AccessLog`/`AccessLogType` (005), `RabbitTemplate`, routing/exchange da task-012.
- **Saídas:** resolução ENTRADA/SAIDA, registro em `access_log`, publicação de evento para o RH.

- **Critérios de aceite:**
  - [ ] `resolveType`: alterna ENTRADA↔SAIDA pelo último registro; sem anterior → ENTRADA
  - [ ] `record`: persiste `id_user`, `uuid_rfid`, `type`, `timestamp` (Instant agora)
  - [ ] `RfidAccessEvent{idUser, uuidRfid, timestamp, type}` em record
  - [ ] `publishAccess` publica na exchange `fablab.access` / `access.rfid.event`
  - [ ] `AccessLogServiceTest` e `AccessLogRepositoryTest` verdes

- **Dependências:** `task-005`, `task-006`, `task-012`

---

## task-011 — `AuthController` + DTOs + exceptions

- **Escopo:** `src/main/java/com/fablab/auth/controller/AuthController.java`, `dto/*.java`, `exception/*.java`, `mapper/LoginMapper.java`
- **Entradas:** services (009), spec §5 (endpoints).
- **Saídas:** API REST `/auth/*` completa com validação de DTOs e respostas de erro JSON.

- **Critérios de aceite:**
  - [ ] `POST /auth/login`, `POST /auth/logout`, `POST /auth/validate-rfid`, `GET /auth/me`, `POST /auth/refresh`, `GET /auth/permissions`
  - [ ] `LoginRequest` valida `@Email` (opcional) e `senha` `@NotBlank` 6-72
  - [ ] `@Valid` nos bodies; `GlobalExceptionHandler` centraliza erros (`ErrorResponse`)
  - [ ] `me()` lê `LoginPrincipal` do `Authentication`
  - [ ] `permissions` com `@RequestParam(defaultValue = "ADMIN") Role role`
  - [ ] `logout` extrai Bearer do header ou usa body `LogoutRequest`
  - [ ] `AuthControllerIntegrationTest` verde

- **Dependências:** `task-009`, `task-010`

---

## task-012 — RabbitMQ wiring (`RabbitMqConfig`) + evento RFID

- **Escopo:** `src/main/java/com/fablab/auth/config/RabbitMqConfig.java`
- **Entradas:** env `RABBITMQ_*` (application.yml), convenção de exchanges do monorepo.
- **Saídas:** exchange tópica durável `fablab.access`; constantes `ACCESS_EXCHANGE` e `RFID_ROUTING_KEY = "access.rfid.event"`.

- **Critérios de aceite:**
  - [ ] `TopicExchange("fablab.access", true, false)` registrado como `@Bean`
  - [ ] `RfidEventPublisher` publica com `convertAndSend(ACCESS_EXCHANGE, RFID_ROUTING_KEY, event)`
  - [ ] Teste de mensageria verifica mensagem publicada na exchange correta
  - [ ] Sem consumo no auth (consumidores residem em RH/Notification)

- **Dependências:** `task-010`

---

## task-013 — Suíte de testes (unit + integração + segurança + mensageria)

- **Escopo:** `src/test/java/com/fablab/auth/**` (`unit/`, `integration/`, `security/`) + `src/test/resources/application.yml` + `TestData.java`
- **Entradas:** todos os artefatos (002-012).
- **Saídas:** cobertura ≥ 80% cobrindo os fluxos críticos.

- **Critérios de aceite:**
  - [ ] Unit: `JwtServiceTest`, `AuthServiceTest`, `RbacServiceTest`, `AccessLogServiceTest`, `TokenBlacklistServiceTest`
  - [ ] Repositório: `LoginRepositoryTest`, `UserPermissionRepositoryTest` (implícito), `TokenBlacklistRepositoryTest`, `AccessLogRepositoryTest`
  - [ ] Integração: `AuthControllerIntegrationTest`, `RfidValidationIntegrationTest`, `TokenRotationIntegrationTest` | `BaseIntegrationTest`
  - [ ] Segurança: `SecurityConfigTest`, `PasswordHashingTest` (BCrypt)
  - [ ] Mensageria: publicação `access.rfid.event` validada
  - [ ] Cobertura agregada ≥ 80% verificada no build (`mvn verify`)

- **Dependências:** `task-011`, `task-012`

---

## task-014 — Documentação e refinamento

- **Escopo:** `auth-service/README.md`, `auth-service/docs/API.md`, `auth-service/docs/CHANGELOG.md`, `auth-service/docs/ACTION_PLAN.md`, esta spec
- **Entradas:** comportamento implementado (002-013).
- **Saídas:** documentação alinhada ao código real (endpoints, payloads, RBAC, eventos).

- **Critérios de aceite:**
  - [ ] `API.md` documenta os 6 endpoints com exemplos de request/response
  - [ ] RBAC (níveis 0-4 + matriz) documentado e alinhado a `docs/contract.gap.md` §7
  - [ ] Evento `access.rfid.event` documentado (exchange `fablab.access`, payload `RfidAccessEvent`)
  - [ ] `CHANGELOG.md`/`ACTION_PLAN.md` refletem o estado real
  - [ ] Specs (`requirements.md`, `plan.md`, `tasks.md`) revisadas como fonte viva

- **Dependências:** `task-013`

---

## Grafo de dependências (resumo)

```
task-001 ─┬→ 002 ─→ 003 ─┐
          ├→ 004 ────────┼→ 006 ─→ 007 ─→ 008 ─→ 009 ─┬→ 011
          └→ 005 ────────┘                             ├→ 010 ─→ 012
                                                        ├→ 011
                                             task-013  ← 011, 012
                                             task-014  ← 013
```

> Barras de conclusão: manter as especificações acima como fonte viva (atualizar conforme o código evoluir na branch `feature/auth-service`).
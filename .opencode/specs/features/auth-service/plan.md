# Plan: Auth & Identity Service

> Fonte de verdade: **código real da branch `feature/auth-service`** (pacote `com.fablab.auth`, módulo Maven `auth-service`, porta 8081).

---

## 1. Arquitetura

### 1.1 Visão geral
Microsserviço Spring Boot 3.x (Java 21), stateless, com segurança JWT própria e publicação de eventos assíncronos via RabbitMQ. O Auth **não consome** eventos — apenas publica (RH e Notification consomem).

```
[ESP32 / Frontend] → API Gateway (valida JWT) → auth-service:8081
                                   │
        ┌──────────────────────────┼────────────────────────────┐
        │                          │                            │
   auth-service             RabbitMQ (topic)               Eureka 8761
   (PostgreSQL fablab_auth)  fablab.access                    (registro)
  + Flyway V1..V4           access.rfid.event
                                   │
                                   ▼
                        Pessoas & RH Service (ponto / permissões)
```

### 1.2 Camadas
| Camada | Pacote | Responsabilidade |
| :--- | :--- | :--- |
| Controller | `controller` | `AuthController` — endpoints `/auth/*` |
| Service | `service` | `AuthService` (orquestração), `JwtService`, `RbacService`, `TokenBlacklistService`, `AccessLogService`, `RfidEventPublisher`, `TokenBlacklistCleanupTask` |
| Repository | `repository` | `LoginRepository`, `UserPermissionRepository`, `TokenBlacklistRepository`, `AccessLogRepository` |
| Entity | `entity` | `Login`, `UserPermission`, `Role`/`RoleConverter`, `TokenBlacklist`, `AccessLog`, `AccessLogType` |
| DTO | `dto` | `LoginRequest/LoginResponse`, `LogoutRequest/LogoutResponse`, `RefreshRequest/RefreshResponse`, `ValidateRfidRequest/Response`, `MeResponse`, `RbacResponse`, `PermissionDto`, `RfidAccessEvent`, `LoginPrincipal`, `ErrorResponse` |
| Config | `config` | `SecurityConfig`, `JwtAuthenticationFilter`, `JwtProperties`, `RabbitMqConfig`, `AppConfig`, handlers REST de auth/denied |
| Exception | `exception` | `InvalidCredentialsException`, `ForbiddenException`, `TokenInvalidException`, `TokenBlacklistedException`, `ResourceNotFoundException`, `GlobalExceptionHandler` |

### 1.3 Segurança
- `SecurityConfig`: CSRF off, sessão stateless; públicas `POST /auth/login`, `POST /auth/validate-rfid`, `POST /auth/refresh`, `GET /actuator/health`; `GET /auth/permissions` exige `hasRole("ADMIN")`; restante autenticado.
- `JwtAuthenticationFilter`: valida assinatura/issuer/exp do access token, popula `SecurityContext` com `LoginPrincipal{loginId, idUser, role, ...}`.
- `@EnableMethodSecurity` habilita `@PreAuthorize` em métodos.

### 1.4 Mensageria
- `RabbitMqConfig`: `TopicExchange` `fablab.access` (durável).
- `RfidEventPublisher`: `convertAndSend("fablab.access", "access.rfid.event", RfidAccessEvent)`.

---

## 2. Dependências (pom.xml — Maven 3.9+)

| Grupo | Artefato | Uso |
| :--- | :--- | :--- |
| Spring Boot Starter | `spring-boot-starter-web` | REST |
| Spring Boot Starter | `spring-boot-starter-security` | Segurança JWT + BCrypt |
| Spring Boot Starter | `spring-boot-starter-data-jpa` | Persistência |
| Spring Boot Starter | `spring-boot-starter-validation` | Validação de DTOs |
| Spring Boot Starter | `spring-boot-starter-amqp` | RabbitMQ |
| Spring Boot Starter | `spring-boot-starter-actuator` | `/actuator/health,info,metrics` |
| Spring Cloud | `spring-cloud-starter-netflix-eureka-client` | Descobrimento (8761) |
| RabbitMQ | `rabbitmq-docker` (compose) | Broker local |
| jjwt | `jjwt-api`/`jjwt-impl`/`jjwt-jackson` | Geração/parse JWT |
| Flyway | `flyway-core` + driver PostgreSQL | Migrations V1..V4 |
| PostgreSQL | `postgresql` driver | Banco `fablab_auth` |
| Lombok | `lombok` | Entities (getters/setters) |
| Test | `spring-boot-starter-test`, `spring-security-test`, Testcontainers (se disponível) | Testes |
| Docker | `auth-service/Dockerfile` | Build de imagem |

Config externa (env): `PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥32 chars), `JWT_EXPIRATION_SECONDS` (900), `JWT_REFRESH_EXPIRATION_SECONDS` (604800), `JWT_ISSUER` (fablab), `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`, `EUREKA_URI`.

---

## 3. Ordem de Implementação

| # | Passo | Entrega |
| :-- | :--- | :--- |
| 1 | Setup do módulo `auth-service` (pom, Dockerfile, application*.yml, `AuthApplication`) | Build Maven OK; bootstrap com Eureka/Qiila |
| 2 | Migration V1 + Entity `Login` | `login` criada via Flyway |
| 3 | Migration V2 + Entity `UserPermission` + `Role`/`RoleConverter` | RBAC base no schema |
| 4 | Migration V3 + Entity `TokenBlacklist` | Blacklist no schema |
| 5 | Migration V4 + Entity `AccessLog` + `AccessLogType` | `access_log` + índice |
| 6 | Repositórios Spring Data JPA (4) | Queries de busca/UI |
| 7 | `JwtService` + `JwtProperties` + filtro | Emissão/validação JWT |
| 8 | `SecurityConfig` + handlers REST | Regras de rota e RBAC |
| 9 | `AuthService` (login/logout/refresh/me) + `TokenBlacklistService` + cleanup | Fluxos de autenticação |
| 10 | `AccessLogService` + `RfidEventPublisher` + `RfidAccessEvent` | IEEE/validação RFID |
| 11 | `AuthController` (6 endpoints) + DTOs + exceptions | API REST completa |
| 12 | RabbitMQ wiring (`RabbitMqConfig`) + evento RFID | Publicação `access.rfid.event` |
| 13 | Testes (unit + integração + segurança + mensageria) | Cobertura ≥ 80% |
| 14 | Documentação (API.md, README, esta spec) | Docs atualizados |

---

## 4. Riscos e Mitigações

| # | Risco | Impacto | Mitigação |
| :-- | :--- | :--- | :--- |
| R1 | `JWT_SECRET` ausente/fraco (< 32 chars) | Boot falha | Falha rápida (`IllegalStateException`) + env/exemplo no Dockerfile |
| R2 | Token revogado ainda válido | Acesso indevido | Blacklist até `expiry_date` + check no filtro + cleanup diário |
| R3 | Refresh token roubado/reusado | Replay | Rotação: refresh antigo é blacklisted no `refresh` |
| R4 | Cartão RFID desconhecido/forçado | Ruído/registro errado | `ACESSO_NEGADO` com `id_user = null` + log de auditoria |
| R5 | Alternância ENTRADA/SAIDA inconsistente | Ponto errado no RH | `resolveType` baseado no último registro do cartão |
| R6 | Perda de mensagem RabbitMQ | Ponto não registrado no RH | Exchange tópica durável + consumidor RH (fora deste serviço) |
| R7 | Acoplamento `ddl-auto` vs Flyway | Schema divergente | `ddl-auto=validate` — Flyway é a única fonte |
| R8 | Cobertura < 80% | Qualidade | Suíte unit + integração + security + messaging (ver tasks 013) |
| R9 | `role` inválido (int fora de 0-4) | Corrupção RBAC | CHECK constraint + `RoleConverter` + `fromCode` |

---

## 5. Integrações

| Integração | Tipo | Direção | Contrato |
| :--- | :--- | :--- | :--- |
| Pessoas & RH | RabbitMQ (evento) | **Auth → RH** | `access.rfid.event` em `fablab.access`; `RfidAccessEvent{idUser, uuidRfid, timestamp, type}` |
| Pessoas & RH | RabbitMQ (evento) | **RH → Auth** | Eventos de nível/credencial (ex.: `nivel.alterado.event` reenvia para atualizar `login`/`user_permissions`) |
| Notification | RabbitMQ/REST | RH → Notification | Auth não notifica direto; Notification reage a eventos de RH |
| API Gateway | REST/JWT | **Gateway valida JWT** | Auth emite/verifica; Gateway bloqueia sem token válido |
| Eureka | Service Discovery | Auth → Eureka | Registro em `EUREKA_URI` (8761), `prefer-ip-address: true` |
# Requirements: Auth & Identity Service

> Fonte de verdade: **código real da branch `feature/auth-service`**. Onde este documento divergir de `docs/_archive/2026-10-19/prompts/specs.md`, prevalece o código real.
> Serviço: `auth-service` · porta `${PORT:8081}` · DB `fablab_auth` · Eureka 8761 · RabbitMQ · JWT HMAC SHA-256.

---

## 1. Objetivo e Escopo

O **Auth & Identity Service** é o serviço central de identidade, autenticação e autorização do FabLab. Responsável por:

- Autenticar usuários via e-mail ou nome de usuário + senha (BCrypt) e emitir tokens JWT.
- Controlar logout e revogação de tokens (blacklist até expiração).
- Validar leituras de cartão RFID (ESP32), registrar o acesso físico (ponto eletrônico) e publicar eventos para o Pessoas & RH Service.
- Prover controle de acesso baseado em papéis (**RBAC granular**), com níveis 0-4.
- Servir consultas de permissões por nível e dados do usuário autenticado.

**Dentro do escopo do MVP:** login, logout, refresh, validação RFID, registro de ponto, emissão/validação de JWT, RBAC, consulta de permissões.

**Fora do escopo:** ver §10.

---

## 2. Funcionalidades

| ID | Funcionalidade | Descrição |
| :-- | :--- | :--- |
| **F1** | Login | `POST /auth/login`. Autentica por `email` **ou** `nomeUsuario` + `senha`. Verifica hash BCrypt. Retorna par de tokens (access + refresh), tipo `Bearer`, TTL do access, `id_user`, `role`, `setor` e `nome_usuario`. Requer permissão ativa no `user_permissions`. |
| **F2** | Logout / Blacklist | `POST /auth/logout`. Revoga o token JWT atual (via header `Authorization: Bearer ...` ou body `token`), acrescentando-o à `token_blacklist` até sua expiração. |
| **F3** | Validação RFID (ESP32) | `POST /auth/validate-rfid`. Recebe `{uuid}` do cartão. Se reconhecido, registra `access_log` alternando `ENTRADA`/`SAIDA` (baseado no último registro) e publica `acesso.rfid.event`. Se desconhecido, registra `ACESSO_NEGADO` com `id_user = null` e retorna `allowed=false`. |
| **F4** | Emissão de JWT | Tokens HMAC SHA-256 (chave ≥ 32 caracteres), issuer `fablab`, claims `id_user`, `role`, `setor`, `type` (`access`/`refresh`), `sub` = id da tabela `login`, `iat`, `exp`. Access: 900s (15min). Refresh: 604800s (7 dias). |
| **F5** | Refresh | `POST /auth/refresh`. Renova o par de tokens com **rotação** (o refresh token antigo é invalidado na blacklist). Rejeita refresh de tipo errado (`type != refresh`), blacklisted ou expirado. |
| **F6** | Dados do usuário autenticado | `GET /auth/me`. Retorna `id`, `id_user`, `email`, `nomeUsuario`, `setor` e lista de permissões (`role`, `label`, `active`) do principal autenticado via JWT. |
| **F7** | Consulta de permissões por nível | `GET /auth/permissions?role=ADMIN`. Endpoint protegido (somente Admin). Retorna `{role, code, label, permissions}` da matriz RBAC. |
| **F8** | RBAC granular | Autorização por papel (0-4) aplicada via Spring Security + `@PreAuthorize`/`hasRole` e matriz de permissões por papel. Ver §7. |

---

## 3. Tabelas (Flyway — resposta)

### V1 — `login`
Criada em `V1__create_login_table.sql`.

| Campo | Tipo | Restrições |
| :--- | :--- | :--- |
| `id` | BIGSERIAL | PK |
| `id_user` | BIGINT | NOT NULL (referência ao usuário no RH) |
| `uuid` | VARCHAR(255) | NOT NULL, UNIQUE (`uk_login_uuid`) — cartão RFID |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE (`uk_login_email`) |
| `nome_usuario` | VARCHAR(255) | NOT NULL, UNIQUE (`uk_login_nome_usuario`) |
| `senha_hash` | VARCHAR(255) | NOT NULL (BCrypt) |
| `setor` | VARCHAR(255) | NULL |

### V2 — `user_permissions`
Criada em `V2__create_user_permissions_table.sql`.

| Campo | Tipo | Restrições |
| :--- | :--- | :--- |
| `id` | BIGSERIAL | PK |
| `id_user` | BIGINT | NOT NULL |
| `role` | INTEGER | NOT NULL, CHECK `role BETWEEN 0 AND 4` |
| `active` | BOOLEAN | NOT NULL, DEFAULT TRUE |

Índice: `ix_user_permissions_id_user` (`id_user`).

> **Importante:** não existe `responsabilidade` nesta tabela — responsabilidades são conceito do Pessoas & RH Service, não do Auth.

### V3 — `token_blacklist`
Criada em `V3__create_token_blacklist_table.sql`.

| Campo | Tipo | Restrições |
| :--- | :--- | :--- |
| `id` | BIGSERIAL | PK |
| `token` | TEXT | NOT NULL, UNIQUE (`uk_token_blacklist_token`) |
| `expiry_date` | TIMESTAMPTZ | NOT NULL |

### V4 — `access_log`
Criada em `V4__create_access_log_table.sql`.

| Campo | Tipo | Restrições |
| :--- | :--- | :--- |
| `id` | BIGSERIAL | PK |
| `id_user` | BIGINT | NULL (null em cartão não reconhecido / `ACESSO_NEGADO`) |
| `uuid_rfid` | VARCHAR(255) | NOT NULL |
| `timestamp` | TIMESTAMPTZ | NOT NULL |
| `type` | VARCHAR(32) | NOT NULL — `ENTRADA`, `SAIDA`, `ACESSO_NEGADO` |

Índice: `ix_access_log_uuid` (`uuid_rfid`, `timestamp DESC`).

**Não há `emailVer` no MVP** — nenhuma migração/tabela relacionada a verificação de e-mail.

---

## 4. Fluxos

### 4.1 Login
1. Cliente envia `POST /auth/login` com `email` (ou `nomeUsuario`) e `senha`.
2. `AuthService` busca `Login` por e-mail ou nome de usuário.
3. Se não encontrado **ou** `BCrypt.matches(senha, senha_hash)` falhar → `InvalidCredentialsException` (401).
4. Busca permissão ativa (`findFirstByIdUserAndActiveTrue`) em `user_permissions`. Se nenhuma → `ForbiddenException` (403).
5. Gera access token (15min) e refresh token (7 dias) via `JwtService` com claims `id_user`, `role`, `setor`.
6. Retorna `LoginResponse`.

### 4.2 RFID / ESP32
1. ESP32 envia `POST /auth/validate-rfid` com `{uuid}`.
2. `AuthService` busca `Login` por `uuid`.
3. **Cartão desconhecido:** registra `access_log` tipo `ACESSO_NEGADO` (`id_user = null`) e responde `allowed=false`.
4. **Cartão conhecido:** `AccessLogService.resolveType(uuid)` alterna `ENTRADA`/`SAIDA` conforme o último registro do cartão (sem anterior → `ENTRADA`).
5. Registra em `access_log` e publica `RfidAccessEvent` (`{idUser, uuidRfid, timestamp, type}`) na exchange `fablab.access`.
6. Responde com `allowed=true`, `type`, dados do usuário e timestamp.

### 4.3 RBAC / Autorização
1. Requisição chega ao Auth; `JwtAuthenticationFilter` valida o token (assinatura, issuer `fablab`, expiração, clock skew 5s) e popula o contexto.
2. Passa por checagem de rotas do `SecurityConfig` (públicas × protegidas).
3. Autorização por papel via `@EnableMethodSecurity` + `hasRole("ADMIN")` (ex.: `GET /auth/permissions`).
4. O **API Gateway** valida o JWT antes de rotear; o Auth firma e verifica.

---

## 5. Endpoints

Base: `/auth` · porta `8081`.

| Método | Rota | Autenticação | Corpo / Parâmetro | Resposta |
| :--- | :--- | :--- | :--- | :--- |
| POST | `/auth/login` | Pública | `LoginRequest{email?, nomeUsuario?, senha}` | `LoginResponse{accessToken, refreshToken, tokenType, expiresIn, idUser, role, setor, nomeUsuario}` |
| POST | `/auth/logout` | — (parse do token) | Header `Authorization` ou `LogoutRequest{token?}` | `LogoutResponse{message}` |
| POST | `/auth/validate-rfid` | Pública | `ValidateRfidRequest{uuid}` | `ValidateRfidResponse{allowed, message, idUser?, uuidRfid, nomeUsuario?, setor?, type, timestamp}` |
| GET | `/auth/me` | Bearer JWT | — | `MeResponse{id, idUser, email, nomeUsuario, setor, permissions[]}` |
| POST | `/auth/refresh` | Pública | `RefreshRequest{refreshToken}` | `RefreshResponse{accessToken, refreshToken, tokenType, expiresIn}` |
| GET | `/auth/permissions` | Bearer JWT + `hasRole("ADMIN")` | `?role=ADMIN` (default `ADMIN`) | `RbacResponse{role, code, label, permissions[]}` |

Rotas públicas no `SecurityConfig`: `POST /auth/login`, `POST /auth/validate-rfid`, `POST /auth/refresh`, `GET /actuator/health`. Demais autenticadas.

---

## 6. Integrações

| Parceiro | Sentido | Detalhe |
| :--- | :--- | :--- |
| **Pessoas & RH** (consumidor) | Auth → RH (RabbitMQ) | Auth publica `access.rfid.event` na exchange tópica `fablab.access` (routing key `access.rfid.event`). Evento `RfidAccessEvent{idUser, uuidRfid, timestamp, type}` usado pelo RH para registro de ponto. |
| **Pessoas & RH** (emissor) | RH → Auth | RH reenvia eventos de ponto/nível (`nivel.alterado.event`) que alteram/mantêm `login` e `user_permissions` (criação de credenciais e papéis). |
| **Notification** | RH → Notification | Notificação sobre eventos de acesso/nível (fora deste serviço; consumido pelo Notification). |
| **API Gateway** | Gateway ↔ Auth | Gateway valida JWT antes de rotear qualquer requisição dos serviços. Auth firma/verifica tokens e é fonte da identidade. |
| **Eureka** | Auth → Eureka | Registro/descoberta em `EUREKA_URI` (default `http://localhost:8761/eureka`). |

Eventos RabbitMQ: exchange `fablab.access` (TopicExchange, durável) configurada em `RabbitMqConfig`.

---

## 7. RBAC

### 7.1 Níveis
Papéis persistidos em `user_permissions.role` (inteiro 0-4):

| code | Role (enum) | Label |
| :--- | :--- | :--- |
| 0 | `ADMIN` | Admin |
| 1 | `BOLSISTA` | Bolsista |
| 2 | `VOLUNTARIO` | Voluntário |
| 3 | `ESTAGIARIO` | Estagiário |
| 4 | `RECRUTANDO` | Recrutando |

Conversão int ↔ enum via `RoleConverter`; campo `active` Boolean habilita/inabilita o papel.

### 7.2 Matriz granular (RbacService + docs/contract.gap.md §7)
| Papel | Permissões (`RbacService.MATRIX`) | Observações (matriz do sistema) |
| :--- | :--- | :--- |
| **ADMIN (0)** | `*` (acesso total) | Aprova/rejeita certificados; único acesso a **Financeiro**; aplica penalidades 5S; gestão/máquinas exclusivas. |
| **BOLSISTA (1)** | `catalogo:read`, `projetos:write`, `equipamentos:reserve`, `ponto:write`, `relatorios:export` | Participa do 5S; recebe extrato. |
| **VOLUNTARIO (2)** | `catalogo:read`, `equipamentos:read` | — |
| **ESTAGIARIO (3)** | `catalogo:read`, `equipamentos:read`, `ponto:write` | — |
| **RECRUTANDO (4)** | `catalogo:read` | **Não participa do 5S; não recebe extrato** (matriz de negócio, `docs/contract.gap.md` §7). |

Regras de negócio da matriz granular:
- Admin tem acesso total.
- Admin aprova/rejeita certificados.
- Apenas Admin acessa o módulo Financeiro (`/financeiro/**`).
- Recrutando não participa do 5S nem recebe extrato mensal de horas.

---

## 8. Requisitos Não-Funcionais

| # | Requisito |
| :-- | :--- |
| NFR-1 | Senhas armazenadas com **BCrypt** (`BCryptPasswordEncoder`); senha do login validada entre 6 e 72 caracteres. |
| NFR-2 | JWT assinado com **HMAC SHA-256**; `JWT_SECRET` obrigatório e ≥ 32 caracteres (impede boot caso contrário). |
| NFR-3 | Access token expira em **900s (15min)**; refresh em **604800s (7 dias)**; issuer `fablab`; clock skew 5s. |
| NFR-4 | Tokens de logout e refresh rotacionado ficam na **blacklist até a expiração**; limpeza diária (`@Scheduled` cron `0 0 3 * * *`). |
| NFR-5 | Ambiente local: **Docker + Docker Compose** (PostgreSQL `fablab_auth`, Eureka 8761, RabbitMQ) e **Tailscale** para acesso remoto. |
| NFR-6 | Cobertura de testes **≥ 80%** (unit + integração + segurança + mensageria). |
| NFR-7 | `access_log` serve como **log de auditoria** de acesso físico (uuid, usuário, horário, tipo). |
| NFR-8 | Sessões stateless (JWT), CSRF desabilitado, respostas de erro JSON (entry point/denial handler REST). |
| NFR-9 | `spring.jpa.hibernate.ddl-auto=validate` — schema sempre via Flyway, sem `ddl-auto` gerando tabelas. |

---

## 9. Critérios de Sucesso

1. `POST /auth/login` autentica usuário válido e retorna par de tokens; rejeita credenciais inválidas (401) e usuário sem permissão ativa (403).
2. `POST /auth/logout` revoga o token; usar o token revogado em `/auth/me` falha (blacklist).
3. `POST /auth/validate-rfid` registra `ENTRADA`/`SAIDA` alternados por cartão; cartão desconhecido gera `ACESSO_NEGADO` com `id_user = null`.
4. Publica `acesso.rfid.event` na exchange `fablab.access` a cada leitura válida (consumível pelo RH).
5. `POST /auth/refresh` rotaciona o par e invalida o refresh antigo.
6. `GET /auth/permissions` restrito a Admin; matriz por papel reflete §7.2.
7. Endpoint `/auth/me` retorna dados e permissões do usuário autenticado.
8. Cobertura de testes ≥ 80% e pipeline de build verde.
9. Migrations V1-V4 aplicadas via Flyway em DB limpo (`ddl-auto=validate`).

---

## 10. Fora de Escopo (MVP)

- **`emailVer`** (verificação de e-mail) — não existe no MVP.
- **Recuperação de senha** / reset de senha.
- **WhatsApp** (fica para o Notification Service, fora do auth).
- Cadastro/CRUD de usuários (feito pelo Pessoas & RH, que alimenta `login`/`user_permissions`).
- Responsabilidades de negócio (`responsabilidade`) — pertencem ao RH, não ao Auth.
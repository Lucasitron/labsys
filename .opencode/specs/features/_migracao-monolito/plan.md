# Plan — PIVOT Laravel: M0 (shared + scaffold `backend/`) + M1 (`auth`)

> Despacho do Orquestrador (2026-10-06) — **PIVOT DE LINGUAGEM por ordem expressa do PO/humano**:
> o backend será **PHP/Laravel**, não Java/Spring. Este plano **sobrescreve o plano Java anterior**
> (M0+M1 Spring Boot 3.4.5). Alvo: monólito modular MVC Laravel (`system/architecture.md`,
> `system/conventions.md` — já migrados p/ Laravel pelo humano; este Planner só conferiu e fez
> um ajuste fino no detalhe do índice V4). **Nunca alterar `docs/`; nenhum código agora — só planejar.**
> Estratégia: **reaproveitar o contrato, descartar o código Java.**

## 1. Estratégia contrato-preservado / código-descartado

1. **O que é reaproveitado (referência, não código):** `auth-service/src/**` (90 arqs) +
   `.opencode/specs/features/auth-service/{requirements.md,plan.md,tasks.md}` — rotas PT, tabelas
   V1–V8 (colunas/constraints/seeds), claims JWT, fluxos F1–F8, RBAC 0–4, eventos. Nada de Java é
   movido linha-a-linha: a reescrita é **idiomática Laravel** (Eloquent, FormRequest, Policy).
2. **O que é descartado:** todo o `backend/` Java atual (`pom.xml`, `src/**`, `target/`) —
   **descarte autorizado pelo PO neste despacho** (pivot de linguagem), registrado aqui para não
   constar como desperdício: scaffold M0+M1 Spring (não commitado, working tree sobre `main`) é
   substituído pelo scaffold Laravel no mesmo path `backend/`. A primeira ação do Builder é
   `rm -rf backend` (inclui `target/` — artefato de build, nunca versionado).
3. **Ordem:** T1 scaffold → T2 config → T3 migrations → T4 models → T5 policies/middleware →
   T6 controllers/requests/resources/routes → T7 services/actions → T8 events/listeners →
   T9 seed → T10 testes → T11 gate DoD. T4→T5→T6 encadeiam; T7 pode iniciar após T4.
4. **Ajustes de empacotamento permitidos (sem novo contrato):**
   - Base path `AuthController`: `/auth` (serviço Java) → `/api/auth` (contrato do front no
     monólito). Demais paths já estão no padrão e seguem **inalterados**: `/api/usuarios`,
     `/api/permissoes`, `/api/configuracoes/sistema`, `/api/configuracoes/tokens`.
   - `fablab_auth` (banco por serviço) → schema `auth` no banco único `fablab`
     (`Schema::create('auth.login', …)`, `protected $table = 'auth.login'`).
   - `RabbitTemplate.convertAndSend` → `Event`/`Listener` nativo (`ShouldQueue`, fila `database`),
     **mesmo nome** `access.rfid.event`, mesmo payload.
   - `SecurityConfig` + regras do gateway → middleware `auth:api` (JWT) + `can:`/`Policy`
     (fronteira antes feita pelo gateway).
   - Eureka/`discovery-service`, `gateway-service`, HTTP inter-serviços: **eliminados**, sem substituto
     (viram `Contract` in-process / `Event`).

## 2. Mapeamento contrato (Java/Spring) → Laravel

| Origem (`auth-service/`, referência) | Destino (`backend/`, Laravel) | Peça Laravel / observação |
| :--- | :--- | :--- |
| `pom.xml` + `AuthApplication` + `application*.yml` | `backend/` (scaffold `composer`, `bootstrap/app.php`, `.env`, `config/`) | T1+T2 — `create` (não há nada em PHP hoje) |
| `db/migration/V1–V8.sql` (Flyway) | `database/migrations/auth/*.php` + 1 migration `CREATE SCHEMA auth` | T3 — `Schema::create('auth.<tabela>')`; seeds V6/V7 viram seeders na própria migration ou `DatabaseSeeder` do módulo |
| `entity/Login`, `UserPermission`, `TokenBlacklist`, `AccessLog`, `SituacaoUsuario`, `PermissaoMatriz(+Id)`, `PermissaoNivel`, `Modulo`, `ParametroSistema`, `TokenIntegracao` (14 arqs) | `app/Modules/Auth/Models/*.php` (7 models) | T4 — `$table='<schema>.<tabela>'`, `$timestamps=false` (tabelas sem `created_at`), `$fillable` explícito, casts; `Role`/`AccessLogType`/`SituacaoUsuario` viram **enums PHP 8.1** (`int`/`string backed`); `RoleConverter`/`PermissaoMatrizId` **somem** (cast `AsEnum` + PK composta nativa) |
| `repository/*` (7 interfaces Spring Data) | **nada** — Eloquent direto nos Services/Actions | T4 — convenção do repo proíbe `Repository` por hábito (não planejar) |
| `service/JwtService` (HMAC, claims, 900s/604800s, issuer `fablab`, skew 5s) | `config/jwt.php` (`php-open-source-saver/jwt-auth`) + claims em `Login::getJWTCustomClaims()` | T2+T7 — ver decisão §6 (JWT, não Sanctum) |
| `service/AuthService` (login/logout/refresh/me/senha) | `app/Modules/Auth/Services/AuthService.php` (ou `Actions/`, 1 classe por fluxo se o Builder preferir) | T7 — regra de negócio só aqui |
| `service/TokenBlacklistService` + `TokenBlacklistCleanupTask` (`@Scheduled 0 0 3 * * *`) | blacklist via tabela `auth.token_blacklist` + purge em `routes/console.php` (`Schedule::dailyAt('03:00')`) | T7+T8 — `Schedule` nativo |
| `service/AccessLogService.resolveType` + `RfidEventPublisher` | `AccessLogService::resolveType()` + `event(new RfidAccessEvent(...))` | T7+T8 — alternância ENTRADA↔SAIDA preservada |
| `service/RbacService` (MATRIX em memória) + V6 seed | `RbacService` lendo `auth.permissao_matriz` (**verdade persistente**); MATRIX vira fallback/compat só-leitura | T7 — sem reescrita de regra |
| `service/UsuarioService`, `SistemaService`, `TokenIntegracaoService` | `Services/` do módulo (mesmas regras; token integração exibe claro 1×, persiste só hash+prefixo) | T7 |
| `mapper/LoginMapper` | **nada** — API `Resource` (`Http/Resources/`) faz o shape | T6 — não planejar mapper |
| `dto/*` (18 records + Bean Validation) | `Http/Requests/*FormRequest` (entrada/validação) + `Http/Resources/*` (saída) | T6 — `FormRequest` valida+autoriza; senha 6–72 chars, email opcional |
| `config/SecurityConfig` + `@PreAuthorize hasRole('ADMIN')` + `JwtAuthenticationFilter` | middleware `auth:api` + `Policies/` + `Gate`/`can:` nas rotas (rbac-matrix.md) | T5+T6 — `verify` de JWT pelo guard `api` |
| `exception/*` (8) + `GlobalExceptionHandler` + `ErrorResponse` | `app/Shared/Exceptions/` (Handler + `ErrorResponse`) — 401/403/404/400 JSON | T2 (shared) — único ponto, sem `exception/` por módulo |
| `dto/RfidAccessEvent` + `RabbitMqConfig` (exchange `fablab.access`) | `app/Modules/Auth/Events/RfidAccessEvent.php` + `Listeners/` (`ShouldQueue`, fila `database`); Rabbit **só** se contrato exigir entrega durável real (não é o caso M1) | T8 |
| `config/AuthSeedRunner` + `SeedProperties` (`admin@fablab.org`/`admin`/`admin123` BCrypt + role ADMIN) | `database/seeders/AuthSeeder.php` dev-only (`AUTH_SEED_ENABLED=true`, idempotente, nunca loga credencial) | T9 |
| Testes (unit/integration/security/mensageria) | `tests/Unit` + `tests/Feature` (`php artisan test`, PHPUnit/Pest) + 401/403 por rota | T10 — cobertura ≥ 80% |

Rotas finais (PT, contrato com o front): `POST /api/auth/login`, `POST /api/auth/logout`,
`POST /api/auth/validate-rfid`, `GET /api/auth/me`, `POST /api/auth/refresh`,
`GET /api/auth/permissions?role=ADMIN`, `PUT /api/auth/senha`, `GET/POST /api/usuarios`,
`PUT /api/usuarios/{id}`, `PATCH /api/usuarios/{id}/status`, `GET /api/permissoes`,
`PUT /api/permissoes/{modulo}/{nivel}`, `GET/PUT /api/configuracoes/sistema`,
`GET/POST /api/configuracoes/tokens`, `DELETE /api/configuracoes/tokens/{id}`.

## 3. Decisões de enxugamento (código mínimo obrigatório)

**Reusado do contrato (regra preservada, roupagem Laravel):**
- Tabelas/colunas/constraints/seeds V1–V8 **idênticos** (só qualificados `auth.`); sem `responsabilidade`
  em `user_permissions` (é do RH — requirements §3/V2); `token_integracao` sem segredo em claro.
- Fluxos F1–F8, TTLs 900s/604800s, issuer `fablab`, claims `id_user/role/setor/type`, rotação de refresh
  com blacklist do antigo, alternância RFID, `ACESSO_NEGADO` com `id_user=null`, matriz granular §7.2.
- `Hash::make` (bcrypt) no lugar de `BCryptPasswordEncoder`; `Schedule` no lugar de `@Scheduled`;
  `Event/Listener` no lugar de `RabbitTemplate`; Eloquent no lugar de JPA+repositories.
- `JacksonConfig` não tem equivalente — JSON é nativo no Laravel (nada a planejar).

**`create` justificado (só o que não existe e o requirements/DoD exige):**
1. Scaffold `backend/` Laravel + `composer.json` + `.env` + `config/` — sem isso não há app (T1–T2).
2. Migrations `database/migrations/auth/` — schema único exige reescrita em PHP (T3).
3. `Contract` público do Auth (`app/Modules/Auth/Contracts/` ou `Shared/Contracts`) — fronteira
   in-process exigida pela arquitetura; não existe no serviço Java (era HTTP/gateway) (T8, junto aos eventos).
4. Slice de testes security 401/403 por rota — DoD exige; equivalente parcial no Java não é reusável (T10).

**Explicitamente NÃO planejado (fora de escopo / especulativo):**
- `Repository/` por módulo, `Mapper/`, `DTO/` mutáveis, `config/exception` por módulo, `api/` sem
  consumidor, re-export/helper "futuro", `emailVer`/recuperação de senha/WhatsApp (requirements §10),
  Eureka/Gateway/banco-por-serviço/HTTP entre módulos, `Dockerfile` por serviço, Rabbit além do
  necessário (fila `database` cobre M1), campo `responsabilidade` em `user_permissions`.

## 4. Ordem de execução (topológica)

```
T1 scaffold ─→ T2 config ─→ T3 migrations ─→ T4 models ─┬→ T5 policies/middleware ─┐
                                                         ├→ T7 services ─→ T8 events ├→ T9 seed ─→ T10 testes ─→ T11 gate
                                                         └→ T6 controllers/requests/resources/routes ──────────────┘
```

T6 exige T4 (models) + T5 (auth/RBAC nas rotas). T7 exige T4 (+T2 p/ JWT). T8 exige T7 (payload do
evento sai do service). T10 exige tudo implementado. T11 é o gate M0+M1.

## 5. Riscos e mitigações

| # | Risco | Mitigação (task) |
| :--- | :--- | :--- |
| R1 | `backend/` Java ainda na working tree — Builder pode tentar reaproveitar/mergear | T1: `rm -rf backend` explícito como passo 1; descarte PO-registrado (§1.2) |
| R2 | Sem `php`/`composer` nativos; rede limitada pode quebrar `composer create-project` | T1: scaffold **via container Docker** (`docker run --rm php:8.3/composer`); **fallback manual** (composer.json + estrutura mínima escritos à mão, `composer install` quando houver rede) — decisão detalhada em T1 |
| R3 | Schemas PG (`auth.*`): migration sem `CREATE SCHEMA` ou model sem `$table` qualificada quebra tudo | T3: primeira migration cria schemas; T4: `$table` qualificada + `$timestamps=false`; T11 valida em DB limpo |
| R4 | JWT com claims custom + blacklist + rotação não é o default Sanctum | §6/T2: pacote `jwt-auth` (fork mantido); T10 cobre expiração/blacklist/rotação/claims |
| R5 | `JWT_SECRET` fraco/ausente | T2: fail-fast no boot (`strlen < 32` → exceção), secret só via `.env` (nunca versionado) |
| R6 | Cobertura ≥ 80% em PHP exige driver (pcov/xdebug) no container de teste | T10: imagem de teste com `pcov`; pré-condição do Builder (§7 relatório) |
| R7 | `infra/docker-compose.yml` defaulta `POSTGRES_DB=fablab_auth` (banco por serviço) | Pré-condição p/ Deployer: alinhar p/ `fablab` (fora deste plano; registrado no relatório) |
| R8 | `permissao_matriz` seed (V6) vs MATRIX em memória divergem | T7: tabela é a verdade; MATRIX só-leitura/compat |
| R9 | `situacao != ATIVO → 403` aplicado cedo demais (migration-time) bloqueia login | T7: regra só no service, nunca na migration/seed |

## 6. Decisão de autenticação: JWT (`php-open-source-saver/jwt-auth`), não Sanctum

O contrato exige **JWT stateless HMAC** com claims `id_user/role/setor/type`, issuer `fablab`,
access 900s + refresh 7 dias com **rotação**, **blacklist até expiração** e purge 03:00. Sanctum
(PAT opaco em banco, sem claims/TTL por tipo) **não preserva esse contrato sem reescrevê-lo** —
violaria "reuse de contrato > recriar regra". O mecanismo da plataforma para JWT com claims é o
guard `jwt` via pacote `php-open-source-saver/jwt-auth` (fork mantido do `tymon/jwt-auth`,
compatível Laravel 11/12). Sanctum fica **fora de escopo** salvo se o Builder provar
incompatibilidade do pacote — nesse caso, escalar ao Orquestrador (não improvisar auth própria).

---

# M2 — módulo `rh` (Pessoas & RH) — estratégia de migração Java → Laravel

> Despacho do Orquestrador (2026-10-06). **Estende M0+M1 sem sobrescrever.** Referência de regra
> (reescrever em PHP, nunca mover Java): `rh-service/src/main/java/com/fablab/rh/**` (~152 arqs:
> 8 controllers, 58 DTOs, 22 entities, 13 repositories, 13 services, 6 mappers, messaging, security,
> config) + migrations `V1–V12+V14` + `specs/features/rh-service/requirements.md` (F1–F9).
> Base pronta: `App\Modules\Auth` (`AuthContract`, evento `access.rfid.event`) + `App\Shared`.
> **Nunca `docs/`; nenhum código agora; não commitar.**

## M2.1. Estratégia contrato-preservado / código-descartado

1. **Reaproveitado (regra, não código):** tabelas/colunas/constraints V1–V12+V14 (com o gap V13
   preservado — não existe upstream), fluxos F1–F9, RBAC por endpoint (requirements §RBAC +
   `rbac-matrix.md`), máscara LGPD de CPF, distinção Tutor×Instrutor, matriz de níveis Admin,
   convites, PS com grupos/estágio/avaliação, regra de horas (horário+motivo+fachada+coerência),
   consolidação de certificados, scheduler `0 0 6 1 * *` (Spring, 6 campos) → `0 6 1 * *`
   (Laravel, 5 campos), nomes dos 6 eventos preservados.
2. **Descartado:** todo o Java (`entity/repository/service/mapper/config` Spring, `RabbitTemplate`,
   `@Scheduled`, `@PreAuthorize`, Eureka, Flyway). Vira Eloquent/`FormRequest`/`Resource`/
   `Policy`/`Event`/`Schedule` idiomáticos.
3. **Ordem:** R1 migrations → R2 models/enums → R3 policies → R4–R5 controllers/requests/resources
   → R6–R8 services → R9 events/listeners+contract → R10 scheduler → R11 seeders → R12 testes →
   R13 gate. R4–R5 exigem R2+R3; R6–R8 exigem R2 (+R9 p/ payloads); R12 exige tudo.
4. **Ajustes de empacotamento permitidos (sem novo contrato):**
   - Base path `/pessoas`, `/funcionarios`, … (serviço Java) → `/api/rh/**` (contrato do front).
   - Banco `fablab_rh` → schema `rh` no banco único `fablab` (`rh.pessoa`, `$table='rh.pessoa'`).
   - `RabbitTemplate.convertAndSend` (exchange `fablab.rh`/`fablab.access`) → `Event`/`Listener`
     nativo (`ShouldQueue`, fila `database`), **mesmo nome** de evento e mesmo payload.
   - `SecurityConfig` + `@PreAuthorize` → `auth:api` + `blacklist` + `can:`/`Policy` (mesma matriz).
   - `JwtService`/`JwtAuthenticationFilter` do RH **somem**: JWT é validado pelo guard `api` do Auth
     (M1); o RH lê identidade via `AuthContract::verify()` / `auth()->user()`.
   - `@Scheduled(cron="0 0 6 1 * *")` → `Schedule::cron('0 6 1 * *')` em `routes/console.php`
     (conversão 6→5 campos documentada; flag `rh.certificado.extrato.scheduler.enabled` vira
     config `rh.extrato_scheduler_enabled`, default on).
   - `searchPeople` (fila backend no Java) → query Eloquent com `where LIKE` + paginação
     (`PessoasPaginaResponse` → `Resource` paginado); sem fila dedicada.
   - `RestAccessDeniedHandler`/`RestAuthenticationEntryPoint`/`GlobalExceptionHandler` do RH
     **somem**: Handler JSON central do `Shared` (M0) já cobre.

## M2.2. Mapeamento contrato (Java/Spring) → Laravel

| Origem (`rh-service/`, referência) | Destino (`backend/`, Laravel) | Peça / observação |
| :--- | :--- | :--- |
| `db/migration/V1–V12+V14.sql` (Flyway) | `database/migrations/rh/*.php` | R1 — `Schema::create('rh.<tabela>')`; V11 CPF (nullable+UK), V12 grupo/nota/feedback, V14 hora_inicio/fim+motivo; gap V13 inexistente (não inventar) |
| `entity/*` (22 arqs: 13 entidades + converters + enums) | `app/Modules/Rh/Models/*.php` (13) + `Enums/` | R2 — `$table='rh.*'`, `$timestamps=false`, `$fillable`, casts; `NivelAcessoConverter`/`PessoaStatusConverter` somem (cast enum); enums viram `int`/`string backed` PHP 8.1 |
| `repository/*` (13 interfaces Spring Data) | **nada** — Eloquent direto | R2 — proibido `Repository` por hábito (não planejar) |
| `mapper/*` (6) + `dto/*Response/*Request` (58) | `Http/Requests/*FormRequest` (entrada) + `Http/Resources/*` (saída) | R4–R5 — `FormRequest` valida+autoriza; `Resource` dá o shape (incl. CPF mascarado, paginação, facetas) |
| `dto/CpfUtil` + `AutorizacaoHelper` + `RhPrincipal` | `Rules/CpfRule` (ou `Validator::extend`) + `Policy` + `auth()->user()` | R2/R3 — util vira rule de validação; helper vira `Policy`; `RhPrincipal` some (guard JWT do Auth) |
| `config/SecurityConfig` + `@PreAuthorize` (8 controllers) | `auth:api`+`blacklist`+`Policies/`+`can:` | R3 — matriz requirements §RBAC ∩ `rbac-matrix.md`; enforcement server-side + próprio-registro (`visualizar (próprio)`) na `Policy`, nunca no front |
| `service/PessoaService` (CPF/LGPD, search, detalhe, facetas) | `Services/PessoaService.php` | R6 — CPF opcional/único/mascarado na saída; `searchPeople` = Eloquent LIKE+paginate |
| `service/FuncionarioService` (vínculo, nível, `totalHoras`/coerência) | `Services/FuncionarioService.php` | R6 — coerência presença×(encomenda+projeto) por dia preservada (endpoint p/ Financeiro) |
| `service/NivelService` (matriz, alterarMembro, convidar) | `Services/NivelService.php` | R6 — só Admin altera (Policy); grava `historico_nivel`; convite cria pessoa+funcionário nível 4 |
| `service/PontoService` + `RfidEventListener` | `Services/PontoService.php` + Listener de `access.rfid.event` | R6+R9 — entrada/saída + `total_horas`; idempotente por `(id_funcionario,data)` |
| `service/ApontamentoHorasService` + `HorasController` (fachada) | `Services/ApontamentoService.php` (+ fachada embutida, sem classe extra) | R7 — nasce PENDENTE; horário (hora_inicio/fim) recalcula `horas_trabalhadas`; motivo obrigatório na rejeição |
| `service/ExtratoService` + `ExtratoMensalScheduler` | `Services/ExtratoService.php` + `Schedule::cron('0 6 1 * *')` | R7+R10 — mês anterior fechado; exceto nível 4; publica 1 evento por funcionário ativo |
| `service/ProcessoSeletivoService` (grupos/estágio/avaliação) | `Services/ProcessoSeletivoService.php` | R8 — candidato = pessoa+funcionário nível 4 + status Recrutando |
| `service/TreinamentoService` | `Services/TreinamentoService.php` | R8 — nota 0–10 + feedback; tutor = vínculo `tutor` (não "instrutor" genérico) |
| `service/CertificadoService` (disponíveis, aprovar/rejeitar, consolidar) | `Services/CertificadoService.php` | R8 — disponíveis = VALIDADOS ∧ ¬consolidado; aprovar consolida TODAS (sem parcial no MVP), UUID `codigo_verificacao`, `hora_consolidada`; rejeitar mantém disponíveis |
| `service/RhEventPublisher` (6 `convertAndSend`) | `Events/*.php` (`ShouldQueue`, fila `database`) | R9 — nomes `nivel.alterado.event`, `horas.validadas.event`, `certificado.{solicitado,aprovado,rejeitado}.event`, `extrato.mensal.horas.event` preservados |
| `config/RabbitMqConfig` (exchanges/filas) | **nada** (fila `database` nativa) | R9 — Rabbit só se contrato exigir entrega externa real (não é o caso M2) |
| `config/*Seed*` (se houver) | `database/seeders/RhSeeder.php` dev-only **se** o contrato exigir | R11 — sem seed V1–V12+V14 no contrato → tarefa de verificação, sem seed inventado |
| Testes (unit/integration/security) | `tests/Unit/Rh/*` + `tests/Feature/Rh/*` + slice 401/403 + LGPD/CPF | R12 — `php artisan test`, cobertura ≥ 80% |

Rotas finais (PT, `/api/rh/**` exatas — ver R4/R5): `pessoas` (CRUD+detalhe), `funcionarios`
(+`/{id}/nivel`, +`/{id}/horas`), `niveis` (+`/{id}/membros`, +`/convites`),
`apontamentos-horas` (+`/{id}/validar`), `horas` (fachada: `/disponiveis`, `GET/POST`,
`/{id}/validar`, `/{id}/rejeitar`), `processo-seletivo` (+`/grupos`, +`/{id}/estagio`,
+`/{id}/membros/{pessoaId}/avaliar`), `treinamentos` (+`/{id}/avaliacoes`),
`certificados` (`/solicitar`, `/solicitacoes`, `/solicitacoes/{id}/aprovar|rejeitar`,
`/emitidos`, `/emitidos/{id}`), `extrato-mensal-horas`.

## M2.3. Decisões de enxugamento (código mínimo obrigatório)

**Reusado do contrato (regra preservada, roupagem Laravel):**
- Tabelas/colunas/constraints/índices/UKs V1–V12+V14 idênticos (só qualificados `rh.`); CPF
  nullable+UK; `consolidado` default false; `hora_inicio/fim`+`motivo_rejeicao`; grupo/nota/feedback.
- Fluxos F1–F9, matriz RBAC por endpoint, validações (matrícula única, CPF dígitos, nota 0–10,
  enums via CHECK), alternância RFID, coerência de horas, consolidação total no MVP, UUID de
  verificação, exclusão de Recrutandos do extrato.
- `Hash`/bcrypt n/a (RH não guarda senha); `Schedule` no lugar de `@Scheduled`; `Event/Listener`
  no lugar de `RabbitTemplate`; Eloquent no lugar de JPA+repositories; `Resource` no lugar de
  mappers; `FormRequest` no lugar de records+Bean Validation.

**`create` justificado (só o que não existe e o requirements/DoD exige):**
1. Migrations `database/migrations/rh/` — schema único exige reescrita em PHP (R1).
2. `CpfRule` (validação de dígitos) — sem equivalente nativo no Laravel (R2).
3. `Contract` público do RH (`Contracts/RhContract.php`) — fronteira in-process exigida pela
   arquitetura p/ Financeiro (coerência/horas) e Produção (funcionários/responsáveis); não existe
   no Java (era REST/Eureka) (R9).
4. Slice de testes security 401/403 por rota + LGPD/CPF — DoD exige; Java não é reusável (R12).

**Explicitamente NÃO planejado (fora de escopo / especulativo):**
- `Repository/`, `Mapper/`, `DTO/` mutáveis, `config/exception` do módulo, `JwtService`/
  `JwtAuthenticationFilter` próprios do RH, `api/` sem consumidor, re-export/helper "futuro".
- Geração de PDF/templates `.tex` de certificado/extrato, endpoint público de verificação de
  autenticidade (requirements §Fora de Escopo), cálculo financeiro de custo (Financeiro consome
  eventos), recuperação de senha/WhatsApp, Eureka/Gateway/banco-por-serviço/HTTP entre módulos,
  Rabbit além do necessário, campo `responsabilidade` (é granular do Auth — fora do RH),
  certificação parcial (rejeitada pelo MVP: consolida TODAS), seed inventado (sem seed no contrato).

---

# M3 — módulo `estoque` (Estoque & Suprimentos) — estratégia de migração Java → Laravel

> Despacho do Orquestrador (2026-10-06). **Estende M0+M1+M2 sem sobrescrever.** Referência de regra
> (reescrever em PHP, nunca mover Java): `estoque-service/src/main/java/com/fablab/estoque/**`
> (~96 arqs: 7 controllers, 25 DTOs, 12 entities/enums, 8 repositories, 9 services, 7 mappers,
> rabbit, scheduler, config) + migrations `V1–V9` + `specs/features/estoque-service/requirements.md`
> (F1–F9). Base pronta: `App\Modules\Auth` (`AuthContract`), `App\Modules\Rh` (`RhContract`,
> pessoa p/ fail-soft E-4), `App\Shared`, padrões M0–M2 (PHP 8.3/Laravel 12, PHPUnit pgsql
> `fablab_testing`, Handler JSON, blacklist, Gate admin, migrator com glob de subdirs,
> `DB_SEARCH_PATH`). **Nunca `docs/`; nenhum código agora; não commitar.**

## M3.1. Estratégia contrato-preservado / código-descartado

1. **Reaproveitado (regra, não código):** tabelas/colunas/constraints/índices/UKs **V1–V9** (ordem
   `localizacao → item → fornecedor → entrada_estoque → saida_estoque → emprestimo →
   lista_materiais → item_bom`, +V9 `responsavel` nas 3 movimentações) — **drift registrado:**
   `system/architecture.md` §Persistência lista V1–V8 do estoque; o upstream real tem **V9**
   (`responsavel`), que prevalece (não tocar em `docs/`, só registrar aqui); fluxos F1–F9;
   validações (quantidades/valores positivos, CNPJ, datas, enums via CHECK); payloads e nomes dos
   3 eventos emitidos; cron scheduler 03:00 (Spring 6 campos `0 0 3 * * *` → Laravel 5 campos
   `dailyAt('03:00')`); idempotência da baixa por `(tipo_saida=CONSUMO, id_referencia)`;
   formato CSV `;` com cabeçalho PT; regra E-7 (Admin vê todos os empréstimos, demais só os
   próprios); rótulo fail-soft E-4 (`Pessoa #id`).
2. **Descartado:** `entity/repository/service/mapper/config` Spring, `RabbitTemplate`/`RabbitConfig`,
   `RestClient`+`DiscoveryClient` (`RhPessoaClient` → `RhContract`), `@PreAuthorize`/`SecurityConfig`,
   `JwtService` próprio (JWT é o guard `api` do Auth/M1), `EstoquePrincipal` (vira
   `auth()->user()` + `AuthContract::roleOf/isAdmin`), `GlobalExceptionHandler` próprio (Handler
   JSON central do `Shared`), Eureka/Flyway/banco-por-serviço.
3. **Ordem:** E1 migrations → E2 models/enums → E3 policies → E4–E5 controllers/requests/resources
   → E6–E8 services → E9 events/listeners+contract → E10 scheduler → E11 seeders → E12 testes →
   E13 gate. E4–E5 exigem E2+E3; E6–E8 exigem E2 (+E9 p/ payloads); E12 exige tudo.
4. **Ajustes de empacotamento permitidos (sem novo contrato):**
   - Base paths `/itens`, `/entradas`, … (serviço Java) → `/api/estoque/**` (contrato do front).
   - Banco `fablab_estoque` → schema `estoque` no banco único `fablab`
     (`Schema::create('estoque.item', …)`, `$table='estoque.item'`).
   - `RabbitTemplate.convertAndSend` (exchange `fablab.estoque`) → `Event` nativo (`ShouldQueue`,
     fila `database`), **mesmo nome** de evento e mesmo payload; `ConsumoProducaoListener`
     (`@RabbitListener`) → `Listener` do evento `producao.concluida.event` delegando ao **mesmo**
     service idempotente (produtor real chega em M6/Produção).
   - `SecurityConfig` + `@PreAuthorize` → `auth:api` + `blacklist` + `Policy`/`can:` — com **um
     delta consciente:** a matriz `rbac-matrix.md` (fonte sobreposta) **prevalece** onde diverge do
     Java: `EDICAO = ADMIN+BOLSISTA` excluía o Voluntário da escrita, mas a matriz dá
     `Editar (se responsável)` a Bolsista **e** Voluntário em itens/entradas/saídas/empréstimos —
     vale a matriz, com guarda de vínculo `usuario_responsabilidade` (E-3/responsável por estoque)
     + `canSeeLoans` E-7 nos empréstimos.
   - `findByIdForUpdate` (`PESSIMISTIC_WRITE`) → `lockForUpdate()` dentro de `DB::transaction`;
     coluna `versao` (@Version) **preservada na migration 1:1**, sem maquinaria optimistic extra
     (lock pessimista transacional cobre o MVP).
   - `RhPessoaClient` (REST/Eureka, timeout 1s/2s) → `RhContract::nomePessoa()` in-process,
     fail-soft mantido (E-4), **sem join cross-schema**.
   - `searchPeople`-style: filtros de `GET /itens` (categoria/localização/baixo) e `?idItem=` de
     entradas/saídas e `?projetoId=` de BOMs viram query Eloquent + paginação; **sem `?status=` em
     entradas/saídas** (E-1 minimal: front não consome — fora salvo ordem expressa); empréstimos
     **mantêm** `?status=ativos|atrasados|historico` + `GET /atrasados` (contrato Java + tela).

## M3.2. Mapeamento contrato (Java/Spring) → Laravel

| Origem (`estoque-service/`, referência) | Destino (`backend/`, Laravel) | Peça / observação |
| :--- | :--- | :--- |
| `db/migration/V1–V9.sql` (Flyway) | `database/migrations/estoque/*.php` | E1 — `Schema::create('estoque.<tabela>')`; V2 `item` com `versao` default 0 + CHECK categoria; V5 CHECK tipo; V6 CHECK status; V8 UK `(id_bom,id_item)`; V9 `responsavel` nullable nas 3 movimentações |
| `entity/*` (8 entidades + `Categoria/StatusEmprestimo/TipoSaida/NivelAcesso`) | `app/Modules/Estoque/Models/*.php` (8) + `Enums/` (3) | E2 — `$table='estoque.*'`, `$timestamps=false`, `$fillable`, casts decimal; enums viram `string backed` PHP 8.1; `NivelAcesso` **some** (reuse `Auth\Enums\Role`); `@Version` vira coluna `versao` simples |
| `repository/*` (8 interfaces, incl. `findByIdForUpdate` + `@Lock`) | **nada** — Eloquent `lockForUpdate()` em `DB::transaction` | E2/E6–E8 — proibido `Repository` por hábito (não planejar) |
| `mapper/*` (7) + `dto/*Request/*Response` (25) + `ErrorResponse` | `Http/Requests/*FormRequest` (entrada) + `Http/Resources/*` (saída, incl. `tomador` E-4) | E4–E5 — `FormRequest` valida+autoriza; `Resource` dá o shape; `ErrorResponse` some (Handler `Shared`) |
| `dto/EstoquePrincipal` + `NivelAcesso` | **nada** — `auth()->user()` + `AuthContract` | E3 — `isAdmin()` vira `Gate admin`/`AuthContract::isAdmin()`; `idPessoa` (claim `id_user`) lido do guard |
| `config/SecurityConfig` + `EDICAO/VISUALIZACAO/OPERACAO` (7 controllers) | `auth:api`+`blacklist`+`Policies/EstoquePolicy`+`can:`/`Gate` | E3 — matriz `rbac-matrix.md` ∩ requirements §RBAC, com delta Voluntário-documentado em §M3.1.4; Recrutando (4) sem acesso ao módulo |
| `service/ItemService` (CRUD/filtros/CSV/consumo/idempotência) | `Services/ItemService.php` | E6 — filtros Eloquent; CSV `;` com **sanitização anti-injection no export**; `baixarPorConsumo` idempotente por `(CONSUMO,id_referencia)` |
| `service/EntradaService` (soma saldo, `valor_total`, compra+alerta) | `Services/EntradaService.php` | E6 — `valor_total = quantidade × valor_unitario` no service; publica `compra.solicitada.event` + checa `estoque.baixo` |
| `service/SaidaService` (subtrai com lock, nunca negativo) | `Services/SaidaService.php` | E6 — `lockForUpdate` + `novoSaldo < 0 → 409/422`; checa `estoque.baixo` |
| `service/EmprestimoService` (criar p/ si, devolver, E-7, atraso, E-4) | `Services/EmprestimoService.php` | E7 — não-admin só p/ si; devolução soma saldo com lock; `verificarAtrasados()` ATIVO+vencido→ATRASADO; `toResponse` com `RhContract::nomePessoa ?? "Pessoa #id"` |
| `service/BomService` (versão incremental, consumo→baixa) | `Services/BomService.php` | E8 — `atualizar` incrementa `versao` (default `atual+1`), remove/adiciona `item_bom`; `registrarConsumo` preenche `quantidade_real` + baixa via `ItemService::registrarConsumo` (`id_referencia` = id da BOM) |
| `service/FornecedorService`, `LocalizacaoService` | métodos no `Model` ou `Services` triviais | E6 — CRUD simples **sem service forçado** (regra trivial pode ficar no `Model`/controller delegando ao Eloquent; Builder escolhe, sem duplicar) |
| `service/RhPessoaClient` (REST fail-soft) | `RhContract::nomePessoa()` (aditivo) + fallback no service | E9 — **modify** no `Rh` (método novo, sem quebra); sem HTTP, sem join cross-schema |
| `rabbit/EstoqueEventPublisher` (3 `convertAndSend`) | `Events/*.php` (`ShouldQueue`, fila `database`) | E9 — nomes `estoque.baixo.event`, `emprestimo.atrasado.event`, `compra.solicitada.event` + payloads preservados |
| `rabbit/ConsumoProducaoListener` (baixa idempotente) | `Listeners/ConsumoProducaoListener.php` do evento `producao.concluida.event` | E9 — delega a `ItemService::baixarPorConsumo()` (mesma idempotência); produtor real em M6 |
| `scheduler/EmprestimoAtrasadoScheduler` (`0 0 3 * * *` + flags) | `Schedule::call(...)->dailyAt('03:00')` + `config/estoque.php` | E10 — flag `estoque.emprestimo_scheduler_enabled` (default on, equivale a `estoque.alerta.emprestimo.scheduler.enabled`) |
| `JwtService/JwtProperties` + `RabbitConfig` + `RestAccessDeniedHandler` | **nada** | E9 — JWT é do Auth; fila `database` cobre M3; Handler é do `Shared` |
| seeds (inexistentes em V1–V9) | **nada** (verificação E11) | E11 — sem seed inventado |
| Testes (unit/integration/concorrência) | `tests/Unit/Estoque/*` + `tests/Feature/Estoque/*` + slice 401/403 + concorrência | E12 — `php artisan test` (pgsql `fablab_testing`), cobertura ≥ 80% |

Rotas finais (PT, `/api/estoque/**` exatas — ver E4/E5): `itens` (`POST/GET` +filtros,
`GET /{id}`, `PUT /{id}`, `GET /export`, `POST /import`), `entradas` (`POST/GET` `?idItem=`,
`GET /{id}`), `saidas` (`POST/GET` `?idItem=`, `GET /{id}`), `emprestimos` (`POST/GET`
`?status=`, `GET /atrasados`, `GET /{id}`, `PUT /{id}/devolucao`), `fornecedores`
(`POST/GET`), `localizacoes` (`GET/POST`), `boms` (`POST/GET` `?projetoId=`, `GET /{id}`,
`PUT /{id}`, `POST /{id}/consumo`).

## M3.3. Decisões de enxugamento (código mínimo obrigatório)

**Reusado do contrato (regra preservada, roupagem Laravel):**
- Tabelas/colunas/constraints/índices/UKs V1–V9 idênticos (só qualificados `estoque.`); `versao`
  em `item` (default 0); `valor_total` calculado; `data_entrada` default hoje; `data_saida`
  `LocalDateTime.now()`; empréstimo gera `saida_estoque` `EMPRESTIMO` com `id_referencia` =
  id do empréstimo; devolução soma saldo e marca DEVOLVIDO; atraso = ATIVO ∧ prevista vencida ∧
  real nula; baixa usa `quantidade_real` (BOM) e é idempotente por referência; alerta baixo =
  `quantidade_atual <= estoque_minimo` após qualquer movimentação.
- RBAC por endpoint (§RBAC requirements) **com o delta Voluntário da matriz** (§M3.1.4,
  consciente e reversível); E-7/`canSeeLoans` server-side; Recrutando fora do módulo.
- `Hash`/senha n/a; `Schedule` no lugar de `@Scheduled`; `Event/Listener` no lugar de
  `RabbitTemplate`; Eloquent+`lockForUpdate` no lugar de JPA+`@Lock`; `Resource` no lugar de
  mappers; `FormRequest` no lugar de records+Bean Validation.

**`create` justificado (só o que não existe e o requirements/DoD exige):**
1. Migrations `database/migrations/estoque/` — schema único exige reescrita em PHP (E1).
2. `RhContract::nomePessoa()` (aditivo, `Rh` `modify`) — fronteira in-process p/ E-4 exigida
   pela arquitetura; não existe no Java (era REST/Eureka) (E9).
3. `Contracts/EstoqueContract.php` — fronteira in-process exigida pela arquitetura p/ M4
   (Vendas: `itemExiste`/`saldoDe`) e M6 (Produção: `baixarConsumo` idempotente); não existe no
   Java (era REST/Eureka) (E9). **Congelado em 3 métodos** — só o consumido.
4. Sanitização anti-CSV-injection no export — sem equivalente no Java (vulnerabilidade real:
   células iniciadas por `=,+,-,@`/tab ganham prefixo `'`); regra mínima dentro de E6.
5. Slice de testes security 401/403 por rota + concorrência sem-negativo — DoD exige; Java não
   é reusável (E12).

**Explicitamente NÃO planejado (fora de escopo / especulativo):**
- `Repository/`, `Mapper/`, `DTO/` mutáveis, `config/exception` do módulo, `JwtService` próprio,
  `api/` sem consumidor, re-export/helper "futuro", `EstoquePrincipal`/`NivelAcesso` próprios.
- `?status=` em entradas/saídas (E-1 minimal — front não consome; só sob ordem expressa).
- Tabela de reserva de estoque (sem upstream; a "reserva p/ produção" é o `baixarConsumo`
  idempotente + `EstoqueContract`, sem persistência nova).
- Export público/anônimo de CSV, importação assíncrona/lote gigante, PDF/relatórios de estoque,
  controle de lote/validade/série (requirements §Fora de Escopo), cálculo financeiro
  (Financeiro consome o evento), Eureka/Gateway/banco-por-serviço/HTTP entre módulos, Rabbit
  além do necessário, seed inventado, join cross-schema `estoque↔rh` (E-4 é via `Contract`).

## M3.4. Ordem de execução (topológica)

```
E1 migrations ─→ E2 models/enums ─┬→ E3 policies ─→ E4 controllers núcleo ─┐
                                   ├→ E3 ─→ E5 controllers resto ──────────┤
                                   ├→ E6 services movimentações ──────────┤→ E9 events/contract ─→ E10 scheduler ─┐
                                   ├→ E7 services empréstimo ────────────┤→ E9 ───────────────────────────────────┤→ E12 testes ─→ E13 gate
                                   └→ E8 services BOM ───────────────────┘→ E9 ───────────────────────────────────┘
E1 ─→ E11 seedverificação ─→ E12   (E4 × E5 × E6 × E7 × E8 paralelizáveis após E2/E3)
```

## M3.5. Riscos e mitigações

| # | Risco | Mitigação (task) |
| :--- | :--- | :--- |
| R1 | Lock pessimista no pgsql de teste (sqlite não trava de verdade) | E12 roda em pgsql `fablab_testing` (padrão M0–M2); teste de concorrência com 2 transações simultâneas no mesmo item |
| R2 | Delta RBAC Voluntário (matriz × Java EDICAO) contestado | §M3.1.4 documenta; E3 implementa via vínculo `usuario_responsabilidade`; E12 cobre Voluntário com/sem vínculo; reversível por Gate |
| R3 | `RhContract::nomePessoa()` quebra M2 (contrato já commitado) | E9: método **aditivo** na interface + default/implementação; testes Rh rodam sem regressão em E12 |
| R4 | Publicar evento quebrando o request (broker/fila fora) | E9: dispatch nativo fila `database`; falha de fila nunca estoura 500 na movimentação (fail-soft do publisher preservado) |
| R5 | CSV injection no export (planilha executa fórmula) | E6: sanitiza células `=,+,-,@`/tab com prefixo `'`; E12 testa payload malicioso |
| R6 | Baixa duplicada (BOM consumo × evento produção, retry) | E6/E8: idempotência por `(CONSUMO,id_referencia)`; E12 testa duplo dispatch |
| R7 | `?status=` em entradas/saídas ressurgir por hábito | E4: só `?idItem=`; E13 faz grep de `status` nas rotas de entradas/saídas |
| R8 | V9 `responsavel` vs architecture V1–V8 (drift) | E1 inclui V9 (upstream real); registrado em §M3.1.1, `docs/` intocado |
| R9 | `EstoqueContract` virar dependência cíclica (M4/M6 → Estoque → Rh) | E9: contract só lê schema próprio + `RhContract` (acíclico: Estoque→Rh, nunca Rh→Estoque) |

---

# M4 — módulo `vendas` (Vendas & CRM) — estratégia de migração Java → Laravel

> Despacho do Orquestrador (2026-10-06). **Estende M0–M3 sem sobrescrever.** Referência de regra
> (reescrever em PHP, nunca mover Java): `vendas-service/src/main/java/com/fablab/vendas/**`
> (~78 arqs: 9 controllers, 9 dtos, 18 entities, 11 repositories, 11 services, rabbit, config) +
> migrations `V1–V11` + `specs/features/vendas-service/requirements.md` (F1–F8).
> Base pronta: `App\Modules\Auth` (`AuthContract`), `App\Modules\Rh` (`RhContract`),
> `App\Modules\Estoque` (`EstoqueContract`: `itemExiste/saldoDe/baixarConsumo`), `App\Shared`,
> padrões M0–M3 (PHP 8.3/Laravel 12, PHPUnit pgsql `fablab_testing`, Handler JSON, blacklist,
> Gate admin, migrator glob, `DB_SEARCH_PATH`). **Nunca `docs/`; nenhum código agora; não commitar.**

## M4.1. Estratégia contrato-preservado / código-descartado

1. **Reaproveitado (regra, não código):** tabelas/colunas/constraints/índices/UKs **V1–V11** (ordem
   `cliente → tag_cliente → cliente_tag → orcamento → item_orcamento → encomenda →
   historico_status_encomenda → registro_marketplace → interacao_cliente → tarefa_marketing →
   solicitacao_edicao`) — **drift registrado:** `system/architecture.md` §Persistência lista V1–V10
   do vendas; o upstream real tem **V11** (`solicitacao_edicao`), que prevalece (não tocar em
   `docs/`, só registrar aqui); fluxos F1–F8; validações (CPF/CNPJ, quantidades/valores positivos,
   datas, enums via CHECK); payloads e nomes dos 3 eventos emitidos; lock otimista do Kanban
   (`versao`, 409 em conflito); idempotência da conversão (1 orçamento → 1 encomenda);
   venda direta (sem orçamento: cliente + valor obrigatórios); nova ordem por alteração
   (encerra atual + cria nova com `encomenda_origem_id`); duplicar orçamento (cópia Pendente);
   bulk-tag Admin-only; `liquido = bruto − taxa` (Marketplace); máscara LGPD de documento.
2. **Descartado:** `entity/repository/service/mapper/config` Spring, `RabbitTemplate`/`RabbitConfig`,
   `RestClient`/`DiscoveryClient`, `@PreAuthorize`/`SecurityConfig`, `JwtService` próprio (JWT é o
   guard `api` do Auth/M1), `VendasPrincipal` (vira `auth()->user()` + `AuthContract::isAdmin`),
   `GlobalExceptionHandler` próprio (Handler JSON central do `Shared`), Eureka/Flyway/banco-por-serviço.
3. **Ordem:** V1 migrations → V2 models/enums/rule → V3 policies → V4–V6 controllers/requests/resources
   → V7–V9 services → V10 events/listeners+contract → V11 seeders → V12 testes → V13 gate.
   V4–V6 exigem V2+V3; V7–V9 exigem V2 (+V10 p/ payloads); V12 exige tudo.
4. **Ajustes de empacotamento permitidos (sem novo contrato):**
   - Base paths `/clientes`, `/orcamentos`, … (serviço Java) → `/api/vendas/**` (contrato do front).
   - Banco `fablab_vendas` → schema `vendas` no banco único `fablab`
     (`Schema::create('vendas.cliente', …)`, `$table='vendas.cliente'`).
   - `RabbitTemplate.convertAndSend` (exchange `fablab.vendas`) → `Event` nativo (`ShouldQueue`,
     fila `database`), **mesmo nome** de evento e mesmo payload; `ProducaoStatusListener`
     (`@RabbitListener`) → `Listener` do evento `producao.status.alterado.event` com a **mesma**
     idempotência (status igual = no-op, status desconhecido = warn/ignore; produtor real chega em M6/Produção).
   - `SecurityConfig` + `@PreAuthorize` → `auth:api` + `blacklist` + `Policy`/`can:` — com **um
     delta consciente:** a matriz `rbac-matrix.md` (fonte sobreposta) **prevalece** onde diverge do
     Java: `ESCRITA = ADMIN+BOLSISTA+VOLUNTARIO` permitia escrita a não-Admin, mas a matriz dá
     `Editar` só ao Admin em clientes/orçamentos/marketplace/CRM — vale a matriz: **escrita = Admin**,
     leitura = qualquer autenticado exceto Recrutando (4). O `Editar (se atribuído)` da linha
     Encomendas/Kanban é implementado como **criador+Admin** (`criado_por = claim `id_user`` ou
     Admin, senão 403 — `PermissaoUtil.exigirCriadorOuAdmin` preservado na `Policy`, sem papel novo
     "responsável de Vendas" por decisão do PO). Reversível por Gate (ver §M4.5 R2).
   - `@Version versao` → coluna `versao` BIGINT + checagem manual em `DB::transaction`
     (`request.versao != model.versao → 409`), incremento no mover Kanban (sem maquinaria optimistic extra).
   - `DocumentoUtil` (CPF/CNPJ + máscara) → `Rules/DocumentoRule` (validação) + máscara na `Resource`
     (saída; LGPD) — `CpfRule` do Rh **não** é reusado (só cobre CPF; CNPJ exige algoritmo próprio).
   - `GET /interacoes` global **não existe**: o contrato só tem `GET /interacoes/{idCliente}`
     (escopo por cliente) — sem rota fora do contrato. Sem CSV (só Estoque tem CSV).
   - `converter orçamento` **não** é endpoint próprio: é o `POST /encomendas` com `idOrcamento`
     (conversão validada no service); `duplicar` (`POST /orcamentos/{id}/duplicar`) é preservado.

## M4.2. Mapeamento contrato (Java/Spring) → Laravel

| Origem (`vendas-service/`, referência) | Destino (`backend/`, Laravel) | Peça / observação |
| :--- | :--- | :--- |
| `db/migration/V1–V11.sql` (Flyway) | `database/migrations/vendas/*.php` | V1 — `Schema::create('vendas.<tabela>')`; V6 `versao` + CHECK kanban + `criado_por` + `encomenda_origem_id`; V11 `solicitacao_edicao` (drift §M4.1.1) |
| `entity/*` (11 entidades + `TipoPessoa/StatusKanban/StatusOrcamento/StatusSolicitacao/Tipo*` + `NivelAcesso`) | `app/Modules/Vendas/Models/*.php` (10) + `Enums/` (6) | V2 — `$table='vendas.*'`, `$timestamps=false`, `$fillable`, casts decimal/date; `cliente_tag` vira `belongsToMany` (sem model pivot); enums viram `string backed` PHP 8.1; `NivelAcesso` **some** (reuse `Auth\Enums\Role`) |
| `repository/*` (11 interfaces) | **nada** — Eloquent direto | V2 — proibido `Repository` por hábito (não planejar) |
| `mapper/*` + `dto/*Request/*Response` (9 arqs agregados) + `ErrorResponse` | `Http/Requests/*FormRequest` (entrada) + `Http/Resources/*` (saída, incl. documento mascarado) | V4–V6 — `FormRequest` valida+autoriza; `Resource` dá o shape; `ErrorResponse` some (Handler `Shared`) |
| `dto/VendasPrincipal` + `NivelAcesso` | **nada** — `auth()->user()` + `AuthContract` | V3 — `isAdmin()` vira `Gate admin`/`AuthContract::isAdmin()`; `idPessoa` (claim `id_user`) lido do guard |
| `config/SecurityConfig` + `ESCRITA/LEITURA/ACESSO` (9 controllers) | `auth:api`+`blacklist`+`Policies/VendasPolicy`+`can:` | V3 — matriz `rbac-matrix.md` ∩ requirements §7, com delta Admin-escrita + criador+Admin documentado em §M4.1.4; Recrutando (4) sem acesso ao módulo |
| `service/DocumentoUtil` (CPF/CNPJ + máscara) | `Rules/DocumentoRule.php` + máscara na `Resource` | V2 — **create** justificado (CNPJ sem equivalente nativo nem no `CpfRule` do Rh) |
| `service/ClienteService` (CRUD/filtros/tags/bulk/excluir-guard) | `Services/ClienteService.php` | V7 — filtros Eloquent (termo/tipo/tags via `whereHas`); excluir bloqueado com encomendas vinculadas; `bulkTag` Admin-only |
| `service/TagService` (criar/listar) | **nada dedicado** — CRUD trivial no `Model`/Eloquent | V7 — sem service forçado (regra trivial; Builder escolhe controller→Model) |
| `service/OrcamentoService` (itens+total, ajustes, aprovar→evento, duplicar) | `Services/OrcamentoService.php` | V7 — `valor_total = Σ(qtd × unit)` no service; aprovado+convertido trava edição; `duplicar` copia itens como Pendente; `id_item_estoque` opcional validado via `EstoqueContract::itemExiste` (sem coluna nova) |
| `service/EncomendaService` (criar, kanban+histórico+versao, nova-ordem, criador+Admin) | `Services/EncomendaService.php` | V8 — nasce `Fila` + histórico inicial; mover exige `versao` (409) e bloqueia `Entregue`; `novaOrdem` encerra atual + referencia origem; publica 2 eventos via V10 |
| `service/HistoricoService` (listar) | **nada dedicado** — método em `EncomendaService::historico()` | V8 — leitura trivial, sem classe extra |
| `service/MarketplaceService` (registrar/listar/`liquido`) | `Services/MarketplaceService.php` | V9 — `liquido(bruto, taxa)` puro no service (dado p/ Financeiro via evento/recursos) |
| `service/CrmService` (interações+tarefas) | `Services/CrmService.php` | V9 — interação exige cliente existente; tarefa valida `data_fim ≥ data_inicio`; responsável persistido sem join cross-schema |
| `service/SolicitacaoService` (solicitar/listar+counts/decidir) | `Services/SolicitacaoService.php` | V9 — **D-4 contrato mínimo**: decidir só flipa status (+`decidido_por/motivo/data_decisao`), **sem aplicação automática**; rejeitar exige motivo; decidir = Admin-only |
| `service/PermissaoUtil.exigirCriadorOuAdmin` | `Policies/VendasPolicy` (método kanban/nova-ordem) | V3 — backend nega não-criador não-Admin com 403 (teste em V12) |
| `rabbit/VendasEventPublisher` (3 `convertAndSend`) | `Events/*.php` (`ShouldQueue`, fila `database`) | V10 — nomes `encomenda.criada.event`, `encomenda.status.alterado.event`, `orcamento.aprovado.event` + payloads preservados |
| `rabbit/ProducaoStatusListener` (sync idempotente) | `Listeners/ProducaoStatusListener.php` do evento `producao.status.alterado.event` | V10 — mesma idempotência (igual = no-op; desconhecido = ignore); `id_usuario=0` (sistema) preservado |
| `JwtService/RabbitConfig` + `GlobalExceptionHandler` | **nada** | V10 — JWT é do Auth; fila `database` cobre M4; Handler é do `Shared` |
| seeds (inexistentes em V1–V11) | **nada** (verificação V11) | V11 — sem seed inventado |
| Testes (unit/integration/security) | `tests/Unit/Vendas/*` + `tests/Feature/Vendas/*` + slice 401/403 + 403 não-criador | V12 — `php artisan test` (pgsql `fablab_testing`), cobertura ≥ 80% |

Rotas finais (PT, `/api/vendas/**` exatas — ver V4–V6): `clientes` (`POST/GET` +filtros,
`GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/tags`, `DELETE /{id}/tags/{tagId}`,
`POST /bulk-tag` — Admin), `tags` (`POST/GET`), `orcamentos` (`POST/GET`, `GET /{id}`,
`PUT /{id}`, `POST /{id}/duplicar`), `encomendas` (`POST/GET` `?status=&clienteId=`,
`GET /{id}`, `GET /{id}/status`, `PUT /{id}/kanban`, `POST /{id}/nova-ordem`),
`historico-status/{idEncomenda}` (`GET`), `marketplace` (`POST/GET` `?encomendaId=&plataforma=`),
`interacoes` (`POST`, `GET /{idCliente}` — sem list-all global), `tarefas-marketing`
(`POST/GET` `?responsavelId=&status=`, `PUT /{id}`), `solicitacoes` (`POST/GET` `?status=`,
`PUT /{id}/decisao` — Admin).

## M4.3. Decisões de enxugamento (código mínimo obrigatório)

**Reusado do contrato (regra preservada, roupagem Laravel):**
- Tabelas/colunas/constraints/índices/UKs V1–V11 idênticos (só qualificados `vendas.`); `versao`
  em `encomenda`; `criado_por` + `encomenda_origem_id`; CHECKs de kanban/orçamento/solicitação;
  `cpf_cnpj` único; UK `cliente_tag`.
- Fluxos F1–F8, matriz RBAC por endpoint **com o delta Admin-escrita/criador+Admin** (§M4.1.4),
  validações (documento, valores, datas, `data_fim ≥ data_inicio`, motivo na rejeição),
  conversão 1:1 orçamento→encomenda, venda direta, nova ordem com referência legada, duplicar
  como Pendente, bulk-tag Admin-only, `liquido`, máscara LGPD, counts de solicitações.
- `Schedule` n/a (Vendas não tem scheduler); `Event/Listener` no lugar de `RabbitTemplate`;
  Eloquent+`versao` manual no lugar de JPA+`@Version`; `Resource` no lugar de mappers;
  `FormRequest` no lugar de records+Bean Validation.

**`create` justificado (só o que não existe e o requirements/DoD exige):**
1. Migrations `database/migrations/vendas/` — schema único exige reescrita em PHP (V1).
2. `Rules/DocumentoRule` (CPF+CNPJ) — `CpfRule` do Rh não cobre CNPJ; sem equivalente nativo (V2).
3. `Contracts/VendasContract.php` — fronteira in-process exigida pela arquitetura p/ M6
   (Produção: encomenda) e M5 (Financeiro: valores); não existe no Java (era REST/Eureka) (V10).
   **Congelado em 2 métodos** (`dadosEncomenda`, `dadosOrcamento`) — só o consumido.
4. Slice de testes security 401/403 por rota + 403 não-criador — DoD exige; Java não é reusável (V12).

**Explicitamente NÃO planejado (fora de escopo / especulativo):**
- `Repository/`, `Mapper/`, `DTO/` mutáveis, `config/exception` do módulo, `JwtService`/
  `VendasPrincipal`/`NivelAcesso` próprios, `TagService`/`HistoricoService` dedicados (trivial
  dobrado em Model/`EncomendaService`), `api/` sem consumidor, re-export/helper "futuro".
- Papel "responsável de Vendas" / vínculo `usuario_responsabilidade` no módulo (decisão PO:
  Admin decide; criador+Admin cobre o "se atribuído").
- Aplicação automática de solicitação aprovada (D-4 contrato mínimo: só flipa status).
- `GET /interacoes` global fora do contrato; CSV import/export (só Estoque tem); coluna
  `id_item_estoque` persistida (validação transitória via `EstoqueContract`, sem coluna nova).
- `?status=` em marketplace além de `encomendaId/plataforma`? — não: contrato só tem esses dois
  filtros (sem inventar). Tabela de reserva/custo (Financeiro/Estoque donos); recibo fiscal/NF-e;
  integração automática com marketplaces; portal do cliente (requirements §Fora de Escopo);
  consumo de `estoque.consumo.realizado.event` (produtor inexistente — M5/M6 demandam);
  Eureka/Gateway/banco-por-serviço/HTTP entre módulos; Rabbit além do necessário; seed inventado;
  join cross-schema `vendas↔rh` (responsáveis persistidos como id, sem FK; nomes via `RhContract`
  ficam p/ M5/M6 se consumirem — fora de V10).

## M4.4. Ordem de execução (topológica)

```
V1 migrations ─→ V2 models/enums/rule ─┬→ V3 policies ─→ V4 controllers clientes/tags ─┐
                                       ├→ V3 ─→ V5 controllers orçamentos/encomendas ─┤
                                       ├→ V3 ─→ V6 controllers marketplace/CRM/solic ──┤→ V10 events/contract ─┐
                                       ├→ V7 services cliente/orçamento ──────────────┤→ V10 ──────────────────┤→ V12 testes ─→ V13 gate
                                       ├→ V8 services encomenda ─────────────────────┤→ V10 ──────────────────┤
                                       └→ V9 services marketplace/CRM/solic ─────────┘→ V10 ──────────────────┘
V1 ─→ V11 seedverificação ─→ V12   (V4 × V5 × V6 × V7 × V8 × V9 paralelizáveis após V2/V3)
```

## M4.5. Riscos e mitigações

| # | Risco | Mitigação (task) |
| :--- | :--- | :--- |
| R1 | Delta RBAC Admin-escrita (matriz × Java ESCRITA) contestado pelo front | §M4.1.4 documenta; V3 implementa via `Gate admin`; V12 cobre B/V escrevendo →403; reversível por Gate |
| R2 | `criado_por` de registros legados sem dono bloqueia kanban p/ não-Admin | V8: só o criador registrado ou Admin move; registros sem `criado_por` exigem backfill manual (sem default silencioso) |
| R3 | Conflito de `versao` em teste pgsql (concorrência real) | V12 testa duplo mover com mesma `versao` → 1×200 + 1×409; sem sqlite (pgsql `fablab_testing`) |
| R4 | `EstoqueContract::itemExiste` quebra M3 (contrato já commitado) | V7: consumo **só-leitura** do método existente, sem alterar M3; testes Estoque sem regressão em V12 |
| R5 | Publicar evento quebrando o request (fila fora) | V10: dispatch nativo fila `database`; falha de fila nunca estoura 500 (fail-soft do publisher preservado) |
| R6 | Solicitação aprovada aplicada por hábito (D-4) | V9: decidir só flipa status; V13 faz grep de `update`/`save` em alvo fora de `solicitacao_edicao` no decidir |
| R7 | `GET /interacoes` global ou CSV ressurgirem por hábito | V6: só `GET /{idCliente}`; V13 faz grep de rota global/CSV no módulo |
| R8 | V11 `solicitacao_edicao` vs architecture V1–V10 (drift) | V1 inclui V11 (upstream real); registrado em §M4.1.1, `docs/` intocado |
| R9 | `VendasContract` virar dependência cíclica (M5/M6 → Vendas → Estoque → Rh) | V10: contract só lê schema próprio + `EstoqueContract` (acíclico: Vendas→Estoque→Rh, nunca o inverso) |

---

# M5 — módulo `financeiro` (Financeiro) — estratégia de migração Java → Laravel

> Despacho do Orquestrador (2026-10-07). **Estende M0–M4 sem sobrescrever.** Referência de regra
> (reescrever em PHP, nunca mover Java): `financeiro-service/src/main/java/com/fablab/financeiro/**`
> (~75 arqs: 7 controllers, 10 dtos, 16 entities, 9 repositories, 13 services, 3 consumers, config) +
> migrations `V1–V9` + `specs/features/financeiro-service/requirements.md` (F1–F9).
> Base pronta: `App\Modules\Auth` (`AuthContract`), `App\Modules\Rh` (`RhContract`:
> `horasValidadasNoPeriodo/incoerencias`), `App\Modules\Estoque` (`EstoqueContract`),
> `App\Modules\Vendas` (`VendasContract`: `dadosEncomenda/dadosOrcamento`), `App\Shared`,
> padrões M0–M4 (PHP 8.3/Laravel 12, PHPUnit pgsql `fablab_testing`, Handler JSON, blacklist,
> Gate admin, migrator com glob, `DB_SEARCH_PATH` — estender aditivamente p/ `financeiro`).
> **Nunca `docs/`; nenhum código agora; não commitar.**

## M5.1. Estratégia contrato-preservado / código-descartado

1. **Reaproveitado (regra, não código):** tabelas/colunas/constraints/índices/UKs **V1–V9** (ordem
   `categoria_financeira → lancamento_financeiro → doacao_recurso → valor_hora_nivel →
   parametro_overhead → fechamento_encomenda → custo_encomenda → solicitacao_compra →
   horas_encomenda`); fluxos F1–F9; RBAC **Admin-exclusivo em todos os controllers**
   (`@PreAuthorize("hasRole('ADMIN')")` → `can:admin`, matriz `rbac-matrix.md` §Financeiro);
   validações (valores > 0, tamanhos, datas, enums via CHECK, nível 0–3); shapes servidos D-2
   (`counts`+`resumo` na lista, agregados/semanas/meses nos relatórios — front nunca deriva);
   filtros combinados D-4 (`status/tipo/categoria/período`, `?periodo=yyyy-MM` nos relatórios) e
   D-8 (`origem` = trecho da referência externa, `vencimentoDe/vencimentoAte` delimitam o
   vencimento, com alias `dataInicio/dataFim`); custeio por ordem (materiais + mão de obra com
   default nível 2 + overhead, `margem = valor_fechado − custo_total`); fechamento imutável
   (nova ordem encerra+cria, nunca edita — D-5) + auditoria em log (usuário/data); nomes e
   payloads dos eventos; scheduler de vencidos com flag.
2. **Descartado:** `entity/repository/service/mapper/config` Spring, `RabbitTemplate`/`RabbitConfig`,
   `RestClient`/`DiscoveryClient`, `@PreAuthorize`/`SecurityConfig`, `JwtService` próprio (JWT é o
   guard `api` do Auth/M1), `FinanceiroPrincipal` (vira `auth()->user()`), `PaymentProcessor` +
   `ManualPaymentProcessor` (abstração sem provedor — vira 1 linha de `Log::info` na liquidação,
   ver §M5.3), `GlobalExceptionHandler` próprio (Handler JSON central do `Shared`), Eureka/Flyway/
   banco-por-serviço.
3. **Ordem:** F1 migrations → F2 models/enums → F3 policies → F4–F6 controllers/requests/resources
   → F7–F9 services → F10 events/listeners+scheduler → F11 seeders → F12 testes → F13 gate.
   F4–F6 exigem F2+F3; F7–F9 exigem F2 (+F10 p/ payloads); F12 exige tudo.
4. **Ajustes de empacotamento permitidos (sem novo contrato):**
   - Base paths `/lancamentos`, `/categorias`, … (serviço Java) → `/api/financeiro/**` (contrato do front).
   - Banco `fablab_financeiro` → schema `financeiro` no banco único `fablab`
     (`Schema::create('financeiro.lancamento_financeiro', …)`, `$table='financeiro.*'`).
   - `RabbitTemplate.convertAndSend` (exchange `fablab.financeiro`) → `Event` nativo (`ShouldQueue`,
     fila `database`), **mesmo nome** de evento e mesmo payload; os 3 `consumer/*`
     (`@RabbitListener`) → `Listener`s dos eventos `encomenda.criada.event`,
     `horas.validadas.event`, `producao.concluida.event` (produtor deste último chega em M6/Produção).
   - `SecurityConfig` + `@PreAuthorize("hasRole('ADMIN')")` → `auth:api` + `blacklist` + `can:admin`
     em **todas** as rotas (módulo 100% Admin, sem delta: Java e matriz coincidem).
   - `@Scheduled(cron="${…:0 0 8 * * *}")` (Spring, 6 campos, default **08:00**) →
     `Schedule::call(...)->dailyAt('03:00')` em `routes/console.php` (**delta consciente:** 03:00
     alinha ao padrão do monólito M0–M4; reversível por config — ver §M5.5 R2) + flag
     `financeiro.vencidos_scheduler_enabled` (default on).
   - `id_encomenda`/`id_item_estoque`/`id_funcionario` seguem **inteiros opacos sem FK cross-schema**
     (como no Java): sem validação cruzada inventada via `Contract` (ver §M5.3); `id_referencia_externa`
     é string opaca comparada por igualdade exata no custeio (convenção: id da encomenda) e agregada
     no `custo-maquina` (convenção: id da máquina).
   - `PaymentProcessor.registrarPagamentoManual` → `Log::info` dentro de `registrarPagamento`
     (auditoria preservada, abstração descartada — §M5.3).
   - `marcarVisualizada` (service sem rota) e `cancelar` fechamento (service sem rota) **não viram
     endpoint nem método** (sem consumidor no contrato — §M5.3).
   - **Sem endpoint bulk:** a bulk-bar do front liquida item a item (`PUT …/pagamento` por id, erro
     409/422 por item permite `allSettled` no front); sem `POST /lancamentos/bulk`/CSV/PDF.

## M5.2. Mapeamento contrato (Java/Spring) → Laravel

| Origem (`financeiro-service/`, referência) | Destino (`backend/`, Laravel) | Peça / observação |
| :--- | :--- | :--- |
| `db/migration/V1–V9.sql` (Flyway) | `database/migrations/financeiro/*.php` | F1 — `Schema::create('financeiro.<tabela>')`; V2 CHECKs tipo/status + índices `(status,vencimento)` e `referencia`; V4 CHECK nível 0–3 + índice `(nivel,vigencia)`; V6 `id_encomenda` UNIQUE + `horas_validadas` default 0; V9 UK `(encomenda,funcionario,data)`; `DB_SEARCH_PATH` estendido aditivamente (`…vendas,financeiro`) |
| `entity/*` (9 entidades + `TipoCategoria/TipoLancamento/StatusLancamento/TipoDoacao/StatusFechamento/StatusCompra` + `NivelAcesso`) | `app/Modules/Financeiro/Models/*.php` (9) + `Enums/` (6) | F2 — `$table='financeiro.*'`, `$timestamps=false`, `$fillable`, casts decimal/date; enums viram `string backed` PHP 8.1; `nivel_acesso` segue **int + `min:0,max:3`** no Request (sem enum dedicado — CHECK 0–3); `NivelAcesso` **some** (reuse `Auth\Enums\Role` quando for papel; aqui é parâmetro de custeio) |
| `repository/*` (9 interfaces, incl. `somarPorTipoEReferencia`, vigente-por-nível, último-custo, vencidos) | **nada** — Eloquent direto (`where/sum/orderByDesc`) | F2 — proibido `Repository` por hábito (não planejar) |
| `dto/*Request/*Response` (10 arqs agregados: `of(entity)`, Bean Validation) + `ErrorResponse` | `Http/Requests/*FormRequest` (entrada) + `Http/Resources/*` (saída, incl. `counts`/`resumo`/agregados) | F4–F6 — `FormRequest` valida+autoriza (`periodo` com allowlist `yyyy-MM` → 422 fora do formato); `Resource` dá o shape servido D-2; `ErrorResponse` some (Handler `Shared`) |
| `dto/FinanceiroPrincipal` + `NivelAcesso` | **nada** — `auth()->user()` + `AuthContract` | F3 — auditoria (`idUsuario`) lida do guard |
| `config/SecurityConfig` + `@PreAuthorize hasRole('ADMIN')` (7 controllers) | `auth:api`+`blacklist`+`can:admin` em todas as rotas | F3 — sem delta (Java ≡ matriz: Financeiro exclusivo Admin); Recrutando (4) sem acesso |
| `service/CategoriaFinanceiraService` | `Services/CategoriaService.php` (ou método no `Model` se trivial) | F7 — cadastro `RECEITA/DESPESA` + listar com `?tipo=` |
| `service/LancamentoFinanceiroService` (criar/listar/pagar/`emitirVencidos` + `paymentProcessor`) | `Services/LancamentoService.php` | F7 — nasce `PENDENTE` (ou `ATRASADO` + evento imediato se vencido); `valor_total` nunca confiado do payload (só `valor`); pagar: `PAGO`+`data_pagamento` (default hoje), `PAGO/CANCELADO → 409/422`, `Log::info` (ex-`ManualPaymentProcessor`); `emitirVencidos` idempotente (PENDENTE→ATRASADO + 1 evento por vencido, sem liquidar); lista serve `counts`+`resumo` (D-2) com filtros D-4/D-8 |
| `service/ParametroService` (valor/hora 0–3 + overhead, vigência, auditoria em log) | `Services/ParametroService.php` | F9 — append-only (definir cria nova linha, nunca edita); vigentes = maior `data_vigencia`; ausente → `422/409` orientativo (pré-condição de custeio); `Log::info` usuário/data (D-5) |
| `service/DoacaoRecursoService` | `Services/DoacaoService.php` | F9 — `DOACAO/PROJETO` + filtros `?tipo=&dataInicio=&dataFim=`; `id_projeto_associado` opcional, sem join cross-schema |
| `service/SolicitacaoCompraService` (registrar/`REGISTRADA`+evento, listar, detalhar, `marcarVisualizada`, concluir) | `Services/CompraService.php` | F9 — fluxo informativo (sem bloqueio); concluir: já-`CONCLUIDA → 409/422`; `marcarVisualizada` **não** recriada (sem rota no contrato — §M5.3); publica `compra.solicitada.event` (`idFornecedor=null` no MVP, contrato preservado) |
| `service/FechamentoEncomendaService` (criar, `criarDoEvento` idempotente, consultar, `registrarHorasValidadas` upsert, `novaOrdem` D-5, `cancelar`) | `Services/FechamentoService.php` | F8 — imutável após criar (sem PUT; duplicado `id_encomenda → 409/422`); auto-abertura usa `valorFinal/dataCriacao` do evento; horas: upsert `(encomenda,funcionario,data)` + `horas_validadas`; `novaOrdem` encerra atual (`CONCLUIDA`) + cria nova; `cancelar` **não** recriado (sem rota — §M5.3) |
| `service/CusteioService` (`NIVEL_DEFAULT=2`, `calcular/calcularDoEvento/consultar`, `custo.calculado.event`) | `Services/CusteioService.php` | F8 — `materiais = Σ SAIDA¬canceladas c/ referencia=idEncomenda`; `maoObra = Σ horas×valorHora(nivel ?? 2)`; `overhead = taxa×Σhoras`; `total`, `margem = valor_fechado−total`; persiste `custo_encomenda` + publica evento; consultar = último cálculo (`orderByDesc data_calculo,id`); pré-condições com erro orientativo, sem persistir parcial |
| `service/RelatorioService` (6 relatórios + `resolverPeriodo`, semanas `yyyy-'S'w`, meses `yyyy-MM`) | `Services/RelatorioService.php` | F9 — shapes servidos 1:1 (fluxo-caixa, dre, lucratividade c/ margem %, inadimplência só `ENTRADA ATRASADO`, doacoes-despesas c/ cobertura %, custo-maquina agregado por referência); `periodo` allowlist `yyyy-MM`, `dataInicio/dataFim` têm precedência; cancelados excluídos das somas |
| `service/FinanceiroEventPublisher` (3 `convertAndSend` fail-soft) | `Events/*.php` (`ShouldQueue`, fila `database`) | F10 — nomes `lancamento.vencido.event`, `custo.calculado.event`, `compra.solicitada.event` + payloads preservados; dispatch nunca estoura 500 |
| `consumer/*` (3 `@RabbitListener`) | `Listeners/*.php` dos eventos `encomenda.criada.event`, `horas.validadas.event`, `producao.concluida.event` | F10 — mesma idempotência (fechamento existente = no-op; evento malformado = warn/ignore); produtor do `producao.concluida` chega em M6 |
| `service/VencidosScheduler` + `PaymentProcessor/ManualPaymentProcessor` + `JwtService`/`RabbitConfig` + `GlobalExceptionHandler` | `Schedule::call()->dailyAt('03:00')` + `Log::info` + **nada** (resto) | F7/F10 — JWT é do Auth; fila `database` cobre M5; Handler é do `Shared`; `PaymentProcessor` some (§M5.3) |
| `Contracts/` | **nada em M5** | F10 — nenhum módulo consome o Financeiro hoje; Dashboard/M8 definirá os métodos via `modify` aditivo (§M5.3) |
| seeds (inexistentes em V1–V9) | **nada** (verificação F11) | F11 — sem seed inventado |
| Testes (unit/integration/security/mensageria) | `tests/Unit/Financeiro/*` + `tests/Feature/Financeiro/*` + slice 401/403 | F12 — `php artisan test` (pgsql `fablab_testing`), cobertura ≥ 80% |

Rotas finais (PT, `/api/financeiro/**` exatas — ver F4–F6): `categorias` (`POST/GET`
`?tipo=`), `lancamentos` (`POST/GET` `?status=&tipo=&idCategoria=&dataInicio=&dataFim=&origem=` +
`&vencimentoDe=&vencimentoAte=`, `PUT /{id}/pagamento`), `doacoes-recursos` (`POST/GET`
`?tipo=&dataInicio=&dataFim=`), `valores-hora` (`POST/GET`), `parametros-overhead`
(`POST/GET` — GET devolve o vigente), `fechamento-encomenda` (`POST/GET`,
`GET /{idEncomenda}`, `POST /{idEncomenda}/nova-ordem`), `custos-encomenda/{idEncomenda}`
(`GET`), `solicitacoes-compra` (`POST/GET` `?status=`, `GET /{id}`, `PUT /{id}/concluir`),
`relatorios/fluxo-caixa|dre|doacoes-despesas` (`GET` `?dataInicio=&dataFim=&periodo=`),
`relatorios/lucratividade|inadimplencia|custo-maquina` (`GET` sem período, como no Java).

## M5.3. Decisões de enxugamento (código mínimo obrigatório)

**Reusado do contrato (regra preservada, roupagem Laravel):**
- Tabelas/colunas/constraints/índices/UKs V1–V9 idênticos (só qualificados `financeiro.`);
  `versao` n/a (Financeiro não usa lock otimista); `horas_validadas` default 0; UK
  `horas_encomenda(encomenda,funcionario,data)`.
- Fluxos F1–F9, RBAC Admin-exclusivo total, validações, `counts`/`resumo`/agregados servidos,
  filtros D-4/D-8, `periodo` allowlist `yyyy-MM`, `NIVEL_DEFAULT=2` (horas sem nível),
  pré-condições de custeio com erro orientativo, congelamento D-5 + auditoria em log,
  idempotências (fechamento por evento, horas por upsert, `emitirVencidos` sem liquidar).
- Coerência de horas F7: **dono é o Rh** (`RhContract::incoerencias`, já exposto em M2) —
  o Financeiro consome só horas validadas acumuladas; sem verificação duplicada.
- Dados de Vendas/Estoque chegam **por evento** (payload preservado); `id_encomenda` e
  `id_item_estoque` seguem opacos sem FK cross-schema, como no Java.
- `Schedule` no lugar de `@Scheduled`; `Event/Listener` no lugar de `RabbitTemplate`;
  Eloquent no lugar de JPA+repositories; `Resource` no lugar de `Response.of`;
  `FormRequest` no lugar de records+Bean Validation.

**`create` justificado (só o que não existe e o requirements/DoD exige):**
1. Migrations `database/migrations/financeiro/` + extensão aditiva de `DB_SEARCH_PATH`
   (`…vendas,financeiro`) — schema único exige reescrita em PHP (F1).
2. `config/financeiro.php` (flag `vencidos_scheduler_enabled`, default on) — padrão M3/E10
   p/ schedulers com kill-switch (F10).
3. Slice de testes security 401/403 por rota + matemática do custeio + idempotências — DoD
   exige; Java não é reusável (F12).

**Explicitamente NÃO planejado (fora de escopo / especulativo):**
- `Repository/`, `Mapper/`, `DTO/` mutáveis, `config/exception` do módulo, `JwtService`/
  `FinanceiroPrincipal`/`NivelAcesso` próprios, `PaymentProcessor`/`ManualPaymentProcessor`
  (vira `Log::info`; sem provedor no MVP — F9 do requirements Java é preparação futura, não
  código), `Contracts/FinanceiroContract` (nenhum consumidor atual; M8/Dashboard define via
  `modify` aditivo), `api/` sem consumidor, re-export/helper "futuro".
- `marcarVisualizada` e `cancelar` fechamento (métodos Java **sem rota** — dead code; sem endpoint).
- Listener próprio p/ `orcamento.aprovado.event` (o Java não tem consumer: o fluxo chega via
  `encomenda.criada.event` na conversão; ver §M5.5 R5) e consumer p/ `compra.solicitada.event`
  do Estoque (o Java não tem: registro é via `POST /solicitacoes-compra`; ver §M5.5 R6).
- D-6: export PDF/CSV de relatórios/lançamentos (fora do MVP — front omite botões); contas
  derivadas no backend além dos shapes D-2 (front exibe o servido); endpoint bulk de
  liquidação (bulk-bar do front = N×`PUT …/pagamento`, 409 por item); `PUT` em
  fechamento/parâmetros (append-only + nova ordem); recibo fiscal/NF-e, conciliação bancária,
  Pix/Cartão/Boleto; Eureka/Gateway/banco-por-serviço/HTTP entre módulos; Rabbit além do
  necessário; seed inventado; join cross-schema.

## M5.4. Ordem de execução (topológica)

```
F1 migrations ─→ F2 models/enums ─┬→ F3 policies ─→ F4 controllers lanc/cat ─┐
                                   ├→ F3 ─→ F5 controllers doa/compra/param ─┤
                                   ├→ F3 ─→ F6 controllers custeio/relat ───┤→ F10 events/scheduler ─┐
                                   ├→ F7 services lanc/cat ────────────────┤→ F10 ──────────────────┤→ F12 testes ─→ F13 gate
                                   ├→ F8 services fech/custeio ────────────┤→ F10 ──────────────────┤
                                   └→ F9 services doa/compra/param/relat ───┘→ F10 ──────────────────┘
F1 ─→ F11 seedverificação ─→ F12   (F4 × F5 × F6 × F7 × F8 × F9 paralelizáveis após F2/F3)
```

## M5.5. Riscos e mitigações

| # | Risco | Mitigação (task) |
| :--- | :--- | :--- |
| R1 | `DB_SEARCH_PATH` sem `financeiro` quebra `migrate:fresh`/wipe | F1: extensão aditiva (`…vendas,financeiro`) + `migrate:fresh` verde em DB limpo |
| R2 | Delta scheduler 08:00 (Java default) → 03:00 (monólito) contestado | §M5.1.4 documenta; F10 implementa via `dailyAt('03:00')` + flag; reversível por config |
| R3 | `id_referencia_externa` ambíguo (encomenda no custeio × máquina no custo-maquina) | F8/F9: convenção documentada (igualdade exata p/ custeio; agregação p/ máquina); sem normalização inventada |
| R4 | Custeio sem pré-condição (fechamento/valor-hora/overhead) persiste parcial | F8: valida tudo antes de persistir; erro orientativo; F12 cobre cada pré-condição ausente |
| R5 | `orcamento.aprovado.event` sem consumer parecer lacuna | F10: documentado — sem listener (fluxo chega via `encomenda.criada.event`); escalar ao Orquestrador se PO quiser reação própria |
| R6 | `compra.solicitada.event` com 2 publicadores (Estoque M3 + Financeiro F9) parecer duplicado | F9/F10: preservado 1:1 (cada lado publica o seu, como no Java); sem consumer cruzado inventado |
| R7 | Publicar evento quebrando o request (fila fora) | F10: dispatch nativo fila `database`; falha de fila nunca estoura 500 (fail-soft preservado) |
| R8 | `periodo` fora de `yyyy-MM` ou `dataInicio/dataFim` + `periodo` juntos | F4/F6: allowlist no Request (422 fora do formato); precedência `dataInicio/dataFim` > `periodo` (F9) |
| R9 | `FinanceiroContract` cobrado pelo Dashboard antes de M8 | F10: sem `Contracts/`; M8 define métodos via `modify` aditivo (precedente M3/E9 no `RhContract`) |

---

# M6 — módulo `producao` (Produção & Projetos) — estratégia de migração Java → Laravel

> Despacho do Orquestrador (2026-10-07). **Estende M0–M5 sem sobrescrever.** Referência de regra
> (reescrever em PHP, nunca mover Java): `producao-service/src/main/java/com/fablab/producao/**`
> (~150 arqs: 10 controllers, ~50 DTOs, 28 entities, 17 repositories, 14 services, 3 consumers,
> config) + migrations `V1–V18` + `specs/features/producao-service/requirements.md` (F1–F12).
> Base pronta: `App\Modules\Auth` (`AuthContract`), `App\Modules\Rh` (`RhContract`),
> `App\Modules\Estoque` (`EstoqueContract`: `baixarConsumo` idempotente),
> `App\Modules\Vendas` (`VendasContract`: `dadosEncomenda`), `App\Shared`,
> padrões M0–M5 (PHP 8.3/Laravel 12, PHPUnit pgsql `fablab_testing`, Handler JSON, blacklist,
> Gate admin, migrator com glob, `DB_SEARCH_PATH` com auth/rh/estoque/vendas/financeiro —
> estender aditivamente p/ `producao`). **Nunca `docs/`; nenhum código agora; não commitar.**

## M6.1. Estratégia contrato-preservado / código-descartado

1. **Reaproveitado (regra, não código):** tabelas/colunas/constraints/índices/UKs **V1–V18** (ordem
   `projeto → tarefa → encomenda_kanban → historico_kanban → maquina → historico_uso_maquina →
   setor → setor_material → setor_sinalizacao → setor_checklist → setor_responsavel →
   inspecao_5s → item_inspecao_5s → advertencia_membro → projeto_mesa → auditoria_projeto_mesa →
   parametro_5s → consumo_encomenda`) — **drift registrado:** requirements §3.5 diz
   `projeto_mesa.id_mesa` String ("Mesa 1"), mas a migration **V15 real usa `BIGINT NOT NULL`**;
   prevalece o SQL upstream (não tocar em `docs/`, só registrar aqui); fluxos F1–F12; validações
   (tamanhos, datas, enums via CHECK, quantidades positivas); lock otimista do Kanban (`version`,
   409 em conflito); `recalcularStatus` da inspeção (qualquer item `conforme=false` →
   `NAO_CONFORME`); contador sequencial de advertências + limite 3; QR `fablab://projeto-mesa/{id}`;
   `data_ultima_evolucao` + auditoria por `diasParaAuditoriaProjeto` (default 15); nomes e payloads
   dos eventos (routing keys `RabbitMqConfig` → nomes de evento 1:1).
2. **Descartado:** `entity/repository/service/mapper/config` Spring, `RabbitTemplate`/`RabbitConfig`,
   `RestClient`/`DiscoveryClient`, `@PreAuthorize`/`SecurityConfig`, `JwtService` próprio (JWT é o
   guard `api` do Auth/M1), `ProducaoPrincipal`/`AcessoService` (viram `auth()->user()` +
   `Policy::isResponsavelAtribuido`), `NivelAcesso` próprio (reuse `Auth\Enums\Role`),
   `GlobalExceptionHandler` próprio (Handler JSON central do `Shared`), Eureka/Flyway/banco-por-serviço.
3. **Ordem:** P1 migrations → P2 models/enums → P3 policies → P4–P9 controllers/requests/resources
   → P10–P13 services → P14 events/listeners+contract → P15 scheduler/seed → P16 testes → P17 gate.
   P4–P9 exigem P2+P3; P10–P13 exigem P2 (+P14 p/ payloads); P16 exige tudo.
4. **Ajustes de empacotamento permitidos (sem novo contrato):**
   - Base paths `/projetos`, `/tarefas`, … (serviço Java) → `/api/producao/**` (contrato do front).
   - Banco `fablab_producao` → schema `producao` no banco único `fablab`
     (`Schema::create('producao.projeto', …)`, `$table='producao.projeto'`).
   - `RabbitTemplate.convertAndSend` (exchanges `fablab.producao`/`fablab.notificacao`) → `Event`
     nativo (`ShouldQueue`, fila `database`), **mesmo nome** de evento e mesmo payload; os 2
     `consumer/*` com produtor real (`EncomendaCriadaListener`, `NivelAlteradoListener`) viram
     `Listener`s; `EstoqueConsumoListener` **não** vira Listener (sem produtor de
     `estoque.consumo.realizado.event` no monólito — ver §M6.3).
   - `SecurityConfig` + `@PreAuthorize` → `auth:api` + `blacklist` + `Policy`/`can:` — com
     **deltas conscientes** (matriz `rbac-matrix.md` ∩ requirements §7 prevalecem onde divergem do
     Java, todos documentados em §M6.5 R2 e aplicados em P3): cadastro/status de máquina =
     Admin-only; designação de responsáveis de setor = Admin-only; inclusão no Kanban = Admin+Bolsista.
   - `@Version version` → coluna `version` BIGINT + checagem manual em `DB::transaction`
     (`request.version != model.version → 409`), como M4/V8 (sem maquinaria optimistic extra).
   - `AcessoService::podeEditar` (+ SpEL `@acesso`) → métodos na `ProducaoPolicy`
     (`isResponsavelAtribuido`), enforcement backend, nunca no front.
   - `QrCodeService` (ZXing) → string de conteúdo `fablab://projeto-mesa/{id}` persistida +
     `GET /{id}/qrcode` servindo PNG via `chillerlan/php-qrcode` (puro-PHP, única dep nova —
     justificativa em §M6.3; sem GD/imagick).
   - `id_encomenda`/`id_responsavel`/`id_funcionario`/`id_inspetor` seguem **inteiros opacos sem FK
     cross-schema** (como no Java): a única validação cruzada é `VendasContract::dadosEncomenda`
     no incluir-kanban (Kanban dona única: Produção LINKA a encomenda do Vendas, não duplica);
     responsáveis são opacos (precedente M4/M5 — sem checagem via `RhContract` por escrita).
   - `PATCH /tarefas/{id}` (**create** justificado — vínculo da tarefa atribuída, §M6.3): conclui a
     própria tarefa (`id_responsavel` = claim `id_user` ou Admin, senão 403); `PUT /{id}/status`
     mantém a regra Java (responsável do **projeto**). Ponto de compatibilidade p/ o repoint de
     `PATCH /api/tasks/{id}` do Dashboard em M8 (não migrar em M6).
   - `POST /kanban/encomenda/{idEncomenda}/consumo` (**create** justificado — F11 exige o registro
     da BOM final em `consumo_encomenda`; o Java só acumulava via listener sem produtor): upsert
     idempotente `(id_encomenda,id_item)`; a baixa física ocorre **só** no mover→`ENTREGUE` via
     `EstoqueContract::baixarConsumo` (`idReferencia = id_encomenda`).
   - `@Scheduled(cron="${…:0 0 6 * * *}")` (Spring, 6 campos, 06:00) →
     `Schedule::call(...)->dailyAt('06:00')` em `routes/console.php` (**horário preservado** do
     contrato, ao contrário do padrão 03:00 dos demais módulos) + flag
     `producao.auditoria.scheduler.enabled` (default on).

## M6.2. Mapeamento contrato (Java/Spring) → Laravel

| Origem (`producao-service/`, referência) | Destino (`backend/`, Laravel) | Peça / observação |
| :--- | :--- | :--- |
| `db/migration/V1–V18.sql` (Flyway) | `database/migrations/producao/*.php` | P1 — `Schema::create('producao.<tabela>')`; V3 `id_encomenda` UNIQUE + `version` default 0; V15 `id_mesa` BIGINT (drift §M6.1.1); V18 `consumo_encomenda` sem PK composta (upsert por par); `DB_SEARCH_PATH` estendido aditivamente (`…financeiro,producao`) |
| `entity/*` (18 entidades + `ProjetoStatus/TarefaStatus/PrioridadeTarefa/KanbanStatus/MaquinaStatus/StatusInspecao/TurnoInspecao/TipoAdvertencia/StatusProjetoMesa` + `NivelAcesso`) | `app/Modules/Producao/Models/*.php` (18) + `Enums/` (9) | P2 — `$table='producao.*'`, `$timestamps=false`, `$fillable`, casts (`horas_uso`/`quantidade` decimal, datas); enums viram `string backed` PHP 8.1; `NivelAcesso` **some** (reuse `Auth\Enums\Role`) |
| `repository/*` (17 interfaces, incl. `@Version` + finders) | **nada** — Eloquent direto (`version` manual no Kanban) | P2 — proibido `Repository` por hábito (não planejar) |
| `dto/*Request/*Response` (~50) + `ErrorResponse` | `Http/Requests/*FormRequest` (entrada) + `Http/Resources/*` (saída) | P4–P9 — `FormRequest` valida+autoriza; `Resource` dá o shape; `ErrorResponse` some (Handler `Shared`) |
| `dto/ProducaoPrincipal` + `service/AcessoService` | **nada** — `auth()->user()` + `Policies/ProducaoPolicy` | P3 — `podeEditar` vira `isResponsavelAtribuido`; caminho de sistema (listener/scheduler) sem bypass de Policy |
| `config/SecurityConfig` + `@PreAuthorize` (10 controllers) | `auth:api`+`blacklist`+`Policies/ProducaoPolicy`+`can:` | P3 — matriz ∩ requirements §7, com deltas documentados (§M6.1.4/R2); Recrutando (4) sem acesso ao módulo |
| `service/ProjetoService` (criar-só-se-responsável, status+`data_fim_real`) | `Services/ProjetoService.php` | P10 — criar exige ser o `id_responsavel` ou Admin (403); `CONCLUIDO` carimba `data_fim_real` (default hoje) |
| `service/TarefaService` (vínculo projeto-responsável, `data_conclusao`) | `Services/TarefaService.php` | P10 — criar/atualizar/status/remover exigem responsável do **projeto** ou Admin; `CONCLUIDA` carimba `data_conclusao`; **`PATCH` próprio** (tarefa-responsável, §M6.1.4) |
| `service/KanbanService` (incluir, mover+histórico+3 eventos, conclusão, remover) | `Services/KanbanService.php` | P11 — incluir valida `VendasContract::dadosEncomenda` (404 se inexistente) + UNIQUE (409 duplicado); mover exige `version` (409), responsável do cartão ou Admin, grava histórico + publica `producao.status.alterado` + `kanban.status.alterado`; `ENTREGUE` (1ª vez) publica `producao.concluida` + baixa via `EstoqueContract::baixarConsumo` (idempotente) |
| `service/MaquinaService` (CRUD, status, uso abre/fecha+`horas_uso`, histórico) | `Services/MaquinaService.php` | P12 — `iniciarUso` exige `DISPONIVEL` (→`EM_USO`, 409 senão); `encerrarUso` valida vínculo uso↔máquina + `fim ≥ inicio` (422), calcula `horas_uso`, devolve `DISPONIVEL`; uso com máquina em `MANUTENCAO` → 409 |
| `service/SetorService` (CRUD+detalhe, materiais, sinalizações, checklist editável, responsáveis com rotação) | `Services/SetorService.php` | P12 — checklist: edita **só** `item`+`ativo` (sem peso/reordenação — sem coluna upstream); responsável ativo novo desativa o anterior (rotação); sem checagem cruzada de Recrutando (ids opacos, regra documentada) |
| `service/Inspecao5SService` (checklist-ativo, vínculo item↔setor, `recalcularStatus`, auto-advertência) | `Services/Inspecao5SService.php` | P13 — item de checklist de outro setor → 422; **status recalculado no backend** (nunca confiado do payload); `NAO_CONFORME` → `AdvertenciaService::registrarAutomatica` (período experimental suprime a formal — ver P13) |
| `service/AdvertenciaService` (contador, `registrarAutomatica`, 2 eventos) | `Services/AdvertenciaService.php` | P13 — `contador = count+1`; sempre publica `advertencia.registrada.event`; `contador ≥ 3` publica também `advertencia.limite.atingido.event`; período experimental (`parametro_5s.periodo_experimental_ativo=true`) suprime a formal p/ penalidade (F10) |
| `service/ProjetoMesaService` (criar+QR, evolucao, remover, auditoria+evento, `projetosSemEvolucao`) + `AuditoriaProjetoMesaController` | `Services/ProjetoMesaService.php` (auditoria dobrada, sem service extra) | P13 — cria `ATIVO`+`qr_code_totem`; `evolucao` reativa `ABANDONADO→ATIVO`; `auditar` grava auditoria + espelha `status` + publica `projeto.mesa.abandonado.event` só se `ABANDONADO` |
| `service/Parametro5SService` (listar/buscar/atualizar/`obterInteiro`) | **nada dedicado** — CRUD trivial no `Model`/Eloquent | P9 — sem service forçado (regra trivial; Builder escolhe controller→Model) |
| `service/QrCodeService` (ZXing PNG) | conteúdo `fablab://projeto-mesa/{id}` + `chillerlan/php-qrcode` no endpoint | P9/P13 — **create** justificado (única dep nova; contrato PNG preservado) |
| `service/ProjetoMesaAuditoriaScheduler` (06:00 + flag + `diasParaAuditoriaProjeto` default 15) | `Schedule::call(...)->dailyAt('06:00')` + `config/producao.php` | P15 — só loga `warn` com os pendentes (auditoria é manual/Admin); flag desliga sem erro |
| `service/ProducaoEventPublisher` (6 `convertAndSend` fail-soft) | `Events/*.php` (`ShouldQueue`, fila `database`) | P14 — 6 nomes + payloads preservados (incl. `advertencia.limite.atingido.event` upstream); dispatch nunca estoura 500 |
| `consumer/EncomendaCriadaListener` (auto-insert FILA idempotente) | `Listeners/EncomendaCriadaListener.php` do evento `encomenda.criada.event` | P14 — `idEncomenda` nulo = warn/ignore; existente = no-op; cria `FILA`+`ordem 0` |
| `consumer/NivelAlteradoListener` (log-only) | `Listeners/NivelAlteradoListener.php` do evento `nivel.alterado.event` | P14 — só `Log::info` (sem persistência, sem model) |
| `consumer/EstoqueConsumoListener` (acumula consumo) | **nada** — sem produtor de `estoque.consumo.realizado.event` no monólito | P14 — ver §M6.3 (NÃO planejado; pendência §M6.5 R6) |
| `service/JwtService`/`JwtProperties` + `RabbitConfig` + `GlobalExceptionHandler` + `JwtAuthenticationFilter` | **nada** | P14 — JWT é do Auth; fila `database` cobre M6; Handler é do `Shared` |
| `Contracts/` | `Contracts/ProducaoContract.php` | P14 — **create** justificado, **congelado em 2 métodos** (`resumoKanban`, `tarefasAtivasPorResponsavel`) p/ M8/Dashboard; só o consumido |
| seeds (V17 `parametro_5s` — verificar) | verificação P15 | P15 — se V17 trouxer inserts, viram seeder; senão, sem seed inventado |
| Testes (unit/integration/security/mensageria) | `tests/Unit/Producao/*` + `tests/Feature/Producao/*` + slice 401/403 + vínculo | P16 — `php artisan test` (pgsql `fablab_testing`), cobertura ≥ 80% |

Rotas finais (PT, `/api/producao/**` exatas — ver P4–P9): `projetos` (`POST/GET`
`?status=&idResponsavel=`, `GET /{id}`, `PUT /{id}`, `PUT /{id}/status`, `DELETE /{id}`),
`tarefas` (`POST`, `GET` `?idProjeto=&idResponsavel=&status=`, `GET /{id}`, `PUT /{id}`,
`PUT /{id}/status`, **`PATCH /{id}`** — concluir própria, `DELETE /{id}`), `kanban` (`POST`,
`GET` `?status=`, `GET /encomenda/{idEncomenda}`, `GET /encomenda/{idEncomenda}/historico`,
`PUT /{id}/mover`, `DELETE /{id}`), **`kanban/encomenda/{idEncomenda}/consumo`** (`POST` upsert,
`GET` lista), `maquinas` (`POST/GET` `?status=`, `GET /{id}`, `PUT /{id}`, `PUT /{id}/status`,
`POST /{id}/uso`, `PUT /{id}/uso/{idUso}/fim`, `GET /{id}/historico`, `DELETE /{id}`),
`setores` (`POST/GET` `?ativo=`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`,
`POST|DELETE /{id}/materiais[/{idMaterial}]`, `POST|DELETE /{id}/sinalizacoes[/{idSinalizacao}]`,
`POST /{id}/checklist`, `PUT|DELETE /{id}/checklist/{idItem}`, `POST /{id}/responsaveis` —
Admin, `GET /{id}/responsaveis`, `DELETE /{id}/responsaveis/{idResponsavel}`),
`inspecoes-5s` (`POST`, `GET` `?idSetor=`, `GET /{id}`, `DELETE /{id}` — Admin),
`advertencias` (`POST` — Admin, `GET`, `GET /{idFuncionario}`), `projetos-mesa` (`POST`,
`GET` `?status=`, `GET /{id}`, `PUT /{id}`, `PUT /{id}/evolucao`, `GET /{id}/qrcode` PNG,
`GET /{id}/auditorias`, `DELETE /{id}`), `auditorias-projeto-mesa` (`POST` — Admin,
`GET /{idProjetoMesa}`), `parametros-5s` (`GET`, `GET /{id}`, `PUT /{id}` — Admin).

## M6.3. Decisões de enxugamento (código mínimo obrigatório)

**Reusado do contrato (regra preservada, roupagem Laravel):**
- Tabelas/colunas/constraints/índices/UKs V1–V18 idênticos (só qualificados `producao.`);
  `version` em `encomenda_kanban` (default 0); `id_encomenda` UNIQUE no Kanban; `id_mesa` BIGINT
  (drift V15); `qr_code_totem` nullable preenchido pós-insert.
- Fluxos F1–F12, RBAC por endpoint **com os deltas §M6.1.4**, validações, `recalcularStatus`
  server-side, contador de advertências + limite 3, QR vinculado ao id, auditoria com espelho de
  status, scheduler 06:00 + flag, idempotências (kanban-existente, consumo-upsert, ENTREGUE-1ª-vez).
- Responsáveis opacos sem join cross-schema (precedente M4/M5); dados de Vendas chegam por
  `VendasContract` (LINKA) + evento `encomenda.criada`; baixa chega por `EstoqueContract`.
- `Schedule` no lugar de `@Scheduled`; `Event/Listener` no lugar de `RabbitTemplate`;
  Eloquent+`version` manual no lugar de JPA+`@Version`; `Resource` no lugar de `Response::from`;
  `FormRequest` no lugar de records+Bean Validation.

**`create` justificado (só o que não existe e o requirements/DoD exige):**
1. Migrations `database/migrations/producao/` + extensão aditiva de `DB_SEARCH_PATH`
   (`…financeiro,producao`) — schema único exige reescrita em PHP (P1).
2. `PATCH /tarefas/{id}` (concluir própria tarefa, vínculo tarefa-responsável) — sem equivalente
   no Java (`PUT /status` usa vínculo do projeto); exigido pelo despacho (compat M8/`PATCH
   /api/tasks/{id}`) (P4/P10).
3. `POST|GET /kanban/encomenda/{idEncomenda}/consumo` (upsert da BOM final) — F11 exige o registro;
   o Java só acumulava via listener de evento sem produtor (caminho morto no monólito) (P5/P11).
4. `Contracts/ProducaoContract.php` (**congelado em 2 métodos**) — fronteira in-process exigida
   pela arquitetura p/ M8/Dashboard; não existe no Java (era REST/Eureka) (P14).
5. `chillerlan/php-qrcode` (puro-PHP, sem ext) — única dep nova; sem equivalente nativo p/ servir
   o PNG do contrato `GET /{id}/qrcode` (P9).
6. `config/producao.php` (flag `auditoria_scheduler_enabled`, default on) — padrão M3/E10 p/
   schedulers com kill-switch (P15).
7. Slice de testes security 401/403 por rota + vínculo (PATCH alheio→403, mover não-responsável→403)
   + lock 409 + idempotências — DoD exige; Java não é reusável (P16).

**Explicitamente NÃO planejado (fora de escopo / especulativo):**
- `Repository/`, `Mapper/`, `DTO/` mutáveis, `config/exception` do módulo, `JwtService`/
  `ProducaoPrincipal`/`AcessoService`/`NivelAcesso` próprios, `Parametro5SService` dedicado
  (trivial dobrado no Model), segundo service de auditoria (dobrado em `ProjetoMesaService`),
  `api/` sem consumidor, re-export/helper "futuro".
- Consumer de `estoque.consumo.realizado.event` (`EstoqueConsumoListener`): **sem produtor** no
  monólito (M3 não emite; M5 registra a lacuna) — o acumulado de consumo é escrito pelo endpoint
  P5/P11; reavaliar se um produtor surgir (pendência §M6.5 R6).
- Listener próprio p/ `orcamento.aprovado.event` (o Java não tem: o fluxo chega via
  `encomenda.criada.event`); consumer de `compra.solicitada.event` (sem upstream no Produção).
- Peso/reordenação de checklist, nota numérica 5S, ranking/pendências 5S, Top-1/semana-livre,
  chamado de máquina com relatório, preventiva de máquina (citações de despacho antigo sem
  entidade/migration upstream — sem tabela, sem endpoint).
- Proposta/aprovação de setor como workflow persistido (sem tabela upstream): não-Admin sem
  vínculo recebe 403 e o Admin executa (aprovação = ação do Admin, sem máquina de estado nova).
- Documentos/anexos de projeto, saída de projeto, impressão física de QR/sinalização, upload de
  fotos de setor (persiste só URL, como no Java), PDF/CSV/export, portal do membro, contas
  derivadas além dos shapes (M8 deriva via `ProducaoContract`).
- Validação de responsáveis via `RhContract` por escrita (opacos, precedente M4/M5); join
  cross-schema `producao↔rh/vendas/estoque`; Eureka/Gateway/banco-por-serviço/HTTP entre módulos;
  Rabbit além do necessário; seed inventado.

## M6.4. Ordem de execução (topológica)

```
P1 migrations ─→ P2 models/enums ─┬→ P3 policies ─→ P4 controllers proj/tarefas ─┐
                                   ├→ P3 ─→ P5 controllers kanban/consumo ───────┤
                                   ├→ P3 ─→ P6 controllers maquinas ────────────┤→ P14 events/contract ─┐
                                   ├→ P3 ─→ P7 controllers setores ─────────────┤→ P14 ─────────────────┤→ P16 testes ─→ P17 gate
                                   ├→ P3 ─→ P8 controllers 5S ─────────────────┤→ P14 ─────────────────┤
                                   ├→ P3 ─→ P9 controllers mesa/auditoria/param ┤→ P14 ─────────────────┘
                                   ├→ P10 services proj/tarefas ───────────────┤→ P14 ──────────────────┘
                                   ├→ P11 services kanban/consumo ─────────────┤→ P14
                                   ├→ P12 services maquina/setor ──────────────┤→ P14
                                   └→ P13 services 5S/advert/mesa ─────────────┘→ P14 ─→ P15 scheduler ─→ P16
P1 ─→ P15 seedverificação ─→ P16   (P4 × … × P13 paralelizáveis após P2/P3)
```

## M6.5. Riscos e mitigações

| # | Risco | Mitigação (task) |
| :--- | :--- | :--- |
| R1 | `DB_SEARCH_PATH` sem `producao` quebra `migrate:fresh`/wipe | P1: extensão aditiva (`…financeiro,producao`) + `migrate:fresh` verde em DB limpo |
| R2 | Deltas RBAC Java×matriz (máquina cadastro, responsáveis de setor, kanban-incluir, mesa-Estagiário) contestados | §M6.1.4 documenta cada delta; P3 implementa via `Policy`; P16 cobre permitido/negado por papel; reversível por Gate |
| R3 | `version` concorrente em teste pgsql | P16 testa duplo mover com mesma `version` → 1×200 + 1×409 (precedente M4); sem sqlite (pgsql `fablab_testing`) |
| R4 | `VendasContract::dadosEncomenda` quebra M4 (contrato já commitado) | P11: consumo **só-leitura** do método existente, sem alterar M4; testes Vendas sem regressão em P16 |
| R5 | Baixa duplicada no Estoque (retry mover→ENTREGUE, duplo dispatch) | P11: publica/baixa só na 1ª transição a `ENTREGUE` + `EstoqueContract::baixarConsumo` idempotente por `(CONSUMO,id_encomenda)`; P16 testa duplo mover |
| R6 | `estoque.consumo.realizado.event` cobrado como lacuna (listener Java existe) | §M6.3 documenta: sem produtor no monólito; consumo via endpoint P5; escalar ao Orquestrador se PO quiser produtor |
| R7 | `advertencia.limite.atingido.event` sem consumidor parecer duplicado | P14 preserva 1:1 (M7/Notification decide o consumo); sem consumer inventado em M6 |
| R8 | QR PNG sem lib nativa no PHP | P9: `chillerlan/php-qrcode` (puro-PHP); fallback (só sob ordem): servir só a string e o front renderizar — escalar, não improvisar |
| R9 | Publicar evento quebrando o request (fila fora) | P14: dispatch nativo fila `database`; falha de fila nunca estoura 500 (fail-soft preservado) |
| R10 | V15 `id_mesa` BIGINT × requirements String (drift) | P1 usa BIGINT (SQL prevalece); registrado em §M6.1.1, `docs/` intocado |
| R11 | Período experimental mal aplicado (formal contando p/ limite) | P13: com flag on, auto-advertência não incrementa penalidade; P16 cobre on/off |

---

# M7 — módulo `notification` (Notification, CONSUMIDOR central) — estratégia de migração Java → Laravel

> Despacho do Orquestrador (2026-10-07). **Estende M0–M6 sem sobrescrever.** Referência de regra
> (reescrever em PHP, nunca mover Java): `notification-service/src/main/java/com/fablab/notification/**`
> (~26 arqs: 1 controller, 7 DTOs, 1 entity, 1 repository, 3 services, config) + migrations
> `V1–V2` + `docs/_archive/2026-10-19/07-Notification_Service.md` (canônico de domínio: lifecycle PENDENTE/ENVIADA/LIDA,
> `notificacao_historico`, `configuracao_canal`, revisão do Admin, expurgo 90d) + D-1..D-7
> (concluídos no Java: lista, isolamento JWT, sino/badge, preferências, history).
> Base pronta: `App\Modules\Auth|Rh|Estoque|Vendas|Financeiro|Producao` (todos emitem eventos —
> M7 é o **CONSUMIDOR central**, sem produtor próprio) + `Shared`, padrões M0–M6 (PHP 8.3/Laravel 12,
> PHPUnit pgsql `fablab_testing`, Handler JSON, blacklist, Gate admin, migrator glob,
> `DB_SEARCH_PATH` com 6 schemas — estender aditivamente p/ `notification`).
> **Nunca `docs/`; nenhum código agora; não commitar.**

## M7.1. Estratégia contrato-preservado / código-descartado

1. **Reaproveitado (regra, não código):** endpoints/verbos/shapes do `NotificacaoController`
   (unread-count, listar paginada, ler, ler-todas, preferences, history) + queries do
   `NotificacaoRepository` (`countByIdUsuarioAndLidaFalse`, `buscarDoUsuario`, `buscarHistorico`,
   `marcarTodasComoLidas`) + allowlists `TIPOS`/`CANAIS` + matriz default de preferências
   (tudo true exceto `sistema/push=false`) + `LinkValidator` (allowlist caminho-interno/`http(s)`)
   + lifecycle `docs/_archive/2026-10-19/07` §4–§5 (status de entrega, `data_envio`/`data_leitura`, `id_referencia`,
   histórico move-on-review, expurgo >90d, `configuracao_canal` EMAIL on / WHATSAPP off,
   reenvio de PENDENTE, sem opt-out no MVP) + nomes/payloads dos 18 eventos assinados.
2. **Descartado:** `entity/repository/service/config` Spring, `RabbitTemplate`/`RabbitConfig`,
   `RestClient`/`DiscoveryClient`, `@PreAuthorize`/`SecurityConfig`, `JwtService` próprio (JWT é o
   guard `api` do Auth/M1), `AuthPrincipal` (vira `NotificationPrincipal` + `auth()->user()`),
   `ErrorResponse`/`GlobalExceptionHandler` próprios (Handler JSON central do `Shared`),
   Eureka/Flyway/banco-por-serviço.
3. **Ordem:** N1 migrations → N2 models/enums/rule → N3 policies → N4–N5 controllers/requests/resources
   → N6–N7 services → N8 listeners+contract → N9 schedulers → N10 seed-verificação → N11 testes →
   N12 gate. N4–N5 exigem N2+N3; N6–N7 exigem N2; N8 exige N7 (+N6 p/ tipos); N11 exige tudo.
4. **Ajustes de empacotamento permitidos (sem novo contrato) + reconciliações conscientes:**
   - D1 — Base path `/notifications` (serviço Java) → `/api/notification/notificacoes/**`
     (prefixo do módulo per architecture + recurso PT), com **alias** `GET /api/notificacoes` (lista)
     e `GET /api/notificacoes/nao-lidas/contagem` (mesmos controllers — o App Shell/sino consome
     `/api/notificacoes`; precedente M1: Auth expõe `/api/usuarios`, `/api/permissoes` fora do
     prefixo). `PATCH /{id}/read` → `PATCH .../{id}/ler` (**Java prevalece** sobre o `PUT /{id}/ler`
     de `docs/_archive/2026-10-19/07` §6 — contrato implementado vence o doc preliminar).
   - D2 — V1 `notificacao` = Java V1 **+ colunas lifecycle de `docs/_archive/2026-10-19/07` §4** (`status` CHECK
     `PENDENTE/ENVIADA` default `PENDENTE`, `data_envio`, `data_leitura`, `id_referencia` nullable):
     `lida` boolean do Java é **mantido** (queries `countByIdUsuarioAndLidaFalse`/`buscarDoUsuario`
     1:1); o `LIDA` do status de `docs/_archive/2026-10-19/07` ≡ `lida=true` (leitura = read-model Java; `status`
     governa só a **entrega**, que o reenvio consulta).
   - D3 — V2 = Java V2 1:1 (`tipo`/`canal`/`link` + índice `usuario+tipo`) **+ `create`
     `notificacao_historico`** (architecture V2; colunas espelhadas da `notificacao` +
     `data_revisao_admin` + `id_admin_revisor`, per `docs/_archive/2026-10-19/07` §4). Drift registrado: architecture
     chama V2 de `notificacao_historico` e o despacho de `notificacao_tipo_canal_link` —
     **ambos cabem na V2**, sem conflito.
   - D4 — V3 = `configuracao_canal` (`docs/_archive/2026-10-19/07` §4: `id`, `canal` UK, `habilitado`, `parametros`
     JSON nullable) **+ `preferencia_notificacao`** (`tipo`, `canal`, `habilitado`, UK par —
     **create** justificado: a matriz do Java era `AtomicReference` **in-memory**, que não sobrevive
     em PHP stateless; sem tabela, o `PUT preferences` do contrato é morto). V4 = seeds idempotentes
     (V4a canais: EMAIL true / WHATSAPP false; V4b matriz default Java) **dentro da migration V4**
     (upserts, padrão Auth V6/V7).
   - D5 — **1 Listener por módulo-origem (5 classes) em vez de 18**: cada método `handle*`
     vincula 1 evento (nome+payload preservados) e delega a `NotificationDispatchService`
     (precedente M3–M6 era 1 classe/evento; aqui seriam 18 delegações de 1 linha — boilerplate).
   - D6 — Schedulers em `routes/console.php`: reenvio `->everyThirtyMinutes()` (equivale ao
     `*/30 * * * *` do despacho) + expurgo `->dailyAt('03:00')` (**alinha ao padrão 03:00 do
     monólito**; `docs/_archive/2026-10-19/07` §5 pede só "job diário", sem hora) + `config/notification.php` com
     kill-switches (padrão M3/E10).
   - D7 — Destinatário: `RhContract::dadosDestinatario(idFuncionario): ?array{idUsuario,nome,email}`
     (**modify** aditivo no Rh — `rh.pessoa.id` **é** o `id_user` do Auth, `funcionario.id_pessoa`
     resolve; `email` = `contato` quando formato-email, senão null) + `AuthContract::adminIds(): array`
     (**modify** aditivo no Auth — broadcasts p/ Admin; sem leitura cross-schema de
     `auth.user_permissions`). Ambos **aditivos, sem quebra** (precedente M3/E9).
   - D8 — E-mail MVP via `Mail::raw` **fail-soft** (`try/catch` → PENDENTE p/ retry; `Mail::fake`
     em teste): inapp nasce `ENVIADA` (sem dependência externa); email tenta envio real quando
     `configuracao EMAIL` on + pref `(tipo,email)` on + email resolvido; **push ≡ inapp persistido**
     (polling no GET — sem infra push no MVP); **WhatsApp = só seed off, zero código**
     (`docs/_archive/2026-10-19/07` §2: "não será implementado no MVP").
   - D9 — Únicos 2 endpoints além do Java (ambos `docs/_archive/2026-10-19/07` §6, Admin): `POST .../notificacoes/revisar`
     (`{ids[]}` → move p/ histórico; sem ele, `notificacao_historico` + expurgo são código morto) e
     `GET|PUT .../configuracao-canais` (sem ele, o seed de canais é ingerenciável).
   - D10 — `access.rfid.event` **NÃO assinado** (ver §M7.3): payload publicado cobre só leituras
     **válidas** (`idUser` não-nulo, sem flag de negação); notificar cada acesso válido é spam sem
     caso de uso em D-1..D-7/`docs/_archive/2026-10-19/07`; negados não têm destinatário. Pendência PO (§M7.5 R7).
   - D11 — `custo.calculado.event` (Financeiro-interno, fora da lista do despacho) sem listener.
   - D12 — `link` das notificações criadas por evento = **`null`** (válido p/ `LinkValidator`;
     sem inventar paths do front — ele navega pela lista central `/notificacoes`).
   - Chaves JSON **EN preservadas 1:1** (`id/type/title/body/read/createdAt/link/channel`,
     `items/total/page/size/pageSize/totalPages`, `{count}`, `{items,total}`, `{lidas}`) — é o que
     o front D-1 parseia; só os **paths** viram PT. Erros: `IllegalArgumentException`→400 (página/
     tamanho/período/preferências, padrão M0 Handler), `LinkInvalidoException`→**422** (D-6),
     `Forbidden`→403, ausente→404.

## M7.2. Mapeamento contrato (Java/Spring + docs/_archive/2026-10-19/07) → Laravel

| Origem (referência) | Destino (`backend/`, Laravel) | Peça / observação |
| :--- | :--- | :--- |
| `db/migration/V1__notificacao.sql` + lifecycle `docs/_archive/2026-10-19/07` §4 | `database/migrations/notification/*V1*.php` | N1 — Java V1 1:1 + `status` CHECK(PENDENTE/ENVIADA) default PENDENTE + `data_envio/data_leitura/id_referencia` nullable; `CREATE SCHEMA IF NOT EXISTS notification`; `DB_SEARCH_PATH` += `,notification` (modify aditivo) |
| `V2__notificacao_tipo_canal_link.sql` + architecture V2 | `*V2*.php` | N1 — alter Java 1:1 (`tipo/canal/link` + índice) + `create notificacao_historico` (D3) |
| `docs/_archive/2026-10-19/07` §4 `configuracao_canal` + matriz in-memory | `*V3*.php` | N1 — `configuracao_canal` 1:1 + `preferencia_notificacao` (create D4) |
| seed architecture V4 (EMAIL on/WHATSAPP off) + matriz default | `*V4*.php` (upserts) | N1/N10 — dentro da migration, idempotente (padrão Auth V6/V7) |
| `entity/Notificacao` | `Models/Notificacao.php` + `Models/NotificacaoHistorico.php` + `Models/ConfiguracaoCanal.php` + `Models/PreferenciaNotificacao.php` | N2 — `$table='notification.*'`, `$timestamps=false`, `$fillable`, casts; `StatusEntrega` enum (`PENDENTE/ENVIADA`); `TipoNotificacao`/`CanalNotificacao` como **consts allowlist** no service (não enums dedicados — Java usa `Set.of`, validação é `in:`, sem CHECK no banco) |
| `repository/NotificacaoRepository` (4 queries) | **nada** — Eloquent no service | N2/N6 — proibido `Repository` por hábito (não planejar) |
| `dto/*Response/Paged/UnreadCount/HistoryPayload` (shapes EN) | `Http/Resources/*` (keys EN 1:1) | N4–N5 — `NotificationResource`, `PagedNotifications` via `Resource::collection` + meta (`pageSize/totalPages` aliases), `UnreadCount {count}`, `History {items,total}`, `{lidas}` |
| `dto/*Request` implícitos + Bean Validation | `Http/Requests/*FormRequest` | N4–N5 — `page/size|pageSize` (default 10, 1..100), `type/read/channel/search/period`; `RevisarRequest {ids[]}`, `PreferenciaRequest` (matriz estrita Java 1:1), `CanalRequest {habilitado, parametros?}` |
| `dto/AuthPrincipal` | `NotificationPrincipal` (idUser/role/admin, de `auth()->user()`) | N3 — JWT é o guard `api` do Auth (M1) |
| `config/SecurityConfig` + `@PreAuthorize hasRole('ADMIN')` (preferences/history) | `auth:api`+`blacklist`+`Policies/NotificationPolicy`+`can:admin` | N3 — inbox = qualquer autenticado **incl. Recrutando** (matriz `Minhas notificações ✅` p/ 0–4 prevalece); admin-only: preferences/history/revisar/config (matriz `Revisar/Histórico + Configuração de Canal = Editar/Admin`) |
| `service/NotificacaoService` (contar/listar/ler/ler-todas/prefs/history/registrar + `normalizar/resolverPeriodo`) | `Services/NotificacaoService.php` | N6 — **1:1** (+ `revisar(ids, admin)` move→histórico, D9; `read` ≡ `status=LIDA` via `lida`) |
| `service/LinkValidator` | `Support/LinkValidator.php` (static final 1:1) | N2 — **reuse** (consumido por `registrar`; sem virar Rule — sem `FormRequest` com `link`) |
| fan-in de 18 eventos + reenvio + expurgo + gates de canal (sem upstream) | `Services/NotificationDispatchService.php` | N7 — **create** justificado (único ponto consumido por 5 listeners + 2 schedulers; `notificar/notificarAdmins/reenviarPendentes/expurgarHistorico`; inapp sempre — sem opt-out `docs/_archive/2026-10-19/07` §8; email/push sob prefs+config) |
| 18 eventos (nomes+payloads preservados) | 5 `Listeners/*NotificacaoListener.php` (Rh/Estoque/Vendas/Financeiro/Producao) | N8 — D5; `Event::listen()` em `AppServiceProvider` (modify); malformado = warn/ignore (nunca 500 — protege o produtor em fila sync); `access.rfid` fora (D10) |
| `Contracts/` | `Contracts/NotificationContract.php` (**congelado em 2**: `naoLidas(int): int`, `inbox(int,page,size,tipo,lida): array`) | N8 — **create** justificado (M8/Dashboard consome contagem+inbox; só o consumido) |
| `RhContract` / `AuthContract` | +`dadosDestinatario()` / +`adminIds()` (aditivos) | N8 — **modify** D7 (precedente M3/E9; sem quebra; sem join cross-schema) |
| reenvio PENDENTE `*/30` + expurgo >90d (despacho + `docs/_archive/2026-10-19/07` §5) | `routes/console.php` + `config/notification.php` | N9 — `everyThirtyMinutes` + `dailyAt('03:00')`, flags default on |
| `JwtService`/`RabbitConfig`/`GlobalExceptionHandler`/`NotificacaoResponse`(legado) | **nada** | N8 — JWT é do Auth; fila `database` cobre M7; Handler é do `Shared`; `NotificacaoResponse` (sem tipo/canal/link) é legado — shape canônico é `NotificationResponse` |
| Testes | `tests/Unit/Notification/*` + `tests/Feature/Notification/*` + slice 401/403 + isolamento | N11 — `php artisan test` (pgsql `fablab_testing`), cobertura ≥ 80% |

Tabela evento → destinatário(s) → `tipo` (N8 implementa; `titulo/mensagem` PT com ids; `link=null` D12;
`id_referencia` = id da entidade; `lancamento.vencido` **só admins** — RBAC Financeiro):

| Evento(s) | Destinatário(s) | `tipo` |
| :--- | :--- | :--- |
| `nivel.alterado`, `horas.validadas`, `extrato.mensal.horas`, `certificado.aprovado/rejeitado`, `advertencia.registrada` | funcionário (`RhContract::dadosDestinatario`) | `pessoas` / `producao` (advertência) |
| `certificado.solicitado`, `compra.solicitada` (Estoque e Financeiro — mesmo NAME, 2 classes), `encomenda.criada`, `encomenda.status.alterado`, `orcamento.aprovado`, `kanban.status.alterado`, `producao.concluida`, `estoque.baixo`, `lancamento.vencido` | admins (`AuthContract::adminIds()`) | origem (`estoque/encomenda/financeiro/producao`; `orcamento.aprovado`→`encomenda`; `kanban.*`→`producao`) |
| `advertencia.limite.atingido`, `projeto.mesa.abandonado` | funcionário **+** admins | `producao` |
| `emprestimo.atrasado` | `idPessoa` direto (= `id_usuario`) | `estoque` |
| `producao.status.alterado` | `idUsuario` ?? admins | `producao` |

Rotas finais (PT; `can:admin` onde indicado — ver N4/N5):
`GET /api/notification/notificacoes` (+alias `GET /api/notificacoes`), `GET .../nao-lidas/contagem`
(+alias `GET /api/notificacoes/nao-lidas/contagem` — sino/badge), `PATCH .../{id}/ler`,
`POST .../ler-todas`, `POST .../revisar` (admin, D9), `GET .../historico` (admin),
`GET|PUT .../preferencias` (admin), `GET|PUT .../configuracao-canais` (admin, D9).

## M7.3. Decisões de enxugamento (código mínimo obrigatório)

**Reusado do contrato (regra preservada, roupagem Laravel):**
- Colunas/índices Java V1–V2 1:1 (só qualificados `notification.`); queries do repository
  (inbox sempre `where id_usuario` + `order criada_em DESC`; history com `LIKE`+filtros; update em
  massa p/ ler-todas); paginação 1-based size≤100; `periodo` `24h/30d`; matriz default de prefs;
  allowlists `TIPOS/CANAIS`; `LinkValidator` byte-a-byte; seed EMAIL on / WHATSAPP off.
- Fluxos D-1..D-7 + `docs/_archive/2026-10-19/07` §5 (receber→PENDENTE→enviar→ENVIADA; ler→`lida`+`data_leitura`;
  revisar→histórico; expurgo 90d; reenvio PENDENTE) + RBAC matriz (inbox 0–4, resto Admin).
- `Schedule` no lugar de job agendado; `Event/Listener` no lugar de RabbitMQ; Eloquent no lugar
  de JPA+repositories; `Resource` no lugar de records de saída; `FormRequest` no lugar de
  Bean Validation; `Mail` no lugar de SMTP manual.

**`create` justificado (só o que não existe e o requirements/DoD exige):**
1. Migrations `database/migrations/notification/` + extensão aditiva de `DB_SEARCH_PATH`
   (`,notification`) — schema único exige reescrita em PHP (N1).
2. `notificacao_historico` (architecture V2 + `docs/_archive/2026-10-19/07` §4) — sem ela, revisão/expurgo são mortos (N1).
3. `preferencia_notificacao` — matriz in-memory não sobrevive em PHP; sem tabela o `PUT`
   preferences é morto (N1).
4. `StatusEntrega` enum (2 casos) — CHECK de entrega sem equivalente nativo (N2).
5. `POST .../revisar` + `GET|PUT .../configuracao-canais` — únicos endpoints sem upstream Java,
   ambos `docs/_archive/2026-10-19/07` §6; sem eles, V2-histórico e seed de canais são mortos (N5).
6. `NotificationDispatchService` — fan-in de 5 listeners + 2 schedulers;Inbox service não serve
   (consumidores diferentes, regra de entrega ≠ regra de leitura) (N7).
7. `RhContract::dadosDestinatario` + `AuthContract::adminIds` (modifies aditivos) — fronteira
   in-process exigida pela arquitetura; sem eles, ou join cross-schema ou broadcast impossível (N8).
8. `NotificationContract` (**congelado em 2 métodos**) — M8/Dashboard consome; não existe no Java
   (era REST/gateway) (N8).
9. `config/notification.php` (2 kill-switches default on) — padrão M3/E10 p/ schedulers (N9).
10. Alias `GET /api/notificacoes` (+contagem) — App Shell/sino consome esse path; 2 linhas de rota,
    mesmos controllers (N4).
11. Slice de testes security 401/403 + isolamento (dono/Admin) + schedulers manuais — DoD exige;
    Java não é reusável (N11).

**Explicitamente NÃO planejado (fora de escopo / especulativo):**
- `Repository/`, `Mapper/`, `DTO/` mutáveis, `config/exception` do módulo, `JwtService`/
  `AuthPrincipal` próprios, `NotificacaoResponse` legado (shape sem tipo/canal/link), `api/` sem
  consumidor, re-export/helper "futuro".
- Listener de `access.rfid.event` (D10 — sem destinatário/caso de uso; escalar ao PO, não
  implementar spam), listener de `custo.calculado.event` (fora da lista do despacho),
  `encomenda.criada`→ funcionário/cliente (evento não carrega criador; admins, documentado).
- Envio real WhatsApp/push-provider, `PaymentProcessor`-like p/ canais (sem provedor no MVP —
  `Mail::raw` fail-soft + `Log::info` de auditoria), SMTP credenciais versionadas (`.env`, nunca
  no seed — `parametros` do seed = `{}`), template engine de e-mail (corpo = `mensagem`, sem Blade),
  anexos, agendamento de envio futuro, digest/resumo periódico, opt-out por usuário (`docs/_archive/2026-10-19/07` §8:
  tudo obrigatório — prefs governam só email/push, **inapp sempre**).
- `GET /interacoes`-like: history **não** pagina server-side (`HistoryPayload items/total` integral,
  front pagina client-side — D-4/Java 1:1); CSV/PDF de notificações; `DELETE` de notificação
  (sem upstream — revisão move, nunca apaga ativo); `PUT /{id}/ler` (verbo é PATCH, D1);
  validação de destinatário via `RhContract` por escrita (ids opacos, precedente M4/M5);
  Eureka/Gateway/banco-por-serviço/HTTP entre módulos; Rabbit além do necessário; seed inventado
  além de V4; join cross-schema `notification↔rh/auth` (tudo via `Contract`).

## M7.4. Ordem de execução (topológica)

```
N1 migrations ─→ N2 models/enums/support ─┬→ N3 policies ─→ N4 controllers inbox ─┐
                                           ├→ N3 ─→ N5 controllers admin ─────────┤
                                           ├→ N6 service inbox ──────────────────┤→ N8 listeners/contract ─┐
                                           └→ N7 dispatch ───────────────────────┤→ N8 ─→ N9 schedulers ────┤→ N11 testes ─→ N12 gate
N1 ─→ N10 seed-verificação ─→ N11   (N4 × N5 × N6 × N7 paralelizáveis após N2/N3)
```

## M7.5. Riscos e mitigações

| # | Risco | Mitigação (task) |
| :--- | :--- | :--- |
| R1 | `DB_SEARCH_PATH` sem `notification` quebra `migrate:fresh`/wipe | N1: extensão aditiva (`…,producao,notification`) + `migrate:fresh` verde em DB limpo |
| R2 | Reconciliação V1/V2/V3 (Java × `docs/_archive/2026-10-19/07` × architecture) contestada | §M7.1 D2–D4 documenta cada delta; N1 confere `.sql` + `docs/_archive/2026-10-19/07` §4 coluna a coluna; N12 grep de `lida`+`status` |
| R3 | `RhContract`/`AuthContract` aditivos quebram M1/M2 (contratos commitados) | N8: métodos **aditivos** (interface + default impl); testes Auth/Rh sem regressão em N11 |
| R4 | Broadcast p/ admins sem nenhum Admin seedado (lista vazia) | N7: `adminIds()` vazio = warn/log + zero rows (nunca 500); N11 cobre |
| R5 | `compra.solicitada.event` com 2 classes (Estoque + Financeiro, mesmo NAME) parecer duplicado | N8: preservado 1:1 (cada lado publica o seu, como no Java); 2 binds por classe, `tipo` por origem |
| R6 | `Vendas/ProducaoStatusAlteradoEvent` × `Producao/ProducaoStatusAlteradoEvent` (mesmo NAME) | N8: bind **pela classe do produtor** (Producao); sem re-dispatch inventado |
| R7 | `access.rfid` cobrado como lacuna (consta na lista do despacho) | §M7.1 D10 + §M7.3 documentam o NÃO-escopo; N12 verifica ausência do bind; **escalar ao PO**: se quiser alerta de acesso negado, Auth precisa publicar evento com flag+dono — fora de M7 |
| R8 | Publicar evento quebrando o request do produtor (fila fora) | N8: listeners com `try/catch` + warn (precedente M3/M4); dispatch nunca estoura no produtor (consumo é async fila `database`) |
| R9 | E-mail real sem SMTP no ambiente (timeout estourando job) | N7: `Mail::raw` com timeout curto + fail-soft PENDENTE; `N11` usa `Mail::fake`; pré-condição Builder: `MAIL_*` em `.env.example` (sem segredo) |
| R10 | History integral sem paginação estourar memória (D-4 Java) | N6: history lê `notificacao_historico` (pós-revisão, volume baixo) + filtros no banco; documentado como 1:1 |
| R11 | Alias `/api/notificacoes` divergir do prefixo do módulo | N4: alias só 2 GETs, mesmos controllers; N12 grep de paths duplicados com regra divergente |

---

# M8 — módulo `dashboard` (Dashboard, AGREGADOR sem persistência — ÚLTIMO do monólito)

> Despacho do Orquestrador (2026-10-07). **Estende M0–M7 sem sobrescrever. Último módulo.**
> Referência de regra (reescrever em PHP, nunca mover Java):
> `dashboard-service/src/main/java/com/fablab/dashboard/**` (~24 arqs: 2 controllers, 8 dtos,
> 4 services, config) + `src/main/resources/application.yml` (só config — **zero `.sql`: confirmado
> SEM migrations upstream, agregador sem persistência**).
> Base pronta: TODOS os Contracts disponíveis — `AuthContract` (`isAdmin/roleOf`), `RhContract`,
> `EstoqueContract`, `VendasContract`, `ProducaoContract` (`resumoKanban`,
> `tarefasAtivasPorResponsavel`), `NotificationContract` (`naoLidas`, `inbox`) — consumo via
> Contracts in-process (**NUNCA HTTP entre módulos**); `Shared` (Handler JSON, blacklist);
> padrões M0–M7 (PHP 8.3/Laravel 12, PHPUnit pgsql `fablab_testing`, Handler JSON, blacklist,
> Gate admin, migrator glob, `DB_SEARCH_PATH` com 7 schemas — **M8 não estende: sem schema,
> sem models**). **Nunca `docs/`; nenhum código agora; não commitar.**

## M8.1. Estratégia contrato-preservado / código-descartado

1. **Reaproveitado (regra, não código):** shape `DashboardSummary` EN 1:1
   (`tasks{id,title,module,due,dueLabel,urgent}`, `kpis{value,total,delta,deltaTone,restricted}`,
   `ordersByStatus[]`, `machinesByStatus[]`, `activity[]` — é o que o front
   (`frontend/src/lib/types/dashboard.ts`, `fetchSummary`, `DonutChart/MyTasks/KpiGrid/ActivityFeed`)
   parseia); regra de `restricted` **no BACKEND** (só Admin recebe
   `loansOpen/machinesActive/machinesByStatus` — chaves **omitidas** p/ demais, front tipa
   opcionais; nunca só ocultar no front); `maquinaAtiva = DISPONIVEL‖EM_USO`,
   `fatiasPorStatus` (disponivel/success, em_uso/brand, manutencao/warn, default/muted) 1:1;
   `dueLabel` (`Hoje`/`dd/MM`/`Sem prazo`) + `urgent = vence hoje` 1:1; activity
   (solicitação → `act-sol-{id}`, emitido → `act-emit-{id}`, top 8 desc) 1:1; RBAC matriz
   (Summary/KPIs ✅ 0–3 incl. Estagiário restrito, ❌ Recrutando; concluir tarefa ✅ se atribuído).
2. **Descartado:** `DominioClient` (Eureka/`DiscoveryClient` + `RestClient` + reenvio de bearer +
   degrade fail-soft) → Contracts in-process (falha interna = exceção/500 via Handler, sem
   catch-e-degrada inventado — fila/saúde de módulo não existe no monólito); `CompletedTasks`
   (in-memory `rh-cert-*`) → **dono real é o Produção** (`PATCH /api/producao/tarefas/{id}` de M6,
   vínculo tarefa-responsável; marca em memória some); `TaskController` (validação de vínculo via
   bearer + regex `rh-cert-(\d+)`) → alias reuse (ver D4); `JwtService`/`JwtProperties`/
   `JwtAuthenticationFilter` próprios (JWT é o guard `api` do Auth/M1); `SecurityConfig`/
   `RestAccessDeniedHandler`/`RestAuthenticationEntryPoint` (Handler `Shared`); `ErrorResponse`
   próprio; Eureka/gateway/banco-por-serviço.
3. **Ordem:** D1 contracts-aditivos → D2 service → D3 policy → D4 controller/resource/routes →
   D5–D6 testes → D7 gate M8 → D8 gate FINAL. D2 exige D1; D4 exige D2+D3; D5–D6 exigem tudo.
4. **Ajustes de empacotamento permitidos (sem novo contrato) + deltas conscientes:**
   - D1 — Base path `/dashboard` (serviço Java) → `/api/dashboard/**` (prefixo do módulo).
     `GET /summary` → **`GET /api/dashboard/summary` (EN preservada — front `fetchSummary`
     chama `/dashboard/summary`; quebrar o path quebra o App Shell)** + alias
     `GET /api/dashboard/resumo` (PT, 1 linha, mesmo controller — precedente M7/N4).
   - D2 — `PATCH /tasks/{id}` (Java, `rh-cert-*` + `{done:true}` + in-memory) **NÃO vira controller
     próprio**: alias `PATCH /api/tasks/{id}` → `Producao\TarefaController@concluir` (1 linha,
     reuse — precedente M7 alias). Compatibilidade: `concluir` usa `Request` puro (ignora o body —
     `{done:true}` do front passa sem 400); vínculo tarefa-responsável ou Admin senão 403 (M6);
     Recrutando → 403 (`exigeLeitura`); idempotente (já-CONCLUIDA → 200). Task `id` servido pelo
     resumo = **string numérica `"{id_tarefa}"`** (`module: "producao"`) — o front devolve o mesmo
     `id` no PATCH sem parsear prefixo.
   - D3 — Tasks do resumo = **`ProducaoContract::tarefasAtivasPorResponsavel(id_user)`** (minhas
     tarefas do autenticado; `claim id_user` = `id_responsavel`, mesmo espaço de ids — M6
     `concluirPropria` já usa essa equivalência). `due` = `data_fim_prevista` (delta documentado:
     Java derivava `due` de `dataSolicitacao` do certificado; tarefa real tem prazo próprio).
     `tasks rh-cert-*` + filtro `PENDENTE` + `CompletedTasks` **morrem** (sem equivalente: dono
     mudou p/ Produção em M6 por despacho).
   - D4 — KPIs: `notificationsUnread` = `NotificationContract::naoLidas(id_user)` (todos 0–3);
     `ordersActive` = cartões não-`ENTREGUE` via `resumoKanban` (**melhoria real sem contrato novo**:
     Java hardcoded `0`); `ordersByStatus` = fatias de `resumoKanban` (Java servia `[]` —
     agora real: Kanban LINKA Vendas, sem agregação duplicada via `VendasContract`);
     `loansOpen` = `EstoqueContract::emprestimosAbertos()` (**modify** aditivo: `count`
     `ATIVO+ATRASADO` ≡ `?status=ativos` E-7; chamado só se Admin); `machinesActive` +
     `machinesByStatus` via **`ProducaoContract::resumoMaquinas()`** (**modify** aditivo, espelho
     de `resumoKanban`: `status => count`); `delta=null`, `deltaTone="muted"` 1:1;
     `restricted=true` em `loansOpen/machinesActive`, `false` nos demais (matriz regra 6).
   - D5 — Activity = `RhContract::solicitacoesCertificado()` + `::certificadosEmitidos()`
     (**modifies** aditivos; chamado `emitidos` só se Admin — Java 1:1; não-Admin usa as próprias
     solicitações PENDENTE). Polling/refetch = front (sem endpoint de stream no MVP).
   - D6 — `GET /summary` sem input → **sem `FormRequest`** (só `auth:api`+`blacklist`+`can:`);
     PATCH alias reusa o `Request` puro do `concluir` (sem Request novo p/ `{done}`).
   - D7 — `DB_SEARCH_PATH` **não** é estendido (sem schema `dashboard`); `migrate:fresh` precisa
     seguir verde sem migration nova (D7 verifica).

## M8.2. Mapeamento contrato (Java/Spring) → Laravel

| Origem (`dashboard-service/`, referência) | Destino (`backend/`, Laravel) | Peça / observação |
| :--- | :--- | :--- |
| `resources/application.yml` (porta 8088, Eureka, JWT) | **nada** — porta única 8080, sem Eureka, JWT é do Auth/M1 | D4 — sem config própria |
| `db/migration/*.sql` | **inexistentes upstream — nada** (agregador sem persistência, confirmado) | D7 — zero `Model/Migration/Seeder`; `migrate:fresh` verde sem nada novo |
| `dto/DashboardSummary/TaskDto/KpiDto/SliceDto/ActivityItemDto` (shapes EN) | `Http/Resources/DashboardSummaryResource.php` (**1 classe**; tasks/kpis/slices/activity como arrays) | D4 — keys EN 1:1 (front parseia); sem 5 classes DTO (record→array, menos verbo) |
| `dto/CompleteTaskRequest({done})` | **nada** — alias ignora o body (`concluir` usa `Request` puro) | D4 — sem Request novo |
| `dto/AuthPrincipal` | `auth()->user()` + `AuthContract::roleOf/isAdmin` | D3 — JWT é o guard `api` (M1) |
| `config/SecurityConfig` (tudo autenticado) | `auth:api`+`blacklist`+`Policies/DashboardPolicy`+`can:ver-resumo` | D3 — Recrutando (4) → 403 no módulo (matriz); resto 0–3 |
| `service/DominioClient` (5 fan-outs REST/Eureka fail-soft) | 4 **modifies** aditivos em Contracts existentes (D1) | D1 — `Rh::solicitacoesCertificado(?status,?idUsuario)` + `Rh::certificadosEmitidos()` + `Estoque::emprestimosAbertos(): int` + `Producao::resumoMaquinas(): array`; sem quebra (precedente M3/E9, M7/N8); **sem** `FinanceiroContract` (Financeiro segue isolado, matriz exclusivo-Admin — dashboard Java nunca tocou) e **sem** método novo em `Vendas`/`Notification` (o consumido já basta) |
| `service/DashboardService.resumo` | `Services/DashboardService.php` | D2 — **create** justificado (única regra nova: composição via Contracts + filtro restricted + derivações `dueLabel/urgent/fatias`; testável sem HTTP com Contracts mockados) |
| `service/CompletedTasks` (in-memory) | **nada** — dono real Produção (M6) | D2 — sem estado no dashboard (stateless PHP) |
| `controller/DashboardController` (`GET /summary`) | `Http/Controllers/DashboardController.php` (`summary`) + rotas `summary` + alias `resumo` | D4 — só-HTTP (delega ao service, devolve `Resource`) |
| `controller/TaskController` (`PATCH /{id}`) | alias `PATCH /api/tasks/{id}` → `Producao\TarefaController@concluir` | D4 — **reuse 1 linha**, sem controller/request novos (compat §M8.1 D2) |
| `exception/*` + `ErrorResponse` | **nada** (Handler `Shared`) | — |
| Testes | `tests/Unit/Dashboard/*` + `tests/Feature/Dashboard/*` + slice 401/403 + filtro por papel + alias | D5–D6 — `php artisan test` (pgsql `fablab_testing`), cobertura ≥ 80% (módulo) + global |
| Gate M8 + Gate FINAL | D7 (DoD M8) + D8 (monólito inteiro) | verificação, sem código |

Rotas finais (`/api/dashboard/**` + alias tasks — ver D4): `GET /api/dashboard/summary`
(front) + alias `GET /api/dashboard/resumo` (PT, mesmo controller); `PATCH /api/tasks/{id}`
(alias → Producao `concluir`, vínculo M6).

## M8.3. Decisões de enxugamento (código mínimo obrigatório)

**Reusado do contrato (regra preservada, roupagem Laravel):**
- Shape `DashboardSummary` EN 1:1 (o front parseia keys, não paths); `restricted` filtrado no
  backend (omitir chaves p/ não-Admin, como o Java); `maquinaAtiva`, `fatiasPorStatus`,
  `dueLabel/urgent`, activity `act-sol-/act-emit-` top-8 desc, `deltaTone muted`.
- RBAC matriz (resumo 0–3, Recrutando fora; concluir se atribuído/Admin — enforcement já em M6).
- `Schedule`/fila n/a (agregador síncrono; sem eventos próprios — M8 só consome Contracts).

**`create` justificado (só o que não existe e o requirements/DoD exige):**
1. `Services/DashboardService.php` — composição + filtro restricted + derivações; sem ela não há
   agregador (D2).
2. `Policies/DashboardPolicy.php` (`ver-resumo`: 0–3, nega 4) — RBAC do módulo exigido pela
   matriz; `concluir` reuse M6 (D3).
3. `Http/Controllers/DashboardController.php` + `Http/Resources/DashboardSummaryResource.php`
   (**1 Resource**, resto arrays) + `routes.php` + registro em `routes/api.php` + 2 aliases
   (`/resumo`, `PATCH /api/tasks`) — fronteira HTTP do módulo; aliases 1 linha cada, precedente
   M7/N4 (D4).
4. 4 modifies aditivos em Contracts (`Rh` ×2, `Estoque` ×1, `Producao` ×1) — fronteira in-process
   exigida pela arquitetura; cada método com consumidor imediato em D2 (D1).
5. Slices de teste (Unit service + Feature 401/403 + filtro por papel + alias PATCH) — DoD exige;
   Java não é reusável (D5–D6).

**Explicitamente NÃO planejado (fora de escopo / especulativo):**
- `Model/`, `Migration/`, `Seeder/`, `Repository/`, `Mapper/`, `DTO/` mutáveis,
  `config/exception` do módulo, `JwtService` próprio, `FormRequest` (sem input no GET; PATCH
  reusa `Request` puro), `Events/Listeners/Scheduler`, `CompletedTasks`, `DominioClient`/Eureka/
  `RestClient`/HTTP entre módulos, re-export/helper "futuro".
- `FinanceiroContract` (nenhum consumidor; Financeiro exclusivo-Admin e Java nunca agregou —
  M8 não abre a exceção); método novo em `VendasContract`/`NotificationContract` (o consumido
  basta: kanban LINKA vendas; `naoLidas` cobre o sino); endpoint bulk/stream/websocket;
  `GET /notifications/unread/count` (path EN do front p/ sino — dono M7/alias `/api/notificacoes`;
  front ajusta a base, fora de M8); CSV/PDF de dashboard; `PUT /{id}/ler`-like; join
  cross-schema; Rabbit além do necessário; seed inventado.
- `ordersActive` derivado no front (servido pronto, como `dueLabel/urgent/color` — backend é a
  autoridade); `tasks rh-cert-*` (dono mudou p/ Produção em M6 — formato morto).

## M8.4. Ordem de execução (topológica)

```
D1 contracts-aditivos ─→ D2 service ─┬→ D3 policy ─→ D4 controller/resource/routes ─┐
                                      └→ D2 ───────────────────────────────────────┤→ D5 unit ─┐
                                                                                   ├→ D7 gate M8 ─→ D8 gate FINAL
D4 ─→ D6 feature ──────────────────────────────────────────────────────────────────┘
(D5 × D6 paralelizáveis após D2/D4)
```

## M8.5. Riscos e mitigações

| # | Risco | Mitigação (task) |
| :--- | :--- | :--- |
| R1 | `Rh/Estoque/Producao Contracts` aditivos quebram M2/M3/M6 (contratos commitados) | D1: métodos **aditivos** (interface + default impl, sem alterar assinaturas); testes Rh/Estoque/Producao sem regressão em D5–D6 |
| R2 | `GET /api/dashboard/resumo` vs `/summary` divergirem (2 paths, 1 regra) | D4: alias 1 linha, mesmo controller; D6 cobre os 2 paths com o mesmo shape; D8 grep anti-duplicação de regra |
| R3 | `PATCH /api/tasks/{id}` com `{done:false}` concluir mesmo assim (Java dava 400) | D4 documenta o delta (concluir idempotente M6 ignora body); D6 testa `{done:true}`→200 + sem body→200; escalar ao PO se quiser 400 de volta |
| R4 | Task id string numérica colidir com legado `rh-cert-*` no front cacheado | D2: formato novo documentado (`"{id_tarefa}"`, `module producao`); front refetch (polling) expurga o cache; sem parse de prefixo no alias |
| R5 | `ordersActive` (não-ENTREGUE) contestado vs Java `0` | §M8.1 D4 documenta a melhoria (dado real, sem contrato novo); D5 testa a soma; reversível p/ `0` por 1 linha |
| R6 | `id_user` (claim) ≠ `id_responsavel` (tarefa) em dados legados → minhas tarefas vazias | D2: mesma equivalência já usada por M6 `concluirPropria`; sem backfill silencioso (tarefa sem responsável só Admin conclui) |
| R7 | Admin sem `resumoMaquinas`/`emprestimosAbertos` (módulos vazios) quebrar o shape | D2: vazio = `0`/`[]` com chaves presentes p/ Admin (nunca omite p/ Admin, nunca serve p/ não-Admin); D5 cobre |
| R8 | Cobertura global cair abaixo de 80% no gate final | D8: `php artisan test` + relatório global; M8 é agregador fino (service puro mockável) — D5 mira ~100% do módulo |

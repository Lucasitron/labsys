# Tasks — PIVOT Laravel: M0 (shared + scaffold) + M1 (auth)

> Destino: `backend/` Laravel (`App\Modules\Auth`, `App\Shared`). Referência de contrato (não código):
> `auth-service/src/**` + `specs/features/auth-service/requirements.md` + `system/rbac-matrix.md`.
> Convenções: Eloquent sem `Repository`; `FormRequest` valida+autoriza; `Resource` dá o shape;
> regra só em `Service/Action` (ou `Model` se trivial); `Policy`/`Gate` p/ RBAC; eventos
> `<ctx>.<ent>.<acao>.event` via `Event/Listener` (fila `database`); cobertura ≥ 80%.
> Legenda tipo: `reuse` = regra preservada do contrato em roupagem Laravel · `create` = justificado no `plan.md` §3.
> **Nunca `docs/`; não commitar (Builder entrega em working tree).**

## T1 — Scaffold `backend/` Laravel (descarta Java, composer via Docker, fallback manual) — `create`

- **descricao:** Remover o `backend/` Java e criar o esqueleto Laravel 12 (PHP 8.3): `composer.json`,
  estrutura `app/Modules/Auth` + `app/Shared`, `bootstrap/`, `config/`, `routes/`,
  `database/migrations/auth`, `database/seeders`, `tests/`. Sem Eureka/Gateway/banco-por-serviço.
- **escopo:** módulo `—` (scaffold) + peça Laravel `app/bootstrap/composer`.
- **entradas:** `backend/` atual (a descartar: `pom.xml`, `src/**`, `target/`),
  `infra/docker-compose*.yml` (porta 8080, Postgres), `system/architecture.md` (estrutura MVC).
- **saidas:** `backend/` Laravel instalável: `composer.json` (laravel/framework 12, `php-open-source-saver/jwt-auth`,
  pgsql driver, pest/phpunit), `artisan` funcional **dentro de container Docker**
  (`docker run --rm -v ./backend:/app php:8.3-composer ...`), porta 8080, `.env.example`
  (sem segredo real). **Fallback offline:** se `composer create-project` falhar sem rede, escrever à mão
  `composer.json` mínimo + estrutura de pastas + `artisan`/`bootstrap/app.php` mínimos e rodar
  `composer install` quando houver rede (registrar qual via foi usada).
- **criterios_de_conclusao:**
  - [ ] Nenhum vestígio Java (`pom.xml`, `*.java`, `target/`) em `backend/`
  - [ ] `php artisan --version` verde **via Docker** (sem php nativo) e `artisan test` roda (mesmo que 0 testes)
  - [ ] Sem dependência/referência a Eureka/Gateway/banco-por-serviço
- **dependencias:** — (inicial).
- **prioridade:** P0 (bloqueia todas).

## T2 — Config base `shared`: `.env`, database pgsql `fablab`, JWT, exceptions — `create` (+1 `reuse`)

- **descricao:** Configurar app único: conexão pgsql `fablab` (+ `search_path`/schemas por módulo),
  guard `api` JWT (`config/jwt.php`: secret, TTL access 900s, refresh 7 dias, issuer `fablab`,
  HMAC, skew 5s), fila `database`, `App\Shared\Exceptions` (Handler + `ErrorResponse` JSON:
  401 credenciais/token, 403 RBAC, 404 recurso, 400 validação). Fail-fast se `JWT_SECRET < 32 chars`.
- **escopo:** módulo `Shared` + peça Laravel `config/exception`.
- **entradas:** T1; `auth-service/.../config/{SecurityConfig,JwtProperties,AppConfig}` (regras, não código);
  `exception/*` + `GlobalExceptionHandler` + `ErrorResponse` (mapeamento HTTP); requirements NFR-1–4/NFR-8.
- **saidas:** `.env` (fora do git) + `.env.example`, `config/database.php` (pgsql `fablab`),
  `config/jwt.php`, `app/Shared/Exceptions/*`, `routes/console.php` (base p/ T8).
- **criterios_de_conclusao:**
  - [ ] Boot falha com erro claro se `JWT_SECRET` ausente ou `< 32 chars`
  - [ ] 401/403/404/400 em JSON padronizado; nenhum try/catch de mapeamento HTTP nos services/controllers
  - [ ] `csrf`/`sessions` não interferem na API stateless
- **dependencias:** T1.
- **prioridade:** P0.

## T3 — Migrations `database/migrations/auth/` (schemas + V1–V8) — `reuse`

- **descricao:** Reescrever as 8 migrations Flyway como migrations Laravel, qualificando o schema `auth`,
  **sem alterar** tabelas/colunas/constraints/seeds (incl. seeds V6 8 módulos×5 roles e V7 4 chaves).
  Primeira migration: `CREATE SCHEMA IF NOT EXISTS auth` (+ demais schemas do monólito, idempotente).
- **escopo:** módulo `auth` + peça Laravel `Migration`.
- **entradas:** `auth-service/src/main/resources/db/migration/V1–V8.sql`; T2 (conexão `fablab`).
- **saidas:** `database/migrations/auth/*.php`: `auth.login` (UKs `uk_login_uuid/email/nome_usuario` +
  `situacao` default ATIVO + CHECK), `auth.user_permissions` (CHECK 0–4, índice `id_user`, sem
  `responsabilidade`), `auth.token_blacklist` (UK token), `auth.access_log` (índice `uuid_rfid`,
  `timestamp DESC`; `id_user` NULL p/ negado), `auth.permissao_matriz` (PK composta + seed),
  `auth.parametro_sistema` (PK chave + 4 seeds), `auth.token_integracao` (só hash+prefixo).
- **criterios_de_conclusao:**
  - [ ] `php artisan migrate:fresh` em DB limpo `fablab` verde; `migrate:status` sem pendência
  - [ ] Constraints/UKs/seeds conferem 1:1 com os `.sql` V1–V8 (qualquer delta documentado e justificado)
- **dependencias:** T2.
- **prioridade:** P0.

## T4 — Models Eloquent + enums (7 models, `auth.*`, sem Repository) — `reuse`

- **descricao:** Criar os 7 models (`Login`, `UserPermission`, `TokenBlacklist`, `AccessLog`,
  `PermissaoMatriz`, `ParametroSistema`, `TokenIntegracao`) com `$table = 'auth.<tabela>'`,
  `$timestamps = false`, `$fillable` explícito, casts; roles/tipos/situação como enums PHP
  (`Role:int 0–4`, `AccessLogType:string`, `SituacaoUsuario:string`); `Login` implementa
  `JWTSubject` (claims `id_user`, `role`, `setor`). `RoleConverter`/`PermissaoMatrizId`/`repositories`
  **não** são recriados (cast enum + PK composta + Eloquent).
- **escopo:** módulo `auth` + peça Laravel `Model`.
- **entradas:** `auth-service/.../entity/*` (14 arqs: campos/tipos); T3 (tabelas).
- **saidas:** `app/Modules/Auth/Models/*.php` + `Enums/`; zero `Repository/` no módulo.
- **criterios_de_conclusao:**
  - [ ] Models batem com T3 (coluna a coluna; `token_integracao` sem segredo em claro)
  - [ ] Sem `responsabilidade` em `UserPermission`; mass assignment bloqueado (`$fillable` explícito)
- **dependencias:** T3.
- **prioridade:** P0.

## T5 — Policies/middleware/Gates RBAC (`auth:api` + `can:`) — `reuse`

- **descricao:** Proteger as rotas por `rbac-matrix.md`: públicas (`POST login/validate-rfid/refresh`,
  `GET /up` health) vs autenticadas; `permissions/usuarios/permissoes/configuracoes/**` só `ADMIN (0)`;
  `me/senha` qualquer autenticado (com `situacao=ATIVO` checada no service, não aqui).
  `Policies/` do módulo + `Gate` onde houver vínculo (responsável/atribuído — marcar hook p/ M2/RH).
- **escopo:** módulo `auth` (+`Shared/Auth`) + peça Laravel `Policy/Middleware`.
- **entradas:** `@PreAuthorize hasRole('ADMIN')` nos 5 controllers + `SecurityConfig` (rotas públicas);
  `system/rbac-matrix.md` (regras 1–4: só Admin altera RBAC/Financeiro/aprova; Recrutando sem 5S/extrato); T4.
- **saidas:** `app/Modules/Auth/Policies/*` (+ base em `Shared` se transversal), rotas com
  `auth:api` + `can:`/`middleware` aplicados (ver T6), 401 sem token / 403 sem role em JSON.
- **criterios_de_conclusao:**
  - [ ] Públicas acessíveis sem token; `permissions/usuarios/permissoes/configuracoes` devolvem 401 sem token e 403 com role ≠ 0 (coberto por teste em T10)
  - [ ] Nenhum resíduo de gateway/Eureka na fronteira
- **dependencias:** T4.
- **prioridade:** P0.

## T6 — Controllers + FormRequests + Resources + rotas PT — `reuse`

- **descricao:** Criar os 5 controllers **só-HTTP** (validam via `FormRequest`, delegam ao service,
  devolvem `Resource`): `AuthController` (`POST login/logout/validate-rfid/refresh`,
  `GET me`, `GET permissions?role=`, `PUT senha`), `UsuarioController`, `PermissaoController`,
  `ConfiguracaoSistemaController`, `TokenIntegracaoController`; `routes.php` do módulo com paths PT
  exatos (§2 plan). Sem regra de negócio, sem mapper.
- **escopo:** módulo `auth` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** 5 controllers Java (assinaturas/paths/verbos) + 18 DTOs (shapes in/out + validações:
  senha 6–72, email opcional); T4 (models), T5 (proteção), requirements §5.
- **saidas:** `Http/Controllers/*`, `Http/Requests/*` (validação+autorização), `Http/Resources/*`
  (shape de saída incl. `LoginResponse{accessToken,refreshToken,tokenType,expiresIn,idUser,role,setor,nomeUsuario}`),
  `routes.php` montado em `/api/auth`, `/api/usuarios`, `/api/permissoes`, `/api/configuracoes/**`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §2 (verbo+path+query `?role=`); controllers sem cálculo/regra (grep de revisão em T11)
  - [ ] Validação rejeita 400 antes do service; `me()` lê o usuário do guard JWT
- **dependencias:** T4, T5.
- **prioridade:** P0.

## T7 — Services/Actions: login/logout/refresh/RFID/RBAC/usuários/sistema/tokens/purge — `reuse`

- **descricao:** Implementar as regras (só aqui): login por email|nomeUsuario + `Hash::check` (401) +
  permissão ativa + `situacao=ATIVO` (403); logout header/body→blacklist até expiry; refresh com
  **rotação** (invalida o antigo; rejeita `type!=refresh`/blacklisted/expirado); `resolveType`
  ENTRADA↔SAIDA; RFID desconhecido→`ACESSO_NEGADO` (`id_user=null`, `allowed=false`);
  RBAC lê `permissao_matriz` (verdade persistente); CRUD usuários (search/nível/situacao);
  parâmetros KV; tokens integração (claro 1×, persiste hash+prefixo, `ultimo_uso/revogado`).
- **escopo:** módulo `auth` + peça Laravel `Service/Action`.
- **entradas:** `service/{JwtService,AuthService,TokenBlacklistService,AccessLogService,RbacService,UsuarioService,SistemaService,TokenIntegracaoService}` + requirements §4 (fluxos) + NFR-1–4; T4, T2 (JWT).
- **saidas:** `app/Modules/Auth/Services/*.php` (ou `Actions/`, 1 classe por fluxo — escolha do Builder,
  sem duplicar service+action); regra `situacao`/permissão/RBAC centralizada e testável sem HTTP.
- **criterios_de_conclusao:**
  - [ ] Credencial inválida→401; sem permissão ativa ou `situacao!=ATIVO`→403; token revogado falha em `me`
  - [ ] Refresh rotaciona e invalida o antigo; senha 6–72 bcrypt; access 900s / refresh 7 dias
  - [ ] RFID alterna por cartão; desconhecido registra negado e retorna `allowed=false`
- **dependencias:** T4 (T2 p/ JWT).
- **prioridade:** P0.

## T8 — Events/Listeners + Contract in-process + Scheduler purge — `reuse` (+1 `create`)

- **descricao:** Publicar `access.rfid.event` via `Event` nativo (`ShouldQueue`, fila `database`,
  nome+payload `idUser/uuidRfid/timestamp/type` preservados); criar o **`Contract` público do Auth**
  (**create** justificado: validar token / dados+role do usuário p/ outros módulos; ponto de consumo
  de `nivel.alterado.event` do RH que mantém `login`/`user_permissions` — implementação plena em M2);
  purge diário 03:00 (`Schedule`) removendo só expirados de `token_blacklist`.
- **escopo:** módulo `auth` (+`Shared/Events`) + peça Laravel `Event/Listener/Contract/Schedule`.
- **entradas:** `dto/RfidAccessEvent` + `RfidEventPublisher` (payload/nome); `RabbitMqConfig`
  (exchange `fablab.access` — mantida **só** se contrato exigir entrega durável externa, senão fila `database`);
  `TokenBlacklistCleanupTask` (cron); requirements §6 (RH↔Auth); T7.
- **saidas:** `Events/RfidAccessEvent.php`, `Listeners/*`, `Contracts/AuthContract.php` (só o consumido),
  `routes/console.php` (`purge diário 03:00`); nenhum `RabbitTemplate`/broker no caminho crítico M1.
- **criterios_de_conclusao:**
  - [ ] Nome `access.rfid.event` + payload inalterados; listener futuro (RH, M2) recebe via fila `database`
  - [ ] Nenhum módulo precisa importar `Service/Model` do auth — só `Contract`/`Event` (grep em T11)
  - [ ] Purge remove só expirados
- **dependencias:** T7.
- **prioridade:** P1.

## T9 — Seeder admin dev-only idempotente — `reuse`

- **descricao:** `AuthSeeder` cria `admin@fablab.org` / `admin` / bcrypt(`admin123`) + `user_permissions`
  `ADMIN` ativa (+ seeds V6/V7 se não estiverem nas migrations) **só** com `AUTH_SEED_ENABLED=true`
  e `login` vazia; nunca em prod, nunca loga credencial, nunca duplica.
- **escopo:** módulo `auth` + peça Laravel `Seeder`.
- **entradas:** `config/AuthSeedRunner` + `SeedProperties` (valores/condições); T3–T4.
- **saidas:** `database/seeders/AuthSeeder.php` (+ flag documentada no `.env.example`).
- **criterios_de_conclusao:**
  - [ ] Roda 2× sem duplicar; não executa sem a flag; login do seed passa em `POST /api/auth/login`
- **dependencias:** T7 (services usados pelo seed, se houver).
- **prioridade:** P1.

## T10 — Testes Unit/Feature (401/403, fluxos, 80%) — `reuse` (+1 `create`)

- **descricao:** Cobrir M1 em PHPUnit/Pest via `php artisan test` **em container Docker** (com `pcov`):
  unit (jwt/claims, auth, rbac, access, blacklist, usuario/sistema/token) + feature (login 200/401/403,
  logout→blacklist, refresh-rotação, RFID alternado/negado, `me`, matriz, usuários, config, tokens,
  evento `access.rfid.event` publicado) + **slice security 401/403 por rota sensível** (**create**).
- **escopo:** módulo `auth` (+`Shared`) + peça Laravel `Test`.
- **entradas:** suíte Java (casos, não código) + T6–T8; DoD (`monolito-checklist.md`, a atualizar pelo Orquestrador p/ Laravel).
- **saidas:** `tests/Unit/Auth/*`, `tests/Feature/Auth/*` verdes; relatório de cobertura ≥ 80%.
- **criterios_de_conclusao:**
  - [ ] `php artisan test` verde (container); cobertura ≥ 80%
  - [ ] 401 sem token e 403 sem role em `permissions/usuarios/permissoes/configuracoes`; `validate-rfid` pública
- **dependencias:** T6, T7, T8, T9.
- **prioridade:** P0.

## T11 — Gate DoD M0+M1 (verificação final, sem código) — `create` (verificação)

- **descricao:** Conferir o DoD M0+M1 (checklist a ser atualizado p/ Laravel pelo Orquestrador):
  estrutura MVC, schema próprio, fronteiras, rotas, `php artisan test` verde + 80% + 401/403,
  código mínimo (cada `create` justificado no plan §3). Sem código — relatório pass/fail.
- **escopo:** módulo `Shared`+`auth` + peça `verificação`.
- **entradas:** T1–T10 + `.opencode/state/monolito-checklist.md` (DoD).
- **saidas:** relatório de gate (pass/fail por item) + lista de remoções BAIXA/MÉDIA se houver excesso.
- **criterios_de_conclusao:**
  - [ ] Código em `app/{Shared,Modules/Auth}/{Models,Http,Services|Actions,Policies,Events,Listeners}` (+`Contracts/` só consumido); `config/exceptions` só em `Shared`
  - [ ] Models `auth.*`; regra só em `Service/Action`; migrations `database/migrations/auth/`; `migrate:fresh` verde em DB limpo
  - [ ] Zero Eureka/Gateway/HTTP entre módulos (`grep` limpo); evento via nativo com nome preservado
  - [ ] Rotas `/api/auth/**`, `/api/usuarios`, `/api/permissoes`, `/api/configuracoes/**` exatas
  - [ ] `reuse > create`; `docs/` intocado; nada commitado (working tree)
- **dependencias:** T10.
- **prioridade:** P0.

## Grafo de dependências (resumo)

```
T1 ─→ T2 ─→ T3 ─→ T4 ─┬→ T5 ─→ T6 ─┐
                       ├→ T7 ─→ T8 ─┤→ T9 ─→ T10 ─→ T11
                       └→ T7 ──────┘   (T6 × T7 paralelizáveis após T4/T5)
```

---

# Tasks M2 — módulo `rh` (prefixo R)

> Destino: `backend/` Laravel (`App\Modules\Rh`, schema `rh`). Referência de regra (não código):
> `rh-service/src/**` + `specs/features/rh-service/requirements.md` (F1–F9) + `system/rbac-matrix.md`.
> Base pronta: `AuthContract` (M1, `verify`/`isAdmin`), evento `access.rfid.event` (Listener pendente
> do RH). Convenções: Eloquent sem `Repository`; `FormRequest` valida+autoriza; `Resource` dá o shape;
> regra só em `Service` (ou `Model` se trivial); `Policy`/`can:` p/ RBAC + próprio-registro;
> eventos `<ctx>.<ent>.<acao>.event` via `Event/Listener` (`ShouldQueue`, fila `database`); cobertura ≥ 80%.
> Legenda tipo: `reuse` = regra preservada em roupagem Laravel · `create` = justificado no `plan.md` §M2.3.
> **Nunca `docs/`; não commitar (Builder entrega em working tree).**

## R1 — Migrations `database/migrations/rh/` (schemas + V1–V12+V14) — `reuse`

- **descricao:** Reescrever as migrations Flyway como migrations Laravel qualificando o schema `rh`,
  **sem alterar** tabelas/colunas/constraints/índices/UKs: V1 `pessoa` (UK matrícula), V2 `funcionario`
  (CHECK 0–4, UK pessoa), V3 `tutor` (qualificação 0–6, UK funcionário), V4 `registro_ponto_diario`
  (UK funcionário+data), V5 `apontamento_horas` (+V10 `consolidado` default false +V14
  `hora_inicio`/`hora_fim`/`motivo_rejeicao`), V6 `processo_seletivo` (+V12 `id_grupo`/`nota`/
  `feedback`), V7 `treinamento`, V8 `avaliacao_treinamento` (nota 0–10), V9 `historico_nivel`,
  V10 `solicitacao_certificado`/`certificado_emitido` (UKs solicitação+código UUID)/
  `hora_consolidada`, V11 `cpf` nullable+UK em `pessoa`, V12 `grupo_processo_seletivo`. Gap V13
  **não existe upstream — não inventar migration**.
- **escopo:** módulo `rh` + peça Laravel `Migration`.
- **entradas:** `rh-service/src/main/resources/db/migration/V*.sql`; M1 (conexão `fablab`, `CREATE SCHEMA`).
- **saidas:** `database/migrations/rh/*.php` com `Schema::create('rh.<tabela>')`.
- **criterios_de_conclusao:**
  - [ ] `php artisan migrate:fresh` em DB limpo verde; `migrate:status` sem pendência (com M1 aplicado)
  - [ ] Constraints/UKs/índices conferem 1:1 com os `.sql` (qualquer delta documentado e justificado)
- **dependencias:** M0+M1 (scaffold, conexão `fablab`).
- **prioridade:** P0 (bloqueia R2–R12).

## R2 — Models Eloquent + enums + `CpfRule` (13 models, `rh.*`, sem Repository) — `reuse` (+1 `create`)

- **descricao:** Criar os 13 models (`Pessoa`, `Funcionario`, `Tutor`, `RegistroPontoDiario`,
  `ApontamentoHoras`, `ProcessoSeletivo`, `GrupoProcessoSeletivo`, `Treinamento`,
  `AvaliacaoTreinamento`, `HistoricoNivel`, `SolicitacaoCertificado`, `CertificadoEmitido`,
  `HoraConsolidada`) com `$table='rh.<tabela>'`, `$timestamps=false`, `$fillable` explícito, casts;
  enums PHP 8.1 (`NivelAcesso:int 0–4`, `PessoaStatus`, `StatusApontamento`, `StatusProcesso`,
  `StatusSolicitacao`, `TipoApontamento`, `TipoCertificado`); `CpfUtil` vira `Rules/CpfRule`
  (**create** justificado: validação de dígitos sem equivalente nativo); converters/
  `repositories`/`RhPrincipal` **não** recriados.
- **escopo:** módulo `rh` + peça Laravel `Model/Enum/Rule`.
- **entradas:** `rh-service/.../entity/*` + `dto/CpfUtil.java`; R1 (tabelas).
- **saidas:** `app/Modules/Rh/Models/*.php` + `Enums/` + `Rules/CpfRule.php`; zero `Repository/` no módulo.
- **criterios_de_conclusao:**
  - [ ] Models batem com R1 coluna a coluna (`cpf` nullable; `consolidado` cast bool; UUID `codigo_verificacao`)
  - [ ] Mass assignment bloqueado (`$fillable` explícito); CPF inválido/duplicado rejeitado na validação
- **dependencias:** R1.
- **prioridade:** P0.

## R3 — Policies/middleware/Gates RBAC (`auth:api` + `can:`, próprio-registro) — `reuse`

- **descricao:** Proteger as rotas pela matriz requirements §RBAC ∩ `rbac-matrix.md`: tudo autenticado
  (`auth:api`+`blacklist`, health `/up` é do bootstrap); Admin-only: `PUT funcionarios/{id}/nivel`,
  `niveis/**`, validar/rejeitar apontamentos, aprovar/rejeitar certificados; tutor-valida
  (`apontamentos-horas/{id}/validar` p/ tutor responsável — hook `usuario_responsabilidade`);
  próprio-registro: `solicitacoes/emitidos/horas/disponiveis/extrato` filtram pelo usuário
  (server-side, nunca no front); Recrutando (4) sem ponto/horas/certificado/extrato/5S.
  `AutorizacaoHelper` vira `Policy`; `RhPrincipal` some (guard JWT do Auth).
- **escopo:** módulo `rh` (+`Shared` se transversal) + peça Laravel `Policy/Middleware`.
- **entradas:** `@PreAuthorize` dos 8 controllers + `system/rbac-matrix.md` (regras 1–4); R2.
- **saidas:** `app/Modules/Rh/Policies/*` + rotas com `auth:api`+`blacklist`+`can:` aplicados (ver R4–R5).
- **criterios_de_conclusao:**
  - [ ] Admin-only devolve 401 sem token e 403 com role ≠ 0 (coberto em R12)
  - [ ] Não-Admin não enxerga dados de terceiros em escopos "próprio" (teste em R12)
- **dependencias:** R2.
- **prioridade:** P0.

## R4 — Controllers pessoas/funcionários/níveis + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP** (validam via `FormRequest`, delegam ao service, devolvem
  `Resource`): `PessoaController` (`POST/GET /api/rh/pessoas`, `GET /{id}`, `GET /{id}/detalhe`,
  `PUT /{id}`, `DELETE /{id}` — Admin), `FuncionarioController` (`GET/POST /api/rh/funcionarios` —
  Admin, `PUT /{id}/nivel` — Admin, `GET /{id}/horas`), `NivelController` (`GET /api/rh/niveis`,
  `PATCH /{id}/membros`, `POST /convites` — todos Admin). Saída `pessoa` com **CPF mascarado**
  (LGPD); listagem com busca/filtros/paginação (`PessoasPaginaResponse`→`Resource` paginado) +
  facetas (`FacetaResponse`/`FiltrosPessoasResponse`); `searchPeople` = Eloquent LIKE (fila backend,
  sem fila dedicada). Sem regra, sem mapper.
- **escopo:** módulo `rh` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `PessoaController`, `FuncionarioController`, `NivelController` (paths/verbos) + DTOs
  de entrada/saída; R2 (models), R3 (proteção).
- **saidas:** `Http/Controllers/{Pessoa,Funcionario,Nivel}Controller.php`, `Http/Requests/*`,
  `Http/Resources/*`, trechos de `routes.php` (`/api/rh/pessoas`, `/funcionarios`, `/niveis`).
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas (verbo+path); controllers sem cálculo/regra (grep em R13)
  - [ ] CPF nunca sai em claro; validação rejeita 400 antes do service
- **dependencias:** R2, R3.
- **prioridade:** P0.

## R5 — Controllers horas/apontamentos (fachada) + PS + treinamentos + certificados + extrato — `reuse`

- **descricao:** Controllers **só-HTTP**: `ApontamentoHorasController` (`POST/GET
  /api/rh/apontamentos-horas`, `PUT /{id}/validar` — Admin), `HorasController` fachada
  (`GET /api/rh/horas/disponiveis`, `GET /api/rh/horas`, `POST /api/rh/horas`,
  `PATCH /{id}/validar|rejeitar` — Admin; delega ao mesmo `ApontamentoService`, **sem classe
  fachada extra**), `ProcessoSeletivoController` (`POST/GET /api/rh/processo-seletivo`,
  `PUT /{id}`, `POST /grupos`, `PATCH /{id}/estagio`, `POST /{id}/membros/{pessoaId}/avaliar`),
  `TreinamentoController` (`POST /api/rh/treinamentos`, `POST|GET /{id}/avaliacoes`),
  `CertificadoController` (`POST /api/rh/certificados/solicitar`, `GET /solicitacoes`,
  `PUT /solicitacoes/{id}/aprovar|rejeitar` — Admin, `GET /emitidos`, `GET /emitidos/{id}`),
  `GET /api/rh/extrato-mensal-horas` (leitura do extrato; geração é R10). Sem regra, sem mapper.
- **escopo:** módulo `rh` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** 5 controllers Java restantes (paths/verbos/`@PreAuthorize`) + DTOs; R2, R3.
- **saidas:** `Http/Controllers/{ApontamentoHoras,Horas,ProcessoSeletivo,Treinamento,Certificado,Extrato}Controller.php`
  (ou ações embutidas — sem duplicar controller+action), `Requests/*`, `Resources/*`, `routes.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M2.2; `horas/*` e `apontamentos-horas/*` convergem p/ o mesmo service
  - [ ] Validação (nota 0–10, motivo de rejeição, UUID) rejeita 400 antes do service
- **dependencias:** R2, R3.
- **prioridade:** P0.

## R6 — Services pessoa/funcionário/nível/ponto (CPF/LGPD, convites, search, RFID-in) — `reuse`

- **descricao:** Regras (só aqui): pessoa CRUD + matrícula única + CPF opcional/único/dígitos
  (`CpfRule`) + detalhe/facetas; funcionário vincular/listar (filtros nível/departamento) +
  `totalHoras` com **coerência** presença×(encomenda+projeto) por dia (lista incoerências p/
  Financeiro via `RhContract`); nível: matriz (Admin), `alterarMembro` + `historico_nivel` +
  evento (via R9), `convidar` (pessoa+funcionário nível 4/Recrutando); ponto:
  `processarEventoRfid` (entrada/saída/`total_horas`, idempotente por funcionário+data).
- **escopo:** módulo `rh` + peça Laravel `Service`.
- **entradas:** `service/{Pessoa,Funcionario,Nivel,Ponto}Service` + requirements F1–F3/F7; R2; `AuthContract`.
- **saidas:** `app/Modules/Rh/Services/{Pessoa,Funcionario,Nivel,Ponto}Service.php`, regra testável sem HTTP.
- **criterios_de_conclusao:**
  - [ ] CPF duplicado/inválido rejeitado; saída sempre mascarada (teste LGPD em R12)
  - [ ] Nível só por Admin (Policy R3) + `historico_nivel` gravado; ponto idempotente no reenvio do evento
- **dependencias:** R2 (R9 p/ emissão dos eventos de nível).
- **prioridade:** P0.

## R7 — Services apontamento/horas/extrato-leitura (horário, motivo, coerência) — `reuse`

- **descricao:** Regras (só aqui): apontamento nasce PENDENTE (próprio-registro p/ 1/2, Admin tudo);
  `hora_inicio`/`hora_fim` (V14) recalculam `horas_trabalhadas`; validar (Admin ou tutor
  responsável) publica `horas.validadas.event` (via R9); rejeitar exige `motivo_rejeicao`;
  `horasDisponiveis` = VALIDADOS ∧ ¬consolidado; leitura do extrato mensal por funcionário/mês
  (geração é R10); Fachada `HorasController` atendida por este service (sem duplicar).
- **escopo:** módulo `rh` + peça Laravel `Service`.
- **entradas:** `service/ApontamentoHorasService` + `HorasController` (fachada) + F4; R2.
- **saidas:** `app/Modules/Rh/Services/ApontamentoService.php` (+ `ExtratoService::leitura` ou método dedicado).
- **criterios_de_conclusao:**
  - [ ] Apontamento nasce PENDENTE; rejeição sem motivo → 400/422; validados disparam evento
  - [ ] Horas consolidadas nunca voltam a disponíveis
- **dependencias:** R2 (R9 p/ emissão).
- **prioridade:** P0.

## R8 — Services PS/treinamento/certificado (grupos/estágio/avaliação, consolidação total) — `reuse`

- **descricao:** Regras (só aqui): PS iniciar/atualizar estágio (INSCRITO→…→APROVADO/REPROVADO),
  grupos (`criarGrupo`, `moverEstagio`, `avaliar` nota/feedback por membro), candidato aprovado
  vira pessoa+funcionário; treinamento CRUD + avaliações (nota 0–10, `id_tutor` = vínculo `tutor` —
  Tutor responde por grupo/PS, instrutor ministra); certificado: solicitar (tipo EXTENSAO/
  COMPLEMENTAR/ESTAGIO, horas = disponíveis, PENDENTE) → aprovar (Admin: emite com UUID
  `codigo_verificacao`, consolida **TODAS** as disponíveis — sem parcial no MVP — grava
  `hora_consolidada`, publica aprovado) / rejeitar (Admin: mantém disponíveis, publica rejeitado).
- **escopo:** módulo `rh` + peça Laravel `Service`.
- **entradas:** `service/{ProcessoSeletivo,Treinamento,Certificado}Service` + F5/F6/F8; R2.
- **saidas:** `app/Modules/Rh/Services/{ProcessoSeletivo,Treinamento,Certificado}Service.php`.
- **criterios_de_conclusao:**
  - [ ] Candidato criado com nível 4 + status Recrutando; aprovação gera UUID único e consolida tudo
  - [ ] Rejeição preserva disponíveis; `solicitacoes`/`emitidos` filtram próprio p/ não-Admin
- **dependencias:** R2 (R9 p/ emissão).
- **prioridade:** P0.

## R9 — Events/Listeners + `RhContract` (consome RFID, emite 6 eventos) — `reuse` (+1 `create`)

- **descricao:** Consumidor: `Listener` de `access.rfid.event` (Auth/M1, fila `database`) → chama
  `PontoService::processarEventoRfid` (idempotente, sem broker). Emissão via `Event`s nativos
  (`ShouldQueue`, fila `database`, **nomes+payloads preservados**): `nivel.alterado.event` (→Auth,
  Notification), `horas.validadas.event` (→Financeiro), `certificado.solicitado.event` /
  `certificado.aprovado.event` / `certificado.rejeitado.event` + `extrato.mensal.horas.event`
  (→Notification). Criar **`Contracts/RhContract.php`** (**create** justificado: coerência/horas e
  funcionários/responsáveis consumidos por Financeiro/Produção in-process; **só o consumido**).
  `RabbitMqConfig`/`RhEventPublisher` com `RabbitTemplate` **somem** (fila `database` cobre M2).
- **escopo:** módulo `rh` (+`Shared/Events` se transversal) + peça Laravel `Event/Listener/Contract`.
- **entradas:** `dto/{RfidAccessEvent,NivelAlterado,HorasValidadas,Certificado*,ExtratoMensalHoras}Event`
  + `RhEventPublisher` (nomes/payloads) + `RfidEventListener`; R6–R8.
- **saidas:** `Events/*.php`, `Listeners/ProcessarPontoRfid.php`, `Contracts/RhContract.php`
  (+ implementação lendo Eloquent do próprio schema).
- **criterios_de_conclusao:**
  - [ ] 6 nomes de evento + payloads inalterados; Listener recebe via fila `database` (teste em R12)
  - [ ] Nenhum módulo importa `Service/Model` do RH — só `Contract`/`Event` (grep em R13)
- **dependencias:** R6, R7, R8.
- **prioridade:** P1.

## R10 — Scheduler extrato mensal (`0 6 1 * *`) + config — `reuse`

- **descricao:** `ExtratoService::gerarExtratoMensal(?YearMonth)`: mês anterior fechado, 1 evento
  `extrato.mensal.horas.event` por funcionário ativo **exceto nível 4** (Recrutando não recebe);
  agendar em `routes/console.php` com `Schedule::cron('0 6 1 * *')` (conversão documentada do
  Spring `0 0 6 1 * *`) + config `rh.extrato_scheduler_enabled` (default on, equivale a
  `rh.certificado.extrato.scheduler.enabled`).
- **escopo:** módulo `rh` + peça Laravel `Schedule/config`.
- **entradas:** `service/{ExtratoService,ExtratoMensalScheduler}` + requirements F9; R7 (leitura), R9 (evento).
- **saidas:** `Services/ExtratoService::gerarExtratoMensal()`, entrada em `routes/console.php`, `config/rh.php`.
- **criterios_de_conclusao:**
  - [ ] Chamada manual com mês fixo gera 1 evento por ativo não-Recrutando e zero p/ nível 4
  - [ ] Agendamento registrado no cron `0 6 1 * *`; flag desliga sem erro
- **dependencias:** R7, R9.
- **prioridade:** P1.

## R11 — Seeders (verificação — sem seed no contrato) — `verificação`

- **descricao:** Conferir que V1–V12+V14 **não trazem seeds** (ao contrário do Auth V6/V7): nenhuma
  `RhSeeder` inventada. Se o Builder achar dado mínimo exigido por constraint/Policy (ex. níveis),
  propor seed dev-only idempotente com flag (mesmo padrão `AUTH_SEED_ENABLED` do M1) e escalar ao
  Orquestrador — nunca popular em silêncio.
- **escopo:** módulo `rh` + peça Laravel `Seeder` (se houver).
- **entradas:** migrations R1; padrão `AuthSeeder` (M1).
- **saidas:** relatório "sem seed necessário" OU `database/seeders/RhSeeder.php` dev-only justificado.
- **criterios_de_conclusao:**
  - [ ] `migrate:fresh` + testes passam sem seed; nenhum dado inventado versionado sem justificativa
- **dependencias:** R1, R2.
- **prioridade:** P2.

## R12 — Testes Unit/Feature (401/403, fluxos, LGPD/CPF, 80%) — `reuse` (+1 `create`)

- **descricao:** Cobrir M2 em PHPUnit/Pest via `php artisan test` **em container Docker** (com `pcov`):
  unit (CPF rule/máscara, Ponto alternância/idempotência, Apontamento status/motivo/horário,
  Certificado disponíveis/consolidação/UUID, Nível histórico, Extrato mês-anterior/exceto-4,
  PS estágios/grupos/avaliação, Treinamento nota) + feature (CRUD pessoa/funcionário, convites,
  apontar→validar→evento, solicitar→aprovar→consolidação→eventos, extrato manual, Listener RFID
  via fila `database`) + **slice security 401/403 por rota sensível + LGPD/CPF** (**create**).
- **escopo:** módulo `rh` (+`Shared`) + peça Laravel `Test`.
- **entradas:** suíte Java (casos, não código) + R4–R10; DoD (`monolito-checklist.md`).
- **saidas:** `tests/Unit/Rh/*`, `tests/Feature/Rh/*` verdes; cobertura ≥ 80% (módulo).
- **criterios_de_conclusao:**
  - [ ] `php artisan test` verde (container); cobertura ≥ 80%
  - [ ] 401 sem token e 403 sem papel em cada rota Admin-only; CPF nunca em claro; próprio-registro isolado
- **dependencias:** R4, R5, R6, R7, R8, R9, R10, R11.
- **prioridade:** P0.

## R13 — Gate DoD M2 (verificação final, sem código) — `create` (verificação)

- **descricao:** Conferir o DoD M2 (estende M0+M1, sem sobrescrever): estrutura MVC do `Rh`, schema
  próprio, fronteiras (`Contract`/`Event` só), rotas `/api/rh/**` exatas, `php artisan test` verde +
  80% + 401/403 + LGPD, código mínimo (cada `create` justificado no plan §M2.3). Sem código —
  relatório pass/fail.
- **escopo:** módulo `rh` + peça `verificação`.
- **entradas:** R1–R12 + `.opencode/state/monolito-checklist.md` (DoD).
- **saidas:** relatório de gate (pass/fail por item) + remoções BAIXA/MÉDIA se houver excesso.
- **criterios_de_conclusao:**
  - [ ] Código em `app/Modules/Rh/{Models,Http,Services,Policies,Events,Listeners}` (+`Contracts/` só
    consumido, +`Enums/`+`Rules/`); `config/exceptions` só em `Shared`
  - [ ] Models `rh.*`; regra só em `Service`; migrations `database/migrations/rh/`; `migrate:fresh` verde
  - [ ] Zero Eureka/Gateway/HTTP entre módulos (`grep` limpo); eventos nativos com nomes preservados
  - [ ] Rotas `/api/rh/**` exatas (§M2.2); `reuse > create`; `docs/` intocado; nada commitado
- **dependencias:** R12.
- **prioridade:** P0.

## Grafo de dependências M2 (resumo)

```
R1 ─→ R2 ─┬→ R3 ─→ R4 ─┐
           ├→ R3 ─→ R5 ─┤
           ├→ R6 ──────┤→ R9 ─→ R10 ─┐
           ├→ R7 ──────┤→ R9 ────────┤→ R12 ─→ R13
           └→ R8 ──────┘→ R9 ────────┘
R1 ─→ R11 ─→ R12   (R4 × R5 × R6 × R7 × R8 paralelizáveis após R2/R3)
```

---

# Tasks M3 — módulo `estoque` (prefixo E)

> Destino: `backend/` Laravel (`App\Modules\Estoque`, schema `estoque`). Referência de regra (não código):
> `estoque-service/src/**` + `specs/features/estoque-service/requirements.md` (F1–F9) + `system/rbac-matrix.md`
> (prevalece onde diverge do Java — ver plan §M3.1.4). Base pronta: `AuthContract` (M1),
> `RhContract` (M2, +`nomePessoa` aditivo em E9). Convenções: Eloquent sem `Repository`;
> `FormRequest` valida+autoriza; `Resource` dá o shape (incl. `tomador` E-4); regra só em `Service`
> (ou `Model` se trivial); `Policy`/`can:` p/ RBAC + responsável-por-estoque + `canSeeLoans` E-7;
> eventos `<ctx>.<ent>.<acao>.event` via `Event/Listener` (`ShouldQueue`, fila `database`);
> concorrência via `lockForUpdate()` em `DB::transaction` (sem-negativo); cobertura ≥ 80%.
> Legenda tipo: `reuse` = regra preservada em roupagem Laravel · `create` = justificado no `plan.md` §M3.3.
> **Nunca `docs/`; não commitar (Builder entrega em working tree).**

## E1 — Migrations `database/migrations/estoque/` (schemas + V1–V9) — `reuse`

- **descricao:** Reescrever as migrations Flyway como migrations Laravel qualificando o schema `estoque`,
  **sem alterar** tabelas/colunas/constraints/índices/UKs: V1 `localizacao`, V2 `item` (CHECK categoria,
  `versao` default 0, FK localização, índice), V3 `fornecedor`, V4 `entrada_estoque` (FKs item/fornecedor,
  índices), V5 `saida_estoque` (CHECK tipo CONSUMO/PERDA/AJUSTE/EMPRESTIMO, índice), V6 `emprestimo`
  (CHECK status ATIVO/DEVOLVIDO/ATRASADO, índices), V7 `lista_materiais`, V8 `item_bom` (UK bom+item,
  FKs, índice), V9 `responsavel` nullable em entrada/saída/empréstimo. Primeira migration garante
  `CREATE SCHEMA IF NOT EXISTS estoque` (idempotente, padrão M1).
- **escopo:** módulo `estoque` + peça Laravel `Migration`.
- **entradas:** `estoque-service/src/main/resources/db/migration/V1–V9.sql`; M0+M1 (conexão `fablab`, `CREATE SCHEMA`).
- **saidas:** `database/migrations/estoque/*.php` com `Schema::create('estoque.<tabela>')`.
- **criterios_de_conclusao:**
  - [ ] `php artisan migrate:fresh` em DB limpo verde; `migrate:status` sem pendência (com M1+M2 aplicados)
  - [ ] Constraints/CHECKs/UKs/FKs/índices conferem 1:1 com os `.sql` V1–V9 (qualquer delta documentado e justificado)
- **dependencias:** M0+M1+M2 (scaffold, conexão `fablab`, migrator com glob).
- **prioridade:** P0 (bloqueia E2–E13).

## E2 — Models Eloquent + enums (8 models, `estoque.*`, sem Repository) — `reuse`

- **descricao:** Criar os 8 models (`Localizacao`, `Item`, `Fornecedor`, `EntradaEstoque`, `SaidaEstoque`,
  `Emprestimo`, `ListaMateriais`, `ItemBom`) com `$table='estoque.<tabela>'`, `$timestamps=false`,
  `$fillable` explícito, casts (`quantidade/valor` decimal, `data_*` date/datetime, `editavel` bool);
  enums PHP 8.1 `string backed` (`Categoria`: INSUMO/FERRAMENTA/PECA, `StatusEmprestimo`:
  ATIVO/DEVOLVIDO/ATRASADO, `TipoSaida`: CONSUMO/PERDA/AJUSTE/EMPRESTIMO); `NivelAcesso` **não**
  recriado (reuse `Auth\Enums\Role`); `versao` coluna simples (sem trait optimistic);
  `repositories` **não** recriados.
- **escopo:** módulo `estoque` + peça Laravel `Model/Enum`.
- **entradas:** `estoque-service/.../entity/*`; E1 (tabelas).
- **saidas:** `app/Modules/Estoque/Models/*.php` + `Enums/`; zero `Repository/` no módulo.
- **criterios_de_conclusao:**
  - [ ] Models batem com E1 coluna a coluna (`responsavel` nullable; `versao` default 0; UK item_bom)
  - [ ] Mass assignment bloqueado (`$fillable` explícito); nenhum `Repository/`/`Mapper/` no módulo
- **dependencias:** E1.
- **prioridade:** P0.

## E3 — Policies/Gates RBAC (responsável-por-estoque + `canSeeLoans` E-7) — `reuse`

- **descricao:** `Policies/EstoquePolicy.php` + Gates: escrita (itens/entradas/saídas/fornecedores/
  localizações/boms/consumo) = Admin **ou** Bolsista/Voluntário **com vínculo**
  `usuario_responsabilidade` (contexto/setor de estoque) — delta matriz×Java documentado no plan
  §M3.1.4; leitura = qualquer autenticado **exceto Recrutando (4)** (matriz: `–` no módulo → 403);
  empréstimos: criar/devolver = Admin ou próprio (`id_pessoa` = claim `id_user`); leitura com escopo
  `canSeeLoans` E-7 (Admin tudo, demais só próprios — filtro **server-side no service**, nunca no
  front). JWT validado pelo guard `api` do Auth (M1) + `blacklist`.
- **escopo:** módulo `estoque` (+`Shared` se transversal) + peça Laravel `Policy/Gate`.
- **entradas:** `EDICAO/VISUALIZACAO/OPERACAO` dos 7 controllers + `system/rbac-matrix.md` (regras 1–5:
  responsável/atribuído exige vínculo; Recrutando sem estoque); E2; `AuthContract::isAdmin/roleOf`.
- **saidas:** `app/Modules/Estoque/Policies/EstoquePolicy.php` (+ Gates `editar-estoque`, `can-see-loans`
  ou `can:` equivalentes) aplicados nas rotas E4/E5.
- **criterios_de_conclusao:**
  - [ ] Escrita devolve 401 sem token e 403 (Voluntário sem vínculo, Estagiário, Recrutando) — coberto em E12
  - [ ] Não-Admin nunca enxerga empréstimo de terceiros (teste E-7 em E12)
- **dependencias:** E2.
- **prioridade:** P0.

## E4 — Controllers núcleo (itens c/ CSV, entradas, saídas) + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `ItemController` (`POST/GET /api/estoque/itens` com filtros
  `?categoria=&idLocalizacao=&baixo=`, `GET /{id}`, `PUT /{id}`, `GET /export` download `itens.csv`,
  `POST /import` upload multipart), `EntradaController` (`POST/GET /api/estoque/entradas` com
  **só** `?idItem=`, `GET /{id}`), `SaidaController` (`POST/GET /api/estoque/saidas` com **só**
  `?idItem=`, `GET /{id}`). **Sem `?status=` em entradas/saídas** (E-1 minimal — grep em E13).
  Item responde com localização física completa (armário/prateleira/caixa). Sem regra, sem mapper.
- **escopo:** módulo `estoque` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `Item/Entrada/SaidaController` Java (paths/verbos/`@PreAuthorize`) + DTOs (shapes +
  Bean Validation: quantidades/valores positivos, datas, enums); E2 (models), E3 (proteção).
- **saidas:** `Http/Controllers/{Item,Entrada,Saida}Controller.php`, `Http/Requests/*`,
  `Http/Resources/*`, trechos de `routes.php` (`/api/estoque/itens|entradas|saidas`).
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas (verbo+path+query `?categoria=&idLocalizacao=&baixo=` só em itens; `?idItem=` só em entradas/saídas; zero `?status=` nessas rotas)
  - [ ] Controllers sem cálculo/regra (grep em E13); validação rejeita 400/422 antes do service
- **dependencias:** E2, E3.
- **prioridade:** P0.

## E5 — Controllers empréstimos/BOM/fornecedores/localizações + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `EmprestimoController` (`POST /api/estoque/emprestimos`,
  `GET` com `?status=ativos|atrasados|historico` (+nulo = todos), `GET /atrasados`, `GET /{id}`,
  `PUT /{id}/devolucao`), `BomController` (`POST/GET /api/estoque/boms` com `?projetoId=`,
  `GET /{id}`, `PUT /{id}`, `POST /{id}/consumo`), `FornecedorController` (`POST/GET
  /api/estoque/fornecedores`), `LocalizacaoController` (`GET/POST /api/estoque/localizacoes`).
  Resposta de empréstimo inclui `tomador` (rótulo E-4, via service). Sem regra, sem mapper.
- **escopo:** módulo `estoque` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** 4 controllers Java restantes (paths/verbos/`OPERACAO`/`EDICAO`) + DTOs (devolução
  prevista futura, `responsavel` ≤255, versão, quantidades); E2, E3.
- **saidas:** `Http/Controllers/{Emprestimo,Bom,Fornecedor,Localizacao}Controller.php`,
  `Requests/*`, `Resources/*`, `routes.php` do módulo + registro em `routes/api.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M3.2 (`?status=` só em empréstimos; `?projetoId=` só em BOMs)
  - [ ] `status` inválido → 400/422; validação rejeita 400/422 antes do service
- **dependencias:** E2, E3.
- **prioridade:** P0.

## E6 — Services movimentações (item+CSV, entrada, saída, sem-negativo) — `reuse` (+1 `create`)

- **descricao:** Regras (só aqui, tudo em `DB::transaction` + `lockForUpdate()` no item):
  `ItemService`: CRUD + filtros + `resolverLocalizacao` + `verificarEstoqueBaixo` (dispara via E9)
  + `registrarConsumo` (lock, sem-negativo, saída CONSUMO) + `baixarPorConsumo` (idempotente por
  `(CONSUMO,id_referencia)`) + `exportarCsv` (formato `;` PT, **sanitização anti-injection**
  — **create** justificado: prefixo `'` em células iniciadas por `=,+,-,@`/tab) + `importarCsv`
  (linha `nome;descricao;categoria;unidade;atual;minimo[;idLocalizacao]`, reaproveita `criar`);
  `EntradaService::registrar` (`valor_total = qtd × unit`, soma saldo, publica compra via E9);
  `SaidaService::registrar` (`novoSaldo < 0 → SaldoInsuficienteException` → 409/422). Fornecedor/
  localização: CRUD trivial **no `Model`/Eloquent sem service forçado** (sem duplicar).
- **escopo:** módulo `estoque` + peça Laravel `Service`.
- **entradas:** `service/{Item,Entrada,Saida,Fornecedor,Localizacao}Service` + requirements F1–F5/F8–F9; E2.
- **saidas:** `app/Modules/Estoque/Services/{Item,Entrada,Saida}Service.php`, regra testável sem HTTP.
- **criterios_de_conclusao:**
  - [ ] Saída/consumo que negativaria → 409/422 e saldo intacto (teste concorrente em E12)
  - [ ] Export nunca emite célula executável (`=cmd|…` sai `'=cmd|…`); import reaproveita validações do criar
- **dependencias:** E2 (E9 p/ emissão dos eventos).
- **prioridade:** P0.

## E7 — Service empréstimos (E-7, devolução, atraso, tomador E-4) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction` + `lockForUpdate()`): `criar` (não-admin só p/ si
  — `id_pessoa` = claim `id_user`, senão 403; prevista ≥ hoje; lock, sem-negativo, status ATIVO,
  + saída `EMPRESTIMO` com `id_referencia` = id do empréstimo); `devolver` (Admin ou responsável,
  senão 403; já-devolvido → 400/422; soma saldo com lock; `data_devolucao_real` = hoje, DEVOLVIDO);
  `listar(?status, principal)` + `listarAtrasados(principal)` + `buscar(id, principal)` com escopo
  E-7/`canSeeLoans` (Admin tudo, demais só `id_pessoa` próprio); `verificarAtrasados()` (ATIVO ∧
  prevista < hoje → ATRASADO + evento via E9, retorna contagem); `toResponse` com tomador =
  `RhContract::nomePessoa(id) ?? "Pessoa #id"` (**sem join cross-schema**, nunca 500 pelo nome).
- **escopo:** módulo `estoque` + peça Laravel `Service`.
- **entradas:** `service/EmprestimoService` + `EmprestimoMapper::rotuloPessoa/toResponse` (regra do
  rótulo) + F6; E2; `RhContract` (E9).
- **saidas:** `app/Modules/Estoque/Services/EmprestimoService.php`.
- **criterios_de_conclusao:**
  - [ ] Terceiro criando/devolvendo/lendo empréstimo alheio → 403; devolução soma o saldo exato
  - [ ] RH fora → resposta traz `Pessoa #id` e status 200 (fail-soft testado em E12)
- **dependencias:** E2 (E9 p/ `RhContract` + eventos).
- **prioridade:** P0.

## E8 — Service BOM (versões + consumo→baixa) — `reuse`

- **descricao:** Regras (só aqui): `criar` (`versao` default 1, `editavel` default true, itens com
  `quantidade_prevista`, UK bom+item); `listar(?idProdutoServico)` (nulo = todas — E-2/R-8);
  `atualizar` (`versao` default `atual+1`, reconcilia `item_bom`: atualiza previstos existentes,
  adiciona novos, remove ausentes); `registrarConsumo(id, itens)` (item pertence à BOM senão
  400/422; preenche `quantidade_real`; baixa via `ItemService::registrarConsumo` com
  `id_referencia` = id da BOM; retorna BOM). **Sem tabela de reserva** (fora de escopo — a
  "reserva p/ produção" é este fluxo idempotente + `EstoqueContract`, plan §M3.3).
- **escopo:** módulo `estoque` + peça Laravel `Service`.
- **entradas:** `service/BomService` + F7–F8; E2; E6 (`ItemService`).
- **saidas:** `app/Modules/Estoque/Services/BomService.php`.
- **criterios_de_conclusao:**
  - [ ] Cada `atualizar` incrementa `versao` e mantém rastreabilidade dos itens
  - [ ] Consumo baixa o saldo exato e registra saída `CONSUMO` com `id_referencia` = BOM; item estranho → 400/422 sem baixa parcial
- **dependencias:** E2, E6.
- **prioridade:** P0.

## E9 — Events/Listeners + `EstoqueContract` (+`RhContract::nomePessoa` aditivo) — `reuse` (+2 `create`)

- **descricao:** Emissão via `Event`s nativos (`ShouldQueue`, fila `database`, **nomes+payloads
  preservados**): `estoque.baixo.event` (`idItem,nome,quantidadeAtual,estoqueMinimo`),
  `emprestimo.atrasado.event` (`idEmprestimo,idPessoa,idItem,dataDevolucaoPrevista`),
  `compra.solicitada.event` (`idEntrada,idItem,idFornecedor,quantidade,valorUnitario,valorTotal,
  dataEntrada,notaFiscal`) — consumo real em M7/Notification e M5/Financeiro (fora de E9).
  Consumidor: `Listeners/ConsumoProducaoListener.php` do evento `producao.concluida.event`
  (`idEncomenda,idProdutoServico,itens[]`) → `ItemService::baixarPorConsumo()` (produtor real em
  M6). Criar **`Contracts/EstoqueContract.php`** (**create** justificado: `itemExiste(int): bool`,
  `saldoDe(int): ?string`, `baixarConsumo(array $itens, int $idReferencia): array` idempotente —
  p/ Vendas/M4 e Produção/M6; **só o consumido**). Estender `RhContract` com
  `nomePessoa(int): ?string` (**create** justificado, **modify** aditivo no `Rh`: lê `rh.pessoa`,
  null quando ausente; sem quebra, sem join cross-schema). Dispatch fail-soft: falha de fila nunca
  estoura 500 na movimentação.
- **escopo:** módulo `estoque` (+`Rh/Contracts` aditivo, +`Shared/Events` se transversal) + peça
  Laravel `Event/Listener/Contract`.
- **entradas:** `dto/{EstoqueBaixo,EmprestimoAtrasado,CompraSolicitada,ProducaoConcluida,ItemConsumido}Event`
  + `EstoqueEventPublisher` (nomes/payloads/fail-soft) + `ConsumoProducaoListener`; E6–E8.
- **saidas:** `Events/{EstoqueBaixo,EmprestimoAtrasado,CompraSolicitada,ProducaoConcluida}Event.php`,
  `Listeners/ConsumoProducaoListener.php`, `Contracts/EstoqueContract.php` (+ implementação
  Eloquent do próprio schema), `Rh/Contracts/RhContract.php` + `DefaultRhContract::nomePessoa`.
- **criterios_de_conclusao:**
  - [ ] 3 nomes emitidos + payloads inalterados; `producao.concluida.event` com itens vazios = no-op; duplo dispatch = 1 baixa (idempotência)
  - [ ] Nenhum módulo importa `Service/Model` do Estoque — só `Contract`/`Event` (grep em E13); testes Rh sem regressão
- **dependencias:** E6, E7, E8.
- **prioridade:** P1.

## E10 — Scheduler empréstimos atrasados (`dailyAt 03:00`) + config — `reuse`

- **descricao:** `EmprestimoService::verificarAtrasados()` executável manualmente (contagem de marcados);
  agendar em `routes/console.php` com `Schedule::call(...)->dailyAt('03:00')->name('estoque:emprestimos-atrasados')`
  (conversão documentada do Spring `0 0 3 * * *`) + `config/estoque.php` com
  `emprestimo_scheduler_enabled` (default on — equivale a
  `estoque.alerta.emprestimo.scheduler.enabled`; cron customizável se o Builder preferir
  `->cron()` com a mesma config).
- **escopo:** módulo `estoque` + peça Laravel `Schedule/config`.
- **entradas:** `scheduler/EmprestimoAtrasadoScheduler` + requirements F6/NFR; E7 (regra), E9 (evento).
- **saidas:** entrada em `routes/console.php`, `config/estoque.php`.
- **criterios_de_conclusao:**
  - [ ] Chamada manual marca só ATIVO+vencido como ATRASADO e publica 1 evento por marcado (zero p/ não-vencidos/devolvidos)
  - [ ] Agendamento registrado `03:00` diário; flag desliga sem erro
- **dependencias:** E7, E9.
- **prioridade:** P1.

## E11 — Seeders (verificação — sem seed no contrato) — `verificação`

- **descricao:** Conferir que V1–V9 **não trazem seeds** (ao contrário do Auth V6/V7): nenhuma
  `EstoqueSeeder` inventada. Se o Builder achar dado mínimo exigido por constraint/Policy,
  propor seed dev-only idempotente com flag (mesmo padrão `AUTH_SEED_ENABLED` do M1) e escalar ao
  Orquestrador — nunca popular em silêncio.
- **escopo:** módulo `estoque` + peça Laravel `Seeder` (se houver).
- **entradas:** migrations E1; padrão `AuthSeeder` (M1).
- **saidas:** relatório "sem seed necessário" OU `database/seeders/EstoqueSeeder.php` dev-only justificado.
- **criterios_de_conclusao:**
  - [ ] `migrate:fresh` + testes passam sem seed; nenhum dado inventado versionado sem justificativa
- **dependencias:** E1, E2.
- **prioridade:** P2.

## E12 — Testes Unit/Feature (401/403, fluxos, concorrência, CSV, E-4/E-7, 80%) — `reuse` (+1 `create`)

- **descricao:** Cobrir M3 em PHPUnit/Pest via `php artisan test` **em container Docker** (pgsql
  `fablab_testing`, com `pcov`): unit (saldo soma/subtrai, `valor_total`, sem-negativo, atraso,
  BOM versão/reconciliação/consumo, CSV roundtrip + injection sanitizada, rótulo E-4, escopo E-7)
  + feature (CRUD itens/entradas/saídas/empréstimos/fornecedores/localizações/BOMs, filtros,
  devolução, `/atrasados`, `?status=` válido/inválido, consumo→baixa→saída CONSUMO, Listener
  `producao.concluida.event` via fila `database`, `EstoqueContract::baixarConsumo` idempotente,
  scheduler manual, 3 eventos via `Event::fake`) + **concorrência real** (2 transações simultâneas
  no mesmo item nunca negativam — **create**) + **slice security 401/403 por rota sensível**
  (incl. Voluntário com/sem vínculo, Estagiário escrita →403, Recrutando →403 no módulo).
- **escopo:** módulo `estoque` (+`Shared`, +regressão Rh/Auth) + peça Laravel `Test`.
- **entradas:** suíte Java (casos, não código) + E4–E11; DoD (`monolito-checklist.md`).
- **saidas:** `tests/Unit/Estoque/*`, `tests/Feature/Estoque/*` verdes; cobertura ≥ 80% (módulo); Auth+Rh sem regressão.
- **criterios_de_conclusao:**
  - [ ] `php artisan test` verde (container); cobertura ≥ 80%
  - [ ] 401 sem token e 403 sem papel/vínculo em cada rota de escrita; E-7 isolado; CSV malicioso neutralizado; concorrência sem negativo
- **dependencias:** E4, E5, E6, E7, E8, E9, E10, E11.
- **prioridade:** P0.

## E13 — Gate DoD M3 (verificação final, sem código) — `create` (verificação)

- **descricao:** Conferir o DoD M3 (estende M0+M1+M2, sem sobrescrever): estrutura MVC do `Estoque`,
  schema próprio, fronteiras (`Contract`/`Event` só; `RhContract` aditivo sem quebra), rotas
  `/api/estoque/**` exatas, `php artisan test` verde + 80% + 401/403 + concorrência, código mínimo
  (cada `create` justificado no plan §M3.3). Sem código — relatório pass/fail.
- **escopo:** módulo `estoque` + peça `verificação`.
- **entradas:** E1–E12 + `.opencode/state/monolito-checklist.md` (DoD).
- **saidas:** relatório de gate (pass/fail por item) + remoções BAIXA/MÉDIA se houver excesso.
- **criterios_de_conclusao:**
  - [ ] Código em `app/Modules/Estoque/{Models,Http,Services,Policies,Events,Listeners}` (+`Contracts/` só
    consumido, +`Enums/`); `config/exceptions` só em `Shared`
  - [ ] Models `estoque.*`; regra só em `Service` (ou `Model` se trivial); migrations
    `database/migrations/estoque/`; `migrate:fresh` verde
  - [ ] Zero Eureka/Gateway/HTTP entre módulos (`grep` limpo); eventos nativos com nomes preservados; sem join cross-schema
  - [ ] Rotas `/api/estoque/**` exatas (§M3.2); zero `?status=` em entradas/saídas; naming PT (`/api/estoque/**`, nunca `/stock`); `reuse > create`; `docs/` intocado; nada commitado
- **dependencias:** E12.
- **prioridade:** P0.

## Grafo de dependências M3 (resumo)

```
E1 ─→ E2 ─┬→ E3 ─→ E4 ─┐
           ├→ E3 ─→ E5 ─┤
           ├→ E6 ──────┤→ E9 ─→ E10 ─┐
           ├→ E7 ──────┤→ E9 ────────┤→ E12 ─→ E13
           └→ E8 ──────┘→ E9 ────────┘
E1 ─→ E11 ─→ E12   (E4 × E5 × E6 × E7 × E8 paralelizáveis após E2/E3)
```

---

# Tasks M4 — módulo `vendas` (prefixo V)

> Destino: `backend/` Laravel (`App\Modules\Vendas`, schema `vendas`). Referência de regra (não código):
> `vendas-service/src/**` + `specs/features/vendas-service/requirements.md` (F1–F8) + `system/rbac-matrix.md`
> (prevalece onde diverge do Java — ver plan §M4.1.4). Base pronta: `AuthContract` (M1),
> `RhContract` (M2), `EstoqueContract` (M3, `itemExiste` p/ V7). Convenções: Eloquent sem `Repository`;
> `FormRequest` valida+autoriza; `Resource` dá o shape (incl. documento mascarado LGPD); regra só em `Service`
> (ou `Model` se trivial); `Policy`/`can:` p/ RBAC (escrita Admin + criador+Admin no Kanban);
> eventos `<ctx>.<ent>.<acao>.event` via `Event/Listener` (`ShouldQueue`, fila `database`);
> concorrência via coluna `versao` + 409 (sem-negativo análogo: sem-mover-conflitante); cobertura ≥ 80%.
> Legenda tipo: `reuse` = regra preservada em roupagem Laravel · `create` = justificado no `plan.md` §M4.3.
> **Nunca `docs/`; não commitar (Builder entrega em working tree).**

## V1 — Migrations `database/migrations/vendas/` (schemas + V1–V11) — `reuse`

- **descricao:** Reescrever as migrations Flyway como migrations Laravel qualificando o schema `vendas`,
  **sem alterar** tabelas/colunas/constraints/índices/UKs: V1 `cliente` (UK `cpf_cnpj`), V2 `tag_cliente`
  (UK nome), V3 `cliente_tag` (UK cliente+tag, FKs), V4 `orcamento` (CHECK status, FK cliente),
  V5 `item_orcamento` (FK orçamento), V6 `encomenda` (`versao`, CHECK kanban, `criado_por`,
  `encomenda_origem_id` auto-FK, índices status/cliente/orçamento), V7 `historico_status_encomenda`
  (FK encomenda), V8 `registro_marketplace` (FK encomenda), V9 `interacao_cliente`, V10 `tarefa_marketing`,
  V11 `solicitacao_edicao` (CHECKs tipo/alvo/status, índice status — drift §M4.1.1, upstream prevalece).
  Primeira migration garante `CREATE SCHEMA IF NOT EXISTS vendas` (idempotente, padrão M1).
- **escopo:** módulo `vendas` + peça Laravel `Migration`.
- **entradas:** `vendas-service/src/main/resources/db/migration/V1–V11.sql`; M0+M1 (conexão `fablab`, `CREATE SCHEMA`).
- **saidas:** `database/migrations/vendas/*.php` com `Schema::create('vendas.<tabela>')`.
- **criterios_de_conclusao:**
  - [ ] `php artisan migrate:fresh` em DB limpo verde; `migrate:status` sem pendência (com M1+M2+M3 aplicados)
  - [ ] Constraints/CHECKs/UKs/FKs/índices conferem 1:1 com os `.sql` V1–V11 (qualquer delta documentado e justificado)
- **dependencias:** M0+M1+M2+M3 (scaffold, conexão `fablab`, migrator com glob).
- **prioridade:** P0 (bloqueia V2–V13).

## V2 — Models Eloquent + enums + `DocumentoRule` (10 models, `vendas.*`, sem Repository) — `reuse` (+1 `create`)

- **descricao:** Criar os 10 models (`Cliente`, `TagCliente`, `Orcamento`, `ItemOrcamento`, `Encomenda`,
  `HistoricoStatusEncomenda`, `RegistroMarketplace`, `InteracaoCliente`, `TarefaMarketing`,
  `SolicitacaoEdicao`) com `$table='vendas.<tabela>'`, `$timestamps=false`, `$fillable` explícito,
  casts (decimal `valor_*`, date/datetime); `cliente_tag` como `belongsToMany` (sem model pivot);
  enums PHP 8.1 `string backed` (`TipoPessoa`: PF/PJ, `StatusOrcamento`: Pendente/Aprovado/Recusado/Ajuste,
  `StatusKanban`: Fila/Produção/Acabamento/Pronto/Entregue, `StatusSolicitacao`: Pendente/Aprovada/Rejeitada,
  `TipoSolicitacao`, `TipoAlvoSolicitacao`: CLIENTE/ORCAMENTO/ENCOMENDA); tipo de interação /
  status e prioridade de tarefa via regra `in:` (sem enum dedicado); `NivelAcesso` **não** recriado
  (reuse `Auth\Enums\Role`); `repositories` **não** recriados. Criar `Rules/DocumentoRule.php`
  (**create** justificado: valida CPF **e** CNPJ com dígitos; `CpfRule` do Rh não cobre CNPJ).
- **escopo:** módulo `vendas` + peça Laravel `Model/Enum/Rule`.
- **entradas:** `vendas-service/.../entity/*` + `service/DocumentoUtil.java` (algoritmos + máscara); V1 (tabelas).
- **saidas:** `app/Modules/Vendas/Models/*.php` + `Enums/` + `Rules/DocumentoRule.php`; zero `Repository/` no módulo.
- **criterios_de_conclusao:**
  - [ ] Models batem com V1 coluna a coluna (`cpf_cnpj` único; `versao`; `criado_por`; `encomenda_origem_id`)
  - [ ] Mass assignment bloqueado (`$fillable` explícito); documento inválido/duplicado rejeitado na validação
- **dependencias:** V1.
- **prioridade:** P0.

## V3 — Policies/Gates RBAC (escrita Admin + criador+Admin no Kanban) — `reuse`

- **descricao:** `Policies/VendasPolicy.php` + Gates: escrita de domínio (clientes/tags/bulk-tag,
  orçamentos/duplicar, encomendas criar, marketplace, interações, tarefas) = **Admin (0)** —
  delta matriz×Java documentado no plan §M4.1.4 (sem papel "responsável de Vendas" por decisão PO);
  leitura (`GET`, listar, `/status`, `historico-status`) = qualquer autenticado **exceto Recrutando (4)**
  (matriz: `–` no módulo → 403); `solicitar` (POST) = B/V/E autenticados (pedido, não decisão);
  `decidir` + `bulk-tag` = Admin-only; **kanban mover + nova-ordem = criador (`criado_por` = claim
  `id_user`) ou Admin, senão 403** (`PermissaoUtil.exigirCriadorOuAdmin` preservado na Policy —
  enforcement backend, nunca no front). JWT validado pelo guard `api` do Auth (M1) + `blacklist`.
- **escopo:** módulo `vendas` (+`Shared` se transversal) + peça Laravel `Policy/Gate`.
- **entradas:** `ESCRITA/LEITURA/ACESSO` dos 9 controllers + `service/PermissaoUtil.java` + `system/rbac-matrix.md`
  (regras 1–5: só Admin decide; Recrutando fora do módulo); V2; `AuthContract::isAdmin`.
- **saidas:** `app/Modules/Vendas/Policies/VendasPolicy.php` (+ Gates/`can:` equivalentes) aplicados nas rotas V4–V6.
- **criterios_de_conclusao:**
  - [ ] Escrita por B/V/E devolve 401 sem token e 403 com token (coberto em V12); Recrutando →403 em tudo
  - [ ] Kanban/nova-ordem por não-criador não-Admin →403 (teste dedicado em V12)
- **dependencias:** V2.
- **prioridade:** P0.

## V4 — Controllers clientes/tags + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `ClienteController` (`POST/GET /api/vendas/clientes` com filtros
  `?termo=&tipo=&tags=`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` — bloqueado com encomendas vinculadas,
  `POST /{id}/tags`, `DELETE /{id}/tags/{tagId}`, `POST /bulk-tag` — Admin), `TagController`
  (`POST/GET /api/vendas/tags`). Saída de cliente com **documento mascarado** (LGPD — `DocumentoUtil::mascarar`
  na `Resource`). Sem regra, sem mapper.
- **escopo:** módulo `vendas` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `ClienteController`, `TagController` Java (paths/verbos/`@PreAuthorize`) + DTOs (shapes +
  Bean Validation: documento, email, tipo PF/PJ); V2 (models+rule), V3 (proteção).
- **saidas:** `Http/Controllers/{Cliente,Tag}Controller.php`, `Http/Requests/*`, `Http/Resources/*`
  (documento mascarado), trechos de `routes.php` (`/api/vendas/clientes`, `/api/vendas/tags`).
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas (verbo+path+query `?termo=&tipo=&tags=` só em clientes); controllers sem cálculo/regra (grep em V13)
  - [ ] Documento nunca sai em claro; validação rejeita 400/422 antes do service
- **dependencias:** V2, V3.
- **prioridade:** P0.

## V5 — Controllers orçamentos/encomendas/histórico + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `OrcamentoController` (`POST/GET /api/vendas/orcamentos`
  `?status=&clienteId=`, `GET /{id}`, `PUT /{id}`, `POST /{id}/duplicar`), `EncomendaController`
  (`POST /api/vendas/encomendas`, `GET` `?status=&clienteId=`, `GET /{id}`, `GET /{id}/status`,
  `PUT /{id}/kanban` com `versao`, `POST /{id}/nova-ordem`), `HistoricoController`
  (`GET /api/vendas/historico-status/{idEncomenda}`). **Sem endpoint `converter`** (conversão é o
  `POST /encomendas` com `idOrcamento`, conforme contrato). Sem regra, sem mapper.
- **escopo:** módulo `vendas` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `Orcamento/Encomenda/HistoricoController` Java (paths/verbos) + DTOs (itens, kanban+versao,
  nova-ordem, status); V2, V3.
- **saidas:** `Http/Controllers/{Orcamento,Encomenda,Historico}Controller.php`, `Requests/*`,
  `Resources/*`, trechos de `routes.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M4.2 (incl. `/{id}/kanban`, `/{id}/nova-ordem`, `/{id}/duplicar`, `/{id}/status`)
  - [ ] `versao` ausente/divergente no kanban → 409; validação rejeita 400/422 antes do service
- **dependencias:** V2, V3.
- **prioridade:** P0.

## V6 — Controllers marketplace/CRM/solicitações + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `MarketplaceController` (`POST/GET /api/vendas/marketplace`
  com **só** `?encomendaId=&plataforma=`), `InteracaoController` (`POST /api/vendas/interacoes`,
  `GET /{idCliente}` — **sem list-all global**, fora do contrato), `TarefaMarketingController`
  (`POST/GET /api/vendas/tarefas-marketing` `?responsavelId=&status=`, `PUT /{id}`),
  `SolicitacaoController` (`POST/GET /api/vendas/solicitacoes` `?status=`, `PUT /{id}/decisao` —
  Admin). **Sem CSV** (só Estoque tem). Sem regra, sem mapper.
- **escopo:** módulo `vendas` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** 4 controllers Java restantes (paths/verbos/`ACESSO`+Admin em decisao) + DTOs
  (taxa, tipo interação, datas tarefa, decisao APROVAR/REJEITAR); V2, V3.
- **saidas:** `Http/Controllers/{Marketplace,Interacao,TarefaMarketing,Solicitacao}Controller.php`,
  `Requests/*`, `Resources/*`, `routes.php` do módulo + registro em `routes/api.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M4.2 (sem `GET /interacoes` global — grep em V13); `status`/`decisao` inválidos → 400/422
  - [ ] Decisão por não-Admin →403 (coberto em V12)
- **dependencias:** V2, V3.
- **prioridade:** P0.

## V7 — Services cliente/orçamento (filtros, total, ajustes, aprovar, duplicar) — `reuse`

- **descricao:** Regras (só aqui): `ClienteService`: CRUD + `DocumentoRule` + filtros Eloquent
  (termo LIKE, tipo, tags via `whereHas`) + vincular/desvincular + `bulkTag` (Admin, retorna
  contagem) + excluir bloqueado com encomendas vinculadas; `TagCliente`: CRUD trivial **no
  `Model`/Eloquent sem service forçado** (sem duplicar). `OrcamentoService`: criar com itens
  (`valor_total = Σ(qtd × unit)` no service; `id_item_estoque` opcional **validado** via
  `EstoqueContract::itemExiste` — 422 se inexistente, sem coluna nova); atualizar (ajustes → status
  `Ajuste`; aprovado+convertido trava edição); aprovar publica `orcamento.aprovado.event` (via V10);
  `duplicar` (cópia Pendente + itens, `criado_por` = atual).
- **escopo:** módulo `vendas` + peça Laravel `Service`.
- **entradas:** `service/{Cliente,Orcamento,Tag}Service` + requirements F1–F3; V2; `EstoqueContract` (M3).
- **saidas:** `app/Modules/Vendas/Services/{Cliente,Orcamento}Service.php`, regra testável sem HTTP.
- **criterios_de_conclusao:**
  - [ ] Total recalculado no service (nunca confiado do payload); orçamento de item-estoque inexistente →422 sem persistir
  - [ ] Orçamento aprovado+convertido não edita (409); `duplicar` gera Pendente com mesmos itens
- **dependencias:** V2 (V10 p/ emissão do evento).
- **prioridade:** P0.

## V8 — Service encomenda (criar, kanban+versão, nova-ordem) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction`): `criar` (com `idOrcamento`: exige Aprovado +
  não-convertido, herda cliente+total — `valorFinal` do payload só sobrescreve; sem `idOrcamento`:
  venda direta exige cliente existente + valor; nasce `Fila` + histórico inicial + publica
  `encomenda.criada.event` via V10); `listar(?status, ?clienteId)`; `detalhar`; `status`;
  `moverKanban` (criador+Admin via V3, `versao` divergente →409, `Entregue` trava, grava histórico +
  publica `encomenda.status.alterado.event`); `novaOrdem` (criador+Admin; encerra atual como
  `Entregue`/encerrada + cria nova com `encomenda_origem_id` + publica criada); `historico(id)`
  (leitura do `HistoricoService` dobrada aqui, sem classe extra).
- **escopo:** módulo `vendas` + peça Laravel `Service`.
- **entradas:** `service/{Encomenda,Historico}Service` + requirements F4–F5; V2; V3 (criador+Admin).
- **saidas:** `app/Modules/Vendas/Services/EncomendaService.php`.
- **criterios_de_conclusao:**
  - [ ] Encomenda de orçamento não-aprovado/já-convertido →409; venda direta sem cliente/valor →400/422
  - [ ] Kanban com `versao` antiga →409 sem mover; `Entregue` não move; nova ordem referencia a origem
- **dependencias:** V2, V3 (V10 p/ emissão).
- **prioridade:** P0.

## V9 — Services marketplace/CRM/solicitações (D-4 sem aplicação automática) — `reuse`

- **descricao:** Regras (só aqui): `MarketplaceService::registrar` (exige encomenda existente;
  `?encomendaId=&plataforma=` no listar) + `liquido(bruto, taxa)` puro (dado p/ Financeiro);
  `CrmService`: `registrarInteracao` (exige cliente; `id_usuario` = criador atual), `listarInteracoes`
  (por cliente), `criarTarefa` (`data_fim ≥ data_inicio`), `listarTarefas(?responsavelId, ?status)`,
  `atualizarTarefa` (status/prioridade); responsável persistido como id sem join cross-schema;
  `SolicitacaoService`: `solicitar` (B/V/E; alvo CLIENTE/ORCAMENTO/ENCOMENDA + campo/valor/justificativa),
  `listar(?status)` + counts por status, `decidir` (Admin-only; APROVAR flipa p/ Aprovada,
  REJEITAR exige motivo; grava `decidido_por/motivo/data_decisao`; **sem aplicação automática — D-4**).
- **escopo:** módulo `vendas` + peça Laravel `Service`.
- **entradas:** `service/{Marketplace,Crm,Solicitacao}Service` + requirements F6–F8; V2.
- **saidas:** `app/Modules/Vendas/Services/{Marketplace,Crm,Solicitacao}Service.php`.
- **criterios_de_conclusao:**
  - [ ] Marketplace de encomenda inexistente →404; tarefa com `data_fim < data_inicio` →400/422
  - [ ] Decidir solicitação já decidida →409; rejeitar sem motivo →400/422; aprovada **não** altera o alvo (D-4, grep em V13)
- **dependencias:** V2.
- **prioridade:** P0.

## V10 — Events/Listeners + `VendasContract` (emite 3, consome produção) — `reuse` (+1 `create`)

- **descricao:** Emissão via `Event`s nativos (`ShouldQueue`, fila `database`, **nomes+payloads
  preservados**): `encomenda.criada.event` (`idEncomenda,idCliente,valorFinal,dataCriacao`),
  `encomenda.status.alterado.event` (`idEncomenda,statusAnterior,statusNovo,dataAlteracao`),
  `orcamento.aprovado.event` (`idOrcamento,idCliente,valorTotal`) — consumo real em M6/Produção,
  M5/Financeiro e M7/Notification (fora de V10). Consumidor:
  `Listeners/ProducaoStatusListener.php` do evento `producao.status.alterado.event`
  (`idEncomenda,statusNovo,observacao`) → sync idempotente (igual = no-op, desconhecido = ignore,
  `id_usuario=0` sistema + histórico; produtor real em M6). Criar
  **`Contracts/VendasContract.php`** (**create** justificado: `dadosEncomenda(int): ?array`
  (id/idCliente/idOrcamento/valorFinal/statusKanban/dataCriacao/dataPrevisao) p/ M6,
  `dadosOrcamento(int): ?array` (id/idCliente/valorTotal/status) p/ M5; **só o consumido**,
  congelado em 2 métodos). Dispatch fail-soft: falha de fila nunca estoura 500.
- **escopo:** módulo `vendas` (+`Shared/Events` se transversal) + peça Laravel `Event/Listener/Contract`.
- **entradas:** `rabbit/{VendasEventos,VendasEventPublisher,ProducaoStatusListener}` (nomes/payloads/
  fail-soft/idempotência) + `RabbitConfig` (routing keys → nomes de evento); V7–V9.
- **saidas:** `Events/{EncomendaCriada,EncomendaStatusAlterado,OrcamentoAprovado,ProducaoStatusAlterado}Event.php`,
  `Listeners/ProducaoStatusListener.php`, `Contracts/VendasContract.php` (+ implementação Eloquent
  do próprio schema).
- **criterios_de_conclusao:**
  - [ ] 3 nomes emitidos + payloads inalterados; sync da produção com status igual = no-op; desconhecido = ignore sem 500
  - [ ] Nenhum módulo importa `Service/Model` do Vendas — só `Contract`/`Event` (grep em V13)
- **dependencias:** V7, V8, V9.
- **prioridade:** P1.

## V11 — Seeders (verificação — sem seed no contrato) — `verificação`

- **descricao:** Conferir que V1–V11 **não trazem seeds** (ao contrário do Auth V6/V7): nenhuma
  `VendasSeeder` inventada. Se o Builder achar dado mínimo exigido por constraint/Policy,
  propor seed dev-only idempotente com flag (mesmo padrão `AUTH_SEED_ENABLED` do M1) e escalar ao
  Orquestrador — nunca popular em silêncio.
- **escopo:** módulo `vendas` + peça Laravel `Seeder` (se houver).
- **entradas:** migrations V1; padrão `AuthSeeder` (M1).
- **saidas:** relatório "sem seed necessário" OU `database/seeders/VendasSeeder.php` dev-only justificado.
- **criterios_de_conclusao:**
  - [ ] `migrate:fresh` + testes passam sem seed; nenhum dado inventado versionado sem justificativa
- **dependencias:** V1, V2.
- **prioridade:** P2.

## V12 — Testes Unit/Feature (401/403, 403 não-criador, fluxos, LGPD, 80%) — `reuse` (+1 `create`)

- **descricao:** Cobrir M4 em PHPUnit/Pest via `php artisan test` **em container Docker** (pgsql
  `fablab_testing`, com `pcov`): unit (DocumentoRule CPF/CNPJ + máscara, total do orçamento,
  `liquido`, `versao`/conflito, D-4 sem auto-apply, duplicar, counts) + feature (CRUD clientes/tags,
  filtros, bulk-tag Admin-only, orçamentos→aprovar→evento→converter→encomenda, venda direta,
  kanban+histórico+evento, nova-ordem+origem, marketplace, interações escopadas, tarefas,
  solicitar→decidir, Listener `producao.status.alterado.event` via fila `database`,
  `VendasContract` 2 métodos, 3 eventos via `Event::fake`) + **slice security 401/403 por rota
  sensível + 403 não-criador no kanban/nova-ordem** (**create**) + Auth/Estoque/Rh sem regressão.
- **escopo:** módulo `vendas` (+`Shared`, +regressão Auth/Rh/Estoque) + peça Laravel `Test`.
- **entradas:** suíte Java (casos, não código) + V4–V11; DoD (`monolito-checklist.md`).
- **saidas:** `tests/Unit/Vendas/*`, `tests/Feature/Vendas/*` verdes; cobertura ≥ 80% (módulo); M1+M2+M3 sem regressão.
- **criterios_de_conclusao:**
  - [ ] `php artisan test` verde (container); cobertura ≥ 80%
  - [ ] 401 sem token e 403 sem papel em cada rota de escrita; não-criador não-Admin →403 no kanban/nova-ordem; documento nunca em claro
- **dependencias:** V4, V5, V6, V7, V8, V9, V10, V11.
- **prioridade:** P0.

## V13 — Gate DoD M4 (verificação final, sem código) — `create` (verificação)

- **descricao:** Conferir o DoD M4 (estende M0–M3, sem sobrescrever): estrutura MVC do `Vendas`, schema
  próprio, fronteiras (`Contract`/`Event` só; `EstoqueContract` só-leitura sem quebra), rotas
  `/api/vendas/**` exatas, `php artisan test` verde + 80% + 401/403 + 403 não-criador + LGPD, código
  mínimo (cada `create` justificado no plan §M4.3). Sem código — relatório pass/fail.
- **escopo:** módulo `vendas` + peça `verificação`.
- **entradas:** V1–V12 + `.opencode/state/monolito-checklist.md` (DoD).
- **saidas:** relatório de gate (pass/fail por item) + remoções BAIXA/MÉDIA se houver excesso.
- **criterios_de_conclusao:**
  - [ ] Código em `app/Modules/Vendas/{Models,Http,Services,Policies,Events,Listeners}` (+`Contracts/` só
    consumido, +`Enums/`+`Rules/`); `config/exceptions` só em `Shared`
  - [ ] Models `vendas.*`; regra só em `Service` (ou `Model` se trivial); migrations `database/migrations/vendas/`; `migrate:fresh` verde
  - [ ] Zero Eureka/Gateway/HTTP entre módulos (`grep` limpo); eventos nativos com nomes preservados; sem join cross-schema
  - [ ] Rotas `/api/vendas/**` exatas (§M4.2); sem `GET /interacoes` global; sem CSV; sem auto-apply em decidir (`grep`); `reuse > create`; `docs/` intocado; nada commitado
- **dependencias:** V12.
- **prioridade:** P0.

## Grafo de dependências M4 (resumo)

```
V1 ─→ V2 ─┬→ V3 ─→ V4 ─┐
           ├→ V3 ─→ V5 ─┤
           ├→ V3 ─→ V6 ─┤→ V10 ─┐
           ├→ V7 ──────┤→ V10 ──┤→ V12 ─→ V13
           ├→ V8 ──────┤→ V10 ──┤
           └→ V9 ──────┘→ V10 ──┘
V1 ─→ V11 ─→ V12   (V4 × V5 × V6 × V7 × V8 × V9 paralelizáveis após V2/V3)
```

---

# Tasks M5 — módulo `financeiro` (prefixo F)

> Destino: `backend/` Laravel (`App\Modules\Financeiro`, schema `financeiro`). Referência de regra (não código):
> `financeiro-service/src/**` + `specs/features/financeiro-service/requirements.md` (F1–F9) + `system/rbac-matrix.md`
> (coincide com o Java: Financeiro 100% Admin — sem delta). Base pronta: `AuthContract` (M1),
> `RhContract` (M2, coerência/horas — sem leitura nova em M5), `EstoqueContract` (M3), `VendasContract`
> (M4, `dadosEncomenda/dadosOrcamento` — sem leitura nova em M5: dados externos chegam por evento).
> Convenções: Eloquent sem `Repository`; `FormRequest` valida+autoriza (incl. allowlist `periodo=yyyy-MM`);
> `Resource` dá o shape servido D-2 (`counts`/`resumo`/agregados); regra só em `Service`;
> `Policy`/`can:admin` em tudo; eventos `<ctx>.<ent>.<acao>.event` via `Event/Listener` (`ShouldQueue`,
> fila `database`); scheduler `dailyAt('03:00')` + flag; cobertura ≥ 80%.
> Legenda tipo: `reuse` = regra preservada em roupagem Laravel · `create` = justificado no `plan.md` §M5.3.
> **Nunca `docs/`; não commitar (Builder entrega em working tree).**

## F1 — Migrations `database/migrations/financeiro/` (schemas + V1–V9) + `DB_SEARCH_PATH` — `reuse` (+`modify`)

- **descricao:** Reescrever as migrations Flyway como migrations Laravel qualificando o schema `financeiro`,
  **sem alterar** tabelas/colunas/constraints/índices/UKs: V1 `categoria_financeira` (CHECK RECEITA/DESPESA),
  V2 `lancamento_financeiro` (CHECKs tipo/status, `valor NUMERIC(14,2) > 0`, índices `(status,vencimento)` e
  `referencia`), V3 `doacao_recurso` (CHECK DOACAO/PROJETO), V4 `valor_hora_nivel` (CHECK nível 0–3,
  índice `(nivel,vigencia)`), V5 `parametro_overhead` (`NUMERIC(12,4)`), V6 `fechamento_encomenda`
  (`id_encomenda` UNIQUE, CHECK ABERTA/CONCLUIDA/CANCELADA, `horas_validadas` default 0),
  V7 `custo_encomenda` (índice `id_encomenda`), V8 `solicitacao_compra` (CHECK
  REGISTRADA/VISUALIZADA/CONCLUIDA), V9 `horas_encomenda` (UK encomenda+funcionário+data, índice
  `id_encomenda`). Primeira migration garante `CREATE SCHEMA IF NOT EXISTS financeiro` (idempotente,
  padrão M1). **Modify** aditivo: estender `DB_SEARCH_PATH` em `config/database.php`
  (`public,auth,rh,estoque,vendas,financeiro`).
- **escopo:** módulo `financeiro` (+`Shared/config` aditivo) + peça Laravel `Migration`.
- **entradas:** `financeiro-service/src/main/resources/db/migration/V1–V9.sql`; M0–M4 (conexão `fablab`, migrator glob).
- **saidas:** `database/migrations/financeiro/*.php` com `Schema::create('financeiro.<tabela>')`; `DB_SEARCH_PATH` estendido.
- **criterios_de_conclusao:**
  - [ ] `php artisan migrate:fresh` em DB limpo verde; `migrate:status` sem pendência (com M1–M4 aplicados)
  - [ ] Constraints/CHECKs/UKs/FKs/índices conferem 1:1 com os `.sql` V1–V9 (qualquer delta documentado e justificado)
- **dependencias:** M0+M1+M2+M3+M4 (scaffold, conexão `fablab`, migrator com glob).
- **prioridade:** P0 (bloqueia F2–F13).

## F2 — Models Eloquent + enums (9 models, `financeiro.*`, sem Repository) — `reuse`

- **descricao:** Criar os 9 models (`CategoriaFinanceira`, `LancamentoFinanceiro`, `DoacaoRecurso`,
  `ValorHoraNivel`, `ParametroOverhead`, `FechamentoEncomenda`, `CustoEncomenda`, `SolicitacaoCompra`,
  `HorasEncomenda`) com `$table='financeiro.<tabela>'`, `$timestamps=false`, `$fillable` explícito,
  casts (`valor_*` decimal, `data_*` date); enums PHP 8.1 `string backed` (`TipoCategoria`:
  RECEITA/DESPESA, `TipoLancamento`: ENTRADA/SAIDA, `StatusLancamento`: PENDENTE/PAGO/ATRASADO/CANCELADO,
  `TipoDoacao`: DOACAO/PROJETO, `StatusFechamento`: ABERTA/CONCLUIDA/CANCELADA, `StatusCompra`:
  REGISTRADA/VISUALIZADA/CONCLUIDA); `nivel_acesso` como int (CHECK 0–3 validado no Request, sem enum
  dedicado); `NivelAcesso`/`repositories` **não** recriados.
- **escopo:** módulo `financeiro` + peça Laravel `Model/Enum`.
- **entradas:** `financeiro-service/.../entity/*`; F1 (tabelas).
- **saidas:** `app/Modules/Financeiro/Models/*.php` + `Enums/`; zero `Repository/` no módulo.
- **criterios_de_conclusao:**
  - [ ] Models batem com F1 coluna a coluna (`id_encomenda` UNIQUE; UK horas; `horas_validadas` default 0)
  - [ ] Mass assignment bloqueado (`$fillable` explícito); nenhum `Repository/`/`Mapper/` no módulo
- **dependencias:** F1.
- **prioridade:** P0.

## F3 — Policies/Gates RBAC (Admin-exclusivo total) — `reuse`

- **descricao:** `Policies/FinanceiroPolicy.php` + `can:admin` em **todas** as rotas F4–F6 (l lançamentos,
  categorias, doações, valores-hora, overhead, fechamento/custos, compras, relatórios): qualquer
  autenticado com role ≠ 0 → 403; sem token → 401 (guard `api` do Auth/M1 + `blacklist`). Sem papel
  "responsável financeiro", sem vínculo `usuario_responsabilidade`, sem escopo "próprio" (matriz:
  módulo exclusivo Admin). Auditoria (`idUsuario` p/ logs D-5) lida de `auth()->user()` (ex-
  `FinanceiroPrincipal` some).
- **escopo:** módulo `financeiro` (+`Shared` se transversal) + peça Laravel `Policy/Gate`.
- **entradas:** `@PreAuthorize("hasRole('ADMIN')")` dos 7 controllers + `system/rbac-matrix.md` (regra 2:
  só Admin acessa o Financeiro); F2; `AuthContract::isAdmin`.
- **saidas:** `app/Modules/Financeiro/Policies/FinanceiroPolicy.php` (+ `can:admin`) aplicados nas rotas F4/F5/F6.
- **criterios_de_conclusao:**
  - [ ] Qualquer rota sem token →401; com Bolsista/Voluntário/Estagiário/Recrutando →403 (coberto em F12)
  - [ ] Zero rota pública no módulo além de `/up` do bootstrap (grep em F13)
- **dependencias:** F2.
- **prioridade:** P0.

## F4 — Controllers lançamentos/categorias + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `LancamentoController` (`POST /api/financeiro/lancamentos`,
  `GET` com filtros combinados `?status=&tipo=&idCategoria=&dataInicio=&dataFim=&origem=` +
  `&vencimentoDe=&vencimentoAte=` (D-4/D-8; `origem` = trecho da referência externa; sem `?periodo=`
  aqui), `PUT /{id}/pagamento` com `PagamentoRequest` opcional), `CategoriaController`
  (`POST/GET /api/financeiro/categorias` com `?tipo=`). Lista serve `lancamentos` + `counts` (todas as
  4 situações, zeradas quando ausentes) + `resumo{entradas,saidas,pendente,saldo}` (D-2). **Sem
  endpoint bulk** (bulk-bar do front = N×`PUT …/pagamento`). Sem regra, sem mapper.
- **escopo:** módulo `financeiro` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `LancamentoController`, `CategoriaController` Java (paths/verbos) + DTOs (shapes +
  Bean Validation: valor > 0, tamanhos, categoria obrigatória, datas); F2 (models+enums), F3 (proteção).
- **saidas:** `Http/Controllers/{Lancamento,Categoria}Controller.php`, `Http/Requests/*`
  (allowlist de enums/status/tipo), `Http/Resources/*` (`counts`+`resumo`), trechos de `routes.php`
  (`/api/financeiro/lancamentos|categorias`, `can:admin`).
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas (verbo+path+queries D-4/D-8; zero `?periodo=` em lançamentos — grep em F13)
  - [ ] Controllers sem cálculo/regra (grep em F13); validação rejeita 400/422 antes do service
- **dependencias:** F2, F3.
- **prioridade:** P0.

## F5 — Controllers doações/compras/parâmetros + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `DoacaoController` (`POST/GET /api/financeiro/doacoes-recursos`
  `?tipo=&dataInicio=&dataFim=`), `CompraController` (`POST/GET /api/financeiro/solicitacoes-compra`
  `?status=`, `GET /{id}`, `PUT /{id}/concluir` — **sem** rota de visualizar; `marcarVisualizada`
  não exposta), `ParametroController` (`POST/GET /api/financeiro/valores-hora`,
  `POST/GET /api/financeiro/parametros-overhead` — GET devolve o **vigente**, como no Java).
  Sem regra, sem mapper.
- **escopo:** módulo `financeiro` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `Doacao/Compra/ParametroController` Java (paths/verbos) + DTOs (origem ≤200, quantidade
  > 0, nível 0–3, vigências obrigatórias); F2, F3.
- **saidas:** `Http/Controllers/{Doacao,Compra,Parametro}Controller.php`, `Requests/*`, `Resources/*`,
  trechos de `routes.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M5.2 (sem `PUT …/visualizar`, sem `GET /parametros-overhead` plural-list — grep em F13)
  - [ ] `status`/`tipo`/`nivel` inválidos → 400/422; validação rejeita antes do service
- **dependencias:** F2, F3.
- **prioridade:** P0.

## F6 — Controllers custeio/fechamento + relatórios + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `CusteioController` (`POST /api/financeiro/fechamento-encomenda`,
  `GET /fechamento-encomenda` (listar), `GET /fechamento-encomenda/{idEncomenda}`,
  `POST /fechamento-encomenda/{idEncomenda}/nova-ordem`, `GET /api/financeiro/custos-encomenda/{idEncomenda}`
  — **sem** PUT/cancel em fechamento), `RelatorioController` (`GET /api/financeiro/relatorios/fluxo-caixa|dre|
  lucratividade|inadimplencia|doacoes-despesas|custo-maquina`; `?dataInicio=&dataFim=&periodo=` só em
  fluxo-caixa/dre/doacoes-despesas — `periodo` com **allowlist `yyyy-MM`** no Request, 422 fora do
  formato; lucratividade/inadimplência/custo-máquina sem período, como no Java). **Sem PDF/CSV/export**.
  Sem regra, sem mapper.
- **escopo:** módulo `financeiro` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `Custeio/RelatorioController` Java (paths/verbos/`@PreAuthorize`) + DTOs (estimativas ≥ 0,
  shapes D-2); F2, F3.
- **saidas:** `Http/Controllers/{Custeio,Relatorio}Controller.php`, `Requests/*` (allowlist `periodo`),
  `Resources/*` (shapes 1:1), `routes.php` do módulo + registro em `routes/api.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M5.2 (sem PUT em fechamento, sem export — grep em F13)
  - [ ] `periodo=2026-13`/`periodo=hoje` →422; validação rejeita 400/422 antes do service
- **dependencias:** F2, F3.
- **prioridade:** P0.

## F7 — Services lançamento/categoria (contas, liquidação, vencidos, shapes D-2) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction`): `LancamentoService::criar` (categoria existente
  senão 404; `PENDENTE`, ou `ATRASADO` + `lancamento.vencido.event` imediato se vencido);
  `listar` (filtros D-4/D-8 combinados + `counts`/`resumo` servidos, cancelados fora das somas);
  `registrarPagamento` (`PAGO`+`data_pagamento` default hoje; `PAGO/CANCELADO → 409/422`;
  `Log::info` de auditoria no lugar do `ManualPaymentProcessor`); `emitirVencidos` (PENDENTE/ATRASADO
  vencidos → ATRASADO + 1 evento cada, **sem liquidar**, retorna contagem). `CategoriaService`:
  criar (`nome` trim, `RECEITA/DESPESA`) + listar (`?tipo=`). Sem `PaymentProcessor` (não planejar).
- **escopo:** módulo `financeiro` + peça Laravel `Service`.
- **entradas:** `service/{LancamentoFinanceiro,CategoriaFinanceira,ManualPaymentProcessor}Service` + F1–F2/F5; F2.
- **saidas:** `app/Modules/Financeiro/Services/{Lancamento,Categoria}Service.php`, regra testável sem HTTP.
- **criterios_de_conclusao:**
  - [ ] Criar vencido publica 1 evento imediato; pagar `PAGO/CANCELADO` →409/422 sem mutar
  - [ ] `emitirVencidos` marca só PENDENTE→ATRASADO, publica por vencido e nunca liquida
- **dependencias:** F2 (F10 p/ emissão do evento).
- **prioridade:** P0.

## F8 — Services fechamento/custeio (congelamento D-5, horas, Job Order Costing) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction`): `FechamentoService::criar` (duplicado
  `id_encomenda → 409/422`; nasce `ABERTA`, `horas_validadas=0`, `Log` auditoria; `id_encomenda`
  opaco, sem checagem via `VendasContract`); `criarDoEvento` (idempotente: existente = no-op;
  `valorFechado=dataCriacao` do evento, estimadas 0); `consultar`; `registrarHorasValidadas`
  (upsert `(encomenda,funcionário,data)` + soma `horas_validadas`); `novaOrdem` (atual→`CONCLUIDA` +
  cria nova — nunca edita congelado; `Log` auditoria). `CusteioService::calcular` (materiais =
  Σ `SAIDA`¬canceladas c/ `referencia=str(idEncomenda)`; mão de obra = Σ `horas×valorHora(nivel ?? 2
  — `NIVEL_DEFAULT` const)`; overhead = `taxa×Σhoras`; `total`; `margem = fechado−total`;
  persiste + publica `custo.calculado.event`; pré-condições — sem fechamento, sem valor/hora do
  nível, sem overhead — com erro orientativo e **sem persistir parcial**); `consultar` (último
  cálculo `orderByDesc(data_calculo,id)`). Sem `cancelar` (sem rota — não planejar).
- **escopo:** módulo `financeiro` + peça Laravel `Service`.
- **entradas:** `service/{FechamentoEncomenda,Custeio}Service` + requirements F5/F7; F2; `ParametroService` (F9).
- **saidas:** `app/Modules/Financeiro/Services/{Fechamento,Custeio}Service.php`.
- **criterios_de_conclusao:**
  - [ ] Fechamento duplicado/nova-ordem preservam o congelado (imutável — sem PUT; grep em F13)
  - [ ] Custeio bate a fórmula item a item (materiais/mão-de-obra-overhead/total/margem); hora sem nível usa nível 2; pré-condição ausente → erro orientativo sem persistir
- **dependencias:** F2, F9 (parâmetros vigentes).
- **prioridade:** P0.

## F9 — Services doação/compra/parâmetro/relatório (informativo, vigências, 6 shapes) — `reuse`

- **descricao:** Regras (só aqui): `DoacaoService`: registrar (`DOACAO/PROJETO`, `id_projeto_associado`
  opcional sem join) + listar (`?tipo=&dataInicio=&dataFim=`); `CompraService`: registrar
  (`REGISTRADA`, `data_solicitacao`=hoje, publica `compra.solicitada.event` com `idFornecedor=null`
  no MVP) + listar (`?status=`) + detalhar + concluir (já-`CONCLUIDA → 409/422`); fluxo
  **informativo, sem bloqueio**; `ParametroService`: `definirValorHora`/`definirOverhead`
  (append-only + `Log` auditoria D-5), `valoresVigentes` (1 por nível 0–3), `overheadVigente`,
  `obterValorHoraVigente/obterTaxaVigente` (ausente → exceção orientativa); `RelatorioService`:
  6 relatórios 1:1 com o Java (`resolverPeriodo`: `dataInicio/dataFim` > `periodo yyyy-MM`;
  semanas `yyyy-'S'w`, meses `yyyy-MM`; cancelados fora das somas; inadimplência = só `ENTRADA
  ATRASADO`; dre receitas = entradas + doações). Sem PDF/export.
- **escopo:** módulo `financeiro` + peça Laravel `Service`.
- **entradas:** `service/{DoacaoRecurso,SolicitacaoCompra,Parametro,Relatorio}Service` + F3/F4/F6/F8; F2.
- **saidas:** `app/Modules/Financeiro/Services/{Doacao,Compra,Parametro,Relatorio}Service.php`.
- **criterios_de_conclusao:**
  - [ ] Compra concluir-dupla →409/422; parâmetros nunca editam (só criam vigência nova)
  - [ ] 6 relatórios conferem contra fixtures (somas, margem %, cobertura %, dias em atraso, semanas/meses)
- **dependencias:** F2 (F10 p/ emissão do evento de compra).
- **prioridade:** P0.

## F10 — Events/Listeners + scheduler vencidos (`dailyAt 03:00`) + config (sem `Contracts/`) — `reuse` (+1 `create`)

- **descricao:** Emissão via `Event`s nativos (`ShouldQueue`, fila `database`, **nomes+payloads
  preservados**, dispatch fail-soft nunca 500): `lancamento.vencido.event`
  (`idLancamento,valor,dataVencimento,idReferenciaExterna`) — no criar-ATRASADO (F7) e no
  `emitirVencidos`; `custo.calculado.event` (`idEncomenda,custoTotal,margemLucro,dataCalculo`) — no
  calcular (F8); `compra.solicitada.event` (`idCompra,idFornecedor=null`) — no registrar (F9).
  Consumidores `Listeners/*.php` (malformado = warn/ignore, sem 500): `encomenda.criada.event` →
  `FechamentoService::criarDoEvento` (idempotente); `horas.validadas.event` →
  `registrarHorasValidadas` (upsert); `producao.concluida.event` → `CusteioService::calcularDoEvento`
  (produtor real em M6). **Sem listener próprio p/ `orcamento.aprovado.event`** (sem consumer no
  Java — fluxo chega via `encomenda.criada`; §M5.5 R5) e **sem consumer p/ `compra.solicitada`
  do Estoque** (§M5.5 R6). **Sem `Contracts/`** (nenhum consumidor atual; M8 define via `modify`).
  Scheduler em `routes/console.php`: `Schedule::call(emitirVencidos)->dailyAt('03:00')` (delta
  08:00→03:00 §M5.1.4) + `config/financeiro.php` (**create** justificado, padrão M3/E10:
  `vencidos_scheduler_enabled`, default on).
- **escopo:** módulo `financeiro` (+`Shared/Events` se transversal) + peça Laravel
  `Event/Listener/Schedule/config`.
- **entradas:** `dto/FinanceiroEventos` + `FinanceiroEventPublisher` (nomes/payloads/fail-soft) +
  `consumer/*` (idempotências) + `VencidosScheduler`; F7–F9.
- **saidas:** `Events/{LancamentoVencido,CustoCalculado,CompraSolicitada,EncomendaCriada,HorasValidadas,ProducaoConcluida}Event.php`,
  `Listeners/{EncomendaCriada,HorasValidadas,ProducaoConcluida}Listener.php`, entrada em
  `routes/console.php`, `config/financeiro.php`; zero `Contracts/` no módulo.
- **criterios_de_conclusao:**
  - [ ] 3 nomes emitidos + payloads inalterados (`Event::fake` em F12); duplo `encomenda.criada` = 1 fechamento; horas duplicadas somam no upsert
  - [ ] Chamada manual de `emitirVencidos` publica 1 evento por vencido e zero p/ pagos/não-vencidos; flag desliga sem erro
  - [ ] Nenhum módulo importa `Service/Model` do Financeiro — só `Event` (grep em F13)
- **dependencias:** F7, F8, F9.
- **prioridade:** P1.

## F11 — Seeders (verificação — sem seed no contrato) — `verificação`

- **descricao:** Conferir que V1–V9 **não trazem seeds** (ao contrário do Auth V6/V7): nenhuma
  `FinanceiroSeeder` inventada (categorias/valores-hora iniciais são cadastrados via API pelo Admin).
  Se o Builder achar dado mínimo exigido por constraint/Policy, propor seed dev-only idempotente
  com flag (mesmo padrão `AUTH_SEED_ENABLED` do M1) e escalar ao Orquestrador — nunca popular em silêncio.
- **escopo:** módulo `financeiro` + peça Laravel `Seeder` (se houver).
- **entradas:** migrations F1; padrão `AuthSeeder` (M1).
- **saidas:** relatório "sem seed necessário" OU `database/seeders/FinanceiroSeeder.php` dev-only justificado.
- **criterios_de_conclusao:**
  - [ ] `migrate:fresh` + testes passam sem seed; nenhum dado inventado versionado sem justificativa
- **dependencias:** F1, F2.
- **prioridade:** P2.

## F12 — Testes Unit/Feature (401/403, fluxos, custeio, idempotência, 80%) — `reuse` (+1 `create`)

- **descricao:** Cobrir M5 em PHPUnit/Pest via `php artisan test` **em container Docker** (pgsql
  `fablab_testing`, com `pcov`): unit (fórmula do custeio item a item, `NIVEL_DEFAULT=2`,
  pré-condições sem persistir parcial, `resolverPeriodo`/allowlist, `resumo`/`counts`, margem %,
  cobertura %, dias em atraso, upsert de horas, filtros D-4/D-8) + feature (CRUD categorias,
  criar→(ATRASADO+evento)/pagar→409-duplo, doações, compras registrar→concluir→409-duplo,
  valores-hora/overhead append-only + vigentes, fechamento manual→409-duplo→nova-ordem imutável,
  custo calcular→consultar-último, 6 relatórios contra fixtures, 3 Listeners via fila `database`
  + idempotências, scheduler manual, 3 eventos via `Event::fake`) + **slice security 401/403 por
  rota do módulo** (**create**: sem token →401, Bolsista/Voluntário/Estagiário/Recrutando →403 em
  tudo) + M1–M4 sem regressão.
- **escopo:** módulo `financeiro` (+`Shared`, +regressão Auth/Rh/Estoque/Vendas) + peça Laravel `Test`.
- **entradas:** suíte Java (casos, não código) + F4–F11; DoD (`monolito-checklist.md`).
- **saidas:** `tests/Unit/Financeiro/*`, `tests/Feature/Financeiro/*` verdes; cobertura ≥ 80% (módulo); M1–M4 sem regressão.
- **criterios_de_conclusao:**
  - [ ] `php artisan test` verde (container); cobertura ≥ 80%
  - [ ] 401 sem token e 403 com role ≠ 0 em cada rota; `periodo` inválido →422; pré-condição de custeio ausente → erro orientativo
- **dependencias:** F4, F5, F6, F7, F8, F9, F10, F11.
- **prioridade:** P0.

## F13 — Gate DoD M5 (verificação final, sem código) — `create` (verificação)

- **descricao:** Conferir o DoD M5 (estende M0–M4, sem sobrescrever): estrutura MVC do `Financeiro`,
  schema próprio, fronteiras (só `Event`; sem `Contracts/`, sem leitura cross-schema), rotas
  `/api/financeiro/**` exatas, `php artisan test` verde + 80% + 401/403, código mínimo (cada `create`
  justificado no plan §M5.3). Sem código — relatório pass/fail.
- **escopo:** módulo `financeiro` + peça `verificação`.
- **entradas:** F1–F12 + `.opencode/state/monolito-checklist.md` (DoD).
- **saidas:** relatório de gate (pass/fail por item) + remoções BAIXA/MÉDIA se houver excesso.
- **criterios_de_conclusao:**
  - [ ] Código em `app/Modules/Financeiro/{Models,Http,Services,Policies,Events,Listeners}` (+`Enums/`, sem `Contracts/`, sem `Repository/`); `config/exceptions` só em `Shared`
  - [ ] Models `financeiro.*`; regra só em `Service`; migrations `database/migrations/financeiro/`; `migrate:fresh` verde; `DB_SEARCH_PATH` com `financeiro`
  - [ ] Zero Eureka/Gateway/HTTP entre módulos (`grep` limpo); eventos nativos com nomes preservados; sem join cross-schema; sem `PaymentProcessor`/`marcarVisualizada`/`cancelar`/bulk/export (`grep`)
  - [ ] Rotas `/api/financeiro/**` exatas (§M5.2); zero `?periodo=` em lançamentos; zero PUT em fechamento; `reuse > create`; `docs/` intocado; nada commitado
- **dependencias:** F12.
- **prioridade:** P0.

## Grafo de dependências M5 (resumo)

```
F1 ─→ F2 ─┬→ F3 ─→ F4 ─┐
           ├→ F3 ─→ F5 ─┤
           ├→ F3 ─→ F6 ─┤→ F10 ─┐
           ├→ F7 ──────┤→ F10 ──┤→ F12 ─→ F13
           ├→ F8 ──────┤→ F10 ──┤
           └→ F9 ──────┘→ F10 ──┘
F1 ─→ F11 ─→ F12   (F4 × F5 × F6 × F7 × F8 × F9 paralelizáveis após F2/F3)
```

---

# Tasks M6 — módulo `producao` (prefixo P)

> Destino: `backend/` Laravel (`App\Modules\Producao`, schema `producao`). Referência de regra (não código):
> `producao-service/src/**` + `specs/features/producao-service/requirements.md` (F1–F12) + `system/rbac-matrix.md`
> ∩ requirements §7 (prevalecem onde divergem do Java — ver plan §M6.1.4).
> Base pronta: `AuthContract` (M1), `RhContract` (M2), `EstoqueContract` (M3, `baixarConsumo` p/ P11),
> `VendasContract` (M4, `dadosEncomenda` p/ P11). Convenções: Eloquent sem `Repository`;
> `FormRequest` valida+autoriza; `Resource` dá o shape; regra só em `Service` (ou `Model` se trivial);
> `Policy`/`can:` p/ RBAC + atribuído/responsável; eventos `<ctx>.<ent>.<acao>.event` via `Event/Listener`
> (`ShouldQueue`, fila `database`); lock otimista via coluna `version` + 409; cobertura ≥ 80%.
> Legenda tipo: `reuse` = regra preservada em roupagem Laravel · `create` = justificado no `plan.md` §M6.3.
> **Nunca `docs/`; não commitar (Builder entrega em working tree).**

## P1 — Migrations `database/migrations/producao/` (schemas + V1–V18) + `DB_SEARCH_PATH` — `reuse` (+`modify`)

- **descricao:** Reescrever as migrations Flyway como migrations Laravel qualificando o schema `producao`,
  **sem alterar** tabelas/colunas/constraints/índices/UKs: V1 `projeto`, V2 `tarefa` (FK projeto),
  V3 `encomenda_kanban` (`id_encomenda` UNIQUE, `version` default 0), V4 `historico_kanban`,
  V5 `maquina`, V6 `historico_uso_maquina`, V7 `setor`, V8 `setor_material`, V9 `setor_sinalizacao`,
  V10 `setor_checklist`, V11 `setor_responsavel`, V12 `inspecao_5s`, V13 `item_inspecao_5s`,
  V14 `advertencia_membro` (`id_inspecao` nullable), V15 `projeto_mesa` (**`id_mesa` BIGINT** —
  drift §M6.1.1, SQL prevalece), V16 `auditoria_projeto_mesa`, V17 `parametro_5s` (+inserts, se houver),
  V18 `consumo_encomenda` (sem PK composta; índice `id_encomenda`). Primeira migration garante
  `CREATE SCHEMA IF NOT EXISTS producao` (idempotente, padrão M1). **Modify** aditivo: estender
  `DB_SEARCH_PATH` em `config/database.php` (`public,auth,rh,estoque,vendas,financeiro,producao`).
- **escopo:** módulo `producao` (+`Shared/config` aditivo) + peça Laravel `Migration`.
- **entradas:** `producao-service/src/main/resources/db/migration/V1–V18.sql`; M0–M5 (conexão `fablab`, migrator glob).
- **saidas:** `database/migrations/producao/*.php` com `Schema::create('producao.<tabela>')`; `DB_SEARCH_PATH` estendido.
- **criterios_de_conclusao:**
  - [ ] `php artisan migrate:fresh` em DB limpo verde; `migrate:status` sem pendência (com M1–M5 aplicados)
  - [ ] Constraints/UKs/FKs/índices conferem 1:1 com os `.sql` V1–V18 (deltas: só V15 `id_mesa` documentado)
- **dependencias:** M0+M1+M2+M3+M4+M5 (scaffold, conexão `fablab`, migrator com glob).
- **prioridade:** P0 (bloqueia P2–P17).

## P2 — Models Eloquent + enums (18 models, `producao.*`, sem Repository) — `reuse`

- **descricao:** Criar os 18 models (`Projeto`, `Tarefa`, `EncomendaKanban`, `HistoricoKanban`, `Maquina`,
  `HistoricoUsoMaquina`, `Setor`, `SetorMaterial`, `SetorSinalizacao`, `SetorChecklist`, `SetorResponsavel`,
  `Inspecao5S`, `ItemInspecao5S`, `AdvertenciaMembro`, `ProjetoMesa`, `AuditoriaProjetoMesa`, `Parametro5S`,
  `ConsumoEncomenda`) com `$table='producao.<tabela>'`, `$timestamps=false`, `$fillable` explícito,
  casts (decimal `horas_uso/quantidade`, date/datetime); enums PHP 8.1 `string backed` (`ProjetoStatus`:
  PLANEJADO/EM_ANDAMENTO/CONCLUIDO/CANCELADO, `TarefaStatus`: PENDENTE/EM_ANDAMENTO/CONCLUIDA/ATRASADA,
  `PrioridadeTarefa`: BAIXA/MEDIA/ALTA, `KanbanStatus`: FILA/PRODUCAO/ACABAMENTO/PRONTO/ENTREGUE,
  `MaquinaStatus`: DISPONIVEL/EM_USO/MANUTENCAO, `StatusInspecao`: OK/NAO_CONFORME, `TurnoInspecao`:
  MANHA/TARDE, `TipoAdvertencia`: VERBAL/FORMAL, `StatusProjetoMesa`: ATIVO/ABANDONADO/CONCLUIDO);
  `version` coluna simples (checagem manual em P11); `NivelAcesso`/`repositories` **não** recriados.
- **escopo:** módulo `producao` + peça Laravel `Model/Enum`.
- **entradas:** `producao-service/.../entity/*`; P1 (tabelas).
- **saidas:** `app/Modules/Producao/Models/*.php` + `Enums/`; zero `Repository/` no módulo.
- **criterios_de_conclusao:**
  - [ ] Models batem com P1 coluna a coluna (`id_encomenda` UNIQUE; `version` default 0; `id_mesa` BIGINT)
  - [ ] Mass assignment bloqueado (`$fillable` explícito); nenhum `Repository/`/`Mapper/` no módulo
- **dependencias:** P1.
- **prioridade:** P0.

## P3 — Policies/Gates RBAC (atribuído/responsável + deltas Admin) — `reuse`

- **descricao:** `Policies/ProducaoPolicy.php` (+ `can:`/`Gate`): leitura = qualquer autenticado
  **exceto Recrutando (4)** (matriz: `—` no módulo → 403); escrita por vínculo —
  `isResponsavelAtribuido($idResponsavel)` (Admin sempre true; demais só se `id == claim id_user`):
  projetos/tarefas (responsável do projeto; Estagiário só leitura se atribuído), kanban mover/remover
  (responsável do cartão), uso de máquina (B/V/E), inspeção (inspetor), projetos-mesa (dono);
  **Admin-only** (deltas §M6.1.4): cadastro/status de máquina, designação de responsáveis de setor
  (`POST /setores/{id}/responsaveis`), advertências, auditorias, parâmetros, `DELETE` inspeção;
  **inclusão no Kanban = Admin+Bolsista** (Voluntário `—` no requirements §7); criação de setor e
  checklist/materiais/sinalizações = Admin **ou** B/V **com vínculo** de responsável ativo do setor
  (não-Admin sem vínculo → 403; aprovação = ação do Admin, sem workflow novo). JWT via guard `api`
  do Auth (M1) + `blacklist`.
- **escopo:** módulo `producao` (+`Shared` se transversal) + peça Laravel `Policy/Gate`.
- **entradas:** `@PreAuthorize` dos 10 controllers + requirements §7 + `system/rbac-matrix.md` (regras 1–5:
  só Admin aprova advertências; Recrutando fora do 5S); P2; `AuthContract::isAdmin`.
- **saidas:** `app/Modules/Producao/Policies/ProducaoPolicy.php` (+ Gates/`can:`) aplicados nas rotas P4–P9.
- **criterios_de_conclusao:**
  - [ ] 401 sem token em tudo; Recrutando →403 em tudo; B/V sem vínculo escrevendo →403 (coberto em P16)
  - [ ] Admin-only devolve 403 p/ B/V/E em advertências/auditorias/parâmetros/designação (teste em P16)
- **dependencias:** P2.
- **prioridade:** P0.

## P4 — Controllers projetos/tarefas + Requests/Resources + rotas PT (+`PATCH` vínculo) — `reuse` (+1 `create`)

- **descricao:** Controllers **só-HTTP**: `ProjetoController` (`POST/GET /api/producao/projetos`
  `?status=&idResponsavel=`, `GET /{id}`, `PUT /{id}`, `PUT /{id}/status`, `DELETE /{id}`),
  `TarefaController` (`POST`, `GET /api/producao/tarefas` `?idProjeto=&idResponsavel=&status=`,
  `GET /{id}`, `PUT /{id}`, `PUT /{id}/status`, `DELETE /{id}`, **`PATCH /{id}`** —
  **create** justificado: conclui a própria tarefa (`id_responsavel` = auth ou Admin, senão 403;
  idempotente; compat M8/`PATCH /api/tasks/{id}`)). Sem regra, sem mapper.
- **escopo:** módulo `producao` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `ProjetoController`, `TarefaController` Java (paths/verbos) + DTOs (shapes + Bean
  Validation: nome/título obrigatórios, datas, enums); P2 (models+enums), P3 (proteção).
- **saidas:** `Http/Controllers/{Projeto,Tarefa}Controller.php`, `Http/Requests/*`,
  `Http/Resources/*`, trechos de `routes.php` (`/api/producao/projetos|tarefas`).
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas (verbo+path+queries; `PATCH /tarefas/{id}` presente); controllers sem cálculo/regra (grep em P17)
  - [ ] Validação rejeita 400/422 antes do service; `PATCH` por não-responsável não-Admin →403 (teste em P16)
- **dependencias:** P2, P3.
- **prioridade:** P0.

## P5 — Controllers kanban/consumo + Requests/Resources + rotas PT — `reuse` (+1 `create`)

- **descricao:** Controllers **só-HTTP**: `KanbanController` (`POST /api/producao/kanban` —
  Admin+Bolsista, `GET` `?status=`, `GET /encomenda/{idEncomenda}`, `GET
  /encomenda/{idEncomenda}/historico`, `PUT /{id}/mover` com `version`, `DELETE /{id}`),
  `ConsumoController` (**create** justificado — F11: `POST /api/producao/kanban/encomenda/{idEncomenda}/consumo`
  upsert `[{idItem,quantidade}]`, `GET` lista o acumulado). Sem regra, sem mapper.
- **escopo:** módulo `producao` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `KanbanController` Java (paths/verbos/`@PreAuthorize`) + DTOs (kanban+`version`,
  histórico, consumo); P2, P3.
- **saidas:** `Http/Controllers/{Kanban,Consumo}Controller.php`, `Requests/*`, `Resources/*`, trechos de `routes.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M6.2; `version` ausente no mover → 409 (service P11); validação rejeita 400/422 antes do service
  - [ ] `POST /kanban` por Voluntário →403 (delta P3, teste em P16)
- **dependencias:** P2, P3.
- **prioridade:** P0.

## P6 — Controllers máquinas + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `MaquinaController` (`POST /api/producao/maquinas` —
  Admin, `GET` `?status=` + `GET /{id}` + `GET /{id}/historico` (leitura geral exceto Recrutando),
  `PUT /{id}` + `PUT /{id}/status` — Admin, `POST /{id}/uso`, `PUT /{id}/uso/{idUso}/fim`,
  `DELETE /{id}` — Admin). Sem regra, sem mapper.
- **escopo:** módulo `producao` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `MaquinaController` Java (paths/verbos) + DTOs (cadastro, status, uso-início/fim, histórico); P2, P3.
- **saidas:** `Http/Controllers/MaquinaController.php`, `Requests/*`, `Resources/*`, trechos de `routes.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M6.2; cadastro por B/V →403 (delta P3, teste em P16)
  - [ ] `fim < inicio` →422; uso de máquina inexistente →404 (service P12)
- **dependencias:** P2, P3.
- **prioridade:** P0.

## P7 — Controllers setores (CRUD + materiais/sinalizações/checklist/responsáveis) + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `SetorController` (`POST/GET /api/producao/setores`
  `?ativo=`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `POST|DELETE /{id}/materiais[/{idMaterial}]`,
  `POST|DELETE /{id}/sinalizacoes[/{idSinalizacao}]`, `POST /{id}/checklist`,
  `PUT|DELETE /{id}/checklist/{idItem}` — **edita só `item`+`ativo`, sem peso/reordenação**,
  `POST /{id}/responsaveis` — Admin, `GET /{id}/responsaveis`,
  `DELETE /{id}/responsaveis/{idResponsavel}`). Escrita = Admin ou B/V com vínculo de responsável
  ativo do setor (P3). Sem regra, sem mapper.
- **escopo:** módulo `producao` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `SetorController` Java (paths/verbos) + DTOs (setor, material, sinalização, checklist,
  responsável); P2, P3.
- **saidas:** `Http/Controllers/SetorController.php`, `Requests/*`, `Resources/*`, trechos de `routes.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M6.2 (13 endpoints); designação por B/V →403 (delta P3, teste em P16)
  - [ ] Checklist sem campo peso/ordem no Request (grep em P17); validação rejeita 400/422 antes do service
- **dependencias:** P2, P3.
- **prioridade:** P0.

## P8 — Controllers inspeção/advertência + Requests/Resources + rotas PT — `reuse`

- **descricao:** Controllers **só-HTTP**: `Inspecao5SController` (`POST /api/producao/inspecoes-5s`
  — Admin/B/V inspetor via `podeEditar(idInspetor)`, `GET` `?idSetor=`, `GET /{id}`,
  `DELETE /{id}` — Admin), `AdvertenciaController` (`POST /api/producao/advertencias` — Admin,
  `GET`, `GET /{idFuncionario}` — próprias p/ não-Admin, tudo p/ Admin, server-side). **Status da
  inspeção nunca entra pelo payload** (recalculado em P13 — sem campo `status` no Request).
  Sem regra, sem mapper.
- **escopo:** módulo `producao` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `Inspecao5SController`, `AdvertenciaController` Java (paths/verbos/`@PreAuthorize`) +
  DTOs (inspeção+itens, advertência); P2, P3.
- **saidas:** `Http/Controllers/{Inspecao5S,Advertencia}Controller.php`, `Requests/*` (sem `status`
  em inspeção), `Resources/*`, trechos de `routes.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M6.2; `POST /advertencias` por B/V →403 (teste em P16)
  - [ ] Não-Admin lista só advertências próprias (teste em P16); inspeção com item de outro setor →422 (service P13)
- **dependencias:** P2, P3.
- **prioridade:** P0.

## P9 — Controllers mesa/auditoria/parâmetros (+QR PNG) + rotas PT — `reuse` (+1 `create`)

- **descricao:** Controllers **só-HTTP**: `ProjetoMesaController` (`POST/GET
  /api/producao/projetos-mesa` `?status=`, `GET /{id}`, `PUT /{id}`, `PUT /{id}/evolucao`,
  `GET /{id}/qrcode` — `image/png`, `GET /{id}/auditorias`, `DELETE /{id}`),
  `AuditoriaProjetoMesaController` (`POST /api/producao/auditorias-projeto-mesa` — Admin,
  `GET /{idProjetoMesa}`), `Parametro5SController` (`GET /api/producao/parametros-5s`,
  `GET /{id}`, `PUT /{id}` — Admin; regra trivial **no Model/Eloquent, sem service**).
  QR PNG via `chillerlan/php-qrcode` (**create** justificado — única dep nova, contrato PNG;
  conteúdo `fablab://projeto-mesa/{id}`). `composer require chillerlan/php-qrcode` registrado.
  Sem regra nos controllers, sem mapper.
- **escopo:** módulo `producao` + peça Laravel `Controller/FormRequest/Resource/route` (+1 dep composer).
- **entradas:** `ProjetoMesaController`, `AuditoriaProjetoMesaController`, `Parametro5SController`
  Java (paths/verbos) + DTOs; P2, P3.
- **saidas:** `Http/Controllers/{ProjetoMesa,AuditoriaProjetoMesa,Parametro5S}Controller.php`,
  `Requests/*`, `Resources/*`, `routes.php` do módulo + registro em `routes/api.php`, dep no `composer.json`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M6.2; `GET /{id}/qrcode` devolve `image/png` escaneável do conteúdo
  - [ ] Auditoria/parâmetros-`PUT` por B/V →403 (teste em P16); zero `Services/Parametro5SService.php` (grep em P17)
- **dependencias:** P2, P3.
- **prioridade:** P0.

## P10 — Services projeto/tarefa (vínculos, carimbos, `PATCH` próprio) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction`): `ProjetoService::criar` (só se `id_responsavel`
  = auth ou Admin, senão 403; nasce sem `data_fim_real`) + `atualizar`/`alterarStatus`/`remover`
  (responsável do projeto ou Admin; `CONCLUIDO` carimba `data_fim_real` default hoje);
  `TarefaService::criar/atualizar/alterarStatus/remover` (responsável do **projeto** ou Admin;
  nasce `PENDENTE`; `CONCLUIDA` carimba `data_conclusao`) + **`concluirPropria`** (P4: vínculo da
  **tarefa** — `id_responsavel` = auth ou Admin, senão 403; idempotente).
- **escopo:** módulo `producao` + peça Laravel `Service`.
- **entradas:** `service/{Projeto,Tarefa}Service` + requirements F1–F2; P2; P3 (vínculos).
- **saidas:** `app/Modules/Producao/Services/{Projeto,Tarefa}Service.php`, regra testável sem HTTP.
- **criterios_de_conclusao:**
  - [ ] Projeto criado p/ outro responsável por não-Admin →403; tarefa de projeto alheio →403
  - [ ] `PATCH` em tarefa alheia por não-Admin →403; `PATCH` repetido = no-op 200 (teste em P16)
- **dependencias:** P2 (P14 p/ nenhum evento aqui — projetos/tarefas não publicam).
- **prioridade:** P0.

## P11 — Service kanban/consumo (LINKA Vendas, `version`, histórico, 3 eventos, baixa) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction`): `incluir` (valida
  `VendasContract::dadosEncomenda` — 404 se inexistente; duplicado →409; nasce `FILA`+`ordem 0`+
  histórico inicial); `mover` (responsável do cartão ou Admin senão 403; `version` divergente →409;
  atualiza `status`/`data_entrada_status`; grava histórico com `id_usuario` = auth; publica
  `producao.status.alterado.event` + `kanban.status.alterado.event` via P14); primeira transição a
  `ENTREGUE` → `publicarConclusao` (`producao.concluida.event` com itens do `consumo_encomenda` +
  baixa via `EstoqueContract::baixarConsumo(itens, id_encomenda)` — **só-leitura/aditivo em M4/M3,
  sem quebrar**); `registrarConsumo` (upsert `(id_encomenda,id_item)` soma quantidades; item
  inexistente no Estoque →422 via `itemExiste`); `remover`; `historico`. `idProdutoServico` = null
  (como no Java).
- **escopo:** módulo `producao` + peça Laravel `Service`.
- **entradas:** `service/KanbanService` + requirements F3/F11; P2; `VendasContract`, `EstoqueContract`.
- **saidas:** `app/Modules/Producao/Services/KanbanService.php` (consumo dobrado aqui, sem service extra).
- **criterios_de_conclusao:**
  - [ ] Incluir encomenda inexistente no Vendas →404; duplicada →409; mover com `version` antiga →409 sem mover
  - [ ] `ENTREGUE` publica `producao.concluida` + baixa exata 1×; 2º mover→`ENTREGUE` não rebaixa (teste em P16)
- **dependencias:** P2, P3 (P14 p/ emissão).
- **prioridade:** P0.

## P12 — Services máquina/setor (uso+`horas_uso`, rotação, checklist mínimo) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction`): `MaquinaService`: criar/atualizar/status/remover
  (Admin — P3); `iniciarUso` (exige `DISPONIVEL`, senão 409; `MANUTENCAO` →409; →`EM_USO`);
  `encerrarUso` (uso pertence à máquina senão 422; `fim ≥ inicio` senão 422; `horas_uso` =
  decimal horas; →`DISPONIVEL`); `historico`. `SetorService`: CRUD+detalhe; materiais/sinalizações
  add/remove; checklist add/**update-só-`item`+`ativo`**/remove; `adicionarResponsavel` (Admin —
  P3; ativo novo desativa o anterior — rotação) + `removerResponsavel` + `listarResponsaveis`.
- **escopo:** módulo `producao` + peça Laravel `Service`.
- **entradas:** `service/{Maquina,Setor}Service` + requirements F4–F5; P2.
- **saidas:** `app/Modules/Producao/Services/{Maquina,Setor}Service.php`.
- **criterios_de_conclusao:**
  - [ ] Uso em máquina `EM_USO`/`MANUTENCAO` →409; `fim < inicio` →422 sem mutar; `horas_uso` bate o cálculo
  - [ ] Novo responsável ativo desativa o anterior; checklist sem peso/ordem (grep em P17)
- **dependencias:** P2, P3.
- **prioridade:** P0.

## P13 — Services inspeção/advertência/mesa (status server-side, contador, QR, auditoria) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction`): `Inspecao5SService::registrar` (itens só do
  checklist **do setor**, senão 422; **`recalcularStatus`** ignora payload; `NAO_CONFORME` →
  `AdvertenciaService::registrarAutomatica` VERBAL ao responsável ativo); `AdvertenciaService`:
  `contador = count+1`, `id_admin_registrou` = auth; publica `advertencia.registrada.event` sempre +
  `advertencia.limite.atingido.event` se `≥3`; **período experimental** (`parametro_5s`
  `periodo_experimental_ativo=true`): auto-advertência não incrementa penalidade (F10);
  `ProjetoMesaService`: criar (`ATIVO`, `data_inicio`/`ultima_evolucao` = hoje,
  `qr_code_totem` = `fablab://projeto-mesa/{id}` pós-insert) + atualizar + `registrarEvolucao`
  (reativa `ABANDONADO→ATIVO`) + remover + `conteudoQrCode` (P9) + `auditar` (Admin — P3: grava
  auditoria, espelha `status`, publica `projeto.mesa.abandonado.event` só se `ABANDONADO`) +
  `projetosSemEvolucao` (leitura p/ P15).
- **escopo:** módulo `producao` + peça Laravel `Service`.
- **entradas:** `service/{Inspecao5S,Advertencia,ProjetoMesa,Parametro5S}Service` + F6–F10; P2.
- **saidas:** `app/Modules/Producao/Services/{Inspecao5S,Advertencia,ProjetoMesa}Service.php`.
- **criterios_de_conclusao:**
  - [ ] Inspeção toda-conforme →`OK` mesmo com payload divergente; item estranho →422 sem persistir
  - [ ] 3ª advertência publica os 2 eventos; experimental-on não conta p/ limite (teste em P16); auditoria `ATIVO` não publica abandono
- **dependencias:** P2, P3 (P14 p/ emissão; P9 p/ QR).
- **prioridade:** P0.

## P14 — Events/Listeners + `ProducaoContract` (6 emitidos, 2 consumidos, produtor real) — `reuse` (+1 `create`)

- **descricao:** Emissão via `Event`s nativos (`ShouldQueue`, fila `database`, **nomes+payloads
  preservados**, dispatch fail-soft nunca 500): `producao.status.alterado.event`
  (`idEncomenda,statusNovo,idUsuario,observacao`), `producao.concluida.event`
  (`idEncomenda,idProdutoServico=null,itens[{idItem,quantidadeConsumida}],dataConclusao`) —
  **produtores reais aguardados por M4/V10 (`ProducaoStatusListener`), M5/F10 (custeio) e M3/E9
  (baixa)** —, `kanban.status.alterado.event` (`idEncomenda,statusAnterior,statusNovo,dataAlteracao`),
  `advertencia.registrada.event` + `advertencia.limite.atingido.event`
  (`idFuncionario,contador,motivo`), `projeto.mesa.abandonado.event`
  (`idProjetoMesa,idFuncionario,acaoTomada`). Consumidores: `Listeners/EncomendaCriadaListener.php`
  (`encomenda.criada.event` → auto-insert `FILA`, idempotente) + `Listeners/NivelAlteradoListener.php`
  (`nivel.alterado.event` → `Log::info`, sem persistência). **Sem** listener p/
  `estoque.consumo.realizado.event` (§M6.3/R6). Criar **`Contracts/ProducaoContract.php`**
  (**create** justificado, **congelado em 2 métodos**: `resumoKanban(): array` (contagem por status),
  `tarefasAtivasPorResponsavel(int): array` p/ M8/Dashboard; só o consumido).
- **escopo:** módulo `producao` (+`Shared/Events` se transversal) + peça Laravel `Event/Listener/Contract`.
- **entradas:** `dto/*Event` + `ProducaoEventPublisher` (nomes/payloads/fail-soft) + 3 consumers; P10–P13.
- **saidas:** `Events/*.php` (6), `Listeners/{EncomendaCriada,NivelAlterado}Listener.php`,
  `Contracts/ProducaoContract.php` (+ implementação Eloquent do próprio schema).
- **criterios_de_conclusao:**
  - [ ] 6 nomes + payloads inalterados (`Event::fake` em P16); `encomenda.criada` duplicada = 1 cartão; nula = ignore
  - [ ] Nenhum módulo importa `Service/Model` do Producao — só `Contract`/`Event` (grep em P17); M3/M4/M5 sem regressão
- **dependencias:** P10, P11, P12, P13.
- **prioridade:** P1.

## P15 — Scheduler auditoria de mesas (`dailyAt 06:00`) + config + seeders (verificação) — `reuse` (+1 `create`)

- **descricao:** `ProjetoMesaService::projetosSemEvolucao(limite)` executável manualmente (lista de ids);
  agendar em `routes/console.php` com `Schedule::call(...)->dailyAt('06:00')`
  (horário **preservado** do Java `0 0 6 * * *`, não o padrão 03:00) + `config/producao.php`
  (**create** justificado, padrão M3/E10: `auditoria_scheduler_enabled`, default on — equivale a
  `producao.auditoria.scheduler.enabled`; `diasParaAuditoriaProjeto` lido de `parametro_5s`,
  default 15). Seeders: conferir se V17 traz inserts de `parametro_5s` — se sim, viram seeder
  idempotente; senão, **nenhuma seeder inventada** (padrão M2–M5).
- **escopo:** módulo `producao` + peça Laravel `Schedule/config/Seeder` (se houver).
- **entradas:** `service/ProjetoMesaAuditoriaScheduler` + requirements F12/NFR; P13 (regra), P14 (sem evento aqui — só `warn`).
- **saidas:** entrada em `routes/console.php`, `config/producao.php` (+ seeder dev-only se V17 exigir).
- **criterios_de_conclusao:**
  - [ ] Chamada manual lista só `ATIVO` sem evolução além do limite (zero p/ recentes/concluídos/abandonados)
  - [ ] Agendamento registrado `06:00`; flag desliga sem erro; nenhum dado inventado sem justificativa
- **dependencias:** P13, P14 (P1 p/ seeders).
- **prioridade:** P1.

## P16 — Testes Unit/Feature (401/403, vínculo, lock, idempotência, 80%) — `reuse` (+1 `create`)

- **descricao:** Cobrir M6 em PHPUnit/Pest via `php artisan test` **em container Docker** (pgsql
  `fablab_testing`, com `pcov`): unit (carimbos `data_fim_real`/`data_conclusao`, `recalcularStatus`,
  contador/limite 3, experimental on/off, `horas_uso`, rotação desativa-anterior, QR conteúdo,
  `version`/conflito, upsert consumo, `projetosSemEvolucao`) + feature (CRUD projetos/tarefas,
  `PATCH` próprio, kanban incluir→mover→histórico→2 eventos→`ENTREGUE`→conclusão+baixa, consumo
  upsert→baixa, máquinas uso abre/fecha, setores+checklist+responsáveis, inspeção→auto-advertência,
  advertir→eventos, mesa→evolução→auditoria→evento, parâmetros, QR PNG, 2 Listeners via fila
  `database`, `ProducaoContract` 2 métodos, scheduler manual, 6 eventos via `Event::fake`) +
  **slice security 401/403 por rota + vínculo** (**create**: sem token →401; Recrutando →403 em
  tudo; `PATCH` alheio →403; mover não-responsável →403; advertência/auditoria/designação por B/V
  →403; Voluntário incluindo kanban →403; Estagiário escrevendo →403) + M1–M5 sem regressão.
- **escopo:** módulo `producao` (+`Shared`, +regressão Auth/Rh/Estoque/Vendas/Financeiro) + peça Laravel `Test`.
- **entradas:** suíte Java (casos, não código) + P4–P15; DoD (`monolito-checklist.md`).
- **saidas:** `tests/Unit/Producao/*`, `tests/Feature/Producao/*` verdes; cobertura ≥ 80% (módulo); M1–M5 sem regressão.
- **criterios_de_conclusao:**
  - [ ] `php artisan test` verde (container); cobertura ≥ 80%
  - [ ] 401 sem token e 403 sem papel/vínculo em cada rota sensível; lock duplo →200+409; duplo `ENTREGUE` = 1 baixa
- **dependencias:** P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15.
- **prioridade:** P0.

## P17 — Gate DoD M6 (verificação final, sem código) — `create` (verificação)

- **descricao:** Conferir o DoD M6 (estende M0–M5, sem sobrescrever): estrutura MVC do `Producao`,
  schema próprio, fronteiras (`Contract`/`Event` só; `Vendas/EstoqueContract` só-leitura sem quebra),
  rotas `/api/producao/**` exatas, `php artisan test` verde + 80% + 401/403 + vínculo, código mínimo
  (cada `create` justificado no plan §M6.3). Sem código — relatório pass/fail.
- **escopo:** módulo `producao` + peça `verificação`.
- **entradas:** P1–P16 + `.opencode/state/monolito-checklist.md` (DoD).
- **saidas:** relatório de gate (pass/fail por item) + remoções BAIXA/MÉDIA se houver excesso.
- **criterios_de_conclusao:**
  - [ ] Código em `app/Modules/Producao/{Models,Http,Services,Policies,Events,Listeners}` (+`Contracts/` só
    consumido, +`Enums/`); `config/exceptions` só em `Shared`
  - [ ] Models `producao.*`; regra só em `Service` (ou `Model` se trivial); migrations `database/migrations/producao/`; `migrate:fresh` verde; `DB_SEARCH_PATH` com `producao`
  - [ ] Zero Eureka/Gateway/HTTP entre módulos (`grep` limpo); eventos nativos com nomes preservados; sem join cross-schema; sem `Repository`/`Mapper`/`Parametro5SService`/peso-checklist (`grep`)
  - [ ] Rotas `/api/producao/**` exatas (§M6.2); `PATCH /tarefas/{id}` e consumo presentes; `reuse > create`; `docs/` intocado; nada commitado
- **dependencias:** P16.
- **prioridade:** P0.

## Grafo de dependências M6 (resumo)

```
P1 ─→ P2 ─┬→ P3 ─→ P4 ─┐
           ├→ P3 ─→ P5 ─┤
           ├→ P3 ─→ P6 ─┤→ P14 ─┐
           ├→ P3 ─→ P7 ─┤→ P14 ─┤→ P16 ─→ P17
           ├→ P3 ─→ P8 ─┤→ P14 ─┤
           ├→ P3 ─→ P9 ─┤→ P14 ─┘
           ├→ P10 ─────┤→ P14 ──┘
           ├→ P11 ─────┤→ P14
           ├→ P12 ─────┤→ P14
           └→ P13 ─────┘→ P14 ─→ P15 ─→ P16
P1 ─→ P15 ─→ P16   (P4 × … × P13 paralelizáveis após P2/P3)
```

---

# Tasks M7 — módulo `notification` (prefixo N, CONSUMIDOR central)

> Destino: `backend/` Laravel (`App\Modules\Notification`, schema `notification`). Referência de regra
> (não código): `notification-service/src/**` (controller+7 DTOs+entity+repository+3 services+config,
> D-1..D-7) + `docs/_archive/2026-10-19/07-Notification_Service.md` (canônico de domínio — lifecycle, histórico, canais,
> revisão, expurgo 90d) + `system/architecture.md` §Persistência (V1–V4) + `system/rbac-matrix.md`
> (inbox 0–4, resto Admin). Base pronta: eventos+contracts de Auth/Rh/Estoque/Vendas/Financeiro/
> Produção (M7 só consome; sem produtor próprio). Convenções: Eloquent sem `Repository`;
> `FormRequest` valida+autoriza; `Resource` dá o shape (**keys EN 1:1** — é o que o front D-1 parseia;
> só paths viram PT); regra só em `Service`; `Policy`/`can:admin` p/ admin; eventos
> `<ctx>.<ent>.<acao>.event` via `Event/Listener` (`ShouldQueue`, fila `database`); schedulers
> `everyThirtyMinutes` + `dailyAt('03:00')` + flags; cobertura ≥ 80%.
> Legenda tipo: `reuse` = regra preservada em roupagem Laravel · `create` = justificado no `plan.md` §M7.3.
> **Nunca `docs/`; não commitar (Builder entrega em working tree).**

## N1 — Migrations `database/migrations/notification/` (V1–V4 reconciliadas) + `DB_SEARCH_PATH` — `reuse` (+2 `create`, +1 `modify`)

- **descricao:** Reescrever V1–V2 Flyway como migrations Laravel qualificando o schema `notification`
  (**reconciliação §M7.1 D2–D4**): V1 `notificacao` = Java V1 1:1 (`id`, `id_usuario` NOT NULL,
  `titulo` 160 NOT NULL, `mensagem` TEXT, `lida` bool default false, `criada_em` timestamptz NOT NULL,
  índice `(id_usuario,lida)`) **+ lifecycle `docs/_archive/2026-10-19/07` §4** (`status` VARCHAR(20) NOT NULL default
  `PENDENTE` CHECK `PENDENTE/ENVIADA`, `data_envio`/`data_leitura` timestamptz nullable,
  `id_referencia` BIGINT nullable); V2 = Java V2 1:1 (`tipo` 40, `canal` 20, `link` 2000, índice
  `(id_usuario,tipo)`) **+ `create notificacao_historico`** (espelho + `data_revisao_admin`
  NOT NULL + `id_admin_revisor` NOT NULL, índice `data_revisao_admin` p/ expurgo); V3 =
  `configuracao_canal` (`id`, `canal` VARCHAR(20) UK, `habilitado` bool, `parametros` JSON nullable)
  **+ `create preferencia_notificacao`** (`id`, `tipo` 40, `canal` 20, `habilitado` bool default true,
  UK `(tipo,canal)`); V4 = seeds idempotentes por upsert (**dentro da migration**, padrão Auth V6/V7:
  `EMAIL habilitado=true/parametros={}`, `WHATSAPP habilitado=false/parametros={}` + matriz default
  Java tudo-true exceto `sistema/push=false`). Primeira migration garante
  `CREATE SCHEMA IF NOT EXISTS notification` (idempotente, padrão M1). **Modify** aditivo: estender
  `DB_SEARCH_PATH` em `config/database.php` (`…,producao,notification`).
- **escopo:** módulo `notification` (+`Shared/config` aditivo) + peça Laravel `Migration`.
- **entradas:** `notification-service/.../db/migration/V1–V2.sql`; `docs/_archive/2026-10-19/07` §4 (colunas lifecycle/
  histórico/canais); `system/architecture.md` §Persistência (V1–V4); M0–M6 (conexão `fablab`, migrator glob).
- **saidas:** `database/migrations/notification/*.php` com `Schema::create('notification.<tabela>')`; `DB_SEARCH_PATH` estendido.
- **criterios_de_conclusao:**
  - [ ] `php artisan migrate:fresh` em DB limpo verde; `migrate:status` sem pendência (com M1–M6 aplicados)
  - [ ] Colunas Java V1–V2 conferem 1:1 com os `.sql` (deltas: só os 4 lifecycle + historico + 2 tabelas V3, todos documentados em §M7.1 D2–D4)
  - [ ] V4 roda 2× sem duplicar (upsert por UK)
- **dependencias:** M0+M1+M2+M3+M4+M5+M6 (scaffold, conexão `fablab`, migrator com glob).
- **prioridade:** P0 (bloqueia N2–N12).

## N2 — Models Eloquent + enum + `Support/LinkValidator` (4 models, `notification.*`, sem Repository) — `reuse` (+1 `create`)

- **descricao:** Criar os 4 models (`Notificacao`, `NotificacaoHistorico`, `ConfiguracaoCanal`,
  `PreferenciaNotificacao`) com `$table='notification.<tabela>'`, `$timestamps=false`, `$fillable`
  explícito, casts (`lida/habilitado` bool, `criada_em/data_*` datetime, `parametros` array);
  enum PHP 8.1 `string backed StatusEntrega: PENDENTE/ENVIADA` (**create** justificado — CHECK de
  entrega sem equivalente nativo); `TipoNotificacao`/`CanalNotificacao` como **consts allowlist no
  service** (Java usa `Set.of` + `in:` — sem enum dedicado, sem CHECK no banco); `LinkValidator`
  **reuse 1:1** (`Support/LinkValidator.php`, static final: null/blank ok; `/...` sem espaço/`\\`;
  absoluto só `http(s)` com host — resto 422); `repositories` **não** recriados.
- **escopo:** módulo `notification` + peça Laravel `Model/Enum/Support`.
- **entradas:** `entity/Notificacao.java` + `service/LinkValidator.java`; N1 (tabelas).
- **saidas:** `app/Modules/Notification/Models/*.php` + `Enums/StatusEntrega.php` + `Support/LinkValidator.php`; zero `Repository/` no módulo.
- **criterios_de_conclusao:**
  - [ ] Models batem com N1 coluna a coluna (`status` default PENDENTE; UKs `(tipo,canal)` e `canal`)
  - [ ] Mass assignment bloqueado (`$fillable` explícito); `javascript:/data:` rejeitado pelo validator
- **dependencias:** N1.
- **prioridade:** P0.

## N3 — Policy + `NotificationPrincipal` RBAC (inbox 0–4, resto Admin) — `reuse`

- **descricao:** `Policies/NotificationPolicy.php` (+ `can:admin` nas rotas N5): inbox
  (listar/contar/ler/ler-todas) = **qualquer autenticado incl. Recrutando (4)** (matriz:
  `Minhas notificações ✅` p/ 0–4 — sem `exigeLeitura` anti-Recrutando, delta consciente vs outros
  módulos); isolamento **server-side**: inbox filtra sempre por `id_user` do guard (nunca por param);
  `marcarComoLida`: dono **ou** Admin (bypass Java 1:1); preferences/history/revisar/config =
  **Admin-only** (matriz `Revisar/Histórico + Configuração de Canal = Editar/Admin`). Criar
  `NotificationPrincipal` (`idUser/role/admin`, de `auth()->user()` — ex-`AuthPrincipal` some; JWT é o
  guard `api` do Auth/M1 + `blacklist`).
- **escopo:** módulo `notification` + peça Laravel `Policy/Principal`.
- **entradas:** `@PreAuthorize hasRole('ADMIN')` (preferences/history) + `system/rbac-matrix.md`
  (regras 1–4 + linha Notification); N2.
- **saidas:** `app/Modules/Notification/Policies/NotificationPolicy.php` + `NotificationPrincipal.php` aplicados nas rotas N4/N5.
- **criterios_de_conclusao:**
  - [ ] Inbox sem token →401; Recrutando lê as **próprias** (200) — coberto em N11
  - [ ] preferences/history/revisar/config com B/V/E →403 (coberto em N11)
- **dependencias:** N2.
- **prioridade:** P0.

## N4 — Controllers inbox (lista/contagem/ler/ler-todas) + Requests/Resources + rotas PT + alias — `reuse` (+1 `create`)

- **descricao:** Controllers **só-HTTP** (validam via `FormRequest`, delegam ao service, devolvem
  `Resource`): `NotificacaoController` (`GET /api/notification/notificacoes` — `?page=` 1-based
  default 1 + `?size=`/`?pageSize=` default 10, 1..100 + `?type=` + `?read=`; `GET .../nao-lidas/contagem`
  — sino/badge `{count}`; `PATCH .../{id}/ler`; `POST .../ler-todas` → `{lidas}`), saída
  **keys EN 1:1** (`id/type/title/body/read/createdAt/link/channel`; paginada
  `items/total/page/size/pageSize/totalPages`; validação rejeita 400/422 antes do service; `page<1`/
  `size` fora →400). **Create**: alias `GET /api/notificacoes` e
  `GET /api/notificacoes/nao-lidas/contagem` (2 linhas, mesmos controllers — App Shell/sino).
  Sem regra, sem mapper.
- **escopo:** módulo `notification` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `NotificacaoController.java` (paths/verbos) + DTOs (shapes D-1) + `system/conventions.md`
  (recurso PT); N2 (models+validator), N3 (isolamento).
- **saidas:** `Http/Controllers/NotificacaoController.php`, `Http/Requests/*`, `Http/Resources/*`,
  trechos de `routes.php` (`/api/notification/**` + alias `/api/notificacoes`), registro em `routes/api.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M7.2 (verbo+path+queries; `PATCH` no ler — grep anti-`PUT /{id}/ler` em N12)
  - [ ] Keys JSON EN 1:1 com D-1; controllers sem cálculo/regra (grep em N12)
- **dependencias:** N2, N3.
- **prioridade:** P0.

## N5 — Controllers admin (preferencias/historico/revisar/config-canais) + Requests/Resources + rotas PT — `reuse` (+2 `create`)

- **descricao:** Controllers **só-HTTP** (`can:admin` em tudo): `PreferenciaController`
  (`GET|PUT /api/notification/preferencias` — matriz `tipo×canal` validada estrita Java 1:1:
  tipo em TIPOS, canal em CANAIS, valor bool não-nulo, matriz não-vazia); `HistoricoController`
  (`GET /api/notification/historico` — `?search=&type=&read=&channel=&period=`, retorna
  `{items,total}` **integral** — front pagina client-side, D-4/Java 1:1); `POST
  /api/notification/notificacoes/revisar` (**create** D9 — `{ids[]}` → move p/ histórico com
  `data_revisao_admin`+`id_admin_revisor`); `ConfiguracaoCanalController` (**create** D9 —
  `GET|PUT /api/notification/configuracao-canais[/{canal}]`, `{habilitado, parametros?}`).
  Sem regra, sem mapper.
- **escopo:** módulo `notification` + peça Laravel `Controller/FormRequest/Resource/route`.
- **entradas:** `NotificacaoController.java` (preferences/history) + `docs/_archive/2026-10-19/07` §6 (revisar/config);
  N2, N3.
- **saidas:** `Http/Controllers/{Preferencia,Historico,Revisao,ConfiguracaoCanal}Controller.php`
  (ou ações dobradas — sem duplicar controller+action), `Requests/*`, `Resources/*`, `routes.php`.
- **criterios_de_conclusao:**
  - [ ] Rotas PT exatas do plan §M7.2; matriz inválida (`tipo: x`, `canal: zap`, valor null) →400
  - [ ] History sem paginação server-side (grep anti-`paginate` no historico em N12)
- **dependencias:** N2, N3.
- **prioridade:** P0.

## N6 — `NotificacaoService` inbox+prefs+history+revisar (1:1 Java) — `reuse`

- **descricao:** Regras (só aqui, `DB::transaction` onde muta): `contarNaoLidas(idUsuario)`
  (`where lida=false`); `listar(idUsuario,page,size,tipo,lida)` (sempre escopada ao dono +
  `order criada_em DESC`; `paraResponse` EN 1:1); `marcarComoLida(id,idUsuario,admin)` (ausente→404;
  alheia sem admin→403; carimba `lida`+`data_leitura`); `marcarTodasComoLidas` (update em massa,
  retorna contagem); `obter/salvarPreferencias` (validação estrita + cópia defensiva);
  `historico(search,tipo,lida,canal,periodo)` (`LIKE` título/mensagem + corte `24h/30d`);
  `registrar(idUsuario,tipo,canal,titulo,mensagem,link[,idReferencia,status])` (título obrigatório+strip;
  `LinkValidator::assertValido` →422); `revisar(ids,admin)` (move→histórico, remove do ativo).
  `normalizar`/`resolverPeriodo`/`preferenciasPadrao` 1:1.
- **escopo:** módulo `notification` + peça Laravel `Service`.
- **entradas:** `service/NotificacaoService.java` + `repository/NotificacaoRepository.java` (queries); N2.
- **saidas:** `app/Modules/Notification/Services/NotificacaoService.php`, regra testável sem HTTP.
- **criterios_de_conclusao:**
  - [ ] Ler notificação alheia sem admin →403; `page=0`/`size=101`/`periodo=ontem` →400; `link: javascript:x` →422
  - [ ] `revisar` move p/ histórico com revisor+data e some do ativo; `ler-todas` zera a contagem
- **dependencias:** N2 (N8 p/ tipos de evento, se cruzar).
- **prioridade:** P0.

## N7 — `NotificationDispatchService` (fan-in, canais, reenvio, expurgo) — `create`

- **descricao:** Regras (só aqui; **create** justificado §M7.3 — consumido por 5 listeners + 2
  schedulers): `notificar(tipo,idUsuario,titulo,mensagem[,idReferencia])` — cria **1 row por canal**:
  `inapp` **sempre** (sem opt-out, `docs/_archive/2026-10-19/07` §8; nasce `ENVIADA`+`data_envio`); `email`/`push` só se
  pref `(tipo,canal)` on — `email` ainda exige `configuracao EMAIL` on + email resolvido, tenta
  `Mail::raw` (corpo=`mensagem`; `try/catch` → `ENVIADA` ou `PENDENTE` p/ retry; `Log::info`
  auditoria); `push` ≡ persiste `ENVIADA` (polling); `notificarAdmins(tipo,titulo,…)` (resolve via
  `AuthContract::adminIds()`; vazio = warn + zero rows, nunca 500); `reenviarPendentes()` (email
  `PENDENTE` → retry, retorna contagem); `expurgarHistorico()` (delete `data_revisao_admin` > 90d,
  retorna contagem). Exceção por destinatário/evento = warn + segue (nunca 500).
- **escopo:** módulo `notification` + peça Laravel `Service`.
- **entradas:** `docs/_archive/2026-10-19/07` §5/§8 (fluxos, retenção, obrigatoriedade); N2; `AuthContract` (N8), N6 (`registrar`).
- **saidas:** `app/Modules/Notification/Services/NotificationDispatchService.php` (+ `Mail::fake`-compatível).
- **criterios_de_conclusao:**
  - [ ] `EMAIL` off ou pref-email off → zero row email (só inapp); sem email do destinatário → sem row email, sem 500
  - [ ] Falha de SMTP mantém `PENDENTE`; `expurgar` remove só >90d (contagem exata em N11)
- **dependencias:** N2 (N8 p/ contracts).
- **prioridade:** P0.

## N8 — Listeners (5 por origem) + `NotificationContract` (+2 modifies aditivos) — `reuse` (+3 `create`)

- **descricao:** 5 `Listeners/*NotificacaoListener.php` (`Rh/Estoque/Vendas/Financeiro/Producao`),
  cada método vinculando **1 evento por classe do produtor** (nomes+payloads preservados, fila
  `database`, malformado = warn/ignore): Rh(6: nivel/horas/extrato/cert×3), Estoque(3:
  baixo/atrasado/compra), Vendas(3: criada/status/aprovado), Financeiro(1: vencido —
  **sem** `custo.calculado`), Producao(6: kanban/producao-status/concluída/advert×2/mesa) — tabela
  evento→destinatário→tipo no plan §M7.2 (`emprestimo.atrasado` usa `idPessoa` direto;
  `producao.status` usa `idUsuario ?? admins`; `lancamento.vencido` só admins;
  `compra.solicitada` com `tipo` por origem). Registro via `Event::listen()` em
  `AppServiceProvider` (**modify**). Criar **`Contracts/NotificationContract.php`** (**create**:
  `naoLidas(int): int` + `inbox(int,page,size,tipo,lida): array`, congelado — p/ M8).
  Estender **`RhContract::dadosDestinatario(int): ?array{idUsuario,nome,email}`** e
  **`AuthContract::adminIds(): array`** (**modifies aditivos**, sem quebra — precedente M3/E9).
  **`access.rfid` sem bind** (D10 — N12 verifica ausência).
- **escopo:** módulo `notification` (+`Rh/Auth/Contracts` aditivos, +`AppServiceProvider`) + peça
  Laravel `Event/Listener/Contract`.
- **entradas:** `Events/*` dos 6 módulos (nomes/payloads/classes — atenção às 2 classes
  `*CompraSolicitada*` e 2 `*ProducaoStatusAlterado*`: bind pela classe do produtor); N6–N7.
- **saidas:** `Events/` (nenhum próprio — M7 só consome), `Listeners/*`, `Contracts/NotificationContract.php`
  (+ implementação via N6), `Rh/Auth Contracts` estendidos, `Event::listen()` registrado.
- **criterios_de_conclusao:**
  - [ ] 18 binds por classe do produtor; duplo `compra.solicitada` gera `tipo` por origem; evento malformado = warn sem 500
  - [ ] Nenhum módulo importa `Service/Model` do Notification — só `Contract` (grep em N12); testes Auth/Rh sem regressão
  - [ ] Zero bind de `access.rfid`/`custo.calculado` no módulo (grep em N12)
- **dependencias:** N6, N7.
- **prioridade:** P1.

## N9 — Schedulers reenvio (`*/30`) + expurgo (`03:00`) + `config/notification.php` — `reuse` (+1 `create`)

- **descricao:** Em `routes/console.php`: `Schedule::call(reenviarPendentes)->everyThirtyMinutes()
  ->name('notification:reenvio')` (equivale ao `*/30 * * * *` do despacho) +
  `Schedule::call(expurgarHistorico)->dailyAt('03:00')->name('notification:expurgo')` (alinha ao
  padrão 03:00 do monólito; `docs/_archive/2026-10-19/07` §5 pede só "diário"). `config/notification.php`
  (**create**, padrão M3/E10): `reenvio_scheduler_enabled` + `expurgo_scheduler_enabled`
  (default on; `MAIL_*` documentado no `.env.example`, sem segredo versionado).
- **escopo:** módulo `notification` + peça Laravel `Schedule/config`.
- **entradas:** despacho (crons) + `docs/_archive/2026-10-19/07` §5 (fluxos); N7 (métodos).
- **saidas:** entradas em `routes/console.php`, `config/notification.php`.
- **criterios_de_conclusao:**
  - [ ] Chamada manual de `reenviarPendentes` reenvia só `PENDENTE` e atualiza p/ `ENVIADA`/`PENDENTE` (contagem exata)
  - [ ] Chamada manual de `expurgarHistorico` remove só `data_revisao_admin` > 90d; flags desligam sem erro
- **dependencias:** N7, N8.
- **prioridade:** P1.

## N10 — Seeders (V4 na migration + verificação — sem seed inventado) — `verificação`

- **descricao:** Conferir que o **único seed** é o V4 da migration N1 (canais EMAIL/WHATSAPP + matriz
  default — `docs/_archive/2026-10-19/07` §3 + architecture V4 + Java default, nada além): nenhuma `NotificationSeeder`
  inventada (notificações de exemplo, SMTP fake, admins extras). Se o Builder achar dado mínimo
  exigido por constraint/Policy, propor seed dev-only idempotente com flag (padrão
  `AUTH_SEED_ENABLED` do M1) e escalar ao Orquestrador — nunca popular em silêncio.
- **escopo:** módulo `notification` + peça Laravel `Seeder` (se houver).
- **entradas:** migrations N1; padrão `AuthSeeder` (M1).
- **saidas:** relatório "seed = só V4" OU `database/seeders/NotificationSeeder.php` dev-only justificado.
- **criterios_de_conclusao:**
  - [ ] `migrate:fresh` + testes passam com só V4; nenhum dado inventado versionado sem justificativa
- **dependencias:** N1, N2.
- **prioridade:** P2.

## N11 — Testes Unit/Feature (401/403, isolamento, fluxos, schedulers, 80%) — `reuse` (+1 `create`)

- **descricao:** Cobrir M7 em PHPUnit/Pest via `php artisan test` **em container Docker** (pgsql
  `fablab_testing`, com `pcov`): unit (LinkValidator allowlist/`javascript:`→false, `resolverPeriodo`
  `24h/30d`/inválido, matriz default + validação estrita, `StatusEntrega`, shapes EN via Resource) +
  feature (inbox: listar escopada+filtros+paginação+aliases `pageSize`, contagem sino, ler própria,
  ler-todas; admin: prefs GET/PUT + history filtros + revisar move + config-canais toggle;
  isolamento: B não vê/lê de outro — 403/ausência; dispatch: 18 eventos via fila `database` geram
  rows c/ `tipo`/destinatário certos — `Event::fake` p/ emissão, `Mail::fake` p/ email;
  schedulers manuais: reenvio PENDENTE→ENVIADA, expurgo só >90d) + **slice security 401/403 por rota
  sensível** (**create**: sem token →401 em tudo; B/V/E em prefs/history/revisar/config →403;
  Recrutando inbox própria →200) + M0–M6 sem regressão.
- **escopo:** módulo `notification` (+`Shared`, +regressão Auth/Rh/Estoque/Vendas/Financeiro/Producao) + peça Laravel `Test`.
- **entradas:** suíte Java (casos D-1..D-7, não código) + N4–N10; DoD (`monolito-checklist.md`).
- **saidas:** `tests/Unit/Notification/*`, `tests/Feature/Notification/*` verdes; cobertura ≥ 80% (módulo); M1–M6 sem regressão.
- **criterios_de_conclusao:**
  - [ ] `php artisan test` verde (container); cobertura ≥ 80%
  - [ ] 401 sem token em tudo; 403 B/V/E em admin; isolamento dono/Admin; `lancamento.vencido` só gera p/ admins
- **dependencias:** N4, N5, N6, N7, N8, N9, N10.
- **prioridade:** P0.

## N12 — Gate DoD M7 (verificação final, sem código) — `create` (verificação)

- **descricao:** Conferir o DoD M7 (estende M0–M6, sem sobrescrever): estrutura MVC do `Notification`,
  schema próprio, fronteiras (`Contract`/`Event` só; `Rh/Auth Contracts` aditivos sem quebra), rotas
  `/api/notification/**` (+alias) exatas, `php artisan test` verde + 80% + 401/403 + isolamento,
  código mínimo (cada `create` justificado no plan §M7.3). Sem código — relatório pass/fail.
- **escopo:** módulo `notification` + peça `verificação`.
- **entradas:** N1–N11 + `.opencode/state/monolito-checklist.md` (DoD).
- **saidas:** relatório de gate (pass/fail por item) + remoções BAIXA/MÉDIA se houver excesso.
- **criterios_de_conclusao:**
  - [ ] Código em `app/Modules/Notification/{Models,Http,Services,Policies,Listeners}` (+`Contracts/` só
    consumido, +`Enums/`+`Support/`); `config/exceptions` só em `Shared`
  - [ ] Models `notification.*`; regra só em `Service`; migrations `database/migrations/notification/`; `migrate:fresh` verde; `DB_SEARCH_PATH` com `notification`
  - [ ] Zero Eureka/Gateway/HTTP entre módulos (`grep` limpo); 18 eventos com nomes preservados; sem join cross-schema; sem `Repository`/`Mapper`
  - [ ] Rotas `/api/notification/**` exatas (§M7.2) + alias 2 GETs; `PATCH` no ler; history sem `paginate`; zero bind `access.rfid`/`custo.calculado`; `reuse > create`; `docs/` intocado; nada commitado
- **dependencias:** N11.
- **prioridade:** P0.

## Grafo de dependências M7 (resumo)

```
N1 ─→ N2 ─┬→ N3 ─→ N4 ─┐
           ├→ N3 ─→ N5 ─┤
           ├→ N6 ──────┤→ N8 ─┐
           └→ N7 ──────┘→ N8 ─┤→ N9 ─┐
N1 ─→ N10 ─→ N11 ◄───────────┘──────┘→ N12   (N4 × N5 × N6 × N7 paralelizáveis após N2/N3)
```

---

# Tasks M8 — módulo `dashboard` (prefixo D, ÚLTIMO do monólito)

> Destino: `backend/` Laravel (`App\Modules\Dashboard`, **sem schema, sem models**).
> Referência de regra (não código): `dashboard-service/src/**` (shapes EN, filtro restricted no
> backend, derivações servidas) + `system/rbac-matrix.md` (resumo 0–3/Recrutando ❌, concluir se
> atribuído) + front (`frontend/src/lib/{api/dashboard.ts,types/dashboard.ts}` — paths/shapes
> consumidos). Base pronta: `Auth/Rh/Estoque/Vendas/Producao/Notification Contracts` (consumo
> in-process, NUNCA HTTP); `PATCH /api/producao/tarefas/{id}` de M6 (vínculo, reuse —
> compatibilidade, não duplicar). Convenções: Eloquent sem `Repository`; regra só em `Service`;
> `Policy`/`can:` p/ RBAC; cobertura ≥ 80%. Legenda tipo: `reuse` = regra preservada em roupagem
> Laravel · `create` = justificado no `plan.md` §M8.3.
> **Nunca `docs/`; sem `Model/Migration/Seeder/Repository/Mapper/FormRequest/Event`; não commitar.**

## D1 — 4 modifies aditivos em Contracts (`Rh` ×2, `Estoque` ×1, `Producao` ×1) — `modify` (+4 `create` aditivos)

- **descricao:** Estender as interfaces + default impls **sem quebrar** assinaturas (precedente
  M3/E9, M7/N8): `RhContract::solicitacoesCertificado(?string $status, ?int $idUsuario): array`
  (rows `idSolicitacao/nomeFuncionario/tipoCertificado/horasSolicitadas/dataSolicitacao/status`;
  `idUsuario` filtra server-side via `funcionario.id_pessoa = idUsuario`, null = todos);
  `RhContract::certificadosEmitidos(): array` (rows `idCertificado/nomeFuncionario/dataEmissao`,
  todos — chamador D2 só invoca se Admin); `EstoqueContract::emprestimosAbertos(): int` (count
  `ATIVO+ATRASADO` ≡ `?status=ativos` E-7 — chamador D2 só invoca se Admin);
  `ProducaoContract::resumoMaquinas(): array` (`status => count`, espelho de `resumoKanban`).
  Sem join cross-schema (cada impl lê só o próprio schema); **sem** `FinanceiroContract` e sem
  método novo em `Vendas`/`Notification` (fora de escopo §M8.3).
- **escopo:** módulos `rh`, `estoque`, `producao` + peça Laravel `Contract` (modify aditivo).
- **entradas:** `DominioClient.{solicitacoes,emitidos,emprestimosAtivos,maquinas}` (shapes/filtros,
  não código); `Default*Contract` M2/M3/M6; plan §M8.1 D4–D5.
- **saidas:** 4 métodos aditivos em `Contracts/*.php` + impls Eloquent do próprio schema.
- **criterios_de_conclusao:**
  - [ ] Assinaturas antigas intactas; testes Rh/Estoque/Producao existentes verdes sem alteração
  - [ ] `solicitacoesCertificado('PENDENTE', $id)` só traz do usuário; `emprestimosAbertos` == count da lista `?status=ativos`
- **dependencias:** M0–M7 (contracts commitados).
- **prioridade:** P0 (bloqueia D2).

## D2 — `DashboardService` (composição via Contracts + filtro restricted + derivações) — `create`

- **descricao:** Regra (só aqui; `Services/DashboardService.php`, injeta os 5 Contracts + `AuthContract`):
  `resumo(idUsuario, role)`: tasks = `tarefasAtivasPorResponsavel(idUsuario)` → `{id:"{id_tarefa}",
  title:titulo, module:"producao", due:data_fim_prevista, dueLabel, urgent}`; kpis =
  `ordersActive` (não-`ENTREGUE` de `resumoKanban`), `notificationsUnread` (`naoLidas`),
  + **só se Admin** `loansOpen` (`emprestimosAbertos`, `restricted:true`) e `machinesActive`
  (ativas/total, `restricted:true`) — chaves **omitidas** p/ não-Admin; `ordersByStatus` = fatias
  do kanban; `machinesByStatus` = fatias de `resumoMaquinas` (só Admin, senão `[]`); activity =
  solicitacoes (+emitidos se Admin) → `act-sol-/act-emit-`, top 8 desc (regra Java 1:1, fonte
  `data_fim_prevista` no lugar de `dataSolicitacao` p/ due — delta §M8.1 D3). `delta=null`,
  `deltaTone="muted"` 1:1. Admin com módulos vazios = `0`/`[]` com chaves presentes (nunca 500).
- **escopo:** módulo `dashboard` + peça Laravel `Service`.
- **entradas:** `DashboardService.java` (regra, não código); D1 (4 métodos); `rbac-matrix.md` regra 6.
- **saidas:** `app/Modules/Dashboard/Services/DashboardService.php`, regra testável sem HTTP (Contracts mockados).
- **criterios_de_conclusao:**
  - [ ] Não-Admin nunca recebe `loansOpen/machinesActive` nem `machinesByStatus` preenchido (teste em D5)
  - [ ] `dueLabel/urgent` e fatias conferem com a regra Java (IHM servida pronta, front não deriva)
- **dependencias:** D1.
- **prioridade:** P0.

## D3 — Policy RBAC (`ver-resumo` 0–3, Recrutando 403) — `create`

- **descricao:** `Policies/DashboardPolicy.php`: `verResumo` = autenticado com role 0–3
  (`AuthContract::roleOf` ou `auth()->user()`; Recrutando (4) → 403, matriz linha Dashboard);
  concluir tarefa **reusa** M6 (`concluirPropria` + `exigeLeitura` — sem policy nova p/ PATCH).
- **escopo:** módulo `dashboard` + peça Laravel `Policy`.
- **entradas:** `SecurityConfig` (tudo autenticado) + `system/rbac-matrix.md` (linhas Dashboard);
  `ProducaoPolicy::exigeLeitura` (reuse M6).
- **saidas:** `app/Modules/Dashboard/Policies/DashboardPolicy.php` aplicada nas rotas D4 (`can:ver-resumo`).
- **criterios_de_conclusao:**
  - [ ] Recrutando no `GET summary` →403; B/V/E →200 sem chaves restricted (coberto em D6)
- **dependencias:** D2.
- **prioridade:** P0.

## D4 — Controller + `DashboardSummaryResource` (1 classe) + rotas (`summary` + alias `resumo` + alias `PATCH /api/tasks`) — `create` (+2 `reuse`-alias)

- **descricao:** `Http/Controllers/DashboardController.php` **só-HTTP** (`summary()`: delega a D2,
  devolve `DashboardSummaryResource`; sem cálculo); `Http/Resources/DashboardSummaryResource.php`
  (**1 classe**, resto arrays — sem 5 DTOs; keys EN 1:1 com o front); `routes.php` do módulo +
  registro em `routes/api.php`: `GET /api/dashboard/summary` (front, `auth:api`+`blacklist`+
  `can:ver-resumo`) + alias `GET /api/dashboard/resumo` (PT, 1 linha, mesmo controller) +
  alias `PATCH /api/tasks/{id}` → `Producao\TarefaController@concluir` (**reuse**, 1 linha —
  body `{done:true}` ignorado, vínculo M6, idempotente). Sem `FormRequest`/validação nova.
- **escopo:** módulo `dashboard` (+1 linha em `routes/api.php`, +alias p/ `Producao`) + peça
  Laravel `Controller/Resource/route`.
- **entradas:** `DashboardController.java` + `TaskController.java` (paths/verbos, não código);
  front `dashboard.ts` (paths consumidos); D2 (service), D3 (policy).
- **saidas:** `app/Modules/Dashboard/{Http/Controllers,Http/Resources,routes.php}` + requires em `routes/api.php`.
- **criterios_de_conclusao:**
  - [ ] `GET /summary` e `GET /resumo` devolvem o mesmo shape; controllers sem cálculo/regra (grep em D7)
  - [ ] `PATCH /api/tasks/7 {done:true}` ≡ `PATCH /api/producao/tarefas/7` (mesmo controller, zero lógica duplicada — grep em D7)
- **dependencias:** D2, D3.
- **prioridade:** P0.

## D5 — Testes Unit (service puro, filtro restricted, derivações, ~100% do módulo) — `create`

- **descricao:** Cobrir D2 em `tests/Unit/Dashboard/*` (Contracts mockados/fakes, sem banco):
  restricted (Admin completo × B/V/E sem `loansOpen/machinesActive` + `machinesByStatus=[]` ×
  vazio-Admin com chaves presentes); `resumoKanban` → `ordersActive` (não-ENTREGUE) +
  `ordersByStatus`; `maquinaAtiva`/`fatiasPorStatus` (incl. status desconhecido → muted);
  `dueLabel/urgent` (hoje/futura/sem-prazo); tasks mapping (`"{id}"`, `module producao`);
  activity top-8 desc (solic+emitidos Admin × só-solic não-Admin).
- **escopo:** módulo `dashboard` + peça Laravel `Test`.
- **entradas:** `DashboardService.java` (casos, não código) + D2; DoD (`monolito-checklist.md`).
- **saidas:** `tests/Unit/Dashboard/*` verdes; cobertura ~100% do `DashboardService`.
- **criterios_de_conclusao:**
  - [ ] `php artisan test --filter=Dashboard` verde (container, pgsql `fablab_testing`)
  - [ ] Cada regra §M8.1 com caso Admin × não-Admin × vazio
- **dependencias:** D2.
- **prioridade:** P0.

## D6 — Testes Feature (401/403, filtro por papel, alias PATCH, sem regressão) — `create`

- **descricao:** Cobrir D4 em `tests/Feature/Dashboard/*` (pgsql `fablab_testing`, `pcov`):
  sem token →401 em `summary/resumo/tasks`; Recrutando →403 no módulo (incl. PATCH alias);
  Admin →200 shape completo (4 KPIs + 2 donuts + activity); B/V/E →200 sem `loansOpen`/
  `machinesActive` (+`machinesByStatus=[]`); Estagiário →200 restrito; alias `resumo` ≡ `summary`;
  `PATCH /api/tasks/{id} {done:true}` →200 dono / 403 alheio não-Admin / 401 sem token /
  idempotente (2×200); M0–M7 sem regressão (`php artisan test` global verde).
- **escopo:** módulo `dashboard` (+regressão Auth/Rh/Estoque/Vendas/Financeiro/Producao/Notification) + peça Laravel `Test`.
- **entradas:** D3–D4; DoD (`monolito-checklist.md`).
- **saidas:** `tests/Feature/Dashboard/*` verdes; M0–M7 sem regressão; cobertura ≥ 80% (módulo).
- **criterios_de_conclusao:**
  - [ ] `php artisan test` verde (container); cobertura ≥ 80%
  - [ ] 401/403 por rota + filtro restricted por papel + alias PATCH com vínculo (dono×alheio)
- **dependencias:** D3, D4 (D1 p/ dados reais).
- **prioridade:** P0.

## D7 — Gate DoD M8 (verificação final, sem código) — `create` (verificação)

- **descricao:** Conferir o DoD M8 (estende M0–M7, sem sobrescrever): `App\Modules\Dashboard/
  {Services,Http/Controllers,Http/Resources,Policies}` (+`routes.php`); **zero**
  `Models/Migrations/Seeders/Repository/Mapper/FormRequest/Event` no módulo; fronteiras só
  `Contract` (grep: nenhum `use` de `Service/Model` alheio); rotas exatas (§M8.2 + 2 aliases,
  sem lógica duplicada no alias); `migrate:fresh` verde sem migration nova + `DB_SEARCH_PATH`
  com 7 schemas (sem `dashboard`); `php artisan test` verde + 80% + 401/403 + filtro por papel;
  código mínimo (cada `create` justificado no plan §M8.3). Sem código — relatório pass/fail.
- **escopo:** módulo `dashboard` + peça `verificação`.
- **entradas:** D1–D6 + `.opencode/state/monolito-checklist.md` (DoD).
- **saidas:** relatório de gate (pass/fail por item) + remoções BAIXA/MÉDIA se houver excesso.
- **criterios_de_conclusao:**
  - [ ] Agregador sem persistência (grep `Schema::`/`$table` vazio no módulo); zero Eureka/HTTP entre módulos (`grep` limpo)
  - [ ] Rotas `/api/dashboard/summary` (+alias `/resumo`) e `PATCH /api/tasks/{id}` → concluir M6; `reuse > create`; `docs/` intocado; nada commitado
- **dependencias:** D5, D6.
- **prioridade:** P0.

## D8 — Gate FINAL do monólito (todos os módulos, cobertura global, rotas, eventos) — `create` (verificação)

- **descricao:** Conferir o monólito inteiro M0–M8 (sem código — relatório pass/fail global):
  (1) `php artisan test` verde + cobertura **global ≥ 80%**; (2) inventário de rotas
  `/api/{auth,rh,estoque,vendas,financeiro,producao,notification,dashboard}/**` (+aliases
  `/api/usuarios,/api/permissoes,/api/configuracoes/**,/api/notificacoes,/api/tasks` — cada
  alias reuse do mesmo controller, sem regra duplicada); (3) eventos: 19 nomes M7 + 6 Produção
  + demais preservados (`grep` por `.event` × architecture §Eventos); (4) fronteiras: zero
  Eureka/Gateway/HTTP entre módulos, zero `Repository/Mapper`, zero join cross-schema
  (`grep` limpo); (5) migrations por módulo `migrate:fresh` verde em DB limpo + `DB_SEARCH_PATH`
  com 7 schemas; (6) 401/403 por módulo (slice security de cada gate); (7) código mínimo global
  (lista BAIXA/MÉDIA p/ remoção); (8) `docs/` intocado; nada commitado (working tree p/ decisão
  de merge em `dev/backend`).
- **escopo:** monólito `backend/` + peça `verificação`.
- **entradas:** D7 + gates M0–M7 + `.opencode/state/monolito-checklist.md` (marcar M8).
- **saidas:** relatório de gate FINAL (pass/fail por item × módulo) + veredito pronto-para-merge.
- **criterios_de_conclusao:**
  - [ ] 8/8 módulos DoD PASS; cobertura global ≥ 80%; rotas/eventos/fronteiras conferem; `docs/` intocado; nada commitado
- **dependencias:** D7.
- **prioridade:** P0.

## Grafo de dependências M8 (resumo)

```
D1 ─→ D2 ─┬→ D3 ─→ D4 ─┐
           └→ D2 ──────┤→ D5 ─┐
D4 ─→ D6 ───────────────┘──────┤→ D7 ─→ D8
(D5 × D6 paralelizáveis após D2/D4)
```

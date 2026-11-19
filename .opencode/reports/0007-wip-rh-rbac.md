# Etapa 0007 — Reaplicação do WIP RH/RBAC sobre a árvore layer-first

- **Módulo/Feature:** `rh` + `auth` (RBAC) · transversal ao front
- **Escopo:** `backend` + `frontend`
- **Branches/worktrees:** `feat/rh-rbac-wip` (`/home/lucasitron/Documentos/05-dev/labSys-back`),
  `refactor/remap-frontend` (`/home/lucasitron/Documentos/05-dev/labSys`) e `development`
  (`/home/lucasitron/Documentos/05-dev/labSys-dev`)
- **Agentes:** orchestrator, coder (backend), frontend/coder, builder
- **Data:** 2026-11-16
- **Referências:** `wip/backend-pre-remap` (`8d50487`), `remap.md`, `.opencode/specs/features/rh-service/*`,
  `.opencode/specs/frontend/features/rh/*`, `.opencode/state/orchestration.json`
- **HEAD final integrado e enviado:** `a8da62e` (`development`, `dev/backend`, `dev/frontend`,
  `refactor/remap-backend`, `refactor/remap-frontend`) — push em 2026-11-16

## Objetivo

Reaplicar o WIP de RH/RBAC (`8d50487`) — criado antes do movimento layer-first — sobre a árvore
atual, avaliando cada mudança contra o contrato público e corrigindo a cobertura/telas afetadas.
Integrado, enviado ao `origin` em `a8da62e` e **deployado em produção** (tag `2026.11.16-1`,
2026-11-16) pelo `deployer`.

## Decisões do orquestrador

- **WIP parcial e consciente:** o WIP original tornava `CertificadoService`/`ApontamentoService`
  aptos a aceitar Admin **sem vínculo** (`id_admin_aprovador`/`id_admin_validador` nulos). Isso
  conflita com:
  - o teste de paridade Java `CertificadosTest::test_decidir_duas_vezes_e_corrida_por_horas`
    (Admin sem vínculo → 403);
  - a decisão de PO de 2026-10-17 (dar vínculo ao admin semeado em vez de anular auditoria);
  - o seed atual, que sempre cria o vínculo do admin.
  Portanto, esses dois hunks **não** foram aplicados. Em `ApontamentoService`, o risco de
  `TypeError`/500 com Admin sem vínculo foi eliminado com **403 explícito**, preservando o contrato.
- **Achado crítico de segurança (pré-existente) corrigido:** `POST /rh/pessoas` não exigia Admin para
  `nivelAcesso`/`criarAcesso`. Qualquer autenticado podia criar uma conta Admin
  (`nivelAcesso=0, criarAcesso=true`). `PessoaService::atualizar` já bloqueava; `cadastrar` não.
  Corrigido com paridade: nível e provisionamento de acesso exigem Admin também no cadastro.
- **Front alinhado:** `/pessoas/novo` passou a tratar nível e acesso como Admin-only (como
  `/pessoas/[id]/editar` já fazia) e `getNiveis` passou a mapear o novo campo `membros`.
- **Validação independente:** os subagentes `validator`/`security`/`contracts` ficaram indisponíveis
  por limite de uso gratuito durante a etapa. A validação foi executada diretamente pelo
  orquestrador (suíte, Pint, rotas, greps, análise de RBAC/exposição e compatibilidade de payload),
  com os mesmos critérios.

## Diário de bordo dos agentes

### coder (backend)

- **WIP reaplicado (commits `c736569`, `cde9dec`, `73c799e`, `df091cd`):**
  - `RbacService`: Admin nunca é reduzido — `PUT /permissoes/{modulo}/ADMIN` com valor != `EDITAR`
    → 422 e `loadCells()` força `ADMIN = EDITAR`.
  - `PessoaResource`: adiciona `idFuncionario`, `nivelAcesso`, `departamento` (aditivos).
  - `FuncionarioService::vincular`: se `roleOf(idPessoa) === ADMIN`, força nível ADMIN; sempre
    espelha `definirRole` (fonte única `funcionario.nivel_acesso → user_permissions.role`).
  - `NivelService::matriz`: adiciona `membros` (`id`, `name`, `matricula`, `nivel` lowercase, `grupo`).
  - `PessoaService`: eager load de `funcionario` em `cadastrar`/`listar`/`buscar`/`atualizar`.
  - `ApontamentoService::validar`: 403 explícito quando `exigeVinculoOuAdmin` retorna 0.
- **Testes (`94ba66d`):** invariantes de RBAC, espelho de role, `membros`, campos do resource e
  403 de Admin sem vínculo. `CertificadosTest` permaneceu intocado.
- **Correção de segurança (`169c6e1`):**
  - `PessoaService::cadastrar(dados, RhPrincipal, vinculo, acesso)`: 403 para não-admin com
    `nivel_acesso` ou `criarAcesso` (antes da criação; transação não persiste nada).
  - `PessoaController::store`, `NivelService::convidar`/`NivelController::convidar` e
    `SeederTest` atualizados para o novo parâmetro.
  - 4 novos testes em `PessoasTest`: nível 0/1 → 403; `criarAcesso` → 403 sem login; só
    `departamento` → 201 com vínculo BOLSISTA.

### frontend/coder

- **`54bf75f`:** `src/lib/api/rh/niveis.ts` passou a mapear `membros` (`RawMembro` + guard de
  nível), com `getNiveis` repassando o campo e teste para 2 membros/ausência.
- **`c1fd966` + `6f2242a`:** `/pessoas/novo` alinhado ao contrato Admin-only:
  select de nível `disabled={!isAdmin}`, seção de acesso oculta para não-admin e payload sem
  `nivelAcesso`/`criarAcesso`; testes de página cobrindo os dois perfis e correção de tipos
  (`PageProps.user`).

### builder

- `rm -rf build && npm run build` no `frontend/` @ `80dc8bb`: **exit 0**, vite 19.81s,
  adapter-node `✔ done`, `build/` 5,9M (client 3,8M / server 2,1M), handler importável e
  SSR `GET /` → 200. Único warning: a11y pré-existente em `Chip.svelte`. Nenhum arquivo alterado.
- **Artefatos de deploy (`2026.11.16-1`, @ `c388c13`):** imagem `ghcr.io/fablab/backend:2026.11.16-1`
  (`5db66a9f5b4a`, 1,03 GB) com `docker save | gzip` → `fablab-backend-2026.11.16-1.tar.gz`
  (239M, sha256 `8945fb85…`); front com `VITE_API_BASE_URL=http://100.91.193.84:8080/api` embutido
  (`build/` 5,9M, 776 arquivos) → `fablab-front-build-2026.11.16-1.tar.gz` (1,8M, sha256 `b9e8b2a1…`).
  Smoke da imagem: PHP 8.3.35; `artisan --version` exige `JWT_SECRET` em runtime (fail-fast
  esperado, `.env` fora da imagem).

### deployer

- **Deploy OK** em `fablabserver` (`100.91.193.84`, user `litro`, projeto `fablab-infra`), rollback
  não necessário:
  - `docker load` da imagem nova (id `5db66a9f5b4a` conferido), `server.yml` retagueado
    (`2026.10.17-3` → `2026.11.16-1`, backup `.bak-20261116`), `up -d --no-build`: `app` healthy,
    `worker`/`scheduler` recriados, Postgres intocado.
  - `/up` 200; log do app sem stack trace; **"Nothing to migrate"** (sem migração nova).
  - Front: build extraído em `build.new` e trocado atomicamente em `/home/litro/labsys-front/build`;
    `build.prev-20261017` preservado; `docker restart fablab-front`; `GET :3000/` 200 e chunk
    servido com `100.91.193.84:8080/api`.
  - Backup prévio: `/home/litro/backups/fablab_pre_2026-11-16.dump` (228.762 B, 77 TABLE DATA).

### orchestrator — validação direta

- **Backend:** `444 passed + 4 skipped (2537 assertions)`; Pint exit 0 (624 arquivos);
  rotas 223 → 223 com diff `(method, uri, name, middleware)` **vazio**; `App\Modules` = 0.
- **Frontend:** `npm run check` **0 erros / 17 warnings pré-existentes**; **569 testes (39 arquivos)**.
- **Contratos:** payload de `PessoaResource` aditivo e já previsto no front (`RawPessoa.nivelAcesso`,
  `idFuncionario` em horas/treinamentos); `GET /rh/niveis` agora entrega `membros` no shape de
  `NivelMembro`; `PUT /permissoes/.../ADMIN` != Editar → 422 (o front deve deixar de oferecer a
  edição da linha Admin — pendência de UX).
- **Segurança:** escalonamento de Admin no cadastro corrigido; provisionamento permanece Admin-only;
  `definirRole` só roda em rotas Admin; 403 de certificados/horas sem vínculo preservado.
- **Escalabilidade:** `NivelService::matriz` passou a carregar todos os funcionários
  (`with('pessoa')->get()`). Aceitável no volume atual; se a base crescer, paginar/limitar.

## Ponderação do orquestrador

- **Aceito:** núcleo do WIP (invariante de Admin, espelho de role, campos de vínculo, `membros`,
  eager load) — aderente ao desenho "fonte única de nível" e ao que o front já esperava.
- **Rebatido:** permissão de Admin sem vínculo decidir/validar. Auditoria nula e paridade Java
  quebrada; mantido 403.
- **Corrigido além do WIP:** escalonamento de privilégio no cadastro de pessoas (crítico) e
  alinhamento do front `novo` ao contrato Admin-only.
- **Risco residual:** a linha Admin na tela `/configuracoes/permissoes` continua editável na UI e
  passará a receber 422 ao tentar reduzir Admin. É UX, não segurança; registrar como pendência.
- **Integração:** `feat/rh-rbac-wip → development` (`9bf9aa8`), `refactor/remap-frontend →
  development` (`9f85e42`, `80dc8bb`); branches realinhadas em `80dc8bb`.

## Checklist

### Concluído

- [x] WIP `8d50487` avaliado e reaplicado (com exclusão justificada dos hunks de admin nulo)
- [x] Invariante Admin total em `RbacService` (escrita 422 + leitura)
- [x] `PessoaResource` expõe `idFuncionario`/`nivelAcesso`/`departamento`; eager load no service
- [x] `FuncionarioService::vincular` espelha role e impede rebaixamento de Admin
- [x] `GET /rh/niveis` com `membros`; front mapeia `membros`
- [x] 403 explícito (não 500) para Admin sem vínculo validar horas; 403 de certificados preservado
- [x] **Segurança:** `POST /rh/pessoas` restringe nível/provisionamento a Admin (+4 testes)
- [x] Front `/pessoas/novo` alinhado a Admin-only (+ testes)
- [x] Backend 444 passed + 4 skipped; Pint OK; rotas 223 idênticas
- [x] Front check 0 erros / 17 warnings; 569 testes
- [x] Build adapter-node OK
- [x] Integração local em `development`/`dev/*`/`refactor/remap-*` @ `80dc8bb`, sem push

### Falta concluir

- [ ] Desabilitar/ocultar a edição da linha **Admin** em `/configuracoes/permissoes` (hoje 422 na UI)
- [ ] Avaliar paginação/limite de `NivelService::matriz` se o número de funcionários crescer
- [ ] Corrigir a11y pré-existente de `Chip.svelte:18` (tabIndex) — fora do escopo
- [x] `git push` de `development`, `dev/backend`, `dev/frontend`, `refactor/remap-backend` e
      `refactor/remap-frontend` @ `a8da62e` (2026-11-16)
- [x] Artefatos do pipeline versionados em `a8da62e` (`.opencode/`, `.agents/`, `.claude/skills`,
      `skills-lock.json`, `remap.md`); `node_modules`/`backups`/lock órfão/Firebase workflows fora
- [x] Deploy `2026.11.16-1` no `fablabserver` (2026-11-16) — **DEPLOY OK**
- [x] Higiene de produção: `APP_DEBUG=false`, `5432` fechado, prune de imagens, `build.prev` compactado
- [x] Usuário de homologação desativado (pessoa INATIVO + login DESATIVADO)
- [x] Hotfix `DELETE` pessoa com vínculo → 409 (paridade Java) + UX com mensagem real
- [x] HTML do SPA com `cache-control: no-cache` (front `2026.11.18-1`)
- [ ] Validação de UI pelo PO
- [x] Produção: `APP_ENV=production` + `AUTH_SEED_ENABLED=false` (+ overlay versionado em `2a4fc03`)
- [ ] Avaliar `LOG_LEVEL=info` em produção (hoje `debug`, herdado do overlay dev)
- [ ] Backlog de contrato front↔back (paths `/stock` vs `/estoque`, kanban `PATCH`/`PUT`, 5S etc.)

## Deploy 2026-11-16 (tag `2026.11.16-1`)

```
imagem   ghcr.io/fablab/backend:2026.11.16-1  sha256:5db66a9f5b4a…  (anterior 2026.10.17-3)
backend  app healthy :8080 (/up 200) · worker Up · scheduler Up · "Nothing to migrate"
front    fablab-front :3000 · GET / 200 · build com VITE_API_BASE_URL=…:8080/api
backup   /home/litro/backups/fablab_pre_2026-11-16.dump (228.762 B)
rollback tag 2026.10.17-3 + build.prev-20261017 + docker-compose.server.yml.bak-20261116
```

| Smoke | Resultado |
|---|---|
| `POST /api/auth/login` (admin) | 200 (claims `ADMIN`, `id_user:1`) |
| `GET /api/rh/niveis` | 200 com `membros` |
| `GET /api/rh/pessoas` | 200 com `idFuncionario` + `nivelAcesso` |
| `PUT /api/permissoes/rh/ADMIN {"valor":"Ver"}` | **422** (invariante; DB segue `EDITAR`) |
| `POST /api/rh/pessoas` Bolsista `nivelAcesso=0` | **403** (nada criado) |
| `OPTIONS` preflight de `:3000` | 204, `Access-Control-Allow-Origin: *` |
| `GET :3000/` | 200 |

## Higiene de produção 2026-11-16 (pós-deploy)

| Item | Ação | Verificação |
|---|---|---|
| `APP_DEBUG` | `infra/.env`: `true` → `false` (`.bak`), `up -d --no-build` recriando app/worker/scheduler | `/up` 200; `artisan about` = **Debug OFF**; `GET /api/__nao_existe__` → 404 genérico, sem stack trace |
| Postgres `5432` | `server.yml`: `postgres.ports: !reset []` (Compose 5.6.0), `up -d` só do postgres | `ss -ltn` sem 5432; `nc` externo **CLOSED**; app conecta via `fablab-net` (`172.31.0.3:5432`); volume `fablab-infra_postgres-data` preservado |
| Imagens | `rmi` de `2026.10.14-1`, `.17-1`, `.17-2`, `fablab-infra-*` + `prune` de dangling (2,07 GB) | 26 → 21 imagens; disco **89% → 87%** (13G livres); preservadas `2026.11.16-1` e `2026.10.17-3` |
| Front antigo | `build.prev-20261017` → `fablab-front-build-20261017.tar.gz` (1,8M, 791 entradas) e diretório removido (6,0M) | `tar -tzf` OK; `labsys-front` só com `build/` |
| Usuário de homologação | `DELETE /api/rh/pessoas/4` → **422** (possui vínculo); fallback: `status=INATIVO`, login `DESATIVADO`, `user_permissions.active=false` | acesso revogado; restam `rh.funcionario` id=2 e linhas desativadas |
| Backup | `/home/litro/backups/fablab_pre_cleanup_2026-11-16.dump` (224K) | `pg_restore -l` OK |

Smokes reexecutados após a higiene: `/up` 200, front 200, login 200, `/rh/niveis` 200 com `membros`,
`PUT /permissoes/rh/ADMIN=Ver` → 422, bolsista `nivelAcesso=0` → 403. Portas publicadas:
`0.0.0.0:8080` e `0.0.0.0:3000` apenas.

### Config final de produção (2026-11-16, tarde)

| Item | Ação | Verificação |
|---|---|---|
| `APP_ENV` | `local` → `production` no `infra/.env` do host; overlay passou a usar `${APP_ENV:-local}` (o valor estava hardcoded) | `artisan about`: Environment **production**, Debug OFF, config/routes cached |
| `AUTH_SEED_ENABLED` | `true` → `false` | `config('auth.seed.enabled')` = false; boot cita o `AuthSeeder` mas ele faz bail em 0 ms; `auth.login` 3→3 e `rh.pessoa` 4→4 |
| Drift eliminado | `chore(infra)` `2a4fc03`: `infra/docker-compose.dev.yml` → `APP_ENV: "${APP_ENV:-local}"` | `compose config` resolve `production` com a variável e `local` sem ela |

Recriados apenas app/worker/scheduler (postgres e front com o mesmo ID/`StartedAt`). Smokes após a
mudança: `/up` 200, login 200, `/rh/niveis` 200 com `membros`, `PUT /permissoes/rh/ADMIN=Ver` 422,
`GET :3000/` 200, rota inexistente 404 sem stack trace.

## Hotfix 2026-11-16 — `DELETE /rh/pessoas/{id}` com vínculo: 422 → 409

**Sintoma (PO, produção):** `DELETE http://100.91.193.84:8080/api/rh/pessoas/8` → **422**, e a UI
mostrava "Ocorreu um erro inesperado."

**Causa raiz (duas):**
1. **Contrato:** o Java (`03c3bb1`, `ConflitoException` + `GlobalExceptionHandler`) responde **409**
   para exclusão com vínculo/processo seletivo; o porte Laravel lançava `InvalidArgumentException`,
   mapeado globalmente para **422**.
2. **UX:** `confirmarExcluir` usava `toUserMessage(err).message`, que só trata 401/423/5xx — qualquer
   4xx virava "Ocorreu um erro inesperado.", escondendo a mensagem real da API.

**Correção:**
- `b88f00a` (backend): `PessoaService::excluir` lança `ConflitoException` (409) nos dois casos, com os
  mesmos textos do Java; testes cobrem vínculo e processo seletivo (445 passed + 4 skipped, 2540
  assertions; Pint OK; 223 rotas). O mapeamento global de `InvalidArgumentException` → 422 foi
  **mantido** (mudá-lo afetaria validações de todos os módulos).
- `293d531` (front): `confirmarExcluir` usa `apiErrorMessage` (mensagem real do 409) e o modal ganhou
  a orientação "Pessoas com vínculo de funcionário ou processo seletivo não podem ser excluídas —
  nesse caso, desative o cadastro (`status = Inativo`)". 570 testes; check 0 erros.

**Deploy `2026.11.16-2`** (imagem `ea84de021b3f`): app/worker/scheduler recriados; postgres e front
preservados no passo do backend; front com swap atômico (`build.prev-20261116`) e restart; `Nothing
to migrate`; `AuthSeeder` bail. Smoke: `DELETE /api/rh/pessoas/8` → **409**
`Pessoa possui vínculo de funcionário e não pode ser excluída: 8` (SQL confirma pessoa intacta);
inexistente → 404; `/rh/niveis` 200 com `membros`; `PUT /permissoes/rh/ADMIN=Ver` 422; bolsista
`nivelAcesso=0` 403; `/up` e front 200; CORS 204. Backup `fablab_pre_2026-11-16-2.dump` (224K);
rollback: `2026.11.16-1` + `.bak-20261116-2` + `build.prev-20261116` + dump.

**Verificação pós-deploy (a pedido do PO): NO_AR — nenhuma ação necessária.** Os 3 serviços backend
rodam `2026.11.16-2` (`ea84de021b3f`), `server.yml` com 3/3 linhas, `/up` e front 200,
`version.json` servido = build atual, node do hotfix presente, `DELETE pessoas/8` → 409 com a
mensagem exata e registro intacto.

**Achado (causa provável de "vejo a versão antiga"):** o HTML do SPA é servido **sem
`Cache-Control`/`Last-Modified`** (só `etag`), enquanto os assets têm `immutable`. O browser pode
aplicar cache heurístico no shell e exibir a versão anterior até um hard refresh. Correção
recomendada (task separada): `Cache-Control: no-cache` no HTML no adapter-node/`hooks.server.ts`.

## Cache do HTML — SPA sem cache heurístico (2026-11-18)

**Motivo:** o HTML do SPA era servido sem `Cache-Control`/`Last-Modified` (só `etag`), enquanto os
assets eram `immutable`. O browser podia cachear heuristicamente o shell e continuar exibindo a
versão antiga após deploy — exatamente o que aconteceu depois do hotfix 409.

**Correção (`20d468d`):** novo `frontend/src/hooks.server.ts` com `handle` que aplica
`cache-control: no-cache` apenas em `text/html` (revalidação pelo `etag`, 304 quando nada muda);
assets intactos. 3 testes novos → **573 passed** (40 arquivos); check 0 erros / 17 warnings.

**Build `2026.11.18-1`** (adapter-node): 5,9M / 778 arquivos, sha256 `81f43e9a…dfc59`,
`version.json` `1795057026756`. Staging local provou: HTML `no-cache` + `etag`, `If-None-Match` →
304 e asset `immutable`.

**Deploy front-only** (backend `2026.11.16-2` intocado, mesmos IDs de container): `build.prev-20261116-2`
preservado, swap atômico + `docker restart fablab-front`. Produção: HTML `no-cache` (`etag
"1r0xjih"`), 304 em `127.0.0.1` e `100.91.193.84`, `version.json` novo, hotfix 409 presente;
`/up`, front e login 200; `DELETE /rh/pessoas/8` → 409.

## Evidências finais (HEAD `a8da62e`)

```sh
# backend
docker run --rm --network host -v "$PWD":/app -w /app fablab-php:8.3-test php artisan test
# 444 passed, 4 skipped (2537 assertions)

docker run --rm --user "$(id -u):$(id -g)" -e HOME=/tmp -v "$PWD":/app -w /app \
  fablab-php:8.3-test vendor/bin/pint --test
# PASS (624 files)

php artisan route:list --json  # 223 rotas; diff vs 894621d = vazio

# frontend
npm run check  # 0 errors, 17 warnings
npm run test   # 569 passed (39 arquivos)
npm run build  # adapter-node OK (builder)
```

- `grep -rn "App\\\\Modules" app routes bootstrap config database tests` → 0
- `CertificadosTest::test_decidir_duas_vezes_e_corrida_por_horas` → verde sem edição
- `PessoasTest` → 403 para nível/acesso por não-admin; 201 para só departamento
- `git ls-remote --heads origin` → as 5 branches em `a8da62e`; `origin/development...development` = 0/0

## Resumo cache

```
WIP_RH_RBAC_2026-11-16: reaplicado parcialmente em 80dc8bb, integrado e enviado @a8da62e. Aceitos: invariante Admin total (RBAC 422/leitura), PessoaResource com idFuncionario/nivelAcesso/departamento + eager load, vincular espelha role e protege ADMIN, /rh/niveis com membros (front mapeia), 403 explícito p/ Admin sem vínculo ao validar horas. Rejeitado: permitir admin sem vínculo decidir/validar com aprovador/validador nulo (mantém paridade Java/403 e vínculo do seed). Corrigido extra: escalonamento de privilégio em POST /rh/pessoas (nível/provisionamento agora Admin-only) e front /pessoas/novo Admin-only. Back 444+4 (2537), front check 0/17, 569 testes, Pint OK, 223 rotas idênticas, build adapter-node OK. Push OK @a8da62e (5 branches); higiene OK (APP_DEBUG=false, 5432 fechado, disco 87%); config prod OK (APP_ENV=production, seed off); hotfix 409 deployado (2026.11.16-2); deploy OK 2026-11-16 tag 2026.11.16-1 (imagem 5db66a9f5b4a) em fablabserver: app healthy :8080, worker/scheduler Up, front :3000, Nothing to migrate, smokes 200/422/403/204. Pendências: UI da linha Admin em /configuracoes/permissoes (422), paginação de membros, a11y Chip, backlog de contrato.
```

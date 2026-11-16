# Etapa 0006 — Implementação do remap.md (simplificação estrutural)

- **Módulo/Feature:** `system/remap` (transversal back+front)
- **Escopo:** `backend` + `frontend`
- **Branch/Worktree:** `refactor/remap-backend` (`/home/lucasitron/Documentos/05-dev/labSys-back`),
  `refactor/remap-frontend` (`/home/lucasitron/Documentos/05-dev/labSys`) e `development`
  (`/home/lucasitron/Documentos/05-dev/labSys-dev`)
- **Agentes envolvidos:** orchestrator, coder, frontend/coder, tester, frontend/tester, validator,
  frontend/validator, security, contracts, builder
- **Data:** 2026-11-08
- **Referências:** `remap.md` (raiz), `.opencode/specs/system/architecture.md`,
  `.opencode/specs/system/STATUS.md`, `docs/contract.gap.md`
- **HEAD final integrado:** `894621d` (`development`, `dev/backend`, `dev/frontend`,
  `refactor/remap-backend`, `refactor/remap-frontend`)

## Objetivo da etapa

Executar as fases 0–8 do `remap.md` **sem alterar o contrato público** (`/api/<modulo>/**`, payloads,
enums, status, RBAC): baseline, limpeza de artefatos, código morto, validação nativa, consolidação
HTTP/identidade, eventos/exceptions/config, **movimento estrutural layer-first (fase 6)**,
documentação/CI e guardrails contra regressão.

## Decisões do orquestrador antes/durante a execução

- **WIP preservado:** os 7 arquivos não commitados do backend foram preservados em `8d50487`
  (`chore(wip): checkpoint antes do remap estrutural`) e movidos para a branch
  **`wip/backend-pre-remap`**. Como esse WIP deixava a suíte vermelha
  (`CertificadosTest::test_decidir_duas_vezes_e_corrida_por_horas`), a refatoração foi feita a partir
  de `c76d7f7` (último commit verde). O WIP **não** foi reaplicado nesta etapa (fora do escopo do remap).
- **Fase 6:** inicialmente adiada por decisão do PO; nesta execução foi retomada e concluída, um
  módulo por commit, com validação de rotas e suíte a cada etapa.
- **Contrato imutável:** o diff de rotas (`method`, `uri`, `middleware`, `name`) foi comparado antes/
  depois; o diff final é **vazio**.
- **Sem push:** todos os commits são locais. O deploy não foi executado (exclusivo do `deployer`).

## Baseline (Fase 0)

- **Backend** (`c76d7f7`): `php artisan test` → **429 passed, 4 skipped (2454 assertions)** em
  `fablab-php:8.3-test` + Postgres `fablab-test-pg` (`127.0.0.1:5433`).
- **Frontend** (`6228ba9`): `npm run check` → **0 errors, 17 warnings**; `npm run test` →
  **556 passed (37 arquivos)**.
- **Snapshot de rotas:** 223 rotas. O snapshot inicial em `/tmp` foi corrompido em uma das cópias
  (Fatal error `vendor/autoload.php`); o `contracts` regenerou o baseline a partir de `2b19775` e
  confirmou o diff vazio contra o HEAD (`/tmp/opencode/rotas-baseline-2b19775.json` ×
  `/tmp/opencode/rotas-head-verified.json`).
- **Contagens:** `.opencode/state/remap/rotas-antes.json` (snapshot inicial); contagens de arquivos
  registradas no relatório de baseline anterior.

## Diário de bordo dos agentes

### coder (backend) — Fase 6 e fechamento

- **Feito:** movimento layer-first dos 8 módulos (`Auth`, `Notification`, `Estoque`, `Vendas`,
  `Financeiro`, `Producao`, `Rh`, `Dashboard`):
  - `Contracts` → `app/Contracts` (flat);
  - `Enums` → `app/Enums/<Modulo>`;
  - `Events` → `app/Events` (flat);
  - `Http/Controllers|Requests|Resources` → `app/Http/{Controllers,Requests,Resources}/<Modulo>`;
  - `Models` → `app/Models/<Modulo>`;
  - `Services` → `app/Services/<Modulo>`;
  - `Listeners` → `app/Listeners/<Modulo>`;
  - `Policies` → `app/Policies` (flat);
  - `Rules` → `app/Rules` (flat);
  - `routes.php` → `routes/modules/<modulo>.php`;
  - casos especiais: `Auth/Http/Middleware/CheckBlacklist` → `app/Http/Middleware`,
    `EstoquePrincipal`/`RhPrincipal` → `app/Services/<Modulo>`,
    `Notification/Support/LinkValidator` → `app/Support/Notification`,
    `Notification/Exceptions/LinkInvalidoException` → `app/Exceptions`.
- **Fechamento das ressalvas:** unificação de todas as exceções em `app/Exceptions`; traits de
  `Principal` achatados para `app/Http/Controllers/<Modulo>/` (profundidade 4); remoção de
  `app/Shared/Exceptions`; README raiz e `backend/README` atualizados; `withEvents(false)` comentado
  e coberto por `tests/Feature/EventRegistrationTest`; Pint aplicado.
- **Decisões:** `bootstrap/app.php` usa `->withEvents(false)` porque os 28 binds são explícitos em
  `AppServiceProvider`; sem isso, a descoberta automática de `app/Listeners` duplicaria os 28 binds
  (56 no total). Guard test criado para impedir regressão.
- **Evidências:** suíte final **431 passed + 4 skipped (2486 assertions)**; rotas 223/223 com diff
  vazio; `App\Modules` = 0 referências em código; Pint exit 0.
- **Dúvidas/Bloqueios:** nenhum.

### frontend/coder — fases 1–4 e 7 + higiene

- **Feito:** remoção de docs/e2e órfãos; remoção de exports mortos e re-export circular de RH;
  centralização de e-mail em `isEmail`; `apiFetch` com auth automática + `buildQuery` único;
  remoção de `stockFetch`/`bearer()` manuais; barrel explícito de produção; `Paged<T>` unificado
  (depois removido por estar morto); aliases de tipos de paginação; README corrigido; limpeza de
  resíduos de `authMock`/`vi.mock`; teste de `isEmail`.
- **Evidências:** `npm run check` 0 erros / 17 warnings; `npm run test` **563 passed (39 arquivos)**;
  `git status` limpo. Build delegado ao builder.
- **Dúvidas/Bloqueios:** `Paged<T>` não tinha consumidor; a decisão final foi remover o tipo e os
  reexports mortos, mantendo os nomes `PageInfo`/`Pagination` como aliases de `Paginacao`.

### tester (backend)

- **Feito:** suíte completa e comparação de rotas.
- **Resultado:** **429 passed, 4 skipped (2452 assertions)** no estado pós-fase 6 (antes dos guardrails);
  **323 pares method+uri** (223 rotas únicas) antes/depois, diff `(method, uri, middleware)` vazio;
  `php -l` em todos os PHP de `app/routes/bootstrap/config/database/tests` sem erro.
- **Falhas:** nenhuma.

### frontend/tester

- **Feito:** `npm run check`, `npm run test`, greps do remap.
- **Resultado inicial:** 0 erros / 17 warnings; 557 passed (38 arquivos). Apontou duas pendências:
  `Paged<T>` duplicado e README com `(auth)/`.
- **Resultado final (após correções):** 563 passed (39 arquivos), incluindo `validation.test.ts`.

### validator (backend)

- **Feito:** revisão independente em 4 eixos (persistência, escalabilidade, arquitetura,
  funcionalidade), comparação de 415 renames, metadata de 69 models, eventos/binds, autoload.
- **Resultado:** **APROVADO COM RESSALVAS**. Ressalvas: docs citando `app/Modules`, exceções
  divididas, traits em profundidade 5, `withEvents(false)` sem comentário/teste, Pint.
- **Encaminhamento:** todas as ressalvas de código/documentação foram fechadas na rodada de
  correções; `withEvents(false)` recebeu comentário e teste; Pint foi aplicado; README atualizado;
  exceções unificadas; traits achatados.

### frontend/validator

- **Feito:** revisão independente das fases 1–4/7; `check`, `test`, build (executado pelo validator
  antes do builder), greps de `bearer`, `buildQuery`, `export *`, e-mail.
- **Resultado:** **APROVADO COM RESSALVAS** (Paged morto, 14 `URLSearchParams` manuais, tipos de
  paginação duplicados, `isEmail` sem teste, scaffold de `authMock`).
- **Encaminhamento:** Paged removido, aliases de paginação, `isEmail` testado, scaffold limpo,
  `paginarClient` importado direto. A migração dos 14 `URLSearchParams` manuais ficou como dívida
  opcional (o requisito do remap era remover os 3 construtores de query, o que foi feito).

### security (backend)

- **Feito:** RBAC/middleware, superfície de ataque, eventos/listeners, exceções, imports e testes
  de segurança.
- **Resultado:** **APROVADO COM RESSALVAS**. Nenhuma vulnerabilidade introduzida; 223 rotas
  idênticas em middleware; 0 rotas `auth:api` sem `blacklist`; 28 binds únicos; `event:list` sem
  duplicatas; 69/69 models com `$fillable`; `LinkValidator` intacto; suíte completa verde.
- **Ressalvas (pré-existentes / hardening):** (`JWT_BLACKLIST_ENABLED=false` depende do middleware
  por rota), `APP_DEBUG=true` no `.env.example`, upload CSV sem allowlist MIME. Não bloqueiam o remap.

### contracts

- **Feito:** comparação de rotas, payloads/resources, enums/rules, status/erros, RBAC, eventos e
  impacto no front.
- **Resultado:** **APROVADO COM RESSALVAS**. Fase 6 é *contract-neutral*; 223 rotas idênticas; 0
  divergência em shapings/enums/rules/status/RBAC/eventos; `git diff 2b19775..98304f7 -- frontend`
  vazio.
- **Ressalva de processo:** o snapshot de baseline fornecido em uma das cópias estava corrompido;
  regenerado a partir de `2b19775` e confirmado.
- **Divergências pré-existentes front↔back (backlog, não são regressão do remap):** `/stock/*` vs
  `/estoque/*`; `PATCH` vs `PUT` no kanban; `/producao/mesas` vs `/projetos-mesa`;
  `/producao/5s/setores` vs `/producao/setores`; `/rh/people` vs `/rh/pessoas`;
  endpoints 5S/resumo inexistentes; `unread/count` vs `nao-lidas/contagem`.

### builder

- **Feito:** `rm -rf build && npm run build` no `frontend/`.
- **Resultado:** **BUILD OK** — exit 0, 513 módulos, adapter-node `✔ done`, `frontend/build/` (5,9M),
  handler importável, smoke `GET /` e `/auth/login` 200. `npm run check` 0 erros; `npm run test`
  563 passed.
- **Observação:** build sem `VITE_API_BASE_URL` injetado; para entrega no servidor, refazer com a URL
  real se necessário. Deploy não executado (exclusivo do `deployer`).

## Ponderação do orquestrador

- **Aceito:** movimento layer-first mecânico, com 415+ renames e 0 arquivos de comportamento
  alterados; suíte e rotas idênticas ao baseline; contratos preservados.
- **Aceito com correção:** a primeira versão de `withEvents(false)` foi justificada de forma
  incorreta pelo coder ("sem efeito"), mas o teste independente mostrou que sem ela os listeners
  eram duplicados (56 binds). A solução final (`->withEvents(false)` + comentário + guard test) é
  correta e mínima.
- **Devolvido e corrigido:** docs com `app/Modules`, exceções divididas, traits em profundidade 5,
  `Paged<T>` morto, README do front com `(auth)/`, scaffold de `authMock` e `isEmail` sem teste.
- **Não aceito nesta rodada:** migração dos 14 `URLSearchParams` manuais e unificação dos tipos
  `PageInfo`/`Pagination` para consumo real; são dívidas de baixo risco, registradas como backlog.
  O critério do remap (um único `buildQuery` e remoção dos outros construtores) foi atendido.
- **Decisão de integração:** `refactor/remap-backend → development` (`e7f1baf`),
  `development → refactor/remap-frontend` (`8a66e62`, conflito de `README.md` resolvido com a versão
  reescrita do front), `development → refactor/remap-backend` (`bac7e38`) e guardrails (`894621d`).
  `dev/backend`, `dev/frontend`, `development` e as branches `refactor/remap-*` ficaram no mesmo
  commit. O WIP antigo permanece isolado em `wip/backend-pre-remap` (`8d50487`).
- **Risco residual:** o WIP de RH/RBAC precisa ser reaplicado sobre a árvore layer-first em uma
  tarefa separada; a suíte permanece verde sem ele.

## Checklist

### Concluído

- [x] Fase 0 — baseline backend verde (429 passed + 4 skipped) e frontend verde (556 testes, 0 erros)
- [x] Fase 0 — snapshot de rotas (223) e contagens registradas
- [x] Fase 1 — artefatos/scaffolds removidos (Vite/resources/package.json do backend, docs/e2e órfãos)
- [x] Fase 2 — código morto declarado removido (imports, relações sem uso, `bearer()`, exports mortos, re-export circular)
- [x] Fase 3 — validação nativa (authorize vazio, `date_format`, `boolean`, CPF/CNPJ centralizado, e-mail único no front)
- [x] Fase 4 — `Caller` compartilhado/controller base; `apiFetch` com auth automática, `buildQuery` único, sem `stockFetch`/`bearer()`/barrels
- [x] Fase 5 — eventos, exceptions e config consolidados (3 eventos unificados em `app/Events`; `config/fablab.php`)
- [x] Fase 6 — movimento layer-first dos 8 módulos; `app/Modules` eliminado; `routes/modules/*.php`; 223 rotas idênticas
- [x] Fase 7 — README raiz/backend/frontend, infra e CI (`backend-ci.yml`, `frontend-ci.yml`); docs duplicadas removidas
- [x] Fase 8 — guardrails: `ArchitectureBoundaryTest`, `EventRegistrationTest`, `vendor/bin/pint --test` no CI e checklist de PR
- [x] Exceções unificadas em `app/Exceptions`; traits de Principal em profundidade 4; Pint aplicado
- [x] Suíte backend final: **433 passed + 4 skipped (2488 assertions)**; frontend: **563 passed (39 arquivos)**, check 0 erros
- [x] Build frontend OK (adapter-node)
- [x] Contratos validados: rotas/payloads/enums/status/RBAC/eventos sem alteração observável
- [x] Integração local: `development`, `dev/backend`, `dev/frontend` e `refactor/remap-*` em `894621d`
- [x] WIP antigo preservado em `wip/backend-pre-remap` (`8d50487`)

### Falta concluir

- [ ] Reaplicar/avaliar o WIP `8d50487` (RH/RBAC) sobre a árvore layer-first; corrigir o teste
      `CertificadosTest::test_decidir_duas_vezes_e_corrida_por_horas` antes de integrar
- [ ] Migrar os 14 blocos manuais de `URLSearchParams` para `buildQuery` (dívida opcional do front)
- [ ] Resolver as divergências pré-existentes front↔back listadas pelo `contracts` (backlog de contrato)
- [ ] Adicionar `knip`/`ts-prune` e PHPStan/Larastan nível 5 (Fase 8 completa de tooling)
- [ ] `git push` de `development`/`dev/*` — decisão do PO (nada foi enviado ao remoto)
- [ ] Deploy da nova árvore no servidor — exclusivo do `deployer`, quando o PO autorizar
- [ ] Limpar artefatos untracked do workspace (`.agents/`, `.claude/`, workflows Firebase de outro
      projeto, `skills-lock.json`) — não versionados; remoção exige confirmação do PO

## Evidências finais

```sh
# backend (worktree labSys-back / integrado em development)
docker run --rm --network host -v "$PWD":/app -w /app fablab-php:8.3-test php artisan test
# 433 passed, 4 skipped (2488 assertions)

docker run --rm --network host -v "$PWD":/app -w /app fablab-php:8.3-test php artisan route:list --json
# 223 rotas; diff (method, uri, name, middleware) vs baseline = vazio

docker run --rm --user "$(id -u):$(id -g)" -e HOME=/tmp -v "$PWD":/app -w /app fablab-php:8.3-test vendor/bin/pint --test
# exit 0 (624 arquivos)

# frontend (worktree principal / integrado em development)
cd frontend
npm run check   # 0 errors, 17 warnings
npm run test    # 563 passed (39 arquivos)
npm run build   # adapter-node OK (builder)
```

- `grep -rn "App\\\\Modules" backend/app backend/routes backend/bootstrap backend/config backend/database backend/tests` → 0
- `find backend/app -type d -name Modules` → vazio
- `event:list` → 28 binds explícitos, sem duplicatas (guard test)
- `git diff` de `frontend/src/lib/api` no remap: paths/métodos/payloads inalterados; apenas remoção de
  `headers: bearer()` e troca de `stockFetch` por `apiFetch`.

## Resumo cache

```
REMAP_2026-11-08: fases 0-8 concluídas. Backend layer-first (app/Http|Models|Services|Enums|Listeners/<Modulo>, app/Contracts|Events|Policies|Rules|Exceptions, routes/modules/*.php); app/Modules eliminado. 223 rotas idênticas; suíte 433 passed + 4 skipped (2488 assertions); Pint OK; ArchitectureBoundaryTest/EventRegistrationTest. Front: apiFetch auth automática + buildQuery único, sem bearer/stockFetch/barrels, isEmail único, Paged<T> morto removido; check 0 erros/17 warnings, 563 testes, build adapter-node OK. Integrado em development/dev/backend/dev/frontend/refactor/remap-* @894621d (sem push). WIP 8d50487 preservado em wip/backend-pre-remap; pendente reaplicar RH/RBAC + corrigir CertificadosTest. Divergências front↔back pré-existentes e tooling (knip/PHPStan) ficam em backlog.
```

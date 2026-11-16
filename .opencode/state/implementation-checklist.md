# Checklist de Implementação — Frontend (dev/frontend)

> Fonte de verdade do que **falta** implementar no SPA SvelteKit.
> Gerado: 2026-09-20 · Mantido por: **tracker** (agente de acompanhamento) + Orchestrator.
> Estado vivo: `.opencode/state/frontend-orchestration.json` (este arquivo é o resumo operacional).

## Legenda de status

| Status | Significado |
|---|---|
| `✅ FEITO` | Entregue e verificado (commit/mockup/spec existem) |
| `🟡 PARCIAL` | Parte implementada; falta o que estiver listado |
| `🔴 FALTA` | Nada feito nesta etapa |
| `⛔ BLOQUEADO` | Depende de decisão humana / backend / outra feature |

## Base já entregue em `dev/frontend` (20/09)

| SHA | Commit | Conteúdo |
|---|---|---|
| `19385cc` | login | tela login, `api/auth.ts`, `api/client.ts`, `stores/auth.ts`, `types/auth.ts`, `utils/errors.ts`, guard `(app)/+layout.ts` |
| `b5fde2c` | app shell + dashboard | `(app)/+layout.svelte`, Sidebar/Topbar/Breadcrumb/UserMenu/NotificationsButton, dashboard (KpiGrid, charts, MyTasks, ActivityFeed), `config/menu.ts`, `utils/permissions.ts` |
| `e56ea03` | estoque + layout + UI kit | 8 telas estoque, `api/stock/*` (bom, items, loans, locations, movements, request, suppliers), `api/rh.ts`, UI kit (`ui/*`, `estoque/*`), toast, SidebarGroup |

---

## Checklist por feature (9)

### 1. `auth` — ✅ IMPLEMENTADA E VALIDADA (status: CONCLUIDA)
- [x] Mockup (`docs/mokups/login/`) — **revisão humana pendente** (`[ ]` no design-checklist)
- [x] Spec: requirements + plan + tasks (`auth-001..007`)
- [x] Código base implementado (login, store, guard, client com timeout 15s)
- [x] **Builder — 7 tasks:** `auth-001` interceptor global 401 (G-1) · `auth-002` consumir `?redirect=` (G-2) · `auth-003` rota `/auth/forgot-password` (G-3) · `auth-004..007` testes (G-4)
- [x] Validator (plan.md §5) — **APROVADA** 20/09 (10/10) · [x] Security (plan.md §6) — **APROVADA COM RESSALVAS** 20/09 (0 Alto, 1 Médio, 4 Baixo)
- [x] `npm run test` 63/63 verdes · `npm run check` 12/15/7 (baseline estoque, zero novos) · **falta: commit em `dev/frontend`**
- ℹ️ Infra testes: `vite.config.js` ganhou alias svelte→`index-client.js` + `server.deps.inline: true` + `defineConfig` de `vitest/config` (necessário p/ testes de componente Svelte 5; removido `svelteTesting()` p/ não vazar erro de tipo)
- 🟡 Ressalvas Security p/ feature dona: env `VITE_API_BASE_URL` https em prod; guarda `token?` no `fetchMe`; centralizar 401/sanitize (duplicação em `dashboard.ts`/`request.ts`)

### 2. `dashboard` — ✅ IMPLEMENTADA E VALIDADA (status: CONCLUIDA)
- [x] Mockup · [x] **Revisão humana APROVADA** (20/09)
- [x] Código implementado no shell (`b5fde2c`)
- [x] Spec: **plan.md + tasks.md materializados** (9 gaps → `dash-001..009`) 20/09
- [x] **Builder — 9 tasks** (`dash-001..009`) 20/09: handler 401 único, load não-bloqueante, refetch visibilitychange, Greeting empty, 5 suítes de teste
- [x] Validator — **APROVADA COM RESSALVAS** 20/09 (11/11, visuais menores) · Security — **APROVADA COM RESSALVAS** 20/09 (0 Alto; 2 Médios contrato backend)
- [x] `npm run test` **95/95** (63+32) · `npm run check` baseline exato (zero novos) · **falta: commit em `dev/frontend`**
- ⛔ Contrato backend: dashboard-service filtrar `restricted` no summary; producao-service `PATCH /tasks/{id}` c/ vínculo

### 3. `estoque` — ✅ CONCLUÍDA (status: CONCLUIDA)
- [x] Mockup (19 telas) · [x] **Revisão humana APROVADA** (20/09, global 9/9)
- [x] Spec: requirements (PT `/api/estoque/**`) + plan (decisões R-9/R-8/D-2) + **tasks `est-001..036`** materializados 21/09
- [x] **Builder — 6 blocos** 21/09: DTOs reais + migração EN→PT · telas existentes · rotas novas (editar item, detalhes entrada/saída, empréstimos completo) · fornecedores/localizações/BOM · testes (252 verdes; e2e skip) · correções + **código mínimo (−122 linhas)**
- [x] **Validator** APROVADA COM RESSALVAS (14 itens; M-1/M-2 corrigidos) · **Security** APROVADA COM RESSALVAS (0 Alto; M-1/M-3/B-1/B-2/I-1..I-5 corrigidos)
- [x] Commits `dev/frontend`: `f7b3be5` · `192004c` · `f16d0c7` · `f300d83` · `d512324` · `f8ae34c`
- [x] `npm run check` **0 errors** (baseline era 12 erros em estoque) · `npm run build` ok
- ⛔ Contract (fila backend/PO): R-9 (listas globais), R-8 (listagem BOM), D-2/D-4 (responsável; `pessoa` na resposta), escape CSV no export de itens, mensagens não-técnicas, RBAC `GET /estoque/emprestimos/**`

### 4. `rh` — ✅ CONCLUÍDA (status: CONCLUIDA)
- [x] Mockup (12 telas, rotas `/pessoas/*`) · [x] **Revisão humana APROVADA** (20/09, global 9/9)
- [x] Spec completa: requirements + plan (G-1..G-13; R-1..R-11 de enxugamento; D-1..D-10) + **30 tasks `rh-001..rh-030`**
- [x] **Builder — 7 blocos**: fundação (types PT + 5 APIs `/api/rh/**` + rh-status + TypeBadge/DonutChart/KanbanBoard + RBAC helpers) · pessoas CRUD (guarda, lista server-side + bulk sem CSV, detalhe tabs, novo, editar) · Kanban PS (Tutor) + registro-horas (extrato backend, zero soma) · treinamentos (second sidebar + overview/detalhe/agenda/documentação/sessão, Instrutor) · níveis Admin + submenu `menu.ts` · testes (381 verdes; e2e skip) · correções (H1 admin filtrado, deletePessoa na API, hints estáticos, tabs próprias role 3/4, legado −34 linhas)
- [x] **Validator** APROVADA COM RESSALVAS (0 Alta; M1/M3/B1 corrigidos; M2 mitigado) · **Security** APROVADA COM RESSALVAS (H1/B1/B2/B3/B4 corrigidos; H2/M-backend fila)
- [x] Commits `dev/frontend`: `4b33407` · `9d76271` · `642a856` · `b8c4a8c` · `607084e` · `4406cb0` · `ff0843d`
- [x] `npm run check` 0 errors/16 warnings/7 files · build ok
- ⛔ Contract (fila backend/PO): D-1..D-10 (quase todos 🔴) + H2 (autorizar cada escrita server-side) + `DELETE /api/rh/pessoas/{id}` + searchPeople legado + recalcular `hours` + CPF/retenção

### 5. `vendas` — ✅ CONCLUÍDA (status: CONCLUIDA)
- [x] Mockup (11 telas, incl. Marketplace/CRM) · [x] **Revisão humana APROVADA** (20/09, global 9/9)
- [x] Spec completa: requirements (reconciliado no plan §8: C-1..C-7) + plan (G-1..G-12; R-1..R-9; D-1..D-9) + **32 tasks `vnd-001..vnd-032`**
- [x] **Builder — 8 blocos**: fundação (types PT + 7 APIs `/api/vendas/**` + status/format + Timeline/MoneyInput + RBAC criador) · clientes (guarda, lista + bulk, SolicitarEdicaoModal, detalhe timeline, novo) · orçamentos (lista tabs, novo página cheia, detalhe conversão) · encomendas (Kanban reusado — dona única — + detalhe) · marketplace/marketing/solicitações · menu · testes (451 verdes; e2e skip) · correções (gates criador H1/H2/H4/M1, máscara documento H3, toUserMessage, canSee, 204, 409 refetch)
- [x] **Validator** APROVADA COM RESSALVAS (0 Alta; M-1/B-2..B-4 corrigidos; B-1 documentado) · **Security** REPROVADA → **todas as 17 correções front aplicadas** (H1-H4/M1-M7/B1-B6); condicionante externa: backend 403 por criador
- [x] Commits `dev/frontend`: `ed7dbcd` · `7e3eb8b` · `0a00ccf` · `83ff024` · `3ad1396` · `7beb58b` · `481ef8b`
- [x] `npm run check` 0 errors/17 warnings/8 files · build ok
- ⛔ Contract (fila backend/PO): D-1 🔴 service+gateway · D-2 shapes/duplicar/bulk · D-3 🔴 criador+Admin · D-4 🟡 solicitações · D-5/D-6/D-7/D-8/D-9 · 403 por criador (condicionante)

### 6. `financeiro` — ✅ CONCLUÍDA (status: CONCLUIDA)
- [x] Mockup (7 telas, **Admin-only**) · [x] **Revisão humana APROVADA** (20/09, global 9/9)
- [x] Spec completa: requirements (reconciliado no plan §8) + plan (G-1..G-11; R-1..R-10; D-1..D-8) + **23 tasks `fin-001..fin-023`**
- [x] **Builder — 9 blocos**: fundação (types PT + 6 APIs `/api/financeiro/**` + status/format + guarda Admin-only) · lançamentos+resumo · contas derivadas + doações · custeio (4 abas) + relatórios (6 panes) · menu · testes (524 verdes; e2e skip) · correções (modal unificado **−386 linhas**, periodo allowlist, encode ids, bulk allSettled, tipo fixo)
- [x] **Validator** APROVADA COM RESSALVAS (0 Alta; R-M1 corrigido; R-M2/R-B4 documentados) · **Security** APROVADA COM RESSALVAS (0 Alto/Médio; B1–B4 corrigidos)
- [x] Commits `dev/frontend`: `2e78ed8` · `4ccc9e3` · `b9a6eb0` · `49cbfb2` · `06aa0f6` · `0d3203b`
- [x] `npm run check` 0 errors/17 warnings/8 files · build ok
- ⛔ Contract (fila backend/PO): D-1 🔴 service+gateway · D-2 shapes/séries · D-3 🔴 `@PreAuthorize` 0 · D-4 filtros+periodo · D-5 congelamento/auditoria · D-6 PDF fora MVP · D-7 erros · D-8 vencimento/origem

### 7. `producao` — ✅ CONCLUÍDA (status: CONCLUIDA)
- [x] Mockup v2 (12 telas, 24 modais, verify-prod OK) · [x] **Revisão humana APROVADA** (20/09)
- [x] Spec completa: requirements v2 + plan v2 + **42 tasks `prd-001..042`**
- [x] **Builder — Blocos 1..6 + correções (bloco 7)**: types/utils/client · 12 módulos API + mock resumo · CartaoBadge+8 modais · menu/RBAC/layout · **12 telas** · testes (150 verdes; e2e skip sem Playwright) · ressalvas corrigidas
- [x] **Validator** APROVADA COM RESSALVAS (9/9) · **Security** APROVADA COM RESSALVAS (0 Alto; M-1/B-2/B-3/B-4 corrigidos)
- [x] Commits `dev/frontend`: `c4406f4` · `84b643a` · `a011991` · `7bc70f5` · `6863ce3` · `a6633ef` · `45546ca`
- ⛔ Contract 🟡 (fila backend/PO): gateway **não expõe `/api/producao/**`**; `POST /projetos/{id}/documentos|materiais|saida`; coluna kanban/checklist de tarefa; nota 5S recalc backend
- ✅ ConflictMap C1/C2 liberado — `menu.ts`/`permissions.ts` podem ser tocados por `configuracoes` (próxima, sequencial)

### 8. `notificacoes` — ✅ CONCLUÍDA (status: CONCLUIDA)
- [x] Mockup (3 telas) · [x] **Revisão humana APROVADA** (20/09, global 9/9)
- [x] Reuso do código existente: `stores/notifications.ts` (polling 60s) + `NotificationsButton` (topbar, fora de escopo)
- [x] Spec completa: requirements + plan (G-1..G-9; R-1..R-12 de enxugamento) + **16 tasks `not-001..016`**
- [x] **Builder — 5 blocos + correções**: fundação (types/API/util/Icon bell) · lista central `/notificacoes` (guarda 303, 5 estados, URL `?page&type&status`, otimistas) · admin (`/preferencias` matriz+dirty, `/historico` filtros+tabela) · submenu `menu.ts` (children Admin hide) · testes (300 verdes; e2e skip sem Playwright) · bloco 6 (correções A1/M1/B1..B5)
- [x] **Validator** APROVADA COM RESSALVAS (16 tasks; A1 corrigido) · **Security** APROVADA COM RESSALVAS (0 Alto; B2/B3 corrigidos)
- [x] Commits `dev/frontend`: `d62efee` · `18714ee` · `ebfdcd8` · `65c158b` · `e2f2471`
- [x] `npm run check` 0 errors/16 warnings/7 files · build ok
- ⛔ Contract (fila backend/PO): D-1 shape `UnreadCount`/paginado · D-2 isolamento JWT em `GET /notifications` · D-3 🔴 `@PreAuthorize` Admin em preferences/history · D-4 params do history + lista completa (paginação client-side hoje) · D-5 mensagens não-técnicas · D-6 `link?` scheme · D-7 shape `HistoryPayload`

### 9. `configuracoes` — ✅ CONCLUÍDA (status: CONCLUIDA)
- [x] Mockup (4 telas, verify-config OK) · [x] **Revisão humana APROVADA** (20/09, global 9/9)
- [x] Spec completa: requirements + plan + **21 tasks `cfg-001..021`**
- [x] **Builder — Blocos 1..3 + correções (bloco 4)**: types+API (perfil/senha/usuarios/permissoes/sistema) · RBAC (menu.ts+permissions.ts) + componentes (PermCell/ModalUsuario/ModalConfirm) · guarda + **4 telas** (`/perfil` todos; `/usuarios` `/permissoes` `/sistema` Admin-only hide) · testes (162 verdes; e2e skip sem Playwright) · ressalvas corrigidas
- [x] **Validator** APROVADA COM RESSALVAS (14/14) · **Security** APROVADA COM RESSALVAS (0 Alto; B1/B2/I3 corrigidos)
- [x] Commits `dev/frontend`: `d06833d` · `d9d0875` · `ee895ae` · `78b6516`
- ⛔ Contract 🟡 (fila backend/PO): `GET/PUT /configuracoes/perfil`, `PUT /auth/senha`, `GET/POST/PUT/PATCH /usuarios*`, `GET/PUT /permissoes*`, `GET/PUT /configuracoes/sistema`, `GET/POST/DELETE /configuracoes/tokens` — todos **assumidos**; exigem `@PreAuthorize` Admin + perfil do JWT (M1/M2)
- ✅ ConflictMap C1/C2 consumido — `menu.ts`/`permissions.ts` agora livres p/ próxima feature (sequencial)

---

## Sumário executivo

| Feature | Código | Spec tasks | Builder | Validator/Security | Próximo passo |
|---|---|---|---|---|---|
| **auth** | ✅ | ✅ 7 | ✅ | ✅ (Aprovada c/ ressalvas) | **commit `dev/frontend` → dashboard** |
| dashboard | ✅ | ✅ 9 | ✅ | ✅ (Aprovada c/ ressalvas) | concluída |
| estoque | ✅ | ✅ 36 | ✅ | ✅ (Aprovada c/ ressalvas — corrigidas) | concluída |
| rh | ✅ | ✅ 30 | ✅ | ✅ (Aprovada c/ ressalvas — corrigidas) | concluída |
| vendas | ✅ | ✅ 32 | ✅ | ✅ (Validator aprovada c/ ressalvas; Security reprovada→corrigida) | concluída |
| financeiro | ✅ | ✅ 23 | ✅ | ✅ (Aprovada c/ ressalvas — corrigidas) | concluída |
| producao | ✅ | ✅ 42 | ✅ | ✅ (Aprovada c/ ressalvas — corrigidas) | concluída |
| notificacoes | ✅ | ✅ 16 | ✅ | ✅ (Aprovada c/ ressalvas — corrigidas) | concluída |
| configuracoes | ✅ | ✅ 21 | ✅ | ✅ (Aprovada c/ ressalvas — corrigidas) | **concluída** |

## Dívidas técnicas / contratos (cross-cutting)
- [ ] **`package.json`/`package-lock.json` na RAIZ do repo** — vieram do PR antigo `c00d4e2` ("connect module create", 04/set) integrado no merge da `main` (24/09). O lockfile da raiz é de um esqueleto legado (`API/`), não do frontend. **Decisão PO pendente:** remover da raiz num commit futuro (manter lockfile apenas em `frontend/`) ou manter. `.opencode/` e `package-lock.json` (raiz) ficam **fora do GitHub** (não rastreados), conforme decisão do humano em 24/09.
- [x] **Revisão humana 1 a 1 dos 9 mockups** (`design-checklist.md`) — concluída (9/9 aprovados)
- [ ] `docs/contract.gap.md`: materializar endpoints assumidos 🟡 (producao, configuracoes, estoque, rh) + **expor `/api/producao/**` no gateway** + **nota 5S validada/calculada no backend** + **@PreAuthorize Admin em configuracoes** + mensagens de erro genéricas + contrato do token de integração (chave 1x) + KPIs no Paginado de usuários
- [ ] Migração de token `#1a2235` → `#111a2c` nos mockups antigos (decisão PO)
- [x] **auth**: Validator APROVADA (20/09, §5 10/10) + Security APROVADA COM RESSALVAS (20/09, §6 8/8, 0 Alto): env `VITE_API_BASE_URL` https em prod (Médio); guarda `token?` no `fetchMe`; centralizar 401/sanitize em helper único; regex sanitizeRedirect controles (Baixos) — pendências de hardening na feature dona
- [ ] Nenhum PR aberto / merge integrado (trabalho vive nas vertentes `dev/backend` + `dev/frontend`)

## Sequência de execução recomendada
1. **Revisão humana dos 9 mockups** → libera tudo
2. **auth**: #### DONE — Builder ✅ · Validator ✅ APROVADA · Security ✅ APROVADA COM RESSALVAS · falta commit em `dev/frontend`
3. **dashboard / estoque**: materializar plan+tasks; estoque continua Builder das telas faltantes
4. **rh → vendas → financeiro → notificacoes**: plan/tasks + Builder (módulos podem ser paralelos, exceto os que toquem `client.ts`/`menu.ts` simultaneamente)
5. **producao (42 tasks) e configuracoes (21 tasks)**: em sequência, **nunca em paralelo** (C1–C5) — producao primeiro (C2 push pra configuracoes já está no plan)

## Regras de manutenção (tracker)
- Nunca editar `docs/**`, mockups, requirements aprovados ou código — **consultar e reportar apenas**
- Atualizar este checklist após cada marco (task concluída, feature avançada, contrato fechado)
- Consultar os prompts de kickoff em `docs/prompts/*.md` como base de aconselhamento
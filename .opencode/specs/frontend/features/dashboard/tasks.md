# Tasks: Dashboard — Frontend (gaps)

> Branch: `dev/frontend` · Spec da feature: `.opencode/specs/frontend/features/dashboard/`
> Mockup aprovado: `docs/mokups/dashboard/` (requisitos.md, dashboard.html, dashboardState.html)
> Referência de auth: interceptor 401 global em `client.ts` (commit 5b68329) + `sanitizeRedirect`.
> Regra: **1 arquivo ≡ 1 task** · `acao` = create | modify · texto UI pt-BR · tokens design-system ·
> RBAC hide-não-disable · **NÃO alterar** `menu.ts`/`permissions.ts`/`docs/**`/`.opencode/state/**`.
> Estas tasks cobrem **apenas os gaps** (G-1..G-9 do `plan.md`).

---

## Resumo

| id | Titulo | Arquivo | Acao | Depende |
| :--- | :--- | :--- | :--- | :--- |
| dash-001 | Remover handler 401 duplicado do dashboard | `frontend/src/lib/api/dashboard.ts` | modify | — |
| dash-002 | Desbloquear renderizacao (load imediato) | `frontend/src/routes/(app)/dashboard/+page.ts` | modify | — |
| dash-003 | Fetch no mount com skeleton e refetch por foco | `frontend/src/routes/(app)/dashboard/+page.svelte` | modify | dash-002 |
| dash-004 | Variante Empty "Bem-vindo" no Greeting | `frontend/src/routes/(app)/dashboard/components/Greeting.svelte` | modify | — |
| dash-005 | Testes da API do dashboard | `frontend/tests/unit/dashboard-api.test.ts` | create | dash-001 |
| dash-006 | Testes do polling de notificacoes | `frontend/tests/unit/notifications.test.ts` | create | — |
| dash-007 | Testes de RBAC por bloco do KpiGrid | `frontend/tests/unit/dashboard-kpigrid.test.ts` | create | — |
| dash-008 | Testes do MyTasks (toggle otimista) | `frontend/tests/unit/dashboard-mytasks.test.ts` | create | dash-001 |
| dash-009 | Testes de estados da pagina dashboard | `frontend/tests/unit/dashboard-page.test.ts` | create | dash-002, dash-003, dash-004 |

---

## dash-001 — Remover handler 401 duplicado do dashboard

- **Titulo:** Delegar 401 ao interceptor global e remover `handle401`/`NOT_AUTH_REDIRECT`
- **Arquivo:** `frontend/src/lib/api/dashboard.ts`
- **Acao:** `modify`
- **Detalhes:**
  - Remover a constante `NOT_AUTH_REDIRECT` e a função `handle401`; remover imports sem uso
    (`goto` de `$app/navigation`, `ApiError` de `./client`, `logout` de `$lib/stores/auth`).
  - Ajustar `fetchSummary`, `fetchUnreadCount` e `completeTask` para chamar `apiFetch` diretamente e
    **rethrow** (`throw err` / sem try-catch que engole): o interceptor global do `client.ts`
    (auth-001) já trata 401 (logout + redirect sanitizado da rota atual); os callers atuais
    (`MyTasks.toggle`, páginas) continuam recebendo `ApiError`.
  - Manter intacto o helper `bearer()` (header `Authorization: Bearer <token>` condicional) usado
    pelas três funções — não centralizar header nesta task (consolidação futura fora do escopo).
  - Endpoints inalterados: `GET /dashboard/summary`, `GET /notifications/unread/count`,
    `PATCH /tasks/{id}` body `{ done: true }`.
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] `rg "NOT_AUTH_REDIRECT|handle401" frontend/src` sem ocorrências em `api/dashboard.ts`
  - [ ] arquivo não importa `goto`, `ApiError` nem `logout`
  - [ ] 401 em dashboard → interceptor global (logout + redirect `?redirect=` sanitizado); sem
    double-logout, sem destino hardcoded
  - [ ] `completeTask` rethrow preservado (falha → `MyTasks` restaura item)
  - [ ] `npm run check` sem erros; testes `dashboard-api.test.ts` (dash-005) verdes

---

## dash-002 — Desbloquear renderizacao (load imediato)

- **Titulo:** `+page.ts` não aguardar a rede (habilitar skeleton na entrada)
- **Arquivo:** `frontend/src/routes/(app)/dashboard/+page.ts`
- **Acao:** `modify`
- **Detalhes:**
  - Remover o `fetchSummary`/try-catch do `load`: o retorno é imediato (ex.: `return { summary: null,
    error: null }` ou shape equivalente sem dados), mantendo `ssr = false` / `prerender = false`.
  - O fetch do summary passa a acontecer no mount da página (dash-003) para exibir o estado Loading
    (skeleton) do mockup §5/State1 — `load` bloqueante hoje impede o skeleton de aparecer.
  - Não alterar o guard `(app)/+layout.ts` (auth) nem outras rotas.
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] navegar para `/dashboard` renderiza a página imediatamente (sem esperar a rede)
  - [ ] `+page.svelte` sem dependência de `data.summary` (dash-003 concluído)
  - [ ] sem regressão no guard `(app)/+layout.ts`; `npm run check` sem erros

---

## dash-003 — Fetch no mount com skeleton e refetch por foco

- **Titulo:** Carregar summary na página (skeleton inicial) + refetch em `visibilitychange`
- **Arquivo:** `frontend/src/routes/(app)/dashboard/+page.svelte`
- **Acao:** `modify`
- **Dependencia:** `dash-002` (contrato de `data` mudou)
- **Detalhes:**
  - Iniciar com `loading = true` (ou `summary === null && primeiro render`) para exibir os skeletons
    já existentes na entrada — mantendo o layout sem shift (blocos skeleton por seção).
  - Chamar `refetch()` no mount (`$effect`/`onMount`) — único fetch de entrada do summary.
  - Adicionar listener `visibilitychange`: página `!document.hidden` → `refetch()` (mockup §10:
    "Summary — fetch no mount + refetch ao ganhar foco"); remover no destroy.
  - Remover o gate `settled`/dependência de `data` (de `PageProps`) — estado passa a ser todo local.
  - Corrigir `isEmpty` para incluir `summary.machinesByStatus.length === 0` e `summary.kpis`
    (ausente/vazio), mantendo ordenação das cláusulas.
  - Sub aprovado: `refetch()` limpa `error`, seta `loading`, repõe `summary` e trata falha com a
    mensagem amigável (banner global + blocos com placeholder) — lógica já existente, reusar.
  - RBAC dos blocos (machines via `canSeeMachines`) e grid reflow **inalterados**.
- **Verificar (DoD):**
  - [ ] primeira entrada mostra skeletons (não spinner, não tela em branco)
  - [ ] retry do ErrorBanner continua funcionando
  - [ ] trocar de aba (ocultar→mostrar) dispara refetch do summary
  - [ ] listener removido no destroy (sem leaks ao navegar)
  - [ ] `isEmpty` inclui `machinesByStatus`/`kpis`
  - [ ] testes `dashboard-page.test.ts` (dash-009) verdes; `npm run check` sem erros

---

## dash-004 — Variante Empty "Bem-vindo" no Greeting

- **Titulo:** Saudação do estado Empty conforme mockup (State2)
- **Arquivo:** `frontend/src/routes/(app)/dashboard/components/Greeting.svelte`
- **Acao:** `modify`
- **Detalhes:**
  - Adicionar prop opcional `empty?: boolean` (default `false`).
  - Quando `empty` para `true`, o h1 renderiza **"Bem-vindo, {nome}."** (mockup State2) em vez da
    saudação temporal; `sub` default continua "Ainda não há dados para exibir. Comece por aqui:" —
    a página já passa esse texto.
  - Demais estados (sucesso/erro/loading) mantêm `greeting()` inalterado.
  - A página (`+page.svelte`) passa `empty={isEmpty}` ao `<Greeting>`.
  - UI/textos pt-BR; tokens `text-xl font-semibold tracking-tight` preservados.
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] empty: h1 = "Bem-vindo, {nome}."
  - [ ] sucesso: h1 = "Bom dia/tarde/noite, {nome}." (inalterado)
  - [ ] erro: sub "Alguns dados não puderam ser carregados." mantido
  - [ ] `npm run check` sem erros

---

## dash-005 — Testes da API do dashboard

- **Titulo:** Cobrir `api/dashboard.ts` (URLs, Bearer, PATCH, rethrow, sem redirect local)
- **Arquivo:** `frontend/tests/unit/dashboard-api.test.ts`
- **Acao:** `create`
- **Dependencia:** `dash-001` (handler 401 removido)
- **Detalhes:**
  - Com vitest + jsdom, mockar `fetch` e a store `auth` (para o token) no padrão de `client.test.ts`:
    - `fetchSummary`: monta `${API_BASE}/dashboard/summary` e header `Content-Type`
    - `fetchUnreadCount`: monta `/notifications/unread/count`
    - `completeTask`: `PATCH /tasks/{id}` com body `JSON.stringify({ done: true })` e método PATCH
    - Bearer: header `Authorization: Bearer <token>` presente quando token na store; **ausente** quando
      não autenticado (`bearer()` retorna vazio — sem `Bearer null`)
  - Pós G-1 (delegação 401 ao interceptor):
    - mockar `$app/navigation` e `$lib/stores/auth` e asserir que **nenhum** `goto`/`logout` local é
      chamado quando a resposta é 401 (o interceptor global é testado em `client.test.ts`)
    - 401/5xx com payload `{code,message}` → `ApiError` rethrow com status/code preservados
    - sucesso → JSON tipado retornado
  - Seguir env: `vi.stubEnv('VITE_API_BASE_URL', 'http://api.test')` e `vi.unstubAllEnvs()` no afterEach.
- **Verificar (DoD):**
  - [ ] 3 endpoints cobertos com URLs e headers exatos
  - [ ] Bearer condicional (com e sem token)
  - [ ] 401 sem efeito local no módulo (somente rethrow) após remoção do handler
  - [ ] green: `npm run test`

---

## dash-006 — Testes do polling de notificacoes

- **Titulo:** Cobrir `stores/notifications.ts` (60s, pausa hidden, resume, cleanup)
- **Arquivo:** `frontend/tests/unit/notifications.test.ts`
- **Acao:** `create`
- **Detalhes:**
  - Com vitest + jsdom e **fake timers** (`vi.useFakeTimers()`), mockar `api/dashboard.fetchUnreadCount`
    e reimportar o módulo dinamicamente (`vi.resetModules()` + `await import(...)`) para isolar os
    flags módulo-level (`timer`, `polling`):
    - `startPolling()` faz refresh imediato (contador atualizado)
    - intervalo de 60s dispara novo refresh; conta chamadas ao avançar `vi.advanceTimersByTime`
    - `document.hidden = true` → intervalo NÃO chama `refresh` (pausa)
    - `visibilitychange` com página visível → refresh imediato + re-agendamento (mockup §10)
    - `stopPolling()` limpa timer e listener (nenhum refresh após destroy)
    - `startPolling()` duas vezes → sem dupla instância (flag `polling`), sem timers concorrentes
    - falha no `fetchUnreadCount` → contador mantém valor anterior (sem crash)
  - Reset do `document.hidden` e listeners no afterEach (`vi.useRealTimers()`).
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] poll 60s coberto com fake timers
  - [ ] pausa + resume em `document.hidden` cobertos (trocar `Object.defineProperty(document, 'hidden')`)
  - [ ] sem leaks: cleanup e anti-duplicidade validados
  - [ ] green: `npm run test`

---

## dash-007 — Testes de RBAC por bloco do KpiGrid

- **Titulo:** Cobrir ocultação de Empréstimos/Máquinas por papel e badge RESP
- **Arquivo:** `frontend/tests/unit/dashboard-kpigrid.test.ts`
- **Acao:** `create`
- **Detalhes:**
  - Com `@testing-library/svelte` + jsdom, montar `KpiGrid.svelte` mockando a store `auth`
    (`$lib/stores/auth`) com diferentes `User`:
    - Admin (role 0): renderiza 4 cards (Encomendas, Empréstimos, Máquinas, Notificações)
    - Voluntária com `responsibilities.estoque = ['cat_3d']`: vê Empréstimos, **não** vê Máquinas
    - Voluntária com `responsibilities.producao = ['laser']`: vê Máquinas, **não** vê Empréstimos
    - Sem responsabilidade nem role 0: só Encomendas e Notificações — **hide** (card ausente, não
      `disabled`/visível-clicável)
  - Badge "RESP." presente nos cards restritos quando renderizados (`restricted` prop)
  - KPIs com valores: `ordersActive`, `loansOpen`, `machinesActive`, `notificationsUnread`
  - Referência às funções de permissions já testadas (`permissions.test.ts`) — aqui é a integração
    componente × regra (não re-testar a regra).
  - Usar `data-testid="kpi-card"` para asserções de contagem/renderização.
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] 4 combinações de role/responsabilidade cobertas
  - [ ] blocos restritos ausentes do DOM (não apenas visualmente ocultos)
  - [ ] badge RESP ao renderizar KPI restrito
  - [ ] green: `npm run test`

---

## dash-008 — Testes do MyTasks (toggle otimista)

- **Titulo:** Cobrir conclusão otimista, restore em falha e refetch do `MyTasks.svelte`
- **Arquivo:** `frontend/tests/unit/dashboard-mytasks.test.ts`
- **Acao:** `create`
- **Dependencia:** `dash-001` (comportamento de falha do `completeTask` após remoção do handler)
- **Detalhes:**
  - Com `@testing-library/svelte` + jsdom, mockar `api/dashboard.completeTask`:
    - marca o checkbox de `data-testid="task-item"` → item sai da lista (otimista) e `completeTask(id)`
      é chamado uma vez
    - sucesso: `onRefetch` disparado; contagem "N pendentes" atualizada
    - falha: `completeTask` rejeita → item volta à lista (restore) e continua visível para nova
      tentativa; `onRefetch` ainda dispara
    - `pending` impede toggle duplo na mesma tarefa enquanto `completeTask` está em voo
    - `aria-label` por tarefa (`Concluir tarefa: ...`)
    - empty: "Sem tarefas por aqui" + botão "Explorar módulos"; error: "Não foi possível carregar
      suas tarefas." + "Tentar novamente"
  - Fixture de `Task[]` no formato `types/dashboard.ts` (com `dueLabel`, `urgent`, `module`).
- **Verificar (DoD):**
  - [ ] fluxo otimista + restore em falha cobertos
  - [ ] `onRefetch`, contagem e guard de `pending` validados
  - [ ] estados empty/error do bloco cobertos
  - [ ] green: `npm run test`

---

## dash-009 — Testes de estados da pagina dashboard

- **Titulo:** Cobrir Loading/Empty/Erro + refetch por foco do `+page.svelte`
- **Arquivo:** `frontend/tests/unit/dashboard-page.test.ts`
- **Acao:** `create`
- **Dependencia:** `dash-002` e `dash-003` (fetch na página) e `dash-004` (greeting empty)
- **Detalhes:**
  - Com `@testing-library/svelte` + jsdom, mockar `api/dashboard.fetchSummary` e a store `auth`:
    - **Loading**: fetch pendente → skeletons renderizados (sem blocos de dados), sem layout shift
    - **Sucesso**: 200 com `DashboardSummary` completo (admin) → Greeting + MyTasks + KpiGrid +
      OrdersChart + MachinesChart + ActivityFeed renderizados
    - **Empty**: summary com arrays vazios → greeting "Bem-vindo, {nome}." e blocos empty
    - **Erro**: `fetchSummary` rejeita → ErrorBanner global com "Tentar novamente"; clicar refaz o
      fetch (retry)
    - **Refetch por foco**: disparar `visibilitychange` (página visível) → `fetchSummary` chamado de
      novo (mockup §10)
  - `visibilitychange` simulado com `new Event('visibilitychange')` sobre `document`.
  - RBAC: usuário sem `canSeeMachines` → `MachinesChart` ausente do DOM e `OrdersChart` com
    `md:col-span-2` (grid reflow).
- **Verificar (DoD):**
  - [ ] 4 estados (loading/sucesso/empty/erro) cobertos
  - [ ] refetch por `visibilitychange` coberto
  - [ ] RBAC/grid reflow validados
  - [ ] green: `npm run test`

---

## DoD da feature (após dash-001..dash-009)

- [ ] `npm run test` verde (suíte unit existente + novos testes de dashboard)
- [ ] `npm run check` sem erros de tipo
- [ ] Validador executa checklist do `plan.md` §5 (mockup 1-a-1, estados, RBAC, polling, a11y)
- [ ] Security executa checklist do `plan.md` §6 (handler único, redirect sanitizado, polling sem
      leaks/loop, RBAC no DOM, payload restricted no backend)
- [ ] Nenhuma alteração em `docs/`, `docs/mokups/`, specs de sistema, `.opencode/state/**`,
      `menu.ts`/`permissions.ts` nem arquivos de outras features
- [ ] Contrato respeitado: apenas `GET /api/dashboard/summary`, `GET /api/notifications/unread/count`
      e `PATCH /api/tasks/{id}` consumidos
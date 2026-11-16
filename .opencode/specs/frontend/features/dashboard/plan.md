# Plan: Dashboard — Frontend (gap analysis vs código real)

> **Fonte de verdade:** código real da branch `dev/frontend` (commits `b5fde2c` app shell+dashboard,
> `5b68329` auth finalizada) + mockup aprovado `docs/mokups/dashboard/` (`requisitos.md` RF-01..12,
> RNF-01..06, contrato §4, estados §5, RBAC §8, donuts §9, polling §10, a11y §11, critérios §12,
> notas técnicas §14) + spec da feature (`.opencode/specs/frontend/features/dashboard/requirements.md`).
> Método: **inventariar → comparar → gaps arquivo a arquivo** — NÃO replanejamento do zero (padrão auth).

---

## 1. Estado real (inventário arquivo a arquivo)

| Arquivo | Papel | Estado vs mockup/spec | Observação |
| :--- | :--- | :--- | :--- |
| `src/routes/(app)/+layout.ts` | guard + fetchMe (auth) | OK | RF-03; criado pela feature auth — não tocar |
| `src/routes/(app)/+layout.svelte` | app shell: Sidebar/Topbar/Breadcrumb/UserMenu/**NotificationsButton** + polling | OK | RF-01/02, RNF-05; sidebar persiste `fablab.sidebar.collapsed`; `startPolling/stopPolling` no mount/destroy |
| `src/routes/(app)/dashboard/+page.ts` | `load` do summary (bloqueante) | GAP | **G-2a**: `load` aguarda a rede → skeleton inicial nunca aparece (mockup §5/State1) |
| `src/routes/(app)/dashboard/+page.svelte` | orquestra estados + blocos + RBAC | GAP | **G-2b/G-3**: sem fetch no mount (skeleton só no retry); sem refetch em `visibilitychange` (mockup §10); `isEmpty` ignora `machinesByStatus`/`kpis` |
| `components/Greeting.svelte` | saudação contextual | GAP | **G-4**: estado Empty do mockup exige h1 "Bem-vindo, {nome}." — hoje renderiza sempre saudação temporal ("Bom dia...") |
| `components/KpiGrid.svelte` | 4 KPIs com RBAC | OK | Kpi "Empréstimos em aberto" e "Máquinas ativas" ocultos por `canSeeLoans`/`canSeeMachines` (hide, não disable); badges RESP via `KpiCard` |
| `components/KpiCard.svelte` | card genérico KPI | OK | valor+total (`3/8`), delta com tom, `formatNumber` pt-BR, estado erro "indisponível", badge RESP |
| `components/OrdersChart.svelte` | donut "Encomendas por status" | OK | público (mockup RF-07 não restringe; §8 só restringe Empréstimos/Máquinas) |
| `components/DonutChart.svelte` | SVG donut reutilizável | OK | §9: `viewBox 42`, `r=15.9`, `stroke-width=5`, dash array proporcional, animação 300ms, `role="img"`+`aria-label`, trilha vazia no empty |
| `components/MachinesChart.svelte` | donut "Status das máquinas" (restrito) | OK | badge RESP; omitido do DOM quando `!canSeeMachines` (+page); grid reflow via `md:col-span-2` no OrdersChart |
| `components/MyTasks.svelte` | bloco primário de tarefas | OK | RF-05; check otimista + `PATCH /tasks/{id}` + restore na falha + `onRefetch`; badge pendentes; empty com CTA "Explorar módulos" |
| `components/ActivityFeed.svelte` | feed de atividade | OK | ator+verbo+alvo (brand) + `relativeTime` pt-BR; tom por módulo; link "Ver histórico →" (rota futura) |
| `src/lib/api/dashboard.ts` | `fetchSummary`, `fetchUnreadCount`, `completeTask` | GAP | **G-1**: handler 401 local duplicado + `NOT_AUTH_REDIRECT` hardcoded (`/dashboard`) — redundante com interceptor global (auth; `sanitizeRedirect`) |
| `src/lib/types/dashboard.ts` | `DashboardSummary`, `Task`, `Kpi`, `StatusSlice`, `ActivityItem`, `UnreadCount` | OK | contrato §4.2/§4.3 exato |
| `src/lib/stores/notifications.ts` | `unreadCount` + polling | OK-código / GAP-testes | polling 60s existe (`POLL_MS=60_000`), pausa em `document.hidden`, resume+refresh em `visibilitychange`, cleanup em `stopPolling`, flag anti-duplicidade (RNF-06, mockup §10) — **sem testes** (G-5) |
| `src/lib/components/layout/NotificationsButton.svelte` | ícone + badge danger | OK | §7.4/§11: `aria-label="Notificações (N não lidas)"`, clique → `/notificacoes` (rota da feature notificacoes) |
| `src/lib/components/ui/Skeleton.svelte` / `ErrorBanner.svelte` | skeleton + banner global | OK | RNF-04; `role="alert"` no banner; retry presente |
| `src/lib/utils/permissions.ts` | `canSeeLoans`, `canSeeMachines`, `canView`, `canEdit` | OK | **Não modificar** (dono: configurações/RBAC) — dashboard só consome; testes já existem (`permissions.test.ts`) |
| `src/lib/utils/format.ts` | `greeting`, `firstName`, `relativeTime`, `formatNumber` | OK | §14: `Intl.RelativeTimeFormat`/`NumberFormat('pt-BR')`; tests existem (`format.test.ts`) |
| `src/lib/config/menu.ts` | `moduleLabel` (usado no MyTasks) | OK | **Não modificar** (dono: configurações) — consumo apenas |
| `tests/unit/` | testes existentes (auth/format/permissions/sanity) | GAP | **zero testes de dashboard** (G-5..G-9) |

## 2. Aderência ao mockup (`docs/mokups/dashboard/requisitos.md`)

### Requisitos Funcionais
| ID | Status | Nota |
| :--- | :--- | :--- |
| RF-01 sidebar colapsável + filtro por papel | OK | `Sidebar.svelte` via `canView` + `menuItems` (hide, não disable); colapso persistido |
| RF-02 topbar breadcrumb/notificações/user menu | OK | `Topbar`/`Breadcrumb`/`NotificationsButton`/`UserMenu` |
| RF-03 redirect não autenticado | OK | guard `(app)/+layout.ts` com `?redirect=` |
| RF-04 saudação contextual | OK | `greeting()` local (<12/<18/else) |
| RF-05 "Minhas tarefas" em destaque | OK | `lg:col-span-2` |
| RF-06 KPIs | OK | encomendas, empréstimos* (RESP), máquinas* (RESP), notificações |
| RF-07 donut "Encomendas por status" | OK | público conforme mockup |
| RF-08 donut "Status das máquinas"* | OK | omitido do DOM se `!canSeeMachines` |
| RF-09 feed de atividade recente | OK | relativo pt-BR |
| RF-10 omitir blocos restritos | OK | hide (permissions.ts existente), grid reflow |
| RF-11 contador de notificações periódico | OK | polling 60s + pausa hidden (store) |
| RF-12 estados loading/empty/error/partial | GAP | loading inicial não exibido (G-2a/b) |

### Requisitos Não-Funcionais
| ID | Status | Nota |
| :--- | :--- | :--- |
| RNF-01 tema dark tokens | OK | classes token; hex só no donut (documentado, ver §8 obs. 5) |
| RNF-02 responsivo 1→3 colunas | OK | `grid-cols-1 lg:grid-cols-3` |
| RNF-03 blocos carregam independentes | OK* | summary é agregado (§4.2 recomendado) — falha parcial = erro total nos blocos; componentes aceitam `error` por bloco |
| RNF-04 skeleton em vez de spinner | GAP | skeleton existe e é fiel, mas só em retry (não na entrada) |
| RNF-05 sidebar colapsável persistida | OK | `fablab.sidebar.collapsed` |
| RNF-06 sem WebSocket — polling REST | OK | `stores/notifications.ts` |

### Contrato §4
| Item | Status | Nota |
| :--- | :--- | :--- |
| `GET /api/dashboard/summary` | OK | `fetchSummary`; tipagem exata |
| `GET /api/notifications/unread/count` | OK | `fetchUnreadCount`; `UnreadCount{count}` |
| `PATCH /api/tasks/{id}` `{done:true}` | OK | `completeTask` |
| 401 → limpa store + redirect sanitizado | **DUPLICADO** | interceptor global (client.ts) + handler local `dashboard.ts` (G-1) |
| timeout 15s / `AbortController` | OK | `client.ts` |

### Estados §5 / State1-4 do mockup
| Estado | Status | Nota |
| :--- | :--- | :--- |
| Loading (skeleton) | GAP | markup pronto e fiel; não é mostrado na entrada (G-2a/b) |
| Sucesso | OK | todos os blocos conforme RBAC |
| Empty | GAP | blocos vazios OK (MyTasks onboarding, KPI 0 "sem registros", donut trilha vazia, atividade) — **greeting** diverge (G-4) |
| Erro total/parcial | OK | ErrorBanner global + placeholders por bloco; KPI "indisponível"; retry global |
| RBAC restrito | OK | omitido do DOM; grid reflow |

### Critérios §12
Cobertos **exceto**: skeleton de entrada (G-2), greeting do Empty "Bem-vindo" (G-4) e contagem de testes (G-5..G-9).

## 3. Gaps encontrados (o que falta)

| ID | Arquivo | Gap | Impacto |
| :--- | :--- | :--- | :--- |
| G-1 | `frontend/src/lib/api/dashboard.ts` (modify) | Handler 401 local (`handle401` + `NOT_AUTH_REDIRECT` hardcoded `/dashboard`) redundante com o interceptor global do `client.ts` (auth-001). Violação da ressalva Security "centralizar sanitize/logout 401 em helper único (feature dona: dashboard/estoque)". O redirect local não passa por `sanitizeRedirect` e fixa o destino em `/dashboard`. | Médio — duplicação/consistência de expiração |
| G-2 | `+page.ts` + `+page.svelte` (modify) | Loading inicial sem skeleton: `load` do `+page.ts` aguarda a rede (bloqueante na SPA) → mockup State1 (skeletons em todos os blocos, sem shift) nunca aparece na entrada; skeleton só em retry. | Médio — fidelidade de estado (RF-12/RNF-04) |
| G-3 | `+page.svelte` (modify) | Sem refetch do summary em `visibilitychange` (mockup §10: "Summary — refetch ao ganhar foco"). Só o store de notificações escuta o evento hoje. | Médio — dados desatualizados ao voltar à aba |
| G-4 | `components/Greeting.svelte` (modify) | Empty do mockup (State2) usa "Bem-vindo, {nome}."; código renderiza sempre saudação temporal. | Baixo — texto do estado empty |
| G-5 | `frontend/tests/unit/notifications.test.ts` (create) | Polling (60s, pausa hidden, resume, cleanup, anti-duplicidade) sem testes — é a unidade com mais lógica síncrona pura. | Médio — DoD/qualidade |
| G-6 | `frontend/tests/unit/dashboard-api.test.ts` (create) | `api/dashboard.ts` sem cobertura (URLs, Bearer condicional, body PATCH, rethrow e **não**-redirecionamento local após G-1). | Médio — DoD/qualidade |
| G-7 | `frontend/tests/unit/dashboard-kpigrid.test.ts` (create) | RBAC por bloco do `KpiGrid` sem teste de componente (hide loans/machines por role, badge RESP). | Médio — DoD/qualidade |
| G-8 | `frontend/tests/unit/dashboard-mytasks.test.ts` (create) | Lógica do `MyTasks` (toggle otimista, restore em falha, `onRefetch`, contagem pendentes) sem cobertura. | Médio — DoD/qualidade |
| G-9 | `frontend/tests/unit/dashboard-page.test.ts` (create) | Estados da página (skeleton na entrada, sucesso, empty, erro, refetch em `visibilitychange`) sem teste de componente. | Médio — DoD/qualidade |

> **Não-gaps confirmados:** tipos e contrato §4; `stores/notifications.ts` (código do polling); shell (`+layout.svelte`); `NotificationsButton`; `KpiCard`/`KpiGrid` (RBAC hide); `OrdersChart`/`MachinesChart`/`DonutChart`/`ActivityFeed`; `format.ts`/`permissions.ts` (com testes); estados de erro dos blocos; a11y básica (`role="img"`, `role="alert"`, skip link, aria-labels).

## 4. Estratégia e ordem

Ordem (padrão dispatch: types → API → components → routes → states → RBAC → tests → DoD):

1. **API layer** — `dashboard.ts`: remover handler 401 duplicado e deixar o interceptor global agir
   (header Bearer condicional mantido; rethrow preservado para callers).
2. **Routes** — `+page.ts`: deixar de bloquear a renderização (load imediato; fetch passa à página).
3. **Routes** — `+page.svelte`: fetch no mount (`loading=true` inicial → skeleton fiel ao State1),
   refetch em `visibilitychange` (mockup §10), corrigir `isEmpty` (incluir `machinesByStatus`/`kpis`),
   remover o gate `settled`/dependência de `data`.
4. **Components** — `Greeting.svelte`: variante Empty "Bem-vindo, {nome}." (prop opcional).
5. **RBAC** — nenhuma task em `menu.ts`/`permissions.ts` (consumo apenas; dono configurações/produção).
6. **Tests** — G-5..G-9 (notifications, api/dashboard, KpiGrid, MyTasks, página).
7. **DoD** — `npm run test` + `npm run check` + passagem Validator/Security.

Nenhuma mudança em `docs/`, `docs/mokups/`, specs de sistema, `.opencode/state/**` nem arquivos de
outras features. Endpoints consumidos: apenas `GET /api/dashboard/summary`,
`GET /api/notifications/unread/count` e `PATCH /api/tasks/{id}`.

## 5. Checklist para o Validator

- [ ] `dashboard.html` 1-a-1: grid `lg:grid-cols-3` (tarefas 2 col + KPIs), donuts `32x32` em `md:grid-cols-2`, feed de atividade; badges RESP; espaçamento/pt-BR
- [ ] `dashboardState.html` 1-a-1: **Loading** (skeleton em todos os blocos, sem layout shift, greeting com 2 linhas) na primeira entrada **e** no retry
- [ ] **Empty**: greeting "Bem-vindo, {nome}." + sub; "Sem tarefas por aqui" + CTA "Explorar módulos"; KPIs `0` muted + "sem registros"; donuts com trilha vazia; "Nenhuma atividade recente."
- [ ] **Erro total**: ErrorBanner global no topo ("Não foi possível carregar o dashboard" + hint + "Tentar novamente"); blocos com placeholder; KPI "indisponível"; retry refaz o fetch
- [ ] **RBAC**: sem role 0/responsabilidades → Empréstimos e Máquinas **ausentes do DOM** (nunca `disabled`); responsável/Admin vê com badge RESP; grid reflow (`md:col-span-2` no OrdersChart); sidebar filtrando por role
- [ ] **KPIs**: valor + total (`3/8`) + delta com tom (success/danger/warn/muted); `formatNumber` pt-BR
- [ ] **Donuts**: proporção `count/total`, animação 300ms, legenda alinhada à direita, `role="img"` com `aria-label`
- [ ] **MyTasks**: check remove otimista e chama `PATCH`; falha restaura item e viabiliza nova tentativa; badge "N pendentes"; "Ver todas →"
- [ ] **Atividade**: ator + verbo + alvo (brand) + tempo relativo pt-BR
- [ ] **Polling**: 60s; aba oculta pausa; `visibilitychange` visível → refresh imediato; `stopPolling` limpa tudo; sem intervalos duplicados ao re-montar o shell (navegar e voltar ao `(app)`)
- [ ] **Summary**: refetch ao ganhar foco (`visibilitychange`) após G-3; título `Dashboard — FabLab Management`
- [ ] Responsividade 360px → 1920px (`grid-cols-1` mobile → `lg:grid-cols-3`)
- [ ] Acessibilidade: tab order; focus rings; `aria-hidden` nos skeletons; labels/aria-labels; skip link
- [ ] Testes novos verdes (`npm run test`, suíte completa) e `npm run check` sem erros de tipo

## 6. Checklist para o Security

- [ ] **Handler 401 único**: `dashboard.ts` sem `goto`/`logout`/`NOT_AUTH_REDIRECT` — decorrente do interceptor global com `sanitizeRedirect` (sem open redirect, sem loop, sem double logout)
- [ ] **Redirecionamento de expiração**: rota atual preservada e sanitizada (não fixa `/dashboard`); 401 em `/auth/login` e em fluxos públicos seguem sem redirect (opt-out do client)
- [ ] **Bearer**: token apenas em header `Authorization`; nunca em query string/body; sem `Bearer null` quando não autenticado
- [ ] **Polling sem leaks**: `stopPolling` em `onDestroy` da shell; flag `polling` impede múltiplos intervalos; timer nunca dispara com aba oculta
- [ ] **RBAC hide no DOM**: blocos restritos não chegam ao DOM para quem não tem permissão (nada de `hidden` com conteúdo presente)
- [ ] **Dados restricted no payload**: confirmar com backend que `GET /api/dashboard/summary` **não** devolve `loansOpen`/`machines*` a quem não é responsável/Admin (front só oculta; backend é autoridade — menor privilégio, rbac-matrix §7)
- [ ] **completeTask**: body fixo `{done:true}`; id vem da lista do próprio usuário; backend deve validar vínculo (rbac-matrix: concluir tarefa só se atribuído)
- [ ] **XSS**: texto de tarefas/atividade renderizado como texto (Svelte escapa); confirmar ausência de `{@html}`
- [ ] **Sem segredos**: nenhuma credencial/chave em código ou log; erros de UI genéricos (sem expor detalhes de infra)
- [ ] Sessão: expiração segue o fluxo da feature auth (env `VITE_API_BASE_URL` https em prod — ressalva Security da auth, mantida)

## 7. Conflitos de escopo com outras features (arquivos compartilhados)

| Arquivo | Dono lógico | Conflito |
| :--- | :--- | :--- |
| `src/lib/api/dashboard.ts` | **dashboard** | Sem outro dono. Handler 401 removido aqui; `estoque/stock/request.ts` e `rh.ts` mantêm o padrão duplicado (remoção na feature dona — não paralelo). |
| `src/lib/config/menu.ts` + `src/lib/utils/permissions.ts` | configurações/RBAC (producao/config) | **NÃO TOCAR** (ConflictMap). Dashboard só consome `canSeeLoans`/`canSeeMachines`/`canView`/`moduleLabel`. Nenhuma task referenciada. |
| `src/routes/(app)/+layout.svelte` | app shell (auth criou) | Dashboard já monta polling (b5fde2c). **Nenhuma alteração planejada nesta gap set.** Se um dia MyTasks precisar de toast global, será infra aparte (ToastHost hoje vive só em `estoque/+layout.svelte` — fora do escopo; mover para o shell exigiria coordenação com estoque). |
| `src/lib/stores/notifications.ts` | **dashboard** (origem: mockup dashboard) / futuro: notificacoes | Polling implementado e consumido pelo shell. A feature `notificacoes` (page de lista/preferências) deve **reutilizar** o store, não duplicar. Flag para o Orchestrator. |
| `src/routes/(app)/dashboard/+page.ts` / `+page.svelte` | **dashboard** | Sem conflito. |
| `src/lib/components/layout/NotificationsButton.svelte` | dashboard (shell) / notificacoes (clique → `/notificacoes`) | Clicar hoje navega para rota ainda inexistente (feature notificacoes) — dead-link temporário, fora do escopo. |

## 8. Observações e riscos (não geram task nesta feature)

1. **Skew de nomenclatura requirements.md vs mockup**: requirements cita bloco restrito "Encomendas em
   aberto"; o mockup aprovado restringe o **KPI "Empréstimos em aberto"** (RESP) e mantém o donut
   "Encomendas por status" **público**. O código segue o mockup (fonte visual). Reconciliar a spec
   com o Orchestrator (sem alterar código).
2. **rbac-matrix (sistema)** diz Recrutando(4) sem acesso a Summary/KPIs, enquanto o mockup §7.3 e
   `VIEW_RULES.dashboard` incluem role 4. Front segue mockup; autorização real é do backend.
3. **Erro parcial por bloco**: com `GET /dashboard/summary` (recomendado no mockup §4.2), uma falha
   derruba todos os blocos juntos — a regra "banner global só com ≥2 falhas" perde sentido prático
   no arranjo atual (componentes já aceitam `error` por bloco para o futuro). Aceito.
4. **Feedback de falha no MyTasks é silencioso** (restaura o item, sem toast): mockup não exige toast
   e ToastHost não está no shell. Decisão: manter restore silencioso; toast global fica para passe de
   infra (mover ToastHost para `(app)/+layout.svelte` e remover do `estoque/+layout` — coordenação
   com estoque).
5. **Hex no DonutChart** (`COLOR_HEX`, trilha `#1a2235`): exceção documentada pelo design-system
   (§Donut chart) e pelo mockup §9 — necessário para `stroke` do SVG; não é violação de token.
   Obs.: `#1a2235` é o token visual real (login/estoque) — alinhado ao token drift já reportado.
6. **Dead-links planejados**: "Ver todas →" (`/producao/tarefas`) e "Ver histórico →"
   (`/notificacoes/revisao`) apontam para features futuras; NotificationsButton → `/notificacoes`.
   Aceitos (fora do escopo do dashboard).
7. **Auth já commitada** (5b68329): `client.ts` com interceptor 401 + `sanitizeRedirect`; a remoção do
   handler local (G-1) é segura inclusive para 401 concorrentes (flag anti-duplicidade do client).
8. **`+page.ts` sem fetch** (pós G-2a): SvelteKit segue a navegação sem esperar rede; o `load` de
   `(app)/+layout.ts` continua garantindo guard + fetchMe. Sem regressão de auth esperada.

## 9. Status e decisão

- **Status da feature:** PLANEJADA (gaps G-1..G-9 mapeados; 9 tasks em `tasks.md`, `dash-001..009`).
- **Próximo passo:** Builder executa `dash-001..dash-009` (não paralelo com configurações/produção nas
  camadas compartilhadas), depois Validator (checklist §5) e Security (checklist §6).
# DESPACHO — Planner (Frontend) · DASHBOARD · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **gap analysis** da feature `dashboard` contra o código real.
> Alvo: `.opencode/specs/frontend/features/dashboard/` → `plan.md` + `tasks.md` (tasks = **o que falta**, com DoD).

---

## Contexto (ler antes)

- `.opencode/state/frontend-orchestration.json` — feature `dashboard`: SPEC_CADASTRADA, **mockup APROVADO pelo humano** (20/09, REVISAO_HUMANA_CONCLUIDA), tasks vazias.
- Spec da feature: `.opencode/specs/frontend/features/dashboard/requirements.md` (rota `/(app)/dashboard`, blocos: greeting, KPIs, encomendas em aberto, status máquinas, minhas tarefas, atividade recente; `GET /api/dashboard/summary`; `GET /api/notifications/unread/count` polling 60s pausa em `document.hidden`; RBAC por bloco).
- Mockup aprovado (fonte da verdade visual): `docs/mokups/dashboard/` (`dashboard.html`, `dashboardState.html`, `requisitos.md`).
- **Código real já existe** no commit `b5fde2c` (app shell + dashboard). O Planner NÃO re-planeja do zero: **reconcilia com o código real** (padrão auth).
- **AUTH já finalizada** (commit `5b68329`): `client.ts` agora tem interceptor global 401 + `sanitizeRedirect`. Isso torna o handler 401 local de `dashboard.ts` **redundante** (Security apontou duplicação — ver dívida).
- Referência de formato: `.opencode/specs/frontend/features/auth/{plan,tasks}.md` (o par auth é o exemplo mais recente de plan/tasks frontend).
- Specs de sistema: `.opencode/specs/frontend/{architecture,conventions,design-system}.md`, `.opencode/specs/system/rbac-matrix.md`.

## Inventário real (conferir, não confiar)

- `frontend/src/routes/(app)/+layout.ts` — guard + fetchMe (auth)
- `frontend/src/routes/(app)/+layout.svelte` — app shell: Sidebar/Topbar/Breadcrumb/UserMenu/**NotificationsButton** (o dropdown de notificações é ESC OPO do mockup do dashboard — não redesenhado; o polling de unread pode morar aqui ou no dashboard, decidir)
- `frontend/src/routes/(app)/dashboard/+page.svelte` + `+page.ts` (load)
- `frontend/src/routes/(app)/dashboard/components/`: `Greeting.svelte`, `KpiGrid.svelte`, `KpiCard.svelte`, `OrdersChart.svelte`, `DonutChart.svelte`, `MachinesChart.svelte`, `MyTasks.svelte`, `ActivityFeed.svelte`
- `frontend/src/lib/api/dashboard.ts` — `fetchSummary`, `fetchUnreadCount`, `completeTask`; **handler 401 local com `NOT_AUTH_REDIRECT` hardcoded** (não sanitizado, duplicado do interceptor global)
- `frontend/src/lib/types/dashboard.ts` — `DashboardSummary`, `UnreadCount` (conferir existência)

## Gaps prováveis para o Planner avaliar (confirmar, não assumir)

1. `dashboard.ts`: handler 401 local agora é redundante com o interceptor global (`client.ts`) — task `modify` para remover `handle401`/`NOT_AUTH_REDIRECT` e usar headers sem duplicação (manter compat com `completeTask`).
2. Polling `unread/count` 60s + pausa em `document.hidden` (requirements) — conferir se implementado e cobrir (task `create`/`modify` se faltar; teste com fake timers se viável).
3. Estados loading/success/empty/error no dashboard (mockup `dashboardState.html`) — conferir skeleton/empty.
4. RBAC por bloco (ocultar "Encomendas em aberto" e "Status das máquinas" fora de responsável/Admin) — conferir uso de `canView`/`permissions.ts`.
5. `completeTask` (PATCH `/tasks/{id}`) sem confirmação/erro amigável? Conferir UX do `MyTasks.svelte` contra mockup.
6. Testes de dashboard: **não há testes** (padrão: testes de componente/API como auth-004..007) — avaliar tasks de teste para as unidades com lógica (api/dashboard, sanitize/polling, RBAC por bloco).

## Regras

1. **Sem mockup aprovado → bloqueado (`MOCKUP_REQUIRED`).** Mockup APROVADO — só bloquear se alguma parte estiver incompleta para planejar.
2. Método: inventariar código real → comparar com requirements + mockup → listar **gaps** arquivo a arquivo → tasks atômicas (1 arquivo ≡ 1 task; `id` `dash-0XX`, `titulo` PT, `arquivo`, `acao` create|modify, `detalhes`, `dependencias`, `verificar`) → ordem types → API → components → routes → states → RBAC → tests → DoD.
3. **Não escrever código**; **não alterar `docs/`, `docs/mokups/` nem código real**.
4. Endpoints: só `GET /api/dashboard/summary`, `GET /api/notifications/unread/count`, `PATCH /api/tasks/{id}`. Não inventar.
5. ⚠️ **NÃO planejar alterações em** `src/lib/config/menu.ts` nem `src/lib/utils/permissions.ts` (donos: producao/configuracoes — ConflictMap). Se o dashboard precisar de algo lá, sinalizar conflito no plan.md em vez de task.
6. UI text pt-BR; tokens design-system; RBAC hide-não-disable.

## Passos

- **P1** Inventariar o código real (acima) e conferir cada arquivo.
- **P2** Comparar com requirements + mockup `docs/mokups/dashboard/requisitos.md` (estados, polling, RBAC, cards).
- **P3** Gerar `plan.md` (criar): visão do estado real (implementado x gap), estratégia dos gaps e dependências (dashboard usa shell; NÃO tocar menu/permissions), conflitos de escopo (arquivos compartilhados), checklists Validator/Security (incluir: polling sem leaks/loop, RBAC por bloco, redirect sanitizado, remoção de handler 401 duplicado).
- **P4** Gerar `tasks.md` (criar): tasks atômicas do **que falta** apenas. Se algo já está aderente, indicar explicitamente.
- **P5** **NÃO alterar estado** — apenas reportar; o Orchestrator atualiza `frontend-orchestration.json`.

## Relatório esperado (responda com)

1. Inventário do que existe hoje (arquivos-chave) e status de aderência ao mockup/requirements.
2. Gaps encontrados (ou "sem gaps").
3. Lista de tasks propostas (`dash-001..`) com `arquivo` + `acao`.
4. Conflitos de escopo (arquivos compartilhados) se houver.
5. Checklists Validator/Security sugeridos.
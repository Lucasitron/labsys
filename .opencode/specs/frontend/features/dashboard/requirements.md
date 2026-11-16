# Dashboard — Frontend Requirements

## Route
- `/(app)/dashboard`

## Access
- Todos autenticados (RBAC-restrito por bloco)

## Screens
1. **Dashboard** — `/(app)/dashboard`

## Data Requirements
- `GET /api/dashboard/summary` (agregado)
- `GET /api/notifications/unread/count` (polling 60s; pausa em `document.hidden`)
- **Bloco "Minhas tarefas"** (Dívida 5): `GET /api/dashboard/tasks` + `PATCH /api/dashboard/tasks/{id}` (`{done:true}`); ids `rh-cert-*` — conclusão **em memória no MVP**, dono real = Produção quando existir (degradar p/ lista vazia)

## Envelope do resumo (Dívida 2 — naming PT canônico)
- `GET /api/dashboard/summary` devolve: `tasks[]`, `kpis{ ordersActive, loansOpen, machinesActive, notificationsUnread }`, `ordersByStatus[]`, `machinesByStatus[]`, `activity[]`
- `kpi.delta/deltaTone` e `task.dueLabel/urgent` **servidos prontos** pelo backend (o front não traduz)
- Campos restritos (`loansOpen`, `machinesActive`, `machinesByStatus`) podem ser **omitidos** p/ não-responsável — front aplica RBAC complementar (remove, nunca esconde-info)
- `StatusSlice.color` ∈ `brand|warn|success|danger|muted|neutral|gray`

## States
- Loading (skeletons), Success (com blocos RBAC), Empty, Error

## Blocos
1. **Hero greeting** — `Bom dia/tarde/noite, {nome}` (+ data)
2. **KPIs** — Encomendas ativas, Itens com estoque baixo, Horas pendentes, Alertas ativos
3. **Encomendas em aberto** — lista compacta com donut de status
4. **Status das máquinas** — lista com tone por estado
5. **Minhas tarefas** — tarefas atribuídas
6. **Atividade recente** — feed

## RBAC
- "Encomendas em aberto" e "Status das máquinas" escondidos se não responsável/Admin
- Sidebar filtra por role (remove, não desabilita)

## Details
- Ver `/docs/mokups/dashboard/requisitos.md`

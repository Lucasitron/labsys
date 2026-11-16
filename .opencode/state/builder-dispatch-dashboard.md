# DESPACHO — Builder (Frontend) · DASHBOARD (dash-001..dash-009) · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **implementar as 9 tasks do `dashboard`** em `dev/frontend`
> (branch atual). Tasks = o que falta; o resto já é aderente (NÃO reimplementar).

---

## Contexto (ler antes)

- Tasks: `.opencode/specs/frontend/features/dashboard/tasks.md` (`dash-001..dash-009`, 1 arquivo ≡ 1 task — **executar em ordem numérica**)
- Estratégia/plan: `.opencode/specs/frontend/features/dashboard/plan.md` (§gaps, §estratégia, §5 Validator, §6 Security)
- Mockup aprovado (fonte da verdade visual): `docs/mokups/dashboard/` (`dashboard.html`, `dashboardState.html`)
- Auth finalizada (`5b68329`): `client.ts` tem interceptor global 401 + `sanitizeRedirect` — o dashboard **só consome** (não edita `client.ts`)

## Arquivos (o Builder toca SOMENTE estes)

| Task | Arquivo | Acao | DoD |
|---|---|---|---|
| dash-001 | `frontend/src/lib/api/dashboard.ts` | modify | remover `handle401`/`NOT_AUTH_REDIRECT`/imports órfãos (`goto`, `ApiError`, `logout`); 401 delegado ao interceptor global; manter `bearer()`; rethrow dos demais erros |
| dash-002 | `frontend/src/routes/(app)/dashboard/+page.ts` | modify | `load` retorna imediatamente (sem aguardar rede) — summary sai de store/estado inicial |
| dash-003 | `frontend/src/routes/(app)/dashboard/+page.svelte` | modify | fetch no mount (skeleton inicial entra), refetch summary em `visibilitychange`, `isEmpty` considera `machinesByStatus`/`kpis`, remover gate `settled`/duplicação |
| dash-004 | `frontend/src/routes/(app)/dashboard/components/Greeting.svelte` | modify | estado empty do mockup: "Bem-vindo, {nome}." (sem saudação temporal) |
| dash-005 | `frontend/tests/unit/dashboard-api.test.ts` | create | URLs/Bearer/PATCH done/rethrow; **sem redirect local** (interceptor global cobre) |
| dash-006 | `frontend/tests/unit/notifications.test.ts` | create | polling 60s + pausa `document.hidden` + resume/refresh em `visibilitychange` + cleanup + sem timer duplicado (fake timers) |
| dash-007 | `frontend/tests/unit/dashboard-kpigrid.test.ts` | create | RBAC hide por role (blocos ausentes do DOM) + badge RESP |
| dash-008 | `frontend/tests/unit/dashboard-mytasks.test.ts` | create | toggle otimista + PATCH + restore em falha + pending + empty/error |
| dash-009 | `frontend/tests/unit/dashboard-page.test.ts` | create | 4 estados (loading/success/empty/error) + refetch por foco + grid reflow |

## Regras (Builder padrao)

1. 1 task por vez, em ordem; commit **NÃO** (o Orchestrator commita ao final) — apenas implementar.
2. **NUNCA** tocar: `docs/`, `docs/mokups/`, specs de sistema, `client.ts`, `menu.ts`, `permissions.ts`, `stores/auth.ts`, `(app)/+layout.ts`, arquivos de outras features (`estoque/**`, `stock/request.ts`, `rh.ts`, `notificacoes` futuras).
3. `stores/notifications.ts` (polling) já existe no shell — **reutilizar**, não duplicar.
4. Endpoints reais apenas: `GET /api/dashboard/summary`, `GET /api/notifications/unread/count`, `PATCH /api/tasks/{id}` (body `{done:true}`). Não inventar.
5. UI pt-BR; tokens design-system; RBAC **hide** (nunca disable); a11y mantida.
6. Ao final: `npm run test` (63 existentes + novos verdes) e `npm run check` SEM erros novos (baseline: 12 erros/15 warnings/7 arquivos, todos em `estoque/**` e `ui/Chip.svelte`).
7. Se alguma task depender de decisão ausente → reportar, não inventar.

## Relatório esperado (responda com)

- Para cada task `dash-00X`: ✅/❌ + evidência (arquivo:linha + como verificou)
- Resultado final: `npm run test` (contagem) + `npm run check` (resumo)
- Pontos de atenção / desvios do mockup, se houver
# DESPACHO — Planner (Frontend) · NOTIFICACOES · 21/09/2026

> Objetivo: materializar **plan.md + tasks.md** da feature `notificacoes` (gap analysis vs código real).
> **Princípio código mínimo (specs 21/09):** plano enxuto — reuso > create; sem código especulativo;
> cada task deve resolver caso real do requirements/mockup. NÃO implementar código.

## Entradas
- **Requirements:** `.opencode/specs/frontend/features/notificacoes/requirements.md` (ler integral)
- **Mockup aprovado:** `docs/mokups/notificacoes/` (full.html · página central lista+filtros `?page&type&status` + admin preferencias/history, 5 estados + rbac hide; dropdown topbar permanece no dash)
- **Código real:** `frontend/src/lib/stores/notifications.ts` (store do dashboard — REUTILIZAR), `frontend/src/lib/components/layout/NotificationsButton.svelte` (dropdown topbar — fora de escopo), `frontend/src/lib/config/menu.ts` (item Notificações já presente? conferir), `frontend/src/lib/api/dashboard.ts` (se expor notificações), `$lib/components/ui/*`
- **Estado Cronograma:** `.opencode/state/frontend-orchestration.json` (notificacoes: SPEC_CADASTRADA; dropdown no mockup do dashboard)

## Produtos exigidos
1. **`plan.md`** em `.opencode/specs/frontend/features/notificacoes/`:
   - Estado real arquivo-a-arquivo; gaps G-1..G-N (ex.: store vs API de lista central, filtros `?page&type&status`, admin preferências/histórico, RBAC hide, token elevated `#111a2c`, estados)
   - Camadas/arquivos + ConflictMap (menu.ts/permissions.ts — livres atualmente, consumir)
   - **Seção "Decisões de enxugamento"** (o que foi REUSADO — store/NotificationsButton/UI kit — e o que foi explicitamente NÃO planejado por não resolver caso real)
   - Ordem em blocos + checklists Validator §5 e Security §6 + riscos (contrato 🟡/🔴 — notificação-service) + critérios de entrega
2. **`tasks.md`**: tasks atômicas 1 arquivo=1 task (`not-001..not-NNN`), com arquivo/conteúdo/saída/DoD/deps/prioridade; marcando endpoints 🟡/🔴; bloco de testes (unit; e2e condicional) e tasks de correção previstas.

## Regras
- NÃO editar `docs/**`; NÃO implementar; NÃO commitar. Ler `frontend/src` livremente; alinhar ao padrão vigente (auth/dashboard/producao/configuracoes/estoque) e ao princípio código mínimo.
- Conflito de escopo → CONFLICT_REPORT. Devolver decisões de PO/backend sem inventar endpoint.

## Relatório final
- Caminhos criados; nº de gaps e de tasks; resumo dos blocos; **decisões de enxugamento**; riscos/conflitos; decisões que exigem backend/PO.
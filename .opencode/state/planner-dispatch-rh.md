# DESPACHO — Planner (Frontend) · RH / PESSOAS · 21/09/2026

> Objetivo: materializar **plan.md + tasks.md** da feature `rh` (12 telas; gap analysis vs código real).
> **Princípio código mínimo:** reuso > create; nada especulativo; cada task resolve caso real do requirements/mockup.
> NÃO implementar código.

## Entradas
- **Requirements:** `.opencode/specs/frontend/features/rh/requirements.md` (ler integral)
- **Mockup aprovado:** `docs/mokups/pessoas/` (full.html · novasTelasEstados.html · requisitos.md · instrunctions.md) — 12 telas, rotas `/pessoas/*`
- **Código real:** `frontend/src/lib/api/rh.ts` (único existente — mapear o que já cobre), `frontend/src/lib/api/client.ts`, `frontend/src/lib/components/ui/*`, `frontend/src/lib/config/menu.ts` (item Pessoas? conferir), `frontend/src/lib/utils/permissions.ts`, `frontend/src/lib/utils/format.ts`
- **Estado:** `.opencode/state/frontend-orchestration.json` (rh → SPEC_CADASTRADA; mockupPath `docs/mokups/pessoas/`)
- **Padrões de plan/tasks:** `.opencode/specs/frontend/features/{estoque,notificacoes}/{plan,tasks}.md`

## Produtos exigidos
1. **`plan.md`** em `.opencode/specs/frontend/features/rh/`:
   - Estado real arquivo-a-arquivo; gaps G-1..G-N (rotas `/pessoas/**` inexistentes?; `api/rh.ts` cobertura; Kanban processo-seletivo; registro-horas com extrato do backend; treinamentos; certificados Dívida 4; níveis Admin-only; RBAC hide por responsável; Dívida 6 Tutor×Instrutor)
   - Camadas/arquivos + ConflictMap (`menu.ts` — **compartilhado**; `permissions.ts` — compartilhado)
   - **Seção "Decisões de enxugamento"** (o que reusar; o que NÃO planejar por não resolver caso real — ex.: não somar horas no client (backend serve `ExtratoMensalHorasEvent`); certificados só se houver mockup/contrato)
   - Blocos + checklists Validator §5 e Security §6 + riscos (contrato 🟡/🔴) + critérios de entrega
2. **`tasks.md`**: tasks atômicas `rh-001..rh-NNN` (1 arquivo=1 task) com arquivo/conteúdo/saída/DoD/deps/prioridade; marcar endpoints 🟡/🔴; bloco de testes (unit; e2e condicional) e previsão de correções.

## Regras
- NÃO editar `docs/**`; NÃO implementar; NÃO commitar; alinhar ao padrão vigente e ao princípio código mínimo.
- Conflito de escopo → CONFLICT_REPORT. Decisões de PO/backend sem inventar endpoint (padrão D-x/R-x).
- Atenção: `menu.ts` e `permissions.ts` são compartilhados — planejar execução **sequencial** com outras features.

## Relatório final (mensagem única)
- Caminhos criados; nº de gaps; nº de tasks; resumo dos blocos; **decisões de enxugamento**; riscos/conflitos; decisões que exigem backend/PO.
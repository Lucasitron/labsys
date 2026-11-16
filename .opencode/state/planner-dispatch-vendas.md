# DESPACHO — Planner (Frontend) · VENDAS & CRM · 23/09/2026

> Objetivo: materializar **plan.md + tasks.md** da feature `vendas` (gap analysis vs código real).
> **Princípio código mínimo:** reuso > create; nada especulativo; cada task resolve caso real do mockup/spec.
> NÃO implementar código.

## Entradas (ordem de autoridade em caso de divergência)
1. **Mockup aprovado:** `docs/mokups/vendas/` (full.html · requisitos.md · instrunctions.md) — 11 telas
2. **Contrato travado:** `docs/contract.gap.md` §5.3 (decisões definitivas: PT `/clientes /orcamentos /encomendas /marketplace /interacoes(timeline) /tarefas-marketing /tags`; `/vendas/interacoes` REMOVIDA; nova `/vendas/solicitacoes`; encomenda nasce na Fila do Kanban; orçamento novo em página cheia; RBAC edição = criador + Admin; Kanban por clique `PUT /encomendas/{id}/kanban`)
3. **Requirements atual:** `.opencode/specs/frontend/features/vendas/requirements.md` (DESATUALIZADO — cita "marketplace/CRM mockup pendente" e 4 rotas; reconciliar com mockup+§5.3 e registrar a limpeza necessária, sem editar o requirements — apenas documentar no plan)
4. **Código real:** `frontend/src/lib/api/` (há `api/vendas*`? mapear), `frontend/src/lib/config/menu.ts` (item `Vendas & CRM` raiz já existe, linha ~48), `frontend/src/lib/components/ui/*`, `frontend/src/lib/utils/{permissions,format}.ts`
- **Estado:** `.opencode/state/frontend-orchestration.json` (vendas → SPEC_CADASTRADA)
- **Padrões:** `.opencode/specs/frontend/features/{rh,estoque,notificacoes}/{plan,tasks}.md`

## Produtos exigidos
1. **`plan.md`** em `.opencode/specs/frontend/features/vendas/`: estado real arquivo-a-arquivo; gaps G-1..G-N (rotas `/vendas/**` existem?; API layer?; Kanban encomendas reusado por produção — como compartilhar sem duplicar; marketplace Admin; solicitações; RBAC por criador/atribuído); camadas/arquivos; ConflictMap (`menu.ts` compartilhado ⚠️ sequencial — livre agora; `permissions.ts`); **Seção "Decisões de enxugamento"**; blocos; checklists Validator §5 e Security §6; riscos (tudo 🔴 — `vendas-service` scaffold vazio); critérios de entrega.
2. **`tasks.md`**: tasks atômicas `vnd-001..vnd-NNN` (1 arquivo=1 task) com arquivo/conteúdo/saída/DoD/deps/prioridade; endpoints 🔴/🟡; bloco de testes (unit; e2e condicional).

## Regras
- NÃO editar `docs/**` nem `requirements.md`; NÃO implementar; NÃO commitar; princípio código mínimo.
- Conflito de escopo → CONFLICT_REPORT. Decisões sem inventar endpoint (padrão D-x/R-x).
- Atenção: Kanban de encomendas será **reusado pela Produção** (sem tela duplicada) — planejar componente compartilhado ou dono único.

## Relatório final (mensagem única)
- Caminhos criados; nº de gaps; nº de tasks; resumo dos blocos; **decisões de enxugamento**; reconciliação requirements×mockup×§5.3; riscos/conflitos; decisões backend/PO.
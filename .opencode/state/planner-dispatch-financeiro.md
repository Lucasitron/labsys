# DESPACHO — Planner (Frontend) · FINANCEIRO · 23/09/2026

> Objetivo: materializar **plan.md + tasks.md** da feature `financeiro` (gap analysis vs código real).
> **Princípio código mínimo:** reuso > create; nada especulativo; cada task resolve caso real do mockup/spec.
> NÃO implementar código.

## Entradas (ordem de autoridade em caso de divergência)
1. **Mockup aprovado:** `docs/mokups/financeiro/` (full.html · requisitos.md · instrunctions.md) — 7 telas
2. **Contrato travado:** `docs/contract.gap.md` §5.4 (decisões definitivas: PT `/lancamentos /categorias /doacoes-recursos /valores-hora /parametros-overhead /fechamento-encomenda /solicitacoes-compra /custos-encomenda /relatorios/*`; **Admin-only total**; contas/KPIs = vistas derivadas de `lancamentos`; status canônicos PT servidos; custeio com congelamento + nova ordem; compras informativas; filtros `?periodo=` 🟡)
3. **Requirements atual:** `.opencode/specs/frontend/features/financeiro/requirements.md` (5 rotas: lancamentos/contas/custeio/doacoes/relatorios — reconciliar com mockup+§5.4 e documentar no plan, SEM editar o requirements)
4. **Código real:** `frontend/src/lib/api/` (há algo de financeiro? mapear), `frontend/src/lib/config/menu.ts` (item Financeiro? conferir), `frontend/src/lib/components/ui/*` (Timeline/MoneyInput reusáveis), `frontend/src/lib/utils/{permissions,format}.ts`
- **Estado:** `.opencode/state/frontend-orchestration.json` (financeiro → SPEC_CADASTRADA)
- **Padrões:** `.opencode/specs/frontend/features/{vendas,rh}/{plan,tasks}.md`

## Produtos exigidos
1. **`plan.md`** em `.opencode/specs/frontend/features/financeiro/`: estado real; gaps G-1..G-N; camadas/arquivos; ConflictMap (`menu.ts` ⚠️ — último a tocar; `permissions.ts` — `isAdmin` já existe, provavelmente sem helpers novos); **Seção "Decisões de enxugamento"** (ex.: sem soma client — totais/resumo servidos; sem CRUD de contas — vistas derivadas; PDF fora do MVP?; sem store); blocos; checklists Validator §5 e Security §6; riscos (tudo 🔴 — `financeiro-service` scaffold); critérios de entrega.
2. **`tasks.md`**: tasks atômicas `fin-001..fin-NNN` (1 arquivo=1 task) com arquivo/conteúdo/saída/DoD/deps/prioridade; endpoints 🔴/🟡; bloco de testes (unit; e2e condicional).

## Regras
- NÃO editar `docs/**` nem `requirements.md`; NÃO implementar; NÃO commitar; princípio código mínimo.
- Conflito de escopo → CONFLICT_REPORT. Decisões sem inventar endpoint (padrão D-x/R-x).
- Atenção: módulo **inteiramente Admin-only** (guarda + hide total, padrão `configuracoes/sistema` + `pessoas/niveis`).

## Relatório final (mensagem única)
- Caminhos criados; nº de gaps; nº de tasks; resumo dos blocos; **decisões de enxugamento**; reconciliação requirements×mockup×§5.4; riscos/conflitos; decisões backend/PO.
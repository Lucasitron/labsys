# DESPACHO — Validator (Frontend) · VENDAS · 23/09/2026

> Validar a feature `vendas` implementada (commits ed7dbcd · 7e3eb8b · 0a00ccf · 83ff024 · 3ad1396 · 7beb58b) contra spec/mockup.
> **Princípio código mínimo: verificar pertinência/código mínimo; sinalizar código não pertinente.**

## Contexto
- Workspace `/home/lucasitron/Documentos/05-dev/labSys`; branch `dev/frontend` (NÃO tocar em nada; somente leitura)

## Entradas
- Spec: `.opencode/specs/frontend/features/vendas/{requirements,plan,tasks}.md` (32 tasks vnd-001..032; reconciliação requirements×mockup×§5.3 no plan §8)
- Mockup aprovado: `docs/mokups/vendas/` (full.html 11 telas + requisitos.md § testids)
- Contrato: `docs/contract.gap.md` §5.3 (decisões travadas)
- Código: `src/lib/{types/vendas.ts, api/vendas/*.ts (7), utils/vendas-{status,format}.ts, utils/permissions.ts(helpers), components/ui/{Timeline,MoneyInput}.svelte, config/menu.ts(submenu)}` · `src/routes/(app)/vendas/**` (+layout.ts, clientes/**, orcamentos/**, encomendas/**, marketplace, marketing, solicitacoes) · testes `frontend/tests/unit/vendas-*.test.ts`
- DoD/checklists em plan.md §5 (se existir) + tasks DoD

## Critérios
1. 32 tasks (1 arquivo=1 task); tipos sem any; APIs PT query mínima; `duplicarOrcamento` NÃO criado (D-2); sem `proximoEstagio|contarPorColuna`; sem `somar|totalizar|liquido(`; sem rota `/vendas/interacoes`
2. Guarda `vendas/+layout.ts` (302; solicitacoes 303 p/ !canDecide); RBAC **hide** (Editar vs "Sugerir alteração" por `canEditVendas(record)`; ausência de DOM); submenu sem canSee (Recrutando barrado na guarda)
3. URL fonte de verdade (clientes `?search=&tipo=&tags=&ordenar=&page=&pageSize=`; orcamentos `?tab=`; encomendas `?status_kanban=`; marketplace/marketing/solicitacoes); estados completos; paginação server-side; Kanban coluna do servido (nunca recalculada); 409 → toast
4. Detalhes com tabs deep-linkáveis + erro parcial; banner conversão só Aprovado sem encomenda (some via refetch); KPIs/totais servidos (nunca somados; preview de líquido rotulado); testids do mockup; pt-BR; aria-label; SEM Exportar CSV
5. Testes: 451 verdes; cobertura coerente (70 novos); sem frágeis; build/check ok
6. **Código mínimo:** duplicações/speculativo → ressalva (ex.: `?editar=`/`?duplicar=` no novo-cliente em vez de rotas; `ClienteComKpis` local; bulk solicitar agregada; `EncomendaDetalhe` local)

## Produto
- **UM output completo inline** (proibido resumir/encaminhar): veredito + checklist por critério (✅/❌/⚠️ + evidência arquivo:linha) + ressalvas por severidade (Alta/Média/Baixa) com correção mínima + verificação: `npm run test` (451), `npm run check`, `npm run build`.

## Regras
- Somente leitura; **NÃO editar/commitar**; sem e2e (sem Playwright).
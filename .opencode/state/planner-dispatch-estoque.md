# DESPACHO — Planner (Frontend) · ESTOQUE · 21/09/2026

> Preparado pelo Orchestrator. Objetivo: materializar **plan.md + tasks.md** da feature `estoque`
> a partir do `requirements.md` existente, do mockup aprovado e do **código real** (gap analysis).
> NÃO implementar código. Working tree: branch `dev/frontend` (NÃO rode git checkout/switch).

## Entradas
- **Requirements (existe):** `.opencode/specs/frontend/features/estoque/requirements.md` (10 rotas, 13 telas, `/api/estoque/**` PT canônico, RBAC View Admin/Bolsista/Voluntário/Estagiário · Edit Admin OU responsável · Recrutando bloqueado)
- **Mockup (aprovado 20/09):** `docs/mokups/estoque/` — 19 telas: `estoque.html`, `estoqueEstados.html`, `itenDatails.html`, `editar-item.html`, `entradas.html`, `entrada-detalhe.html`, `saida-detalhe.html`, `emprestimo-detalhe.html`, `fornecedor-novo.html`, `localizacao-novo.html`, `full.html`, `requisitos.md`, `instrunctions.md` (NÃO editar docs)
- **Código real (gap analysis):** `frontend/src/routes/(app)/estoque/**` (rotas/itens/entradas/saidas parciais), `frontend/src/lib/api/stock/**` + `frontend/src/lib/api/rh.ts`, `frontend/src/lib/types/stock.ts`, `frontend/src/lib/components/estoque/{ItemPicker,MovementList,PeoplePicker}.svelte`, `frontend/src/lib/config/menu.ts` (submenu Estoque), `frontend/src/lib/utils/permissions.ts` (helpers de estoque/`VIEW_RULES`/`EDIT_RULES`)
- **Dívidas conhecidas:** `docs/contract.gap.md` §5.6 — Dívida 1 (endpoints assumidos: `GET /movimentacoes/{id}`, `POST /fornecedores`, `POST /localizacoes`, `GET /emprestimos?status=`, `GET /itens/{id}/emprestimos`) e Dívida 2 (spec em EN `/api/stock/**` vs PT `/api/estoque/**` canônico). Mockup deve ser unificado ao PT quando o service existir.
- **Estado do Orchestrator:** `.opencode/state/frontend-orchestration.json` (estoque: SPEC_CADASTRADA; mockup 19 telas aprovado; decisões PO: POST /stock/suppliers, POST /stock/locations, GET /stock/movements/{id} são responsabilidade do BACKEND — front só consome via gateway; rota `/estoque/emprestimos/[id]` MANTIDA)

## Produtos exigidos
1. **`plan.md`** em `.opencode/specs/frontend/features/estoque/`:
   - Estado real arquivo-a-arquivo (o que existe, o que falta, o que está em EN a migrar)
   - **Gaps G-1..G-N** (ex.: `/api/stock/**`→`/api/estoque/**`; rotas faltantes — `editar-item`, detalhes de entrada/saída/empréstimo/devolução, fornecedores, localizações, BOM; telas do mockup não implementadas; estados Loading/Empty/Empty-filtrado/Error; RBAC hide; guarda Recrutando→/dashboard)
   - Camadas/arquivos (types, api/estoque, componentes, rotas) + **ConflictMap** (menu.ts/permissions.ts — donos atuais: liberados após configuracoes; indicar sequencialidade)
   - Ordem de implementação em blocos + checklists **Validator §5** e **Security §6** (padrão de auth/dashboard/producao)
   - Riscos (contrato 🟡/🔴, Dívida 2 EN↔PT, migração de mocks) e critérios de entrega
2. **`tasks.md`**: tasks atômicas **1 arquivo real = 1 task** (`est-001..est-NNN`), cada uma com: arquivo (create/modify), conteúdo, saída, DoD, deps, prioridade. Marcar 🟡/🔴 endpoints. Incluir bloco de testes (unit; e2e condicional — Playwright NÃO instalado) e tasks de correção previstas.

## Regras
- NÃO editar `docs/**`; NÃO implementar código; NÃO commitar. Pode ler todo o `frontend/src` e specs.
- Alinhar com o padrão vigente das features concluídas (auth/dashboard/producao/configuracoes): `$lib/api/client.ts` (`apiFetch`/`ApiError`), estados, data-testid, tokens design-system, pt-BR, RBAC hide.
- Considerar `utils/permissions.ts` já com `hasPermissao`/`isAdmin` (configuracoes) — reutilizar.
- Reportar conflitos de escopo como CONFLICT_REPORT se houver (não resolver silenciosamente).

## Relatório final
- Caminhos criados; nº de gaps e de tasks (faixa); resumo dos blocos; riscos/conflitos; decisões que exigem PO/backend (Dívida 2, endpoints assumidos).
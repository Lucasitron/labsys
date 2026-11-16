# DESPACHO — Builder (Frontend) · FINANCEIRO — Blocos 7+8 (B7 Menu + B8 Testes) · 23/09/2026

> Implemente **fin-018..fin-023** de `.opencode/specs/frontend/features/financeiro/tasks.md` (ler B7+B8 integral).
> **Princípio código mínimo: testes só do que resolve caso real; sem duplicar cobertura existente.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-6 commitados. Baseline **451 testes verdes** (31 files). Padrão: `frontend/tests/unit/*.test.ts` (vitest + @testing-library/svelte; alias vite + server.deps.inline)
- e2e: **Playwright NÃO instalado** → criar `frontend/tests/e2e/financeiro.spec.ts` **apenas se alguma suíte e2e ativa existir** em `frontend/tests/e2e/`. Verificar; senão, documentar SKIP (sem arquivo).
- Notas: módulo Admin-only total (guarda 303; `VIEW/EDIT_RULES.financeiro=[0]`; sem helpers novos); zero soma/cálculo client (R-7); SEM Exportar (R-9/D-6); contas derivadas (sem endpoint próprio); relatórios 1 endpoint por pane.
- NÃO alterar specs/docs/requirements; NÃO commitar; **menu.ts é compartilhado (⚠️ sequencial — último a tocar, livre agora)**

## Tasks
- **fin-018** `src/lib/config/menu.ts` ⚠️ MODIFY — no item `Financeiro` (linha ~80), adicionar `children: [{Resumo,/financeiro},{Lançamentos,/financeiro/lancamentos},{Contas a pagar,/financeiro/contas-pagar},{Contas a receber,/financeiro/contas-receber},{Doações & recursos,/financeiro/doacoes},{Custeio,/financeiro/custeio},{Relatórios,/financeiro/relatorios}]`. Sem alterar `module`/`icon`; sem `canSee` por item (pai hide-total por `canView` regra `[0]`).
- **fin-019** `frontend/tests/unit/financeiro-status-format.test.ts` — maps cobrem unions de fin-001 (`{label,color}`); `formatSignedBRL` (`+R$ 1.240,00` / `−R$ 380,00`); prefixo `LAN-`. Pure functions.
- **fin-020** `frontend/tests/unit/financeiro-api.test.ts` — mock `apiFetch` (`vi.mock('$lib/api/client')`): listas montam query PT (`status`, `tipo`, `categoria`, `periodo`) e **omitem** vazios; `registrarPagamento` path `{id}/pagamento` PUT; `createFechamento` congela (payload sem campo calculado); `concluirCompra` path `{id}/concluir`; relatórios 1 endpoint por `RelatorioId` + `?periodo=`; 409/422 propagam `ApiError`.
- **fin-021** `frontend/tests/unit/financeiro-guard.test.ts` — `load` de fin-010: roles 1–4 em qualquer `/financeiro/*` → redirect 303 `/dashboard`; role 0 passa; `VIEW/EDIT_RULES.financeiro` seguem `[0]`; item Financeiro invisível a não-Admin via `canView` (base `permissions.test.ts` verde — **sem helpers novos**).
- **fin-022** `frontend/tests/unit/financeiro-pages.test.ts` — render + `vi.mock` APIs: skeleton/success/empty/empty-filtrado/error+retry; bulk bar só com ≥1 seleção (`registrarPagamento` + refetch; checkbox stopPropagation não abre detalhe); contas derivadas filtram `tipo`+`status` sem endpoint próprio; custeio `?tab=` troca pane + notas visíveis; relatórios `?relatorio=` troca pane sem recálculo; guarda redireciona não-Admin.
- **fin-023** e2e condicional — SKIP documentado (sem Playwright/suíte).

## Regras
1. Tocar SÓ: `menu.ts` + arquivos de teste (+mocks locais; sem tocar produção a menos que bug real exija — reportar).
2. Rodar `npm run test` (todos verdes; total) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; fin-018 ✅/❌ + evidência; por teste ✅/❌ + nº de casos; total final; check; build; nota e2e SKIP; desvios.
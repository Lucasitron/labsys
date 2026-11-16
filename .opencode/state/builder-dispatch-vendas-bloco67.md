# DESPACHO — Builder (Frontend) · VENDAS — Bloco 6+7 (B6 Menu + B7 Testes) · 23/09/2026

> Implemente **vnd-027..vnd-032** de `.opencode/specs/frontend/features/vendas/tasks.md` (ler B6+B7 integral).
> **Princípio código mínimo: testes só do que resolve caso real; sem duplicar cobertura existente.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-5 commitados. Baseline **381 testes verdes** (27 files). Padrão: `frontend/tests/unit/*.test.ts` (vitest + @testing-library/svelte; alias vite + server.deps.inline)
- e2e: **Playwright NÃO instalado** → criar `frontend/tests/e2e/vendas.spec.ts` **apenas se alguma suíte e2e ativa existir** em `frontend/tests/e2e/`. Verificar; senão, documentar SKIP (sem arquivo).
- Notas: `duplicarOrcamento` NÃO existe na API (ações Duplicar via toast honesto D-2); Kanban move via `moverKanban` (409 → toast); banner conversão só Aprovado sem encomenda; guarda `/solicitacoes` 303 p/ `!canDecide`; Recrutando barrado na guarda (302).
- NÃO alterar specs/docs/requirements; NÃO commitar; **menu.ts é compartilhado (⚠️ sequencial — livre agora)**

## Tasks
- **vnd-027** `src/lib/config/menu.ts` ⚠️ MODIFY — no item `Vendas & CRM` (linha ~48), adicionar `children: [{Clientes,/vendas/clientes},{Orçamentos,/vendas/orcamentos},{Encomendas,/vendas/encomendas},{Marketplace,/vendas/marketplace},{Marketing,/vendas/marketing},{Solicitações de edição,/vendas/solicitacoes}]`. Sem alterar `module`/`icon`; sem `canSee` por item (Recrutando barrado na guarda; Solicitações guardada por rota).
- **vnd-028** `frontend/tests/unit/vendas-status-format.test.ts` — maps cobrem unions de vnd-001 (`{label,color}`); `formatMoneyBRL` (`R$ 1.240,00`); `formatDateBR` (`18/09/2026`); prefixos CLI-/OC-/EN-. Pure functions.
- **vnd-029** `frontend/tests/unit/vendas-api.test.ts` — mock `apiFetch` (`vi.mock('$lib/api/client')`): listas montam query PT (`status_kanban`, `tags`, `plataforma`) e **omitem** vazios; `moverKanban` path `{id}/kanban` PUT; `createEncomenda` com `{idOrcamento}`; marketplace envia `valorTaxa` numérico; solicitações monta decisão com motivo; 409 propaga `ApiError`.
- **vnd-030** `frontend/tests/unit/vendas-permissions.test.ts` — `canEditVendas` (Admin tudo; criador edita; outro não; sem createdBy não); `canDecideVendas` (Admin + responsável decidem, criador comum não); `isResponsavelVendas` (role 4 nunca); base `permissions.test.ts` verde.
- **vnd-031** `frontend/tests/unit/vendas-pages.test.ts` — render + `vi.mock` APIs: skeleton/success/empty/empty-filtrado/error+retry; `!canEdit` mostra "Sugerir alteração" e **omite** Editar do DOM (hide real); Kanban move chama `moverKanban` + refetch; 409 move → toast; banner conversão só Aprovado sem encomenda; guarda `/solicitacoes` redireciona `!canDecide`.
- **vnd-032** e2e condicional — SKIP documentado (sem Playwright/suíte).

## Regras
1. Tocar SÓ: `menu.ts` + arquivos de teste (+mocks locais; sem tocar produção a menos que bug real exija — reportar).
2. Rodar `npm run test` (todos verdes; total) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; vnd-027 ✅/❌ + evidência; por teste ✅/❌ + nº de casos; total final; check; build; nota e2e SKIP; desvios.
# DESPACHO — Builder (Frontend) · ESTOQUE — Bloco 5 (est-032..est-036) · 21/09/2026

> Testes (unit) + fechamento (check/build/grep).

## Contexto
- **Tasks:** `.opencode/specs/frontend/features/estoque/tasks.md` §6 (`est-032..est-036`)
- Blocos 1–4 concluídos (`f7b3be5`, `192004c`, `f16d0c7` + atual): types DTOs, API PT, RBAC, telas existentes, rotas novas (editar/detalhes/empréstimos), fornecedores/localizações/BOM
- Baseline: **176 testes verdes; svelte-check 0 errors/16 warnings/7 files**; Playwright NÃO instalado (`package.json` sem `@playwright/test`)
- Utils a testar: `utils/stock-status.ts`, `utils/stock-url.ts`, `utils/stock-format.ts`; API: `api/stock/{items,movements,loans,suppliers,locations}.ts`
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)

## Tasks (ordem; NÃO commitar)
- **est-032** `frontend/tests/unit/estoque/stock-status.test.ts` + `stock-url.test.ts` (create) — tons por status; `exitReasonMeta` com TipoSaida; `loanComputedMeta` devolvido→muted; parsers de URL (str/int/arr/bool/sanitizeSort).
- **est-033** `frontend/tests/unit/estoque/stock-format.test.ts` (create) — fmtMoney (Intl pt-BR), fmtQty com unidade, fmtDate/fmtDateTime, toDateInputValue, daysFromToday.
- **est-034** `frontend/tests/unit/estoque/api-stock.test.ts` (create) — mock fetch: `listarItens` (List, sem paginação), `criarItem` (201), `atualizarItem` (409→ApiError), `importarCsv` (multipart), `listarEntradasPorItem`/`buscarSaida` (path PT), `criarEmprestimo`/`devolverEmprestimo` (PUT sem body), `criarFornecedor`, `criarLocalizacao`; garantir **nenhum `/stock/`** nos paths.
- **est-035** `frontend/e2e/estoque.spec.ts` (create) — fluxos completos com `describe.skip` (Playwright ausente); não quebrar `npm test`.
- **est-036** fechamento — `npm run check` (0 errors) · `npm run build` ok · `npm test` · `rg "/stock/" frontend/src` (paths de API = 0) · conferir critérios plan §8.

## Regras
1. **NÃO commitar.** Ordem numérica. Tocar SOMENTE os arquivos de teste acima (+ nada de produção). Não alterar API/types/utils/rotas/permissions/menu/docs/specs. Não instalar dependências (e2e = skip).
2. Seguir padrão dos testes vitest existentes (jsdom; sem deps novas). Se precisar de helpers de mock, criar inline no próprio teste.
3. Ao final: `npm run test` (deve subir bem acima de 176, todos verdes) + `npm run check` (0 errors; registrar warnings/files) + `npm run build` (ok) + `rg "/stock/" frontend/src` (API paths 0).

## Relatório
- Por task ✅/❌ (est-035 SKIP) + evidência (nº de casos por arquivo)
- `npm run test` (nº final) · `npm run check` · `npm run build` · `rg "/stock/"` · critérios plan §8 (o que atende/o que fica pendente de contrato R-9/R-8/D-2)
- Pontos de atenção/desvios
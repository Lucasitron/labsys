# DESPACHO — Builder (Frontend) · FINANCEIRO — Bloco 1 (B1 Fundação) · 23/09/2026

> Implemente **fin-001..fin-010** de `.opencode/specs/frontend/features/financeiro/tasks.md` (ler o bloco B1 integral + mockup `docs/mokups/financeiro/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo. Antes de cada linha: é necessário? a linguagem/plataforma já resolve? há forma menos verbosa? resolve problema real?**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme com `git branch --show-current`; se não for, PARE e reporte)
- Baseline: `api/client.ts` (apiFetch/ApiError), `utils/format.ts` (initials/relativeTime/formatNumber), `utils/vendas-format.ts` (`formatMoneyBRL`/`formatDateBR` — REUSAR, não duplicar Intl), `utils/permissions.ts` (`isAdmin`, `VIEW/EDIT_RULES.financeiro=[0]` — **NÃO tocar**), UI kit (MoneyInput/Timeline/TypeBadge/Tabs/BulkActionsBar — **zero UI novo**), modal padrão
- Padrão de guarda: `configuracoes/+layout.ts` (Admin-only 303) — NÃO `vendas/pessoas`
- NÃO alterar specs/docs/requirements/`permissions.ts`; NÃO commitar; sem libs novas

## Tasks (ordem sugerida)
- **fin-001** `src/lib/types/financeiro.ts` — unions PT + modelos do tasks.md (CategoriaFinanceira/Lancamento/LancamentosResult/TotaisContas/DoacaoRecurso/DoacoesResult/ValorHoraNivel/ParametroOverhead/FechamentoEncomenda/CustoEncomenda/SolicitacaoCompra/ResumoFinanceiro/6 shapes de relatório/Paged<T>/params). Sem any.
- **fin-002** `src/lib/api/financeiro/lancamentos.ts` — bearer() + apiFetch; listLancamentos (query só não-nulos) 🔴, getLancamento 🟡, createLancamento 🔴 (status inicial Pendente), registrarPagamento PUT `{id}/pagamento` 🔴. Só `/api/financeiro/**`.
- **fin-003** `src/lib/api/financeiro/categorias.ts` — list/create 🔴.
- **fin-004** `src/lib/api/financeiro/doacoes.ts` — list/create 🔴 (`{registros,resumo,cobertura}` servidos). **Sem soma de cobertura.**
- **fin-005** `src/lib/api/financeiro/custeio.ts` — listFechamentos/createFechamento/getCusto/listValoresHora/definirValorHora/definirOverhead 🔴. **Sem `calcular|soma|margem(`.**
- **fin-006** `src/lib/api/financeiro/solicitacoes-compra.ts` — list/create/concluirCompra 🔴. Sem bloqueio (informativo).
- **fin-007** `src/lib/api/financeiro/relatorios.ts` — 6 getters (1 endpoint por relatório, `?periodo=` 🟡 D-4) 🔴. **Sem `totalizar|agregar|somar`.**
- **fin-008** `src/lib/utils/financeiro-status.ts` — 5 maps `{label,color}` (padrão `stock-status.ts`, importar `Meta`); **só cor, sem tradução**.
- **fin-009** `src/lib/utils/financeiro-format.ts` — `formatSignedBRL` (sinal + `formatMoneyBRL` **importado** de vendas-format) + `codigoLancamento` (`LAN-`). REUSAR vendas-format/format (não duplicar Intl).
- **fin-010** `src/routes/(app)/financeiro/+layout.ts` — `if (!isAdmin(user)) throw redirect(303, '/dashboard')` para **todo** `/financeiro/**`; ssr=false/prerender=false; sem duplicar fetchMe. Sem `canView` (regra `[0]` já esconde o item).

## Regras
1. Criar SÓ os 10 arquivos acima.
2. pt-BR; aria-label; sem any; sem libs; Svelte 5 runes; tokens design-system.
3. Ao final: `npm run test` (regressão — baseline 451 verdes) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência (arquivo:linha); contrato 🔴/🟡 por endpoint; resultados test/check/build; desvios.
# DESPACHO — Builder (Frontend) · FINANCEIRO — Bloco 2 (B2 Lançamentos + Resumo) · 23/09/2026

> Implemente **fin-011..fin-012** de `.opencode/specs/frontend/features/financeiro/tasks.md` (ler o bloco B2 integral + mockup `docs/mokups/financeiro/requisitos.md` § testids).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Bloco 1 commitado (`2e78ed8`): `types/financeiro.ts`, `api/financeiro/*` (6 módulos), `utils/financeiro-{status,format}.ts`, guarda `financeiro/+layout.ts` Admin-only 303 ativa
- Reuso: UI kit (`PageHeader`, `SearchInput`, `Dropdown`?, `Chip`, `TableSkeleton`, `EmptyState`, `ErrorBanner`, `Pagination`, `StatusBadge`, `BulkActionsBar`?, `ConfirmDialog`?, modal padrão, `MoneyInput`), `utils/vendas-format.ts` (`formatMoneyBRL`), `utils/format.ts`, `stores/toast`
- Padrão de referência: `routes/(app)/pessoas/+page.svelte` (lista URL-state + bulk) e `routes/(app)/vendas/clientes/+page.svelte`
- NÃO alterar specs/docs/requirements/`permissions.ts`/APIs; NÃO commitar; sem libs novas

## Tasks
- **fin-011** `src/routes/(app)/financeiro/lancamentos/+page.svelte` (+`+page.ts`) — URL `?search=&status=&tipo=&categoria=&periodo=&ordenar=&page=&pageSize=`; PageHeader + "Nova categoria" (ghost → `modal-categoria` local: Nome\*/Tipo Receita|Despesa/Descrição → `createCategoria` 🔴) + "Novo lançamento" (primary → `modal-novo-lancamento` local: Categoria\*/Tipo radio/Valor\* `MoneyInput`/Vencimento\*/Pagamento opcional/Referência/Observação → `createLancamento` 🔴); KPIs **servidos** (`resumo`: Entradas/Saídas/Recebido×pendente/Fluxo líquido); filtros (SearchInput 300ms + 5 triggers Status/Tipo/Categoria/Período/Ordenar + Chips `fin-chips` + "Limpar filtros"); tabela (checkbox select-all/indeterminate · Vencimento danger se Atrasado · Descrição + `REF • EN-…` mono · Categoria · Tipo badge · Valor signed mono · Status badge · ⋮) + linha → `modal-lancamento-detalhe` local (ficha + "Registrar recebimento/pagamento" → `modal-pagamento` local → `registrarPagamento` 🔴); `BulkActionsBar` (`bulk-bar`, `bulk-count`: Registrar pagamento/recebimento · **SEM Exportar** (sem endpoint R-9) · Limpar); rodapé "Mostrando X de Y" + Pagination; TableSkeleton/Empty/Empty-filtrado/ErrorBanner+retry; Atrasado `bg-danger/[0.03]`; checkbox stopPropagation. `data-testid="lan-row/lan-checkbox/select-all-rows/bulk-bar/lan-novo/fin-chips"`; seleção não persiste entre páginas.
- **fin-012** `src/routes/(app)/financeiro/+page.svelte` (+`+page.ts`) — PageHeader + "Exportar resumo" (**omitido** sem endpoint R-9 — nem botão desabilitado; simplesmente não renderizar) + "Novo lançamento" (reuso do modal de fin-011 — **composição local, sem arquivo próprio**: copiar o mínimo necessário ou extrair? **NÃO extrair componente novo** — duplicar o modal localmente só se o custo for menor que acoplamento; decidir pelo menor código e registrar); KPIs servidos (Saldo em caixa · A receber com "sendo R$ X inadimplentes" danger · A pagar com "R$ X vencem em 7 dias" warn · Custo/margem média); grid 3 cols: Fluxo de caixa (barras CSS mensais Recebido success/Previsto brand + "Ver relatórios" → `/financeiro/relatorios`) + Alertas (cards por severidade com atalhos `data-goto`); grid 2 cols: Últimos lançamentos (lista → `/financeiro/lancamentos`) + Entradas/Saídas + Categorias com maiores saídas (barras progress); erro parcial por bloco. `data-testid="fin-saldo/fin-alerta"`; **zero soma client** (R-7 — verificar com `rg`).

## Regras
1. Criar SÓ os arquivos acima.
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; debounce 300ms + AbortController; Intl pt-BR; sem `{@html}`; sem console.log; **SEM Exportar**.
3. Ao final: `npm run test` (regressão 451) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; decisão do modal "Novo lançamento" no resumo (duplicado local vs extraído); contrato 🔴/🟡; confirmação zero-soma (rg); resultados test/check/build; desvios.
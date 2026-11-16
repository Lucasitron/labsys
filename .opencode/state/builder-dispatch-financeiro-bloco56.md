# DESPACHO — Builder (Frontend) · FINANCEIRO — Blocos 5+6 (B5 Custeio + B6 Relatórios) · 23/09/2026

> Implemente **fin-016..fin-017** de `.opencode/specs/frontend/features/financeiro/tasks.md` (ler B5+B6 integral + mockup `docs/mokups/financeiro/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-4 commitados: fundação + lançamentos/resumo + contas/​doações; guarda Admin-only ativa
- Reuso: `api/financeiro/{custeio,solicitacoes-compra,relatorios}.ts`, kit (PageHeader, SearchInput?, Select, Tabs, Chip, TableSkeleton, EmptyState, ErrorBanner, Pagination?, StatusBadge, modal, MoneyInput), `utils/financeiro-{status,format}.ts`, `utils/format.ts`, `stores/toast`. Modais como composição local (NÃO criar arquivos de componente).
- **Zero cálculo de custo/margem/totais no client** (R-7) — só renderizar o servido
- NÃO alterar specs/docs/requirements/`permissions.ts`/APIs; NÃO commitar; sem libs novas

## Tasks
- **fin-016** `src/routes/(app)/financeiro/custeio/+page.svelte` (+`+page.ts`) — URL `?tab=fechamentos|custos|valores|compras`; PageHeader + "Valores/hora & overhead" (→ `modal-valor-hora` local) + "Novo fechamento" (→ `modal-novo-fechamento` local: aviso de congelamento · Encomenda\* select só Aberta · Valor fechado\* MoneyInput · Horas estimadas\* · Data default hoje → `createFechamento` 🔴); KPIs **servidos** (Custo médio/encomenda · Margem média · Overhead acumulado · Horas validadas + "coerência de horas ok ✓"); Tabs (`custeio-tabs`): Fechamentos (tabela Encomenda mono/Cliente/Valor/Horas/Data/Status + nota de congelamento; linha → `modal-custo-detalhe` local via `getCusto` 🔴) · Custos (cards Materiais/Mão de obra/Overhead + barra + Custo total + Margem %, danger se negativa) · Valores/hora & Overhead (tabela por nível 0–3 + Editar → `modal-valor-hora` → `definirValorHora` 🔴; card Overhead R$/h + fórmula + "Editar taxa" → `modal-overhead` → `definirOverhead` 🔴; nota de auditoria) · Compras informativo (aviso "não há bloqueio" brand + tabela Item/Qtd/Valor estimado/Solicitação/Status/"Concluir" → `modal-concluir-compra` local → `concluirCompra` 🔴; concluídas mostram "saída lançada"); erro parcial por aba. `data-testid="cus-tab-custos/cus-concluir"`; notas de congelamento/auditoria/compras visíveis.
- **fin-017** `src/routes/(app)/financeiro/relatorios/+page.svelte` (+`+page.ts`) — URL `?relatorio=fluxo|dre|lucratividade|inadimplencia|doacoes|custo-maquina&periodo=`; PageHeader + Select período ("Setembro 2026" default) + "Exportar" (**omitido** sem endpoint R-9/D-6 — nem botão); seletor 6 cards `.rel-btn` (Fluxo brand · DRE warn · Lucratividade success · Inadimplência danger · Doações brandhi · Custo-máquina muted; ativo `border-brand` + tint; clique → pane + `?relatorio=`); pane resultado por relatório (fluxo: semana Entradas/Saídas/Líquido + total · dre: receita/custos/despesas/doações/resultado · lucratividade: venda/custo/margem R$/margem % danger se negativa · inadimplência: cliente/referência/vencimento/dias danger/valor · doações: mês Doações/Despesas/Saldo + acumulado · custo-máquina: máquina/horas/custo/% + total). **Nunca recalcular totais — só renderizar o servido** (1 endpoint por relatório; `?periodo=` 🟡 D-4). `data-testid="rel-btn-fluxo/rel-table"`; pane reflete a URL e vice-versa.

## Regras
1. Criar SÓ os arquivos acima.
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; debounce 300ms + AbortController; Intl pt-BR; sem `{@html}`; sem console.log.
3. Ao final: `npm run test` (regressão 451) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; contrato 🔴/🟡; confirmação zero-cálculo (rg `calcular|soma|margem(|totalizar|agregar`); resultados test/check/build; desvios.
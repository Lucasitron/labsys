# DESPACHO — Builder (Frontend) · VENDAS — Bloco 4 (B4 Encomendas) · 23/09/2026

> Implemente **vnd-022..vnd-023** de `.opencode/specs/frontend/features/vendas/tasks.md` (ler o bloco B4 integral + mockup `docs/mokups/vendas/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-3 commitados: fundação + clientes + orçamentos (banner de conversão `createEncomenda` pronto)
- Reuso OBRIGATÓRIO: `ui/KanbanBoard.svelte` (do kit — **NÃO editar**, dono kit/RH) para o Kanban; `api/vendas/encomendas.ts` (list/get/create/moverKanban 🔴/🟡); kit (PageHeader, SearchInput, Chip, TableSkeleton, EmptyState, ErrorBanner, StatusBadge, Tabs, Timeline); `utils/vendas-{status,format}.ts`; permissions; `SolicitarEdicaoModal` (para `!canEditVendas`); `stores/toast`
- Dona única do Kanban = Vendas (Produção linka, sem duplicar)
- NÃO alterar specs/docs/requirements/`permissions.ts`/APIs/kit; NÃO commitar; sem libs novas

## Tasks
- **vnd-022** `src/routes/(app)/vendas/encomendas/+page.svelte` (+`+page.ts`) — URL `?status_kanban=&search=&clienteId=`; PageHeader + "Registrar venda" → `modal-nova-encomenda` local (Cliente\* · Origem · Valor\* · Previsão · Produtos → `createEncomenda` 🔴, nasce na Fila) + busca; KPIs **servidos** (Em produção, Prontas, Atrasadas, Entregues/mês); **`KanbanBoard` reusado** (5 colunas fixas Fila/muted·Produção/brand·Acabamento/warn·Pronto/success·Entregue/muted; header label+count; coluna vazia empty mini; scroll-x): card (código mono, cliente, previsão `danger` se atrasada, valor mono, origem badge, itens count, botão mover → popover `move-en{id}` com destinos → `moverKanban` 🔴 + refetch + toast; erro → toast no card); card → detalhe; skeleton-colunas/error-banner. `data-testid="kanban-col-producao/en-card/move-en*"`. Coluna derivada do servido (nunca recalculada); 409 → toast "atualize a tela".
- **vnd-023** `src/routes/(app)/vendas/encomendas/[id]/+page.svelte` (+loader) — voltar + "Avançar etapa" (popover destino; some em Entregue) + `Sugerir alteração` se `!canEditVendas` + ⋮; hero (código mono, cliente, origem badge, StatusBadge da coluna, valor final); KPIs servidos (Valor, Previsão/dias, Itens, Tempo na etapa); Tabs `?tab=` (visao-geral/itens/historico): visão (cliente + link OC origem + observações + marketplace vinculado), itens (tabela), histórico (Timeline de `historico_status_encomenda`); erro parcial por aba. `data-testid="enc-avancar/enc-tab-historico"`.

## Regras
1. Criar SÓ os arquivos acima.
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; debounce 300ms + AbortController; Intl pt-BR; sem `{@html}`; sem console.log.
3. Ao final: `npm run test` (regressão 381) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; contrato 🔴/🟡; resultados test/check/build; desvios.
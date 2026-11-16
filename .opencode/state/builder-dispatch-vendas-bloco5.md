# DESPACHO — Builder (Frontend) · VENDAS — Bloco 5 (B5 Marketplace/Marketing/Solicitações) · 23/09/2026

> Implemente **vnd-024..vnd-026** de `.opencode/specs/frontend/features/vendas/tasks.md` (ler o bloco B5 integral + mockup `docs/mokups/vendas/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-4 commitados: fundação + clientes + orçamentos + encomendas; guarda `vendas/+layout.ts` ativa (solicitações 303 p/ `!canDecide`)
- Reuso: `api/vendas/{marketplace,marketing,solicitacoes}.ts`, `ui/MoneyInput.svelte`, kit (PageHeader, SearchInput, Select, Tabs, Chip, TableSkeleton, EmptyState, ErrorBanner, Pagination, StatusBadge, modal), `utils/vendas-{status,format}.ts`, `stores/toast`
- NÃO alterar specs/docs/requirements/`permissions.ts`/APIs; NÃO commitar; sem libs novas

## Tasks
- **vnd-024** `src/routes/(app)/vendas/marketplace/+page.svelte` (+`+page.ts`) — URL `?tab=&page=` (tabs todos/ml/shopee/elo7); PageHeader + "Registrar venda externa" → `modal-registrar-venda` local (Plataforma\* · Código externo\* mono · Encomenda vinculada search+valor · Data · Taxa R$\* via `MoneyInput` → líquido = valor − taxa **só preview** → `registrarVenda` 🔴); KPIs **servidos** (Bruto, Taxas, **Líquido**, Vendas/período); tabela (Plataforma badge, Código mono, Encomenda link, Cliente, Data, Bruto, Taxa, Líquido mono) + card de totais **do servido**. skeleton/empty-por-tab/error. `data-testid="mp-tab-ml/mp-liquido/mp-registrar"`. Totais nunca recalculados; taxa nunca em %.
- **vnd-025** `src/routes/(app)/vendas/marketing/+page.svelte` (+`+page.ts`) — URL `?search=&status=&prioridade=&page=`; PageHeader + "Nova tarefa" → `modal-nova-tarefa` local (Título\* · Descrição · Responsável\* · Início · Prazo\* · Prioridade radio → `createTarefa` 🔴); KPIs servidos (Pendentes, Em andamento, Concluídas, Alta prioridade); tabela (Tarefa+descrição, Responsável Avatar, Prazo mono `danger` se vencido/≤7d, Prioridade/Status badges, ⋮); filtros search+2 selects + chips; skeleton/empty/error. `data-testid="mk-tarefa/mk-nova"`. Criação liberada a autenticados (só oculta "Nova tarefa" se necessário — conferir mockup).
- **vnd-026** `src/routes/(app)/vendas/solicitacoes/+page.svelte` (+`+page.ts`) — URL `?tab=` (pendentes/aprovadas/rejeitadas); PageHeader + explicação do fluxo; KPIs servidos (Pendentes, Aprovadas/mês, Rejeitadas/mês, Revisar hoje); tabs com counts; cards (`bg-elevated/30`): solicitante Avatar+tempo, tipo + badge alvo CLI/OC/EN + status, resumo `atual → proposto` mono, ações "Aprovar" + "Rejeitar" (→ `modal-decisao-solicitacao` local: resumo + radio + Motivo\* ao rejeitar → `decidirSolicitacao` 🟡 + refetch); aprovadas/rejeitadas read-only (autor/data+motivo). Guarda `canDecide` já ativa no layout. `data-testid="sol-decidir/sol-aprovar/sol-rejeitar"`. Motivo obrigatório ao rejeitar (inline); decisão reflete via refetch.

## Regras
1. Criar SÓ os arquivos acima.
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; debounce 300ms + AbortController; Intl pt-BR; sem `{@html}`; sem console.log.
3. Ao final: `npm run test` (regressão 381) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; contrato 🔴/🟡; confirmação de totais servidos (rg sem soma); resultados test/check/build; desvios.
# DESPACHO — Builder (Frontend) · FINANCEIRO — Blocos 3+4 (B3 Contas + B4 Doações) · 23/09/2026

> Implemente **fin-013..fin-015** de `.opencode/specs/frontend/features/financeiro/tasks.md` (ler B3+B4 integral + mockup `docs/mokups/financeiro/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-2 commitados: fundação + lançamentos/resumo; guarda Admin-only ativa
- Reuso: `api/financeiro/{lancamentos,doacoes}.ts`, kit (PageHeader, SearchInput, Dropdown/Select, Chip, TableSkeleton, EmptyState, ErrorBanner, Pagination, StatusBadge, modal), `utils/financeiro-{status,format}.ts`, `utils/format.ts`, `stores/toast`. Modais (novo-lancamento/pagamento) como composição local (padrão fin-011 — NÃO criar arquivos de componente).
- **Contas = vistas derivadas** de `listLancamentos` (R-8) — nenhum endpoint `/contas`; nenhum CRUD próprio
- NÃO alterar specs/docs/requirements/`permissions.ts`/APIs; NÃO commitar; sem libs novas

## Tasks
- **fin-013** `src/routes/(app)/financeiro/contas-pagar/+page.svelte` (+`+page.ts`) — vista derivada `listLancamentos({tipo:'Saída',status:'Pendente,Atrasado'})`; URL `?search=&status=&vencimento=&page=`; PageHeader + "Nova conta a pagar" (→ modal-novo-lancamento local pré-selecionado Saída); KPIs **servidos** (`totais`: A pagar no mês · Vencidas, 0 → "Em dia ✓" · Pagas no mês · Próximos 30 dias); filtros busca + Status + Vencimento + chips + limpar; tabela (Conta/fornecedor + `REF • …` · Categoria · Vencimento · Valor mono · Status badge · registrar pagamento → modal-pagamento local + ⋮); rodapé "X contas a pagar · total R$ Y" (servido). `data-testid="cp-row/cp-pagar"`.
- **fin-014** `src/routes/(app)/financeiro/contas-receber/+page.svelte` (+`+page.ts`) — vista derivada `listLancamentos({tipo:'Entrada',status:'Pendente,Atrasado'})`; URL `?search=&status=&origem=&page=`; PageHeader + "Novo recebimento" (→ modal-novo-lancamento local pré-selecionado Entrada); KPIs servidos (A receber no mês · Inadimplência "{n} recebíveis vencidos" · Recebido no mês · Próximos 30 dias); filtros busca + Status + Origem (Encomendas/Marketplace/Outros) + chips + limpar; tabela (Cliente/origem Avatar + nota atraso danger/vencimento muted · Referência mono `EN-…`/`OC-…` · Origem badge · Vencimento danger se atrasado · Valor · Status · registrar recebimento → modal-pagamento local + ⋮); atrasadas `bg-danger/[0.03]`; rodapé "X recebíveis · pendente + atrasado = R$ Y" (servido). `data-testid="cr-row/cr-receber"`.
- **fin-015** `src/routes/(app)/financeiro/doacoes/+page.svelte` (+`+page.ts`) — URL `?search=&tipo=&periodo=&page=`; PageHeader + "Nova doação/recurso" (→ `modal-nova-doacao` local: Tipo\* radio Doação/Projeto · Origem\* · Valor\* MoneyInput · Recebimento\* date · Projeto universitário select `PJ-…`/— + nota "gera entrada separada" → `createDoacao` 🔴); KPIs **servidos** (Recebido no ano · Doações com nº · Recursos de projetos com nº · Doação média); card Doações vs Despesas (barras mensais brand + cobertura servida); filtros busca + Tipo + Período + chips + limpar; tabela (Tipo badge Doação brand/Projeto success · Origem · Projeto associado mono · Recebimento · Valor · ⋮); rodapé "{n} registros no período". `data-testid="doa-nova/doa-row"`. Cobertura nunca calculada no client.

## Regras
1. Criar SÓ os arquivos acima.
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; debounce 300ms + AbortController; Intl pt-BR; sem `{@html}`; sem console.log.
3. Ao final: `npm run test` (regressão 451) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; contrato 🔴; confirmação de vistas derivadas sem endpoint próprio (rg `/contas`); resultados test/check/build; desvios.
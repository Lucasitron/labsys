# DESPACHO — Builder (Frontend) · VENDAS — Bloco 3 (B3 Orçamentos) · 23/09/2026

> Implemente **vnd-019..vnd-021** de `.opencode/specs/frontend/features/vendas/tasks.md` (ler o bloco B3 integral + mockup `docs/mokups/vendas/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-2 commitados: fundação + clientes (guarda `vendas/+layout.ts` ativa; `SolicitarEdicaoModal` pronto em `vendas/components/`)
- Reuso: `api/vendas/{orcamentos,encomendas,clientes}.ts`, `ui/{StatusBadge,MoneyInput,Timeline}`, kit (PageHeader, SearchInput/SearchCombo?, Tabs, Chip, TableSkeleton, EmptyState, ErrorBanner, Pagination, modal), `utils/vendas-{status,format}.ts`, `utils/format.ts`, permissions (`canEditVendas(user,record)`), `stores/toast`
- **Decisão B1 (duplicarOrcamento):** função NÃO criada (🔴 sem confirmação) → ação "Duplicar" **omitida da UI** ou via toast honesto ("recurso indisponível — backend pendente D-2"); nunca chamar endpoint inexistente
- NÃO alterar specs/docs/requirements/`permissions.ts`/APIs; NÃO commitar; sem libs novas

## Tasks
- **vnd-019** `src/routes/(app)/vendas/orcamentos/+page.svelte` (+`+page.ts`) — URL `?tab=&search=&page=&pageSize=` (tabs todos/pendentes/aprovados/ajuste/recusados); PageHeader + "Novo orçamento" → tela 5 (**SEM "Exportar"** — D-6 fora do MVP); KPIs **servidos** (4, nunca derivados); tabela (Código mono, Cliente Avatar+nome, Itens, Valor mono, Validade, StatusBadge, ⋮ → Ver/Editar/`Sugerir alteração`/Duplicar); Pagination; skeleton/empty-por-tab/error. `data-testid="orc-tab-pendentes/orc-row"`.
- **vnd-020** `src/routes/(app)/vendas/orcamentos/novo/+page.svelte` — voltar; PageHeader; card Cliente (busca por nome/documento + card selecionado + "Novo"); card Itens (tabela resumo + `+ Adicionar item` → overlay `modal-item-orcamento` local: origem Do-estoque [busca+materiais, "Em estoque: X"] / Fora-do-estoque [tipo livre + "Precisa ser comprado"]; comuns Descrição\*/Material\*/Qtd.material\*+unidade/Horas/Quantidade\*/Valor unit.\* + preview Subtotal; "Faltam informações: …" `role="alert"` + foco); card Resumo (Intl BRL: subtotal, desconto R$, TOTAL); card Prazos (Validade\* default +30d, Observações); footer sticky Cancelar/"Salvar orçamento" → `createOrcamento` 🔴 → detalhe; validação ≥1 item + cliente. `data-testid="orc-cliente/orc-item-add/orc-total/submit-orcamento"`. **Subtotal = qtd × valorUnitario (R-8 — soma de LINHA apenas, nunca totais do orçamento; TOTAL exibido = soma local dos itens para preview, com nota de que o backend é autoritativo? — conferir: se o backend serve o total, exibir o servido; o preview local é cálculo de formulário, permitido)**.
- **vnd-021** `src/routes/(app)/vendas/orcamentos/[id]/+page.svelte` (+loader) — voltar + ações (Editar/`Sugerir alteração` via `canEditVendas` · Duplicar → **omitir ou toast honesto D-2** · ⋮); **banner de conversão** (só `status==='Aprovado'` sem encomenda) → `createEncomenda({idOrcamento})` 🔴 → EN na Fila + refetch; KPIs (Valor, Itens, Validade/dias, Cliente); tabela Itens; Timeline de histórico; card cliente + observações; erro parcial por bloco. `data-testid="orc-criar-encomenda/orc-encomenda-criada"`. Banner some após conversão (refetch, nunca estado local).

## Regras
1. Criar SÓ os arquivos acima.
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; debounce 300ms + AbortController; Intl pt-BR; sem `{@html}`; sem console.log; **SEM Exportar CSV**.
3. Ao final: `npm run test` (regressão 381) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; decisão Duplicar; contrato 🔴/🟡; resultados test/check/build; desvios.
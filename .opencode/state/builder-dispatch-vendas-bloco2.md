# DESPACHO — Builder (Frontend) · VENDAS — Bloco 2 (B2 Clientes) · 23/09/2026

> Implemente **vnd-014..vnd-018** de `.opencode/specs/frontend/features/vendas/tasks.md` (ler o bloco B2 integral + mockup `docs/mokups/vendas/requisitos.md` § testids).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Bloco 1 commitado (`ed7dbcd`): `types/vendas.ts`, `api/vendas/*` (7 módulos), `utils/vendas-{status,format}.ts`, `ui/{Timeline,MoneyInput}`, permissions (`isResponsavelVendas`, `canEditVendas(user,record)`, `canDecideVendas`)
- Reuso: UI kit (`PageHeader`, `SearchInput`, `Dropdown`, `Chip`, `TableSkeleton`, `EmptyState`, `ErrorBanner`, `Pagination`, `StatusBadge`, `Tabs`, `RowActions`?, `BulkActionsBar`?, `ConfirmDialog`?, `RadioCards`?, modal padrão) — conferir nomes reais; `utils/format.ts`; `stores/toast`; `utils/errors.ts` (`toUserMessage`)
- Padrão de referência: `routes/(app)/pessoas/+page.svelte` (lista URL-state + bulk) e `routes/(app)/pessoas/[id]/+page.svelte` (tabs + erro parcial)
- NÃO alterar specs/docs/requirements/`permissions.ts`/APIs; NÃO commitar; sem libs novas

## Tasks
- **vnd-014** `src/routes/(app)/vendas/+layout.ts` — `canView(user,'vendas')` senão `redirect(302,'/dashboard?denied=...')` (padrão estoque; Recrutando já fora de `VIEW_RULES`); retorna `{ canEdit: isAdmin(user) }` base; se path `/vendas/solicitacoes*` e `!canDecideVendas(user)` → `redirect(303,'/vendas/clientes')`. ssr=false/prerender=false; sem duplicar fetchMe.
- **vnd-015** `src/routes/(app)/vendas/clientes/+page.svelte` (+`+page.ts`) — URL `?search=&tipo=&tags=&ordenar=&page=&pageSize=`; SearchInput 300ms + 3 Dropdown (Tipo/Tags/Ordenar, AND) + Chips + "Limpar filtros"; tabela (Avatar+nome/email, código mono `CLI-XXXX`, tags chips, Tipo badge, `RowActions` → Ver/Editar/`Sugerir alteração`/Duplicar/Excluir) + `BulkActionsBar` (Adicionar tag + Solicitar alteração + Limpar, com `ConfirmDialog`); Pagination; PageHeader + "+ Novo cliente" (todos autenticados criam); mobile cards; linha → detalhe (stopPropagation); TableSkeleton/Empty/Empty-filtrado/ErrorBanner+retry. `data-testid="cliente-row/cliente-checkbox/select-all/bulk-bar"`. **`canEditVendas(user,record)` decide Editar vs "Sugerir alteração" (hide real — nunca disabled).**
- **vnd-016** `src/routes/(app)/vendas/components/SolicitarEdicaoModal.svelte` — props {alvo:{tipo:'CLI'|'OC'|'EN',id,nome}, campo?, valorAtual?, onClose, onSent}; Tipo de alteração · Campo · Valor atual (readonly) · Valor proposto\* · Justificativa\* → `createSolicitacao` 🟡 → toast + onSent; Modal do kit; validação inline + busy anti-duplo. `data-testid="modal-solicitar-edicao"`.
- **vnd-017** `src/routes/(app)/vendas/clientes/[id]/+page.svelte` (+loader) — voltar (preserva filtros) + "Editar" (só `canEditVendas`, senão "Sugerir alteração" → vnd-016) + ⋮; hero (Avatar, nome, `código · tipo · desde`, tags, ações ✉/📞); KPIs **servidos** (Total encomendado, Orçamentos em aberto, Encomendas em produção, Última interação — nunca somar no client); Tabs `?tab=` (visao-geral/interacoes/orcamentos/encomendas): visão (`<dl>` + relacionamento em dias), **interações** (`Timeline` + "Registrar interação" → modal local → `listInteracoes`/`registrarInteracao`), orçamentos/encomendas (mini-tabelas filtradas por cliente); erro parcial por aba; 404 → EmptyState + voltar. `data-testid="cliente-tabs"`.
- **vnd-018** `src/routes/(app)/vendas/clientes/novo/+page.svelte` — voltar; PageHeader; form em cards (Identificação: Tipo RadioCards PF/PJ · Nome/Razão\* · CPF/CNPJ\* mascarado+validado · Nascimento PF; Contato: E-mail\* · Telefone\* · Endereço com CEP; Segmentação: tags toggle + criar nova 🟡 + Observações); footer Cancelar/"Salvar e adicionar outro"/"Salvar cliente" (busy); `createCliente` 🔴; 409 → inline (D-7); sucesso → detalhe (ou reset). `data-testid="cliente-nome/cliente-cpf-cnpj/submit-cliente"`; `novalidate`; documento mascarado em leitura.

## Regras
1. Criar SÓ os arquivos acima (rotas `vendas/clientes/**` + `vendas/components/**` + layout).
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; debounce 300ms + AbortController; Intl pt-BR; sem `{@html}`; sem console.log; **SEM Exportar CSV**.
3. Ao final: `npm run test` (regressão 381) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; contrato 🔴/🟡; paginação (client/server); resultados test/check/build; desvios.
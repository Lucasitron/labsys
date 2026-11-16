# Plan — Vendas & CRM (Frontend)

> Feature: `vendas` (frontend) · Branch: `dev/frontend` · Gerado por: Planner stand-in (despacho `.opencode/state/planner-dispatch-vendas.md`, 23/09/2026)
> Autoridade (ordem): (1) mockup `docs/mokups/vendas/` (`full.html` 11 sections · `requisitos.md` v0.1 · `instrunctions.md` 12 prompts); (2) `docs/contract.gap.md` §5.3 (PT, sem `/interacoes`, com `/solicitacoes`, Kanban por clique, RBAC criador+Admin); (3) `.opencode/specs/frontend/features/vendas/requirements.md` (**desatualizado — reconciliado no §8, sem edição**).
> Escopo: SÓ frontend. NÃO editar `docs/**` nem `requirements.md`; NÃO implementar; NÃO commitar. Nenhum endpoint inventado (só os da spec/§5.3; divergências → D-x).
> Princípio **código mínimo**: reuso > create; nada especulativo; cada arquivo resolve caso real do mockup.

---

## 1. Contexto e decisão de rotas

O canônico são as **11 telas do mockup** (`requisitos.md` §2), prefixo `/(app)/vendas/**`, sem second sidebar (submenu colapsável na sidebar principal, `requisitos.md` §4.4). Divergências contra o `requirements.md` atual (4 rotas, "marketplace/CRM mockup pendente") estão reconciliadas no **§8** (só documentadas aqui).

| # | Tela | Rota canônica | Mockup section |
|---|---|---|---|
| 1 | Clientes (lista) | `/vendas/clientes` (`?search=&tipo=&tags=&ordenar=&page=&pageSize=`) | `clientes` |
| 2 | Cliente (detalhe) | `/vendas/clientes/[id]` (`?tab=`) | `cliente-detalhe` |
| 3 | Novo cliente | `/vendas/clientes/novo` | `novo-cliente` |
| 4 | Orçamentos (lista) | `/vendas/orcamentos` (`?tab=&search=&page=&pageSize=`) | `orcamentos` |
| 5 | Novo orçamento (página cheia) | `/vendas/orcamentos/novo` | `novo-orcamento` |
| 6 | Orçamento (detalhe) | `/vendas/orcamentos/[id]` | `orcamento-detalhe` |
| 7 | Encomendas (Kanban) | `/vendas/encomendas` (`?status_kanban=&search=&clienteId=`) | `encomendas` |
| 8 | Encomenda (detalhe) | `/vendas/encomendas/[id]` (`?tab=`) | `encomenda-detalhe` |
| 9 | Marketplace | `/vendas/marketplace` (`?tab=&page=`) | `marketplace` |
| 10 | Marketing | `/vendas/marketing` (`?search=&status=&prioridade=&page=`) | `marketing` |
| 11 | Solicitações de edição | `/vendas/solicitacoes` (`?tab=`) | `solicitacoes` |

> Sem rota `/vendas/interacoes` — interações vivem como **timeline no detalhe do cliente** (decisão §5.3). Encomenda nasce sempre na **Fila** (conversão de orçamento aprovado, venda direta ou marketplace).

---

## 2. Estado real arquivo-a-arquivo (frontend)

### 2.1 Rotas (`src/routes/(app)/`)

| Arquivo | Estado | Nota |
|---|---|---|
| `(app)/+layout.ts` | ✅ OK | Guarda auth global (`fetchMe` + redirect). Não tocar. |
| `(app)/vendas/**` | 🔴 **G-1** | **Não existe** (só `configuracoes dashboard estoque notificacoes pessoas producao`). As 11 telas + 1 layout serão create. |
| `(app)/estoque/+layout.ts` | ✅ referência | Guarda `canView → redirect` + `canEdit` no `load` (replicar em `vendas/+layout.ts`). |
| `(app)/notificacoes/+layout.ts` | ✅ referência | Guarda Admin por pathname (`isAdmin`, redirect 303). Padrão para `/vendas/solicitacoes` (`canDecide`). |

### 2.2 API (`src/lib/api/`)

| Arquivo | Estado | Nota |
|---|---|---|
| `client.ts` | ✅ OK | `apiFetch` (timeout 15s, Bearer, 401→logout+goto sanitizado, `ApiError/NetworkError`). Base de toda API nova. Não tocar. |
| `api/vendas/*` | 🔴 **G-2** | **Inexistente** (`ls api/vendas*` → vazio). 7 módulos PT sobre `apiFetch` serão create (§4). Gateway não expõe `/api/vendas/**`; `vendas-service` é scaffold vazio (Dívida 1) — tudo 🔴/🟡 (§10). |
| `dashboard.ts` / `notifications.ts` / `producao/*` / `configuracoes/*` / `stock/*` / `rh/*` | ✅ OK | Padrão `bearer()` local + `apiFetch`. Não tocar. |

### 2.3 Tipos e utils

| Arquivo | Estado | Nota |
|---|---|---|
| `types/auth.ts` | ✅ OK | `Role 0–4`, `Module` inclui `vendas`, `User { responsibilities?: Partial<Record<Module,string[]>> }`. Base do `canDecide` (responsável de Vendas). |
| `types/vendas.ts` | 🔴 **G-3** | Inexistente. Criar DTOs PT (Cliente, Tag, Orçamento+Item, Encomenda+Histórico, Marketplace, Interação, TarefaMarketing, Solicitação, paginados). |
| `utils/permissions.ts` | 🟡 **G-7** | `VIEW_RULES.vendas=[0,1,2,3]` ✅ (Recrutando 4 já fora) e `EDIT_RULES.vendas=[0]` existem, mas **sem** helpers de criador: sem `canEditVendas(user, record)` (`isAdmin \|\| createdBy === user.id`), sem `canDecideVendas` (Admin ou responsável de Vendas), sem `isResponsavelVendas`. Completar por helpers, sem alterar as regras. |
| `utils/format.ts` | ✅ OK | `initials`, `relativeTime`, `formatNumber` — **REUSAR** (R-1). Não cobre moeda BRL nem datas `dd/mm/yyyy`. |
| `utils/vendas-status.ts` | 🔴 **G-4** | Inexistente. Criar **só** mapas `{label,color}` canônicos (mockup §7.8). |
| `utils/vendas-format.ts` | 🔴 **G-4b** | Inexistente. Criar **só** `formatMoneyBRL` (`Intl`), `formatDateBR`, prefixos de código (`CLI-/OC-/EN-`). Nada além. |
| `utils/errors.ts` | ✅ OK | `toUserMessage` cobre Network/5xx/UNKNOWN. Reusar (409 inline). |

### 2.4 Componentes (`src/lib/components/ui/` — 25 arquivos)

✅ Prontos e **obrigatoriamente reusados**: `Avatar, SearchInput, Dropdown, Checkbox (indeterminate), Select, SearchCombo, StatusBadge, TypeBadge, Pagination, TableSkeleton (+Skeleton), EmptyState, ConfirmDialog, Chip, PageHeader, Tabs, Modal, ErrorBanner, BulkActionsBar, RowActions, RadioCards, Icon, ToastHost, DonutChart`.
**`KanbanBoard.svelte` ✅ existe e será REUSADO sem edição** (colunas genéricas + `children` snippet `{columnId, card}` — cobre mover-por-clique via popover na página; ver R-2). Dono do componente = UI kit (neutro); dono da **tela** Kanban de encomendas = Vendas; **Produção reusa por link, sem tela duplicada** (§5.5, R-2).

| Faltante | Gap | Decisão |
|---|---|---|
| `Timeline.svelte` | 🔴 **G-5a** | **Create** (1 arquivo): timeline vertical (interações do cliente, histórico de orçamento/encomenda). Nada no kit cobre. |
| `MoneyInput.svelte` | 🔴 **G-5b** | **Create** (1 arquivo): input monetário `Intl` pt-BR (bruto, taxa, líquido no marketplace). Nada no kit cobre. |

Nada mais de UI novo (filtros/skeletons/empties/errors/modais/tabs/chips/KPIs são composição do kit — precedente notificações R-3).

### 2.5 Componentes de domínio / stores

- `SolicitarEdicaoModal` (modal-solicitar-edicao: Tipo · Alvo · Campo · Valor atual · Valor proposto\* · Justificativa\*) — 🔴 **G-8b**; **1** componente compartilhado em `routes/(app)/vendas/components/` (usado por todos os detalhe/listas; cria a solicitação decidida na tela 11).
- Modais `modal-nova-interacao`, `modal-item-orcamento`, `modal-nova-encomenda`, `modal-registrar-venda`, `modal-nova-tarefa`, `modal-decisao-solicitacao` — composição local de cada tela (`Modal` do kit); **não** são arquivos próprios (R-4).
- `stores/auth.ts`, `stores/toast.ts` ✅ reusar. **Sem** store novo de vendas (dado via fetch + refetch; precedente notificações R-1).
- `components/layout/Sidebar.svelte` ✅ renderiza `children` filtrados por `canSee` — nenhum toque além do `menu.ts`.

### 2.6 Config (`src/lib/config/menu.ts`)

| Estado | Nota |
|---|---|
| 🟡 **G-6** | Item `Vendas & CRM → /vendas (module vendas)` existe (linha 48) mas **sem `children`**. Adicionar submenu: Clientes · Orçamentos · Encomendas · Marketplace · Marketing · Solicitações de edição. Crumb atual de `/vendas/x` cai em `beautify` genérico — submenu corrige. |

### 2.7 Testes existentes

- `tests/unit/permissions.test.ts` — RBAC base; estender com casos vendas (G-7) sem quebrar base.
- `tests/unit/format.test.ts`, `client.test.ts`, `errors.test.ts` — cobrem o que será reusado.
- Zero testes de vendas → **G-12**. Infra: vitest+jsdom+testing-library (sem Playwright → e2e **skip**, padrão das features).

---

## 3. Gaps (G-1 … G-12)

| Gap | Descrição | Tipo |
|---|---|---|
| **G-1** | 11 rotas `/vendas/**` + 1 layout (guarda) inexistentes. | Frontend |
| **G-2** | `api/vendas/*` inexistente (7 módulos PT sobre `apiFetch`); gateway sem `/api/vendas/**`; `vendas-service` scaffold vazio — todos os endpoints 🔴/🟡. | Contrato 🔴 |
| **G-3** | `types/vendas.ts` inexistente (Cliente/Tag/Orçamento/Encomenda/Marketplace/Interação/TarefaMarketing/Solicitação + paginados). | Tipos |
| **G-4** | `utils/vendas-status.ts` + `vendas-format.ts` inexistentes (mapas §7.8; moeda/data/códigos). | Frontend |
| **G-5** | UI kit: `Timeline` e `MoneyInput` inexistentes (`KanbanBoard` ✅ reusado sem edição). | Frontend |
| **G-6** | `menu.ts` sem submenu Vendas. | Frontend+RBAC |
| **G-7** | `permissions.ts` sem helpers de criador (`canEditVendas`, `canDecideVendas`, `isResponsavelVendas`). | RBAC |
| **G-8** | Fluxo de permissão: `SolicitarEdicaoModal` compartilhado + tela `/vendas/solicitacoes` (listagem/decisão 🟡, D-4) + "Sugerir alteração" no lugar de Editar quando `!canEdit`. | Frontend+contrato 🟡 |
| **G-9** | Kanban: mover **por clique** via `PUT …/kanban` + popover + lock otimista (409 → toast) + `historico_status_encomenda`; encomenda nasce na **Fila**. | Frontend+contrato 🔴 |
| **G-10** | Marketplace: `valorTaxa` em R$ (nunca %), `valorLiquido = valor_final − valorTaxa` e `totais` **servidos pelo backend** (nunca recalculados; só preview no modal). Botão "Exportar" do mockup **sem endpoint → fora do MVP** (D-6). | Contrato 🔴/🟡 |
| **G-11** | Marketing (`tarefas-marketing`) + interações como timeline no detalhe (sem rota própria) + novo-orçamento em página cheia com `modal-item-orcamento` (origem estoque/compra; extensão `material/horas/compra` 🟡 D-5). | Frontend+contrato 🟡 |
| **G-12** | Zero testes unit do módulo. | Testes |

---

## 4. Camadas / arquivos a criar ou editar

**Create (27):**
- `src/lib/types/vendas.ts` (G-3)
- `src/lib/api/vendas/clientes.ts` · `orcamentos.ts` · `encomendas.ts` · `marketplace.ts` · `interacoes.ts` · `marketing.ts` · `solicitacoes.ts` (G-2/G-8/G-9/G-10/G-11; tags em `clientes.ts`)
- `src/lib/utils/vendas-status.ts` · `vendas-format.ts` (G-4)
- `src/lib/components/ui/Timeline.svelte` · `MoneyInput.svelte` (G-5)
- `src/routes/(app)/vendas/+layout.ts` (guarda: Recrutando nega; `/solicitacoes` exige `canDecide`) (G-1)
- `src/routes/(app)/vendas/components/SolicitarEdicaoModal.svelte` (G-8)
- 11 telas: `clientes/+page` (+`+page.ts`) · `clientes/[id]/+page` · `clientes/novo/+page` · `orcamentos/+page` · `orcamentos/novo/+page` · `orcamentos/[id]/+page` · `encomendas/+page` · `encomendas/[id]/+page` · `marketplace/+page` · `marketing/+page` · `solicitacoes/+page` (G-1/G-8/G-9/G-10/G-11)
- Testes: `tests/unit/vendas-status-format.test.ts` · `vendas-api.test.ts` · `vendas-permissions.test.ts` · `vendas-pages.test.ts` (G-12)

**Modify (2):**
- `src/lib/utils/permissions.ts` (G-7 — **só** adicionar helpers; `VIEW_RULES`/`EDIT_RULES` intactos)
- `src/lib/config/menu.ts` (G-6 — adicionar `children` ao item existente)

**Não criar** (enxugamento §7): `KanbanBoard` novo, stores vendas, `api/vendas/unread`-like, componentes de estado por tela, modais como arquivos próprios, export CSV, second sidebar, rota `/vendas/interacoes`.

---

## 5. ConflictMap (arquivos compartilhados)

| Arquivo | Dono/Impacto |
|---|---|
| `src/lib/config/menu.ts` | 🔶 Vendas adiciona `children` ao item `Vendas & CRM` existente. **Livre agora** (rh/notificações/configurações concluídas; nenhuma feature ativa o toca). Próxima feature que tocar menu (financeiro) executa **sequencial**. |
| `src/lib/utils/permissions.ts` | 🔶 Vendas adiciona `canEditVendas/canDecideVendas/isResponsavelVendas` **sem alterar** `VIEW_RULES`/`EDIT_RULES`. Sequencial com qualquer feature que toque o arquivo. Estoque/produção/rh/config já concluídos — sem conflito ativo. |
| `src/lib/components/ui/KanbanBoard.svelte` | ✅ **Reuse sem edição** (dono: UI kit, criado no RH). Vendas consome via props + `children` snippet; Produção linka a tela de Vendas (sem duplicar). Nenhum conflito. |
| `docs/**` + `requirements.md` | ⛔ **Congelados**. Reconciliação só documentada aqui (§8); dívidas ficam no §10, nunca em `docs/contract.gap.md`. |

---

## 6. Decisões de enxugamento (código mínimo)

| # | Decisão | Detalhe |
|---|---|---|
| **R-1** | **REUSADO** `format.ts` (`initials`/`relativeTime`/`formatNumber`), `errors.ts`, stores `auth`/`toast`, kit (~24 comps) | `vendas-format.ts` carrega **só** o que falta (moeda BRL, data BR, prefixos CLI-/OC-/EN-). Sem store vendas, sem componentes de estado por tela. |
| **R-2** | **REUSADO** `KanbanBoard.svelte` do kit (dono neutro) — **dono único da tela = Vendas** | Mover-por-clique = popover na página + `onMove` → `PUT …/kanban`; sem drag-lib, sem `KanbanBoard` novo. **Produção não duplica**: linka `/vendas/encomendas` (§5.5). |
| **R-3** | API em 7 módulos PT sobre `apiFetch` (padrão notificações/rh) | Espelha os recursos §5.3 (`clientes, orcamentos, encomendas, marketplace, interacoes, tarefas-marketing, tags→clientes.ts`) + `solicitacoes.ts` 🟡. `1 arquivo = 1 task`. Query só com params não-nulos; erros como `ApiError` (409 inline). |
| **R-4** | **1** componente de domínio (`SolicitarEdicaoModal`); demais modais = composição local com `Modal` do kit | `modal-nova-interacao/item-orcamento/nova-encomenda/registrar-venda/nova-tarefa/decisao` vivem dentro das telas que os usam. Sem pasta de modais. |
| **R-5** | Sem mock local, sem banner "contrato pendente" | Padrão **configurações**: endpoints 🔴/🟡 chamados direto; falha → estado Error amigável por página/bloco. Sem `USE_VENDAS_MOCK`. |
| **R-6** | Guarda em **1** `vendas/+layout.ts` | `canView(vendas)` nega Recrutando (redirect); `/solicitacoes` exige `canDecideVendas` (redirect 303 `/vendas/clientes`). `canEdit` via contexto/`canEditVendas(user, record)`. Sem `+page.ts` além do par mínimo da lista (SPA). |
| **R-7** | Marketplace: totais do backend, nunca somados no client | KPIs `bruto/taxas/líquido` e `counts` renderizam o servido; após mutação → refetch. Preview de líquido **só** no modal de registro. Botão "Exportar" do mockup **fora do MVP** (D-6, sem endpoint). |
| **R-8** | Orçamento: subtotal = `qtd × valorUnitario`; `material/horas` = custo informativo | Não alteram preço; item fora do estoque sai com `compra: true` (sugestão). Extensão de DTO 🟡 (D-5). |
| **R-9** | **NÃO planejado**: rota `/vendas/interacoes`, export CSV, envio de orçamento por e-mail, painel do cliente, recibo interno, integração automática com plataformas, auditoria avançada | Tudo §10 do mockup (fora de escopo) ou sem endpoint. Interações = timeline no detalhe do cliente. |

**Explicitamente NÃO planejado** (por não resolver caso real): `KanbanBoard` duplicado; stores vendas; soma de totais/líquido no client; `pageSize`/`sort` além do URL state do §1; notificações de prazo/status (dono: Notification Service); alteração de encomenda fechada (gera nova ordem — só documentar); dados de `producao/bom` e `rh/pessoas` além de leitura para selects (integrações 🔴, Dívida 5).

---

## 7. Ordem em Blocos

- **Bloco 1 — Fundação** (vnd-001..vnd-013): types → 7 APIs → `vendas-status` → `vendas-format` → `Timeline` → `MoneyInput` → `permissions.ts` helpers.
- **Bloco 2 — Clientes** (vnd-014..vnd-018): guarda `vendas/+layout.ts` → lista → `SolicitarEdicaoModal` → detalhe (tabs + timeline) → novo.
- **Bloco 3 — Orçamentos** (vnd-019..vnd-021): lista (tabs+KPIs) → novo página cheia (`modal-item-orcamento`) → detalhe (banner de conversão).
- **Bloco 4 — Encomendas** (vnd-022..vnd-023): Kanban (reuse `KanbanBoard` + popover + `modal-nova-encomenda`) → detalhe (tabs + avançar etapa).
- **Bloco 5 — Marketplace/Marketing/Solicitações** (vnd-024..vnd-026): marketplace (tabs+totais) → marketing → solicitações (decisão).
- **Bloco 6 — Menu** (vnd-027): `menu.ts` submenu (depende de todas as rotas existirem).
- **Bloco 7 — Testes** (vnd-028..vnd-032): status+format → api mock → permissions/RBAC → páginas; e2e **skip** (sem Playwright).

Dependências: **B1 → B2 → B3 → B4 → B5 → B6 → B7** (B7 parcial desde B1 para status/format; B3/B4 paralelizáveis entre si após B2).

---

## 8. Reconciliação requirements × mockup × §5.3 (limpeza documentada, SEM editar o requirements)

| # | Divergência | Autoridade | Limpeza necessária no `requirements.md` |
|---|---|---|---|
| C-1 | Requirements lista **4 rotas**; mockup tem **11 telas** (falta `clientes/novo`, `orcamentos/novo` página cheia, `marketing`, `solicitacoes`, detalhe de encomenda) | Mockup §2 | Expandir `Routes` para as 11 rotas do §1. |
| C-2 | Requirements: "marketplace/CRM mockup pendente"; mockup **cobre** Marketplace (tela 9) e CRM (timeline no detalhe do cliente) | Mockup §§8.2/8.9 | Remover a nota "mockup pendente". |
| C-3 | Requirements: `PATCH /{id}/aprovar\|recusar`, `PATCH /{id}/mover` | §5.3 + mockup §7.2 | Trocar por `POST /encomendas {idOrcamento}` (conversão, nasce na Fila) e `PUT /encomendas/{id}/kanban` (mover por clique). |
| C-4 | Requirements RBAC: "Edit: Admin; Bolsista/Voluntário **se atribuído à encomenda**" | §5.3 | Trocar por **criador + Admin**; demais via solicitação (`canEditVendas`); decisão por Admin/responsável de Vendas (`canDecideVendas`). Modelo vendas não tem "atribuído" — tem `createdBy`. |
| C-5 | Requirements omite `/vendas/solicitacoes` e `/vendas/marketing`; URL Kanban `?coluna=` | §5.3 + mockup §§8.7/8.10/8.11 | Adicionar as 2 rotas; Kanban `?status_kanban=&search=&clienteId=`; registrar que `/vendas/interacoes` foi removida. |
| C-6 | Requirements: "Marketplace edit → consume estoque (integração evento)" | Mockup §10 (fora de escopo) | Remover/integração manual; entregues notificam Financeiro (fora das telas). |
| C-7 | Requirements: `GET /api/vendas/marketplace` (RBAC Admin) | Mockup §5 (view p/ todos; Recrutando bloqueado) | Marketplace **não** é Admin-only; corrigir `Access`. |

---

## 9. Checklists obrigatórias

### Validator §5 (todas as tasks de tela/API)
- [ ] Svelte 5 runes (`$props/$state/$derived/$effect`), sem `export let`;
- [ ] TS estrito (sem `any`; `import type` para tipos); unions literais PT de status (`OrcamentoStatus/KanbanStatus/Plataforma/TarefaStatus/Prioridade/TipoInteracao/SolicitacaoStatus`);
- [ ] A11y: `aria-*` nos controles, labels em inputs, `role="tablist"`/`aria-selected` nas tabs, `role="dialog"`+Escape+backdrop nos modais, foco visível; **RBAC = HIDE, nunca disable** (`!canEdit` → "Sugerir alteração", sem controles de escrita no DOM);
- [ ] URL como fonte de verdade (§1) via `goto`+refetch; "Limpar filtros" volta à URL base; debounce 300ms + `AbortController` nas buscas;
- [ ] i18n pt-BR 100%; status exibidos **direto do backend, sem tradução** (só cor via `vendas-status.ts`); moeda `Intl` BRL; datas `dd/mm/yyyy`; códigos `font-mono`;
- [ ] Estados sem layout shift: Loading (skeleton na geometria), Success, Empty, Empty filtrado (chips+limpar), Error (banner+retry), RBAC-restrito, erro parcial por bloco/tab; Kanban: coluna vazia com empty mini; erro de mover → toast no card;
- [ ] Após mutação: refetch da página atual (ou última página válida ao excluir); `409` documento/e-mail duplicado inline; `data-testid` kebab-case do mockup (`cliente-*`, `orc-*`, `en-card`, `move-en*`, `mp-*`, `mk-*`, `sol-*`).

### Security §6 (idem)
- [ ] Bearer via `apiFetch` (helper `bearer()` local por módulo api); 401 → logout+goto sanitizado (global); sem credenciais hardcoded; sem `console.log`;
- [ ] Guardas client-side = **defesa em profundidade** (autoridade é o backend — D-3): `vendas/+layout.ts` (`canView` + `canDecide` em `/solicitacoes`), ações `hide` por `canEditVendas`;
- [ ] 403 → mensagem "sem permissão" na tela (sem crash); 404 cliente/orçamento/encomenda → `EmptyState` + voltar;
- [ ] Sem `{@html}`/injeção; `motivo`/justificativa renderizados como texto;
- [ ] CPF/CNPJ mascarados em leitura (`***.000.42*-**`); retenção/máscara a confirmar (D-7, LGPD);
- [ ] e2e sem credenciais hardcoded (sem Playwright → **skip** documentado).

---

## 10. Riscos de contrato (endpoints — quase tudo 🔴)

| Endpoint | Método | Classe | Status |
|---|---|---|---|
| `GET/POST /api/vendas/clientes` · `GET/PUT /api/vendas/clientes/{id}` | GET/POST/PUT | 🔴 | `vendas-service` scaffold vazio (Dívida 1); `PUT /{id}` e `GET` lista 🟡 assumidos (mockup §7.2) |
| `GET /api/vendas/clientes/{id}/interacoes` · `POST /api/vendas/interacoes` | GET/POST | 🔴/🟡 | Recurso PT em §5.3; timeline sem rota própria |
| `GET/POST /api/vendas/tags` | GET/POST | 🔴/🟡 | Recurso PT em §5.3 |
| `GET/POST /api/vendas/orcamentos` · `GET/PUT /api/vendas/orcamentos/{id}` | GET/POST/PUT | 🔴/🟡 | `GET`s 🟡 assumidos (mockup §7.2); extensão `material/horas/compra` 🟡 (D-5) |
| `POST /api/vendas/encomendas` (`{idOrcamento}`) · `GET /api/vendas/encomendas` · `GET /{id}` | POST/GET | 🔴/🟡 | Conversão nasce na Fila; `GET /{id}` 🟡 assumido |
| `PUT /api/vendas/encomendas/{id}/kanban` (+ lock otimista; 409) | PUT | 🔴 | Registra `historico_status_encomenda`; concorrência a confirmar (D-9) |
| `GET/POST /api/vendas/marketplace` | GET/POST | 🔴 | `valorTaxa` R$; bruto/líquido/totais servidos (D-2) |
| `GET/POST /api/vendas/tarefas-marketing` · `PUT /{id}` | GET/POST/PUT | 🔴 | — |
| `GET /api/vendas/solicitacoes` · decisão (aprovar/rejeitar + motivo) | GET/PUT | 🟡 | **Sem lastro em §5.3/docs/04** — assumidos p/ tela 11 (D-4) |
| `GET /api/producao/bom?itemId=` · `GET /api/rh/pessoas` (responsáveis) | GET | 🔴 | Integrações (Dívida 5); selects com fallback manual |

> Falha de contrato → estado **Error** amigável por página/bloco (padrão configurações, sem mock). Nada bloqueia o frontend além do backend.

### 10.1 Decisões que exigem backend/PO (fila — padrão D-x, sem inventar endpoint)
- **D-1 🔴** — Implementar `vendas-service` + expor `/api/vendas/**` no gateway (Dívida 1).
- **D-2 🟡** — Confirmar shapes paginados (`clientes+pagination+filters`, `orcamentos+counts`, `encomendas+counts`, `marketplace+totais`, `tarefas+counts`, `solicitacoes+counts`); totais/counts **servidos, nunca derivados**.
- **D-3 🔴** — Enforcement server-side criador+Admin (`@PreAuthorize` por recurso) + `canDecide` Admin/responsável-Vendas; definir como o backend identifica "responsável de Vendas" (`responsibilities.vendas`?); guards aqui são depth-only.
- **D-4 🟡** — Solicitações: expor listagem/decisão/criação; confirmar máquina de status e motivo obrigatório ao rejeitar.
- **D-5 🟡** — Confirmar extensão `item_orcamento` (`material{ tipo,quantidade,unidade }`, `horas`, `compra`) — mockup §7.2 documenta como extensão.
- **D-6 🟡** — Exportar orçamentos (botão no mockup, sem endpoint): confirmar ou remover do MVP.
- **D-7 🟡** — CPF/CNPJ: validação, máscara em leitura, retenção (LGPD); 409 duplicado inline.
- **D-8 🟡** — Mensagens de erro não-técnicas (`code` estruturado) — ressalva transversal.
- **D-9 🟡** — Kanban: confirmar lock otimista (409 → "atualize a tela") e transições válidas entre colunas.

---

## 11. Critérios de Entrega (DoD da feature)
- [ ] 11 telas `/vendas/**` funcionais sobre API real (SPA), URL state deep-linkável, `data-testid` espelhando `full.html`.
- [ ] Kanban 5 colunas com mover **por clique** (popover) + histórico servido; encomenda sempre nasce na **Fila**; **sem** `KanbanBoard` novo; Produção sem tela duplicada.
- [ ] RBAC hide-real: `!canEdit` troca Editar por "Sugerir alteração"; `/solicitacoes` só `canDecide`; Recrutando sem acesso; `EDIT_RULES`/`VIEW_RULES` intactos.
- [ ] API 100% PT nova (`api/vendas/*.ts` via `apiFetch`); marketplace sem soma client; orçamento sem alterar preço por custo.
- [ ] `menu.ts` com submenu Vendas (6 itens); `permissions.ts` só adiciona helpers.
- [ ] Testes unit verdes (`npm test`); `npm run check` sem erros novos; `npm run build` ok; e2e documentado como **skip**.
- [ ] Nenhuma alteração em `docs/**` nem `requirements.md`; endpoints 🔴/🟡 reportados (nunca silenciados).

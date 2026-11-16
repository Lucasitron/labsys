# Tasks — Vendas & CRM (frontend, 11 telas)

> Derivado de `requirements.md` (reconciliado no `plan.md` §8) + `plan.md` desta pasta + mockup `docs/mokups/vendas/` (`full.html` 11 sections, `requisitos.md` v0.1, `instrunctions.md`).
> Cada task = **1 arquivo real**. Execução por blocos (deps abaixo); B3/B4 paralelizáveis entre si após B2.
> Legenda: 🟡 contrato assumido (sem evidência de backend/gateway) · 🔴 contrato inexistente/divergente (requer decisão D-x) · ⚠️ arquivo compartilhado — **sequencial**.
> Princípio código mínimo: reuso > create (kit ~24 + `format.ts` + `KanbanBoard` + stores); nada especulativo; rotas canônicas `/vendas/**`.
> Critério global: pt-BR; Svelte 5 runes; TS estrito (sem `any`); `data-testid` kebab-case do mockup; URL fonte de verdade; RBAC **HIDE** (sem Editar → "Sugerir alteração"); debounce 300ms + `AbortController`; `Intl` pt-BR; sem `{@html}`; sem `console.log`.

---

## B1 — Fundação (types + API + utils + UI + RBAC)

### vnd-001 · types do módulo
- **Arquivo:** `src/lib/types/vendas.ts` — create
- **Conteúdo:** `TipoPessoa='pf'|'pj'`; `OrcamentoStatus='Pendente'|'Aprovado'|'Recusado'|'Ajuste'`; `KanbanStatus='Fila'|'Produção'|'Acabamento'|'Pronto'|'Entregue'`; `OrdemOrigem='Orçamento'|'Venda direta'|'Marketplace'`; `Plataforma='Mercado Livre'|'Shopee'|'Elo7'`; `TipoInteracao='E-mail'|'Telefone'|'Reunião'|'WhatsApp'`; `TarefaStatus='Pendente'|'Em Andamento'|'Concluída'`; `Prioridade='Baixa'|'Média'|'Alta'`; `TipoSolicitacao='Alteração de dados'|'Mudança de status'|'Mover encomenda'|'Outra'`; `SolicitacaoStatus='Pendente'|'Aprovada'|'Rejeitada'`. Modelos: `Tag{id,nome,cor}`, `Cliente` (+`codigo,tipoPessoa,documento,tags,createdBy,updatedAt`), `Orcamento` (+`codigo,cliente,valorTotal,validade,qtdItens,createdBy`), `ItemOrcamento{descricao,quantidade,valorUnitario,material?,horas?,compra?}` (extensão 🟡 D-5), `Encomenda` (+`codigo,statusKanban,origem,previsao,valorFinal,itensCount`), `HistoricoEncomenda{statusAnterior,statusNovo,dataAlteracao,responsavel,observacao}`, `RegistroMarketplace` (bruto/taxa/líquido servidos), `Interacao`, `TarefaMarketing`, `SolicitacaoEdicao`, `Paged<T>` genérico + params de lista.
- **Saída:** tipos que alimentam as 7 APIs + 11 telas + testes.
- **DoD:** sem `any`; `import type` nos consumidores; compila (`svelte-check`).
- **Deps:** — · **Prioridade:** Alta

### vnd-002 · API clientes (+ tags)
- **Arquivo:** `src/lib/api/vendas/clientes.ts` — create 🔴/🟡
- **Conteúdo:** helper `bearer()` local + `apiFetch` de `client.ts`; `listClientes({search,tipo,tags,ordenar,page,pageSize}) → GET /api/vendas/clientes` 🔴 · `getCliente(id) → GET /api/vendas/clientes/{id}` 🟡 · `createCliente(payload) → POST /api/vendas/clientes` 🔴 (409 duplicado → inline D-7) · `updateCliente(id,payload) → PUT /api/vendas/clientes/{id}` 🟡 · `listTags() → GET /api/vendas/tags` 🟡 · `createTag(payload) → POST /api/vendas/tags` 🟡 · `deleteCliente(id)` 🔴 · `bulkAdicionarTag(ids,tagId)` 🔴 (a confirmar D-2). Query só com params não-nulos.
- **Saída:** camada das telas 1–3 + selects de cliente das telas 5/7/9.
- **DoD:** nenhum path fora de `/api/vendas/**`; erros como `ApiError`; contrato 🔴/🟡 reportado no relatório do bloco.
- **Deps:** vnd-001 · **Prioridade:** Alta

### vnd-003 · API orçamentos
- **Arquivo:** `src/lib/api/vendas/orcamentos.ts` — create 🔴/🟡
- **Conteúdo:** `listOrcamentos({status,search,clienteId,page,pageSize}) → GET /api/vendas/orcamentos` 🟡 · `getOrcamento(id) → GET …/{id}` 🟡 · `createOrcamento(payload) → POST /api/vendas/orcamentos` 🔴 · `updateOrcamento(id,payload) → PUT …/{id}` 🔴 · `duplicarOrcamento(id) → POST` 🔴 (a confirmar D-2; fallback client-side proibido — só chamar se o backend confirmar, senão remover a ação). Via `apiFetch`.
- **Saída:** camada das telas 4–6.
- **DoD:** DTOs `Orcamento/ItemOrcamento` de vnd-001; D-2/D-5 registrados.
- **Deps:** vnd-001 · **Prioridade:** Alta

### vnd-004 · API encomendas (Kanban por clique)
- **Arquivo:** `src/lib/api/vendas/encomendas.ts` — create 🔴/🟡
- **Conteúdo:** `listEncomendas({status_kanban,search,clienteId}) → GET /api/vendas/encomendas` 🔴 · `getEncomenda(id) → GET …/{id}` 🟡 · `createEncomenda({idOrcamento}|venda direta) → POST /api/vendas/encomendas` 🔴 (nasce na Fila) · `moverKanban(id,{statusKanban,observacao}) → PUT …/{id}/kanban` 🔴 (lock otimista; 409 → toast "atualize a tela" D-9). Via `apiFetch`. **Nenhuma derivação de coluna/status no client.**
- **Saída:** camada das telas 7–8 (Produção consome por link, sem duplicar).
- **DoD:** export sem `proximoEstagio|contarPorColuna`; D-9 registrado.
- **Deps:** vnd-001 · **Prioridade:** Alta

### vnd-005 · API marketplace
- **Arquivo:** `src/lib/api/vendas/marketplace.ts` — create 🔴
- **Conteúdo:** `listMarketplace({plataforma,page,pageSize}) → GET /api/vendas/marketplace` 🔴 (`{registros,totais:{bruto,taxas,liquido},counts}` servidos) · `registrarVenda(payload) → POST /api/vendas/marketplace` 🔴 (`valorTaxa` em R$, nunca %). Via `apiFetch`. **Nenhuma soma de totais/líquido no client** (R-7).
- **Saída:** camada da tela 9.
- **DoD:** export sem `somar|totalizar|liquido(`; D-2 registrado.
- **Deps:** vnd-001 · **Prioridade:** Alta

### vnd-006 · API interações
- **Arquivo:** `src/lib/api/vendas/interacoes.ts` — create 🔴/🟡
- **Conteúdo:** `listInteracoes(clienteId) → GET /api/vendas/clientes/{id}/interacoes` 🟡 · `registrarInteracao(payload) → POST /api/vendas/interacoes` 🟡 (`{clienteId,tipo,descricao,dataInteracao,responsavelId}`). Via `apiFetch`. Sem rota `/vendas/interacoes` (timeline no detalhe — §5.3).
- **Saída:** camada da aba Interações da tela 2.
- **DoD:** DTOs `Interacao` de vnd-001.
- **Deps:** vnd-001 · **Prioridade:** Alta

### vnd-007 · API marketing
- **Arquivo:** `src/lib/api/vendas/marketing.ts` — create 🔴
- **Conteúdo:** `listTarefas({search,status,prioridade,page,pageSize}) → GET /api/vendas/tarefas-marketing` 🔴 (`{tarefas,counts}` servidos) · `createTarefa(payload) → POST` 🔴 · `updateTarefa(id,payload) → PUT …/{id}` 🔴. Via `apiFetch`.
- **Saída:** camada da tela 10.
- **DoD:** DTOs `TarefaMarketing` de vnd-001.
- **Deps:** vnd-001 · **Prioridade:** Média

### vnd-008 · API solicitações
- **Arquivo:** `src/lib/api/vendas/solicitacoes.ts` — create 🟡
- **Conteúdo:** `listSolicitacoes({status}) → GET /api/vendas/solicitacoes` 🟡 · `createSolicitacao(payload) → POST` 🟡 (`{tipo,alvo,campo,valorAtual,valorProposto,justificativa}`) · `decidirSolicitacao(id,{aprovada,motivo}) → PUT` 🟡 (motivo obrigatório ao rejeitar). Via `apiFetch`. **Sem lastro em §5.3/docs/04 — tudo a confirmar (D-4).**
- **Saída:** camada da tela 11 + `SolicitarEdicaoModal`.
- **DoD:** D-4 registrado; falha → estado Error (sem mock).
- **Deps:** vnd-001 · **Prioridade:** Alta

### vnd-009 · util de status canônicos
- **Arquivo:** `src/lib/utils/vendas-status.ts` — create
- **Conteúdo:** `orcamentoStatusMeta` (Pendente/warn, Aprovado/success, Recusado/danger, Ajuste/brand) · `kanbanStatusMeta` (Fila/muted, Produção/brand, Acabamento/warn, Pronto/success, Entregue/muted) · `plataformaMeta` (ML/warn, Shopee/success, Elo7/brand) · `tarefaStatusMeta` · `prioridadeMeta` (Alta/danger, Média/warn, Baixa/muted) · `interacaoTipoMeta` (E-mail/brand, Telefone/warn, Reunião+WhatsApp/success). Formato `{label,color}` (padrão `stock-status.ts`). **Sem tradução de strings — só cor.**
- **Saída:** cores/labels das 11 telas.
- **DoD:** cobertura total das unions de vnd-001; teste em vnd-028.
- **Deps:** vnd-001 · **Prioridade:** Alta

### vnd-010 · util de formatação vendas
- **Arquivo:** `src/lib/utils/vendas-format.ts` — create
- **Conteúdo:** `formatMoneyBRL` (`Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'})`) · `formatDateBR` (`dd/mm/yyyy` via `Intl`) · `codigoPrefix` (`CLI-/OC-/EN-`). **REUSAR** `initials/relativeTime/formatNumber` de `format.ts` (não duplicar).
- **Saída:** moeda/datas/códigos das 11 telas.
- **DoD:** puro (sem dependência de store); teste em vnd-028.
- **Deps:** — · **Prioridade:** Alta

### vnd-011 · UI `Timeline`
- **Arquivo:** `src/lib/components/ui/Timeline.svelte` — create
- **Conteúdo:** `$props { items: {id, tone, title, body?, meta?}[] }`; linha vertical com dot por `tone` + conteúdo; `role="list"`, `aria-label`. Uso: interações do cliente, histórico de orçamento/encomenda.
- **Saída:** timelines das telas 2/6/8.
- **DoD:** sem quebrar kit; type-check ok.
- **Deps:** — · **Prioridade:** Média

### vnd-012 · UI `MoneyInput`
- **Arquivo:** `src/lib/components/ui/MoneyInput.svelte` — create
- **Conteúdo:** `$props { value: number|null, label? }`; input com máscara `Intl` BRL (bruto, taxa, líquido); `bind:value`; validação inline pt-BR. Uso: telas 5/9.
- **Saída:** inputs monetários do módulo.
- **DoD:** valor numérico (nunca string formatada) no `bind`; type-check ok.
- **Deps:** vnd-010 · **Prioridade:** Média

### vnd-013 · RBAC helpers vendas ⚠️
- **Arquivo:** `src/lib/utils/permissions.ts` — modify ⚠️
- **Conteúdo:** adicionar **sem alterar** `VIEW_RULES`/`EDIT_RULES`: `isResponsavelVendas(user)` (responsibilities.vendas não vazio, `role!==4`) · `canEditVendas(user, record:{createdBy?:string}) = isAdmin(user) || (!!record.createdBy && user.id===record.createdBy)` · `canDecideVendas(user) = isAdmin(user) || isResponsavelVendas(user)` (§5.3 + mockup §5). Criação liberada a todos autenticados (sem helper — `canView` basta).
- **Saída:** RBAC das 11 telas + guarda + menu.
- **DoD:** ⚠️ **sequencial** (compartilhado; demais features concluídas); base de `permissions.test.ts` verde; casos novos em vnd-030.
- **Deps:** — · **Prioridade:** Alta

---

## B2 — Clientes (lista · modal solicitação · detalhe · novo)

### vnd-014 · guarda do módulo
- **Arquivo:** `src/routes/(app)/vendas/+layout.ts` — create
- **Conteúdo:** `canView(user,'vendas')` ou redirect `302 /dashboard?denied=` (padrão estoque; Recrutando já excluído de `VIEW_RULES`); retorna `{ canEdit: isAdmin(user) }` base; se path `/vendas/solicitacoes*` e `!canDecideVendas(user)` → `redirect(303,'/vendas/clientes')`. `ssr=false/prerender=false`.
- **Saída:** proteção das 11 rotas (Solicitações só `canDecide`).
- **DoD:** não-Admin em `/solicitacoes` redirecionado (nunca 403 em tela); sem duplicar `fetchMe`.
- **Deps:** vnd-013 · **Prioridade:** Alta

### vnd-015 · Clientes — lista (tela 1)
- **Arquivo:** `src/routes/(app)/vendas/clientes/+page.svelte` (+ `+page.ts` no mesmo task) — create
- **Conteúdo:** URL `?search=&tipo=&tags=&ordenar=&page=&pageSize=` (fonte de verdade); `SearchInput` debounce 300ms + 3 `Dropdown` (Tipo/Tags/Ordenar, AND entre painéis) + `Chip`s + "Limpar filtros"; tabela (Avatar+nome/email, código mono `CLI-XXXX`, tags chips, Tipo badge, `RowActions` → Ver/Editar/`Sugerir alteração`/Duplicar/Excluir) + `BulkActionsBar` (Adicionar tag + Solicitar alteração + Limpar, com `ConfirmDialog`) + `Pagination`; `PageHeader` + "+ Novo cliente" (todos autenticados podem criar); mobile <768 cards; linha → `/vendas/clientes/{id}` (checkbox/ações com `stopPropagation`). Estados: TableSkeleton/Empty/Empty-filtrado/ErrorBanner+retry. Endpoints: `listClientes` 🔴 (`bulkAdicionarTag` 🔴 D-2).
- **Saída:** rota `/vendas/clientes`.
- **DoD:** `data-testid="cliente-row/cliente-checkbox/select-all/bulk-bar"`; `canEditVendas` decide Editar vs Sugerir (hide real).
- **Deps:** vnd-002, vnd-009, vnd-013, vnd-014 · **Prioridade:** Alta

### vnd-016 · modal compartilhado "Sugerir alteração"
- **Arquivo:** `src/routes/(app)/vendas/components/SolicitarEdicaoModal.svelte` — create
- **Conteúdo:** `$props { alvo:{tipo:'CLI'|'OC'|'EN',id,nome}, campo?, valorAtual?, onClose, onSent }`; campos Tipo de alteração · Campo · Valor atual (readonly) · Valor proposto\* · Justificativa\* → `createSolicitacao` 🟡 → toast + `onSent`. `Modal` do kit (`role="dialog"`, Escape/backdrop). `data-testid="modal-solicitar-edicao"`.
- **Saída:** fluxo de permissão usado por todas as telas (`!canEdit`).
- **DoD:** validação inline pt-BR + busy anti-duplo; teste em vnd-031.
- **Deps:** vnd-008 · **Prioridade:** Alta

### vnd-017 · Cliente — detalhe (tela 2)
- **Arquivo:** `src/routes/(app)/vendas/clientes/[id]/+page.svelte` (+loader) — create
- **Conteúdo:** voltar (preserva filtros) + "Editar" (só `canEditVendas`, senão "Sugerir alteração" → vnd-016) + ⋮; hero (Avatar, nome, `código · tipo · desde`, tags, ações ✉/📞); KPIs servidos (Total encomendado, Orçamentos em aberto, Encomendas em produção, Última interação); `Tabs` (`?tab=` visao-geral/interacoes/orcamentos/encomendas): visão (`<dl>` + relacionamento em dias), **interações** (`Timeline` + "Registrar interação" → `modal-nova-interacao` local → vnd-006), orçamentos/encomendas (mini-tabelas). Erro parcial por aba; 404 → EmptyState + voltar. Endpoints: `getCliente` 🟡 + interações 🟡 + filtros por cliente.
- **Saída:** rota `/vendas/clientes/[id]`.
- **DoD:** tabs deep-linkáveis; `data-testid="cliente-tabs"`; sem soma de KPIs no client.
- **Deps:** vnd-002, vnd-006, vnd-009, vnd-011, vnd-014, vnd-016 · **Prioridade:** Alta

### vnd-018 · Novo cliente (tela 3)
- **Arquivo:** `src/routes/(app)/vendas/clientes/novo/+page.svelte` — create
- **Conteúdo:** voltar; `PageHeader`; form em cards (Identificação: Tipo `RadioCards` PF/PJ · Nome/Razão\* · CPF/CNPJ\* mascarado+validado · Nascimento PF; Contato: E-mail\* · Telefone\* · Endereço com CEP; Segmentação: tags toggle + criar nova 🟡 + Observações); footer Cancelar/"Salvar e adicionar outro"/"Salvar cliente" (busy). `POST /api/vendas/clientes` 🔴; 409 → inline no campo (D-7); sucesso → detalhe (ou reset). `data-testid="cliente-nome/cliente-cpf-cnpj/submit-cliente"`.
- **Saída:** rota `/vendas/clientes/novo`.
- **DoD:** validação inline pt-BR + `novalidate`; documento mascarado em leitura.
- **Deps:** vnd-002, vnd-014 · **Prioridade:** Alta

---

## B3 — Orçamentos (lista · novo página cheia · detalhe)

### vnd-019 · Orçamentos — lista (tela 4)
- **Arquivo:** `src/routes/(app)/vendas/orcamentos/+page.svelte` (+ `+page.ts`) — create
- **Conteúdo:** URL `?tab=&search=&page=&pageSize=` (tabs todos/pendentes/aprovados/ajuste/recusados); `PageHeader` + "Novo orçamento" → tela 5 (**sem** "Exportar" — D-6, fora do MVP); KPIs servidos (4); tabela (Código mono, Cliente Avatar+nome, Itens, Valor mono, Validade, `StatusBadge`, ⋮ → Ver/Editar/`Sugerir alteração`/Duplicar 🔴 D-2); `Pagination`. Estados skeleton/empty-por-tab/error. Endpoints: `listOrcamentos` 🟡. `data-testid="orc-tab-pendentes/orc-row"`.
- **Saída:** rota `/vendas/orcamentos`.
- **DoD:** tab na URL; counts servidos (nunca derivados).
- **Deps:** vnd-003, vnd-009, vnd-014 · **Prioridade:** Alta

### vnd-020 · Novo orçamento — página cheia (tela 5)
- **Arquivo:** `src/routes/(app)/vendas/orcamentos/novo/+page.svelte` — create
- **Conteúdo:** voltar; `PageHeader`; card Cliente (`SearchCombo` por nome/documento + card selecionado + "Novo"); card Itens (tabela resumo + `+ Adicionar item` → overlay `modal-item-orcamento` local: origem Do-estoque [busca+materiais, "Em estoque: X"] / Fora-do-estoque [tipo livre + "Precisa ser comprado"]; comuns Descrição\*/Material\*/Qtd.material\*+unidade/Horas/Quantidade\*/Valor unit.\* + preview Subtotal; "Faltam informações: …" `role="alert"` + foco); card Resumo (`Intl` BRL: subtotal, desconto R$, TOTAL); card Prazos (Validade\* default +30d, Observações); footer sticky Cancelar/"Salvar orçamento" → `POST` 🔴 → detalhe. Validação ≥1 item + cliente. `data-testid="orc-cliente/orc-item-add/orc-total/submit-orcamento"`. Subtotal = qtd × valorUnitario (R-8).
- **Saída:** rota `/vendas/orcamentos/novo`.
- **DoD:** overlay também edita item (prefilled); sem alterar preço por custo.
- **Deps:** vnd-002, vnd-003, vnd-010, vnd-012, vnd-014 · **Prioridade:** Alta

### vnd-021 · Orçamento — detalhe (tela 6)
- **Arquivo:** `src/routes/(app)/vendas/orcamentos/[id]/+page.svelte` (+loader) — create
- **Conteúdo:** voltar + ações (Editar/`Sugerir alteração` via `canEditVendas` · Duplicar 🔴 D-2 · ⋮); **banner de conversão** (só `status==='Aprovado'` sem encomenda) → `POST /api/vendas/encomendas {idOrcamento}` 🔴 → EN na Fila + refetch; KPIs (Valor, Itens, Validade/dias, Cliente); tabela Itens; `Timeline` de histórico; card cliente + observações. Erro parcial por bloco. Endpoints: `getOrcamento` 🟡 + `createEncomenda` 🔴. `data-testid="orc-criar-encomenda/orc-encomenda-criada"`.
- **Saída:** rota `/vendas/orcamentos/[id]`.
- **DoD:** banner some após conversão (refetch, nunca estado local).
- **Deps:** vnd-003, vnd-004, vnd-011, vnd-014, vnd-016 · **Prioridade:** Alta

---

## B4 — Encomendas (Kanban · detalhe)

### vnd-022 · Encomendas — Kanban (tela 7)
- **Arquivo:** `src/routes/(app)/vendas/encomendas/+page.svelte` (+ `+page.ts`) — create
- **Conteúdo:** URL `?status_kanban=&search=&clienteId=`; `PageHeader` + "Registrar venda" → `modal-nova-encomenda` local (Cliente\* · Origem · Valor\* · Previsão · Produtos → `POST` 🔴, nasce na Fila) + busca; KPIs servidos (Em produção, Prontas, Atrasadas, Entregues/mês); **`KanbanBoard` do kit REUSADO** (5 colunas fixas Fila/muted·Produção/brand·Acabamento/warn·Pronto/success·Entregue/muted; header label+count; coluna vazia empty mini; scroll-x): card (código mono, cliente, previsão `danger` se atrasada, valor mono, origem badge, itens count, botão mover → popover `move-en{id}` com destinos → `moverKanban` 🔴 + refetch + toast; erro → toast no card); card → detalhe. Estados skeleton-colunas/error-banner. Endpoints: `listEncomendas` 🔴 + `moverKanban` 🔴. `data-testid="kanban-col-producao/en-card/move-en*"`.
- **Saída:** rota `/vendas/encomendas` (dona única — Produção linka, sem duplicar).
- **DoD:** coluna derivada do servido (nunca recalculada); 409 → toast "atualize a tela".
- **Deps:** vnd-004, vnd-009, vnd-014 · **Prioridade:** Alta

### vnd-023 · Encomenda — detalhe (tela 8)
- **Arquivo:** `src/routes/(app)/vendas/encomendas/[id]/+page.svelte` (+loader) — create
- **Conteúdo:** voltar + "Avançar etapa" (popover destino; some em Entregue) + `Sugerir alteração` se `!canEditVendas` + ⋮; hero (código mono, cliente, origem badge, `StatusBadge` da coluna, valor final); KPIs (Valor, Previsão/dias, Itens, Tempo na etapa — servidos); `Tabs` (`?tab=` visao-geral/itens/historico): visão (cliente + link OC origem + observações + marketplace vinculado), itens (tabela), histórico (`Timeline` de `historico_status_encomenda`). Endpoints: `getEncomenda` 🟡 + `moverKanban` 🔴. `data-testid="enc-avancar/enc-tab-historico"`.
- **Saída:** rota `/vendas/encomendas/[id]`.
- **DoD:** tabs deep-linkáveis; erro parcial por aba.
- **Deps:** vnd-004, vnd-009, vnd-011, vnd-014, vnd-016 · **Prioridade:** Alta

---

## B5 — Marketplace · Marketing · Solicitações

### vnd-024 · Marketplace (tela 9)
- **Arquivo:** `src/routes/(app)/vendas/marketplace/+page.svelte` (+ `+page.ts`) — create
- **Conteúdo:** URL `?tab=&page=` (tabs todos/ml/shopee/elo7); `PageHeader` + "Registrar venda externa" → `modal-registrar-venda` local (Plataforma\* · Código externo\* mono · Encomenda vinculada search+valor · Data · Taxa R$\* via `MoneyInput` → líquido = valor − taxa **só preview** → `POST` 🔴); KPIs servidos (Bruto, Taxas, **Líquido**, Vendas/período); tabela (Plataforma badge, Código mono, Encomenda link, Cliente, Data, Bruto, Taxa, Líquido mono) + card de totais do servido. Estados skeleton/empty-por-tab/error. Endpoints: `listMarketplace`/`registrarVenda` 🔴. `data-testid="mp-tab-ml/mp-liquido/mp-registrar"`.
- **Saída:** rota `/vendas/marketplace`.
- **DoD:** totais nunca recalculados (só exibidos); taxa nunca em %.
- **Deps:** vnd-005, vnd-009, vnd-010, vnd-012, vnd-014 · **Prioridade:** Média

### vnd-025 · Marketing (tela 10)
- **Arquivo:** `src/routes/(app)/vendas/marketing/+page.svelte` (+ `+page.ts`) — create
- **Conteúdo:** URL `?search=&status=&prioridade=&page=`; `PageHeader` + "Nova tarefa" → `modal-nova-tarefa` local (Título\* · Descrição · Responsável\* · Início · Prazo\* · Prioridade radio → `POST` 🔴); KPIs servidos (Pendentes, Em andamento, Concluídas, Alta prioridade); tabela (Tarefa+descrição, Responsável Avatar, Prazo mono `danger` se vencido/≤7d, Prioridade/Status badges, ⋮); filtros search+2 selects + chips. Estados skeleton/empty/error. Endpoints: `listTarefas`/`createTarefa`/`updateTarefa` 🔴. `data-testid="mk-tarefa/mk-nova"`.
- **Saída:** rota `/vendas/marketing`.
- **DoD:** `canCreate` só oculta "Nova tarefa" (criação liberada a autenticados).
- **Deps:** vnd-007, vnd-009, vnd-014 · **Prioridade:** Média

### vnd-026 · Solicitações de edição (tela 11)
- **Arquivo:** `src/routes/(app)/vendas/solicitacoes/+page.svelte` (+ `+page.ts`) — create 🟡
- **Conteúdo:** URL `?tab=` (pendentes/aprovadas/rejeitadas); `PageHeader` + explicação do fluxo; KPIs servidos (Pendentes, Aprovadas/mês, Rejeitadas/mês, Revisar hoje); tabs com counts; cards (`bg-elevated/30`): solicitante Avatar+tempo, tipo + badge alvo CLI/OC/EN + status, resumo `atual → proposto` mono, ações "Aprovar" + "Rejeitar" (→ `modal-decisao-solicitacao` local: resumo + radio + Motivo\* ao rejeitar → `decidirSolicitacao` 🟡 + refetch); aprovadas/rejeitadas read-only (autor/data+motivo). Guarda `canDecide` em vnd-014. Endpoints 🟡 (D-4). `data-testid="sol-decidir/sol-aprovar/sol-rejeitar"`.
- **Saída:** rota `/vendas/solicitacoes`.
- **DoD:** motivo obrigatório ao rejeitar (inline); decisão reflete via refetch.
- **Deps:** vnd-008, vnd-009, vnd-014 · **Prioridade:** Alta

---

## B6 — Espalhamento

### vnd-027 · submenu no menu.ts ⚠️
- **Arquivo:** `src/lib/config/menu.ts` — modify ⚠️
- **Conteúdo:** no item `Vendas & CRM` (já existe, linha 48), `children: [{label:'Clientes',path:'/vendas/clientes'},{label:'Orçamentos',path:'/vendas/orcamentos'},{label:'Encomendas',path:'/vendas/encomendas'},{label:'Marketplace',path:'/vendas/marketplace'},{label:'Marketing',path:'/vendas/marketing'},{label:'Solicitações de edição',path:'/vendas/solicitacoes'}]`. Sem alterar `module`/`icon`; sem `canSee` por item (Recrutando barrado na guarda; Solicitações guardada por rota).
- **Saída:** submenu colapsável no Sidebar (renderização já existente).
- **DoD:** ⚠️ **sequencial** (livre agora); navegação mantém submenu aberto + ativo via `$page.url.pathname`.
- **Deps:** vnd-014..vnd-026 (todas as rotas) · **Prioridade:** Média

---

## B7 — Testes

### vnd-028 · unit status+format
- **Arquivo:** `frontend/tests/unit/vendas-status-format.test.ts` — create
- **Conteúdo:** mapas cobrem todas as unions de vnd-001 com `{label,color}` esperados (§7.8); `formatMoneyBRL` (`R$ 1.240,00`), `formatDateBR` (`18/09/2026`), prefixos CLI-/OC-/EN-.
- **DoD:** verdes; pure functions (sem render).
- **Deps:** vnd-009, vnd-010 · **Prioridade:** Média

### vnd-029 · unit api
- **Arquivo:** `frontend/tests/unit/vendas-api.test.ts` — create
- **Conteúdo:** mocks de `apiFetch`; casos: listas montam query PT (`status_kanban`, `tags`, `plataforma`) e **omitem** params vazios; `moverKanban` path `{id}/kanban` PUT; `createEncomenda` com `{idOrcamento}`; marketplace envia `valorTaxa` numérico; solicitações monta decisão com motivo; 409 propaga `ApiError`.
- **DoD:** verdes; `vi.mock('$lib/api/client')`.
- **Deps:** vnd-002..vnd-008 · **Prioridade:** Média

### vnd-030 · unit permissions/RBAC
- **Arquivo:** `frontend/tests/unit/vendas-permissions.test.ts` — create
- **Conteúdo:** `canEditVendas`: Admin edita tudo; criador (`createdBy===id`) edita; outro não edita; sem `createdBy` não edita; `canDecideVendas`: Admin + responsável-vendas decidem, criador comum não; `isResponsavelVendas`: role 4 nunca; base `permissions.test.ts` segue verde.
- **DoD:** verdes; RBAC hide (ausência de DOM) coberto em vnd-031.
- **Deps:** vnd-013 · **Prioridade:** Média

### vnd-031 · unit páginas
- **Arquivo:** `frontend/tests/unit/vendas-pages.test.ts` — create
- **Conteúdo:** render com `vi.mock` das APIs: loading skeleton; success; empty sem filtros (CTA criar); empty filtrado (chips+limpar); error (banner+retry refetch); `!canEdit` mostra "Sugerir alteração" e **omite** Editar do DOM; Kanban move chama `moverKanban` e refetch; 409 move → toast; banner de conversão só em Aprovado sem encomenda; guarda `/solicitacoes` redireciona `!canDecide`.
- **DoD:** verdes; `@testing-library/svelte`; `data-testid` do mockup.
- **Deps:** vnd-015..vnd-026 · **Prioridade:** Média

### vnd-032 · e2e (condicional)
- **Arquivo:** `frontend/tests/e2e/vendas.spec.ts` — **create somente se suite e2e existir; senão SKIP documentado**
- **Conteúdo:** happy path: criar cliente → criar orçamento → aprovar e criar encomenda (Fila) → mover Kanban por clique → registrar venda marketplace (líquido = valor − taxa); marketing criar tarefa; solicitação aprovar/rejeitar (motivo); guarda: não-`canDecide` em `/solicitacoes` → redirecionado; Recrutando em `/vendas` → negado. Sem credenciais hardcoded.
- **DoD:** sem Playwright instalado → `SKIP` (documentar no relatório final).
- **Deps:** vnd-027 · **Prioridade:** Baixa

---

## Resumo
- **32 tasks** (30 create · 2 modify ⚠️: `permissions.ts` vnd-013, `menu.ts` vnd-027) · **0 no template**
- **Dependências entre features:** `menu.ts`/`permissions.ts` sequenciais (livres agora; financeiro na fila). `KanbanBoard.svelte` reusado sem edição (dono kit). Tela Kanban de encomendas tem **dono único (Vendas)** — Produção linka, sem duplicar.
- **Endpoints 🔴 (quase tudo — `vendas-service` scaffold, gateway sem rota):** `GET/POST /api/vendas/clientes`, `GET/PUT /{id}`, `GET /{id}/interacoes`, `POST /interacoes`, `GET/POST /tags`, `GET/POST /orcamentos`, `GET/PUT /{id}`, `POST|GET /encomendas`, `GET /{id}`, `PUT /{id}/kanban`, `GET/POST /marketplace`, `GET/POST/PUT /tarefas-marketing` — D-1/D-2/D-3/D-5/D-7/D-8/D-9 no `plan.md` §10.1.
- **Endpoints 🟡 (assumidos, D-4):** `GET/POST /api/vendas/solicitacoes` + decisão — sem lastro em §5.3/docs/04.
- **e2e:** SKIP (sem Playwright) — idem auth/dashboard/producao/configuracoes/estoque/notificacoes/rh.
- **Nota de implementação (23/09, Validator B-1):** `orcamentos/[id]` "Editar" navega para `novo?editar={id}` (prefill + `updateOrcamento`, guarda `canEditVendas`) — extensão não prevista em vnd-020, mantida por ser o caminho sem-rota-extra; sem rota nova criada.

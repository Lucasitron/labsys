# Tasks — Financeiro (frontend, 7 telas)

> Derivado de `requirements.md` (reconciliado no `plan.md` §8) + `plan.md` desta pasta + mockup `docs/mokups/financeiro/` (`full.html` 7 sections, `requisitos.md` v0.1, `instrunctions.md`).
> Cada task = **1 arquivo real**. Execução por blocos (deps abaixo).
> Legenda: 🟡 contrato assumido (sem evidência de backend/gateway) · 🔴 contrato inexistente/divergente (requer decisão D-x) · ⚠️ arquivo compartilhado — **sequencial**.
> Princípio código mínimo: reuso > create (kit 27 + `format.ts` + `vendas-format.ts` + stores; **zero UI novo, zero toque em `permissions.ts`**); nada especulativo; rotas canônicas `/financeiro/**`; módulo **inteiramente Admin-only**.
> Critério global: pt-BR; Svelte 5 runes; TS estrito (sem `any`); `data-testid` kebab-case do mockup; URL fonte de verdade; RBAC **HIDE total**; debounce 300ms + `AbortController`; `Intl` pt-BR; sem `{@html}`; sem `console.log`.

---

## B1 — Fundação (types + API + utils + guarda)

### fin-001 · types do módulo
- **Arquivo:** `src/lib/types/financeiro.ts` — create
- **Conteúdo:** `TipoLancamento='Entrada'|'Saída'`; `LancamentoStatus='Pendente'|'Pago'|'Atrasado'|'Cancelado'`; `FechamentoStatus='Aberta'|'Concluída'|'Cancelada'`; `CompraStatus='Registrada'|'Visualizada'|'Concluída'`; `TipoDoacao='Doação'|'Projeto'`; `CusteioTab='fechamentos'|'custos'|'valores'|'compras'`; `RelatorioId='fluxo'|'dre'|'lucratividade'|'inadimplencia'|'doacoes'|'custo-maquina'`. Modelos: `CategoriaFinanceira{id,nome,tipo:'Receita'|'Despesa',descricao?}`, `Lancamento` (+`codigo,categoria,tipo,valor,dataVencimento,dataPagamento?,status,idReferenciaExterna?,observacao?`), `LancamentosResult{lancamentos,pagination,counts,resumo}`, `TotaisContas`, `DoacaoRecurso`, `DoacoesResult{registros,resumo,cobertura}`, `ValorHoraNivel{nivelAcesso:0|1|2|3,valorHora,dataVigencia}`, `ParametroOverhead`, `FechamentoEncomenda`, `CustoEncomenda`, `SolicitacaoCompra`, `ResumoFinanceiro` (KPIs tela 1), 6 shapes de relatório (`FluxoCaixa{DRE/Lucratividade/Inadimplencia/DoacoesDespesas/CustoMaquina}` — só tipam o servido, nunca somam), `Paged<T>` genérico + params de lista.
- **Saída:** tipos que alimentam as 6 APIs + 7 telas + testes.
- **DoD:** sem `any`; `import type` nos consumidores; compila (`svelte-check`).
- **Deps:** — · **Prioridade:** Alta

### fin-002 · API lançamentos
- **Arquivo:** `src/lib/api/financeiro/lancamentos.ts` — create 🔴/🟡
- **Conteúdo:** helper `bearer()` local + `apiFetch` de `client.ts`; `listLancamentos({search,status,tipo,categoria,periodo,ordenar,page,pageSize}) → GET /api/financeiro/lancamentos` 🔴 (`{lancamentos,pagination,counts,resumo}` servidos; filtros combinados 🟡 D-4) · `getLancamento(id) → GET …/{id}` 🟡 · `createLancamento(payload) → POST /api/financeiro/lancamentos` 🔴 (status inicial `Pendente`) · `registrarPagamento(id,{dataPagamento,observacao}) → PUT …/{id}/pagamento` 🔴 (status → `Pago`). Query só com params não-nulos.
- **Saída:** camada das telas 1–4 (resumo, lançamentos, contas-pagar, contas-receber — derivadas).
- **DoD:** nenhum path fora de `/api/financeiro/**`; erros como `ApiError`; contrato 🔴/🟡 reportado no relatório do bloco.
- **Deps:** fin-001 · **Prioridade:** Alta

### fin-003 · API categorias
- **Arquivo:** `src/lib/api/financeiro/categorias.ts` — create 🔴
- **Conteúdo:** `listCategorias() → GET /api/financeiro/categorias` 🔴 · `createCategoria({nome,tipo:'Receita'|'Despesa',descricao?}) → POST /api/financeiro/categorias` 🔴. Via `apiFetch`.
- **Saída:** camada do `modal-categoria` + selects de categoria (tela 2).
- **DoD:** DTOs `CategoriaFinanceira` de fin-001.
- **Deps:** fin-001 · **Prioridade:** Alta

### fin-004 · API doações & recursos
- **Arquivo:** `src/lib/api/financeiro/doacoes.ts` — create 🔴
- **Conteúdo:** `listDoacoes({search,tipo,periodo,page}) → GET /api/financeiro/doacoes-recursos` 🔴 (`{registros,resumo,cobertura}` servidos) · `createDoacao({tipo:'Doação'|'Projeto',origem,valor,dataRecebimento,idProjetoAssociado?}) → POST /api/financeiro/doacoes-recursos` 🔴 (gera entrada separada das receitas operacionais). Via `apiFetch`.
- **Saída:** camada da tela 5.
- **DoD:** DTOs `DoacaoRecurso` de fin-001; sem soma de cobertura no client (R-7).
- **Deps:** fin-001 · **Prioridade:** Alta

### fin-005 · API custeio (valores-hora, overhead, fechamentos, custos)
- **Arquivo:** `src/lib/api/financeiro/custeio.ts` — create 🔴
- **Conteúdo:** `listFechamentos() → GET /api/financeiro/fechamento-encomenda` 🔴 · `createFechamento({idEncomenda,horasEstimadas,valorFechado,dataFechamento}) → POST /api/financeiro/fechamento-encomenda` 🔴 (congela — G-9) · `getCusto(idEncomenda) → GET /api/financeiro/custos-encomenda/{id}` 🔴 (congelado; nunca recalculado) · `listValoresHora() → GET /api/financeiro/valores-hora` 🔴 · `definirValorHora({nivelAcesso,valorHora,dataVigencia}) → POST /api/financeiro/valores-hora` 🔴 · `definirOverhead({valorTaxaHora,dataVigencia}) → POST /api/financeiro/parametros-overhead` 🔴. Via `apiFetch`. **Nenhuma função de cálculo de custo/margem no client.**
- **Saída:** camada das abas Fechamentos/Custos/Valores da tela 6.
- **DoD:** export sem `calcular|soma|margem(`; D-5 registrado.
- **Deps:** fin-001 · **Prioridade:** Alta

### fin-006 · API solicitações de compra (informativo)
- **Arquivo:** `src/lib/api/financeiro/solicitacoes-compra.ts` — create 🔴
- **Conteúdo:** `listSolicitacoesCompra() → GET /api/financeiro/solicitacoes-compra` 🔴 · `createSolicitacaoCompra(payload) → POST /api/financeiro/solicitacoes-compra` 🔴 · `concluirCompra(id,{dataConclusao,valorReal,notaFiscal?}) → PUT …/{id}/concluir` 🔴 (`→ {status:'Concluída',lancamentoGerado}`). Via `apiFetch`. Sem bloqueio (informativo — G-9).
- **Saída:** camada da aba Compras da tela 6.
- **DoD:** DTOs `SolicitacaoCompra` de fin-001.
- **Deps:** fin-001 · **Prioridade:** Alta

### fin-007 · API relatórios (6 endpoints)
- **Arquivo:** `src/lib/api/financeiro/relatorios.ts` — create 🔴/🟡
- **Conteúdo:** `getFluxoCaixa({periodo})` · `getDre({periodo})` · `getLucratividade({periodo})` · `getInadimplencia({periodo})` · `getDoacoesDespesas({periodo})` · `getCustoMaquina({periodo})` → `GET /api/financeiro/relatorios/{fluxo-caixa|dre|lucratividade|inadimplencia|doacoes-despesas|custo-maquina}` 🔴 (`?periodo=` 🟡 D-4). Via `apiFetch`. **Nenhuma agregação no client — só renderiza o servido.**
- **Saída:** camada da tela 7.
- **DoD:** export sem `totalizar|agregar|somar`; D-4 registrado.
- **Deps:** fin-001 · **Prioridade:** Alta

### fin-008 · util de status canônicos
- **Arquivo:** `src/lib/utils/financeiro-status.ts` — create
- **Conteúdo:** `lancamentoStatusMeta` (Pendente/warn, Pago/success, Atrasado/danger, Cancelado/muted) · `tipoMeta` (Entrada/success, Saída/danger) · `fechamentoStatusMeta` (Aberta/brand, Concluída/success, Cancelada/muted) · `compraStatusMeta` (Registrada/brand, Visualizada/warn, Concluída/success) · `doacaoTipoMeta` (Doação/brand, Projeto/success). Formato `{label,color}` (padrão `stock-status.ts`). **Sem tradução de strings — só cor.**
- **Saída:** cores/labels das 7 telas.
- **DoD:** cobertura total das unions de fin-001; teste em fin-019.
- **Deps:** fin-001 · **Prioridade:** Alta

### fin-009 · util de formatação financeiro
- **Arquivo:** `src/lib/utils/financeiro-format.ts` — create
- **Conteúdo:** `formatSignedBRL(valor, tipo)` (sinal `+`/`−` + `formatMoneyBRL` **importado** de `vendas-format`; nunca duplicar `Intl` base) · `codigoLancamento` (prefixo `LAN-`, `font-mono` na tela). **REUSAR** `formatMoneyBRL`/`formatDateBR` de `vendas-format.ts` e `initials/relativeTime/formatNumber` de `format.ts` (não duplicar).
- **Saída:** valores com sinal + códigos das 7 telas.
- **DoD:** puro (sem dependência de store); teste em fin-019.
- **Deps:** — · **Prioridade:** Alta

### fin-010 · guarda do módulo (Admin-only total)
- **Arquivo:** `src/routes/(app)/financeiro/+layout.ts` — create
- **Conteúdo:** padrão `configuracoes/+layout.ts` (não `vendas/pessoas`): `if (!isAdmin(user)) throw redirect(303, '/dashboard')` para **todo** `/financeiro/**` (sem exceção por sub-rota). `ssr=false/prerender=false`. Sem `canView` (regra `[0]` já garante hide do item).
- **Saída:** proteção das 7 rotas (hide total, não disable).
- **DoD:** não-Admin em qualquer `/financeiro/*` redirecionado (nunca 403 em tela); sem duplicar `fetchMe`.
- **Deps:** — · **Prioridade:** Alta

---

## B2 — Lançamentos (lista + bulk) + Resumo

### fin-011 · Lançamentos — lista (tela 2)
- **Arquivo:** `src/routes/(app)/financeiro/lancamentos/+page.svelte` (+ `+page.ts` no mesmo task) — create 🔴/🟡
- **Conteúdo:** URL `?search=&status=&tipo=&categoria=&periodo=&ordenar=&page=&pageSize=` (fonte de verdade); `PageHeader` + "Nova categoria" (ghost → `modal-categoria` local) + "Novo lançamento" (primary → `modal-novo-lancamento` local: Categoria\*/Tipo radio/Valor\* `MoneyInput`/Vencimento\*/Pagamento opcional/Referência/Observação → `createLancamento` 🔴); KPIs servidos (`resumo`: Entradas/Saídas/Recebido×pendente/Fluxo líquido); filtros (SearchInput debounce 300ms + 5 triggers Status/Tipo/Categoria/Período/Ordenar + `Chip`s `fin-chips` + "Limpar filtros"); tabela (checkbox select-all/indeterminate · Vencimento danger se Atrasado · Descrição + `REF • EN-…` mono · Categoria · Tipo badge · Valor signed mono · Status badge · `⋮`) + linha → `modal-lancamento-detalhe` local (ficha + "Registrar recebimento/pagamento" → `modal-pagamento` local → `registrarPagamento` 🔴); `BulkActionsBar` (`bulk-bar`, `bulk-count`: Registrar pagamento/recebimento · Exportar (omitido sem endpoint — R-9) · Limpar); rodapé "Mostrando X de Y" + `Pagination`. Estados: TableSkeleton/Empty/Empty-filtrado/ErrorBanner+retry. Atrasado: fundo `bg-danger/[0.03]`. Checkbox com `stopPropagation`.
- **Saída:** rota `/financeiro/lancamentos`.
- **DoD:** `data-testid="lan-row/lan-checkbox/select-all-rows/bulk-bar/lan-novo/fin-chips"`; seleção não persiste entre páginas.
- **Deps:** fin-002, fin-003, fin-008, fin-009, fin-010 · **Prioridade:** Alta

### fin-012 · Financeiro — resumo (tela 1)
- **Arquivo:** `src/routes/(app)/financeiro/+page.svelte` (+ `+page.ts`) — create 🔴
- **Conteúdo:** `PageHeader` + "Exportar resumo" (omitido/desabilitado sem endpoint — R-9) + "Novo lançamento" (reuso do modal de fin-011, composição local); KPIs servidos (Saldo em caixa · A receber com "sendo R$ X inadimplentes" danger · A pagar com "R$ X vencem em 7 dias" warn · Custo/margem média); grid 3 cols: Fluxo de caixa (barras CSS mensais Recebido success/Previsto brand + "Ver relatórios" → `/financeiro/relatorios`) + Alertas (cards por severidade com atalhos `data-goto`); grid 2 cols: Últimos lançamentos (lista → `/financeiro/lancamentos`) + Entradas/Saídas + Categorias com maiores saídas (barras progress). Erro parcial por bloco (estado #6). Endpoints: `listLancamentos({limit})` + agregados de resumo 🔴 (D-2).
- **Saída:** rota `/financeiro`.
- **DoD:** `data-testid="fin-saldo/fin-alerta"`; zero soma client (R-7).
- **Deps:** fin-002, fin-008, fin-009, fin-010 · **Prioridade:** Alta

---

## B3 — Contas (vistas derivadas, sem CRUD próprio)

### fin-013 · Contas a pagar (tela 3)
- **Arquivo:** `src/routes/(app)/financeiro/contas-pagar/+page.svelte` (+ `+page.ts`) — create 🔴
- **Conteúdo:** **Vista derivada** de `listLancamentos({tipo:'Saída',status:'Pendente,Atrasado'})` (R-8); URL `?search=&status=&vencimento=&page=`; `PageHeader` + "Nova conta a pagar" (→ `modal-novo-lancamento` local pré-selecionado Saída); KPIs servidos (`totais`: A pagar no mês · Vencidas, 0 → "Em dia ✓" · Pagas no mês · Próximos 30 dias); filtros busca + Status + Vencimento + chips + limpar; tabela (Conta/fornecedor + `REF • …` · Categoria · Vencimento · Valor mono · Status badge · ações registrar pagamento → `modal-pagamento` local + `⋮`); rodapé "X contas a pagar · total R$ Y" (servido).
- **Saída:** rota `/financeiro/contas-pagar`.
- **DoD:** `data-testid="cp-row/cp-pagar"`; nenhum endpoint `/contas` chamado (C-3).
- **Deps:** fin-002, fin-008, fin-009, fin-010 · **Prioridade:** Alta

### fin-014 · Contas a receber (tela 4)
- **Arquivo:** `src/routes/(app)/financeiro/contas-receber/+page.svelte` (+ `+page.ts`) — create 🔴
- **Conteúdo:** **Vista derivada** de `listLancamentos({tipo:'Entrada',status:'Pendente,Atrasado'})` (R-8); URL `?search=&status=&origem=&page=`; `PageHeader` + "Novo recebimento" (→ `modal-novo-lancamento` local pré-selecionado Entrada); KPIs servidos (A receber no mês · Inadimplência com "{n} recebíveis vencidos" · Recebido no mês · Próximos 30 dias); filtros busca + Status + Origem (Encomendas/Marketplace/Outros) + chips + limpar; tabela (Cliente/origem `Avatar` + nota de atraso danger/vencimento muted · Referência mono `EN-…`/`OC-…` · Origem badge · Vencimento danger se atrasado · Valor · Status · ações registrar recebimento → `modal-pagamento` local + `⋮`); atrasadas com `bg-danger/[0.03]`; rodapé "X recebíveis · pendente + atrasado = R$ Y" (servido).
- **Saída:** rota `/financeiro/contas-receber`.
- **DoD:** `data-testid="cr-row/cr-receber"`; nenhum endpoint `/contas` chamado (C-3).
- **Deps:** fin-002, fin-008, fin-009, fin-010 · **Prioridade:** Alta

---

## B4 — Doações & recursos

### fin-015 · Doações & recursos (tela 5)
- **Arquivo:** `src/routes/(app)/financeiro/doacoes/+page.svelte` (+ `+page.ts`) — create 🔴
- **Conteúdo:** URL `?search=&tipo=&periodo=&page=`; `PageHeader` + "Nova doação/recurso" (→ `modal-nova-doacao` local: Tipo\* radio Doação/Projeto · Origem\* · Valor\* `MoneyInput` · Recebimento\* date · Projeto universitário select `PJ-…`/— + nota "gera entrada separada" → `createDoacao` 🔴); KPIs servidos (Recebido no ano · Doações com nº · Recursos de projetos com nº · Doação média); card Doações vs Despesas (barras mensais brand + cobertura servida); filtros busca + Tipo + Período + chips + limpar; tabela (Tipo badge Doação brand/Projeto success · Origem · Projeto associado mono · Recebimento · Valor · `⋮`); rodapé "{n} registros no período". Endpoints 🔴 (fin-004).
- **Saída:** rota `/financeiro/doacoes`.
- **DoD:** `data-testid="doa-nova/doa-row"`; cobertura nunca calculada no client.
- **Deps:** fin-004, fin-008, fin-009, fin-010 · **Prioridade:** Média

---

## B5 — Custeio (4 abas)

### fin-016 · Custeio (tela 6)
- **Arquivo:** `src/routes/(app)/financeiro/custeio/+page.svelte` (+ `+page.ts`) — create 🔴
- **Conteúdo:** URL `?tab=fechamentos|custos|valores|compras`; `PageHeader` + "Valores/hora & overhead" (→ `modal-valor-hora` local) + "Novo fechamento" (→ `modal-novo-fechamento` local: aviso de congelamento · Encomenda\* select só Aberta · Valor fechado\* `MoneyInput` · Horas estimadas\* · Data default hoje → `createFechamento` 🔴); KPIs servidos (Custo médio/encomenda · Margem média · Overhead acumulado · Horas validadas + "coerência de horas ok ✓"); `Tabs` (`custeio-tabs`): Fechamentos (tabela Encomenda mono/Cliente/Valor/Horas/Data/Status + nota de congelamento; linha → `modal-custo-detalhe` local via `getCusto` 🔴) · Custos (cards Materiais/Mão de obra/Overhead + barra + Custo total + Margem %, danger se negativa) · Valores/hora & Overhead (tabela por nível 0–3 + Editar → `modal-valor-hora` → `definirValorHora` 🔴; card Overhead R$/h + fórmula + "Editar taxa" → `modal-overhead` → `definirOverhead` 🔴; nota de auditoria 05 §8) · Compras informativo (aviso "não há bloqueio" brand + tabela Item/Qtd/Valor estimado/Solicitação/Status/"Concluir" → `modal-concluir-compra` local → `concluirCompra` 🔴; concluídas mostram "saída lançada"). Erro parcial por aba (estado #6).
- **Saída:** rota `/financeiro/custeio`.
- **DoD:** `data-testid="cus-tab-custos/cus-concluir"`; notas de congelamento/auditoria/compras visíveis; zero cálculo de custo no client.
- **Deps:** fin-005, fin-006, fin-008, fin-009, fin-010 · **Prioridade:** Alta

---

## B6 — Relatórios (6 panes)

### fin-017 · Relatórios (tela 7)
- **Arquivo:** `src/routes/(app)/financeiro/relatorios/+page.svelte` (+ `+page.ts`) — create 🔴/🟡
- **Conteúdo:** URL `?relatorio=fluxo|dre|lucratividade|inadimplencia|doacoes|custo-maquina&periodo=`; `PageHeader` + `Select` período ("Setembro 2026" default) + "Exportar" (omitido/desabilitado sem endpoint — R-9/D-6); seletor 6 cards `.rel-btn` (Fluxo brand · DRE warn · Lucratividade success · Inadimplência danger · Doações brandhi · Custo-máquina muted; ativo `border-brand` + tint; clique → pane + `?relatorio=`); pane resultado por relatório (fluxo: semana Entradas/Saídas/Líquido + total · dre: receita/custos/despesas/doações/resultado · lucratividade: venda/custo/margem R$/margem % danger se negativa · inadimplência: cliente/referência/vencimento/dias danger/valor · doações: mês Doações/Despesas/Saldo + acumulado · custo-máquina: máquina/horas/custo/% + total). **Nunca recalcular totais — só renderizar o servido** (1 endpoint por relatório, fin-007; `?periodo=` 🟡 D-4).
- **Saída:** rota `/financeiro/relatorios`.
- **DoD:** `data-testid="rel-btn-fluxo/rel-table"`; pane reflete a URL e vice-versa.
- **Deps:** fin-007, fin-008, fin-009, fin-010 · **Prioridade:** Alta

---

## B7 — Espalhamento

### fin-018 · submenu no menu.ts ⚠️
- **Arquivo:** `src/lib/config/menu.ts` — modify ⚠️
- **Conteúdo:** no item `Financeiro` (já existe, linha 80), `children: [{label:'Resumo',path:'/financeiro'},{label:'Lançamentos',path:'/financeiro/lancamentos'},{label:'Contas a pagar',path:'/financeiro/contas-pagar'},{label:'Contas a receber',path:'/financeiro/contas-receber'},{label:'Doações & recursos',path:'/financeiro/doacoes'},{label:'Custeio',path:'/financeiro/custeio'},{label:'Relatórios',path:'/financeiro/relatorios'}]`. Sem alterar `module`/`icon`; sem `canSee` por item (pai hide-total por `canView` com regra `[0]`).
- **Saída:** submenu colapsável no Sidebar (renderização já existente).
- **DoD:** ⚠️ **sequencial — último a tocar** (todas as features concluídas tocam este arquivo); navegação mantém submenu aberto + ativo via `$page.url.pathname`.
- **Deps:** fin-010..fin-017 (todas as rotas) · **Prioridade:** Média

---

## B8 — Testes

### fin-019 · unit status+format
- **Arquivo:** `frontend/tests/unit/financeiro-status-format.test.ts` — create
- **Conteúdo:** mapas cobrem todas as unions de fin-001 com `{label,color}` esperados (§7.9: Pendente/warn, Pago/success, Atrasado/danger, Cancelado/muted, Entrada/success, Saída/danger, Aberta/Registrada/brand, Concluída/success, Visualizada/warn, Doação/brand, Projeto/success); `formatSignedBRL` (`+R$ 1.240,00` Entrada / `−R$ 380,00` Saída); prefixo `LAN-`.
- **DoD:** verdes; pure functions (sem render).
- **Deps:** fin-008, fin-009 · **Prioridade:** Média

### fin-020 · unit api
- **Arquivo:** `frontend/tests/unit/financeiro-api.test.ts` — create
- **Conteúdo:** mocks de `apiFetch`; casos: listas montam query PT (`status`, `tipo`, `categoria`, `periodo`, `tab`→path) e **omitem** params vazios; `registrarPagamento` path `{id}/pagamento` PUT; `createFechamento` congela (payload sem campo calculado); `concluirCompra` path `{id}/concluir`; relatórios 1 endpoint por `RelatorioId` + `?periodo=`; 409/422 propagam `ApiError`.
- **DoD:** verdes; `vi.mock('$lib/api/client')`.
- **Deps:** fin-002..fin-007 · **Prioridade:** Média

### fin-021 · unit guard Admin-only
- **Arquivo:** `frontend/tests/unit/financeiro-guard.test.ts` — create
- **Conteúdo:** `load` de fin-010: não-Admin (roles 1–4) em qualquer `/financeiro/*` → `redirect 303 /dashboard`; Admin (role 0) passa; `VIEW_RULES.financeiro`/`EDIT_RULES.financeiro` seguem `[0]`; item `Financeiro` invisível a não-Admin via `canView` (base `permissions.test.ts` segue verde — **sem helpers novos**).
- **DoD:** verdes; hide-total coberto (ausência de DOM) em fin-022.
- **Deps:** fin-010 · **Prioridade:** Média

### fin-022 · unit páginas
- **Arquivo:** `frontend/tests/unit/financeiro-pages.test.ts` — create
- **Conteúdo:** render com `vi.mock` das APIs: loading skeleton; success; empty sem filtros (CTA criar); empty filtrado (chips+limpar); error (banner+retry refetch); bulk bar só com ≥1 seleção (`registrarPagamento` + refetch; checkbox `stopPropagation` não abre detalhe); contas derivadas filtram `tipo`+`status` sem endpoint próprio; custeio `?tab=` troca pane + notas visíveis; relatórios `?relatorio=` troca pane sem recálculo; guarda redireciona não-Admin.
- **DoD:** verdes; `@testing-library/svelte`; `data-testid` do mockup.
- **Deps:** fin-011..fin-017 · **Prioridade:** Média

### fin-023 · e2e (condicional)
- **Arquivo:** `frontend/tests/e2e/financeiro.spec.ts` — **create somente se suite e2e existir; senão SKIP documentado**
- **Conteúdo:** happy path Admin: criar lançamento (Pendente) → registrar pagamento (→ Pago) → contas-pagar/receber refletem derivação → criar doação → novo fechamento (nota de congelamento) → concluir compra (gera saída) → trocar panes de relatório (`?relatorio=`+`?periodo=`); guarda: não-Admin em `/financeiro` → `/dashboard`. Sem credenciais hardcoded.
- **DoD:** sem Playwright instalado → `SKIP` (documentar no relatório final).
- **Deps:** fin-018 · **Prioridade:** Baixa

---

## Resumo
- **23 tasks** (22 create · 1 modify ⚠️: `menu.ts` fin-018) · **0 no template**
- **Dependências entre features:** `menu.ts` sequencial, **último a tocar** (vendas/rh/produção/estoque concluídos; nenhuma feature ativa o toca). `permissions.ts` **intacto** (sem helpers novos — Admin-only total). `vendas-format.ts` e UI kit reusados sem edição.
- **Endpoints 🔴 (tudo — `financeiro-service` scaffold, gateway sem rota):** `GET/POST /api/financeiro/lancamentos`, `PUT …/{id}/pagamento`, `GET/POST /categorias`, `GET/POST /doacoes-recursos`, `GET/POST /valores-hora`, `POST /parametros-overhead`, `POST+GET /fechamento-encomenda[/{id}]`, `GET /custos-encomenda/{id}`, `POST+PUT /solicitacoes-compra[/{id}/concluir]`, 6× `GET /relatorios/*` — D-1/D-2/D-3/D-5 no `plan.md` §10.1.
- **Endpoints 🟡 (assumidos, D-4):** filtros combinados (`tipo`+`status`+`categoria`) e `?periodo=` nos relatórios — sem lastro no `05` §6.
- **e2e:** SKIP (sem Playwright) — idem auth/dashboard/producao/configuracoes/estoque/notificacoes/rh/vendas.

# Plan — Financeiro (Frontend)

> Feature: `financeiro` (frontend) · Branch: `dev/frontend` · Gerado por: Planner stand-in (despacho `.opencode/state/planner-dispatch-financeiro.md`, 23/09/2026)
> Autoridade (ordem): (1) mockup `docs/mokups/financeiro/` (`full.html` 7 sections · `requisitos.md` v0.1 · `instrunctions.md` 8 prompts); (2) `docs/contract.gap.md` §5.4 (PT, Admin-only total, vistas derivadas, status PT servidos, congelamento de fechamento, filtros `?periodo=` 🟡); (3) `.opencode/specs/frontend/features/financeiro/requirements.md` (**desatualizado — reconciliado no §8, sem edição**).
> Escopo: SÓ frontend. NÃO editar `docs/**` nem `requirements.md`; NÃO implementar; NÃO commitar. Nenhum endpoint inventado (só os do mockup §7.2/§5.4; divergências → D-x).
> Princípio **código mínimo**: reuso > create; nada especulativo; cada arquivo resolve caso real do mockup.

---

## 1. Contexto e decisão de rotas

O canônico são as **7 telas do mockup** (`requisitos.md` §2), prefixo `/(app)/financeiro/**`, sem second sidebar (submenu colapsável na sidebar principal, `requisitos.md` §4.4). Divergências contra o `requirements.md` atual (5 rotas, `contas` única, sem resumo, com CRUD de contas e PDF) estão reconciliadas no **§8** (só documentadas aqui).

| # | Tela | Rota canônica | Mockup section |
|---|---|---|---|
| 1 | Financeiro (resumo) | `/financeiro` | `financeiro` |
| 2 | Lançamentos | `/financeiro/lancamentos` (`?search=&status=&tipo=&categoria=&periodo=&ordenar=&page=&pageSize=`) | `lancamentos` |
| 3 | Contas a pagar | `/financeiro/contas-pagar` (`?search=&status=&vencimento=&page=`) | `contas-pagar` |
| 4 | Contas a receber | `/financeiro/contas-receber` (`?search=&status=&origem=&page=`) | `contas-receber` |
| 5 | Doações & recursos | `/financeiro/doacoes` (`?search=&tipo=&periodo=&page=`) | `doacoes` |
| 6 | Custeio | `/financeiro/custeio` (`?tab=fechamentos\|custos\|valores\|compras`) | `custeio` |
| 7 | Relatórios | `/financeiro/relatorios` (`?relatorio=&periodo=`) | `relatorios` |

> Sem rota `/financeiro/contas` única e sem `[id]` dedicado — detalhe do lançamento vive em **modal** (`modal-lancamento-detalhe`, aberto por linha/`⋮`; checkbox não navega). Contas a pagar/receber são **vistas derivadas** de `GET /lancamentos` (decisão §5.4). Custeio e relatórios usam **abas/panes com estado na URL** (`?tab=`, `?relatorio=`).

---

## 2. Estado real arquivo-a-arquivo (frontend)

### 2.1 Rotas (`src/routes/(app)/`)

| Arquivo | Estado | Nota |
|---|---|---|
| `(app)/+layout.ts` | ✅ OK | Guarda auth global. Não tocar. |
| `(app)/financeiro/**` | 🔴 **G-1** | **Não existe** (só `configuracoes dashboard estoque notificacoes pessoas producao vendas`). As 7 telas + 1 layout serão create. |
| `(app)/configuracoes/+layout.ts` | ✅ referência | Guarda **Admin-only total** (`!isAdmin → redirect 303`, sem `canView`) — replicar em `financeiro/+layout.ts`. |
| `(app)/notificacoes/+layout.ts` | ✅ referência | Guarda Admin por pathname (`isAdmin`, redirect 303) — padrão alternativo; financeiro usa o padrão `configuracoes` (módulo inteiro, sem exceção). |

### 2.2 API (`src/lib/api/`)

| Arquivo | Estado | Nota |
|---|---|---|
| `client.ts` | ✅ OK | `apiFetch` (timeout 15s, Bearer, 401→logout+goto sanitizado, `ApiError/NetworkError`). Base de toda API nova. Não tocar. |
| `api/financeiro/*` | 🔴 **G-2** | **Inexistente** (`ls` confirma). 6 módulos PT sobre `apiFetch` serão create (§4). Gateway não expõe `/api/financeiro/**`; `financeiro-service` é scaffold vazio (Dívida 1) — tudo 🔴/🟡 (§10). |
| `vendas/*` (7 módulos) / `rh/*` / `stock/*` / `producao/*` / `configuracoes/*` | ✅ OK | Padrão `bearer()` local + `apiFetch`. Não tocar. |

### 2.3 Tipos e utils

| Arquivo | Estado | Nota |
|---|---|---|
| `types/auth.ts` | ✅ OK | `Role 0–4`, `Module` inclui `financeiro`. Base da guarda (`isAdmin`). |
| `types/financeiro.ts` | 🔴 **G-3** | Inexistente. Criar DTOs PT (Lancamento, Categoria, DoacaoRecurso, ValorHora, Overhead, Fechamento, CustoEncomenda, SolicitacaoCompra, 6 shapes de relatório + paginados). |
| `utils/permissions.ts` | ✅ **sem mudança** | `VIEW_RULES.financeiro=[0]` e `EDIT_RULES.financeiro=[0]` já corretos; `isAdmin` existe (linha 107). **Nenhum helper novo** — módulo inteiro Admin-only não precisa de `canEdit/canDecide` granular. Confirmado: **zero tasks neste arquivo** (diferença vs vendas/rh). |
| `utils/format.ts` | ✅ OK | `initials`, `relativeTime`, `formatNumber` — **REUSAR** (R-1). Não cobre moeda BRL nem datas `dd/mm/yyyy`. |
| `utils/vendas-format.ts` | ✅ OK | `formatMoneyBRL` (`Intl` BRL), `formatDateBR` — **REUSAR** (R-1). `MoneyInput.svelte` do kit já importa daqui. |
| `utils/financeiro-status.ts` | 🔴 **G-4a** | Inexistente. Criar **só** mapas `{label,color}` canônicos (mockup §7.9). |
| `utils/financeiro-format.ts` | 🔴 **G-4b** | Inexistente. Criar **só** `formatSignedBRL` (sinal +/− colorido por tipo) + prefixo `LAN-`, **importando** `formatMoneyBRL/formatDateBR` de `vendas-format` (sem duplicar). |

### 2.4 Componentes (`src/lib/components/ui/` — 27 arquivos)

✅ Prontos e **obrigatoriamente reusados**: `SearchInput, Dropdown, Checkbox (indeterminate), Select, StatusBadge, TypeBadge, PageHeader, Chip, ConfirmDialog, Tabs, Modal, BulkActionsBar, EmptyState, TableSkeleton (+Skeleton), Pagination, ErrorBanner, Avatar, RowActions, MoneyInput, Timeline`.
**Zero componentes UI novos** (diferença vs vendas `Timeline/MoneyInput` e rh `TypeBadge/DonutChart/KanbanBoard` — todos já existem no kit). Gráficos = barras CSS com divs (mockup §4.5, sem lib). Modais (`modal-novo-lancamento`, `modal-categoria`, `modal-pagamento`, `modal-lancamento-detalhe`, `modal-nova-doacao`, `modal-novo-fechamento`, `modal-custo-detalhe`, `modal-valor-hora`, `modal-overhead`, `modal-concluir-compra`) = **composição local** de cada tela com `Modal` do kit; **não** são arquivos próprios (R-4).

### 2.5 Stores

- `stores/auth.ts`, `stores/toast.ts` ✅ reusar. **Sem** store novo de financeiro (dado via fetch + refetch; precedente notificações R-1).
- `components/layout/Sidebar.svelte` ✅ renderiza `children` filtrados por `canSee` — nenhum toque além do `menu.ts`. Item `Financeiro` já some para não-Admin via `canView` (`VIEW_RULES.financeiro=[0]`).

### 2.6 Config (`src/lib/config/menu.ts`)

| Estado | Nota |
|---|---|
| 🟡 **G-6** | Item `Financeiro → /financeiro (module financeiro)` existe (linha 80) mas **sem `children`**. Adicionar submenu: Resumo · Lançamentos · Contas a pagar · Contas a receber · Doações & recursos · Custeio · Relatórios. Crumb atual de `/financeiro/x` cai em `beautify` genérico — submenu corrige. Sem `canSee` por item (pai já hide-total por `canView`). |

### 2.7 Testes existentes

- `tests/unit/permissions.test.ts` — RBAC base; cobre `isAdmin` + `VIEW_RULES.financeiro` (nada a estender — sem helpers novos).
- `tests/unit/format.test.ts`, `client.test.ts`, `errors.test.ts` — cobrem o que será reusado.
- Zero testes de financeiro → **G-11**. Infra: vitest+jsdom+testing-library (sem Playwright → e2e **skip**, padrão das features).

---

## 3. Gaps (G-1 … G-11)

| Gap | Descrição | Tipo |
|---|---|---|
| **G-1** | 7 rotas `/financeiro/**` + 1 layout (guarda Admin-only total) inexistentes. | Frontend |
| **G-2** | `api/financeiro/*` inexistente (6 módulos PT sobre `apiFetch`); gateway sem `/api/financeiro/**`; `financeiro-service` scaffold vazio — todos os endpoints 🔴/🟡. | Contrato 🔴 |
| **G-3** | `types/financeiro.ts` inexistente (Lancamento/Categoria/Doacao/ValorHora/Overhead/Fechamento/Custo/Compra + 6 relatórios + paginados). | Tipos |
| **G-4** | `utils/financeiro-status.ts` + `financeiro-format.ts` inexistentes (mapas §7.9; sinal monetário/código `LAN-`; moeda/data reusadas de `vendas-format`). | Frontend |
| **G-5** | UI kit: **nada faltante** ✅ (`MoneyInput`, `Timeline`, `TypeBadge`, `Tabs`, `BulkActionsBar` já existem). Não é gap — registrado para travar "zero UI novo" (R-2). | — |
| **G-6** | `menu.ts` sem submenu Financeiro (item existe, linha 80). | Frontend |
| **G-7** | `permissions.ts`: **sem mudança confirmada** ✅ (`VIEW/EDIT_RULES.financeiro=[0]` + `isAdmin` bastam). Não é gap — registrado para travar "zero toque" (R-3). | — |
| **G-8** | Contas a pagar/receber: requirements prevê `GET /contas?status` + `POST` + `PATCH /{id}/pagar`; canônico = **vistas derivadas de `GET /lancamentos`**, pagamento via `PUT /lancamentos/{id}/pagamento` (mockup §7.4, §5.4). | Contrato 🔴 |
| **G-9** | Custeio: fechamento **congela** `horasEstimadas`/`valorFechado`; alteração de encomenda → **nova ordem + novo fechamento** (regra backend); compras **informativas**; valores/hora & overhead com **auditoria** (05 §8). Front só exibe + chama; nunca recalcula. | Contrato 🔴 |
| **G-10** | Relatórios: 6 endpoints `GET /relatorios/*` + `?periodo=` 🟡 assumido (não consta no `05` §6); cada relatório = 1 endpoint; `?relatorio=` só troca o pane. | Contrato 🔴/🟡 |
| **G-11** | Zero testes unit do módulo. | Testes |

---

## 4. Camadas / arquivos a criar ou editar

**Create (22):**
- `src/lib/types/financeiro.ts` (G-3)
- `src/lib/api/financeiro/lancamentos.ts` · `categorias.ts` · `doacoes.ts` · `custeio.ts` (valores-hora + overhead + fechamentos + custos) · `solicitacoes-compra.ts` · `relatorios.ts` (G-2/G-8/G-9/G-10)
- `src/lib/utils/financeiro-status.ts` · `financeiro-format.ts` (G-4)
- `src/routes/(app)/financeiro/+layout.ts` (guarda Admin-only total, padrão `configuracoes`) (G-1)
- 7 telas: `+page` (+`+page.ts` no mesmo task) · `lancamentos/+page` · `contas-pagar/+page` · `contas-receber/+page` · `doacoes/+page` · `custeio/+page` · `relatorios/+page` (G-1/G-8/G-9/G-10; modais = composição local)
- Testes: `tests/unit/financeiro-status-format.test.ts` · `financeiro-api.test.ts` · `financeiro-guard.test.ts` · `financeiro-pages.test.ts` (G-11)

**Modify (1):**
- `src/lib/config/menu.ts` (G-6 — adicionar `children` ao item existente, linha 80)

**Não tocar:** `permissions.ts` (G-7), `docs/**`, `requirements.md`, UI kit (zero arquivos novos).

**Não criar** (enxugamento §7): componentes UI novos, stores financeiro, `api/financeiro/*` além dos 6, arquivos de modal próprios, export PDF, conciliação bancária, second sidebar, rotas `/financeiro/contas` ou `[id]`.

---

## 5. ConflictMap (arquivos compartilhados)

| Arquivo | Dono/Impacto |
|---|---|
| `src/lib/config/menu.ts` | 🔶 Financeiro adiciona `children` ao item `Financeiro` existente. Vendas/rh/produção/estoque concluídos e tocam o mesmo arquivo — **executar sequencial, por último** (só após todas as rotas existirem). ⚠️ **Último a tocar.** |
| `src/lib/utils/permissions.ts` | ✅ **Nenhum toque** — `isAdmin` + `VIEW/EDIT_RULES.financeiro=[0]` já bastam. Sem conflito por construção. |
| `src/lib/utils/vendas-format.ts` | ✅ **Reuse sem edição** (dono: Vendas). Financeiro importa `formatMoneyBRL/formatDateBR`; nada a alterar. |
| `src/lib/components/ui/*` | ✅ **Reuse sem edição** (donos: kit). Nenhum componente novo. |
| `docs/**` + `requirements.md` | ⛔ **Congelados**. Reconciliação só documentada aqui (§8); dívidas ficam no §10, nunca em `docs/contract.gap.md`. |

---

## 6. Decisões de enxugamento (código mínimo)

| # | Decisão | Detalhe |
|---|---|---|
| **R-1** | **REUSADO** `format.ts` (`initials`/`relativeTime`/`formatNumber`), `vendas-format.ts` (`formatMoneyBRL`/`formatDateBR`), `errors.ts`, stores `auth`/`toast`, kit (27 comps) | `financeiro-format.ts` carrega **só** `formatSignedBRL` + prefixo `LAN-`. Sem store financeiro, sem componentes de estado por tela. |
| **R-2** | **Zero componentes UI novos** | `MoneyInput` (novo lançamento/doação/fechamento/valores), `Timeline` (se preciso no detalhe), `Tabs` (custeio), `BulkActionsBar` (lançamentos), `TypeBadge` (origem/doação) já existem. Barras de gráfico = divs CSS (mockup §4.5). |
| **R-3** | **Zero toque em `permissions.ts`** | Módulo inteiramente Admin-only: guarda `isAdmin` no layout + hide total do item (via `canView`, regra `[0]` existente). Sem `canEdit/canDecide` granular — diferença estrutural vs vendas/rh. |
| **R-4** | **1 guarda em `financeiro/+layout.ts`** (padrão `configuracoes`, não `vendas/pessoas`) | `!isAdmin(user) → redirect 303 /dashboard`. Sem exceção por sub-rota (sem `/perfil` equivalente). Sem `+page.ts` além do par mínimo das listas (SPA). |
| **R-5** | API em 6 módulos PT sobre `apiFetch` (padrão vendas) | Espelha os recursos §5.4 (`lancamentos, categorias, doacoes-recursos, valores-hora+overhead+fechamento+custos→custeio.ts, solicitacoes-compra, relatorios/*`). `1 arquivo = 1 task`. Query só com params não-nulos; erros como `ApiError`. |
| **R-6** | Sem mock local, sem banner "contrato pendente" | Padrão **configurações**: endpoints 🔴/🟡 chamados direto; falha → estado Error amigável por página/bloco (estado #5/#6 do mockup). Sem `USE_FINANCEIRO_MOCK`. |
| **R-7** | **Sem soma no client** — totais/resumo/counts/KPIs/margens servidos | `resumo`, `counts`, `totais`, `cobertura`, `margemLucro`, `custoTotal` renderizam o servido; após mutação → refetch. Preview numérico **só** dentro de modais (digitação), nunca como total. |
| **R-8** | **Sem CRUD de contas** — vistas derivadas | Telas 3/4 filtram `GET /lancamentos` (`tipo` + `status`); criar conta = `modal-novo-lancamento` pré-selecionado (Saída/Entrada); pagar/receber = `PUT /lancamentos/{id}/pagamento`. Endpoints `POST /contas` e `PATCH /{id}/pagar` do requirements **não existem** (C-3). |
| **R-9** | **PDF fora do MVP** | Botão "Exportar" do mockup sem endpoint (nenhum `GET …/pdf` no mockup §7.2 nem no `05`). Mantido como botão desabilitado/omitido; registrado como D-6. `GET /relatorios/extrato?periodo (PDF)` do requirements **não existe** (C-5). |
| **R-10** | Modais = composição local; detalhe sem rota | Os 10 modais vivem dentro das telas que os usam (`Modal` do kit). Linha → detalhe via ação explícita; checkbox com `stopPropagation`. |

**Explicitamente NÃO planejado** (por não resolver caso real): export PDF/CSV, conciliação bancária, notas fiscais além do campo informativo, pagamentos reais (`PaymentProcessor` futuro), estorno parcial (MVP: cancelar e relançar), recálculo de custo no client, soma de KPIs, `pageSize`/`sort` além do URL state do §1, notificações de vencimento (dono: Notification Service), bloqueio de compras, alteração de encomenda fechada (gera nova ordem — só exibir nota).

---

## 7. Ordem em Blocos

- **Bloco 1 — Fundação** (fin-001..fin-010): types → 6 APIs → `financeiro-status` → `financeiro-format` → guarda `financeiro/+layout.ts`.
- **Bloco 2 — Lançamentos + Resumo** (fin-011..fin-012): lista (padrão bulk) → resumo (KPIs + barras + alertas).
- **Bloco 3 — Contas** (fin-013..fin-014): pagar (derivada Saída) → receber (derivada Entrada).
- **Bloco 4 — Doações** (fin-015): registro + cobertura.
- **Bloco 5 — Custeio** (fin-016): 4 abas `?tab=` + 4 modais + notas de congelamento/auditoria.
- **Bloco 6 — Relatórios** (fin-017): 6 panes `?relatorio=` + `?periodo=`.
- **Bloco 7 — Menu** (fin-018): `menu.ts` submenu (depende de todas as rotas existirem; ⚠️ último).
- **Bloco 8 — Testes** (fin-019..fin-023): status+format → api mock → guard → páginas; e2e **skip** (sem Playwright).

Dependências: **B1 → B2 → B3 → B4 → B5 → B6 → B7 → B8** (B8 parcial desde B1 para status/format; B3/B4 paralelizáveis entre si após B2).

---

## 8. Reconciliação requirements × mockup × §5.4 (limpeza documentada, SEM editar o requirements)

| # | Divergência | Autoridade | Limpeza necessária no `requirements.md` |
|---|---|---|---|
| C-1 | Requirements lista **5 rotas** (`lancamentos/contas/custeio/doacoes/relatorios`); mockup tem **7 telas** (falta resumo `/financeiro`; `contas` desdobrada em `contas-pagar` + `contas-receber`) | Mockup §2 | Expandir `Routes` para as 7 rotas do §1. |
| C-2 | Requirements: `/(app)/financeiro/lancamentos` (list + `[id]`); mockup: **detalhe em modal**, sem rota `[id]` | Mockup §8.2 | Remover `[id]`; documentar `modal-lancamento-detalhe`. |
| C-3 | Requirements: `GET /contas?status` + `POST` + `PATCH /{id}/pagar` | §5.4 + mockup §7.4 (vistas derivadas; pagamento via `PUT /lancamentos/{id}/pagamento`) | Remover os 3 endpoints de contas; documentar derivação + pagamento canônico. |
| C-4 | Requirements: `GET /custeio?periodo` ("Projeto vs horas custeadas") | §5.4 + mockup §7.6/§7.7/§8.6 (4 recursos: `valores-hora`, `parametros-overhead`, `fechamento-encomenda`, `custos-encomenda/{id}`; custeio em 4 abas, sem `?periodo=`) | Trocar por os 4 recursos PT + `solicitacoes-compra`; custeio usa `?tab=`, não `?periodo=`. |
| C-5 | Requirements: `GET /relatorios/extrato?periodo` (PDF) | Mockup §7.2 (6 relatórios, sem PDF) + §10 fora de escopo | Trocar por os 6 `GET /relatorios/*`; PDF fora do MVP (D-6). |
| C-6 | Requirements: filtros `?tipo&status&periodo&page` em lançamentos | Mockup §8.2 (busca + 5 painéis: status/tipo/categoria/período/ordenar) + §5.4 (filtros combinados 🟡) | Expandir URL state para `?search=&status=&tipo=&categoria=&periodo=&ordenar=&page=&pageSize=` (assumidos 🟡). |
| C-7 | Requirements omite recursos | §5.4 (9 recursos PT: +`categorias`, `doacoes-recursos`, `valores-hora`, `parametros-overhead`, `fechamento-encomenda`, `solicitacoes-compra`, `custos-encomenda`) | Adicionar os 7 recursos faltantes com naming PT. |

---

## 9. Checklists obrigatórias

### Validator §5 (todas as tasks de tela/API)
- [ ] Svelte 5 runes (`$props/$state/$derived/$effect`), sem `export let`;
- [ ] TS estrito (sem `any`; `import type` para tipos); unions literais PT de status (`TipoLancamento/LancamentoStatus/FechamentoStatus/CompraStatus/TipoDoacao/CusteioTab/RelatorioId`);
- [ ] A11y: `aria-*` nos controles, labels em inputs, `role="tablist"`/`aria-selected` nas abas, `role="dialog"`+Escape+backdrop nos modais, foco visível; **RBAC = HIDE total** (não-Admin nunca recebe DOM do módulo — guarda no layout);
- [ ] URL como fonte de verdade (§1) via `goto`+refetch; "Limpar filtros" volta à URL base; debounce 300ms + `AbortController` nas buscas;
- [ ] i18n pt-BR 100%; status exibidos **direto do backend, sem tradução** (só cor via `financeiro-status.ts`); moeda `Intl` BRL com sinal por tipo; datas `dd/mm/yyyy`; códigos `font-mono` (`LAN-…`);
- [ ] Estados sem layout shift: Loading (skeleton na geometria), Success, Empty, Empty filtrado (chips+limpar), Error (banner+retry), erro parcial por bloco/tab (estado #6); bulk bar só com ≥1 seleção; linha atrasada com fundo `bg-danger/[0.03]`;
- [ ] Após mutação: refetch da página atual; `data-testid` kebab-case do mockup (`lan-row`, `lan-checkbox`, `select-all-rows`, `bulk-bar`, `lan-novo`, `fin-saldo`, `fin-alerta`, `cp-row`, `cp-pagar`, `cr-row`, `cr-receber`, `doa-nova`, `doa-row`, `cus-tab-custos`, `cus-concluir`, `rel-btn-fluxo`, `rel-table`, `fin-chips`).

### Security §6 (idem)
- [ ] Bearer via `apiFetch` (helper `bearer()` local por módulo api); 401 → logout+goto sanitizado (global); sem credenciais hardcoded; sem `console.log`;
- [ ] Guardas client-side = **defesa em profundidade** (autoridade é o backend — D-3): `financeiro/+layout.ts` (`isAdmin`, hide total do item por `canView` com regra `[0]`);
- [ ] 403 → mensagem "sem permissão" na tela (sem crash); 404 lançamento/fechamento → `EmptyState` + voltar;
- [ ] Sem `{@html}`/injeção; `observacao`/justificativa renderizados como texto;
- [ ] Sem dados sensíveis novos (valores financeiros visíveis só a Admin por construção; máscara LGPD N/A — sem CPF/CNPJ no módulo);
- [ ] e2e sem credenciais hardcoded (sem Playwright → **skip** documentado).

---

## 10. Riscos de contrato (endpoints — tudo 🔴, `financeiro-service` scaffold)

| Endpoint | Método | Classe | Status |
|---|---|---|---|
| `GET /api/financeiro/lancamentos` (+filtros, `pagination`, `counts`, `resumo`) · `POST /api/financeiro/lancamentos` · `PUT /api/financeiro/lancamentos/{id}/pagamento` | GET/POST/PUT | 🔴/🟡 | Base de 4 telas (resumo, lançamentos, contas-pagar, contas-receber); filtros combinados + `resumo`/`counts` 🟡 assumidos (mockup §7.3) |
| `GET/POST /api/financeiro/categorias` | GET/POST | 🔴 | — |
| `GET/POST /api/financeiro/doacoes-recursos` | GET/POST | 🔴 | Gera entrada separada das receitas operacionais |
| `GET/POST /api/financeiro/valores-hora` · `POST /api/financeiro/parametros-overhead` | GET/POST | 🔴 | Auditoria server-side (05 §8) |
| `POST /api/financeiro/fechamento-encomenda` · `GET /api/financeiro/fechamento-encomenda/{id}` | POST/GET | 🔴 | Congela horas/valor; nova ordem em alteração (G-9) |
| `GET /api/financeiro/custos-encomenda/{id}` | GET | 🔴 | Custo calculado na entrega e congelado; nunca no client |
| `POST /api/financeiro/solicitacoes-compra` · `PUT …/{id}/concluir` | POST/PUT | 🔴 | Informativo; concluir gera saída (`lancamentoGerado`) |
| `GET /api/financeiro/relatorios/fluxo-caixa\|dre\|lucratividade\|inadimplencia\|doacoes-despesas\|custo-maquina` (+`?periodo=` 🟡) | GET | 🔴/🟡 | Um endpoint por relatório; `?periodo=` assumido (§5.4) |
| `GET /api/vendas/**` (encomendas/clientes) · BOM · `GET /api/rh/**` (horas) | GET | 🔴 | Integrações só-leitura para exibição (Dívida 5); selects com fallback manual |

> Falha de contrato → estado **Error** amigável por página/bloco (padrão configurações, sem mock). Nada bloqueia o frontend além do backend.

### 10.1 Decisões que exigem backend/PO (fila — padrão D-x, sem inventar endpoint)
- **D-1 🔴** — Implementar `financeiro-service` + expor `/api/financeiro/**` no gateway (Dívida 1).
- **D-2 🟡** — Confirmar shapes servidos (`lancamentos+pagination+counts+resumo`, `totais` das contas, `cobertura` das doações, KPIs do resumo/custeio); tudo **servido, nunca derivado**.
- **D-3 🔴** — Enforcement server-side Admin-only (`@PreAuthorize` role 0 em **todos** os endpoints); guards aqui são depth-only.
- **D-4 🟡** — Confirmar filtros combinados (`tipo`+`status`+`categoria`+`periodo`) e `?periodo=` nos 6 relatórios (assumidos, sem lastro no `05` §6).
- **D-5 🔴** — Confirmar regra de congelamento (nova ordem + novo fechamento em alteração de encomenda) e trilha de auditoria de valores-hora/overhead.
- **D-6 🟡** — Exportar PDF/extrato (botão no mockup, sem endpoint): confirmar ou remover do MVP.
- **D-7 🟡** — Mensagens de erro não-técnicas (`code` estruturado) — ressalva transversal.
- **D-8 🟡** — Filtros `vencimento` (contas-pagar) e `origem` (contas-receber) são **de apresentação por página** no front (sem param na API; Validator R-M2 24/09) — confirmar params server-side ou manter como está (sem fingir server-side).
- **Nota (24/09, Validator R-B4):** `pageSize` fixo 10 nas contas (fora da URL) — travado como padrão; lançamentos expõe `pageSize` na URL.
- **Nota (24/09, Validator R-B2/R-B3):** série mensal de valores (doações) e `margemPercentual` pendentes de D-2; `data-testid="rel-btn-{id}"` interpola em runtime (auditado via testes).

---

## 11. Critérios de Entrega (DoD da feature)
- [ ] 7 telas `/financeiro/**` funcionais sobre API real (SPA), URL state deep-linkável, `data-testid` espelhando `full.html`.
- [ ] RBAC hide-total: não-Admin redirecionado no layout + item oculto na sidebar; `permissions.ts` intacto.
- [ ] Contas sem CRUD próprio (derivadas); pagamento via `PUT …/pagamento`; custeio com notas de congelamento/auditoria visíveis; relatórios sem recálculo client.
- [ ] API 100% PT nova (`api/financeiro/*.ts` via `apiFetch`); zero UI novo; zero store novo; PDF fora do MVP.
- [ ] `menu.ts` com submenu Financeiro (7 itens, ⚠️ por último).
- [ ] Testes unit verdes (`npm test`); `npm run check` sem erros novos; `npm run build` ok; e2e documentado como **skip**.
- [ ] Nenhuma alteração em `docs/**` nem `requirements.md`; endpoints 🔴/🟡 reportados (nunca silenciados).

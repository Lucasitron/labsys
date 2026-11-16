# Tasks — Estoque & Suprimentos (frontend)

> Derivado de `requirements.md` + `plan.md` desta pasta. **1 arquivo ≡ 1 task** · ordem:
> types → API → utils/RBAC → correção de telas existentes → rotas novas → cadastros → BOM → testes.
> **Legenda:** 🟡 = contrato pendente/assumido (ver plan §7) · 🔴 = contrato inexistente/divergente — requer decisão
> (`d-pendente`/`G-9`, `G-2b`) · ⚠️ = arquivo compartilhado (`permissions.ts`) — **NUNCA em paralelo com producao/configuracoes**.
> **Regra global:** TS strict · pt-BR · `data-testid` pt-BR (`it-*`,`ent-*`,`sai-*`,`emp-*`,`for-*`,`loc-*`,`bom-*`) ·
> **RBAC: HIDE nunca disable** · endpoint real **PT** `/api/estoque/**` (backend `estoque-service` já existe) ·
> **nunca** referenciar `/stock/**` (contrato fictício removido) · validação inline 409/422 via `stockFetch`.

---

## 1. Fundação — types, API e RBAC (contrato real)

### est-001 · types/stock.ts — refatorar para DTOs reais
- **Arquivo:** `src/lib/types/stock.ts` — modify
- **Conteúdo:**
  - `ItemStatus = 'available'|'low'|'out'|'loaned'|'maintenance'` (derivado client) — manter.
  - `StockItem`/`ItemDetail`: `{ id; nome; descricao; categoria: CategoriaEnum; unidadeMedida; quantidadeAtual; estoqueMinimo; localizacao: LocalizacaoResp | null }` — remover campos fictícios (`codigo`, `valorUnitario`, `maximo`, `reorderPoint`, `leadTime`, `categoriaId`) ou marcá-los `// UI-only, não enviar` (decisão §7 D-3).
  - `CategoriaEnum = 'INSUMO'|'FERRAMENTA'|'PECA'` (backend `Categoria`).
  - `LocalizacaoResp = { id; armario; prateleira; caixa; descricao }` — criar (substitui `Location`).
  - `Fornecedor = { id; nome; contato; cnpj }` — criar (substitui shape fictício).
  - `EntradaResponse`/`SaidaResponse` **reais** (registrar no DTO acima); `Entrada`/`Saida` p/ telas com `tipo` derivado client (badge).
  - `Emprestimo = { id; idItem; nomeItem; idPessoa; quantidade; dataEmprestimo; dataDevolucaoPrevista; dataDevolucaoReal: string|null; status: 'ATIVO'|'DEVOLVIDO'|'ATRASADO'; observacao }` + `loanComputed` derivado (`no_prazo|vence_hoje|atrasado`).
  - `BomResponse = { id; idProdutoServico; nome; versao; editavel; itens: { idItem; nomeItem; quantidadePrevista; quantidadeReal }[] }`.
  - Criar: `CreateItemPayload`, `UpdateItemPayload` (= `CreateItemPayload` com campos opcionais), `CreateSupplierPayload { nome; contato; cnpj }`, `CreateLocationPayload { armario; prateleira; caixa; descricao }`, `MovementDetail` (unifica entrada/saída p/ histórico de item).
  - Manter export names estáveis usados pelas telas atuais (compilará p/ migrar em est-012+).
- **DoD:** sem `any`; nenhum `stock/` restante; `svelte-check` ok.
- **Deps:** — · **Prioridade:** Alta

### est-002 · utils — ajustar enums e computados
- **Arquivo:** `src/lib/utils/stock-status.ts` — modify
- **Conteúdo:** `exitReasonMeta` alinhado a `TipoSaida` real (`CONSUMO→consumo_interno`, `PERDA`, `AJUSTE`, `EMPRESTIMO→empréstimo`); `loanComputedMeta` derivado client (no_prazo/vence_hoje/atrasado) + tone para `status='DEVOLVIDO'` (muted) — manter `statusMeta`, `entryKindMeta`, `quantityTone`.
- **DoD:** mesma API de export dos componentes existentes; testes em est-032.
- **Deps:** est-001 · **Prioridade:** Alta

### est-003 · api/stock/items.ts — migrar para `/estoque/itens`
- **Arquivo:** `src/lib/api/stock/items.ts` — modify
- **Conteúdo:** `listarItens({categoria?, idLocalizacao?, baixo?})` → `GET /estoque/itens` (`List<ItemResponse>`, sem paginação backend — paginar client) · `buscarItem(id)` `GET /{id}` · `criarItem(payload)` `POST` (201) · `atualizarItem(id,payload)` `PUT` · `importarCsv(file)` `POST /import` multipart (`arquivo`) → `List<ItemResponse>` · `exportarCsv()` `GET /export` → `stockFetchBlob` (CSV download). **Remover** `fetchCategories` (categoria é enum — dropdown montado de `CategoriaEnum`), `bulkAction`, paginated fake.
- **DoD:** nenhum `/stock/`; 409/422 → `ApiError` com mensagem pt-BR; testes est-034.
- **Deps:** est-001, est-002 · **Prioridade:** Alta

### est-004 · api/stock/movements.ts — substituir por movimento real
- **Arquivo:** `src/lib/api/stock/movements.ts` — modify
- **Conteúdo:** `listarEntradasPorItem(idItem)` → `GET /estoque/entradas?idItem=` · `buscarEntrada(id)` `GET /entradas/{id}` · `listarSaidasPorItem(idItem)` `GET /saidas?idItem=` · `buscarSaida(id)` `GET /saidas/{id}` · `criarEntrada(payload)` `POST /entradas` · `criarSaida(payload)` `POST /saidas`. Remover `/stock/movements` único. Retornar `EntradaResponse`/`SaidaResponse` crus (tipo/badge derivados client).
- **DoD:** histórico de item usa `?idItem=`; detalhe usa `/{id}`; sem paginação backend.
- **Deps:** est-001 · **Prioridade:** Alta

### est-005 · api/stock/loans.ts — migrar para `/estoque/emprestimos`
- **Arquivo:** `src/lib/api/stock/loans.ts` — modify
- **Conteúdo:** `listarAtrasados()` `GET /emprestimos/atrasados` · `buscarEmprestimo(id)` `GET /emprestimos/{id}` · `criarEmprestimo({idItem,idPessoa,quantidade,dataDevolucaoPrevista,observacao})` `POST /emprestimos` · `devolverEmprestimo(id)` `PUT /emprestimos/{id}/devolucao` (**sem body**). 🔴 **Sem endpoint de lista global/por-status** — dependências em est-023 (R-9 plan §7); dados do tomador via `searchPeople` client (D-4).
- **DoD:** status p/ tabs derivado de `/atrasados` + histórico por item; ver plan §7.
- **Deps:** est-001 · **Prioridade:** Alta

### est-006 · api/stock/suppliers.ts — migrar para `/estoque/fornecedores`
- **Arquivo:** `src/lib/api/stock/suppliers.ts` — modify
- **Conteúdo:** `listarFornecedores()` `GET /estoque/fornecedores` (`List<FornecedorResponse>`; filtro/search client-side) · `criarFornecedor({nome,contato,cnpj})` `POST` (201). **Sem page/pageSize** (sem backend). 🟡 `GET` exige `ADMIN/BOLSISTA` — aplicar guarda frontend (`isAdmin` ou canEdit) p/ 403 amigável (ver est-010).
- **DoD:** nenhum `/stock/`; testes est-034.
- **Deps:** est-001 · **Prioridade:** Alta

### est-007 · api/stock/locations.ts — migrar para `/estoque/localizacoes`
- **Arquivo:** `src/lib/api/stock/locations.ts` — modify
- **Conteúdo:** `listarLocalizacoes()` `GET /estoque/localizacoes` (`List<LocalizacaoResponse>`) · `criarLocalizacao({armario,prateleira,caixa,descricao})` `POST` (201). **Sem `{locations,pagination}`** — array puro.
- **DoD:** campos `armario/prateleira/caixa/descricao`; 409 duplicado → inline.
- **Deps:** est-001 · **Prioridade:** Alta

### est-008 · api/stock/bom.ts — migrar para `/estoque/boms` (`G-2b`)
- **Arquivo:** `src/lib/api/stock/bom.ts` — modify
- **Conteúdo:** `buscarBom(id)` `GET /estoque/boms/{id}` · `criarBom(payload)` `POST /boms` · `atualizarBom(id,payload)` `PUT /boms/{id}` · `registrarConsumo(id, {itens})` `POST /boms/{id}/consumo`. 🔴 **Sem listagem por projeto no backend** — tela bom (est-031) decidir scopo (menu projeto client-side ou aguardar endpoint).
- **DoD:** typecheck ok; ver riscos plan §7 R-8.
- **Deps:** est-001 · **Prioridade:** Média

### est-009 · api/rh.ts — verificar contrato
- **Arquivo:** `src/lib/api/rh.ts` — verify (sem mudança esperada)
- **Conteúdo:** confirmar `searchPeople` batendo `/api/rh/people?search=` (gateway `/api/rh/**`); shape `{ people: [...] }`. Usado em novo/devolução de empréstimo (est-024/est-026).
- **DoD:** nenhuma mudança se contrato OK; documentar no relatório.
- **Deps:** — · **Prioridade:** Alta

### est-010 · permissions.ts — RBAC responsável por estoque (⚠️ compartilhado)
- **Arquivo:** `src/lib/utils/permissions.ts` — modify ⚠️
- **Conteúdo:** adicionar `isResponsavelEstoque(user): boolean` = `user.role !== 4` e (`responsibilities?.estoque?.length ?? 0) > 0`; ampliar `canEdit(user,'estoque')` para incluir responsável (`roles.estoque==='edit'` ou responsabilidade). **Manter `EDIT_RULES.estoque=[0]` intacto** (base de testes). Opcional: `canVerFornecedores` → true só p/ Admin/Bolsista (espelha 403 backend).
- **DoD:** ⚠️ **NUNCA paralelo com producao/configuracoes**; testes est-011; sem quebrar `permissions.test.ts` base.
- **Deps:** est-001 · **Prioridade:** Alta

### est-011 · tests/unit/permissions.test.ts — casos responsável-por-estoque
- **Arquivo:** `frontend/tests/unit/permissions.test.ts` — modify
- **Conteúdo:** casos novos: `isResponsavelEstoque` (admin✓, bolsista sem resp ✗, bolsista com resp estoque ✓, recrutando ✗) · `canEdit('estoque')` true p/ admin e responsável · `canView('estoque')` mantém [0..3].
- **DoD:** base (`EDIT_RULES.estoque = [0]`) permanece; `npm test` verde.
- **Deps:** est-010 · **Prioridade:** Alta

---

## 2. Correção das telas existentes (contrato real)

### est-012 · Estoque Itens — lista
- **Arquivo:** `src/routes/(app)/estoque/itens/+page.ts` + `+page.svelte` — modify
- **Conteúdo:** usar `listarItens` (est-003); filtros `categoria` agora enum (`INSUMO|FERRAMENTA|PECA` — dropdown de `CategoriaEnum`), `idLocalizacao`, `baixo` · paginação **client-side** (lista completa do backend, sem `{items,pagination}`) · manter URL state (`search/category/location/status/page/pageSize/sort`) · CSV: botão Exportar → `exportarCsv()` (blob download `itens.csv`) e Importar → dialog upload multipart `importarCsv(file)` · **RBAC HIDE** de ações de escrita quando `!canEdit` ✓.
- **DoD:** `svelte-check` ok; estados loading/empty/error; testid `it-*`; sem `/stock/`.
- **Deps:** est-001, est-003, est-010 · **Prioridade:** Alta

### est-013 · Estoque Itens — detalhe `[id]`
- **Arquivo:** `src/routes/(app)/estoque/itens/[id]/+page.svelte` (e +page.ts) — modify
- **Conteúdo:** hero + metadata reais (`categoria` badge enum, `estoqueMinimo`, localização `armario/prateleira/caixa`); abas **visao/movimentacoes/emprestimos/fornecedores/historico** via `?tab=`; aba movimentações/histórico preenchida via `listarEntradasPorItem` + `listarSaidasPorItem` (movementDetail) e empréstimos via `buscarEmprestimo` por item (est-005) — remover `/stock/movements`.
- **DoD:** `data-testid="it-detalhe"`; ação Editar → `/estoque/itens/[id]/editar` (canEdit).
- **Deps:** est-001, est-004, est-005 · **Prioridade:** Alta

### est-014 · Estoque Itens — novo
- **Arquivo:** `src/routes/(app)/estoque/itens/novo/+page.ts` + `+page.svelte` — modify
- **Conteúdo:** form real: nome* · descrição · categoria* (enum select) · unidadeMedida* · quantidadeAtual* · estoqueMinimo* · localização (select de `idLocalizacao`, carregado de est-007) — **sem** `codigo/max/reorder/leadTime/valorUnitario` (ou read-only conforme D-3); submit `criarItem` (201) → toast + redirect lista; 409/422 inline.
- **DoD:** `data-testid="it-novo-form","submit-iten"` (padrão mockup) — ajustar p/ `submit-item` se necessário; guarda canEdit.
- **Deps:** est-003, est-007, est-010 · **Prioridade:** Alta

### est-015 · Entradas — lista (escopo decisão R-9)
- **Arquivo:** `src/routes/(app)/estoque/entradas/+page.ts` + `+page.svelte` — modify
- **Conteúdo:** 🔴 sem endpoint global — **escopo reduzido até decisão PO**: listar entradas **por item** via seletor ItemPicker (`GET /entradas?idItem=`) OU manter grid com paginação client-side da lista agregada quando endpoint existir (`d-pendente`). Enquanto isso: histórico por item + link para detalhe est-021.
- **DoD:** decisão §7 R-9 registrada; sem `/stock/movements`.
- **Deps:** est-004, est-012 · **Prioridade:** Média

### est-016 · Entradas — nova
- **Arquivo:** `src/routes/(app)/estoque/entradas/nova/+page.ts` + `+page.svelte` — modify
- **Conteúdo:** form real: ItemPicker (idItem*) · Fornecedor (select `fornecedores`, idFornecedor*) · quantidade* · valorUnitario* · dataEntrada (default hoje) · notaFiscal · observacao → `criarEntrada` `POST /entradas` (201) → toast + voltar; 409 saldo/422 inline.
- **DoD:** `data-testid="ent-nova-form"`; guarda canEdit.
- **Deps:** est-004, est-006, est-010 · **Prioridade:** Alta

### est-017 · Saídas — lista (escopo decisão R-9)
- **Arquivo:** `src/routes/(app)/estoque/saidas/+page.ts` + `+page.svelte` — modify
- **Conteúdo:** espelhar est-015 com saídas (por item; link detalhe est-022).
- **DoD:** sem `/stock/`; guarda canEdit apenas p/ ações.
- **Deps:** est-004, est-012 · **Prioridade:** Média

### est-018 · Saídas — nova
- **Arquivo:** `src/routes/(app)/estoque/saidas/nova/+page.ts` + `+page.svelte` — modify
- **Conteúdo:** form real: ItemPicker (idItem*) · quantidade* · tipoSaida* (select `CONSUMO|PERDA|AJUSTE|EMPRESTIMO`) · idReferencia (opcional) · observacao → `criarSaida` `POST /saidas` (201) → toast; 422/409 inline.
- **DoD:** `data-testid="sai-nova-form"`; canEdit guard.
- **Deps:** est-004, est-010 · **Prioridade:** Alta

### est-019 · MovementList — link para detalhe (G-8)
- **Arquivo:** `src/lib/components/estoque/MovementList.svelte` — modify
- **Conteúdo:** linha clicável → `/estoque/entradas/[id]` ou `/estoque/saidas/[id]` (conforme tipo); colunas reais (`EntradaResponse`/`SaidaResponse`); badge tipo derivado client.
- **DoD:** usado por est-013/est-015/est-017; `data-testid="mv-linha"`.
- **Deps:** est-004 · **Prioridade:** Média

---

## 3. Rotas novas — CRUD e detalhes

### est-020 · Editar Item
- **Arquivo:** `src/routes/(app)/estoque/itens/[id]/editar/+page.ts` + `+page.svelte` — create
- **Conteúdo:** `buscarItem(id)` preenche form (mesmo layout de est-014 com card read-only "Estoque atual ... não é editável") → `atualizarItem(id,payload)` `PUT /itens/{id}`; 409/422 inline; card read-only apenas se campos removidos na UI (D-3); guarda canEdit.
- **DoD:** `data-testid="edit-item-form"` + inputs `item-code`(se mantido)/`item-name`/… do mockup; retorno p/ detalhe.
- **Deps:** est-003, est-014, est-010 · **Prioridade:** Alta

### est-021 · Detalhe de Entrada
- **Arquivo:** `src/routes/(app)/estoque/entradas/[id]/+page.ts` + `+page.svelte` — create
- **Conteúdo:** `buscarEntrada(id)`; hero (badge tipo derivado: compra/doação/… client) · metadata (data/valor/fornecedor/responsável via `searchPeople` D-4) · itens/observações; 404 → "Não encontrada" + voltar; **sem ações de escrita**.
- **DoD:** `data-testid="entrada-detalhe","back-lista","entrada-error-retry"`.
- **Deps:** est-004, est-009 · **Prioridade:** Alta

### est-022 · Detalhe de Saída
- **Arquivo:** `src/routes/(app)/estoque/saidas/[id]/+page.ts` + `+page.svelte` — create
- **Conteúdo:** `buscarSaida(id)`; espelho de est-021 (badge tipoSaida real; destino via idReferencia se projeto etc.); sem escrita.
- **DoD:** `data-testid="saida-detalhe","back-lista","saida-error-retry"`.
- **Deps:** est-004 · **Prioridade:** Alta

### est-023 · Empréstimos — lista (tabs) 🔴 scopo conforme R-9
- **Arquivo:** `src/routes/(app)/estoque/emprestimos/+page.ts` + `+page.svelte` — create
- **Conteúdo:** tabs `ativos|atrasados|historico` via `?tab=`; **atrasados** de `/emprestimos/atrasados`; **ativos/histórico** — sem endpoint global: derivar client-side (todos empréstimos carregados por item? inviável) → **decisão R-9**: enquanto não houver `GET /emprestimos`, exibir tab atrasados + aviso "ativos/histórico disponíveis no detalhe do item" OU listar por item selecionado. Botão "+ Novo empréstimo" (canEdit).
- **DoD:** `data-testid="loan-tab-ativos","loan-return"` (mockup); decisão registrada no relatório.
- **Deps:** est-005, est-010 · **Prioridade:** Alta

### est-024 · Novo Empréstimo
- **Arquivo:** `src/routes/(app)/estoque/emprestimos/novo/+page.ts` + `+page.svelte` — create
- **Conteúdo:** ItemPicker (idItem) · quantidade* · tomador: `searchPeople` combo (idPessoa) · observação · dataDevolucaoPrevista* (hoje ou futura — `@FutureOrPresent` backend) → `criarEmprestimo` `POST /emprestimos` (201) → toast + redirect `/estoque/emprestimos`; guarda canEdit/canOperate.
- **DoD:** `data-testid="emp-novo-form","submit-emprestimo"`; 422 inline.
- **Deps:** est-005, est-009, est-010 · **Prioridade:** Alta

### est-025 · Detalhe de Empréstimo
- **Arquivo:** `src/routes/(app)/estoque/emprestimos/[id]/+page.ts` + `+page.svelte` — create
- **Conteúdo:** `buscarEmprestimo(id)`; hero + metadata (tomador via `searchPeople`), status badge (`ATIVO/DEVOLVIDO/ATRASADO` + dot), prazo com tone (atraso/hoje), timeline eventos (dataEmprestimo → devolução/vencimento); ação **"Registrar devolução"** (canEdit e status≠DEVOLVIDO) → `/emprestimos/[id]/devolucao`. ⚠️ rota fora do sitemap original — documentar (plan §7).
- **DoD:** `data-testid="emprestimo-detalhe","btn-devolucao","emprestimo-error-retry"`.
- **Deps:** est-005, est-009, est-010 · **Prioridade:** Alta

### est-026 · Devolução de Empréstimo
- **Arquivo:** `src/routes/(app)/estoque/emprestimos/[id]/devolucao/+page.ts` + `+page.svelte` — create
- **Conteúdo:** resume do empréstimo (`buscarEmprestimo`) + banner atraso (danger) + form: dataDevolucaoReal (default hoje, display) · **backend `PUT /emprestimos/{id}/devolucao` sem body** → botão confirmar; condição do item **sem endpoint real** (D-E) → exibir seletor somente informativo ou omitir; guarda canEdit.
- **DoD:** `data-testid="emp-devolucao-form","confirmar-devolucao"`; redirect `/estoque/emprestimos?tab=…`.
- **Deps:** est-005, est-010, est-025 · **Prioridade:** Alta

---

## 4. Cadastros básicos

### est-027 · Fornecedores — lista
- **Arquivo:** `src/routes/(app)/estoque/fornecedores/+page.ts` + `+page.svelte` — create
- **Conteúdo:** `listarFornecedores()`; search client-side (nome/cnpj/contato); tabela (nome · CNPJ mono · contato); "+ Novo fornecedor" (canEdit); 🟡 se `403` p/ não-Admin/Bolsista → EmptyState "sem permissão" (est-010).
- **DoD:** `data-testid="for-lista"`.
- **Deps:** est-006, est-010 · **Prioridade:** Média

### est-028 · Novo Fornecedor
- **Arquivo:** `src/routes/(app)/estoque/fornecedores/novo/+page.ts` + `+page.svelte` — create
- **Conteúdo:** form real: nome* · contato · cnpj (máx 32, mono) → `criarFornecedor` `POST /fornecedores` (201) → redirect lista; 409/422 inline.
- **DoD:** `data-testid="fornecedor-form","fornecedor-nome","fornecedor-cnpj","submit-fornecedor"` (ajustar campos p/ reais: nome/contato/cnpj); guarda canEdit.
- **Deps:** est-006, est-010 · **Prioridade:** Média

### est-029 · Localizações — lista
- **Arquivo:** `src/routes/(app)/estoque/localizacoes/+page.ts` + `+page.svelte` — create
- **Conteúdo:** `listarLocalizacoes()`; grid cards (armário · prateleira · caixa · descrição) e "+ Nova localização" (canEdit); **sem capacidade/percent cheio** (campo não existe no backend — D-3).
- **DoD:** `data-testid="loc-card"`.
- **Deps:** est-007, est-010 · **Prioridade:** Média

### est-030 · Nova Localização
- **Arquivo:** `src/routes/(app)/estoque/localizacoes/novo/+page.ts` + `+page.svelte` — create
- **Conteúdo:** form real: armario* · prateleira · caixa · descricao → `criarLocalizacao` `POST /localizacoes` (201) → redirect; 409 duplicado inline.
- **DoD:** `data-testid="localizacao-form","localizacao-codigo","submit-localizacao"` (adequar a `armario` …); guarda canEdit.
- **Deps:** est-007, est-010 · **Prioridade:** Média

---

## 5. BOM

### est-031 · BOM — lista de materiais (G-2b/R-8)
- **Arquivo:** `src/routes/(app)/estoque/bom/+page.ts` + `+page.svelte` — create
- **Conteúdo:** seletor de BOM (client — backend sem listagem por projeto: `buscarBom(id)` após obter ids via… 🔴 **decisão R-8**: criar/ler BOM por produto (POST/GET /{id}) OU aguardar endpoint backend; exibir tabela item · quantidadePrevista · quantidadeReal · disponibilidade via `listarItens` local · "Exportar" (CSV client) · ações de escrita canEdit.
- **DoD:** `data-testid="bom-tabela"`; contrato pendente documentado.
- **Deps:** est-003, est-008, est-010 · **Prioridade:** Baixa

---

## 6. Testes e fechamento

### est-032 · unit — stock-status e stock-url
- **Arquivo:** `frontend/tests/unit/estoque/stock-status.test.ts` + `stock-url.test.ts` — create
- **Conteúdo:** tons por status; `exitReasonMeta` com TipoSaida; `loanComputedMeta` devolvido→muted; parsers de URL (str/int/arr/bool/sanitizeSort).
- **DoD:** cobertura ≥ 90% das branches; `npm test` verde (vitest jsdom).
- **Deps:** est-002 · **Prioridade:** Média

### est-033 · unit — stock-format
- **Arquivo:** `frontend/tests/unit/estoque/stock-format.test.ts` — create
- **Conteúdo:** fmtMoney (Intl pt-BR), fmtQty com unidade, fmtDate/fmtDateTime, toDateInputValue, daysFromToday.
- **DoD:** verde; sem dependência externa.
- **Deps:** — · **Prioridade:** Média

### est-034 · unit — api layer (mock fetch)
- **Arquivo:** `frontend/tests/unit/estoque/api-stock.test.ts` — create
- **Conteúdo:** mock `fetch`/`stockFetch`: `listarItens` (parse List, sem paginação), `criarItem` (201), `atualizarItem` (409→ApiError), `importarCsv` (multipart), `listarEntradasPorItem`/`buscarSaida` (path real), `criarEmprestimo`/`devolverEmprestimo` (PUT sem body), `criarFornecedor`, `criarLocalizacao`.
- **DoD:** garante **nenhum `/stock/`** nos paths; verde.
- **Deps:** est-003..est-008 · **Prioridade:** Média

### est-035 · e2e — condicional (skip sem Playwright)
- **Arquivo:** `frontend/e2e/estoque.spec.ts` — create (com `describ.skip`)
- **Conteúdo:** fluxos: navegar → itens → novo item → entradas/nova → saídas/nova → empréstimos (tab atrasados) → fornecedores/novo → localizações/novo → itens/[id]/editar → detalhes entradas/saídas/empréstimos. Marcado `.skip` (Playwright não instalado — `package.json` sem dependência). Registrar no relatório como pendente.
- **DoD:** não quebrar `npm test` (skip); documentação de execução futura.
- **Deps:** blocos 3–5 · **Prioridade:** Baixa

### est-036 · fechamento — validar e relatar
- **Arquivo:** (nenhum — pipeline) 
- **Conteúdo:** `npm run check` (svelte-check) zero erros · `npm run build` ok · rodar `npm test` (todos os módulos) · conferir `rg "/stock/"` = 0 resultados em `src/` · atualizar relatório de feature (caminhos criados, contratos pendentes R-9/R-8/D-2, dívida D-2 backend response tomador).
- **DoD:** entrar critérios de entrega do plan §8.
- **Deps:** est-001..est-035 · **Prioridade:** Alta

---

## Resumo de blocos

| Bloco | Tasks | Saída |
|---|---|---|
| 1 · Fundação contrato | est-001..est-011 | types DTOs reais + API PT + RBAC responsável + testes |
| 2 · Correção telas existentes | est-012..est-019 | itens/detalhe/novo + entradas/saídas reais (+ lista reduzida R-9) |
| 3 · Rotas novas CRUD/detalhes | est-020..est-026 | editar item · detalhes entrada/saída · empréstimos (lista/novo/detalhe/devolução) |
| 4 · Cadastros | est-027..est-030 | fornecedores · localizações (lista+novo) |
| 5 · BOM | est-031 | lista de materiais (scopo R-8) |
| 6 · Testes + fechamento | est-032..est-036 | unit verdes + e2e skip + relatório |
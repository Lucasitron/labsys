# Plan — Estoque & Suprimentos (Frontend)

> Feature: `estoque` (frontend) · Branch: `dev/frontend` · Gerado por: Planner
> Fonte canônica: `.opencode/specs/frontend/features/estoque/requirements.md`
> Contrato real: `estoque-service` (backend, já implementado — commits `feature/estoque-service` no histórico)
> Mockups: `docs/mokups/estoque/` (19 telas) · Instruções por tela: `docs/mokups/estoque/instrunctions.md`
> Escopo: SÓ frontend. Nenhuma alteração em `docs/**` nem em código backend.

---

## 1. Contexto — a descoberta que muda o plano

O frontend atual do estoque (`src/routes/(app)/estoque/**` + `src/lib/api/stock/**`)
foi construído contra um contrato **EN fictício `/stock/**`** com shape `{ items/loans/movements, pagination }`.

O **backend real já existe** (`estoque-service/`, commits T2–T9 no histórico de `dev/frontend`)
e expõe **PT canônico** através do gateway (`gateway-service` rota `Path=/api/estoque/**`):

| Recurso | Endpoints reais (método `<em>GET</em>` → `<em>POST</em>` → `PUT`) | Contrato real |
|---|---|---|
| Itens | `GET /api/estoque/itens` · `POST /api/estoque/itens` · `GET /api/estoque/itens/{id}` · `PUT /api/estoque/itens/{id}` · `GET /api/estoque/itens/export` (CSV) · `POST /api/estoque/itens/import` (multipart) | `List<ItemResponse>` **sem paginação** |
| Entradas | `POST /api/estoque/entradas` · `GET /api/estoque/entradas?idItem=` · `GET /api/estoque/entradas/{id}` | `List<EntradaResponse>` por item |
| Saídas | `POST /api/estoque/saidas` · `GET /api/estoque/saidas?idItem=` · `GET /api/estoque/saidas/{id}` | `List<SaidaResponse>` por item |
| Empréstimos | `POST /api/estoque/emprestimos` · `PUT /api/estoque/emprestimos/{id}/devolucao` · `GET /api/estoque/emprestimos/atrasados` · `GET /api/estoque/emprestimos/{id}` | `List`/`EmprestimoResponse` |
| Fornecedores | `POST /api/estoque/fornecedores` · `GET /api/estoque/fornecedores` | 🔴 `GET` exige `ADMIN/BOLSISTA`; sem `GET /{id}` |
| Localizações | `POST /api/estoque/localizacoes` · `GET /api/estoque/localizacoes` | `List<LocalizacaoResponse>` |
| BOM | `POST /api/estoque/boms` · `GET /api/estoque/boms/{id}` · `PUT /api/estoque/boms/{id}` · `POST /api/estoque/boms/{id}/consumo` | `BomResponse` (sem listar por projeto) |

**Consequências = os gaps G-1/G-2/G-9** e **3 decisões 🔴 para PO/backend** (§ 8.1–8.3).

---

## 2. Estado real arquivo-a-arquivo (frontend)

### 2.1 Rotas (`src/routes/(app)/estoque/`)
| Arquivo | Estado | Nota |
|---|---|---|
| `+layout.ts` | ✅ OK | Guarda `canView` → redirect `/estoque?erro=proibido`; fornece `canEdit` (RBAC frontend). Reaproveitado. |
| `+layout.svelte` | ✅ OK | ToastHost + Outlet. Reaproveitado. |
| `+page.ts` | ✅ OK | Redirect `/estoque/itens`. Reaproveitado. |
| `itens/+page.svelte` | 🟡 G-2 | Depende de `/stock/items` (inexistente); URL state bom (search/category/location/status/page/pageSize/sort). Migrar p/ itens reais. |
| `itens/[id]/+page.svelte` | 🟡 G-2 | Detalhe; tabs `?tab=` **já** mantidas; carrega movimentos de `/stock/movements?itemId=` (inexistente). |
| `itens/novo/+page.svelte` | 🟡 G-2 | Form; `categorias` de `/stock/categories` (inexistente → enum backend `Categoria`); campos excedentes podem virar 422. |
| `entradas/+page.svelte` e `entradas/nova/` | 🔴 G-2/G-9 | Lista global de entradas **não existe no backend**; nova entrada OK (POST `/entradas`). |
| `saidas/+page.svelte` e `saidas/nova/` | 🔴 G-2/G-9 | Idem: lista global de saídas **não existe**; nova saída OK (POST `/saidas`). |
| Faltantes (12) | — | `itens/[id]/editar`, `entradas/[id]`, `saidas/[id]`, `emprestimos/{/,/novo,/novo/[id],/[id],/[id]/devolucao}`, `fornecedores/{/,/novo}`, `localizacoes/{/,/novo}`, `bom` |

> Nota: mockup/`instrunctions.md` desenham listas globais com paginação (Entradas/Saídas/Tabs de Empréstimos). O backend real **só** lista por item (`?idItem=`) e empréstimos por `/atrasados`. Escopo das telas de listagem precisa de decisão (§ 8) ou adaptação client-side (§ 7.1).

### 2.2 API (`src/lib/api/stock/`)
| Arquivo | Estado | Problema |
|---|---|---|
| `request.ts` | ✅ OK | `stockFetch`/`Blob`/`Response`, timeout 15s, AbortController, 401→logout+goto, `ApiError{status,code,message}` (não conflita com `client.ts`; duplicação aceita p/ manter SPA). |
| `items.ts` | 🔴 G-2 | base `/stock/items`; `paginated` espera `{items,pagination}`; `fetchCategories` `/stock/categories` (inexistente); `bulk` `/stock/items/bulk` (inexistente — substituir por `/itens/import` multipart + `/itens/export` CSV). |
| `movements.ts` | 🔴 G-2 | `/stock/movements` **inexistente**; backend separa `/entradas` e `/saidas`. |
| `loans.ts` | 🔴 G-2 | `/stock/loans?...status/dateFilter` inexistente; real: `POST /emprestimos`, `PUT /emprestimos/{id}/devolucao` (sem payload), `GET /emprestimos/atrasados`, `GET /emprestimos/{id}`. |
| `suppliers.ts` | 🟡 G-2 | `/stock/suppliers` com `page/pageSize/search`; real `GET/POST /fornecedores` (sem search nem paginação), `GET` só p/ Admin/Bolsista. |
| `locations.ts` | 🔴 G-2 | `/stock/locations` com `{locations,pagination}`; real `GET/POST /localizacoes` → `List<LocalizacaoResponse>`. |
| `bom.ts` | 🔴 G-2 | `/stock/bom?projectId=` e `/stock/bom/{id}/pedido` **inexistentes**; real `POST /boms`, `GET/PUT /boms/{id}`, `POST /boms/{id}/consumo` (detalhe em G-2b). |
| `rh.ts` | ✅ OK | `searchPeople` → `/rh/people?search=` (confere com rota gateway `/api/rh/**`); usado em devolução/novo empréstimo. |

### 2.3 Tipos (`src/lib/types/stock.ts` — 320 linhas)
| Tipo | Estado | Nota |
|---|---|---|
| `StockItem` | 🟡 G-3a | Ok para exibição; divergência de campos (ver tabela contrato). `min/max/reorderPoint` substituídos por `estoqueMinimo`. |
| `ItemDetail` | ✅ OK | DTO do detalhe (getItemDetail). Manter. |
| `CreateItemPayload` | ✅ OK | `codigo` deve ser `null` (backend não recebe) OU enquadrar como descrição? → 🔴 decisão (nome/categoria/unidadeMedida/quantidadeAtual/estoqueMinimo/idLocalizacao). |
| `ItemStatus`/`statusMeta` | ✅ OK | `available|low|out|loaned|maintenance` derivados. Manter. |
| `Movement`/`MovementsResult` | 🔴 G-3a/G-10 | Shape `{entrada|saida}` fictício; real = `EntradaResponse`/`SaidaResponse` separados. Refatorar p/ `MovimentacaoComum` + variações (TipoUnificada). |
| `Category`/`CategoriesResult` | 🔴 G-3a | Não existe no backend; categorias = **enum** `INSUMO|FERRAMENTA|PECA`. |
| `Location`/`LocationsResult` | 🔴 G-3a | Shape `{codigo,nome,descricao,capacidade}` vs real `{armario,prateleira,caixa,descricao}`. |
| `Fornecedor` | 🔴 G-3a | `{nome,cnpj,telefone,email,obs}` vs real `{nome,contato,cnpj}`. |
| `Loan`/`LoanListResult` | 🔴 G-3a | `{itemId,itemName,borrowerId,borrowerName,status,computed,...}` vs real `{idItem,nomeItem,idPessoa,dataEmprestimo,dataDevolucaoPrevista,dataDevolucaoReal,status(ATIVO|DEVOLVIDO|ATRASADO),observacao}`. |
| `BomReport`/`BomOrderResult` | 🔴 G-3a/G-2b | `{projectId,projectName,units,generated}` inexistente; real `BomResponse{BomItemResponse{idItem,nomeItem,quantidadePrevista,quantidadeReal}}`. |
| `Pagination<T>` | ✅ OK | Mantido (paginador client-side p/ listas). |
| `UpdateItemPayload`/`CreateSupplierPayload`/`CreateLocationPayload`/`MovementDetail` | ➕ G-3b | Criar. |

### 2.4 Utils
| Arquivo | Estado |
|---|---|
| `stock-url.ts` (str/int/arr/boolParam, sanitizeSort) | ✅ OK |
| `stock-status.ts` (statusMeta, entryKindMeta, exitReasonMeta, loanComputedMeta, quantityTone) | ✅ OK — ajustar marcas p/ `TipoSaida` real (CONSUMO→consumo_interno, EMPRESTIMO) |
| `stock-format.ts` (fmtMoney, fmtQty, fmtDate, fmtDateTime, toDateInputValue, daysFromToday) | ✅ OK |
| `permissions.ts` | 🟡 **G-6** — `EDIT_RULES.estoque=[0]` e `canEdit` por regra de papel; requirements pedem **Admin OU responsável por estoque** + backend restringe `ADMIN/BOLSISTA` (§ 8.2). Add `isResponsavelEstoque`. |

### 2.5 Componentes (`src/lib/components/`)
- `estoque/ItemPicker.svelte`, `estoque/PeoplePicker.svelte` — ✅ reutilizáveis (search-combo).
- `estoque/MovementList.svelte` — 🟡 usado em listas globais inexistentes; reuso em histórico por item.
- `ui/` — ✅ prontos (`PageHeader`, `SearchInput`, `Dropdown`, `Checkbox`, `StatusBadge`, `TypeBadge`, `Pagination`, `TableSkeleton`, `EmptyState`, `ConfirmDialog`, `Chip`, `Icon`, `ErrorBanner`, `BulkActionsBar`, `Modal`, `RowActions`) usados em configuracoes; **não recriar**.

### 2.6 Menu e RBAC
- `menu.ts` — ✅ submenu Estoque completo (7 itens). Sem mudança.
- `permissions.ts` — 🟡 G-6 (edit responsibility-aware) + `canSeeLoans` já existe (usado em dashboard). Sem conflito crítico; conflito potencial com producao/configuracoes (arquivo compartilhado) → **ConflictMap §3**.

### 2.7 Testes existentes
- `tests/unit/permissions.test.ts` — RBAC base `canView/canEdit` por módulo; **G-6** adicionar casos responsável-por-estoque (sem quebrar base: manter `EDIT_RULES.estoque=[0]`; mudar apenas o helper `canEdit` quando houver responsabilidade).
- `tests/unit/` — mesmo padrão vitest+jsdom dos demais módulos (`web` type-check via `svelte-check`; sem Playwright). ✅
- `frontend/tests/unit/estoque/` — ➕ novos casos: `stock-status`, `stock-format`, `stock-url`, parser enum, api mocks.

---

## 3. Gaps (G-1 … G-10)

| Gap | Descrição | Tipo/Escopo |
|---|---|---|
| **G-1** | Migração EN→PT nos 7 arquivos de API + ajuste de shape (listas reais sem paginação; endpoints por item; CSV real). | Contrato — bloqueia blocos 2–5 (@ref task est-017). |
| **G-2** | Contratos divergentes nas telas já existentes (itens/novo/entradas/saidas) — campos excedentes → 422; listas globais sem endpoint. | Frontend+adaptativo |
| **G-2b** | BOM: sem listagem por projeto; ajustar p/ DTO real `BomResponse` (não confundir com BOM de produção do módulo producao). | Contrato |
| **G-3** | Tipos: (a) refatorar existentes p/ DTOs reais; (b) criar `UpdateItemPayload`, `MovementDetail`, `CreateSupplierPayload`, `CreateLocationPayload`, enums computados. | Tipos |
| **G-4** | 12 rotas faltantes + 4 telas de CRUD. | Frontend |
| **G-5** | Estados de tela (loading/empty/error/RBAC) nas novas telas — seguir padrão das telas existentes do estoque e de producao/configuracoes. | Frontend |
| **G-6** | RBAC frontend: `canEdit` por responsabilidade + `isResponsavelEstoque` + testes. | RBAC |
| **G-7** | Histórico por item: entradas/saidas por `?idItem=`; detalhe por `GET /entradas/{id}` / `GET /saidas/{id}`; atualizar badge do item. | Contrato |
| **G-8** | `MovementList` sem link p/ detalhe. | Frontend |
| **G-9** | **Listas globais (Entradas/Saídas/Empréstimos tabs) não suportadas** pelo backend = maior divergência. | 🔴 PO/backend |
| **G-10** | Tipos `Movement*` fictícios + `entryKindMeta/exitReasonMeta` com marcação por enum inexistente. | Frontend |

---

## 4. Estratégia de Blocos de Implementação

> Ordem: cada rota **Bloqueia → Depende de**. Frontend 1 task por arquivo; API/contrato primeiro.

- **Bloco 1 — Contrato (API + Tipos)**: `(api/stock)` reescrito p/ PT-comum (G-1) · `types/stock.ts` refatorado p/ DTOs reais + novos tipos (G-3) · `rh.ts` mantido · `permissions.ts` responsável-por-estoque + testes (G-6).
- **Bloco 2 — Correção das 7 telas existentes**: `itens`, `itens/[id]` (tabs+histórico por item), `itens/novo`, `entradas`, `entradas/nova`, `saidas`, `saidas/nova` — agora sobre o contrato real (G-2, G-7, G-8). Bugfix: `itens/[id]` tabs `?tab=` via `nav tab` — já ok.
- **Bloco 3 — CRUD novo + detalhes (rotas faltantes)**: `itens/[id]/editar`, `entradas/[id]`, `saidas/[id]`, `emprestimos` list/tabs + `emprestimos/novo`, `emprestimos/[id]` (detalhe), `emprestimos/[id]/devolucao`.
- **Bloco 4 — Cadastros básicos**: `fornecedores`, `fornecedores/novo`, `localizacoes`, `localizacoes/novo`.
- **Bloco 5 — BOM**: `bom` sobre DTO real (descartada listagem por projeto até decisão backend).
- **Bloco 6 — Testes**: unit dos utils, api mock, RBAC responsável; e2e **skip** (sem Playwright no `package.json`).

Dependencies entre blocos: **B1 → B2 → {B3, B4} → B5**; **B6 parcialmente paralelo (unit de utils desde o início)**.

---

## 5. Conflitos e Arquivos Compartilhados (ConflictMap)

| Arquivo | Dono/Impacto |
|---|---|
| `src/lib/utils/permissions.ts` | 🔶 Estoque precisa **adicionar** `isResponsavelEstoque`/editar `canEdit` (não alterar `EDIT_RULES.estoque` = `[0]`). Conflito com `producao`/`configuracoes` (todas usam o mesmo arquivo). **Sequenciar após `configuracoes` (já concluído na branch atual).** |
| `src/lib/components/layout/Sidebar.svelte` | ✅ Sem conflito (Estoque já visível). |
| `src/lib/config/menu.ts` | ✅ Sem conflito (itens já existem). |
| `src/lib/types/stock.ts` | 🔶 Refatoração ampla; único arquivo de tipos do módulo. Garantir export names estáveis p/ consumidores atuais (`itens`, `itens/[id]`). |
| `src/lib/api/client.ts` | 🔶 Não alterar `API_BASE`; `request.ts` não o exporta. Manter. |
| `docs/**` | ⛔ **Congelado** — nenhuma edição em `docs/contract.gap.md`; Dívida 2 fica no backend. Nota na seção risco (§ 8). |

---

## 6. Checklists Obrigatórias (Validator §5 e Security §6)

> Aplicar em **todas** as tasks de telas/API. (Referência: `producao`/`configuracoes` plan.md — blocos Validator/Security.)

**Validator §5:**
- [ ] Svelte 5 runes (`$props`, `$state`, `$derived`, `$effect`), sem `export let`;
- [ ] Tipagem estrita TS (sem `any`; `import type` quando só tipo);
- [ ] Acessibilidade: `aria-*` em controles interativos, foco/teclado, labels nos inputs, `role="tablist"`/`aria-selected` nas tabs;
- [ ] Formulários: validação inline pt-BR, `novalidate` + mensagens junto ao campo, bloqueio de submit duplicado (busy);
- [ ] URLs como fonte de verdade (`goto` + `invalidateAll`) nas telas com estado (itens, entradas, emprestimos tabs);
- [ ] i18n pt-BR em 100% dos textos de UI e mensagens de erro;
- [ ] Estados de tela: loading skeleton, empty, error (banner+retry), RBAC (guard redirect) — sem layout shift.

**Security §6:**
- [ ] `canEdit` usado para guiar botões/ações (defesa em profundidade; backend é autoridade final);
- [ ] 401 → `logout()` + `goto('/auth/login?redirect=...')` (via `stockFetch`);
- [ ] 403 → mensagem de sem permissão na tela (não crash);
- [ ] 409/422 → mapear code/message e exibir inline amigável (via `toUserMessage`);
- [ ] e2e sem credenciais hardcoded (sem Playwright instalado → **skip**);
- [ ] sem `v-html`/`{@html}`/injeção; textos renderizados como texto.

---

## 7. Riscos / Decisões que exigem PO ou Backend

### 7.1 Riscos 🔴 (contrato)
- **R-9 (🔴)**: listas globais Entradas/Saídas/Tabs de Empréstimos **não existem** no backend (só por-item e `/atrasados`). Sem decisão, telas com escopo reduzido: Entradas/Saídas → histórico por item; Empréstimos tabs → ativos/atrasados via `/atrasados` + histórico por item (P2).
- **R-8 (🔴)**: BOM frontend não bate com backend (não filtra por projeto; shape real BOM = lista de itens com `quantidadePrevista/Real`). Implementar sobre `BomResponse` (detalhe por id) OU aguardar endpoint backend de consulta por projeto (P3).

### 7.2 Divergências 🟡 / a confirmar com PO
- **D-2 (🟡)**: frontend segue requirements (**Edit = Admin OU responsável**), mas backend restringe escrita a `ADMIN/BOLSISTA` (itens/entradas/saídas/fornecedores/localizações/BOM) e `ADMIN/BOLSISTA/VOLUNTARIO` (empréstimos). Adequar `canEdit` ao backend, documentando a regra, OU aguardar backend aceitar responsáveis.
- **D-1 (🟡)**: endpoints "assumidos" da Dívida 1 no gap (GET `/movimentacoes/{id}`, POST `/fornecedores`, POST `/localizacoes`, GET `/emprestimos?status=`) — **já resolvidos** pelo backend real (POST fornecedores/localizações existem; GET entradas/saídas/emprestimos por id existem). Atualizar `docs/contract.gap.md` é ⛔ proibido — registrar apenas aqui.
- **D-3 (🟡)**: campos excedentes nas telas existentes (ex.: `codigo`, `min/max/reorder`, `valorUnitario` em item — backend não possui) → manter na UI como leitura/desabilitado OU remover (decisão visual + validação).
- **D-4 (🟡)**: empréstimos não retornam dados do tomador (`idPessoa` apenas) → renderizar nome via `searchPeople` client-side (busca por id) OU aguardar backend enriquecer.

---

## 8. Critérios de Entrega (DoD da Feature)
- [ ] 7 telas existentes corrigidas p/ contrato real (sem referências a `/stock/**`, `{...pagination}` ok client-side, sem `codigo` fictício).
- [ ] 12 rotas faltantes implementadas (lista completa do Requirements) com estados/UX padrão.
- [ ] API layer 100% PT (uso real de `/api/estoque/**`, `GET/POST/PUT` corretos).
- [ ] Tipos refletindo DTOs reais + novos tipos; nenhum shape "fictício" restante.
- [ ] RBAC frontend responsável-por-estoque implementado + testes; `EDIT_RULES.estoque` intacto.
- [ ] Testes unit (utils, api mock, RBAC) verdes via `npm test`; e2e documentado como **skip** (sem Playwright).
- [ ] `svelte-check` sem erros; `npm run build` ok.
- [ ] **Nenhuma alteração** em `docs/**`.
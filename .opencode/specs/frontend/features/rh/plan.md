# Plan — Pessoas & RH (Frontend)

> Feature: `rh` (frontend) · Branch: `dev/frontend` · Gerado por: Planner (despacho `.opencode/state/planner-dispatch-rh.md`, 21/09/2026)
> Fonte canônica: `.opencode/specs/frontend/features/rh/requirements.md`
> Mockup aprovado: `docs/mokups/pessoas/` (`full.html` 12 sections · `novasTelasEstados.html` telas 3 e 8 · `requisitos.md` v0.4 · `instrunctions.md` 12 prompts)
> Escopo: SÓ frontend. NÃO editar `docs/**`; NÃO implementar; NÃO commitar. Nenhum endpoint inventado (só os da spec; divergências → D-x/R-x).
> Princípio **código mínimo**: reuso > create; nada especulativo; cada arquivo resolve caso real do requirements/mockup.

---

## 1. Contexto e decisão de rotas

O requirements lista rotas curtas (`/(app)/pessoas`, `/processo-seletivo`, `/registro-horas`, `/treinamentos`, `/certificados`, `/niveis`), mas o **mockup aprovado** (fonte visual) prefixa tudo em `/(app)/pessoas/**` (`requisitos.md` §2, tabela de 12 telas). **Decisão: canônico = `/pessoas/**`** (deep-link + crumbs + submenu de Treinamentos funcionam sob um único layout). As rotas curtas do requirements são lidas como atalhos do mesmo recurso.

| # | Tela | Rota canônica | Mockup section |
|---|---|---|---|
| 1 | Pessoas (lista) | `/pessoas` | `pessoas-lista` |
| 2 | Pessoa (detalhe) | `/pessoas/[id]` (`?tab=`) | `pessoa-detalhe` |
| 3 | Editar pessoa | `/pessoas/[id]/editar` | `pessoa-editar` (`data-testid="edit-person"`) |
| 4 | Novo cadastro | `/pessoas/novo` | `nova-pessoa` |
| 5 | Processo seletivo | `/pessoas/processo-seletivo` (`?estagio=`) | `processo-seletivo` |
| 6 | Registro de horas | `/pessoas/registro-horas` (`?status=&periodo=`) | `registro-horas` |
| 7 | Treinamentos (visão geral) | `/pessoas/treinamentos` | `treinamentos` |
| 8 | Treinamento (detalhe) | `/pessoas/treinamentos/[id]` | `treinamento-detalhe` (`data-testid="training-detail"`) |
| 9 | Agenda | `/pessoas/treinamentos/agenda` | `agenda` |
| 10 | Documentação | `/pessoas/treinamentos/documentacao` | `documentacao` |
| 11 | Sessão do instrutor | `/pessoas/treinamentos/sessao` | `treinamento-sessao` |
| 12 | Níveis & acesso (Admin) | `/pessoas/niveis` | `niveis` |
| — | Certificados | **NÃO planejado** (ver §7 R-7; mockup §10 fora de escopo; contrato v0.2.0 futuro) | — |

URL state (fonte de verdade): lista `?search=&setor=&nivel=&status=&page=` · horas `?status=&periodo=` · detalhe `?tab=` · PS `?estagio=`.

---

## 2. Estado real arquivo-a-arquivo (frontend)

### 2.1 Rotas (`src/routes/(app)/`)

| Arquivo | Estado | Nota |
|---|---|---|
| `(app)/+layout.ts` / `+layout.svelte` | ✅ OK | Guarda auth global + polling de notificações. Não tocar. |
| `(app)/pessoas/**` | 🔴 **G-1** | **Não existe** (`ls` confirma: só `configuracoes dashboard estoque notificacoes producao`). As 12 telas serão create. |
| `(app)/estoque/+layout.ts` | ✅ referência | Padrão de guarda `canView → redirect` + `canEdit` no `load` (replicar em `pessoas/+layout.ts`). |
| `(app)/notificacoes/+layout.ts` | ✅ referência | Guarda Admin central (`isAdmin`, redirect 303). Padrão para `/pessoas/niveis`. |

### 2.2 API (`src/lib/api/`)

| Arquivo | Estado | Nota |
|---|---|---|
| `client.ts` | ✅ OK | `apiFetch` (timeout 15s, Bearer, 401→logout+goto sanitizado, `ApiError/NetworkError`). Base de toda API nova. Não tocar. |
| `rh.ts` | 🔴 **G-2** | Só `searchPeople(search) → stockFetch('/rh/people?search=')` (14 linhas). Problemas: (a) naming **EN** (`/rh/people`) diverge do PT canônico `/api/rh/pessoas` (requirements + `contract.gap.md` §3–§4); (b) acoplado a `stock/request` (dono: estoque); (c) cobre ~1 de ~20 operações da spec. **Migrar**: novo `api/rh/*.ts` sobre `apiFetch`; manter `rh.ts` como re-export fino ou remover no Builder (decisão de enxugamento R-2). Usado hoje por estoque (`searchPeople` em devolução/novo empréstimo) — preservar export até migração. |
| `stock/request.ts` | ✅ OK | `stockFetch`/`buildQuery`/401→logout. Referência; RH novo usa `apiFetch` (padrão notificações/config), não duplica. |
| `dashboard.ts` / `notifications.ts` / `producao/*` / `configuracoes/*` | ✅ OK | Padrão `bearer()` local + `apiFetch`. Não tocar. Integrações `GET /api/projects`, `/api/production/machines`, `/api/production/tasks` **não existem** no front (🔴, ver §9). |

### 2.3 Tipos e utils

| Arquivo | Estado | Nota |
|---|---|---|
| `types/auth.ts` | ✅ OK | `Role 0–4`, `Module` (inclui `rh`), `User { responsibilities?: Partial<Record<Module,string[]>> }`. Base do RBAC responsável/Tutor/Instrutor. |
| `types/rh.ts` | 🔴 **G-3** | Inexistente. Criar DTOs PT (Pessoa, Grupo, PS, Horas/Apontamento, Treinamento, Guia, Níveis, paginados). |
| `utils/permissions.ts` | 🟡 **G-7** | `VIEW_RULES.rh=[0,1,2,3,4]`, `EDIT_RULES.rh=[0,1,2]` existem, mas **sem** helpers RH: sem `isResponsavelRH`/`isTutor`/`isInstrutor`, sem `canSeeNiveis`, sem regra "próprio registro" (Estagiário/Recrutando). `EDIT_RULES.rh` é permissivo demais vs requirements (Bolsista/Voluntário **só se responsável**). Completar por helpers (não alterando `EDIT_RULES`). |
| `utils/format.ts` | ✅ OK | `initials`, `relativeTime` pt-BR, `formatNumber` — **REUSAR** (R-1). Nada de `rh-format.ts` duplicado. |
| `utils/rh-status.ts` | 🔴 **G-4** | Inexistente. Criar **só** mapas `{label,color}` canônicos (§7.8 mockup). |
| `utils/errors.ts` | ✅ OK | `toUserMessage` já cobre Network/5xx/UNKNOWN. Reusar (409 inline). |

### 2.4 Componentes (`src/lib/components/ui/` — 22 arquivos)

✅ Prontos e **obrigatoriamente reusados**: `Avatar, SearchInput, Dropdown, Checkbox (indeterminate), Select, SearchCombo, StatusBadge, Pagination, TableSkeleton (+Skeleton), EmptyState, ConfirmDialog, Chip, PageHeader, Tabs, Modal, ErrorBanner, BulkActionsBar, RowActions, RadioCards, Icon (já tem bell/users/wrench/cube), ToastHost`.

| Faltante | Gap | Decisão |
|---|---|---|
| `TypeBadge.svelte` | 🟡 **G-5a** | **Create** (1 arquivo): badge de tipo/motivo cor semântica — `StatusBadge` é dot+status, não cobre tipo (Encomenda/Projeto, trilhas). Uso: horas, treinamentos, documentação. |
| `DonutChart.svelte` | 🟡 **G-5b** | **Create** (1 arquivo): SVG `viewBox 0 0 42 42 r=15.9` (contrato mockup §4.2/Prompt 0). Existe cópia **local** em `dashboard/components/DonutChart.svelte` (não compartilhada) — promover o padrão para o UI kit, sem tocar dashboard. Uso: treinamentos overview + donuts por grupo. |
| `KanbanBoard.svelte` | 🟡 **G-5c** | **Create** (1 arquivo): colunas + cards **sem drag lib** (botões/chips movem etapa; precedente estoque/produção). Uso: processo seletivo (5 colunas por estágio). |

Nada mais de UI novo (filtros/skeletons/empties/errors são composição do kit — precedente notificações R-3).

### 2.5 Componentes de domínio / stores

- `components/estoque/PeoplePicker.svelte` ✅ **reusar** (search-combo de pessoa via `searchPeople`) no modal "Registrar horas" e "Criar grupo" (R-3).
- `components/estoque/ItemPicker.svelte` — não se aplica ao RH.
- `stores/auth.ts`, `stores/toast.ts` ✅ reusar. **Sem** store novo de RH (dado via fetch + refetch; precedente notificações R-1).
- `components/layout/Sidebar.svelte` ✅ renderiza `menuItems.children` filtrados por `canSee` — nenhum toque necessário além do `menu.ts`.

### 2.6 Config (`src/lib/config/menu.ts`)

| Estado | Nota |
|---|---|
| 🟡 **G-6** | Item `Pessoas & RH → /pessoas (module rh)` existe (linha 24) mas **sem `children`**. Adicionar submenu: Pessoas, Processo seletivo, Registro de horas, Treinamentos (toggle do second sidebar), Níveis & acesso (`canSee: isAdmin`). Crumb atual de `/pessoas/x` cai em `beautify` genérico — submenu corrige. |

### 2.7 Testes existentes

- `tests/unit/permissions.test.ts` — RBAC base; estender com casos RH (G-7) sem quebrar base.
- `tests/unit/format.test.ts`, `client.test.ts`, `errors.test.ts` — cobrem o que será reusado.
- Zero testes de RH → **G-13**. Infra: vitest+jsdom+testing-library (sem Playwright → e2e **skip**, padrão das features).

---

## 3. Gaps (G-1 … G-13)

| Gap | Descrição | Tipo |
|---|---|---|
| **G-1** | 12 rotas `/pessoas/**` inexistentes (lista, detalhe, novo, editar, PS, horas, 5 de treinamentos, níveis) + 2 layouts (guarda RH, second sidebar treinamentos). | Frontend |
| **G-2** | `api/rh.ts` cobre ~1/20 operações, naming EN `/rh/people`, acoplado a `stock/request`. Migrar para `api/rh/*.ts` PT sobre `apiFetch`. | Contrato 🟡/🔴 |
| **G-3** | `types/rh.ts` inexistente (Pessoa/Grupo/PS/Horas/Apontamento/Treinamento/Agenda/Guia/Sessão/Níveis + paginados + params). | Tipos |
| **G-4** | `utils/rh-status.ts` inexistente (mapas pessoas/PS/horas/treinamento/disponibilidade/nivel). | Frontend |
| **G-5** | UI kit: `TypeBadge`, `DonutChart` (só local no dashboard), `KanbanBoard` inexistentes no kit. | Frontend |
| **G-6** | `menu.ts` sem submenu RH; Níveis sem `canSee: isAdmin`. | Frontend+RBAC |
| **G-7** | `permissions.ts` sem helpers RH (`isResponsavelRH`, `isTutor`, `isInstrutor`, `canSeeNiveis`, `isProprioRegistro`); `EDIT_RULES.rh=[0,1,2]` permissivo vs "só se responsável". | RBAC |
| **G-8** | Processo seletivo: Kanban **por grupos** + avaliação **individual** + criar grupo/inscrição + mover etapa — inexistente. | Frontend+contrato 🔴 |
| **G-9** | Registro de horas: **modelo duplo** — spec lista `/api/rh/horas` legado, mas backend real tem `/apontamentos-horas` + `/horas/disponiveis` + `ExtratoMensalHorasEvent` (Dívida 4). Risco de somar horas no client. | Contrato 🔴/🟡 |
| **G-10** | Treinamentos/agenda/documentação/sessão: lista/detalhe/sessão/avaliar/tarefa-final/guias/disponibilidade — contratos 🔴; integrações `projects/machines/tasks` 🔴. | Contrato 🔴 |
| **G-11** | Níveis & acesso: matriz + alterar nível + convidar — sem guard/layout Admin. | Frontend+contrato 🔴 |
| **G-12** | Certificados: requirements exige (`/certificados`), mockup §10 declara **fora de escopo MVP**, contrato é v0.2.0 futuro — **não planejar telas**. | Escopo (Dívida 4) |
| **G-13** | Zero testes unit do módulo (status/api/RBAC/páginas). | Testes |

---

## 4. Camadas / arquivos a criar ou editar

**Create (26):**
- `src/lib/types/rh.ts` (G-3)
- `src/lib/api/rh/pessoas.ts` · `processo-seletivo.ts` · `horas.ts` · `treinamentos.ts` · `niveis.ts` (G-2/G-8/G-9/G-10/G-11)
- `src/lib/utils/rh-status.ts` (G-4)
- `src/lib/components/ui/TypeBadge.svelte` · `DonutChart.svelte` · `KanbanBoard.svelte` (G-5)
- `src/routes/(app)/pessoas/+layout.ts` (guarda RH) (G-1)
- `src/routes/(app)/pessoas/+page.svelte` (+ `+page.ts` no mesmo task) (tela 1)
- `src/routes/(app)/pessoas/[id]/+page.svelte` (+loader) (tela 2)
- `src/routes/(app)/pessoas/novo/+page.svelte` (tela 4)
- `src/routes/(app)/pessoas/[id]/editar/+page.svelte` (tela 3)
- `src/routes/(app)/pessoas/processo-seletivo/+page.svelte` (tela 5)
- `src/routes/(app)/pessoas/registro-horas/+page.svelte` (tela 6)
- `src/routes/(app)/pessoas/treinamentos/+layout.svelte` (second sidebar) (mockup §4.4)
- `src/routes/(app)/pessoas/treinamentos/+page.svelte` (tela 7)
- `src/routes/(app)/pessoas/treinamentos/[id]/+page.svelte` (tela 8)
- `src/routes/(app)/pessoas/treinamentos/agenda/+page.svelte` (tela 9)
- `src/routes/(app)/pessoas/treinamentos/documentacao/+page.svelte` (tela 10)
- `src/routes/(app)/pessoas/treinamentos/sessao/+page.svelte` (tela 11)
- `src/routes/(app)/pessoas/niveis/+page.svelte` (tela 12)
- Testes: `tests/unit/rh-status.test.ts` · `tests/unit/rh-api.test.ts` · `tests/unit/rh-permissions.test.ts` · `tests/unit/rh-pages.test.ts` (G-13)

**Modify (2):**
- `src/lib/utils/permissions.ts` (G-7 — **só** adicionar helpers; `EDIT_RULES`/`VIEW_RULES` intactos)
- `src/lib/config/menu.ts` (G-6 — adicionar `children` ao item existente)

**Não criar** (enxugamento §7): `api/rh/unread`-like, stores RH, `rh-format.ts`, componentes de estado por tela, telas de certificados, CSV export, second-sidebar como lib separada.

---

## 5. ConflictMap (arquivos compartilhados)

| Arquivo | Dono/Impacto |
|---|---|
| `src/lib/config/menu.ts` | 🔶 RH adiciona `children` ao item `Pessoas & RH` existente. **Livre agora** (notificações concluída 21/09; configuracoes C1/C2 liberados). Próxima feature que tocar menu (vendas/financeiro) executa **sequencial**. |
| `src/lib/utils/permissions.ts` | 🔶 RH adiciona `isResponsavelRH/isTutor/isInstrutor/canSeeNiveis/isProprioRegistro` + amplia `canEdit(rh)` por responsabilidade **sem alterar `EDIT_RULES.rh`**. Sequencial com qualquer feature que toque o arquivo. Estoque/produção/config já concluídos — sem conflito ativo. |
| `src/lib/api/rh.ts` | 🔶 Dono atual: estoque (importa `searchPeople`). RH migra para `api/rh/*.ts`; manter re-export até estoque migrar o import (task rh-002 cobre; estoque não é reaberto aqui). |
| `src/routes/(app)/dashboard/components/DonutChart.svelte` | ⛔ Dono: dashboard — **não tocar**; RH cria o padrão equivalente no UI kit. |
| `docs/**` | ⛔ **Congelado**. Dívidas 4/6 e naming ficam registradas aqui (§9), nunca em `docs/contract.gap.md`. |

---

## 6. Vocabulário normativo (Dívida 6 — Tutor × Instrutor)

Definitivo (backend `contract.gap.md` §5.1 + requirements §5.1 + mockup §5): **Tutor(a)** acompanha **grupo/processo-seletivo** (dinâmica, evolução de níveis); **Instrutor(a)** **ministra o treinamento** (sessão, avaliação, tarefa final). Uma pessoa pode ser ambos; os papéis **nunca se sobrepõem**.

- Contratos de PS/grupos usam `tutor`/`Tutor(a)` (responsável de grupo, `leaderId`).
- Contratos de treinamentos usam `instrutor`/`Instrutor(a)` (sessão, avaliação).
- Disponibilidade do instrutor reflete **só** tarefas de produção/encomendas/projetos — **nunca** triagens do PS (triagem é do tutor).
- Tela 4 (novo): checkbox **"É instrutor(a)?"** → qualificação 0–6 (autoridade de equipamento). Tela 3 (editar): mesmo campo (`is-instrutor`).

---

## 7. Decisões de enxugamento (código mínimo)

| # | Decisão | Detalhe |
|---|---|---|
| **R-1** | **REUSADO** `format.ts`, `errors.ts`, `auth`/`toast` stores, UI kit (22) | Sem `rh-format.ts`, sem store RH, sem componentes de estado por tela. `initials`/`relativeTime`/`formatNumber`/`toUserMessage` já cobrem RH. |
| **R-2** | API nova sobre `apiFetch`; `api/rh.ts` vira re-export fino | Remove acoplamento `stock/request`; preserva `searchPeople` para estoque até migração. 5 módulos pequenos (pessoas/PS/horas/treinamentos/níveis) em vez de 1 gigante — espelha domínios do mockup; cada um = 1 task. |
| **R-3** | **REUSADO** `PeoplePicker` (estoque) | Modais "Registrar horas" (pessoa) e "Criar grupo" (líder/membros) usam o search-combo existente, não um novo. |
| **R-4** | Só 3 componentes UI novos (`TypeBadge`, `DonutChart`, `KanbanBoard`) | Todo o resto (filtros, tabela, skeleton, empty, error, bulk, modais, tabs, chips) é composição do kit. `DonutChart` promove o padrão do dashboard; `KanbanBoard` sem drag-lib (botões movem etapa). |
| **R-5** | Horas: **extrato mensal servido pelo backend, nunca somado no client** (Dívida 4) | KPIs "validadas no mês/média" renderizam `ExtratoMensalHorasEvent` (`GET /horas/disponiveis`) quando disponível; fallback = counts da lista. Nenhuma soma/derivação de totais no client; após validar/rejeitar → refetch. |
| **R-6** | Treinamentos: sem mock local, sem banner "contrato pendente" | Padrão **configurações**: endpoints 🔴 chamados direto; falha → estado Error amigável por bloco (estado #6). Sem `USE_RH_MOCK`. |
| **R-7** | **NÃO planejado: telas de certificados** | Requirements §Data lista 5 endpoints (Dívida 4, v0.2.0 backend); mockup §10 declara fluxo de certificados **fora de escopo MVP** e não há section/testid. Planejar telas agora = especulativo. Registrado como D-6 na fila backend/PO. |
| **R-8** | **NÃO planejado**: export CSV de pessoas, importação em massa, ajuste de horas por Admin, edição de credenciais, entrevista agendada/remota, painel do candidato, conteúdo real PDF/MDX dos guias | Tudo §10 fora de escopo ou sem endpoint. Bulk da lista limita-se a Alterar nível + Excluir (com `ConfirmDialog`); guias = metadados + `modal-guia`. |
| **R-9** | Guarda em **1** `pessoas/+layout.ts` (+ checagem Admin para `/niveis`) | `canView(rh)` no layout; `canEdit` via contexto; Estagiário/Recrutando "próprio registro" resolvido nas páginas (redirect para `/pessoas/{ownId}` quando aplicável — D-7). Sem `+page.ts` separado por rota além do par mínimo (SPA, padrão dashboard). |
| **R-10** | Second sidebar de Treinamentos = `treinamentos/+layout.svelte` | Painel `w-60` sobre a principal, ativo derivado de `$page.url.pathname`, fecha fora de Treinamentos (mockup §4.4). Sem lib, sem store de UI. |
| **R-11** | Naming PT nos paths novos (`/pessoas`, `/apontamentos-horas`) com fallback EN documentado | Paths novos usam PT (requirements + decisão Dívida 2); `searchPeople` legado mantido até backend alinhar (D-1). Nenhum path `/stock/` ou `/rh/people` novo. |

**Explicitamente NÃO planejado** (por não resolver caso real): telas `/certificados`; soma de horas/extrato no client; CSV de pessoas; importação em massa; credenciais na edição; entrevista remota; painel do candidato; PDF/MDX real; `pageSize` na URL fora da spec; notificações de aprovação (dono: Notification Service); filtros `pageSize`/`sort` além de `?search=&setor=&nivel=&status=&page=`.

---

## 8. Ordem em Blocos

- **Bloco 1 — Fundação** (rh-001..rh-011): types → 5 APIs → `rh-status` → 3 UI (`TypeBadge`, `DonutChart`, `KanbanBoard`) → `permissions.ts` helpers.
- **Bloco 2 — Pessoas** (rh-012..rh-016): guarda `pessoas/+layout.ts` → lista → detalhe (tabs) → novo → editar.
- **Bloco 3 — Seleção + Horas** (rh-017..rh-018): Kanban PS (grupos + avaliação individual, rótulo Tutor) → registro-horas (tabs + validar/rejeitar + extrato backend).
- **Bloco 4 — Treinamentos** (rh-019..rh-024): `treinamentos/+layout.svelte` (second sidebar) → overview (donuts) → detalhe → agenda (+disponibilidade) → documentação (guias) → sessão (instrutor).
- **Bloco 5 — Níveis + Menu** (rh-025..rh-026): `niveis` Admin-only → `menu.ts` submenu (depende de todas as rotas existirem).
- **Bloco 6 — Testes** (rh-027..rh-030): status → api mock → permissions/RBAC → páginas (states/guards); e2e **skip** (sem Playwright).

Dependências: **B1 → B2 → {B3, B4} → B5 → B6** (B3/B4 paralelizáveis entre si após B2; B6 parcial desde B1 para `rh-status`).

---

## 9. Checklists obrigatórias

### Validator §5 (todas as tasks de tela/API)
- [ ] Svelte 5 runes (`$props/$state/$derived/$effect`), sem `export let`;
- [ ] TS estrito (sem `any`; `import type` para tipos); unions literais de status (`PersonStatus/PsStage/HoraStatus/TrainingStatus/GuideStatus/Nivel`);
- [ ] A11y: `aria-*` nos controles, labels em inputs, `role="tablist"`/`aria-selected` nas tabs, `role="dialog"`+Escape+backdrop nos modais, foco visível; **RBAC = HIDE, nunca disable**;
- [ ] URL como fonte de verdade (`?search=&setor=&nivel=&status=&page=`, `?status=&periodo=`, `?tab=`, `?estagio=`) via `goto`+refetch; "Limpar filtros" volta à URL base; debounce 300ms + `AbortController` nas buscas;
- [ ] i18n pt-BR 100%; rótulos **"Tutor(a)"** (PS/grupos) vs **"Instrutor(a)"** (treinamentos) nunca trocados; datas `dd/mm/yyyy` via `Intl`, matrícula `font-mono`;
- [ ] Estados sem layout shift: Loading (skeleton na geometria), Success, Empty, Empty filtrado (chips+limpar), Error (banner+retry), RBAC-restrito (hide + banner em edição), erro parcial por bloco/tab (estado #6);
- [ ] Após mutação: refetch da página atual (ou última página válida ao excluir); `409` email duplicado inline; `data-testid` kebab-case espelhando `full.html` (`person-*`, `training-detail-*`, `edit-person`, `ps-*`, `hora-*`, `agenda-*`, `guide-*`, `sessao-*`, `nivel-*`).

### Security §6 (idem)
- [ ] Bearer via `apiFetch` (helper `bearer()` local por módulo api); 401 → logout+goto sanitizado (global); sem credenciais hardcoded; sem `console.log`;
- [ ] Guardas client-side = **defesa em profundidade** (autoridade é o backend — D-3/D-5): `pessoas/+layout.ts` (`canView`), `niveis` + sessão (`isAdmin`/`isInstrutor`), ações `hide` por `canEdit`/responsável/tutor/instrutor;
- [ ] 403 → mensagem "sem permissão" na tela (sem crash); 404 pessoa/treinamento → `EmptyState` + voltar;
- [ ] Sem `{@html}`/injeção; `link?`/URL de guia só em `<a>` após validar scheme `http(s)` ou path interno `/`;
- [ ] CSV/bulk sem injeção (sem export de pessoas neste escopo); `reason` de rejeição renderizado como texto;
- [ ] e2e sem credenciais hardcoded (sem Playwright → **skip** documentado).

---

## 10. Riscos de contrato (endpoints)

| Endpoint | Método | Classe | Status |
|---|---|---|---|
| `GET /api/rh/pessoas?search=&setor=&nivel=&status=&page=` | GET | 🔴 | Só `GET /pessoas/{id}` existe no backend (contract.gap §3) |
| `GET /api/rh/pessoas/{id}` (+ abas horas/treinamentos/histórico) | GET | 🟡 | Existe `GET /pessoas/{id}`; abas assumidas (D-2) |
| `POST /api/rh/pessoas` (+ `POST /api/auth/users` credencial) | POST | 🟡 | Existe `POST /pessoas`; vínculo credencial a confirmar (D-2) |
| `PUT /api/rh/pessoas/{id}` | PUT | 🟡 | Existe `PUT /pessoas/{id}`; payload PT a alinhar (D-1) |
| `POST /api/rh/processo-seletivo/grupos` · `PATCH …/{id}/estagio` · `POST …/{id}/membros/{pessoaId}/avaliar` · `GET …?estagio=` | POST/PATCH/GET | 🔴 | Inexistentes (há `AvaliacaoRequest` sem rota; PS sem listagem) |
| `GET /api/rh/horas?status=&periodo=` · `POST /api/rh/horas` · `PATCH …/{id}/validar\|rejeitar` | GET/POST/PATCH | 🔴 legado | Modelo substituído: `POST /apontamentos-horas` + `PUT /{id}/validar` + `GET /horas/disponiveis` + `ExtratoMensalHorasEvent` 🟡 (Dívida 4) |
| `GET /api/rh/treinamentos` · `GET …/{id}` · `GET …/{id}/sessao` · `POST …/{id}/avaliar` · `POST …/{id}/tarefa-final` · agenda/guias/disponibilidade | GET/POST | 🔴 | Inexistentes (só `/{id}/avaliacoes` GET/POST existe) |
| `GET /api/rh/niveis` · `PATCH /api/rh/niveis/{id}/membros` | GET/PATCH | 🔴 parcial | Só `PUT /funcionarios/{id}/nivel` existe; sem lista de níveis |
| `GET /api/projects?search=` · `GET /api/production/machines` · `GET /api/production/tasks` | GET | 🔴 | Services vazios (produção antes de RH-treinamentos) |
| Certificados (`POST /solicitar`, `GET /solicitacoes`, `PUT …/aprovar\|rejeitar`, `GET /emitidos`) | — | 🟡 futuro | v0.2.0 backend; **sem telas neste plano** (R-7/D-6) |

> Falha de contrato → estado **Error** amigável por página/bloco (padrão configurações, sem mock). Nada bloqueia o frontend além do backend.

### 10.1 Decisões que exigem backend/PO (fila — padrão D-x, sem inventar endpoint)
- **D-1 🟡** — Naming PT vs EN: confirmar `pessoas` (não `people`), `apontamentos-horas` (não `horas`), `grupos`, `niveis`; manter `searchPeople` legado até alinhar.
- **D-2 🟡** — Abas do detalhe (`horas?personId=&status=`, `trainings`, `level-history`): confirmar paths ou expor agregados; confirmar vínculo `POST /pessoas` × `POST /api/auth/users`.
- **D-3 🔴** — Enforcement server-side de RBAC (`@PreAuthorize` por recurso; Admin-only em níveis; responsável/tutor/instrutor) — guards aqui são depth-only. Inclui regra "Estagiário vê só o próprio".
- **D-4 🔴** — Processo seletivo: expor listagem/grupos/mover-etapa/avaliar-individual; confirmar stage machine (`inscrito/triagem/entrevista/aprovado/reprovado`).
- **D-5 🔴** — Horas (Dívida 4): confirmar `GET /horas/disponiveis` + `ExtratoMensalHorasEvent` como fonte dos KPIs; confirmar `PUT /apontamentos-horas/{id}/validar` (+ rejeitar com `reason`); confirmar filtro `?status=&periodo=`.
- **D-6 🟡** — Certificados (Dívida 4): confirmar v0.2.0 antes de qualquer tela; mockup futuro pelo Designer.
- **D-7 🟡** — "Próprio registro": mapear token não-admin → pessoa (`id_user`→`funcionario`; contract.gap §8.6 degrada a vazio hoje) para Estagiário/Recrutando.
- **D-8 🟡** — Níveis: expor matriz (`GET /niveis`) e troca (`PATCH …/membros` vs `PUT /funcionarios/{id}/nivel`); matriz é autoritativa, nunca calculada no client.
- **D-9 🟡** — Treinamentos: expor lista/detalhe/sessão/avaliar/tarefa-final/agenda/guias/disponibilidade; confirmar `instrutor` vs `tutor` nos DTOs (§6); confirmar integrações projects/machines/tasks.
- **D-10 🟡** — Mensagens de erro não-técnicas (`code` estruturado; 409/422 inline) — ressalva transversal.

---

## 11. Critérios de Entrega (DoD da feature)
- [ ] 12 telas `/pessoas/**` funcionais sobre API real (SPA), URL state deep-linkável, `data-testid` espelhando `full.html` (+ variantes de `novasTelasEstados.html` nas telas 3 e 8).
- [ ] Kanban PS por grupos + avaliação individual (rótulo **Tutor**); registro-horas com extrato do backend (zero soma client); treinamentos com papel **Instrutor** + second sidebar; níveis Admin-only (guard + hide menu).
- [ ] RBAC hide-real: `!canEdit` omite criação/edição/validação/checkboxes do DOM; Estagiário vê próprio registro; Recrutando sem gestão; `EDIT_RULES`/`VIEW_RULES` intactos.
- [ ] API 100% PT nova (`api/rh/*.ts` via `apiFetch`); nenhum path `/rh/people` ou `/stock/` novo; `api/rh.ts` legado com re-export.
- [ ] `menu.ts` com submenu RH (Níveis `canSee: isAdmin`); `permissions.ts` só adiciona helpers.
- [ ] Testes unit verdes (`npm test`); `npm run check` sem erros novos; `npm run build` ok; e2e documentado como **skip**.
- [ ] Nenhuma alteração em `docs/**`; endpoints 🟡/🔴 reportados (nunca silenciados); certificados **não** implementados.

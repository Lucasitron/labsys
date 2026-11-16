# Plan — Notificações (Frontend)

> Feature: `notificacoes` (frontend) · Branch: `dev/frontend` · Gerado por: Planner (despacho 21/09/2026)
> Fonte canônica: `.opencode/specs/frontend/features/notificacoes/requirements.md`
> Mockups: `docs/mokups/notificacoes/` (`full.html` + `instrunctions.md` + `requisitos.md`)
> Escopo: SÓ frontend. Nenhuma alteração em `docs/**`; NENHUM endpoint inventado (só os 7 da spec);
> princípio **código mínimo** aplicado (§ 7): reuso > create, sem código especulativo.

---

## 1. Contexto e descobertas de estado

O **contador da topbar já existe e funciona**: `stores/notifications.ts` (polling 60s + pausa em
`document.hidden` + `visibilitychange`) e `api/dashboard.ts#fetchUnreadCount` →
`GET /notifications/unread/count` — criados pela feature **dashboard** e já sob teste
(`tests/unit/notifications.test.ts`, 8 casos). O **item de menu** "Notificações" já aponta para
`/notificacoes` (`config/menu.ts`), e `NotificationsButton.svelte` já navega para `/notificacoes`.

**A feature central NÃO existe**: não há rota `/(app)/notificacoes/**`, não há tipos, API layer de
lista/read/read-all/preferências/histórico, nem util de formatação por tipo.

A estratégia é **reusar o que o dashboard já entregou** e construir só o que resolve caso real do
mockup: a lista central com 5 estados + URL state, e as 2 telas Admin (Pref/Câmaras de canais e
Histórico). O **dropdown da topbar permanece no escopo do mockup do dashboard** (não desta feature —
`requisitos.md` §2 e instrução do dispatch).

---

## 2. Estado real arquivo-a-arquivo (frontend)

### 2.1 Rotas (`src/routes/(app)/`)
| Arquivo | Estado | Nota |
|---|---|---|
| `+layout.ts` | ✅ OK | Guarda de autenticação global (`(app)`): `fetchMe` + redirect `/auth/login`. Não tocar. |
| `+layout.svelte` | ✅ OK | Já executa `startPolling()`/`stopPolling()` do store de notificações. Não tocar. |
| `dashboard/` | ✅ OK | `+page.svelte`/`+page.ts` com refetch em `visibilitychange`. Consumidor do contador. Não tocar. |
| `notificacoes/` | 🔴 **G-1** | **Não existe.** `menu.ts` e `NotificationsButton.svelte` apontam para `/notificacoes` — hoje cai numa rota inexistente (404/Crumb "Notificacoes" genérico via `beautify`). |
| `configuracoes/`, `estoque/`, `producao/` | ✅ OK | Padrões de `+layout.ts`/`+page.ts`/componentes por rota — referência de estilo. |

### 2.2 API (`src/lib/api/`)
| Arquivo | Estado | Nota |
|---|---|---|
| `dashboard.ts` | ✅ OK | `fetchUnreadCount` → `/notifications/unread/count` (linhas 15-17). **REUTILIZAR** — sem duplicar (R-1). |
| `client.ts` | ✅ OK | `apiFetch` (timeout 15s, Bearer via headers, 401→logout+goto sanitizado, `ApiError/NetworkError`). Base de toda nova API. |
| `configuracoes/*`, `stock/*`, `producao/*`, `auth.ts`, `rh.ts` | ✅ OK | Padrão `bearer()` local. Não tocar. |
| `notifications/` | 🔴 **G-2** | Inexistente. List/read/read-all/pcs/history nunca consumidos; contratos do requirements §Data nunca tocados. |

### 2.3 Tipos e utils
| Arquivo | Estado | Nota |
|---|---|---|
| `types/dashboard.ts` | ✅ OK | `UnreadCount { count: number }` — **contrato real já em uso** pelo store e testes. Divergência com `{ value }` do mockup → manter `{ count }` (R-7). |
| `types/notifications.ts` | 🔴 **G-3** | Inexistente. Criar `NotificationType/Channel/Notification/PagedNotifications/NotificationPreferences/HistoryFilters` (sem redefinir `UnreadCount`). |
| `utils/format.ts` | ✅ OK | `relativeTime()` pt-BR já existe (linhas 30-42). **REUTILIZAR** no heartbeat e linhas da lista (R-2). |
| `utils/notification-format.ts` | 🔴 **G-4** | Inexistente. Criar **apenas** `TYPE_META` (label PT + `IconName` + tone por `NotificationType`) e badge de canal (`CHANNEL_BADGE`). Nada de relativo duplicado. |
| `utils/errors.ts`, `utils/permissions.ts` | ✅ OK | `isAdmin` já existe em `permissions.ts:106`. `toUserMessage` já cobre Network/5xx/UNKNOWN. **Não tocar permissions.ts** (não é dono desta feature). |
| `components/ui/Icon.svelte` | 🟡 **G-5** | `IconName` **não tem `bell`** (usado no mockup para tipo `sistema` e empty states). Adicionar 1 entrada. |

### 2.4 Componentes
| Arquivo | Estado | Nota |
|---|---|---|
| `components/layout/NotificationsButton.svelte` | ✅ OK | Badge `{#if count > 0}` (hide, não disabled — conforme requisito) + `goto('/notificacoes')`. **Dropdown fora de escopo** (mockup dashboard). Não tocar. |
| `components/layout/Sidebar.svelte` | ✅ OK | Renderiza `menuItems` + `children` filtrados por `canSee`. Nenhuma mudança: adicionar children no `menu.ts` já habilita o submenu. |
| `components/ui/*` (22) | ✅ OK | `Skeleton`, `EmptyState`, `ErrorBanner`, `Tabs`, `Select`, `Chip`, `Pagination`, `PageHeader`, `StatusBadge`, `SearchInput` **reutilizáveis** — NÃO recriar (R-6). |

### 2.5 Config
| Arquivo | Estado | Nota |
|---|---|---|
| `config/menu.ts` | 🟡 **G-6** | Item plano `Notificações` já existe (linha 64, `module:'notificacoes'`, sem `children`). Adicionar submenu: `Minhas notificações` + (Admin-only) `Preferências de canais` / `Histórico`. |

### 2.6 Testes
- `tests/unit/notifications.test.ts` — cobre **só o store de polling** (8 casos, já verdes).
- Nenhum teste de API/format/página/prefs/history de notificações → **G-9**.
- Infra: `vitest` + `jsdom` + `@testing-library/svelte` (sem Playwright → e2e **skip**, mesmo padrão das features anteriores).

---

## 3. Gaps (G-1 … G-9)

| Gap | Descrição | Tipo/Escopo |
|---|---|---|
| **G-1** | Rota `/(app)/notificacoes/**` inexistente (menu e bell já apontam para ela). | Frontend |
| **G-2** | API layer das 6 operações restantes (`list`, `markRead`, `markAllRead`, `get/savePreferences`, `history`) inexistente; `unread/count` já vive em `dashboard.ts` (reuso). | Frontend + contrato 🟡 |
| **G-3** | Tipos do módulo inexistentes (`types/notifications.ts`). | Tipos |
| **G-4** | Util de identidade visual por tipo/canal inexistente (`notification-format.ts`). | Frontend |
| **G-5** | `IconName` sem `bell` (tipo `sistema`/empty do mockup). | Frontend (UI kit) |
| **G-6** | `menu.ts` sem submenu Admin-gated para Pref/Histórico (RBAC hide-não-disable exige `canSee: isAdmin`). | Frontend + RBAC |
| **G-7** | Lista central: 5 estados (Loading/Success/Empty/Empty-filtrado/Error), URL state `?page&type&status`, heartbeat, paginação 10/página, ações otimistas `PATCH {id}/read` e `POST read-all` (botão HIDE quando 0 não lidas). | Frontend |
| **G-8** | Telas Admin: Preferências (matriz canal×tipo, dirty/save/reset padrão) + Histórico (tabela, filtros search/type/status/channel/period, paginação) + guarda única central. | Frontend + RBAC |
| **G-9** | Zero testes unit do módulo (api/format/página/prefs/history). | Testes |

---

## 4. Camadas/arquivos a criar ou editar

**Create:**
- `src/lib/types/notifications.ts` (G-3)
- `src/lib/api/notifications.ts` (G-2) — arquivo único, padrão `dashboard.ts`
- `src/lib/utils/notification-format.ts` (G-4)
- `src/routes/(app)/notificacoes/+layout.ts` (guarda central G-8)
- `src/routes/(app)/notificacoes/+page.svelte` (G-1/G-7)
- `src/routes/(app)/notificacoes/components/NotificationRow.svelte` (G-7)
- `src/routes/(app)/notificacoes/preferencias/+page.svelte` (G-8)
- `src/routes/(app)/notificacoes/historico/+page.svelte` (G-8)
- Testes: `tests/unit/notifications-api.test.ts`, `notifications-format.test.ts`, `notifications-page.test.ts`, `notifications-prefs.test.ts`, `notifications-history.test.ts` (G-9)

**Modify:**
- `src/lib/config/menu.ts` (G-6 — adicionar `children`; item já existe)
- `src/lib/components/ui/Icon.svelte` (G-5 — adicionar `bell`)

**Não criar** (enxugamento, ver § 7): novos *stores*, `api/notifications/unread-count.ts`,
`components/notificacoes/Notification{Empty,Error,Filters,Skeleton}.svelte`, dropdown da topbar.

---

## 5. ConflictMap (arquivos compartilhados)

| Arquivo | Dono/Impacto |
|---|---|
| `src/lib/config/menu.ts` | 🟢 **Livre agora** (configuracoes C1/C2 liberados 21/09). Notificações adiciona `children` do item existente. Se a próxima feature (rh/vendas/financeiro) tocar menu, execução **sequencial** — regra orquestração mantida. |
| `src/lib/utils/permissions.ts` | 🟢 **Livres/não tocado**: `isAdmin` já existe e é suficiente. Esta feature NÃO edita `permissions.ts`. |
| `src/lib/components/ui/Icon.svelte` | 🔶 Adição de 1 `IconName` (`bell`) é compatível (sem remover nada); consumidores atuais intactos. |
| `src/lib/stores/notifications.ts` | ⛔ **Dono: dashboard** — REUSAR como está; NÃO alterar (heartbeat é resolvido na página, R-4). |
| `src/lib/api/dashboard.ts` | 🟡 `fetchUnreadCount` reutilizado por import; sem edição (R-1). |
| `src/lib/components/layout/NotificationsButton.svelte` | ⛔ Fora de escopo (dropdown = dashboard). Não tocar. |
| `src/lib/types/dashboard.ts` | ⛔ `UnreadCount { count }` permanece (R-7). Não tocar. |
| `docs/**` | ⛔ **Congelado**. Nenhum endpoint/rote decision materializado em `docs/contract.gap.md`. |

---

## 6. Decisões de enxugamento (código mínimo)

| # | Decisão | Detalhe |
|---|---|---|
| **R-1** | **REUSADO** store `stores/notifications.ts` + `fetchUnreadCount` (`api/dashboard.ts`) | Sem `api/notifications/unread-count.ts`, sem novo store. O store já faz polling 60s/hidden/visibility e tem 8 testes. |
| **R-2** | **REUSADO** `relativeTime()` de `utils/format.ts` | `notification-format.ts` só carrega `TYPE_META` e `CHANNEL_BADGE` (mapas tipados); NÃO duplica relativo. |
| **R-3** | **REUSADO** UI kit (`Skeleton`, `EmptyState`, `ErrorBanner`, `Tabs`, `Select`, `Chip`, `Pagination`, `PageHeader`) | Não criar `NotificationEmpty/NotificationError/NotificationSkeleton/NotificationFilters` do instrunctions — estados são montados na página com os componentes compartilhados (EmptyState aceita `children` p/ chips+CTA; ErrorBanner já tem retry). |
| **R-4** | Sem mock local / banner "contrato pendente" | Padrão **configuracoes** (não producao): endpoints 🟡 chamados direto; o próprio estado **Error** da página cobre falha. Sem `USE_*_MOCK`. |
| **R-5** | API em **arquivo único** `src/lib/api/notifications.ts` | Padrão `dashboard.ts` (3+ funções por arquivo), não os 6 arquivos propostos pelo instrunctions. `1 arquivo = 1 task`. |
| **R-6** | Apenas `NotificationRow.svelte` é componente novo | UI de linha é única (ícone por tipo + badge Não lida + meta + ação). Skeleton/Empty/Error/Filters são composição do UI kit. |
| **R-7** | Contrato `UnreadCount` mantém **`{ count }`** (não `{ value }` do mockup) | O que o store/tests reais consomem. Divergência documentada e devolvida ao backend (D-1). |
| **R-8** | Dropdown topbar **fora de escopo** | `requisitos.md` §2: entregue pelo mockup do dashboard. "Ver todas" já navega para `/notificacoes`. Nada a implementar/a mudar. |
| **R-9** | Paginação com `Pagination.svelte` (pageSize 10, página padrão) | Seletor "10 por página" = comodidade do UI kit; sem persistência extra de pageSize na URL (URL state da spec é `?page&type&status`). |
| **R-10** | **NÃO planejado**: botão "Exportar CSV" do Histórico | Nenhum endpoint de export existe na spec; inventar = violação. Devolvido à fila PO/backend. |
| **R-11** | Guarda Admin centralizada em **1** `notificacoes/+layout.ts` | Verifica `url.pathname` (`/preferencias` ou `/historico`) e redireciona 303 para `/notificacoes` se `!isAdmin`. Menos 1 arquivo que o instrunctions (que propunha 2 layouts). |
| **R-12** | Não criar `+page.ts` nas 3 rotas | SPA (ssr=false), dado opaco e falsamente-frágil; fetch no componente com refetch (padrão dashboard). Contrato 🟡. |

**O que foi explicitamente NÃO planejado** (por não resolver caso real):
- Dropdown topbar / "Ver todas" dentro do dropdown (R-8, mockup dashboard).
- Exportar histórico CSV (R-10, sem endpoint).
- Filtro `pessoa`/busca por destinatário autocomplete (sem contracto; mockup usa input livre).
- `pageSize` na URL (spec não lista; só `?page&type&status`).
- Preferências por **usuário** (matriz é global do sistema — `requisitos.md` §6 Admin).
- Notificação com `link?` para navegação profunda — se o contrato entregar `link`, renderizar como `<a>` validate scheme http(s). **Nada antecipado** até o backend confirmar o campo (D-6).

---

## 7. Ordem em Blocos

- **Bloco 1 — Fundação** (tipos→api→util→ícone): `types/notifications.ts`, `api/notifications.ts` 🟡, `utils/notification-format.ts`, `ui/Icon.svelte` (bell).
- **Bloco 2 — Conte (lista central)**: guarda `+layout.ts` (R-11), `components/NotificationRow.svelte`, `+page.svelte` (5 estados + URL state + heartbeat + paginação + ações otimistas).
- **Bloco 3 — Admin**: `preferencias/+page.svelte`, `historico/+page.svelte`.
- **Bloco 4 — Espalhamento**: `config/menu.ts` (submenu Admin-gated).
- **Bloco 5 — Testes**: api, format, página (com fake timers p/ heartbeat e polling), prefs, history; e2e **skip** (sem Playwright).

Dependencies: **B1 → B2 → B3 → B4**; **B5 paralelo parcial** (teste de format desde B1). `menu.ts`/`Icon.svelte` são os únicos arquivos compartilhados tocados — sequencial e pequeno.

---

## 8. Checklists obrigatórias

### Validator §5 (aplicar em todas as tasks)
- [ ] Svelte 5 runes (`$props`, `$state`, `$derived`, `$effect`), sem `export let`;
- [ ] Tipagem estrita TS (sem `any`; `import type` em tipos); `NotificationType`/`NotificationChannel` como unions literais;
- [ ] A11y: `aria-label` nos controles; `role="tablist"`/`aria-selected` (Tabs); labels nos selects; ímã de label nos toggles; **RBAC = HIDE, nunca disabled** (children do menu e páginas Admin só renderizam se `isAdmin`);
- [ ] URL como fonte de verdade: `?page&type&status` lido de `$page.url.searchParams`; mudança → `goto(keepFocus)` + refetch; "Limpar filtros" → `goto('/notificacoes')`;
- [ ] i18n pt-BR 100% (textos e erros); badges de tipo com **ícone + texto** (cor sozinha nunca expressa sentido);
- [ ] Estados sem layout shift (skeleton na mesma geometria); heartbeat silencioso sem toast;
- [ ] Ações otimistas: `PATCH {id}/read` e `POST read-all` — remove badge/bg imediatamente e **reverte em falha** com toast (`toast.ts`); botão "marcar todas" desaparece (hide) quando não há não lidas;
- [ ] Paginação usa `Pagination.svelte`; rótulo "X notificações · página Y de Z" conforme mockup.

### Security §6 (idem)
- [ ] Bearer via `apiFetch` de `client.ts` (padrão `bearer()` local); 401 → logout+`goto` sanitizado já global; sem credenciais hardcoded; sem `console.log`;
- [ ] Guarda de rota Admin client-side **é defesa em profundidade apenas** — registrar D-3 (enforcement `@PreAuthorize` obrigatório no backend);
- [ ] Sem `{@html}`/injeção; `link?` (se chegar no contrato) só renderiza se scheme `http(s)` e relative interno (validar antes de `<a href>`) — D-6;
- [ ] Parametrização de query **sem inventar**: enviar `page/pageSize/type/read` na lista e `search/type/read/channel/period` no history **somente quando o backend confirmar** (D-4); `status=unread → read=false`, `all → omitido`;
- [ ] Texto de "Restrito ao Admin" em Preferências com ícone + texto (acessibilidade, nunca cor sozinha);
- [ ] e2e sem credenciais hardcoded (sem Playwright instalado → **skip** documentado).

---

## 9. Riscos de contrato (endpoints)

| Endpoint | Método | Classificação | Status |
|---|---|---|---|
| `GET /api/notifications/unread/count` | GET | ✅ **Em uso** (dashboard.ts + store + 8 testes) | contrato real em campo |
| `GET /api/notifications?page&pageSize&type&read` | GET | 🟡 **assumido** | sem evidência de backend/gateway |
| `PATCH /api/notifications/{id}/read` | PATCH | 🟡 **assumido** | idem |
| `POST /api/notifications/read-all` | POST | 🟡 **assumido** | idem |
| `GET/PUT /api/notifications/preferences` | GET/PUT | 🟡 **assumido** | idem |
| `GET /api/notifications/history` | GET | 🟡 **assumido** | idem |

> **Risco** (consistente com dashboard/security nota M1 e configuracoes M1): se o `notifications-service`
> não existir ou não estiver exposto no gateway, as 3 telas exibem estado **Error** amigável (sem crash),
> o contador da topbar mantém `0` (store falha silencioso) — **não bloqueia o frontend**. Endpoints marcados
> 🟡 em `tasks.md` e reportados na fila backend/PO.

### 9.1 Decisões que exigem backend/PO (fila)
- **D-1 🟡** — confirmar shape `UnreadCount` (`{ count }` em uso vs `{ value }` no mockup) e shape do
  paginado (`items/total/page/pageSize/totalPages` vs LazyPage backend).
- **D-2 🟡** — `GET /notifications` deve retornar **apenas** notificações do usuário do JWT (isolamento).
- **D-3 🔴** — `GET/PUT /preferences` e `GET /history` exigem `@PreAuthorize` Admin server-side
  (estende M1/M2 já reportados em configurações; consultar `rbac-matrix.md`).
- **D-4 🟡** — Confirmar aceite de `search/type/read/channel/period` em `GET /history` (não inventar params).
  **Nota de implementação (21/09, Validator M2):** `GET /history` deve retornar a **lista completa** (sem paginação server-side) OU o contrato precisa expor `page/pageSize`; a tela hoje pagina **client-side** (10/pág.) sobre o retorno integral. Se o backend paginar por padrão, a tela exibirá dados parciais — migrar para paginação server-side quando D-4 for confirmado.
- **D-7 🟡** — `history()` e demais respostas tipadas como `HistoryPayload { items, total }` (shape assumido; alinhar com backend).
- **D-5 🟡** — mensagens de erro não técnicas (`code` estruturado) — ressalva transversal já em fila.
- **D-6 🟡** — campo `link?` da notificação: relativo a qual rota interna? Validação de scheme no client.

---

## 10. Critérios de Entrega (DoD da feature)
- [ ] Rota `/(app)/notificacoes` + `/preferencias` + `/historico` funcionais sobre API real (SPA).
- [ ] Lista central com 5 estados (skeleton/rows/empty/empty-filtrado/error+retry), URL state `?page&type&status` deep-linkável, heartbeat "Atualizado · {tempo}", paginação 10.
- [ ] Ações otimistas `PATCH {id}/read` e `POST read-all` com reversão+toast em falha; botão "marcar todas" **hide** quando 0 não lidas.
- [ ] Guarda central: não-Admin em `/preferencias|/historico` → `redirect(303, '/notificacoes')`; menu admin children **omitidos do DOM** para `!isAdmin`.
- [ ] Preferências: matriz canal×tipo, dirty→salvar habilitado, "Restaurar padrão" (todos ON exceto push de `sistema`), pill "Restrito ao Admin" com ícone+texto.
- [ ] Histórico: tabela (Notificação/Tipo/Destinatário/Canal/Status/Enviada) + filtros + paginação; empty filtrado com chips.
- [ ] Sem novos componentes de estado (`Notification{Skeleton,Empty,Error,Filters}` não criados); `UnreadCount` mantém `{ count }`; `menu.ts` tocado, `permissions.ts` **não** tocado.
- [ ] Testes unit dos 5 arquivos verdes (`npm test`); `npm run check` sem **erros novos** (baseline 0/16/7 esperado); `npm run build` ok.
- [ ] Nenhuma alteração em `docs/**`; endpoints 🟡 reportados (nunca silenciados).
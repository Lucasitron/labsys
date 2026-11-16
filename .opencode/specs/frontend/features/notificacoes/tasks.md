# Tasks — Notificações (frontend, 3 telas)

> Conteúdo derivado de `.opencode/specs/frontend/features/notificacoes/{requirements,plan}.md`.
> Cada task = **1 arquivo real**. Execução **sequencial** por blocos (deps abaixo).
> Legenda: 🟡 contrato pendente (endpoint assumido da spec — sem evidência de backend).
> Princípio código mínimo: reuso > create (store/`fetchUnreadCount`/UI kit); nada especulativo.

---

## CRITÉRIO GLOBAL (todas as tasks)
- pt-BR; `aria-label`; `data-testid` conforme mockup (`requisitos.md` §8). Estados: Loading(skeleton) ·
  Success · Empty(CTA dashboard) · Empty filtrado(chips) · Error(banner+retry) · RBAC(HIDE).
- Tokens de `design-system.md` (surface/elevated/border/brand — fase `elevated #111a2c` canônico).
- Sem libs externas; Svelte 5 runes; tipagem estrita (sem `any`); sem `{@html}`.
- Guarda Admin: não-Admin em `/preferencias`/`/historico` → `redirect(303, '/notificacoes')`.
- URL como fonte de verdade: `?page&type&status` (status=unread→`read=false`, all→omitido).
- `UnreadCount` = `{ count }` (contrato real do dashboard; NÃO `{ value }` do mockup — D-1).
- Endpoints 🟡: chamar direto; falha → estado Error amigável (padrão configurações, sem mock/banner).

---

## B1 — Fundação (tipos + API + util + ícone)

### not-001 · types de notificações
- **Arquivo:** `src/lib/types/notifications.ts` — create
- **Conteúdo:** `NotificationType = 'encomenda'|'estoque'|'financeiro'|'producao'|'pessoas'|'sistema'`; `NotificationChannel = 'inapp'|'email'|'push'`; `Notification { id, type, title, body, read, createdAt, link? }`; `PagedNotifications<T> { items, total, page, pageSize, totalPages }`; `NotificationPreferences = Record<NotificationType, Record<NotificationChannel, boolean>>`; `HistoryFilters { search?, type?, read?, channel?, period? }`.
- **Saída:** tipos que alimentam API, página central, prefs e history.
- **DoD:** reflete mockup `requisitos.md` §4; **não redefinir** `UnreadCount` (reusa de `types/dashboard`); sem `any`.
- **Deps:** — · **Prioridade:** Alta

### not-002 · API de notificações
- **Arquivo:** `src/lib/api/notifications.ts` — create 🟡 (arquivo único, padrão `dashboard.ts`)
- **Conteúdo:** helper `bearer()` local; `listNotifications({page,pageSize,type,read}) → GET /api/notifications` 🟡 (monta query só com params não nulos) · `markRead(id) → PATCH /api/notifications/{id}/read` 🟡 · `markAllRead() → POST /api/notifications/read-all` 🟡 · `getPreferences() → GET /api/notifications/preferences` 🟡 · `savePreferences(p) → PUT /api/notifications/preferences` 🟡 · `history(filters) → GET /api/notifications/history` 🟡 (enviar `search/type/read/channel/period` **só** quando a spec backend confirmar — D-4; no build atual montar com os mesmos nomes e registrar no relatório).
  **NÃO** duplicar `fetchUnreadCount` — importar de `$lib/api/dashboard` (R-1).
- **Saída:** camada consumida pelas 3 telas + testes.
- **DoD:** usa `apiFetch` de `client.ts`; erros lançados como `ApiError`; contrato 🟡 reportado no relatório final do bloco.
- **Deps:** not-001 · **Prioridade:** Alta

### not-003 · util de formatação por tipo/canal
- **Arquivo:** `src/lib/utils/notification-format.ts` — create
- **Conteúdo:** `TYPE_META: Record<NotificationType, { label, icon: IconName, tone: Tone }>` (Encomenda→shopping-bag/brand · Estoque→cube/warn · Financeiro→chart/success · Produção→wrench/ink · Pessoas&RH→users/brand · Sistema→bell/muted); `CHANNEL_BADGE: Record<NotificationChannel, { label, tone }>` (In-app/brand · E-mail/success · Push/warn); `NOTIFICATION_TYPES` (ordem de exibição). **REUSAR** `relativeTime` de `$lib/utils/format` (não duplicar).
- **Saída:** identidade visual por linha, options do select de tipo e badges de canal.
- **DoD:** ícones existentes em `Icon.svelte` (após not-004 add `bell`); label sempre acompanha ícone.
- **Deps:** not-001, not-004 · **Prioridade:** Alta

### not-004 · ícone `bell` no UI kit
- **Arquivo:** `src/lib/components/ui/Icon.svelte` — modify
- **Conteúdo:** adicionar `'bell'` a `IconName` e ao `ICON_PATHS` (path heroicons sine/bell padrão usado no mockup linha 108).
- **Saída:** `Icon name="bell"` disponível (tipo `sistema` + empty states da lista).
- **DoD:** entrada nova sem quebrar `IconName` existente; type-check ok.
- **Deps:** — · **Prioridade:** Alta

---

## B2 — Lista central

### not-005 · guarda central
- **Arquivo:** `src/routes/(app)/notificacoes/+layout.ts` — create
- **Conteúdo:** reusa autenticação do `(app)/+layout.ts` (não duplicar fetchMe); lê `url.pathname`; se rota em `/notificacoes/preferencias` ou `/notificacoes/historico` e `!isAdmin(user)` → `redirect(303, '/notificacoes')`. `ssr=false`/`prerender=false`.
- **Saída:** proteção única das 2 páginas Admin (R-11 — menos 1 arquivo que instrunctions.
- **DoD:** não-Admin acessando área Admin é redirecionado (nunca vê 403 em tela). Nenhuma alteração em `permissions.ts`.
- **Deps:** — · **Prioridade:** Alta

### not-006 · row de notificação
- **Arquivo:** `src/routes/(app)/notificacoes/components/NotificationRow.svelte` — create
- **Conteúdo:** `$props { notification, onRead }`; layout do mockup: ícone por tipo (`bg-{tone}/15 border-{tone}/30`), título + badge "Não lida" (`bg-brand/15...)` só quando `!read` (borda `border-l-2 border-brand bg-brand/5` em não-lida), body, meta `{Label} · {relativeTime}`, ação "Marcar como lida" (hide quando lida, troca por tag "Lida" discreta). `data-testid="notification-row"` e `notification-mark-read`.
- **Saída:** linha reutilizada pela lista.
- **DoD:** a11y (`aria-label` na ação); `relativeTime` de `utils/format`; sem estado interno (presentação pura).
- **Deps:** not-001, not-003, not-004 · **Prioridade:** Média

### not-007 · página central
- **Arquivo:** `src/routes/(app)/notificacoes/+page.svelte` — create
- **Conteúdo:** feed da URL (`$page.url.searchParams` → `{page,type,status}`); fetch `listNotifications`; 5 estados (Skeleton ui em grid da row · rows com `NotificationRow` · `EmptyState` CTA "/dashboard" quando sem filtros · `EmptyState` + chips + "Limpar filtros" quando filtrado · `ErrorBanner` + retry sem rows); header com `PageHeader` + heartbeat "Atualizado · {relativeTime}" (timestamp local atualizado em sucesso de fetch/read/read-all e em mudança do store `unreadCount` — R-4) + botão "Marcar todas" (hide quando `unreadCount===0`); filtros com `Tabs` (Todas/Não lidas) + `Select` de tipo + "Limpar filtros"; `Pagination` (10/página). Mudança de filtro → `goto('/notificacoes?'+params, { keepFocus })` + refetch (URL = fonte de verdade). `data-testid` do mockup §8.
- **Saída:** rota `/(app)/notificacoes` (G-1/G-7).
- **DoD:** 5 estados corretos; URL deep-linkável; ações otimistas com reversão+toast em falha (`$lib/stores/toast`); sem layout shift; `svelte:head` título.
- **Deps:** not-002, not-005, not-006 · **Prioridade:** Alta

---

## B3 — Admin

### not-008 · preferências de canais
- **Arquivo:** `src/routes/(app)/notificacoes/preferencias/+page.svelte` — create
- **Conteúdo:** coluna 1 "Canais disponíveis" (in-app/e-mail/push com explicação) + matriz tipo×canal (3 colunas p/ desktop) com toggle `peer-checked` (`bg-brand` ativo), badge "2 de 3 canais" por linha, resumo "Restaurar padrão" (todos ON, push de `sistema` OFF) fica dirty, "Salvar preferências" habilita quando dirty → `savePreferences` → toast; falha → banner mantendo valores. Pill "Restrito ao Admin" (ícone+texto). `data-testid: notif-pref-list/toggle/save/reset`.
- **Saída:** rota `/(app)/notificacoes/preferencias` (G-8).
- **DoD:** loading skeleton do card; dirty state local sem refetch; Admin guardado por not-005.
- **Deps:** not-002, not-005 · **Prioridade:** Média

### not-009 · histórico
- **Arquivo:** `src/routes/(app)/notificacoes/historico/+page.svelte` — create
- **Conteúdo:** filtros (busca `SearchInput` + `Select`s tipo/canal/status + periodo + "Limpar filtros" + chips ativos) — params derivados da URL (deep-link) quando o contrato confirmar (D-4), senão estado local de página; tabela (Notificação/Tipo/Destinatário/Canal/Status/Enviada) com badges de canal e status (Lida muted / Não lida brand com dot), hover `bg-elevated/40`; paginação 10. Estados loading(TableSkeleton)/empty("Nenhum envio...")/error+retry. `data-testid: history-table/row/filters/clear-filters/pagination`.
- **Saída:** rota `/(app)/notificacoes/historico` (G-8).
- **DoD:** sem botão "Exportar CSV" (R-10 sem endpoint); `Enviada em` mono `tabnums`.
- **Deps:** not-002, not-005 · **Prioridade:** Média

---

## B4 — Espalhamento

### not-010 · submenu no menu.ts
- **Arquivo:** `src/lib/config/menu.ts` — modify
- **Conteúdo:** no item `Notificações` (já existe, linha 64), `children: [{label:'Minhas notificações', path:'/notificacoes'}, {label:'Preferências de canais', path:'/notificacoes/preferencias', canSee: isAdmin}, {label:'Histórico', path:'/notificacoes/historico', canSee: isAdmin}]`. Sem alterar `module`/`icon`.
- **Saída:** submenu colapsável no Sidebar (renderização já filtra `canSee` — sem render para não-Admin).
- **DoD:** RBAC hide real (ausência do DOM p/ `!isAdmin` — coberto em teste B5); itens Admin-only nunca disabled.
- **Deps:** — · **Prioridade:** Média · ⚠️ arquivo compartilhado (sequencial; livres agora)

---

## B5 — Testes

### not-011 · unit api
- **Arquivo:** `frontend/tests/unit/notifications-api.test.ts` — create
- **Conteúdo:** mocks de `apiFetch`; casos: list monta `/notifications` com `page/pageSize/type/read` (e **omite** type/read vazios); `status=unread→read=false`/`all→omitido`; markRead path `{id}/read`; markAllRead POST; get/save preferences; history monta query de filtros.
- **DoD:** verdes com vitest; `vi.mock('$lib/api/client')`.
- **Deps:** not-002 · **Prioridade:** Média

### not-012 · unit format
- **Arquivo:** `frontend/tests/unit/notification-format.test.ts` — create
- **Conteúdo:** `TYPE_META` cobre os 6 tipos com label/ícone/tone esperados; `CHANNEL_BADGE` cobre 3 canais; ordem `NOTIFICATION_TYPES`.
- **DoD:** verdes; sem depender de render (pure functions).
- **Deps:** not-003, not-004 · **Prioridade:** Baixa

### not-013 · unit página central
- **Arquivo:** `frontend/tests/unit/notifications-page.test.ts` — create
- **Conteúdo:** render de `+page.svelte` com fake timers: loading skeleton; success com rows; empty sem filtros (CTA dashboard); empty filtrado (chips+limpar); error (banner+retry refetch); mark-read otimista decrementa e revert em falha; mark-all some quando 0; heartbeat atualiza após polling do store; pagination chamam list com page nova.
- **DoD:** verdes; `@testing-library/svelte` + `vi.mock('$lib/api/notifications')`.
- **Deps:** not-007 · **Prioridade:** Média

### not-014 · unit preferências
- **Arquivo:** `frontend/tests/unit/notifications-prefs.test.ts` — create
- **Conteúdo:** render matriz; toggle marca dirty e habilita salvar; salvar chama `savePreferences` com matriz atual; falha → banner e mantém valores; "Restaurar padrão" (push de `sistema` OFF) fica dirty; pill "Restrito ao Admin" presente.
- **DoD:** verdes; `data-testid` do mockup.
- **Deps:** not-008 · **Prioridade:** Média

### not-015 · unit histórico
- **Arquivo:** `frontend/tests/unit/notifications-history.test.ts` — create
- **Conteúdo:** render tabela com rows; filtros alteram chamada de `history`; empty filtrado; error+retry; paginação.
- **DoD:** verdes; coerente com contrato 🟡 (params filtrados só os confirmados).
- **Deps:** not-009 · **Prioridade:** Baixa

### not-016 · e2e (condicional)
- **Arquivo:** `frontend/tests/e2e/notificacoes.spec.ts` — **create somente se suite e2e existir; senão SKIP documentado**
- **Conteúdo:** happy path Admin: lista→marcar lida→marcar todas; preferencias toggle+salvar; histórico filtrar; guarda: colaborador em `/historico` → redirecionado. Sem credenciais hardcoded.
- **DoD:** sem Playwright instalado → `SKIP` (documentar no `tasks.md`/relatório final).
- **Deps:** not-010 · **Prioridade:** Baixa

---

## Resumo
- **17 arquivos** (15 create · 2 modify: `menu.ts`, `Icon.svelte`) · **0 no template `_template`**
- **Dependências entre features:** `menu.ts` agora com submenu de notificações — executar **sequencial** com
  a próxima feature que tocar menu (rh/vendas/financeiro). `stores/notifications.ts` e `api/dashboard.ts`
  são de dono dashboard — somente import (sem edição).
- **Endpoints 🟡 (fila backend/PO):** GET `/api/notifications`, PATCH `/{id}/read`, POST `/read-all`,
  GET+PUT `/preferences`, GET `/history` — D-1..D-6 no `plan.md` §9.1.
- **e2e:** SKIP (sem Playwright) — idem auth/dashboard/producao/configuracoes/estoque.
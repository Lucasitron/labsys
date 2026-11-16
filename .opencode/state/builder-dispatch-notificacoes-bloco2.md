# DESPACHO — Builder (Frontend) · NOTIFICACOES — Bloco 2 (B2 Lista central) · 21/09/2026

> Implemente **not-005..not-007** de `.opencode/specs/frontend/features/notificacoes/tasks.md` (ler tasks + plan + mockup `docs/mokups/notificacoes/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Bloco 1 já implementado (not-001..004 must exist): `types/notifications.ts`, `api/notifications.ts`, `utils/notification-format.ts`, Icon `bell`
- Reuso: `api/client.ts` (apiFetch), `components/ui/*` (PageHeader, Tabs, Select, Skeleton, EmptyState, ErrorBanner, Pagination, Chip), `utils/format.ts` (relativeTime, Paginado), `stores/notifications.ts` (polling/unreadCount), `stores/toast`, `stores/auth` (user/isAdmin de `utils/permissions`)
- Rota inexistente hoje: `routes/(app)/notificacoes/` (dead-end no menu.ts:64 e NotificationsButton) — criar
- NÃO alterar specs/docs; NÃO commitar; NÃO alterar permissions.ts

## Tasks
- **not-005** `src/routes/(app)/notificacoes/+layout.ts` — reusa autenticação do `(app)/+layout.ts` (não duplicar fetchMe); se pathname em `/notificacoes/preferencias` ou `/notificacoes/historico` e `!isAdmin(user)` → redirect(303,'/notificacoes'); ssr=false/prerender=false.
- **not-006** `src/routes/(app)/notificacoes/components/NotificationRow.svelte` — presentação pura: ícone por tipo (bg-{tone}/15 border-{tone}/30), título + badge "Não lida" quando !read, body, meta `{Label} · {relativeTime}`, ação "Marcar como lida" (hide quando lida → tag "Lida"); não-lida com `border-l-2 border-brand bg-brand/5`; data-testid notification-row / notification-mark-read; aria-label na ação.
- **not-007** `src/routes/(app)/notificacoes/+page.svelte` — URL fonte de verdade (`?page&type&status`; status unread→read=false, all→omitido); listNotifications; 5 estados (Skeleton grid / rows / EmptyState CTA "/dashboard" sem filtros / EmptyState+campos+`Limpar filtros` filtrado / ErrorBanner+retry); PageHeader + heartbeat "Atualizado · {relativeTime}" (timestamp local; atualiza em fetch/read/read-all e no store unreadCount) + botão "Marcar todas" (hide quando unreadCount===0); Tabs (Todas/Não lidas) + Select tipo + "Limpar filtros"; Pagination 10/página; mudança de filtro → goto + refetch (keepFocus); ações otimistas com reversão+toast em falha; svelte:head título.

## Regras
1. Criar SÓ os 3 arquivos acima (+ nada mais).
2. pt-BR; aria-label; data-testid do mockup §8; sem any; sem libs; Svelte 5 runes; tokens design-system (`elevated #111a2c`).
3. Ao final: `npm run test` (regressão 252+) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- Por task ✅/❌ + evidência (arquivo:linha); mudanças (+/−); contrato 🟡 observado; resultados test/check/build.
# DESPACHO — Builder (Frontend) · NOTIFICACOES — Bloco 1 (B1 Fundação) · 21/09/2026

> Implemente **not-001..not-004** de `.opencode/specs/frontend/features/notificacoes/tasks.md` (ler tasks + plan + requirements/mockup requisitos.md).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Baseline códigos: `api/dashboard.ts` (tem `fetchUnreadCount`), `stores/notifications.ts`, `utils/format.ts` (relativeTime), `types/dashboard.ts` (UnreadCount), `components/ui/Icon.svelte`, `components/ui/*` (PageHeader/Tabs/Select/...), `api/client.ts` (apiFetch/ApiError)
- NÃO alterar specs/docs; NÃO commitar
- Ordem: not-004 (ícone) → not-001 (types) → not-002 (api) → not-003 (util)

## Tasks
- **not-004** `src/lib/components/ui/Icon.svelte` — adicionar `'bell'` a IconName + ICON_PATHS (path heroicons sine/bell). type-check ok.
- **not-001** `src/lib/types/notifications.ts` — types do tasks.md (NotificationType 6 valores; NotificationChannel inapp/email/push; Notification {id,type,title,body,read,createdAt,link?}; PagedNotifications<T>; NotificationPreferences; HistoryFilters). **Não redefinir UnreadCount** (importar de types/dashboard se precisar). Sem any.
- **not-002** `src/lib/api/notifications.ts` — helper bearer() local (padrão dashboard.ts); listNotifications({page,pageSize,type,read}) → GET /api/notifications 🟡 (query só com params não nulos); markRead(id) → PATCH /{id}/read 🟡; markAllRead() → POST /read-all 🟡; getPreferences() → GET /preferences 🟡; savePreferences → PUT /preferences 🟡; history(filters) → GET /history 🟡 (montar com search/type/read/channel/period e registrar D-4 no relatório). **Importar fetchUnreadCount de $lib/api/dashboard — NÃO duplicar.** Usar apiFetch; erros ApiError.
- **not-003** `src/lib/utils/notification-format.ts` — TYPE_META (6 tipos: Encomenda→shopping-bag/brand · Estoque→cube/warn · Financeiro→chart/success · Produção→wrench/ink · Pessoas&RH→users/brand · Sistema→bell/muted); CHANNEL_BADGE (In-app/brand · E-mail/success · Push/warn); NOTIFICATION_TYPES ordenado. REUSAR relativeTime de utils/format (não duplicar).

## Regras
1. Criar/editar SÓ os 4 arquivos acima.
2. pt-BR; aria-label; sem any; sem libs; Svelte 5 runes; tokens design-system.
3. Ao final: `npm run test` (regressão — baseline 252 verdes; sem novos testes neste bloco) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- Por task ✅/❌ + evidência; mudanças (+/−); contrato 🟡 de cada endpoint; resultados test/check/build.
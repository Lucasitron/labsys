# DESPACHO — Builder (Frontend) · NOTIFICACOES — Bloco 6 (correções Validator+Security) · 21/09/2026

> Correções das ressalvas de Validator (A1/M1/M2/B1..B6) e Security (M1/B1/B2/B3) da feature `notificacoes`.
> **Princípio código mínimo: o saldo deve reduzir ou manter linhas; NÃO criar abstrações novas além do pedido.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Feature commitada: `d62efee` · `18714ee` · `ebfdcd8` · `65c158b`; **294 testes verdes; check 0 errors/16 warnings/7 files; build ok**
- NÃO alterar specs/docs (`plan.md` §9.1 é atualizado pelo orquestrador); NÃO alterar `permissions.ts`; NÃO commitar

## Correções

### C1 (Validator A1 — ALTA) — "Marcar todas" restaura contador obsoleto
- `routes/(app)/notificacoes/+page.svelte` (~linhas 176-194): hoje no sucesso faz `count = get(unreadCount)` — o store só atualiza no polling, então o botão "Marcar todas" reaparece com valor antigo. **Capturar `const countAnterior = count;` antes do otimista**; no sucesso manter `count = 0` (sem `get(unreadCount)`); no catch restaurar `count = countAnterior` (não `get(unreadCount)`).

### C2 (Validator B1 = Security B2) — `ICON_STYLES` duplicado em 3 arquivos
- Exportar `export const TONE_STYLES: Record<Tone, string>` em `src/lib/utils/notification-format.ts` (unificar a ordem de classes dos 3 mapas — usar a do `NotificationRow`) e **remover os 3 mapas locais**, importando de lá em `NotificationRow.svelte`, `preferencias/+page.svelte`, `historico/+page.svelte`.

### C3 (Validator B3) — re-export morto de `fetchUnreadCount`
- `src/lib/api/notifications.ts`: **remover** `import { fetchUnreadCount } from './dashboard'` e `export { fetchUnreadCount }` **após confirmar por grep** que ninguém importa de `api/notifications` (consumidor real é `stores/notifications.ts` ← `api/dashboard`). Remover também o teste tautológico `notifications-api.test.ts:183-186`.

### C4 (Validator B4) — testid `notifications-retry`
- `src/lib/components/ui/ErrorBanner.svelte`: adicionar prop **opcional** `testid?: string` e aplicar no `<button>` de retry (`data-testid={testid}`). Passar `testid="notifications-retry"` em `routes/(app)/notificacoes/+page.svelte` (mantendo `notifications-error` no wrapper). Adicionar asserção de retry por testid no teste da página.

### C5 (Validator B5) — tipar `history()` e remover cast local
- `src/lib/types/notifications.ts`: adicionar `HistoryEntry` (campos usados na tabela: `id`, `title`, `type`, `recipient`, `channel`, `read`, `sentAt` — conferir nomes reais usados em `historico/+page.svelte`) e `HistoryPayload { items: HistoryEntry[]; total: number }`.
- `src/lib/api/notifications.ts`: `history(...): Promise<HistoryPayload>`.
- `historico/+page.svelte`: remover interfaces locais e o cast; consumir o tipo. **Manter** a paginação client-side (contrato 🟡 D-4) — apenas tipada.

### C6 (Validator M1) — testes de guarda Admin (303) e RBAC hide do menu
- Criar `frontend/tests/unit/notifications-guard.test.ts`:
  - `load` de `routes/(app)/notificacoes/+layout.ts`: não-Admin em `/notificacoes/preferencias` e `/notificacoes/historico` → lança `redirect(303,'/notificacoes')`; Admin passa; rota `/notificacoes` passa para qualquer autenticado. (mockar `stores/auth` e usar o padrão de `permissions.test.ts`/outros testes de load se existirem)
  - RBAC hide: os `children` Admin-only de `Notificações` em `config/menu.ts` têm `canSee: isAdmin` e **não** aparecem para não-Admin (testar a função/estrutura de filtragem usada pelo `Sidebar`, sem render pesado se possível).

### C7 (Security B3) — fallback de `TYPE_META` para tipo inesperado
- `NotificationRow.svelte` e `historico/+page.svelte`: usar `TYPE_META[x.type] ?? TYPE_META.sistema` (evita `TypeError` com tipo fora da enum). (Se `preferencias` já indexa por `NOTIFICATION_TYPES`, não muda.)

### C8 (Validator A1 cobertura) — asserção do botão pós-"marcar todas"
- No `notifications-page.test.ts`, adicionar caso: após sucesso de `markAllRead`, `notification-mark-all-read` **desaparece** (`count===0`).

## NÃO fazer (registrado como ressalva/aceito — não é escopo)
- `Tone` continua local em `types/notifications.ts` (padrão já existente em `types/stock.ts` e `types/producao.ts`; extrair para `types/common.ts` tocaria outras features).
- Mensagens cruas `err.message` (padrão app-wide; fila backend).
- Guarda client-side adicional em `getPreferences/savePreferences/history` (defense-in-depth) → fila backend **D-3** (`@PreAuthorize` Admin).

## Regras
1. Tocar SOMENTE: `routes/(app)/notificacoes/**`, `lib/api/notifications.ts`, `lib/types/notifications.ts`, `lib/utils/notification-format.ts`, `lib/components/ui/ErrorBanner.svelte`, testes de notificações. **NÃO commitar.**
2. pt-BR; sem `any`; Svelte 5 runes; tokens design-system; código mínimo (C2/C3/C5 reduzem).
3. Ao final: `npm run test` (todos verdes; reportar total — deve ser ≥294, ajustado pelas remoções) + `npm run check` (0 errors; registrar warnings) + `npm run build` (ok).

## Relatório
- Por correção C1..C8 ✅/❌ + evidência (arquivo:linha) + linhas +/−
- Total de testes · check · build · desvios/observações
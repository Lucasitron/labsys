# DESPACHO — Security (Frontend) · NOTIFICACOES · 21/09/2026

> Auditoria de segurança da feature `notificacoes` implementada (commits d62efee · 18714ee · ebfdcd8 · 65c158b).
> **Princípio código mínimo: sinalizar também código desnecessário/duplicado encontrado na auditoria.**

## Contexto
- Workspace `/home/lucasitron/Documentos/05-dev/labSys`; branch `dev/frontend` (somente leitura)
- Abordagem "contexto zero" (como features anteriores): avaliar o front sem assumir confiança do backend

## Escopo
- `src/lib/api/notifications.ts` (6 endpoints 🟡 + reexport fetchUnreadCount) · `src/lib/types/notifications.ts` · `src/lib/utils/notification-format.ts` · `src/routes/(app)/notificacoes/**` (+layout.ts, +page.svelte, NotificationRow.svelte, preferencias, historico) · `src/lib/config/menu.ts` (submenu com canSee: isAdmin) · `Icon.svelte` (bell) · integração com stores/auth, stores/notifications (polling), utils/permissions

## Pontos de checagem (ex.:)
- RBAC: guarda Admin client-side redireciona (não 403) — botão/ações Admin ocultos para não-Admin; **ausência do DOM**, nunca disable; menu hide real
- XSS: sem `{@html}`; link? (se renderizado) com validação de scheme; título/body de notificações escapados
- Autorização de escrita: `markRead(id)`/`markAllRead`/`savePreferences` disponíveis a qualquer autenticado? (backend deve isolar por JWT — D-2/D-3; avaliar impacto no front, ex.: exposição de ações)
- Exposição de informação: histórico/preferências (Admin-only) com dados sensíveis; `unreadCount` poluição cruzada de usuário (polling global?)
- Segurança de resposta: error handling não vaza stack/HTML; mensagens pt-BR amigáveis (vs cruas do backend)
- Práticas: query montada só com params não nulos; sem credenciais em URL; Content-Type = application/json; sem console.log/any
- Código mínimo: helpers só se necessários; paginação client-side do histórico aceitável? ; `notif-page-state.svelte.js` justificado?

## Produto
- **UM output completo inline** (proibido resumir): veredito (APROVADA/APROVADA COM RESSALVAS/REPROVADA) + contagem por severidade (Alto/Médio/Baixo/Informativo) + lista itemizada com evidência arquivo:linha + correção sugerida mínima + verificação rápida (`npm run check`; conferir `rg "{@html}|console\\.log|any"` no escopo notificacoes).

## Regras
- Somente leitura; **NÃO editar/commitar**; sem libs.
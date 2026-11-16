# DESPACHO — Validator (Frontend) · NOTIFICACOES · 21/09/2026

> Validar a feature `notificacoes` implementada (commits d62efee · 18714ee · ebfdcd8 · 65c158b) contra spec/mockup.
> **Princípio código mínimo: verificar pertinência/código mínimo; sinalizar código não pertinente.**

## Contexto
- Workspace `/home/lucasitron/Documentos/05-dev/labSys`; branch `dev/frontend` (NÃO tocar em nada; somente leitura)

## Entradas
- Spec: `.opencode/specs/frontend/features/notificacoes/{requirements,plan,tasks}.md` (36... 16 tasks not-001..016)
- Mockup aprovado: `docs/mokups/notificacoes/` (full.html + requisitos.md §8 testids)
- Código: `src/lib/{types/notifications.ts, api/notifications.ts, utils/notification-format.ts, components/ui/Icon.svelte(bell), config/menu.ts(submenu notificacoes)}` · `src/routes/(app)/notificacoes/**` (+layout.ts guard, +page.svelte, NotificationRow, preferencias/+page.svelte, historico/+page.svelte) · testes `frontend/tests/unit/notifications-*.test.ts` + `notif-page-state.svelte.js`
- Padrões: DoD/checklists em plan.md §5

## Critérios (checklist §5 do plan + tasks DoD)
1. 16 tasks implementadas (1 arquivo=1 task); tipos sem any; API com query mínima e reuso de fetchUnreadCount/relativeTime/UI kit
2. Guarda Admin (redirect 303) em preferencias/historico — e RBAC **hide** no menu (sem render p/ !isAdmin); NÃO disable
3. URL fonte de verdade `?page&type&status` (unread→read=false, all→omitido); 5 estados (loading/success/empty/empty-filtrado/error+retry); ações otimistas com reversão+toast
4. Matriz de preferencias (dirty/save/restaurar padrão/falha mantém valores); histórico com filtros+paginação+sem export CSV (R-10); testids do mockup; pt-BR; aria-label
5. Testes: 294 verdes; cobertura coerente (42 novos); sem testes frágeis; build/check ok
6. **Código mínimo:** qualquer código não pertinente/duplicado/speculativo → ressalva (ex.: paginação client-side do histórico é aceita? helper notif-page-state é justificado?).

## Produto
- **UM output completo inline** (proibido resumir/encaminhar): veredito (APROVADA/APROVADA COM RESSALVAS/REPROVADA) + checklist por critério (✅/❌/⚠️ com evidência arquivo:linha) + ressalvas separadas por severidade (Alta/Média/Baixa), cada uma com correção sugerida mínima + verificação final: `npm run test` (294), `npm run check`, `npm run build`.

## Regras
- Somente leitura de código; **NÃO editar/commitar**; não instalar libs; não usar e2e (sem Playwright).
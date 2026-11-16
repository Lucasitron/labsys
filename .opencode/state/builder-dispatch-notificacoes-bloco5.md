# DESPACHO — Builder (Frontend) · NOTIFICACOES — Bloco 5 (B5 Testes) · 21/09/2026

> Implemente **not-011..not-016** de `.opencode/specs/frontend/features/notificacoes/tasks.md` (ler tasks).
> **Princípio código mínimo: testes só do que resolve caso real; sem duplicar cobertura existente (ex.: store já coberto 8 casos em dashboard).**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-4 implementados. Baseline **252 testes verdes** (17 files). Padrão de testes: `frontend/tests/unit/*.test.ts` (vitest + @testing-library/svelte; alias no vite.config: svelte→index-client.js + server.deps.inline)
- e2e: **Playwright NÃO instalado** → criar `frontend/tests/e2e/notificacoes.spec.ts` **apenas se alguma suíte e2e existir** (`frontend/tests/e2e/*`). Verificar; se não houver, documentar SKIP no relatório (sem arquivo).
- NÃO alterar specs/docs; NÃO commitar

## Tasks
- **not-011** `frontend/tests/unit/notifications-api.test.ts` — mocks de apiFetch (vi.mock('$lib/api/client')): list monta query page/pageSize/type/read omite vazios; status unread→read=false / all→omitido; markRead path {id}/read; markAllRead POST; get/save preferences; history monta filtros.
- **not-012** `frontend/tests/unit/notification-format.test.ts` — TYPE_META 6 tipos (label/ícone/tone); CHANNEL_BADGE 3 canais; NOTIFICATION_TYPES ordem.
- **not-013** `frontend/tests/unit/notifications-page.test.ts` — render +page: loading; success rows; empty sem filtros (CTA dashboard); empty filtrado (chips+limpar); error banner+retry; mark-read otimista decrementa e revert em falha; mark-all some quando 0; heartbeat atualiza após store; pagination chama list nova página.
- **not-014** `frontend/tests/unit/notifications-prefs.test.ts` — render matriz; toggle marca dirty/habilita salvar; save chama savePreferences com matriz; falha → banner e mantém; "Restaurar padrão" dirty; pill Restrito.
- **not-015** `frontend/tests/unit/notifications-history.test.ts` — render tabela rows; filtros alteram history; empty filtrado; error+retry; paginação.
- **not-016** e2e condicional — SKIP documentado (relatório) se não houver suíte e2e instalada.

## Regras
1. Criar SÓ os arquivos de teste (+ mocks locais; sem tocar código de produção a menos que bug de teste revele e seja parte das tasks).
2. Rodar `npm run test` (todos verdes; novo total reportado) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- Por task ✅/❌ + contagem de casos; total final de testes; check; build; notas sobre e2e SKIP.
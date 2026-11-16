# Despacho Builder — NOTIF-D1-D7 (§6.3 contract.gap)

> Data: 24/09/2026 · Base: **`dev/backend`** · work branch nova **`feature/notification-d1-d7`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend`).
> Fonte da verdade: `docs/contract.gap.md` §6.3 (contratos abaixo) + código `notification-service` em `dev/backend` (hoje: só `GET /notifications/unread/count` + migração) + `.opencode/specs/features/notification-service/` (leitura — NUNCA editar `docs/`).

## Triage minimal-code (verificar antes; reaproveitar entidade/migração JWT existentes)
- **D-1**: `UnreadCount { count }` + `GET /notifications` paginado canônico (`items/total/page/size`).
- **D-2**: `GET /notifications` isolado por JWT (só as do usuário — derivar `id_user` do token, nunca do client).
- **D-3** 🔴: `@PreAuthorize`/`hasRole ADMIN` em `/preferences` e `/history` (server-side).
- **D-4**: `GET /history` aceita `search/type/read/channel/period` e retorna **lista completa** (`HistoryPayload { items, total }` — D-7); front pagina client-side.
- **D-5**: erros não-técnicos PT (handler existente ou novo mínimo). **D-6**: validar `link?` (formato/allowlist mínima; rejeitar 422 se inválido).
- Endpoints: `GET /api/notifications` · `PATCH /api/notifications/{id}/read` · `POST /api/notifications/read-all` · `GET+PUT /api/notifications/preferences` · `GET /api/notifications/history` (rotas gateway `/api/notification/**`+`/api/notifications/**` JÁ existem — verificar, zero código se ok).

## Regras
1. Só `notification-service/` (+ gateway yml SOMENTE se faltar rota — verificar primeiro).
2. Records+Bean Validation PT; `ddl-auto=validate`; `mvn test` verde antes de cada commit; commits PT atômicos + push (SEM merge).
3. Sub-item já atendido → marcar sem código. Sub-item sem spec → PARCIAL + REPORTAR.

## Relatório
```json
{
  "tarefa": "notification D-1..D-7 (§6.3)",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS | FAIL | NAO_RODADO",
  "d1": false, "d2": false, "d3": false, "d4": false, "d5": false, "d6": false, "d7": false,
  "ja_atendidos_sem_codigo": [],
  "blockedReason": null,
  "duvidas": []
}
```

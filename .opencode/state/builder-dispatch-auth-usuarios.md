# Despacho Builder — AUTH-USUARIOS-001 (§6.1 contract.gap) — dono auth-service

> Data: 24/09/2026 · Decisão PO: `usuarios`+`permissoes`+`auth/senha` → **`auth-service`**; `configuracoes/sistema`+`tokens` → **NOVO ESCOPO (fora deste despacho)**.
> Base: **`dev/backend`** (já contém estoque validado+mergeado) · work branch nova **`feature/auth-usuarios-permissoes`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend` — merge é do Orquestrador após o relatório).
> Fonte da verdade: `.opencode/specs/frontend/features/configuracoes/{requirements,tasks}.md` (contratos abaixo) + código canônico `auth-service` em `dev/backend` + `docs/contract.gap.md` §6.1 (leitura — NUNCA editar `docs/`).

## Triage minimal-code (NÃO re-expandir)
- ✅ Já OK (zero código): C-1 parcial (`/auth/permissions` hasRole ADMIN), C-2 (`/auth/me` deriva do JWT).
- ❌ Fora: tudo de `/configuracoes/**` (novo escopo), C-5 tokens (era `/configuracoes/tokens` — novo escopo).
- 🔨 Escopo deste despacho (só o que falta, reaproveitando ao máximo):
  1. `PUT /auth/senha` — altera senha (atual + nova + confirmação; nova ≥ 8 chars; BCrypt; erro PT não-técnico).
  2. `GET /api/usuarios` (serve `page/search/nivel[]/situacao[]` + counts/paginação — C-6; front nunca deriva), `POST /api/usuarios`, `PUT /api/usuarios/{id}`, `PATCH /api/usuarios/{id}/status` — operam **contas/nível/situação** nas tabelas `login`+`user_permissions` (NÃO duplicar RH; identidade física é do `rh-service`).
  3. `GET /api/permissoes` (matriz completa — REAPROVEITAR `RbacService` existente, sem recalcular) + `PUT /api/permissoes/{modulo}/{nivel}` (validar módulo/nível contra enums — C-3; 422 PT se inválido).
  4. C-1: `@PreAuthorize`/matcher **Admin-only** em TODOS os endpoints novos (server-side; guarda do front é burlável).
  5. C-4: erros via `GlobalExceptionHandler` existente (nada de stack/SQL; `code` estruturado PT).
  6. Gateway (config, 2 blocos): `Path=/api/usuarios/**` e `Path=/api/permissoes/**` → `lb://auth-service`.
- ⚠️ Verificar antes: entidade `Login` tem campo situação/ativo? Se NÃO existir: migration mínima `V5__` + campo (justificado — problema real); se existir, zero migration.

## Regras
1. Nunca altera `docs/`, specs, `frontend/`, outros services. Arquivos novos só em `auth-service/` (+ `gateway-service/application.yml` rotas).
2. Records+Bean Validation PT; `ddl-auto=validate` respeitado; `mvn test` do `auth-service` verde antes de cada commit; commits PT atômicos na work branch + push (SEM merge).
3. Se qualquer sub-item exigir spec inexistente ou >~8 arquivos: implementar o restante e REPORTAR o sub-item como BLOQUEADO parcial (não travar tudo).

## Relatório
```json
{
  "tarefa": "auth-usuarios-001 (§6.1)",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "endpoints_novos": [],
  "resultado_mvn_test": "PASS | FAIL | NAO_RODADO",
  "c1_admin_ok": false, "c3_enum_ok": false, "c4_erros_ok": false, "c6_counts_ok": false,
  "blockedReason": null,
  "duvidas": []
}
```

# Despacho Builder — AUTH-SEED-002 (§6.0 contract.gap) — CANÔNICO Login

> Data: 24/09/2026 · Decisões PO: (a) canônico = `feature/auth-service` + specs (modelo `Login`); (b) reconciliação AUTORIZADA; (c) coluna/UNIQUE existente basta (sem migration nova).
> Base: **`dev/backend`** · work branch nova **`feature/auth-reconciliacao-seed`** a partir de `dev/backend` (NUNCA commitar em `dev/frontend`; working tree atual está nela — só leitura).
> Fonte da verdade: `.opencode/specs/features/auth-service/{requirements,plan,tasks}.md` + código em `feature/auth-service` + `docs/contract.gap.md` §6.0/§8 (leitura — NUNCA editar `docs/`).

## Fatos já apurados (NÃO re-investigar do zero, só confirmar)
- Canônico tem: `LoginRepository{findByEmail, findByNomeUsuario, ...}`, `AuthService.login` aceitando **email OU nomeUsuario**, `V1` com `uk_login_email` UNIQUE, `V2` `user_permissions` (Role 0-4, ADMIN=0). **Login por email JÁ funciona no service — nada a fazer aí.**
- Canônico **NÃO tem seed prod** (só massa de teste): sem seeder, sem `auth.seed` no yml.
- Front envia `POST /auth/login {username, password}` (`frontend/src/lib/api/auth.ts:7`); canônico espera `{email|nomeUsuario, senha}`. **Incompatibilidade real de payload** — login via UI falha sem um dos dois lados ceder.
- `dev/backend` contém modelo `Usuario` divergente (90 arqs, 976+/3019-) — **descartar em favor do canônico** (autorização PO).

## Tasks (ordem fixa)
1. **auth-rec-001 — reconciliação**: na work branch, substituir a árvore `auth-service/` pela versão de `feature/auth-service` (`git checkout feature/auth-service -- auth-service`), confirmar `diff work-branch vs feature/auth-service -- auth-service` = vazio. Nada de reset/merge que toque outros services.
2. **auth-seed-002 — seed admin dev-only**: criar `SeedProperties(prefix=auth.seed)` + seeder `ApplicationRunner` que, **só se tabela `login` vazia**, cria `Login{email=admin@fablab.org, nomeUsuario=admin, senhaHash=BCrypt(admin123)}` + `UserPermission{role=ADMIN(0), active=true}`; defaults via env `AUTH_ROOT_*` (precedente §8.1); **dev-only, nunca produção**; sem log de credencial. Confirmar campos reais em `Login.java`/`UserPermission.java`+V1/V2 antes (ler, não adivinhar).
3. **auth-login-003 — compat payload UI (menor código vence)**: preferir `@JsonAlias("username")` em `nomeUsuario` + `@JsonAlias("password")` em `senha` no `LoginRequest` (2 anotações, zero churn no front concluído, PT segue canônico). Só se quebrar teste/spec existente: parar e REPORTAR em vez de tocar o frontend.
4. **auth-test-004**: `mvn test` do `auth-service` verde (reaproveitar suites existentes; teste novo só se couber em arquivo existente: seed-idempotente + login-por-email + login-por-username).

## Regras
1. Nunca altera `docs/`, specs, `frontend/`, `gateway-service/`, outros services.
2. Commits PT atômicos (`feat(auth): ...`) na work branch; ao final: merge na `dev/backend` local + push da work branch e da `dev/backend`.
3. Se qualquer task exigir >~5 arquivos ou violar spec: parar e reportar BLOQUEADO (não improvisar).

## Relatório
```json
{
  "tarefa": "auth-rec-001+auth-seed-002+auth-login-003 (§6.0)",
  "status": "IMPLEMENTADO | BLOQUEADO",
  "arquivos_alterados": [],
  "resultado_mvn_test": "PASS | FAIL | NAO_RODADO",
  "seed_admin_ok": false,
  "login_ui_ok": false,
  "blockedReason": null,
  "duvidas": []
}
```

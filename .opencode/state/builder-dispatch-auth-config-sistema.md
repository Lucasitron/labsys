# Despacho Builder — AUTH-CONFIG-SISTEMA (novo escopo §6.1/C-5, dono auth-service)

> Data: 24/09/2026 · Decisão PO: `configuracoes/sistema+tokens` = novo escopo; dono **`auth-service`** (casa admin existente: usuarios/permissoes/senha).
> Base: **`dev/backend`** · work branch nova **`feature/auth-config-sistema`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend`).
> Fonte da verdade: `.opencode/specs/frontend/features/configuracoes/{requirements,tasks}.md` (cfg-006/cfg-018/cfg-019 + tipos `ParametroSistema`/`TokenIntegracao`) + `docs/mokups/configuracoes/` (leitura — NUNCA editar `docs/`).

## Escopo (Admin-only server-side em TUDO — front é HIDE burlável)
1. `GET /api/configuracoes/sistema` → `{ identidade{nomeFablab, logo?}, cadenciaChecklist5S, cadenciaAuditoria5S?, tokens[] }` (KV `parametro_sistema`, seed de defaults na migration).
2. `PUT /api/configuracoes/sistema` → persiste identidade + cadências (validação PT; 422 se inválido). Alimenta Produção (cadência 5S) — só persistir, sem chamar outro service.
3. `GET /api/configuracoes/tokens` → lista sem segredo (`id, nome, prefixo, criadoEm, ultimoUso, revogado`).
4. `POST /api/configuracoes/tokens {nome}` → gera chave **exibida 1×** na resposta, armazena **só hash SHA-256 + prefixo** (C-5). Registra `ultimoUso` quando usada (se houver ponto de uso; senão REPORTAR como follow-up, não inventar middleware).
5. `DELETE /api/configuracoes/tokens/{id}` → revoga (soft `revogado=true`; 404 se inexistente).
6. Gateway (config): `Path=/api/configuracoes/**` → `lb://auth-service` (verificar antes; zero código se existir).
7. Testes: `mvn test` verde (não-Admin → 403; chave 1× nunca reexposta; hash armazenado ≠ chave; revogado some da lista ativa). Commits PT + push (SEM merge).

## Regras
1. Só `auth-service/` (+ gateway yml SOMENTE p/ rota, após verificar). Records+Bean Validation PT; Flyway; `ddl-auto=validate`; erros PT não-técnicos.
2. NUNCA logar/expor chave após criação. NUNCA retornar hash ao client.
3. Sub-item sem lastro → PARCIAL + REPORTAR.

## Relatório
```json
{
  "tarefa": "auth config-sistema+tokens (novo escopo)",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS n/n | FAIL | NAO_RODADO",
  "sistema_ok": false, "tokens_ok": false, "c5_uma_vez_ok": false,
  "blockedReason": null,
  "duvidas": []
}
```

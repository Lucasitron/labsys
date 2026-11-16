# Despacho Builder — AUTH-RESSALVAS-002 (matriz persistente + revogação na troca de senha)

> Data: 24/09/2026 · PO mandou zerar as 2 ressalvas do §6.1. Ambas são problema real (não estilo): (1) PUT 200 que perde dado no restart = mentira contratual; (2) senha trocada sem revogar token = sessão roubada sobrevive.
> Base: **`dev/backend`** · work branch nova **`feature/auth-ressalvas`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend` — merge é do Orquestrador).
> Fonte da verdade: código canônico `auth-service` em `dev/backend` + `RbacService` (cells ConcurrentHashMap) + `TokenBlacklistService{blacklist(token,claims)}` + tabela `token_blacklist(token, expiry_date)` + `docs/contract.gap.md` §6.1 (leitura — NUNCA editar `docs/`).

## Tasks (ordem fixa, só auth-service)
1. **ressalva-1 — matriz persistente**: nova tabela `permissao_matriz(modulo VARCHAR, role INT/role, nivel VARCHAR, PK(modulo,role))` via migration `V6__`; entity + repository; `RbacService`: trocar `cells ConcurrentHashMap` por leitura/escrita no banco (seed das defaults atuais na primeira leitura ou via migration — o que for menor); `GET/PUT /api/permissoes` passam a refletir o banco (restart preserva). Sem tabela nova além dessa.
2. **ressalva-2 — revogar token na troca de senha**: em `PUT /auth/senha` (autenticado), extrair o Bearer atual do header e chamar `blacklist(token, claims)` (força re-login; demais sessões do usuário NÃO são alcançáveis no schema atual sem coluna de vínculo — REGISTRAR isso como limitação no relatório, não inventar coluna nova).
3. **Testes**: estender suites existentes (matriz persiste após restart simulado = novo contexto/reload; senha trocada → token atual 401; senha segue válida p/ novo login). `mvn test` do `auth-service` verde.

## Regras
1. Só `auth-service/`. Records+Bean Validation PT; `ddl-auto=validate`; commits PT atômicos + push (SEM merge).
2. Se algo exigir spec inexistente ou >~8 arquivos: fazer o restante e REPORTAR o sub-item como BLOQUEADO parcial.

## Relatório
```json
{
  "tarefa": "auth-ressalvas-002",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS | FAIL | NAO_RODADO",
  "matriz_persistente_ok": false,
  "senha_revoga_token_ok": false,
  "limitacoes": [],
  "blockedReason": null,
  "duvidas": []
}
```

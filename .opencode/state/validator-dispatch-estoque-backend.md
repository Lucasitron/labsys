# Despacho Validador — ESTOQUE BACKEND (estq T1..T9, tip órfão aac1450)

> Data: 24/09/2026 · Builder reportou IMPLEMENTADO em 20/09 (9 commits `6c0cb0e..aac1450`, 69 testes, 62/62 `[x]`).
> **ANOMALIA DE ESTADO (não resolver, só contornar p/ validar)**: em 20/09 18:53 a branch `feature/estoque-service` foi RESETada para `dev/backend` (`78a213b`), orfanando a cadeia do Builder. Os commits EXISTEM como objetos; o tip a validar é **`aac1450`**. NUNCA mover ponteiros de branch, nunca `stash apply/drop`, nunca commitar.
> Fonte da verdade (leitura — NUNCA editar `docs/`): `.opencode/specs/features/estoque-service/{requirements,plan,tasks}.md` + `docs/contract.gap.md` §5.6 + `docs/03-Estoque_&_Suprimentos_Service.md`.

## Método (read-only total)
1. Criar worktree destacada: `git worktree add /tmp/vld-estoque aac1450` (NÃO tocar na working tree principal, que está em `dev/backend`). Ao final, remover: `git worktree remove --force /tmp/vld-estoque`.
2. Em `/tmp/vld-estoque/estoque-service`: rodar você mesmo `mvn clean test` + `mvn compile` → resultado REAL (não acreditar no relatório do Builder).
3. Grep de conformidade PT: **zero** `/stock/**`, `/api/stock`, `stock` em paths de endpoint/schema; confirma `com/fablab/estoque/**`, `/api/estoque/**`, endpoints PT (`/itens`, `/entradas`, `/saidas`, `/emprestimos`, `/fornecedores`, `/localizacoes`, `/boms`), porta 8083, Eureka `fablab/estoque-service`, Flyway V1..V8, RabbitMQ `fablab.*`.
4. RBAC: `@PreAuthorize` presente (Admin/Bolsista editam; demais visualizam); saldo nunca negativo (lock pessimista); `estoque.baixo.event` quando `quantidade_atual <= estoque_minimo`; scheduler de atrasados configurável (`enabled`/`cron`); `SaldoInsuficienteException` → 409.
5. Spot-check `tasks.md` T1..T9: cada `[x]` alegado corresponde a implementação real verificável no diff `7bc70f5..aac1450 -- estoque-service/` (não só checkbox). Listar qualquer critério marcado sem lastro como falha.
6. Escopo: confirmar que o diff só toca `estoque-service/` (nenhum `docs/`, `gateway-service/`, `frontend/`, outro service). **Base antiga**: `7bc70f5` é `dev/backend` antigo (pré-dashboard/auth-merge) — REGISTRAR como nota (merge futuro exigirá rebase), não como falha.
7. Dívida 1 (§5.6): `POST /localizacoes`, `GET /entradas/{id}`, `GET /saidas/{id}`, `GET /emprestimos/{id}` presentes.

## Regras (Validador)
1. NUNCA altera código/specs/docs/branches/stash — só lê, roda testes/grep e emite veredito.
2. Bug real reproduzível → `falhas[]` SEM corrigir (correção = novo despacho Builder). Estilo menor → `nonBlockingNota`.
3. Veredito só após `mvn test` rodado por VOCÊ no checkout.

## Veredito (retornar)
```json
{
  "tarefa": "estoque-service T1..T9 @aac1450",
  "veredito": "APROVADO | APROVADO_COM_RESSALVAS | REPROVADO",
  "mvn_test_real": "PASS n/n | FAIL",
  "tasks_todos_com_lastro": true,
  "nenhum_arquivo_fora_escopo": true,
  "naming_pt_conforme": true,
  "falhas": [],
  "nonBlockingNota": "",
  "nota_rebase": "base 7bc70f5 defasada vs dev/backend atual (4cb2778); merge exigira rebase",
  "blockedReason": null
}
```

# Despacho Builder — ESTOQUE-E4-NOME (tomador via RH, fail-soft)

> Data: 25/09/2026 · Decisão PO: nome do tomador via RH = SIM. `?status=` p/ entradas/saídas = NÃO IMPLEMENTAR (front `movements.ts` não envia esse filtro — sem consumidor; regra minimal; PO pode reverter com ordem expressa).
> Base: **`dev/backend`** · work branch nova **`feature/estoque-e4-nome`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend`).
> Fonte da verdade: `Emprestimo.idPessoa` (FK externa RH) + front `Loan.borrower{id,name,initials}` (`frontend/src/lib/types/stock.ts:204`) + `docs/contract.gap.md` §6.2 E-4 (leitura — NUNCA editar `docs/`).

## Escopo (só estoque-service)
- Resolver `borrower.name` (+ `initials` derivadas) em `EmprestimoResponse` (lista global + detalhe) via lookup no `rh-service` (`GET /pessoas/{id}` — VERIFICAR contrato real no código do rh-service antes; se divergir, REPORTAR em vez de adivinhar).
- Client HTTP mínimo (ex.: `RestClient` + `lb://rh-service`, timeout curto) com **fail-soft**: RH fora/lento/404 → fallback ao rótulo atual `Pessoa #id` (nunca 500 por causa do nome).
- Sem nova tabela, sem migration (só se o lookup exigir coluna nova — nesse caso PARAR e reportar; não criar).
- Testes: tomador resolvido + fallback com RH indisponível. `mvn test` verde; commits PT + push (SEM merge).

## Regras
1. Só `estoque-service/`. Records PT; erros PT não-técnicos; nunca tocar docs/specs/frontend/gateway/outros services.
2. Se o custo estourar (~6 arquivos) ou o contrato RH não existir: PARCIAL/BLOQUEADO + REPORTAR (pergunta em tempo de execução, não improviso).

## Relatório
```json
{
  "tarefa": "estoque E-4-nome (tomador via RH)",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS n/n | FAIL | NAO_RODADO",
  "nome_resolvido_ok": false,
  "fallback_ok": false,
  "status_entradas_saidas": "NAO_IMPLEMENTADO_SEM_CONSUMIDOR",
  "blockedReason": null,
  "duvidas": []
}
```

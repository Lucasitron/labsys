# Despacho Builder — ESTOQUE-E1-E7 (§6.2 contract.gap, pós-validação APROVADA)

> Data: 24/09/2026 · Base validada+mergeada em `dev/backend` (branch `feature/estoque-service` restaurada; merge `381518f`).
> Base: **`dev/backend`** · work branch nova **`feature/estoque-e1-e7`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend`).
> Fonte da verdade: `docs/contract.gap.md` §6.2 (contratos abaixo) + código `estoque-service` em `dev/backend` + `.opencode/specs/features/estoque-service/requirements.md` (leitura — NUNCA editar `docs/`).

## Triage minimal-code (verificar cada item; só codar o gap real)
- **E-1 (R-9)**: listas globais `GET /entradas?status=`, `GET /saidas?status=`, `GET /emprestimos?status=ativos|atrasados|historico` (hoje: escopo por item + `/emprestimos/atrasados`). Reaproveitar queries existentes.
- **E-2 (R-8)**: `GET /boms?projetoId=` (hoje: busca por id). Se `lista_materiais` já tem `id_produto_servico`, é só query+endpoint.
- **E-3 (D-2)**: aceitar/persistir `responsavel` na escrita (entradas/saídas/empréstimos). Verificar se coluna existe; se não, migration mínima + campo.
- **E-4 (D-4)**: incluir `pessoa` (tomador) em `EmprestimoResponse` (derivar do vínculo existente; sem nova tabela se der).
- **E-5**: escape CSV (CWE-1236: prefixar `=+-@` com `'`) no export server-side de itens (front já escapa o da BOM).
- **E-6**: mensagens de erro não-técnicas (estender `GlobalExceptionHandler` existente; nada de stack/SQL).
- **E-7**: RBAC server-side em `GET /emprestimos/**` (leitura restrita a Admin/responsável do empréstimo; reaproveitar `EDICAO/VISUALIZACAO/OPERACAO`).

## Regras
1. Só `estoque-service/`. Records+Bean Validation PT; `ddl-auto=validate`; `mvn test` verde antes de cada commit; commits PT atômicos + push (SEM merge).
2. Se sub-item já atendido no código: marcar como TAL no relatório (zero código). Se exigir spec nova: PARCIAL + REPORTAR (não improvisar).

## Relatório
```json
{
  "tarefa": "estoque E-1..E-7 (§6.2)",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS | FAIL | NAO_RODADO",
  "e1": false, "e2": false, "e3": false, "e4": false, "e5": false, "e6": false, "e7": false,
  "ja_atendidos_sem_codigo": [],
  "blockedReason": null,
  "duvidas": []
}
```

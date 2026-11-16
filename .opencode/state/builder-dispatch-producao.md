# Despacho Builder — PRODUCAO (§6.5 + Dívida 5, desbloqueia RH-treinamentos e Estoque-BOM)

> Data: 24/09/2026 · `producao-service` = scaffold (1 java). Specs existem: `.opencode/specs/features/producao-service/{requirements,plan,tasks}.md` (301 linhas) + `docs/06-Produção_&_Projetos_Service.md` + `docs/mokups/producao/requisitos.md` + `contract.gap.md` §5.5/§6.5 (leitura — NUNCA editar `docs/`).
> Base: **`dev/backend`** · work branch nova **`feature/producao-service-impl`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend`).
> Naming PT canônico (`com/fablab/producao/**`, recursos PT §5.5); porta/DB seguem padrão da spec; Eureka/Rabbit `fablab.*`; gateway `/api/producao/**` JÁ existe (verificar, zero código se ok).

## Escopo
1. Implementar `producao-service` conforme tasks.md da spec (projetos, tarefas, kanban, máquinas+uso/chamados/preventivas/histórico, setores, 5S: inspeções/pendências/ranking/penalidades/mesas/parâmetros). Regras §5.5: nota/cartão 5S calculados no backend; Top 1 = 1 semana livre; pendência grave bloqueia; penalidades sugeridas pelo sistema + aplicadas pelo Admin; chamado com relatório obrigatório; RBAC (operacional por responsável; 5S/penalidades/máquinas Admin; Recrutando fora do 5S).
2. Tarefas (dono real): expor conclusão de tarefa vinculada ao autenticado em `producao-service`; **NÃO quebrar** `PATCH /api/tasks/{id}` do dashboard (MVP em memória) — repoint da rota no gateway SOMENTE se contrato 100% compatível, senão REPORTAR como follow-up (não improvisar migração).
3. Dashboard (mínimo, mesmo despacho): filtrar campos `restricted` (`loansOpen`, `machinesActive`, `machinesByStatus`) no `GET /api/dashboard/summary` por papel; validar vínculo em `PATCH /tasks/{id}` (só tarefa do autenticado conclui).
4. Testes: `mvn test` verde por módulo (`producao-service` suite nova + `dashboard-service` sem regressão). Commits PT atômicos + push (SEM merge).

## Regras
1. Só `producao-service/` + `dashboard-service/` (+ gateway yml SOMENTE p/ repoint compatível ou rota faltante, após verificar).
2. Records+Bean Validation PT; Flyway `V1..Vn`; `ddl-auto=validate`; erros PT não-técnicos; sem libs novas sem justificativa no relatório.
3. Bloco sem spec ou com dependência externa (RH-treinamentos, Estoque-BOM) → PARCIAL + REPORTAR (não puxar outro service p/ dentro).

## Relatório
```json
{
  "tarefa": "producao §6.5 + Dívida 5",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS | FAIL | NAO_RODADO",
  "producao_suite": "PASS n/n | NA",
  "dashboard_regressao": "PASS | FAIL | NA",
  "tasks_repoint": "FEITO | REPORTADO_FOLLOWUP | NA",
  "blockedReason": null,
  "duvidas": []
}
```

# Despacho Builder — FINANCEIRO (§6.7 + §5.4, scaffold → spec)

> Data: 24/09/2026 · `financeiro-service` = scaffold (1 java). Specs existem: `.opencode/specs/features/financeiro-service/{requirements,plan,tasks}.md` + `docs/05-Financeiro_Service.md` + `docs/mokups/financeiro/requisitos.md` + `contract.gap.md` §5.4/§6.7 (leitura — NUNCA editar `docs/`).
> Base: **`dev/backend`** · work branch nova **`feature/financeiro-service-impl`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend`).
> Decisões travadas §5.4 (NÃO re-discutir): recursos PT (`/lancamentos`, `/categorias`, `/doacoes-recursos`, `/valores-hora`, `/parametros-overhead`, `/fechamento-encomenda`, `/solicitacoes-compra`, `/custos-encomenda`, `/relatorios/*`); **módulo exclusivo Admin (role 0)**; contas e KPIs são vistas derivadas de `GET /lancamentos` (totais servidos, nunca somados no client); status PT diretos sem tradução; fechamento congela + nova ordem/novo fechamento em alteração; compras informativas; valores/hora & overhead com auditoria.

## Escopo (§6.7)
1. Implementar `financeiro-service` per tasks.md: lançamentos, categorias, doações-recursos, valores-hora, overhead, fechamento/custos, solicitações-compra, 6 relatórios.
2. D-2: shapes servidos (`resumo`/`counts`/`totais`/`cobertura`/KPIs/séries mensais); front nunca deriva.
3. D-3 🔴: `@PreAuthorize` role 0 em TODOS os endpoints (Admin-only; sem exceção).
4. D-4 🟡: filtros combinados + `?periodo=` nos relatórios (assumidos — implementar o assumido, sem expandir).
5. D-5 🔴: congelamento (nova ordem + novo fechamento) + auditoria. D-7: erros PT não-técnicos.
6. D-6 export PDF = FORA DO MVP (front omite) → NÃO implementar, registrar. D-8 🟡: filtros `vencimento`/`origem` — implementar o assumido server-side (front aplica por página); se spec omissa, REPORTAR em vez de inventar semântica.
7. Gateway `/api/financeiro/**` JÁ existe (verificar, zero código se ok). Testes: suite nova `mvn test` verde. Commits PT + push (SEM merge).

## Regras
1. Só `financeiro-service/` (+ gateway yml SOMENTE se rota faltar, após verificar).
2. Records+Bean Validation PT; Flyway V1..; `ddl-auto=validate`; padrão dos demais services; sem libs novas sem justificativa.
3. Bloco subespecificado → PARCIAL + REPORTAR.

## Relatório
```json
{
  "tarefa": "financeiro §6.7",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS n/n | FAIL | NAO_RODADO",
  "d1": false, "d2": false, "d3": false, "d4": false, "d5": false,
  "d6": "FORA_MVP", "d7": false, "d8": "OK | REPORTADO",
  "blockedReason": null,
  "duvidas": []
}
```

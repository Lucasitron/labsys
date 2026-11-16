# Despacho Builder — VENDAS (§6.6 + §5.3, scaffold → spec)

> Data: 24/09/2026 · `vendas-service` = scaffold (1 java). Specs existem: `.opencode/specs/features/vendas-service/{requirements,plan,tasks}.md` + `docs/04-Vendas_&_CRM_Services.md` + `docs/mokups/vendas/requisitos.md` + `contract.gap.md` §5.3/§6.6 (leitura — NUNCA editar `docs/`).
> Base: **`dev/backend`** · work branch nova **`feature/vendas-service-impl`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend`).
> Decisões travadas §5.3 (NÃO re-discutir): recursos PT (`/clientes`, `/orcamentos`, `/encomendas`, `/marketplace`, `/interacoes`, `/tarefas-marketing`, `/tags`); sem rota `/vendas/interacoes` (timeline no detalhe do cliente); tela `/vendas/solicitacoes`; orçamento novo em página cheia; RBAC criação qualquer usuário / edição só criador+Admin / demais via solicitação; Kanban por clique (`PUT /encomendas/{id}/kanban` + `historico_status_encomenda`); encomenda nasce na Fila do orçamento aprovado; Kanban DONO ÚNICO Vendas.

## Escopo (§6.6)
1. Implementar `vendas-service` per tasks.md: clientes, orçamentos, encomendas, marketplace, tarefas-marketing (+solicitações — D-4: se contrato das solicitações estiver subespecificado, PARCIAL+REPORTAR em vez de improvisar).
2. D-2: shapes servidos (counts/totais); confirmar `POST /clientes/bulk-tag`, duplicar orçamento, extensão `item_orcamento` (só o especificado).
3. D-3 🔴: enforcement **criador+Admin** server-side (edição/mover/decidir só criador ou Admin; 403 p/ não-criador) + definição "responsável de Vendas" (se spec omissa, REPORTAR em vez de inventar papel novo).
4. D-5: extensão `item_orcamento` conforme spec. D-7: CPF/CNPJ + LGPD (máscara por padrão). D-8: erros PT não-técnicos. D-9: lock otimista do Kanban (409 em conflito).
5. D-6 export orçamentos = FORA DO MVP (front omite) → NÃO implementar, registrar como tal.
6. Gateway `/api/vendas/**` JÁ existe (verificar, zero código se ok). Testes: suite nova `mvn test` verde. Commits PT + push (SEM merge).

## Regras
1. Só `vendas-service/` (+ gateway yml SOMENTE se rota faltar, após verificar).
2. Records+Bean Validation PT; Flyway V1..; `ddl-auto=validate`; porta/DB/Eureka/Rabbit seguem padrão dos demais services; sem libs novas sem justificativa.
3. Bloco subespecificado → PARCIAL + REPORTAR (D-4, responsável de Vendas).

## Relatório
```json
{
  "tarefa": "vendas §6.6",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS n/n | FAIL | NAO_RODADO",
  "d1": false, "d2": false, "d3": false, "d4": "OK | REPORTADO | NA",
  "d5": false, "d6": "FORA_MVP", "d7": false, "d8": false, "d9": false,
  "blockedReason": null,
  "duvidas": []
}
```

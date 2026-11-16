# Despacho Builder — RH-D1-D10+H2 (§6.4 contract.gap, pós-Produção)

> Data: 24/09/2026 · `rh-service` tem 101 java (certificados/horas parciais). Produção existe agora (Dívida 5 quitada) → integrações D-9 liberadas.
> Base: **`dev/backend`** · work branch nova **`feature/rh-d1-d10`** (NUNCA commitar em `dev/frontend`; NUNCA mergear em `dev/backend`).
> Fonte da verdade: `docs/contract.gap.md` §6.4/§5.1/§3 (leitura) + código `rh-service` em `dev/backend` + `.opencode/specs/features/rh-service/` (leitura — NUNCA editar `docs/`).

## Triage minimal-code (verificar cada item; reaproveitar RBAC/handlers/entidades)
- **D-1**: naming PT canônico nos contratos novos (`/pessoas`, `/funcionarios`, `/apontamentos-horas`, `/treinamentos`, `/certificados`); sem alias EN novo.
- **D-2**: abas do detalhe servidas + vínculo com `POST /auth/users` (confirmar shape; só codar o gap).
- **D-3** 🔴: enforcement server-side + escopo "próprio registro" (Estagiário/Recrutando só o próprio).
- **D-4** 🔴: processo seletivo — grupos, etapa individual, avaliar (tutor, não instrutor — §5.1).
- **D-5** 🔴/🟡: horas — `GET /horas/disponiveis`, `GET/POST /apontamentos-horas`, `PUT /{id}/validar`, rejeitar, `ExtratoMensalHorasEvent` (reaproveitar o que já existe).
- **D-6**: certificados = futuro, front sem telas → VERIFICAR e pular sem código se for o caso.
- **D-7**: mapear token → pessoa (via `id_user` do JWT; degradar vazio se sem vínculo, padrão §8.5).
- **D-8** 🔴: níveis (`GET /niveis`, `PATCH /niveis/{id}/membros`, convites).
- **D-9** 🔴/🟡: treinamentos/agenda/guias/sessão + integrações `producao-service` (projects/machines/tasks — AGORA EXISTEM; consumir server-side, nunca no client).
- **D-10**: erros não-técnicos PT.
- **Extras**: confirmar `DELETE /api/rh/pessoas/{id}`; **H2** autorizar cada escrita por papel/titularidade; recalcular `hours` de `startTime/endTime` (ignorar o enviado); CPF: confirmar necessidade/retenção/máscara (LGPD — se spec omissa, máscara por padrão + REPORTAR); rejeitar `nivel:'admin'` de autor não-admin (403).

## Regras
1. Só `rh-service/`. Records+Bean Validation PT; `ddl-auto=validate`; `mvn test` verde antes de cada commit; commits PT atômicos + push (SEM merge).
2. Item já atendido → sem código. Item sem spec → PARCIAL + REPORTAR. `searchPeople` EN→PT é código FRONTEND — fora deste despacho (só registrar).

## Relatório
```json
{
  "tarefa": "rh D-1..D-10+H2 (§6.4)",
  "status": "IMPLEMENTADO | PARCIAL | BLOQUEADO",
  "resultado_mvn_test": "PASS | FAIL | NAO_RODADO",
  "d1": false, "d2": false, "d3": false, "d4": false, "d5": false, "d6": "SKIP-FUTURO",
  "d7": false, "d8": false, "d9": false, "d10": false, "h2": false,
  "ja_atendidos_sem_codigo": [],
  "blockedReason": null,
  "duvidas": []
}
```

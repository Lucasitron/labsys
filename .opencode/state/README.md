# state/ — estado vivo + registros de execução

## Estado vivo (normativo)
- `orchestration.json` — estado do pipeline **backend** (fases, features, `regras_agentes`,
  `politica_modelos`, `relatorios`, `resumo_cache`, `features_futuras`).
- `frontend-orchestration.json` — estado do pipeline **frontend**.

## Registros de execução (NÃO normativos)
São históricos de despachos/etapas já executadas — **não** são fonte de verdade nem contrato:

- `*-dispatch*.md` — briefings despachados a agentes (builder/planner/validator/security/designer).
- `*-dispatch*.md` de `builder-*` usam a nomenclatura antiga: **"builder" = implementador de código**
  (hoje = `coder`). Ver `.opencode/specs/system/agents-pipeline.md`.
- `*-checklist.md`, `audit-sweep-*.md`, `contract-*.md`, `*-review-*.md` — auditorias/varreduras pontuais.

> Estado atual e linha do tempo: `.opencode/specs/system/STATUS.md`.
> Relatórios por etapa: `.opencode/reports/`.

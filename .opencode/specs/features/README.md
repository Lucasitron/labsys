# specs/features/ — specs de feature (registro)

> **Estas specs são históricas/operacionais, não a fonte do estado atual.**

- O **estado atual e a linha do tempo** ficam em `../system/STATUS.md`.
- A **norma de arquitetura/convenções/RBAC/agentes** fica em `../system/`.
- Specs com sufixo `*-service` (`rh-service`, `estoque-service`, …) usam IDs **históricos** de
  feature (do tempo dos microsserviços) — **não são unidades de deploy**. Hoje tudo vive em `backend/`.
- O termo **"builder"** em textos antigos significa "agente que implementa código"; na taxonomia
  atual o implementador é o **`coder`** e `builder` é o agente de **build** (ver
  `../system/agents-pipeline.md`).
- `_infra-agentes/` e `_migracao-monolito/` são registros de etapas de infraestrutura já concluídas.

Nada aqui deve ser publicado como contrato sem cruzar com `docs/contract.gap.md` e o código real.

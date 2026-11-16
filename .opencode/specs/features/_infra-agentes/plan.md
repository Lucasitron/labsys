# Plan — Padronização de modelo dos agentes + reconciliação do state (monólito modular MVC)

> Feature de infra: `_infra-agentes`. Mudança de **configuração**, sem código de negócio.
> Fonte da verdade: `.opencode/specs/system/architecture.md` (monólito modular MVC).
> Regras: nunca alterar `docs/`; mover-não-reescrever; reuse > create.
>
> ⚠️ **HISTÓRICO/DESATUALIZADO (2026-10-19):** o modelo `opencode/muse-spark-1.3-contributor-free`
> **não existe mais** (causava "Model not found"). Os agentes foram reconfigurados em
> `.opencode/specs/system/agents-pipeline.md` (política atual: `deepseek`/`big-pickle`/`glm`/`beam`).
> Este plan/tasks descreve a padronização antiga — mantido apenas como registro.

## Objetivo
1. Padronizar 100% dos agentes backend+frontend no modelo
   `opencode/muse-spark-1.3-contributor-free` (antes `opencode/big-pickle` em 11 arquivos;
   2 arquivos sem chave `model`).
2. Alinhar `.opencode/state/orchestration.json` ao monólito (campo `arquitetura` dizia
   `microsservicos / banco 1 por serviço`), preservando features/status/histórico.

## Estratégia (passos executados)
1. **A1 — Inventário:** listar `.opencode/agents/*.md` (6 arquivos) e
   `.opencode/agents/frontend/*.md` (7 arquivos); `grep -rn "model:"` como prova.
2. **A2 — Troca mecânica:** `model: opencode/big-pickle` →
   `model: opencode/muse-spark-1.3-contributor-free` nos 11 arquivos com a chave.
   Preservar `mode`, `temperature: 0.1`, `permission`, descrição e corpo intactos.
3. **A3 — Arquivos sem frontmatter** (`frontend/orchestrator.md`, `frontend/designer.md`):
   prepend de bloco mínimo `---\nmodel: ...\n---`, sem reescrever o corpo.
4. **B1 — State:** reescrever só o objeto `arquitetura`
   (estrategia `monolito-modular-mvc`, linguagem, banco único `fablab` c/ schema por
   módulo, deploy `um backend/ porta 8080`, removidos `Eureka/Gateway/banco-por-servico`
   + `referencia` e `nota` explicando que chaves `*-service` são IDs históricos).
5. **B2 — Anotação:** nova chave `monolito_modular_mvc` em `contract_gap_fila`
   (nenhuma nota concluída apagada/alterada).
6. **Verificação:** greps + `python3 -m json.tool` + `git status --short`.

## Decisões de enxugamento
- **Reusado:** frontmatter existente dos 11 arquivos (só o valor de `model` mudou);
  objeto `orchestration.json` inteiro preservado (só `arquitetura` + 1 chave de fila);
  chaves `services`/`features` com sufixo `*-service` **mantidas** — são IDs históricos
  amarrados a dirs em `specs/features/` e notas concluídas; renomeá-las seria reescrita,
  não migração.
- **NÃO planejado (fora de escopo, sem tarefa):**
  - `description:`/`mode:` nos 2 arquivos frontend sem frontmatter (só `model`, bloco mínimo).
  - Renomear dirs `specs/features/*-service` → módulos (histórico; zero benefício funcional).
  - Migrar `frontend-orchestration.json` ou dispatches em `state/` (não citado no despacho).
  - Qualquer endpoint, evento, schema ou camada nova — mudança é só config.

## Riscos / ressalvas
- Bloco frontmatter só-com-`model` nos 2 arquivos frontend é formato inédito no repo;
  se o loader exigir `description`/`mode`, completar depois (tarefa futura, não desta mudança).
- Repo com tudo untracked (`git status` inicial); revisão humana antes de qualquer commit.

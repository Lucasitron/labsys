# Etapa 0001 — Infra de agentes: modelos válidos, papéis exclusivos, relatórios e código mínimo

- **Módulo/Feature:** `_infra-agentes` (configuração de agentes opencode — back e front)
- **Escopo:** `ambos` (configuração; sem código de negócio)
- **Branch/Worktree:** `dev/frontend` (árvore principal) — `.opencode/**` é untracked
- **Agentes envolvidos:** orchestrator (esta execução)
- **Data:** 2026-10-19
- **Referências:** `.opencode/specs/system/agents-pipeline.md`, `.opencode/specs/features/_infra-agentes/{plan,tasks}.md`, `.opencode/state/orchestration.json`

## Objetivo da etapa
Reconfigurar o ecossistema de agentes opencode conforme a diretiva do usuário: corrigir modelos
inválidos, separar papéis (build/deploy exclusivos), criar agentes faltantes (coder/tester/contracts),
instituir relatórios MD por etapa, cobrar **código mínimo** e mover `temAcesso` para feature futura.

## Diário de bordo dos agentes

### orchestrator
- **Feito:**
  - Diagnóstico: todos os 17 agentes usavam `opencode/muse-spark-1.3-contributor-free`, **inexistente**
    (causa do "Model not found"). IDs válidos confirmados via `opencode models`.
  - Raiz reescrita: `orchestrator` (deepseek + duty de relatórios + exclusividade), `planner` (deepseek,
    enxugamento), `builder` → **agora build-only** (deepseek), **novo** `coder` (big-pickle),
    `deployer` (deepseek, deploy-only), **novo** `tester` (deepseek), `validator` (deepseek, 4 eixos),
    `security` (deepseek), **novo** `contracts` (deepseek).
  - Frontend: `orchestrator`/`planner`/`validator`/`security`/`tracker` em deepseek; `frontend/builder`
    renomeado p/ **`frontend/coder`** (big-pickle); **novo** `frontend/tester` (deepseek); `designer`
    (big-pickle, frontmatter completado com description/mode/permission).
  - Criados `.opencode/reports/{README.md,_TEMPLATE.md}` e `.opencode/specs/system/agents-pipeline.md`.
  - `orchestration.json`: `regras_agentes` (exclusividade + papéis), `politica_modelos`,
    `relatorios`, `resumo_cache`, `features_futuras.temAcesso`.
  - `frontend-orchestration.json`: nota `nota_agentes_2026_10_19`.
  - Spec histórica `_infra-agentes/plan.md` marcada como DESATUALIZADA.
- **Decisões:** `builder` = BUILD (não "implementa"); escrita de código em decisão de build (não uso);
  atualizações apenas no final para evitar corrida entre os dois editores concorrentes.
- **Evidências:** `grep -rn "^model:"` → 17 arquivos com modelos válidos; ambos os JSONs parseiam.
- **Dúvidas/Bloqueios:** assumi a leitura de `builder`=build. Se o usuário queria `builder`=código (com
  `coder` para outra coisa), é fácil reverter.

## Ponderação do orquestrador
- **Aceito:** a separação de papéis e a criação de `tester`/`contracts` fecham lacunas reais do pipeline
  (havia build/deploy sem dono único e validação sem execução de testes como etapa própria).
- **Ressalvas/Devoluções:** a nomenclatura `builder` é ambígua (histórico do repo usava "builder" como
  implementador). Mantive o histórico rastreável (nota no estado + spec marcada). Ponto a confirmar com o PO.
- **Decisões:** registrei a política de modelos em dois lugares (spec `agents-pipeline.md` + estado)
  para que qualquer agente a leia sem reaprender; a spec é a fonte normativa, o estado é o cache.

## Checklist

### Concluído
- [x] 17 agentes com modelo válido (0 `muse-spark`)
- [x] Exclusividade build (`builder`) / deploy (`deployer`) registrada nos agentes e no estado
- [x] Agentes novos: `coder`, `tester`, `contracts`, `frontend/coder`, `frontend/tester`
- [x] `validator` com 4 eixos (persistência, escalabilidade, arquitetura, funcionalidade)
- [x] Regra de código mínimo embutida em planner/coder/validator/security (back e front)
- [x] Padrões de frontend (sem resquícios, mensagens padronizadas, sem expor código/banco)
- [x] `.opencode/reports/` com README + template
- [x] `orchestration.json` e `frontend-orchestration.json` atualizados e válidos (JSON OK)
- [x] `temAcesso` registrado como feature futura (não implementada)

### Falta concluir
- [ ] **Usuário:** reiniciar o opencode para recarregar os agentes (frontmatter novo) — responsável: humano
- [ ] Confirmar a semântica `builder`=build vs `builder`=código — responsável: humano
- [ ] (Fora desta etapa) desabilitar `fablab-front.service` (4173) no servidor — requer sudo —
      responsável: humano/deployer

## Resumo cache (para `orchestration.json`)
```
INFRA_AGENTES_2026-10-19: modelos validos (deepseek/big-pickle/glm/beam; muse-spark removido) + builder=BUILD-only/deployer=DEPLOY-only + coder/tester/contracts + validator 4 eixos + codigo minimo + relatorios MD (.opencode/reports/) + temAcesso futuro. 17 agentes validados; JSONs OK.
```

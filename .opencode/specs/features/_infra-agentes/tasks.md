# Tasks — Padronização de modelo dos agentes + reconciliação do state

> Convenção por task: descricao · escopo (módulo + camada) · entradas · saídas ·
> critérios_de_conclusão (checklist) · dependencias · prioridade.
> Escopo desta feature: módulo `infra/agentes`, camada `config`; state: módulo `infra`, camada `state`.

## T1 — Inventariar agentes e ocorrências de modelo
- **descricao:** Listar todos os `.md` em `.opencode/agents/` e `.opencode/agents/frontend/`
  e registrar ocorrências de `model:` / `big-pickle`.
- **escopo:** módulo `infra/agentes`, camada `config` (leitura).
- **entradas:** `.opencode/agents/*.md`, `.opencode/agents/frontend/*.md`.
- **saidas:** inventário (13 arquivos; 11 com `model: opencode/big-pickle`; 2 sem `model`).
- **criterios_de_conclusao:**
  - [ ] `ls` cobre os 13 arquivos (6 + 7).
  - [ ] `grep -rn "model:" .opencode/agents/` documentado antes da troca.
- **dependencias:** nenhuma.
- **prioridade:** ALTA.

## T2 — Trocar `model` nos 11 arquivos com a chave
- **descricao:** `model: opencode/big-pickle` →
  `model: opencode/muse-spark-1.3-contributor-free` em: `planner, orchestrator, builder,
  validator, security, deployer` (raiz) + `frontend/{planner, builder, validator, security,
  tracker}`. Só o valor da chave muda.
- **escopo:** módulo `infra/agentes`, camada `config` (`modify`, 11 arquivos).
- **entradas:** os 11 arquivos (frontmatter existente).
- **saidas:** 11 arquivos com `model: opencode/muse-spark-1.3-contributor-free`;
  `mode`, `temperature: 0.1`, `permission`, descrição e corpo intactos.
- **criterios_de_conclusao:**
  - [ ] `grep -rn "big-pickle" .opencode/agents/` retorna vazio.
  - [ ] Diff de cada arquivo mostra só a linha `model:` alterada.
- **dependencias:** T1.
- **prioridade:** ALTA.

## T3 — Padronizar os 2 arquivos sem `model`
- **descricao:** Prepend de frontmatter mínimo
  (`---` + `model: opencode/muse-spark-1.3-contributor-free` + `---`) em
  `frontend/orchestrator.md` e `frontend/designer.md`, sem reescrever o corpo.
- **escopo:** módulo `infra/agentes`, camada `config` (`modify`, 2 arquivos).
- **entradas:** `frontend/orchestrator.md`, `frontend/designer.md` (sem frontmatter).
- **saidas:** ambos com chave `model` padronizada; corpo byte-idêntico após o bloco.
- **criterios_de_conclusao:**
  - [ ] `grep -rn "muse-spark-1.3-contributor-free" .opencode/agents/ | wc -l` >= 13.
  - [ ] Corpo dos 2 arquivos inalterado (diff só mostra o bloco adicionado).
- **dependencias:** T1.
- **prioridade:** ALTA.

## T4 — Reconciliar `arquitetura` no `orchestration.json`
- **descricao:** Reescrever só o objeto `arquitetura`: estrategia `monolito-modular-mvc`,
  linguagem `Java 21 + Spring Boot 3`, banco `PostgreSQL unico fablab com schema por
  modulo`, deploy `um backend/ porta 8080`, removidos `Eureka/Gateway/banco-por-servico`
  (+ `referencia` a `architecture.md` e `nota` sobre IDs históricos `*-service`).
  Adicionar chave `monolito_modular_mvc` em `contract_gap_fila`; resto intacto.
- **escopo:** módulo `infra`, camada `state` (`modify`, 1 arquivo).
- **entradas:** `.opencode/state/orchestration.json`,
  `.opencode/specs/system/architecture.md`.
- **saidas:** `orchestration.json` com `arquitetura` do monólito; histórico preservado.
- **criterios_de_conclusao:**
  - [ ] `python3 -m json.tool .opencode/state/orchestration.json > /dev/null` (JSON válido).
  - [ ] Zero ocorrência de `microsservicos` no campo `arquitetura`.
  - [ ] Demais campos (`features`, `contract_gap_fila.*` pré-existentes, `proximos_passos`) intactos.
- **dependencias:** nenhuma (paralela a T2/T3).
- **prioridade:** ALTA.

## T5 — Verificação final e relatório
- **descricao:** Rodar as 4 verificações obrigatórias e emitir relatório PT ao Orquestrador.
  Não commitar.
- **escopo:** módulo `infra`, camada `state` (leitura/verificação).
- **entradas:** working tree após T2–T4.
- **saidas:** relatório (arquivos alterados, contagens, saídas dos comandos, ressalvas).
- **criterios_de_conclusao:**
  - [ ] `grep -rn "big-pickle" .opencode/agents/` vazio.
  - [ ] Contagem `muse-spark-1.3-contributor-free` >= 13.
  - [ ] JSON válido.
  - [ ] `git status --short` só com arquivos pretendidos.
- **dependencias:** T2, T3, T4.
- **prioridade:** ALTA.

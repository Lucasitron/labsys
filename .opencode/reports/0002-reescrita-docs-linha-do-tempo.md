# Etapa 0002 — Reescrita dos docs: descarte do histórico inconsistente + Ponto 1 da linha do tempo

- **Módulo/Feature:** documentação/specs (configuração — sem código de negócio)
- **Escopo:** `ambos`
- **Branch/Worktree:** `dev/frontend` (árvore principal) — `docs/` e `.opencode/**` não versionados
- **Agentes envolvidos:** orchestrator (esta execução)
- **Data:** 2026-10-19
- **Referências:** `.opencode/specs/system/STATUS.md`, `.opencode/backups/MANIFEST.md`

## Objetivo da etapa
Descartar as inconsistências históricas da documentação (arquitetura de microsserviços Java/Spring,
termo "Builder" = implementador, modelo `muse-spark`) mantendo o padrão atual, e **fixar o status
atual como Ponto 1 da linha do tempo** — com backup da documentação anterior.

## Diário de bordo dos agentes

### orchestrator
- **Feito:**
  - **Backup:** `.opencode/backups/2026-10-19-pre-rewrite.tar.gz` (915 KB, 276 entradas; `docs/` +
    `.opencode/{specs,state,agents}` + `README.md`) + `MANIFEST.md` com SHA-256.
  - **Arquivamento (opção B):** `docs/00–08`, `docs/frontendDatails.md`, `docs/prompts/`, e as versões
    antigas de `GITFLOW.md`/`contract.gap.md` → `docs/_archive/2026-10-19/` (24 arquivos) + `_archive/README.md`.
  - **Substitutos enxutos:** `docs/README.md` (índice canônico atual), `docs/GITFLOW.md` (branches atuais,
    monólito Laravel), `docs/contract.gap.md` (contrato Backend × Frontend vigente, PT canônico,
    RBAC, decisões duráveis).
  - **`README.md` da raiz** reescrito para o stack atual (Laravel + SvelteKit).
  - **Ponto 1:** criado `.opencode/specs/system/STATUS.md` (linha do tempo com o estado atual).
  - **Purga do AS-IS** em specs normativas: `system/architecture.md` (removida a seção "Migração
    AS-IS → TO-BE" e a lista de removidos; header aponta o arquivo), `system/conventions.md`,
    `system/rbac-matrix.md`, `specs/frontend/{architecture,conventions,design-system}.md` — todas
    com refs a `docs/0X`/`docs/prompts` trocadas por fontes atuais/arquivo.
  - **Registros históricos** delimitados: `.opencode/specs/features/README.md` e `.opencode/state/README.md`
    (deixam claro que `*-dispatch.md`/"builder" antigo são históricos, não normativos).
  - **`orchestration.json`**: novo `linha_do_tempo` + `resumo_cache` atualizado.
- **Decisões:** escolhida a **opção B** (arquivar + substituto enxuto) — evita reescrever 3.700 linhas
  de intenção do PO e preserva a sequência no snapshot. `state/`/`features/` **não** reescritos, só rotulados.
- **Evidências:** `grep` de caminhos arquivados nas normativas = vazio; `orchestration.json` parseia;
  `docs/` ativo = `{README,contract.gap,GITFLOW}.md + mokups/ + _archive/`.
- **Dúvidas/Bloqueios:** nenhum.

## Ponderação do orquestrador
- **Aceito:** a separação "ativo vs. arquivo" resolve a inconsistência sem destruir o contexto do PO;
  o snapshot torna a operação reversível.
- **Ressalvas/Devoluções:** os `specs/features/*` (ex.: `vendas/{plan,tasks}.md`) ainda citam `docs/04`
  e "§5.3" — são registro histórico de features concluídas; mantidos por decisão, com o aviso em
  `features/README.md`. Se o PO quiser, faço uma nova passada de limpeza neles.
- **Decisões:** `docs/mokups/**` permaneceu (verdade visual atual); o par `docs/contract.gap.md` manteve
  o caminho para não quebrar refs dos agentes.

## Checklist

### Concluído
- [x] Backup com checksum (`MANIFEST.md`)
- [x] Docs PO arquivados (`docs/_archive/2026-10-19/`, 24 arquivos)
- [x] Substitutos: `docs/README.md`, `GITFLOW.md`, `contract.gap.md`; `README.md` raiz
- [x] `STATUS.md` = linha do tempo (Ponto 1 = 2026-10-19)
- [x] Specs normativas sem refs obsoletas (microsserviços/`docs/0X`)
- [x] `state/`/`features/` rotulados como histórico (não reescritos)
- [x] `orchestration.json` atualizado e válido

### Falta concluir
- [ ] (Opcional) limpar refs históricas dentro de `.opencode/specs/features/*` — responsável: `orchestrator`
- [ ] (Opcional) remover os diretórios residuais `*-service/`/`web/` e arquivar os git-tracked
      (`auth-service`, `discovery-service`, `gateway-service`) — requer decisão do PO (mexe no git)
- [ ] Reiniciar o opencode para recarregar agentes — responsável: humano

## Resumo cache (para `orchestration.json`)
```
DOCS_REESCRITA_2026-10-19: docs PO (00-08, prompts/, frontendDatails) + GITFLOW/contract.gap antigos -> docs/_archive/2026-10-19/; backup .opencode/backups/2026-10-19-pre-rewrite.tar.gz; substitutos enxutos docs/{README,contract.gap,GITFLOW}.md + README raiz; specs normativas purgadas do AS-IS (microsservicos/Java); STATUS.md = Ponto 1 da linha do tempo; state/features rotulados historicos.
```

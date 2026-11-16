# Etapa 0005 — Propagação GitFlow (back → development → front)

- **Módulo/Feature:** repositório/estrutura
- **Escopo:** `ambos`
- **Agentes envolvidos:** orchestrator (execução)
- **Data:** 2026-10-19

## Objetivo
Executar, em sequência, os merges `dev/backend → development` e `development → dev/frontend`
(propagação que estava adiada), levando a remoção do legado Java e a entrega de RH vínculo/acesso.

## Diário de bordo dos agentes

### orchestrator
- **Dry-run antes de tocar:** `git merge-tree --write-tree` nos dois pares.
  - `dev/backend → development`: **0 conflitos**.
  - `development → dev/frontend`: **1 conflito** (`README.md`).
- **Merge 1** (`dev/backend → development`): executado em worktree próprio (`05-dev/labSys-dev`),
  commit `0eeab43`. Resultado: raiz `backend/ frontend/ infra/`, **0** `*-service`.
- **Merge 2** (`development → dev/frontend`): `backend/` estava **untracked** (153M, com `vendor/`,
  `.env`, `storage/`) e o merge o sobrescreveria. Afastado para `05-dev/labSys-be-untracked`, merge
  feito, conflito de `README.md` resolvido com **ours** (o README reescrito para o monólito), commit
  `6228ba9`. Extras devolvidos com `rsync --ignore-existing` (`.env`, `vendor/`, `storage/`).
- **Verificação:** raiz `backend/ docs/ frontend/ infra/`; `*-service` = 0; `git status` só com untracked.
- **Intercorrências:**
  - `/tmp` foi limpo 2× pelo ambiente → worktrees movidos para `05-dev/` (persistente).
  - Ao apagar o backup temporário, `vendor/` tinha **539 entradas root-owned** (build Docker de 06/out)
    → `rm` sem sudo falhou; sobraram **2,8M** em `05-dev/labSys-be-untracked`. Precisa de `sudo rm -rf`.
  - `remap.md` (56 KB, untracked, na raiz) não é deste fluxo — não foi tocado nem commitado.
- **Dúvidas:** push das duas branches (não executado).

## Ponderação do orquestrador
- **Aceito:** merges limpos, conflito único e de baixo risco (README), resolução coerente com a
  reescrita da Etapa 0002.
- **Ressalvas:** (a) o `backend/` passa a ser **tracked** no `dev/frontend` após o merge (consequência
  natural da propagação — antes era untracked, o que era uma inconsistência); (b) os 2,8M root-owned
  ficaram para limpeza manual por exigirem privilégio.
- **Decisão:** não houve push (aguardando instrução).

## Checklist

### Concluído
- [x] Dry-run dos dois merges
- [x] `dev/backend → development` (`0eeab43`)
- [x] `development → dev/frontend` (`6228ba9`) com `README.md` = ours
- [x] `backend/` (com `.env`/`vendor`/`storage`) preservado e agora tracked
- [x] Legado Java ausente em toda a cadeia
- [x] STATUS/orchestration sincronizados

### Falta concluir
- [ ] `git push` de `development` e `dev/frontend` — decisão do PO
- [ ] `sudo rm -rf /home/lucasitron/Documentos/05-dev/labSys-be-untracked` (2,8M root-owned)
- [ ] Reiniciar o opencode para carregar `beam-free` nos coders — responsável: humano

## Resumo cache
```
MERGE_2026-10-19: dev/backend->development 0eeab43 (0 conflitos) e development->dev/frontend 6228ba9 (conflito README resolvido com ours); backend/ agora tracked no front; legado Java zero em toda a cadeia; sem push; residual root-owned 2,8M em 05-dev/labSys-be-untracked precisa sudo.
```

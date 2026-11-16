# Etapa 0003 — Limpeza de refs em specs/features + remoção do legado Java/microsserviços

- **Módulo/Feature:** documentação/specs + estrutura do repositório (configuração)
- **Escopo:** `ambos`
- **Branch/Worktree:** `dev/frontend` (árvore principal) + `dev/backend` (`/home/lucasitron/Documentos/05-dev/labSys-back`)
- **Agentes envolvidos:** orchestrator (esta execução)
- **Data:** 2026-10-19

## Objetivo da etapa
Fechar as duas pendências da Etapa 0002: (2) limpar referências a caminhos arquivados em
`.opencode/specs/features/*`; (3) remover os diretórios residuais de microsserviços
(`*-service/`, `web/`) e arquivar o que era versionado.

## Diário de bordo dos agentes

### orchestrator
- **Feito:**
  - **(2)** Substituídas 63 referências a `docs/0[0-8]*` e `docs/prompts/*` por
    `docs/_archive/2026-10-19/...` em 13 arquivos de `.opencode/specs/features/`. Verificação: **0** refs
    órfãs restantes. `docs/contract.gap.md` foi mantido (o arquivo atual continua válido).
  - **(3)** Descoberta relevante: o `infra/` da árvore principal (`dev/frontend`) ainda era o **antigo**
    (overlays `docker-compose.{dev,prod,testing}.yml` referenciando `../*-service`) — o `infra/`
    monólito estava apenas em `dev/backend`.
  - **Arquivamento:** `.opencode/backups/2026-10-19-legado-java.tar.gz` (67 KB, 570 entradas — só fontes,
    `target/` excluído) + entrada no `MANIFEST.md` (SHA-256).
  - **Remoção:** `git rm -r` dos 9 `*-service/` versionados + `rm -rf` de `dashboard-service/` e `web/`
    (untracked) **nas duas branches ativas** (`dev/frontend` e `dev/backend`).
  - **Infra:** `infra/` da árvore principal alinhado ao monólito (espelhado de `dev/backend`):
    removidos `infra/{eureka,gateway,rabbitmq}` e os overlays antigos; `docker-compose.{yml,dev,prod,testing}.yml`
    agora são os do monólito (+ `infra/.env.example`).
- **Decisões:** remover em ambas as branches evita reintrodução no próximo merge; o snapshot + histórico
  do git tornam a operação reversível.
- **Commits:** `dev/frontend` `a2db21e` (323 arquivos: 309 deleções + `README.md` + `infra/**` +
  `infra/.env.example`) e `dev/backend` `c76d7f7` (957 deleções). **Não** houve push (aguardando instrução).
- **Evidências:** raiz da árvore = `backend/ docs/ frontend/ infra/`; `ls -d *-service` → 0; deleções
  staged: `dev/frontend` 309, `dev/backend` 957; `orchestration.json`/`MANIFEST.md` atualizados.
- **Dúvidas/Bloqueios:** commit das deleções (duas branches) precisa de decisão do PO.

## Ponderação do orquestrador
- **Aceito:** a limpeza fecha a migração (nada de Java/microsserviços no tree) e corrige uma
  inconsistência que passaria batido (infra antiga na branch de front).
- **Ressalvas/Devoluções:** remover o legado em `dev/frontend` viola "à risca" o GitFlow (back/infra
  pertencem a `dev/backend`). Mitiguei removendo **também** em `dev/backend` — a branch dona — e
  espelhando o `infra/` monólito, então o estado é coerente nas duas.
- **Decisões:** manter as deleções **staged e não commitadas**; commitar é decisão do PO (mensagem sugerida:
  `chore(repo): remove legacy Java microservices scaffolds and old infra`).

## Checklist

### Concluído
- [x] (2) 0 refs a caminhos arquivados em `specs/features/*`
- [x] (3) Legado Java arquivado (67 KB, checksum no `MANIFEST.md`)
- [x] (3) `*-service/` + `web/` removidos em `dev/frontend` e `dev/backend`
- [x] (3) `infra/` da árvore principal alinhado ao monólito
- [x] Commitado: `dev/frontend` `a2db21e` · `dev/backend` `c76d7f7`
- [x] Estado/relatórios atualizados

### Concluído (cont.)
- [x] `git push` das duas branches (`dev/frontend` `a2db21e`, `dev/backend` `c76d7f7`, upstream configurado)
- [x] Worktree `dev/backend` movido para caminho persistente (`/home/lucasitron/Documentos/05-dev/labSys-back`); o `/tmp` é volátil e apagou o worktree 2×.

### Falta concluir
- [ ] **Propagação adiada por decisão do PO (opção 2):** `development` está 54 commits atrás do `dev/backend` e 75 do `dev/frontend`; merge pode conflitar (modify/delete nos `*-service`).
- [ ] Reiniciar o opencode para recarregar agentes — responsável: humano

## Resumo cache (para `orchestration.json`)
```
LIMPEZA_2026-10-19: (2) refs a docs/0X|prompts em specs/features -> docs/_archive/2026-10-19 (0 orfas); (3) legado Java removido (*-service/ + web/) em dev/frontend e dev/backend + infra/ da principal alinhado ao monolito; backup .opencode/backups/2026-10-19-legado-java.tar.gz; delecoes staged NAO commitadas (front 309 / backend 957).
```

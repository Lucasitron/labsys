# Relatórios por etapa (FabLab)

Uma etapa do pipeline (backend ou frontend) = **um arquivo Markdown** nesta pasta.

## Convenção de nome
```
<NNNN>-<slug>.md
```
- `NNNN` — sequencial com 4 dígitos, começando em `0001` (maior número existente + 1).
- `slug` — curto e estável (ex.: `auth-login`, `rh-vinculo-acesso`, `estoque-entradas`).

## Quem escreve
- O **orquestrador** cria o arquivo a partir de `_TEMPLATE.md` ao **iniciar** a etapa e o
  **fecha** ao concluir (ou marcar bloqueio).
- Cada agente que atua escreve a sua seção **Diário de bordo** (o que fez, decisões,
  evidências, dúvidas). O orquestrador **não** reescreve o diário alheio — apenas pondera.

## Conteúdo obrigatório (nesta ordem)
1. **Cabeçalho** — etapa, módulo/feature, branch/worktree, agentes envolvidos, data.
2. **Diário de bordo dos agentes** — uma subseção por agente.
3. **Ponderação do orquestrador** — análise crítica: o que aceitou, rebateu ou devolveu.
4. **Checklist** — itens **Concluído** e itens **Falta concluir** (com responsável/próximo passo).

## Regras
- **Fonte de verdade continua sendo** `.opencode/specs/` e `.opencode/state/`; o relatório é
  narrativa/auditoria, não substitui specs/estado.
- Não colar segredos (senhas, PAT, `APP_KEY`, tokens, `.env`).
- Resumo compacto também vai para `state/orchestration.json` (`resumo_cache`) para não inflar contexto.
- Ver also `orchestration.json` (`relatorios`/`regras_agentes`) e `specs/system/agents-pipeline.md`.

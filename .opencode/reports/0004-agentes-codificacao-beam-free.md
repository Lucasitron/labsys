# Etapa 0004 — Modelo dos agentes de codificação → `beam-free`

- **Módulo/Feature:** configuração do pipeline de agentes
- **Escopo:** `ambos`
- **Agentes envolvidos:** orchestrator (execução)
- **Data:** 2026-10-19

## Objetivo
Definir os **agentes de codificação** (`coder` e `frontend/coder`) para o modelo `opencode/beam-free`.

## Diário de bordo dos agentes

### orchestrator
- **Feito:**
  - `.opencode/agents/coder.md`: `opencode/big-pickle` → `opencode/beam-free`.
  - `.opencode/agents/frontend/coder.md`: `opencode/big-pickle` → `opencode/beam-free`.
  - Sincronizado o estado: `.opencode/specs/system/agents-pipeline.md` (tabelas raiz/frontend) e
    `.opencode/state/orchestration.json` (`politica_modelos.designacao.{coder,frontend/coder}`).
  - Corrigida a regra de escrita de código, que ainda citava `muse spark` (modelo inexistente):
    agora "padrão `beam free`; alternativas `glm`/`big pickle`".
- **Decisão:** `frontend/designer` **não** foi alterado (permanece `big-pickle`) — seu papel é mockup
  HTML/Tailwind, não implementação Svelte/backend. Se o PO quiser, troco também.
- **Evidências:** `grep -rn "big-pickle" .opencode/agents/` → só `frontend/designer.md`; JSON válido.
- **Dúvidas:** confirmar se `designer` deve entrar como "codificação".

## Ponderação do orquestrador
- **Aceito:** troca aplicada aos dois agentes que efetivamente escrevem código de feature.
- **Ressalva:** manter o `designer` em `big-pickle` evita mudar escopo sem pedido explícito.

## Checklist

### Concluído
- [x] `coder` = `opencode/beam-free`
- [x] `frontend/coder` = `opencode/beam-free`
- [x] `agents-pipeline.md` e `orchestration.json` sincronizados
- [x] Regra de escrita sem `muse spark`

### Falta concluir
- [ ] Reiniciar o opencode para carregar o novo modelo — responsável: humano
- [ ] (Opcional) definir `frontend/designer` como `beam-free` — decisão do PO

## Resumo cache (para `orchestration.json`)
```
AGENTES_2026-10-19b: coder + frontend/coder -> opencode/beam-free; designer permanece big-pickle; agents-pipeline.md e politica_modelos sincronizados; restart necessario.
```

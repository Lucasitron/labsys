# Etapa <NNNN> — <título curto>

- **Módulo/Feature:** `<modulo>/<feature>`
- **Escopo:** `backend` | `frontend` | `ambos`
- **Branch/Worktree:** `dev/backend` | `dev/frontend` (<caminho>)
- **Agentes envolvidos:** orchestrator, planner, coder, tester, validator, security, contracts, builder, deployer (os que atuaram)
- **Data:** AAAA-MM-DD
- **Referências:** `specs/features/<...>/{requirements,plan,tasks}.md`

## Objetivo da etapa
<1–3 linhas: o que esta etapa deveria entregar e o critério de pronto.>

## Diário de bordo dos agentes

> Cada agente preenche a sua subseção ao terminar a sua parte. Fatos e evidências, sem enfeite.

### planner
- **Feito:** ...
- **Decisões de enxugamento (código mínimo):** o que foi reusado; o que NÃO foi planejado.
- **Evidências:** `specs/features/.../tasks.md`
- **Dúvidas/Bloqueios:** ...

### coder
- **Feito:** arquivos tocados; task(s).
- **Decisões:** ...
- **Evidências:** `npm run check` / `php artisan test` (resultado).
- **Dúvidas/Bloqueios:** ...

### tester
- **Feito:** suítes executadas.
- **Resultado:** passed/skipped/failed.
- **Evidências:** ...
- **Falhas:** ...

### validator
- **Feito:** eixos avaliados (persistência, escalabilidade, arquitetura, funcionalidade).
- **Resultado:** APROVADO | REPROVADO.
- **Ressalvas (código mínimo):** itens a remover (BAIXA/MÉDIA).
- **Evidências:** ...

### security
- **Feito:** checks (auth, autorização, validação, injeção, dados sensíveis).
- **Resultado:** APROVADO | REPROVADO.
- **Evidências:** ...

### contracts
- **Feito:** contratos back↔front conferidos.
- **Resultado:** OK | DIVERGENTE (bloqueante: sim/não).
- **Evidências:** ...

### builder
- **Feito:** comando de build; artefato.
- **Resultado:** OK | FALHOU. Tag/caminho: ...
- **Evidências:** ...

### deployer
- **Feito:** o que foi subido; tag.
- **Resultado:** NO_AR | PARCIAL | FALHOU.
- **Healthchecks:** app `/up`, front `/`, login.
- **Evidências:** ...

## Ponderação do orquestrador
<Análise crítica dos diários: o que aceito, o que rebato, o que devolvo.

- **Aceito:** ...
- **Ressalvas/Devoluções:** ...
- **Decisões:** ... (e o porquê, à luz de `specs/system/architecture.md` e do princípio de código mínimo)>

## Checklist

### Concluído
- [x] <item verificável>

### Falta concluir
- [ ] <item> — responsável: `<agente>`; próximo passo: <...>

## Resumo cache (para `orchestration.json`)
```
<uma linha compacta com o resultado da etapa>
```

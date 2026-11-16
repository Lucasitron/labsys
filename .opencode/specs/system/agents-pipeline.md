# Pipeline de agentes (FabLab) — papéis, modelos e regras

> Fonte da verdade da **configuração de agentes** do opencode para este projeto.
> Alvo de execução: monólito modular MVC em PHP/Laravel (`system/architecture.md`) + SPA SvelteKit.

## 1. Fluxo do pipeline
```
requirements → plan → tasks → [coder] → [tester] → [validator] → [security] → [contracts] → [builder] → [deployer]
                              ↑                                                            ↑            ↑
                        (código de feature)                                        (só build)   (só deploy)
```
- O **orquestrador** coordena, mantém o estado e escreve os relatórios; **não implementa**.
- Cada etapa concluída gera/atualiza um relatório MD em `.opencode/reports/`.

## 2. Exclusividade de papéis (regra dura)
| Ação | Agente responsável | Todos os outros |
|------|--------------------|-----------------|
| Escrever código de feature (back) | `coder` | proibido |
| Escrever código de feature (front) | `frontend/coder` | proibido |
| **Build** (bundle/imagem) | `builder` | proibido |
| **Deploy** (subir/atualizar servidor) | `deployer` | proibido |
| Merge/commit de integração | `orchestrator` | proibido |

## 3. Agentes e modelos
> Escrita de código: `beam free` (padrão), `glm` ou `big pickle`.
> Teste/planner/validator/security: `deepseek`, `glm` ou `big pickle`.

### Raiz (`.opencode/agents/`)
| Agente | Papel | Modelo |
|--------|-------|--------|
| `orchestrator` | coordena, mantém state, escreve relatórios | `opencode/deepseek-v4.1-flash-free` |
| `planner` | plan.md + tasks.md | `opencode/deepseek-v4.1-flash-free` |
| `coder` | implementa código backend | `opencode/beam-free` |
| `tester` | executa/conferência de testes | `opencode/deepseek-v4.1-flash-free` |
| `validator` | valida código (4 eixos) | `opencode/deepseek-v4.1-flash-free` |
| `security` | auditoria de segurança | `opencode/deepseek-v4.1-flash-free` |
| `contracts` | contratos back↔front | `opencode/deepseek-v4.1-flash-free` |
| `builder` | **só build** | `opencode/deepseek-v4.1-flash-free` |
| `deployer` | **só deploy** | `opencode/deepseek-v4.1-flash-free` |

### Frontend (`.opencode/agents/frontend/`)
| Agente | Papel | Modelo |
|--------|-------|--------|
| `orchestrator` | coordena o SPA | `opencode/deepseek-v4.1-flash-free` |
| `planner` | plan.md + tasks.md | `opencode/deepseek-v4.1-flash-free` |
| `coder` | implementa SvelteKit | `opencode/beam-free` |
| `designer` | mockups HTML/Tailwind | `opencode/big-pickle` |
| `tester` | check + vitest | `opencode/deepseek-v4.1-flash-free` |
| `validator` | valida código (4 eixos) | `opencode/deepseek-v4.1-flash-free` |
| `security` | auditoria de segurança | `opencode/deepseek-v4.1-flash-free` |
| `tracker` | acompanhamento/consultoria | `opencode/deepseek-v4.1-flash-free` |

Fonte alternativa (quando o primário estiver indisponível): `opencode/glm-5.3-flash-free`,
`opencode/beam-free`, `opencode/big-pickle`. **`opencode/muse-spark-*` não existe mais.**

## 4. Regras transversais (front e back)
1. **Código mínimo / simplicidade** — antes de escrever, ponderar necessidade e verbosidade:
   reuse > create; plataforma (Laravel/SvelteKit/TS) > abstração própria; sem código especulativo,
   helpers/mocks/fallbacks por conveniência. A solução mais simples é, em regra, a adequada.
2. **Não resolver um problema criando outro** — considerar efeitos colaterais, persistência e
   contrato antes de codar. Se a task for desnecessária, reportar em vez de implementar.
3. **Validação antes de concluir** — o `validator` percorre o código (contexto zero) nos 4 eixos:
   **persistência, escalabilidade, arquitetura, funcionalidade**. Falha → devolve, não fecha.
4. **Frontend** — sem resquícios de construção (debug/TODO/"em construção"); mensagens
   padronizadas (`toUserMessage`/`toasts`/`ErrorBanner`, PT-BR); telas **nunca** expõem código/banco
   (nomes de tabela/coluna, SQL, stack trace, endpoint cru).
5. **Segredos** — nunca em código/log/relatório/commit; `VITE_*` só não-sensíveis.

## 5. Relatórios (`.opencode/reports/`)
- Um arquivo por etapa (`<NNNN>-<slug>.md`), conforme `_TEMPLATE.md`.
- Blocos obrigatórios: **Diário de bordo dos agentes** → **Ponderação do orquestrador** → **Checklist**.
- Resumo compacto em `state/orchestration.json` (`resumo_cache`).

## 6. Estado e contexto
- Estado vivo: `state/orchestration.json` (back) e `state/frontend-orchestration.json` (front).
- Fonte da verdade das features: `.opencode/specs/` (não re-inventar; `docs/` só contexto).

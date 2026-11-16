# DESPACHO — Builder (Frontend) · PRODUCAO — Bloco 2 (prd-004..prd-015) · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **API layer** da feature `producao` — 12 módulos REST +
> parâmetros 5S + reexports pendentes do client (bloco 1).

---

## Contexto (ler antes)

- Tasks: `.opencode/specs/frontend/features/producao/tasks.md` (`prd-004..prd-015`, executar em ordem numérica)
- Plano: `.opencode/specs/frontend/features/producao/plan.md` (camadas §2, conflitos §3, RBAC §5, riscos §7, DoD §8)
- Mockup aprovado: `docs/mokups/producao/` + `requisitos.md` (decisões PO A1–A5)
- Bloco 1 concluído (`c4406f4`): `types/producao.ts` (com `Paginado<T>`/`Paginacao`), `utils/producao-status.ts`, `api/producao/client.ts` (com helpers pt-BR `montarQuery`, `paginaDaUrl`, `totalDePaginas`, `paginadoVazio`, `paginaSeguinte/Anterior`, `mostrarDeAte` — **reexports agregadores PENDENTES**)
- `apiFetch` global (`client.ts`): base `${API_BASE}` já inclui `/api` → **NO bloco 2 os paths passados ao apiFetch são `/producao/<recurso>`** (ex.: `/producao/resumo`, `/producao/5s/setores`)
- Auth/dashboard: interceptor 401 global (logout+redirect); helpers Bearer no padrão `dashboard.ts` (`get(auth).token ? { Authorization: ... } : {}`)

## Tasks do bloco (o Builder toca SOMENTE estes)

Criar em `frontend/src/lib/api/producao/` (todos `create` 🟡, base path `/producao`):

| Task | Arquivo | Conteúdo (paths via apiFetch) |
|---|---|---|
| prd-004 | `resumo.ts` | `GET /producao/resumo` → `ResumoProducao` (kpis, alertas, rankingTop, setores, maquinas) · DoD: mock local até gateway (ver Regra 4) · Loading/Error/Empty sane |
| prd-005 | `projetos.ts` | `GET /producao/projetos?status&responsavel&page` (Paginado<Projeto>) · `GET/{id}` · `POST` · `PUT/{id}` · `GET/{id}/documentos` 🟡 · `POST/{id}/documentos` 🟡 · `GET/{id}/materiais` 🟡 · `POST/{id}/materiais/saida` 🟡 |
| prd-006 | `tarefas.ts` | `GET /producao/tarefas?projetoId&status&prioridade&responsavel&page` · `POST` · `PUT/{id}` · `PATCH/{id}/mover` · `PATCH/{id}/apontar-horas` · `PATCH/{id}/atribuir` · DoD: Kanban move otimista com rollback fica no componente (aqui só a chamada); bloqueio por pendência grave vem da api |
| prd-007 | `kanban.ts` | `GET /producao/kanban?tab&status` · `PATCH/{id}/mover` (coluna alvo) — encomendas |
| prd-008 | `maquinas.ts` | `GET /producao/maquinas?page&search&categoria&cartao` · `GET/{id}` · `POST` · `PUT/{id}/status` · `POST/{id}/uso` · `POST/{id}/chamados` · `GET/{id}/preventivas` · `POST/{id}/preventivas` · `POST/{id}/historico` · `GET/{id}/chamados` |
| prd-009 | `setores-5s.ts` | `GET /producao/5s/setores?ciclo&page` · `GET/{id}` · `PUT/{id}` · `PUT/{id}/responsaveis` · `PUT/{id}/checklist` · `POST/{id}/aprovar-alteracao` · DoD A1: checklist 5S travado (sem ordem/peso); seletor de papel é UI (aqui só API) |
| prd-010 | `inspecoes-5s.ts` | `GET /producao/inspecoes-5s?status&setor&auditor&page` · `POST` · `GET/{id}` · `PUT/{id}/concluir` · `POST/{id}/nomear-auditor` · DoD A2: **sem nota manual no client**; prévia automática (conformes÷total) é exibição; nota/cartão recalc no backend |
| prd-011 | `pendencias-5s.ts` | `GET /producao/pendencias-5s?gravidade&setor&situacao&page` · `GET/{id}` · `PUT/{id}/resolver` · `POST/{id}/penalidade` |
| prd-012 | `ranking-5s.ts` | `GET /producao/ranking-5s?mes` → `{ranking, mediaGeral}` · `GET /producao/ranking-5s/evolucao?setor&mes` |
| prd-013 | `penalidades-5s.ts` | `GET /producao/penalidades-5s?tipo&situacao&page` · `POST` (Admin) · `PUT/{id}/cumprida` |
| prd-014 | `mesas.ts` | `GET /producao/mesas?mes&situacao&search&page` · `POST` · `GET/{id}` · `PUT/{id}/evolucao` · `POST/{id}/auditoria` |
| prd-015 | `parametros-5s.ts` | `GET/PUT /producao/parametros-5s` (rotacaoDias · diasAuditoriaProjeto · diaSemanaInspecao · periodoExperimental · notaCartaoVerde/Amarelo · prazoPendenciaGrave) · ⚠️ overlap configs/system (cadência compartilhada) — **NUNCA paralelo**; função única |

**Completar (modify):** `frontend/src/lib/api/producao/client.ts` — reexportar `obterResumo`, `obterProjetos`, `obterTarefas`, `obterKanban`, `obterMaquinas` (+ demais módulos 5S) dos módulos criados acima; manter helpers existentes; continuar sem lógica de domínio.

## Regras

1. **1 task por vez, em ordem; NÃO commitar** (Orchestrator commita ao final).
2. **NUNCA** tocar: `docs/`, `docs/mokups/`, specs, `client.ts`, `types/auth.ts`, `menu.ts`, `permissions.ts` (C1/C2 bloco posterior), `estoque/**`, `rh.ts`, componentes/rotas (blocos 3+).
3. Nomenclatura das funções: `obter*` (GET list), `obter*PorId` (GET {id}), `criar*`, `atualizar*`, `concluir*`, `resolver*`, `mover*`, etc. pt-BR; tipos vindos de `types/producao.ts`; **sem `any`**; erros propagam por `ApiError`/`NetworkError` do client (só rethrow — o interceptor 401 global cuida de logout/redirect).
4. **Mock local (prd-004/resumo):** criar `frontend/src/lib/api/producao/mocks.ts` com dados tipados de exemplo + contraste `USE_PRODUCAO_MOCK = true` (const exportada). `obterResumo` retorna o mock quando `USE_PRODUCAO_MOCK` — os demais módulos fazem chamada real via `apiFetch` (sem mock). Deixar comentário claro `TODO contrato 🟡 — remover mock quando gateway expor /api/producao/**` e reportar.
5. Queries montadas com o helper `montarQuery`/`paginar` do client (evitar duplicação); filtros pt-BR conforme requirements (status/prioridade/responsavel/setor/gravidade/situacao/mes/ciclo/categoria/cartao).
6. UI text pt-BR nos estados; tokens design-system; nada de `console.log`.
7. Ao final: `npm run test` (95 verdes, sem testes novos neste bloco) e `npm run check` SEM erros novos (baseline 12/15/7 em estoque/** e ui/Chip.svelte). Se o `client.ts` reexport de módulo inexistente quebrar por ordem de criação, crie os mesmos módulos antes de reexportar (ordem numérica garante).

## Relatório esperado (responda com)

- Por task `prd-00X`: ✅/❌ + evidência (arquivo:linha + assinaturas principais + como verificou)
- Reexports completados no client.ts (lista)
- `npm run test` (contagem) + `npm run check` (resumo)
- Mock do resumo: localização, contraste, observações p/ remoção
- Pontos de atenção / desvios
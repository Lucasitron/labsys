# Plan — Produção & Projetos (frontend, v2 · 12 telas)

> **Fonte da verdade:** mockup v2 `docs/mokups/producao/full.html` (12 telas) + `docs/mokups/producao/instrunctions.md`
> (Prompts 0–12) + `docs/mokups/producao/requisitos.md`. **Spec:** `requirements.md` (v2) nesta pasta — tasks em `tasks.md`.
> **Decisões PO v2 A1–A5 já incorporadas.** Não alterar `docs/**` nem specs de outras features.

---

## 1. Objetivo

Construir o módulo **Produção & Projetos** do frontend (12 telas: Resumo · Setores 5S · Auditoria 5S · Pendências 5S ·
Ranking 5S · Advertências 5S · Mesas de projeto · Projetos · Projeto [detalhe] · Tarefas/Kanban · Máquinas ·
Máquina [detalhe]) consumindo `docs/mokups/producao/` como fonte da verdade visual, com RBAC hide (nunca disable),
TS strict, pt-BR e data-testid `proj-*`, `tar-*`, `maq-*`, `s5-*`, `set-*`, `aud-*`, `pend-*`, `pen-*`, `mesa-*`, `prd-*`.

---

## 2. Camadas

| Camada | Arquivos | Responsabilidade |
|---|---|---|
| Types | `src/lib/types/producao.ts` | Projeto, Tarefa, Cartao5S, Maquina, Setor, Inspecao5S, Pendencia5S, Penalidade5S, Mesa, RankingItem, Parametro5S, KanbanColuna, Estados |
| API | `src/lib/api/producao/{resumo,projetos,tarefas,kanban,maquinas,setores,inspecoes-5s,pendencias-5s,ranking-5s,penalidades-5s,mesas,parametros-5s}.ts` | cliente REST via `apiFetch` · endpoints pt-BR 🟡 |
| Utils | `src/lib/utils/producao-status.ts` | mapeamento status → label/cor (exact de producao-status.md) + helpers RBAC de atribuição |
| Components | `src/lib/components/producao/*` | CartaoBadge, modais módulo (ModalAuditoria, ModalChecklistSetor, ModalDetalhePendencia, ModalRegistrarPenalidade, ModalSaidaProjeto, ModalNovaTarefa/ModalNovaProjeto, ModalMesa, ModalMaquina, ModalChamado, ModalPreventiva, ModalSuspender, ModalRanking) |
| Rotas | `src/routes/(app)/producao/**` (12 rotas + [id]) | telas + `+page.ts` (load) · `+layout.ts` guarda |
| Estados | `src/lib/states/producao.ts` | Loading/Empty/Empty-filtrado/Error/RBAC-restricted |
| RBAC | `src/lib/utils/permissions.ts` (modify) + `src/lib/config/menu.ts` (modify) | submenu Produção + guardas 5S |
| Testes | `frontend/tests/unit/permissions.test.ts` (modify) · `tests/unit/producao-status.test.ts` (create) · e2e se Playwright | DoD |

---

## 3. Conflitos de Escopo (reportar — NÃO paralelo com Configurações)

| # | Arquivo | Produção toca | Configurações toca | Ação |
|---|---|---|---|---|
| C1 | `src/lib/config/menu.ts` | ✓ submenu Produção (Resumo/Projetos/Tarefas/Máquinas + submenu 5S: Setores/Auditoria/Pendências/Ranking/Advertências/Mesas) | ✓ submenu Configurações (Perfil/Usuários/Permissões/Sistema) | **Sequencial, nunca paralelo** |
| C2 | `src/lib/utils/permissions.ts` | ✓ `canSee5S`, `isResponsavelAtribuido`, guarda máquina/projeto | ✓ `isAdmin`, `canViewConfiguracoes`, `canEditConfiguracoes` | **Sequencial, nunca paralelo** |
| C3 | 5S conceitual | auditoria/advertência/ligação com tarefas bloqueadas (bloqueio) | `sistema` edita cadência 5S usada pela auditoria 5S | Endpoint `parametros-5s` compartilhado 🟡 |
| C4 | `rh` (pessoas) | responsáveis 5S / força-tarefa (NomeFunc) | `usuarios` administra contas de acesso | RBAC hide independente; dados de pessoa via RH no servidor |
| C5 | Estoque | BOM/materiais + `saida` de material do projeto (gateway) | — | **Estoque é dependência** p/ aba materiais do projeto; 🟡 saída de material |
| C6 | Auth | — | `api/auth/senha.ts` (novo, namespace auth) | 🟡 endpoint `PUT /api/auth/senha`; novo arquivo, não altera login |

---

## 4. Ordem de Implementação (B3 tasks)

| # | Passo | Tasks |
|---|---|---|
| 0 | Fundação: types + API (12 módulos) + utils | `prd-001`…`prd-013` |
| 1 | Componentes compartilhados (CartaoBadge, modais do módulo) | `prd-014`…`prd-023` |
| 2 | Layout + guarda + RBAC (menu.ts, permissions.ts — **conflito C1/C2**) | `prd-024`·`prd-025` |
| 3 | Tela Resumo (`/producao`) | `prd-026`·`prd-027` |
| 4 | Setores 5S → Auditoria → Pendências → Ranking → Advertências → Mesas | `prd-028`…`prd-039` |
| 5 | Projetos → Projeto [detalhe → abas, docs, BOM, saída ↗), Tarefas/Kanban | `prd-040`…`prd-048` |
| 6 | Máquinas → Máquina [detalhe] | `prd-049`…`prd-052` |
| 7 | Testes (unit status/RBAC + e2e se disponível) | `prd-053`…`prd-055` |

> **Parsing-order:** base → componentes → rotas (pai filho por tela) → states → RBAC → testes — **1 arquivo ≡ 1 task**.

---

## 5. RBAC (resumo — ver requirements.md §Decorrer)

- **Admin (0):** tudo, incl. advertências 5S, suspensões, penalidades, cadastro máquina/setor.
- **Bolsista (1)/Voluntário (2):** vê Produção; edita **se atribuído** (projeto/tarefa/setor 5S responsável).
- **Estagiário (3):** edita se atribuído; **HIDE na auditoria 5S salvo se auditor nomeado**.
- **Recrutando (4):** **HIDE no 5S inteiro** (guarda `redirect`), vê resumo/projetos somente leitura.
- Regra: item **HIDE, nunca disable**; escrita sem atribuição → `redirect(303,'/producao')`.

---

## 6. Dependências entre features

- **auth** — base (autenticação/guarda de app). ✅ concluída.
- **estoque** — provê BOM/materiais (aba materiais do projeto) + **saída de material** — **gatilho de baixa; contrato 🟡**.
- **rh** — NomeFunc/responsáveis 5S (RBAC de atribuição) — dados de pessoa no servidor.
- **configuracoes** — edita `parametros-5s` (cadência auditoria) compartilhado com Produção. **C1/C2/C3 — nunca paralelo.**

---

## 7. Riscos & Mitigações

| Risco | Mitigação |
|---|---|
| 12 telas grandes — risco de múltiplas sessions em produção | **1 Builder por feature**; tasks sequenciais por prompt; nenhum paralelismo em `menu.ts`/`permissions.ts` |
| Playwright não confirmado ([package.json] só vitest) | e2e **condicional** (se suite e2e existir); unit é obrigatório |
| Endpoints 🟡 (produção-service inexistente; mocks) | mocks em `src/lib/api/producao/*` até gateway expor; registrar em `contract.gap.md`; **nunca mockar no client final sem nota** |
| Baixa de material (A4) e documentos (A3) sem contrato | rotas criadas; chamada bloqueada/`saida` com nota "contrato pendente" — reportar ao Orchestrator |

---

## 8. Critérios de Entrega (plan-level DoD)

- 12 telas funcionais + guardas RBAC (hide/redirect) cobrindo os 5 níveis.
- Todos os estados (Loading/Empty/Empty-filtrado/Error/RBAC) nas telas com dados.
- `menu.ts` com submenu Produção (7 itens) + guarda 5S por nível.
- Unit tests (producao-status + RBAC) verdes; e2e se disponível.
- Relatório final com conflitos (C1–C5), JSON de orquestração atualizado, endpoints 🟡 mapeados.

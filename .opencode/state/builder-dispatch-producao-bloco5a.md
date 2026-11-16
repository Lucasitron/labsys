# DESPACHO — Builder (Frontend) · PRODUCAO — BLOCO 5a (prd-028..prd-033) · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **telas Resumo + módulo 5S** (6 telas) da feature `producao`.

## Contexto (ler antes)
- Tasks: `.opencode/specs/frontend/features/producao/tasks.md` (`prd-028..prd-033`); Plano §5 RBAC/§8 DoD
- Mockup visual: `docs/mokups/producao/full.html` + `requisitos.md` (A1..A5) — **fonte da verdade visual**
- Blocos 1–4 concluídos (`c4406f4`, `84b643a`, `a011991`, `7bc70f5`):
  - API `$lib/api/producao` (client barrell): `obterResumo` (mock gate `USE_PRODUCAO_MOCK=true`), `obterSetores5S`, `obterInspecoes5S`, `obterPendencias5S`, `obterRanking5S`, `obterPenalidades5S`, `concluirInspecao5S`, `resolverPendencia5S`, `registrarPenalidadeParaPendencia5S`, etc.
  - Utils `$lib/utils/producao-status`: `statusMeta` maps (setor/inspecao/pendencia/cartao/gravidade/penalidade), `notaToCartao`, `gravidadeMeta`
  - Components `$lib/components/producao/`: `CartaoBadge`, `ModalAuditoria`, `ModalSetor`, `ModalChecklistSetor`, `ModalDetalhePendencia`, `ModalMesa`, `ModalNovaTarefa`, `ModalNovoProjeto`, `ModalSaidaProjeto`
  - Layout `(app)/producao/+layout.ts` **já fornece data flags RBAC**: `canSee5S`, `canAuditar5S`, `canSeeAdvertencias`, `canEditProducao` — ler de `$page.data`
- Padrão visual existente: dashboard (`KpiGrid`, cards, estados Loading/Error/Empty) e estoque (tabelas). Seguir esse estilo — tokens design-system.

## Tasks (criar em `src/routes/(app)/producao/`)

| Task | Arquivos | Conteúdo essencial |
|---|---|---|
| prd-028 | `+page.ts` + `+page.svelte` (raiz /producao = **Resumo**) | `obterResumo` → KPIs (projetos ativos · tarefas pendentes · máquinas · sistema 5S) + alertas + ranking top + cards setores 5S + mini-kanban encomendas + mini máquinas; atalhos `data-goto`; estados Loading/Error parcial por bloco; `data-testid="prd-kpi","prd-alerta","prd-kanban"` |
| prd-029 | `5s/setores/+page.ts` + `+page.svelte` | card grid setores (ciclo badge · responsáveis · `CartaoBadge` + nota · próxima auditoria) · KPIs (4) · filtros (pesquisa · cartão · responsável) · ações Novo setor / Configurar 5S (Admin) · ✎ editar → `ModalSetor`/`ModalChecklistSetor`; `data-testid="set-card","set-novo","set-config"` |
| prd-030 | `5s/auditoria/+page.ts` + `+page.svelte` | KPIs (auditorias mês · hoje · auditores · atrasadas) · filtros (status · setor) · tabela (setor · auditor · data · nota badge · ações Auditar/Nomear auditor) → `ModalAuditoria` (concluir) + nomear; `data-testid="aud-row","aud-concluir","aud-nomear"` |
| prd-031 | `5s/pendencias/+page.ts` + `+page.svelte` | KPIs por gravidade · filtros (gravidade · setor · situação) · tabela (gravidade badge · pendência clicável → `ModalDetalhePendencia` · responsável · prazo · situação) · card "Como funciona" · Resolver / Registrar penalidade (Admin); `data-testid="pend-row","pend-resolver","pend-penalidade"` |
| prd-032 | `5s/ranking/+page.ts` + `+page.svelte` | seletor mês · card regra (Top 1 livre 1 semana) · tabela ranking (posição · responsável · nota · `CartaoBadge` · cartão) · evolução das notas (barras por setor/mês, **sem lib de chart**) · metas/faixas; `data-testid="rank-row","rank-top1"`; nota vem da API |
| prd-033 | `5s/advertencias/+page.ts` + `+page.svelte` | KPIs (avisos · advertências · suspensões · expulsões) · filtros (tipo · situação) · tabela (membro · tipo badge · data · motivo · status) · card escala adversarial · Registrar penalidade (Admin); **HIDE p/ não-admin** (`canSeeAdvertencias`); `data-testid="pen-row","pen-registrar"` |

## Regras
1. **Ordem numérica; NÃO commitar.** Só os 12 arquivos (6 pares page.ts/page.svelte) da tabela; **NÃO** tocar outras rotas/modais/menu/permissions/docs.
2. RBAC: **hide nunca disable**; usar flags de `$page.data`; 5S HIDE p/ sem `canSee5S` já tratado no layout (redirect) — aqui respeitar `canAuditar5S`/`canSeeAdvertencias`/`canEditProducao` para ações/botões.
3. Estados: Loading (skeleton/spinner), Error (mensagem + retry), Empty e Empty-filtrado em cada bloco; formatar moeda/pt-BR via Intl onde houver notação; datas pt-BR (`toLocaleDateString('pt-BR')`).
4. Reutilizar os modais existentes (props): `ModalAuditoria({open, inspecao, itens, onConcluida, onClose})`, `ModalSetor`, `ModalChecklistSetor`, `ModalDetalhePendencia`, `CartaoBadge` — conferir props exatas no componente antes de usar (criados no bloco 3); jsons fictícios dos componentes servem de referência do payload, **mas usar tipos reais** (`Setor5S`, `Inspecao5S`, `Pendencia5S`, `RankingItem`, `Penalidade5S`).
5. pt-BR, sem `any`, sem `console.log`, a11y (labels/focus), `data-testid` conforme DoD.
6. Mock: resumo usa `obterResumo` (já tem mock local); os demais endpoints são reais (contrato 🟡) — tratar erro com mensagem amigável e **registrar no relatório** quais `apiFetch` falharam por falta de gateway.
7. Ao final: `npm run test` (95 verdes) + `npm run check` (baseline 12/15/7, zero novos).

## Relatório
- Por task ✅/❌ + evidência (arquivos, data-testids, estados implementados, wie verificado)
- Quais endpoints 🟡 apresentaram erro real ao montar as telas (sem gateway)
- `npm run test` + `npm run check`
- Pontos de atenção / desvios
# DESPACHO — Builder (Frontend) · PRODUCAO — BLOCO 5b (prd-034..prd-039) · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **telas Mesas/Projetos/Tarefas/Máquinas** (6 telas) da feature `producao`.
> ⚠️ Roda em paralelo com o BLOCO 5a (prd-028..033) — arquivos distintos, sem conflito.

## Contexto (ler antes)
- Tasks: `.opencode/specs/frontend/features/producao/tasks.md` (`prd-034..prd-039`); Plano §5 RBAC/§8 DoD
- Mockup visual: `docs/mokups/producao/full.html` + `requisitos.md` (A4/A5) — **fonte da verdade visual**
- Blocos 1–4 concluídos (`c4406f4`, `84b643a`, `a011991`, `7bc70f5`):
  - API `$lib/api/producao` (barrel client): `obterMesas`, `criarMesa`, `obterProjetos`, `obterProjetoPorId`, `criarProjeto`, `atualizarProjeto`, `obterMateriaisDoProjeto`, `registrarSaidaMaterial`, `obterTarefas`, `criarTarefa`, `moverTarefa`, `apontarHorasTarefa`, `atribuirTarefa`, `obterMaquinas`, `obterMaquinaPorId`, `atualizarStatusMaquina`, `obterKanban`, `moverKanbanItem`, etc.
  - Utils `$lib/utils/producao-status`: maps `PROJETO_STATUS_META`, `TAREFA_STATUS_META`, `MAQUINA_STATUS_META`, `KANBAN_COLUNA_META`, `MESA_STATUS_META`, etc.
  - Components `$lib/components/producao/`: `ModalMesa`, `ModalNovaTarefa`, `ModalNovoProjeto`, `ModalSaidaProjeto`, `CartaoBadge`
  - Layout `(app)/producao/+layout.ts` **fornece flags RBAC no `$page.data`**: `canEditProducao` etc. — usar
- Padrão visual: dashboard (`KpiGrid`, cards, Loading/Error/Empty) + estoque (tabelas/badges). Tokens design-system.

## Tasks (criar em `src/routes/(app)/producao/`)

| Task | Arquivos | Conteúdo essencial |
|---|---|---|
| prd-034 | `5s/mesas/+page.ts` + `+page.svelte` | KPIs (mesas · em uso · pendentes · auditorias) · filtros (mês · situação · busca) · grid cards (mesa · projeto · membro · período experimental chip · QR totem placeholder · status · última auditoria) → `ModalMesa`; ação Auditar mesa / Vincular projeto; `data-testid="mesa-card","mesa-auditar","mesa-vincular"` |
| prd-035 | `projetos/+page.ts` + `+page.svelte` | KPIs (ativos · em andamento · concluídos) · filtros (status · responsável) · grid cards (código mono · status badge · responsável · progresso barra · fim previsto) → `ModalNovoProjeto`; linha → detalhe `[id]`; `data-testid="proj-card","proj-novo"` |
| prd-036 | `projetos/[id]/+page.ts` + `+page.svelte` 🟡 | abas `?tab=geral|documentacao|materiais|tarefas` · documentos (lista + links + **upload fica desabilitado c/ nota "contrato pendente"** 🟡) · materiais (BOM — `obterMateriaisDoProjeto`) · tarefas do projeto (`obterTarefas?projetoId=`) → `ModalNovaTarefa`/`ModalSaidaProjeto`; `data-testid="proj-detail","proj-doc","proj-materiais"` |
| prd-037 | `tarefas/+page.ts` + `+page.svelte` | Kanban 5 colunas (Fila/Produção/Acabamento/Pronto/Entregue) com `?tab&status` · cards por coluna (`moverTarefa`) · filtros · `ModalNovaTarefa` · modal-detalhe-tarefa (descrição + checklist x/y + barra %; **status manual · sem peso**) · modal-atribuir (`atribuirTarefa`) · modal-resolver-pendencia (se bloqueada) · **A5** · RBAC: sem permissão → coluna não-editável (hide mãozinha/ação); `data-testid="tar-row","tar-nova","tar-card"` |
| prd-038 | `maquinas/+page.ts` + `+page.svelte` | KPIs (cadastradas · cartões verdes · em manutenção · atrasada) · filtros (busca · cartão · categoria) · grid cards (código · `CartaoBadge` · nome · categoria · responsável manutenção · status) → modal-maquina (criar/editar básico se existir componente; senão ação simples c/ nota); card → detalhe `[id]`; `data-testid="maq-card","maq-nova"` |
| prd-039 | `maquinas/[id]/+page.ts` + `+page.svelte` 🟡 | abas `?tab=geral|documentacao|receitas|preflight|manutencao|chamados` · preventivas (lista + criar `criarPreventivaMaquina`) · chamados (`abrirChamadoMaquina`, lista) · docs 🟡 (nota "contrato pendente") · modal-relatorio-manutencao/compra 🟡 (ações desabilitadas c/ nota ou salvam e reportam erro) · `data-testid="maq-tab-geral","maq-chamado","maq-suspender"` |

## Regras
1. **Ordem numérica; NÃO commitar.** Só os 12 arquivos da tabela; **NÃO** tocar rotas 5S (bloco 5a), layout/menu/permissions/docs/modais.
2. RBAC hide; usar `$page.data.canEditProducao`; **colunas kanban não-editáveis (hide ação)** quando sem permissão.
3. Estados Loading/Error/Empty em cada bloco; datas pt-BR; moeda via Intl se houver; pt-BR; sem `any`; sem `console.log`; a11y; `data-testid` conforme DoD.
4. **A2/A4/A5**: nota/cartão 5S **nunca escritos no client** (só exibidos da API); saída de material/upload/compra 🟡 → registrar no relatório que falham por contrato pendente (mostrar erro amigável no modal, não blocker silencioso).
5. Upload/arquivos: **sem implementar** componentes de upload real — os itens 🟡 ficam desabilitados com nota "Contrato pendente (backend)" e o botão principal desabilitado; relatar.
6. Reutilizar modais existentes (conferir props no componente). Kanban do mockup = arrastar cards como **visual** (botão mover coluna via `moverTarefa`); **drag-and-drop não é requisito** — preferir botões "mover →" (a11y).
7. Ao final: `npm run test` (95 verdes) + `npm run check` (baseline 12/15/7, zero novos).

## Relatório
- Por task ✅/❌ + evidência (arquivos, data-testids, abas, estados, wie verificado)
- Endpoints 🟡 que falharam ao montar (sem gateway) e desabilitados com nota
- `npm run test` + `npm run check`
- Pontos de atenção / desvios
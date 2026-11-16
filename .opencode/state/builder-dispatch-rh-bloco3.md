# DESPACHO — Builder (Frontend) · RH — Bloco 3 (B3 Seleção + Horas) · 21/09/2026

> Implemente **rh-017..rh-018** de `.opencode/specs/frontend/features/rh/tasks.md` (ler o bloco B3 integral + mockup `docs/mokups/pessoas/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-2 commitados: fundação + rotas `pessoas/**` com guarda `+layout.ts` ativo
- Reuso: `api/rh/processo-seletivo.ts` (listPS/createGrupo/moverEstagio/avaliarMembro 🔴), `api/rh/horas.ts` (legado + nova modelagem 🟡; **zero soma no client**), `ui/KanbanBoard.svelte`, `ui/TypeBadge.svelte`, kit (PageHeader, SearchInput, Tabs, Chip, TableSkeleton, EmptyState, ErrorBanner, Pagination, StatusBadge), `components/estoque/PeoplePicker.svelte` (**reusar** no modal de registrar horas — não duplicar), `utils/rh-status.ts`, permissions (`isTutor`/responsável, `canEditRH`)
- Padrão de referência: telas estoque com modais (ex.: `estoque/emprestimos/novo/+page.svelte`)
- NÃO alterar specs/docs/`permissions.ts`/APIs; NÃO commitar; sem libs novas

## Tasks
- **rh-017** `src/routes/(app)/pessoas/processo-seletivo/+page.svelte` — URL `?estagio=`; header + "Criar grupo" (`ps-criar-grupo`, hide se `!canEdit`) + "Nova inscrição" (cria recrutando via `createPessoa` 🟡) + busca; KPIs (Inscritos/Grupos/Em avaliação/Aprovados via counts servidos — nunca somar client); strip G-01..G-05 (click → `modal-grupo`); `KanbanBoard` 5 colunas (tones do mockup) com cards de **grupo** (nome, n membros, chips iniciais + dots) + "Ver grupo"; modais `modal-nova-inscricao` (Nome\*/Email\*/Curso\*/Período/Disponibilidade), `modal-criar-grupo` (Nome\* + Líder/**Tutor(a)** — rótulo Tutor, Dívida 6 — + members), `modal-grupo` (membros + etapa individual + "Avaliar" → `modal-avaliar-ps`: Aprovado/Reprovado + Nota 0–10 + Feedback → `avaliarMembro` individual); mover etapa por botões (sem drag). `data-testid="ps-kpi-grupos/ps-avaliar/ps-criar-grupo"`. Ações hide se não responsável/tutor; refetch após mover/avaliar.
- **rh-018** `src/routes/(app)/pessoas/registro-horas/+page.svelte` — URL `?status=&periodo=`; header + "+ Registrar horas" (modal: Pessoa\* via **`PeoplePicker` reusado** + Data\*/Início\*/Fim\*/Tipo radio Encomenda/Projeto + Referência search + Obs → `createApontamento` 🟡/legado 🔴); KPIs **do extrato backend** (`getHorasDisponiveis` 🟡 — proibir `reduce.*horas`/`totalHoras =` neste arquivo); Tabs Pendentes/Validadas/Rejeitadas (counts, `hora-tab-*`); tabela (Pessoa avatar+nome, Data dd/mm, Horário mono, Horas mono, Tipo `TypeBadge`, Referência mono, Status + Validar `hora-validar` / Rejeitar+motivo `hora-rejeitar`); rejeitada mostra motivo; Estagiário vê só próprias (colunas/ações hide); estados por tab (skeleton/empty/error+retry); refetch após validar/rejeitar/registrar.

## Regras
1. Criar SÓ os 2 arquivos acima.
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; debounce + AbortController; sem `{@html}`; sem console.log.
3. Ao final: `npm run test` (regressão 300) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; contrato 🔴/🟡; confirmação de zero soma client (rg); resultados test/check/build; desvios.
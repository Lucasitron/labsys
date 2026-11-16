# DESPACHO — Builder (Frontend) · RH — Bloco 1 (B1 Fundação) · 21/09/2026

> Implemente **rh-001..rh-011** de `.opencode/specs/frontend/features/rh/tasks.md` (ler tasks + plan + mockup `docs/mokups/pessoas/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme com `git branch --show-current`; se não for, PARE e reporte)
- Baseline: `api/client.ts` (apiFetch/ApiError), `utils/format.ts` (initials/relativeTime/formatNumber), `utils/permissions.ts`, UI kit 22 componentes, `api/rh.ts` (legado `searchPeople → /rh/people` EN acoplado a `stock/request` — NÃO mexer além do re-export previsto), `components/estoque/PeoplePicker.svelte` (reusar em modais B3+; não copiar)
- NÃO alterar specs/docs; NÃO commitar; **permissions.ts é compartilhado (⚠️ sequencial — livre agora)**: adicionar helpers SEM alterar `VIEW_RULES`/`EDIT_RULES`

## Tasks (ordem sugerida)
- **rh-001** `src/lib/types/rh.ts` — unions + modelos do tasks.md (Person/PersonDetail/PsGroup/PsEvaluation/HoraApontamento/ExtratoMensalHoras/Treinamento/Guia/InstrutorDisponibilidade/NivelMatrix/Paged<T>/RhListParams; tutor em PS vs instrutor em treinamentos). Sem any.
- **rh-002** `src/lib/api/rh/pessoas.ts` — bearer() local + apiFetch; listPessoas (query só não-nulos) 🔴, getPessoa 🟡, createPessoa 🟡, updatePessoa 🟡, `searchPeople` **re-export do legado** `api/rh.ts` (path EN congelado; nenhum path `/rh/people` novo). 409 → ApiError.
- **rh-003** `src/lib/api/rh/processo-seletivo.ts` — listPS/createGrupo(tutorId, nunca instrutor)/moverEstagio/avaliarMembro, todos 🔴 via apiFetch.
- **rh-004** `src/lib/api/rh/horas.ts` — legado (listHoras/registrarHoras/validarHoras/rejeitarHoras 🔴) + nova modelagem (getHorasDisponiveis/listApontamentos/createApontamento/validarApontamento 🟡). **NENHUMA função de soma/total** (R-5).
- **rh-005** `src/lib/api/rh/treinamentos.ts` — list/get/getSessao/avaliar/atribuirTarefaFinal/listAgenda/getDisponibilidade/listGuias, todos 🔴; campo `instrutor` (nunca tutor).
- **rh-006** `src/lib/api/rh/niveis.ts` — getNiveis 🔴, alterarNivel 🔴, convidarUsuario 🟡. Matriz autoritativa (nunca derivar no client).
- **rh-007** `src/lib/utils/rh-status.ts` — personStatusMeta/psStageMeta/horaStatusMeta/trainingStatusMeta/disponibilidadeMeta/nivelMeta `{label,color}` (padrão `stock-status.ts`); REUSAR format.ts (não duplicar).
- **rh-008** `src/lib/components/ui/TypeBadge.svelte` — props {tone brand|success|warn|danger|muted, label}; pt-BR, aria-label.
- **rh-009** `src/lib/components/ui/DonutChart.svelte` — props {total, segments[{value,color}], label?}; SVG viewBox 42×42, r=15.9, trilha #1a2235, dasharray/dashoffset acumulado, overlay {x}/{total} + legenda; 0/0 sem NaN; role="img" + aria-label. **NÃO tocar** `dashboard/components/DonutChart.svelte`.
- **rh-010** `src/lib/components/ui/KanbanBoard.svelte` — props {columns[{id,label,tone,count,cards}]}; colunas+cards genéricos, ações por botões/chips (sem drag, sem lib nova); teclado operável.
- **rh-011** `src/lib/utils/permissions.ts` ⚠️ MODIFY — adicionar SEM alterar VIEW/EDIT_RULES: `isResponsavelRH`, `isTutor`, `isInstrutor`, `canEditRH`, `canSeeNiveis = isAdmin`, `isProprioRegistro`; `canEdit(user,'rh')` inclui responsável (EDIT_RULES.rh=[0,1,2] intacta).

## Regras
1. Criar/editar SÓ os 11 arquivos acima.
2. pt-BR; aria-label; sem any; sem libs; Svelte 5 runes; tokens design-system.
3. Ao final: `npm run test` (regressão — baseline 300 verdes) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência (arquivo:linha); contrato 🔴/🟡 por endpoint; resultados test/check/build; desvios.
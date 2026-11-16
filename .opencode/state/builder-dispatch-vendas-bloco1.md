# DESPACHO — Builder (Frontend) · VENDAS — Bloco 1 (B1 Fundação) · 23/09/2026

> Implemente **vnd-001..vnd-013** de `.opencode/specs/frontend/features/vendas/tasks.md` (ler o bloco B1 integral + mockup `docs/mokups/vendas/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo. Verificar se o código é necessário, se a linguagem/plataforma já resolve, se há forma menos verbosa, e se resolve um problema real.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme com `git branch --show-current`; se não for, PARE e reporte)
- Baseline: `api/client.ts` (apiFetch/ApiError), `utils/format.ts` (initials/relativeTime/formatNumber — REUSAR, não duplicar), `utils/permissions.ts`, UI kit (~24 componentes, incl. `KanbanBoard.svelte` do RH — NÃO editar), modal padrão, `stores/toast`
- NÃO alterar specs/docs/requirements; NÃO commitar; **permissions.ts é compartilhado (⚠️ sequencial — livre agora)**: adicionar helpers SEM alterar `VIEW_RULES`/`EDIT_RULES`

## Tasks (ordem sugerida)
- **vnd-001** `src/lib/types/vendas.ts` — unions literais PT + modelos do tasks.md (Tag/Cliente/Orcamento/ItemOrcamento/Encomenda/HistoricoEncomenda/RegistroMarketplace/Interacao/TarefaMarketing/SolicitacaoEdicao/Paged<T>/params; `createdBy` nos registros — RBAC por criador). Sem any.
- **vnd-002** `src/lib/api/vendas/clientes.ts` — bearer() local + apiFetch; listClientes (query só não-nulos) 🔴, getCliente 🟡, createCliente 🔴 (409 → ApiError), updateCliente 🟡, listTags/createTag 🟡, deleteCliente 🔴, bulkAdicionarTag 🔴 (D-2). Só paths `/api/vendas/**`.
- **vnd-003** `src/lib/api/vendas/orcamentos.ts` — list/get 🟡, create/update 🔴, duplicarOrcamento 🔴 (D-2; se o backend não confirmar, NÃO criar fallback client — registrar e NÃO expor a ação... mas a ação pertence a B3; aqui só a função se o contrato permitir: como é 🔴 sem confirmação, **NÃO criar `duplicarOrcamento`** — registrar D-2 no relatório).
- **vnd-004** `src/lib/api/vendas/encomendas.ts` — list (status_kanban) 🔴, get 🟡, createEncomenda ({idOrcamento}|venda direta, nasce na Fila) 🔴, moverKanban PUT `{id}/kanban` 🔴 (409 → ApiError). **Sem** `proximoEstagio|contarPorColuna`.
- **vnd-005** `src/lib/api/vendas/marketplace.ts` — list/registrarVenda 🔴 (`valorTaxa` em R$, nunca %). **Sem** `somar|totalizar|liquido(`.
- **vnd-006** `src/lib/api/vendas/interacoes.ts` — listInteracoes (nested `clientes/{id}/interacoes`) 🟡, registrarInteracao 🟡. Sem rota própria.
- **vnd-007** `src/lib/api/vendas/marketing.ts` — list/create/update Tarefa 🔴.
- **vnd-008** `src/lib/api/vendas/solicitacoes.ts` — list/create/decidir 🟡 (D-4; motivo obrigatório ao rejeitar — validar no client, backend revalida).
- **vnd-009** `src/lib/utils/vendas-status.ts` — 6 maps `{label,color}` (padrão `stock-status.ts`); **só cor, sem tradução**.
- **vnd-010** `src/lib/utils/vendas-format.ts` — formatMoneyBRL/formatDateBR/codigoPrefix (Intl pt-BR); REUSAR format.ts (não duplicar initials/relativeTime).
- **vnd-011** `src/lib/components/ui/Timeline.svelte` — props {items[{id,tone,title,body?,meta?}]}; linha vertical + dot; role="list".
- **vnd-012** `src/lib/components/ui/MoneyInput.svelte` — props {value: number|null, label?}; máscara Intl BRL; `bind:value` numérico (nunca string formatada); validação inline pt-BR. Atenção ao bug Svelte 5 `type="number"` coagindo bind (normalizar com `String()` se necessário — ver precedente `processo-seletivo/+page.svelte`).
- **vnd-013** `src/lib/utils/permissions.ts` ⚠️ MODIFY — adicionar SEM alterar VIEW/EDIT_RULES: `isResponsavelVendas` (responsibilities.vendas não vazio, role!==4), `canEditVendas(user, record)` = isAdmin || (record.createdBy && user.id===record.createdBy), `canDecideVendas` = isAdmin || isResponsavelVendas. Criação liberada (sem helper).

## Regras
1. Criar/editar SÓ os 13 arquivos acima.
2. pt-BR; aria-label; sem any; sem libs; Svelte 5 runes; tokens design-system.
3. Ao final: `npm run test` (regressão — baseline 381 verdes) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência (arquivo:linha); decisão sobre `duplicarOrcamento`; contrato 🔴/🟡 por endpoint; resultados test/check/build; desvios.
# DESPACHO — Builder (Frontend) · ESTOQUE — Bloco 2 (est-012..est-019) · 21/09/2026

> Correção das telas existentes para o contrato real (PT) + estados + RBAC.

## Contexto
- **Tasks:** `.opencode/specs/frontend/features/estoque/tasks.md` §2 (`est-012..est-019`) — ler o bloco
- **Plan:** `plan.md` §7 (R-9: listas globais de entradas/saídas sem endpoint) · `requirements.md`
- **Bloco 1 concluído (commit anterior):** DTOs reais (`types/stock.ts`), `api/stock/*` migrado p/ `/api/estoque/**`, `utils/stock-status.ts`, `permissions.ts` (`isResponsavelEstoque`/`canEdit` ampliado/`canVerFornecedores`); 176 testes verdes; **svelte-check 0 errors / 11 warnings / 6 files**
- **Ajustes mínimos já feitos** no bloco 1 em `routes/(app)/estoque/**` (listas com MovimentosResult vazio por R-9, etc.) — agora corrigir integralmente
- Mockup: `docs/mokups/estoque/` (itens/itenDatails/editar-item/entradas/entrada-detalhe/saida-detalhe/emprestimo-detalhe/estoqueEstados)
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)

## Tasks (ordem; NÃO commitar)
- **est-012** Itens lista (`itens/+page.ts|svelte`) — `listarItens`; filtros `categoria`(enum)/`idLocalizacao`/`baixo`; paginação **client-side**; URL-state `search/category/location/status/page/pageSize/sort`; Exportar CSV (blob) + Importar (multipart); RBAC HIDE de escrita sem `canEdit`; testid `it-*`; estados loading/empty/error.
- **est-013** Itens detalhe `[id]` — hero+metadata reais; abas `?tab=` visao/movimentacoes/emprestimos/fornecedores/historico; movimentos via `listarEntradasPorItem`+`listarSaidasPorItem`; testid `it-detalhe`; Editar → `/itens/[id]/editar` (canEdit).
- **est-014** Itens novo — form real (nome/descrição/categoria enum/unidadeMedida/quantidadeAtual/estoqueMinimo/localização); `criarItem`; 409/422 inline; testid `it-novo-form`/`submit-item`; guarda canEdit.
- **est-015** Entradas lista — 🔴 escopo reduzido (R-9): por item via ItemPicker (`GET /entradas?idItem=`) + link detalhe; registrar decisão.
- **est-016** Entradas nova — ItemPicker + fornecedor + quantidade/valor/data/notaFiscal/observacao → `criarEntrada`; testid `ent-nova-form`; canEdit.
- **est-017** Saídas lista — espelho de est-015.
- **est-018** Saídas nova — ItemPicker + quantidade + tipoSaida (CONSUMO|PERDA|AJUSTE|EMPRESTIMO) + idReferencia + observacao → `criarSaida`; testid `sai-nova-form`; canEdit.
- **est-019** `components/estoque/MovementList.svelte` — linha clicável → `/estoque/entradas/[id]` ou `/saidas/[id]`; colunas reais; badge tipo; testid `mv-linha`.

## Regras
1. **NÃO commitar.** Ordem numérica. Tocar SOMENTE `routes/(app)/estoque/**` (itens/entradas/saidas) e `components/estoque/MovementList.svelte` (+ `ItemPicker`/`PeoplePicker` se estritamente necessário). Não tocar API/types/utils/permissions/menu/docs/specs.
2. Estados Loading/Empty/Empty-filtrado(chips)/Error(retry) nas listas; pt-BR; datas Intl; `aria-label`; testid pt-BR; tokens design-system; sem `any`/`console.log`/libs novas.
3. RBAC HIDE: ações de escrita só com `canEdit` (Estagiário vê, não edita; Recrutando já é bloqueado no layout).
4. R-9: manter escopo reduzido com aviso claro na UI ("listas globais dependem de contrato 🟡") — não inventar endpoint.
5. Ao final: `npm run test` (≥176 verdes) + `npm run check` (**0 errors** esperado; registrar warnings/files).

## Relatório
- Por task ✅/❌ + evidência (arquivos, testids, estados)
- Decisão R-9 registrada · `npm run test` · `npm run check` · pontos de atenção/desvios
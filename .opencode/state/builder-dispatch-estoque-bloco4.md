# DESPACHO — Builder (Frontend) · ESTOQUE — Bloco 4 (est-027..est-031) · 21/09/2026

> Cadastros básicos (fornecedores, localizações) + BOM.

## Contexto
- **Tasks:** `.opencode/specs/frontend/features/estoque/tasks.md` §4/§5 (`est-027..est-031`)
- **Plan:** §7 (R-8 listagem de BOM sem endpoint; 403 de fornecedores p/ não-Admin/Bolsista)
- Blocos 1–3 concluídos (`f7b3be5`, `192004c` + atual): API PT, telas existentes, rotas novas; **176 testes verdes; svelte-check 0 errors/16 warnings/7 files**
- API: `api/stock/{suppliers,locations,bom,items}.ts`; RBAC: `permissions.ts` (`canEdit`, `canVerFornecedores`)
- Mockup: `docs/mokups/estoque/` (fornecedor-novo, localizacao-novo, estoque.html BOM)
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)

## Tasks (ordem; NÃO commitar)
- **est-027** `fornecedores/+page.{ts,svelte}` — `listarFornecedores()`; search client-side (nome/cnpj/contato); tabela (nome · CNPJ mono · contato); "+ Novo fornecedor" (canEdit); 403 → EmptyState "sem permissão"; testid `for-lista`.
- **est-028** `fornecedores/novo/+page.{ts,svelte}` — form nome*/contato/cnpj (máx 32, mono) → `criarFornecedor`; redirect lista; 409/422 inline; testid `fornecedor-form`/`fornecedor-nome`/`fornecedor-cnpj`/`submit-fornecedor`; canEdit.
- **est-029** `localizacoes/+page.{ts,svelte}` — `listarLocalizacoes()`; grid cards (armário · prateleira · caixa · descrição); "+ Nova localização" (canEdit); sem capacidade/percent (não existe no backend); testid `loc-card`.
- **est-030** `localizacoes/novo/+page.{ts,svelte}` — form armario*/prateleira/caixa/descricao → `criarLocalizacao`; 409 duplicado inline; testid `localizacao-form`/`localizacao-codigo`/`submit-localizacao`; canEdit.
- **est-031** `bom/+page.{ts,svelte}` — 🔴 decisão R-8: sem listagem por projeto → `buscarBom(id)`; tabela item · quantidadePrevista · quantidadeReal · disponibilidade (via `listarItens` local); "Exportar" CSV client; escrita canEdit; testid `bom-tabela`; contrato pendente documentado.

## Regras
1. **NÃO commitar.** Ordem numérica. Tocar SOMENTE `routes/(app)/estoque/{fornecedores,localizacoes,bom}/**`. Não tocar API/types/utils/permissions/menu/docs/specs.
2. Estados Loading/Empty/Empty-filtrado/Error; 403/409/422 amigáveis; pt-BR; `aria-label`; testid pt-BR; tokens design-system; sem `any`/`console.log`/libs novas.
3. RBAC HIDE nas ações de escrita; fornecedores com guarda de leitura (403 → estado amigável).
4. R-8: não inventar endpoint; documentar decisão na UI/relatório.
5. Ao final: `npm run test` (≥176 verdes) + `npm run check` (0 errors esperado; registrar warnings/files — tentar **não aumentar** warnings).

## Relatório
- Por task ✅/❌ + evidência (arquivos, testids, estados)
- Decisão R-8 · `npm run test` · `npm run check` · pontos de atenção/desvios
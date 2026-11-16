# DESPACHO — Builder (Frontend) · ESTOQUE — Bloco 3 (est-020..est-026) · 21/09/2026

> Rotas novas: editar item, detalhes de entrada/saída, empréstimos (lista/novo/detalhe/devolução).

## Contexto
- **Tasks:** `.opencode/specs/frontend/features/estoque/tasks.md` §3 (`est-020..est-026`) — ler o bloco
- **Plan:** `plan.md` §7 (R-9 lista de empréstimos sem endpoint global; rota `emprestimos/[id]` fora do sitemap original)
- Blocos 1–2 concluídos (commits `f7b3be5` + atual): DTOs/API PT, `permissions.ts` (canEdit ampliado), telas existentes corrigidas; **176 testes verdes; svelte-check 0 errors/7 warnings/5 files**
- API disponível: `api/stock/{items,movements,loans,suppliers,locations,bom}.ts`, `api/rh.ts` (`searchPeople`)
- Componentes: `components/estoque/{ItemPicker,MovementList,PeoplePicker}.svelte`; UI kit `$lib/components/ui/*`
- Mockup: `docs/mokups/estoque/` (editar-item, entrada-detalhe, saida-detalhe, emprestimo-detalhe)
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)

## Tasks (ordem; NÃO commitar)
- **est-020** `itens/[id]/editar/+page.{ts,svelte}` — `buscarItem` + form (layout de est-014) + card read-only estoque atual → `atualizarItem`; 409/422 inline; testid `edit-item-form`; canEdit.
- **est-021** `entradas/[id]/+page.{ts,svelte}` — `buscarEntrada`; hero badge tipo derivado; metadata; 404 → "Não encontrada" + voltar; sem escrita; testid `entrada-detalhe`/`back-lista`/`entrada-error-retry`.
- **est-022** `saidas/[id]/+page.{ts,svelte}` — `buscarSaida`; espelho; testid `saida-detalhe`/`back-lista`/`saida-error-retry`.
- **est-023** `emprestimos/+page.{ts,svelte}` — tabs `?tab=ativos|atrasados|historico`; **atrasados** de `listarAtrasados()`; ativos/histórico → 🔴 decisão R-9 (aviso + orientar detalhe do item); botão "+ Novo empréstimo" (canEdit); testid `loan-tab-ativos`/`loan-return`.
- **est-024** `emprestimos/novo/+page.{ts,svelte}` — ItemPicker + quantidade + `searchPeople` (idPessoa) + observação + dataDevolucaoPrevista (hoje/futura) → `criarEmprestimo`; testid `emp-novo-form`/`submit-emprestimo`; 422 inline; canEdit.
- **est-025** `emprestimos/[id]/+page.{ts,svelte}` — `buscarEmprestimo`; status badge, prazo com tone, timeline; ação "Registrar devolução" (canEdit e status≠DEVOLVIDO) → `/emprestimos/[id]/devolucao`; testid `emprestimo-detalhe`/`btn-devolucao`/`emprestimo-error-retry`.
- **est-026** `emprestimos/[id]/devolucao/+page.{ts,svelte}` — resume + banner atraso + form (dataDevolucaoReal display) → `devolverEmprestimo` (**PUT sem body**); testid `emp-devolucao-form`/`confirmar-devolucao`; redirect `/estoque/emprestimos?tab=…`; canEdit.

## Regras
1. **NÃO commitar.** Ordem numérica. Tocar SOMENTE `routes/(app)/estoque/{itens/[id]/editar,entradas/[id],saidas/[id],emprestimos/**}`. Não tocar API/types/utils/permissions/menu/docs/specs.
2. Estados Loading/Error/Empty; 404 amigável; pt-BR; datas Intl; `aria-label`; testid pt-BR; tokens design-system; sem `any`/`console.log`/libs novas.
3. RBAC HIDE nas ações de escrita (canEdit); devolução com confirmação.
4. R-9: não inventar endpoint de lista de empréstimos; documentar a decisão na UI/relatório.
5. Ao final: `npm run test` (≥176 verdes) + `npm run check` (0 errors esperado; registrar warnings/files).

## Relatório
- Por task ✅/❌ + evidência (arquivos, testids, estados)
- Decisão R-9 registrada · `npm run test` · `npm run check` · pontos de atenção/desvios
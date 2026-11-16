# DESPACHO — Validator (contexto zero) · ESTOQUE · 21/09/2026

> Você **não** participou da implementação. Valide **do zero** contra spec + mockup. **Não altere código.**
> Working tree: branch `dev/frontend` (NÃO rode git checkout/switch).

## Artefatos
- Spec (fonte da verdade): `.opencode/specs/frontend/features/estoque/{requirements,plan,tasks}.md` (tasks.md = est-001..036)
- Mockup: `docs/mokups/estoque/` (19 telas) + `requisitos.md`/`instrunctions.md`/`full.html`
- Implementação (commits `f7b3be5` · `192004c` · `f16d0c7` · `f300d83` + testes):
  - `frontend/src/lib/types/stock.ts` (DTOs reais PT) · `utils/stock-status.ts` · `api/stock/**` (PT `/api/estoque/**`)
  - `frontend/src/lib/utils/permissions.ts` (`isResponsavelEstoque`/`canEdit('estoque')` ampliado/`canVerFornecedores`)
  - Rotas: `itens` (lista/detalhe/novo/editar), `entradas` (lista/nova/detalhe), `saidas` (lista/nova/detalhe), `emprestimos` (lista/novo/detalhe/devolucao), `fornecedores` (lista/novo), `localizacoes` (lista/novo), `bom`
  - Testes: `frontend/tests/unit/estoque/*` (80 casos) + `permissions.test.ts` (+14)

## Checklist (plan §8 + requirements)
1. **Rotas**: todas as 10 do requirements existem (itens lista/[id]/novo/[id]/editar · entradas +/nova +/[id] · saidas +/nova +/[id] · emprestimos +/novo +/[id]/devolucao · fornecedores · localizacoes · bom). Rota emprestimos/[id] fora do sitemap original documentada.
2. **API 100% PT**: nenhum `/stock/**` em path de endpoint (rg); `/api/estoque/**`; 409/422 → ApiError pt-BR; `devolverEmprestimo` PUT sem body (verificar: há `{}`? — avaliar se aceitável ou flag).
3. **RBAC** (requirements Access): View Admin/Bolsista/Voluntário/Estagiário; Edit Admin OU responsável (`isResponsavelEstoque`); Recrutando bloqueado (`/estoque` → redirect `/dashboard`); HIDE nunca disable; 403 de fornecedores → EmptyState.
4. **Item detail**: abas `?tab=` visao/movimentacoes/emprestimos/fornecedores/historico; movimentos via entradas+saidas por item; ação Editar com canEdit.
5. **Estados**: Loading/Empty/Empty-filtrado/Error em todas as listas; 404 amigável nos detalhes (itens/entradas/saidas/emprestimos/bom).
6. **URL-state**: itens (search/category/location/status/page/pageSize/sort); emprestimos `?tab=`; fornecedores busca.
7. **Empréstimos** (decisão R-9): tabs ativos/atrasados/historico; atrasados de `/emprestimos/atrasados`; ativos/histórico com aviso + orientação por item; novo (ItemPicker+PeoplePicker+prazo) e devolução (PUT sem body) com confirmação; canEdit.
8. **BOM** (decisão R-8): carregada por `?id=` via `buscarBom`; tabela + disponibilidade; exportar CSV client; registrar consumo canEdit; aviso de contrato pendente.
9. **Import/export CSV** de itens: upload multipart (`arquivo`) e download blob `itens.csv`.
10. **Tipos**: sem `any`; `CategoriaEnum` = INSUMO/FERRAMENTA/PECA; campos fictícios removidos ou marcados UI-only; sem `console.log`.
11. **Endpoints 🟡/🔴** reportados/comentados (R-9, R-8, fornecedores GET 403, `searchPeople` D-4).
12. **Sem libs novas**; pt-BR; data-testid pt-BR (`it-*`,`ent-*`,`sai-*`,`emp-*`,`for-*`,`loc-*`,`bom-*`); tokens design-system; a11y (`aria-label`); CNPJ mono.
13. **Não regressão**: `npm run test` (256 verdes) + `npm run check` (0 errors/16 warnings/7 files) + `npm run build` ok.
14. **Fora de escopo**: somente arquivos estoque + `permissions.ts` (+ `request.ts` FormData) tocados; docs/specs/menu não alterados.

## Formato do relatório
- Veredito: **APROVADA** | **APROVADA COM RESSALVAS** | **REPROVADA**
- Tabela: item × status × evidência (arquivo:linha) × observação
- Ressalvas com severidade (BLOQUEANTE / MÉDIA / BAIXA) e ação sugerida
- Comandos rodados e resultados literais
# DESPACHO — Builder (Frontend) · ESTOQUE — Bloco 1 (est-001..est-011) · 21/09/2026

> Fundação: **types DTOs reais + API PT `/api/estoque/**` + RBAC responsável + testes de permissions**.

## Contexto (ler antes)
- **Tasks (autoritativas):** `.opencode/specs/frontend/features/estoque/tasks.md` §1 (`est-001..est-011`) — leia o bloco inteiro
- **Plan:** `.opencode/specs/frontend/features/estoque/plan.md` (§7 riscos/decisões, §8 critérios)
- **Requirements:** `.opencode/specs/frontend/features/estoque/requirements.md` (rotas, RBAC, `/api/estoque/**` PT canônico)
- **Mockup aprovado:** `docs/mokups/estoque/` (NÃO editar docs)
- **Código atual:** `frontend/src/lib/api/stock/{items,movements,loans,suppliers,locations,bom}.ts` (EN — migrar), `frontend/src/lib/api/stock/client.ts` (`stockFetch`/`stockFetchBlob`), `frontend/src/lib/api/rh.ts` (`searchPeople`), `frontend/src/lib/types/stock.ts`, `frontend/src/lib/utils/stock-{status,url,format}.ts`, `frontend/src/lib/utils/permissions.ts` (⚠️ compartilhado), `frontend/src/lib/config/menu.ts`, `frontend/src/lib/components/estoque/**`, `frontend/src/routes/(app)/estoque/**`
- **Padrão:** `$lib/api/client.ts` (`apiFetch`/`ApiError`); pt-BR; tokens design-system; data-testid pt-BR (`it-*`,`ent-*`,`sai-*`,`emp-*`,`for-*`,`loc-*`,`bom-*`)
- **Branch `dev/frontend`** — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Baseline: 162 testes verdes; svelte-check **12 errors/15 warnings/7 files** (estoque/** + ui/Chip.svelte — baseline pré-existente). ⚠️ A feature estoque vai **corrigir** parte desse baseline; registre os números finais.

## Tasks (ordem; NÃO commitar)
Seguir `tasks.md` §1 literalmente:
- **est-001** `types/stock.ts` — DTOs reais (StockItem/ItemDetail, CategoriaEnum INSUMO|FERRAMENTA|PECA, LocalizacaoResp, Fornecedor, EntradaResponse/SaidaResponse, Emprestimo+loanComputed, BomResponse, CreateItemPayload/UpdateItemPayload/CreateSupplierPayload/CreateLocationPayload/MovementDetail). Manter nomes exportados usados pelas telas atuais p/ compilar.
- **est-002** `utils/stock-status.ts` — enums/computados reais (exitReasonMeta↔TipoSaida; loanComputedMeta).
- **est-003..est-008** `api/stock/{items,movements,loans,suppliers,locations,bom}.ts` — migrar para `/api/estoque/**` (itens/entradas/saidas/emprestimos/fornecedores/localizacoes/boms); **zero `/stock/`**; 409/422 → `ApiError` pt-BR.
- **est-009** `api/rh.ts` — verificar contrato (`searchPeople` → `/api/rh/people?search=`) sem mudança.
- **est-010** ⚠️ `utils/permissions.ts` — `isResponsavelEstoque` + ampliar `canEdit('estoque')`; **manter `EDIT_RULES.estoque=[0]`**; **não** alterar helpers existentes.
- **est-011** `frontend/tests/unit/permissions.test.ts` — casos novos (base intacta).

## Regras
1. **NÃO commitar.** Ordem numérica. Não tocar `docs/`, specs, `config/menu.ts`, rotas, componentes (blocos seguintes) além do necessário para manter compilação.
2. Se a mudança de assinatura das APIs quebrar as páginas existentes (`routes/(app)/estoque/**`) no typecheck, aplique **ajustes mínimos** apenas para compilar e registre-os (o bloco 2 corrige as telas integralmente). Se inviável, reporte.
3. Sem `any`/`console.log`; sem libs novas; endpoints 🟡/🔴 com TODO comentado.
4. Ao final: `npm run test` (todos verdes, ≥162) + `npm run check` (registre o número final; espere redução dos erros de estoque) + `rg "/stock/" frontend/src` deve retornar **0** em paths de API (o diretório `api/stock/` pode permanecer como pasta; apenas os endpoints devem ser PT).

## Relatório
- Por task ✅/❌ + evidência (arquivo:linha + assinaturas)
- Ajustes mínimos feitos nas páginas existentes (se houver) · `npm run test` · `npm run check` (números) · `rg "/stock/"` resultado
- Pontos de atenção/desvios (R-9/R-8/D-2 se tocar)
# DESPACHO — Builder (Frontend) · ESTOQUE — Bloco 6 (correções Validator+Security) · 21/09/2026

> Correções das ressalvas front-acionáveis de Validator (M-1/M-2) e Security (M-1/M-3/B-1/B-2/I-1..I-5).
> Aplique também o **Princípio código mínimo** — remova código não pertinente (não adicione função nova para contornar).

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Feature commitada `f7b3be5` · `192004c` · `f16d0c7` · `f300d83` · `d512324`; **256 testes verdes; 0 errors/16 warnings/7 files; build ok**
- NÃO alterar specs/docs/`config/menu.ts`

## Correções (por ordem)

### C1 (Validator M-1) — `registrarConsumo` deve receber o id da BOM
- `frontend/src/lib/api/stock/bom.ts`: `registrarConsumo(id: string, payload: { itens: {idItem;quantidade}[] })` → `POST /estoque/boms/{id}/consumo` (hoje a rota e o payload estão sem id — segue `tasks.md` est-008 `POST /boms/{id}/consumo`).
- Atualizar o call site `routes/(app)/estoque/bom/+page.svelte` para `registrarConsumo(bom.id, { itens: [...] })`.

### C2 (Validator M-2) — remover fallback de texto livre em `idFornecedor`
- `entradas/nova/+page.svelte`: **remover** o caminho que envia texto digitado como `idFornecedor` (semântica errada — backend espera id real). Quando a lista de fornecedores não carregar (403/erro), **bloquear o submit com mensagem amigável** ("Lista de fornecedores indisponível — contate o administrador") e `idFornecedor` nunca recebe texto livre. Menos código, contrato correto.

### C3 (Security M-1) — CSV injection no export da BOM (CWE-1236)
- `bom/+page.svelte` (~linha 53-78): antes do quoting, **prefixar com `'`** toda célula (string) que comece com `=`, `+`, `-`, `@`, `\t` ou `\r`. Aplicar também ao cabeçalho se dinâmico. (Mesmo princípio vale se outros exports client existirem — conferir.)

### C4 (Security M-3) — guarda de leitura de empréstimos
- `permissions.ts` já tem `canSeeLoans` (Admin/responsável). Aplicar nos loaders: `emprestimos/+page.ts`, `emprestimos/[id]/+page.ts`, `emprestimos/[id]/devolucao/+page.ts` → se `!canSeeLoans(user)` → `redirect(303, '/estoque')` (HIDE, nunca disable; perfil sem acesso não enxerga tomadores). Adicionar casos de teste se houver gancho (mínimo).

### C5 (Security B-1) — upload CSV: checagem barata no client
- `itens/+page.svelte` (`handleImport`): validar `file.type === 'text/csv' || file.name.endsWith('.csv')` e `file.size <= 10_000_000` antes de montar o FormData; erro pt-BR amigável. Backend continua autoridade (limite/validação).

### C6 (Security B-2) — prazo do empréstimo ≥ hoje
- `emprestimos/novo/+page.svelte` (`validate()`): adicionar `dataDevolucaoPrevista >= hoje` → erro inline pt-BR ("Data deve ser hoje ou futura").

### C7 (Security I-1) — `devolverEmprestimo` PUT sem body
- `api/stock/loans.ts`: **remover** `body: JSON.stringify({})` (PUT bodyless; sem Content-Type json). Ajustar teste `api-stock.test.ts` correspondente (esperar `body === undefined`).

### C8 (Security I-2) — branch redundante
- `devolucao/+page.svelte`: remover o `if (err 409)` cujos dois braços fazem o mesmo `toastError`.

### C9 (Security I-3) — remover código morto (código mínimo)
Remover (após confirmar nenhum consumidor por grep):
- `api/stock/items.ts`: `deleteItem`, `fetchStatuses`, `exportItemsCsv`
- `api/stock/suppliers.ts`: `listarFornecedoresOptions`
- `api/stock/bom.ts`: `generateOrder`, `fetchProjects`
- `utils/permissions.ts`: `canVerFornecedores` **apenas se** ninguém consumir além de testes (se teste cobre, remover teste junto — páginas tratam 403 do backend diretamente)
- `components/estoque/ItemPicker.svelte`: prop `onUnit` (sem uso)
- `components/estoque/PeoplePicker.svelte`: prop `defaultOption` (sem uso)
- `types/stock.ts`: tipos/campos legados sem uso (`Loan`, `Movement`, `Supplier`, `HistoryEntry`, campos legados de `ItemDetail`) — remover ou marcar `// UI-only` se usado
Ao remover, rodar testes e corrigir referências em testes se necessário.

### C10 (Security I-4) — `sanitizeRedirect` no `loginRedirectTarget`
- `api/stock/request.ts`: aplicar `sanitizeRedirect` ao `loginRedirectTarget` (consistência com `client.ts`; sem mexer em outras features).

### C11 (Security I-5) — nome do arquivo de export da BOM
- `bom/+page.svelte`: sanitizar nome (ex.: `String(bom.nome).toLowerCase().replace(/[^a-z0-9-_]/g, '-')` ou fallback `bom.csv`).

## Regras
1. **NÃO commitar.** Ordem C1..C11. Tocar SOMENTE os arquivos acima (+ testes relacionados). Não alterar specs/docs/menu.
2. Princípio código mínimo: estas correções devem **reduzir ou manter** o código (C2/C8/C9 removem). Não adicionar helper novo a menos que imprescindível para C3/C5/C6.
3. Ao final: `npm run test` (≥256 ou o que a remoção de código justificar — todos verdes) + `npm run check` (0 errors; registrar warnings) + `npm run build` ok.

## Relatório
- Por correção C1..C11 ✅/❌ + evidência (arquivo:linha) + o que mudou (linhas +/−)
- `npm run test` (contagem final) · `npm run check` · `npm run build` · pontos de atenção/desvios
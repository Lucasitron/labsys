# DESPACHO — Builder (Frontend) · FINANCEIRO — Bloco 9 (correções Validator+Security) · 24/09/2026

> Correções front-acionáveis: Validator R-M1/R-B1 (+ notas R-M2/R-B2..B-4 feitas pelo orquestrador no plan) e Security B1–B4.
> **Princípio código mínimo: o saldo deve reduzir linhas (C1 remove ~3 duplicações); nada especulativo; nenhum helper novo além do pedido.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Feature commitada: `2e78ed8 · 4ccc9e3 · b9a6eb0 · 49cbfb2 · 06aa0f6`; **524 testes verdes; check 0 errors/17 warnings/8 files; build ok**
- NÃO alterar specs/docs/requirements; NÃO commitar

## Correções

### C1 (Validator R-M1 + Security I1) — extrair modal "Novo lançamento" compartilhado
- Criar `src/routes/(app)/financeiro/components/NovoLancamentoModal.svelte` (partial local às rotas — NÃO no kit): props `{ tipoFixo?: 'Entrada'|'Saída', onClose, onCreated }`; unificar os 4 blocos `modalNovo/confirmarNovo` de `financeiro/+page.svelte`, `lancamentos/+page.svelte`, `contas-pagar/+page.svelte`, `contas-receber/+page.svelte` (Categoria/Tipo/Valor/MoneyInput/Vencimento/Pagamento/Referência/Observação + validações + `createLancamento` + toasts). Quando `tipoFixo` presente, **omitir o fieldset de Tipo** (resolve também Security B4) e enviar o literal.
- Substituir os 4 modais locais pelo componente (preservar `data-testid`s e comportamento; `contas-pagar` passa `tipoFixo='Saída'`, `contas-receber` `tipoFixo='Entrada'`).
- Atualizar `financeiro-pages.test.ts` se algum teste quebrar por causa da extração (testids devem continuar passando).

### C2 (Security B1) — validar `?periodo=`
- `relatorios/+page.ts:47`: `const periodo = /^\d{4}-\d{2}$/.test(bruto) ? bruto : PERIODO_PADRAO;`
- `lancamentos/+page.ts:46` e `doacoes/+page.ts:28`: allowlist conforme valores reais de UI (conferir `PERIODO_OPCOES`/equivalentes nos arquivos; fallback para o default quando fora da lista).

### C3 (Security B2) — `encodeURIComponent` nos ids de path
- `api/financeiro/lancamentos.ts:37,57`, `custeio.ts:40`, `solicitacoes-compra.ts:41`: `` `${BASE}/${encodeURIComponent(id)}` `` (e `idEncomenda`).

### C4 (Security B3) — bulk com `allSettled`
- `lancamentos/+page.svelte:315-322`: `Promise.all` → `Promise.allSettled` + toast com contagem (`${ok} registrados; ${fal} falharam — confira a lista.` / sucesso quando 0 falhas). Manter `invalidateAll()`.

### C5 (Security B4) — tipo fixo nas contas
- Coberto por C1 via `tipoFixo` (remover o fieldset de Tipo nessas duas vistas). Se C1 não cobrir algum ponto, fixar literal `tipo: 'Saída'` / `'Entrada'` no payload.

### C6 (Validator R-B1) — label honesto no resumo
- `financeiro/+page.svelte` card "Categorias com maiores saídas" → renomear para **"Categorias mais frequentes"** (admite contagem até D-2 servir valores).

## NÃO fazer (orquestrador documenta no plan/contract; backend)
- R-M2 (filtros `vencimento`/`origem` → D-8 no plan), R-B2 (série mensal → D-2 backend), R-B3 (testid literal auditado via testes — aceito sem mudança), R-B4 (pageSize 10 fixo nas contas → travado no plan), I2–I6 (aceitos/opcionais transversais).

## Regras
1. Tocar SOMENTE: `routes/(app)/financeiro/**` (incl. novo `components/NovoLancamentoModal.svelte`), `lib/api/financeiro/*`, testes financeiro. **NÃO commitar.**
2. pt-BR; sem `any`; Svelte 5; tokens; código mínimo (C1 deve dar saldo negativo de linhas).
3. Ao final: `npm run test` (**suíte CHEIA**) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- Por correção C1..C6 ✅/❌ + evidência (arquivo:linha) + linhas +/− (C1 com saldo)
- Total de testes · check · build · desvios
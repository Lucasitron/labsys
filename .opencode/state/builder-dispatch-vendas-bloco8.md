# DESPACHO — Builder (Frontend) · VENDAS — Bloco 8 (correções Validator+Security) · 23/09/2026

> O Security reprovou (H1–H4, M1–M7) + Validator M-1/B-1..B-4. Todas as correções abaixo são front-acionáveis e mínimas.
> **Princípio código mínimo: verificar necessidade/verbosidade; nada especulativo; não criar helpers novos.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Feature commitada: `ed7dbcd · 7e3eb8b · 0a00ccf · 83ff024 · 3ad1396 · 7beb58b`; **451 testes verdes; check 0 errors/17 warnings/8 files; build ok**
- NÃO alterar specs/docs/requirements; NÃO commitar. **Exceção:** `src/lib/api/client.ts` (compartilhado — correção M6 cirúrgica + suíte cheia p/ regressão)

## Correções

### C1 (H1) — gate de criador em mover/avançar/converter
- `encomendas/+page.svelte` `mover()` (~:160-177): após fechar popover, `if (!canEditVendas(usuario, { createdBy: enc.createdBy })) { toasts.warn('Somente o criador ou Admin pode mover esta encomenda.'); return; }` (importar `canEditVendas` + `auth`/`get` se ausentes; `usuario = $derived(get(auth).user)`).
- `encomendas/[id]/+page.svelte` `avancar()` (~:99-116): mesmo gate com `encomenda.createdBy`.
- `orcamentos/[id]/+page.svelte`: `{#if mostraConversao}` → `{#if mostraConversao && podeEditar}` + no início de `criarEncomendaDoOrcamento()` o mesmo early-return com toast.

### C2 (H2) — gate em `mudarStatus` (marketing)
- `marketing/+page.svelte` `mudarStatus()` (~:119-131): early-return `!canEditVendas(usuario, { createdBy: tarefa.createdBy })` + toast; ocultar as RowActions concluir/andamento/reabrir quando `!canEditVendas` (hidden, nunca disabled).

### C3 (H3) — máscara de documento
- `utils/vendas-format.ts`: adicionar `export function maskDocumento(doc: string): string` (só-dígitos; se >4 dígitos `••••${4 últimos}`, senão `••••`).
- `clientes/[id]/+page.svelte` (~:440): atrás de gate — `{#if podeEditar}{documento integral}{:else}{maskDocumento(...)}{/if}` (conferir nome da var `podeEditar`/equivalente no arquivo).
- `orcamentos/novo/+page.svelte` dropdown (~:508): trocar `{c.codigo} · {c.documento}` por `{c.codigo} · {c.nome}` (remover documento da busca).
- Contrato (backend remover `documento` da busca) fica como fila — só front aqui.

### C4 (H4) — gate no bulk-tag
- `clientes/+page.svelte` `confirmarAdicionarTag()` (~:217-233): filtrar selecionados alheios (`!canEditVendas(usuario, { createdBy: c.createdBy })`); se houver, toast warn + return (não executar parcial).

### C5 (M1) — re-check nos handlers + 403
- Antes de cada chamada de escrita, re-validar (padrão 1 linha): `confirmarExcluir` (lista ~:190-205 e detalhe ~:99-110), `salvar()` em `clientes/novo` (~:269-272), `salvar()` em `orcamentos/novo` (~:370-371), `confirmarDecisao` em `solicitacoes` (~:107, com `canDecideVendas`), `salvarVenda`/`salvarTarefa` (criação liberada — só adicionar tratamento 403 no catch).
- Nos catches: `if (err instanceof ApiError && err.status === 403) toasts.warn('Sem permissão para esta ação.')` (importar `ApiError` onde faltar; conferir import existente).

### C6 (M2) — `err.message` → `toUserMessage`
- Trocar os 8 pontos (`clientes/[id]` :133/:216/:244, `clientes/novo` :360, `orcamentos/novo` :160/:230, `encomendas` :245, `marketplace` :166) de `err instanceof Error ? err.message : ...` para `toUserMessage(err).message` (importar de `$lib/utils/errors`).

### C7 (M3) — `canSee` em Solicitações
- `config/menu.ts`: `{ label: 'Solicitações de edição', ..., canSee: canDecideVendas }` (importar `canDecideVendas`).

### C8 (M5) — layout sem `canEdit` morto + usuario reativo
- `vendas/+layout.ts`: remover `canEdit` do retorno (confirmar por grep que nada consome `data.canEdit` em vendas/ antes).
- `clientes/novo/+page.svelte:17`: `const usuario = get(auth).user` → `const usuario = $derived(get(auth).user)` (ajustar usos).

### C9 (M6) — 204/empty em `apiFetch`
- `src/lib/api/client.ts` (~:107): `if (response.status === 204) return undefined as T;` + `const text = await response.text(); return (text ? JSON.parse(text) : undefined) as T;`. **Rodar a suíte CHEIA** (regressão de todas as features).

### C10 (M7-parcial) — 409 com refetch
- Nos catches 409 de `encomendas/+page.svelte` (~:169-170) e `encomendas/[id]/+page.svelte` (~:108-109): adicionar `await invalidateAll();` após o toast. **NÃO** adicionar `version`/`If-Match` (backend não suporta — especulativo).

### C11 (B1+B2) — mailto/tel
- `clientes/[id]/+page.svelte`: `mailto:${encodeURIComponent(cliente.email)}` + só renderizar `<a>` se email válido (usar a regex já existente no `novo`); telefone: `{#if digitos}<a>{:else}<span>—</span>{/if}` em vez de `'#'`.

### C12 (B3+M4-front) — duplicar sem PII + busca mínima
- `clientes/novo/+page.svelte`: em `?duplicar=` (não em `?editar=`), zerar também `telefone` e `endereco` (manter padrão de documento/email zerados).
- `orcamentos/novo/+page.svelte`: remover `email` de `ClienteSel` (~:62/:95/:165) e a chamada extra `getCliente()` (~:93-97) que só servia para preenchê-lo; tipar resultado da busca como `Pick<Cliente,'id'|'nome'|'codigo'>` onde aplicável sem quebrar tipos (se quebrar, manter e registrar).

### C13 (B4) — `decididoPor` honesto
- `types/vendas.ts` `SolicitacaoEdicao`: adicionar `decididoPor?: string; decididoEm?: string` (opcionais).
- `solicitacoes/+page.svelte` (~:302-307): solicitante continua como solicitante; decisão renderiza `Decidida por {decididoPor ?? '—'}` (+ data se houver). Sem inventar dado.

### C14 (B5+B6) — ViaCEP aviso + taxa com teto
- `clientes/novo/+page.svelte` (bloco CEP): microcopy "CEP usado apenas para autocompletar o endereço (ViaCEP)" + timeout 8s no `AbortController` (`setTimeout` → `abort()`, limpar no sucesso/erro).
- `marketplace/+page.svelte` (validação ~:185-186): `if (taxa > valorFinal) erro = 'Taxa não pode exceder o valor da encomenda.'`.

### C15 (Validator M-1) — modal reativo
- `SolicitarEdicaoModal.svelte:27`: `$effect(() => { campoAtual = campo; })` após a declaração (elimina o warning `state_referenced_locally`).

### C16 (Validator B-2+B-3) — Kanban honesto
- `encomendas/+page.svelte:78`: `count: servida ?? daColuna.length` → `count: servida ?? 0` (com `—` quando nulo, padrão `fmtContagem`).
- Mover `data-testid="kanban-col-{slug}"` para o container da coluna (manter ou remover o do span interno — sem duplicar testid).

### C17 (Validator B-4) — Timeline com título honesto
- `orcamentos/[id]/+page.svelte` (~:84-107): prefixar o título da seção com "Resumo" (ex.: "Resumo do orçamento") para não confundir com histórico real. 1-3 linhas.

## NÃO fazer (fila backend/aceitos)
- H2-backend/403 por criador, M4-contrato (projeção mínima), B5c (truncar no backend), B6-backend (validar taxa), B7 (validar `clienteId`/tenant), M7-`version`, Validator B-1 (documentado pelo orquestrador), M3-goto (padrão aceito), I6 (403→texto em `toUserMessage` — transversal, fora do escopo vendas).

## Regras
1. Tocar SOMENTE: `routes/(app)/vendas/**`, `lib/api/vendas/*`, `lib/utils/vendas-format.ts`, `lib/types/vendas.ts`, `lib/api/client.ts` (só C9), `lib/config/menu.ts` (só C7), testes vendas. **NÃO commitar.**
2. pt-BR; sem `any`; Svelte 5; tokens; código mínimo.
3. Ao final: `npm run test` (**suíte CHEIA**) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- Por correção C1..C17 ✅/❌ + evidência (arquivo:linha) + linhas +/−
- Total de testes · check · build · desvios
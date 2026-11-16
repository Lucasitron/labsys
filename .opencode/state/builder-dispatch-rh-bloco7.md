# DESPACHO — Builder (Frontend) · RH — Bloco 7 (correções Validator+Security) · 23/09/2026

> Correções front-acionáveis dos relatórios Validator (M1/M2/M3/B1) e Security (H1/M1/M2/M4/M5/B1/B2/B3/B4).
> **Princípio código mínimo: saldo deve reduzir ou manter linhas onde possível; NÃO criar abstrações novas.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Feature commitada: `4b33407 · 9d76271 · 642a856 · b8c4a8c · 607084e · 4406cb0`; **381 testes verdes; check 0 errors/16 warnings/7 files; build ok**
- NÃO alterar specs/docs; NÃO alterar `permissions.ts` (só consumir helpers); NÃO commitar

## Correções

### C1 (Security H1 — ALTO) — opção `admin` só para admin nas trocas de nível
- Nos 4 pontos que oferecem `admin` a qualquer `canEdit`, filtrar por `isAdmin(usuario)` (importar de `$lib/utils/permissions`):
  1. `pessoas/+page.svelte` `NIVEL_OPCOES` (~:51-53) usada no bulk `confirmarAlterarNivel`
  2. `pessoas/[id]/editar/+page.svelte` `NIVEIS_OPCOES` (~:291)
  3. `pessoas/novo/+page.svelte` `NIVEL_OPCOES` (~:49-51)
  4. `pessoas/niveis/+page.svelte` select de troca (~:296-311) e `NIVEIS_FALLBACK` do convite (~:367-377)
- Padrão: `NIVEL_OPCOES.filter(n => n.id !== 'admin' || isAdmin(usuario))` (adaptar ao shape real de cada lista — conferir se é string[] ou {id,label}[] antes de editar).
- Backend 403 fica como fila (não é escopo).

### C2 (Validator M1 + Security B2) — `deletePessoa` na camada API
- `src/lib/api/rh/pessoas.ts`: adicionar `export async function deletePessoa(id: string, fetchFn = fetch)` com `bearer()` + `apiFetch('/api/rh/pessoas/{id}', { method: 'DELETE', headers })` (mesmo padrão das demais).
- `pessoas/+page.svelte` `excluirPessoa` (~:157-195): trocar o `apiFetch` inline por `await deletePessoa(id)` (remover `get(auth)` manual dali se ficar sem uso no arquivo — conferir outros usos antes).
- Adicionar 1 caso em `rh-api.test.ts` (path + método DELETE).

### C3 (Validator M3) — toast honesto na agenda
- `treinamentos/agenda/+page.svelte` `emBreve` (~:115-117): trocar corpo para `toasts.warn('Ação indisponível: endpoint de agenda pendente (D-9).')` (1 linha; manter hide sob canEdit).

### C4 (Security B1) — hints de erro sem mensagem bruta do backend
- Em todas as páginas `pessoas/**`, trocar `hint={erro}` (onde `erro` carrega `err.message` cru) por hint estático: `hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."`
- Locais (confirmar por `grep -rn "hint={erro}\|hint={error" "src/routes/(app)/pessoas"`): `+page.svelte`, `[id]/+page.svelte` (2×), `editar/+page.svelte`, `niveis/+page.svelte`, `processo-seletivo/+page.svelte`, `registro-horas/+page.svelte`, `agenda/+page.svelte`, `documentacao/+page.svelte`, etc.
- Nos `+page.ts` que retornam `err.message` para a UI (`pessoas/+page.ts:43-48` e similares em `[id]`/editar): trocar por mensagem genérica pt-BR (a página já mostra o banner; o detalhe técnico fica no console via `console.error`? **NÃO** — sem console; apenas mensagem genérica).
- Toasts com `toUserMessage` permanecem como estão.

### C5 (Security B4) — link de Níveis só para admin no editar
- `pessoas/[id]/editar/+page.svelte` (~:373-381): envolver o `<a href="/pessoas/niveis">` em `{#if isAdmin(usuario)}` (importar `isAdmin`; `usuario` — conferir nome da variável real no arquivo).
- Ajustar o texto do `edit-rbac-banner` (~:150-163) de "não são exibidos" para "os campos estão desabilitados" (precisão, já que o fieldset permanece no DOM com `disabled`).

### C6 (Security M1-detalhe/M2/M4, escopo role 3/4) — registros sensíveis só próprios para Estagiário/Recrutando
- `pessoas/[id]/+page.svelte`: quando `(usuario?.role === 3 || usuario?.role === 4) && !isProprioRegistro(usuario, id)` (importar `isProprioRegistro` de `$lib/utils/permissions`; `id` = params da rota — conferir variável real), **ocultar as tabs Horas/Treinamentos/Histórico** (manter Visão geral + hero). Sem redirect (evita armadilha de 404 quando o mapeamento token→pessoa D-7 não existir).
- `pessoas/registro-horas/+page.svelte`: ocultar a seção de KPIs do extrato global quando `ehEstagiario` (a tabela própria filtrada permanece).
- Isso consome `isProprioRegistro` (resolve Validator B1 / Security I2). **NÃO** restringir a lista `/pessoas` (sem mapeamento confiável D-7 — fica para o backend D-3/D-7).

### C7 (Security B3) — remover legado não utilizado em `horas.ts`
- Por `grep`, confirmar quais de `listHoras` / `registrarHoras` / `validarHoras` (legado `/api/rh/horas`) têm **zero consumidores** em `src/` e testes. Remover as confirmadas mortas. **MANTER** `rejeitarHoras` (usado como fallback documentado D-5) e toda a nova modelagem.
- Atualizar `rh-api.test.ts` removendo/ajustando os casos das funções removidas (sem perder os asserts R-5 de ausência de soma).

### C8 (Security M5, só tipos) — extras do payload tipados
- `src/lib/types/rh.ts`: estender `CreatePessoaPayload` / `UpdatePessoaPayload` com os campos opcionais reais enviados (`cpf?`, `dataNascimento?`, `cursoTurma?`, `username?`, `senha?`, `dataAdmissao?`, `department?`, `shift?`, `status?` — conferir os spreads reais em `novo/+page.svelte` e `editar/+page.svelte`).
- Trocar os spreads `Record<string,string>` + `as CreatePessoaPayload` por objetos tipados (remover o `as`). **Sem mudar comportamento nem remover o campo CPF** (mockup-spec; fila D-2 confirma necessidade/retenção com PO).

## NÃO fazer (registrado como ressalva/aceito)
- H2/M1-backend/M2-backend/M4-backend/B5/B6/B7, Validator M2-lista, M3-guards (`goto` aceito como padrão `ssr=false`), I3 (`fetchCancelavel` duplicado — extração arriscada, fica p/ futuro), `pageSize` na URL (B2, conforme mockup RF-P1), `Tone` local, `searchPeople` legado (estoque depende).

## Regras
1. Tocar SOMENTE: `routes/(app)/pessoas/**`, `lib/api/rh/pessoas.ts`, `lib/api/rh/horas.ts`, `lib/types/rh.ts`, testes rh. **NÃO commitar.**
2. pt-BR; sem `any`; Svelte 5 runes; tokens; código mínimo.
3. Ao final: `npm run test` (todos verdes; total) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- Por correção C1..C8 ✅/❌ + evidência (arquivo:linha) + linhas +/−
- Total de testes · check · build · desvios
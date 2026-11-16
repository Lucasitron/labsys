# DESPACHO — Builder (Frontend) · CONFIGURACOES — Bloco 4 (correções Validator+Security) · 21/09/2026

> Correções das ressalvas **frontend-acionáveis** de Validator (R5/R6/R7) e Security (B1/B2/I3).
> Ressalvas de contrato (R1–R4, B3, M1/M2, I1) → **não implementar**; serão reportadas ao PO/backend.

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Estado: feature `configuracoes` commitada (`d06833d`, `d9d0875`, `ee895ae`); 162 testes verdes; check 12/15/7
- Fontes: `.opencode/specs/system/rbac-matrix.md` (matriz oficial), `docs/mokups/configuracoes/full.html`

## Correções

### C1 (B1) — `DEFAULT_MATRIZ` alinhada à matriz oficial
- **Arquivo:** `frontend/src/routes/(app)/configuracoes/permissoes/+page.svelte` (const `DEFAULT_MATRIZ`)
- Revisar **todos** os módulos contra `rbac-matrix.md` e usar defaults conservadores: `Editar`→'Editar'; `Visualizar`→'Ver'; `–`/sem acesso→'Nenhum'; "Editar se responsável"→'Ver' (edição condicional não é default da matriz).
- Casos citados: `configuracoes` → 0 'Editar', 1..4 **'Nenhum'**; `rh` → 0 'Editar', 1..4 **'Ver'**.
- Adicionar comentário `// alinhado a .opencode/specs/system/rbac-matrix.md (defaults conservadores)`.

### C2 (B2) — `<img>` do logo
- **Arquivo:** `frontend/src/routes/(app)/configuracoes/sistema/+page.svelte`
- Adicionar `referrerpolicy="no-referrer"` e `loading="lazy"` à `<img>` do logo.

### C3 (R5) — Escape fecha modais
- **Arquivo:** `frontend/src/lib/components/ui/Modal.svelte` (melhoria transversal mínima)
- Adicionar `{#if open}<svelte:window onkeydown={...} />{/if}` fechando em `Escape` via `onClose` (ignorar se `event.key !== 'Escape'`).
- Verificar que `ModalConfirm.svelte` e `ModalUsuario.svelte` (configuracoes) usam `Modal` (herdam Escape) ou, se não usarem, aplicar o mesmo listener neles. **Não** alterar comportamento/backdrop existente.

### C4 (R6) — debounce da busca de Usuários
- **Arquivo:** `frontend/src/routes/(app)/configuracoes/usuarios/+page.svelte`
- Passar `delay={300}` ao `SearchInput` (mockup §10).

### C5 (R7) — admin derivado do layout
- **Arquivos:** `frontend/src/routes/(app)/configuracoes/{usuarios,permissoes}/+page.svelte` (e `+layout.ts`/`+layout.svelte` se necessário)
- Expor `isAdmin` no retorno do `load` do `+layout.ts` (ou `+layout.server`-like via parent data) e consumir `data.isAdmin` em vez de `admin={true}` hardcoded. Se o padrão do projeto não permitir data de layout client-side, documentar e manter — **não** inventar store.

### C6 (I3) — fallback do `getPerfil`
- **Arquivo:** `frontend/src/lib/api/configuracoes/perfil.ts`
- Cair para `/auth/me` **apenas** em erro de rede (`NetworkError`) ou 5xx; **propagar** 401/403/404 sem fallback silencioso.

## Regras
1. **NÃO commitar.** Tocar SOMENTE os arquivos acima; não alterar specs/docs/outras features; sem libs novas; sem `any`/`console.log`.
2. Manter 162 testes verdes; `npm run check` baseline 12/15/7 (zero novos).
3. Ao final, `npm run test` + `npm run check` e diff resumido por arquivo.

## Relatório
- Por correção C1..C6 ✅/❌ + evidência (arquivo:linha) + o que mudou
- Qualquer correção não aplicável (explicar) · comandos/resultados
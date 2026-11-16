# DESPACHO — Builder (Frontend) · CONFIGURACOES — Bloco 3 (cfg-020..cfg-021) · 21/09/2026

> Escopo: **testes** da feature `configuracoes`.

## Contexto
- Tasks: `.opencode/specs/frontend/features/configuracoes/tasks.md` (`cfg-020`, `cfg-021`)
- Blocos 1–2 concluídos (`d06833d` + commit das rotas): types, API, RBAC (`isAdmin`/`canViewConfiguracoes`/`hasPermissao`), componentes, 4 telas + guarda
- Testes existentes: `frontend/tests/unit/permissions.test.ts` (49 casos, inclui producao). **Playwright NÃO instalado** (`frontend/tests/e2e/` não existe) — cfg-021 será SKIP se não houver suite e2e (não instalar nada)
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Baseline: 150 testes verdes; svelte-check 12/15/7

## Tasks
### cfg-020 · `frontend/tests/unit/permissions.test.ts` — modify
Adicionar casos (sem remover os existentes):
- `isAdmin`: role 0 → true; roles 1..4 → false; user null → false
- `canViewConfiguracoes`: Admin true; roles 1..4 false (perfil é tratado à parte)
- `hasPermissao(user, modulo, acao, matriz?)`: Admin true; `'Nenhum'` → false; `'Ver'`/`'Editar'` com matriz; fallback sem matriz usa `canView`/`canEdit`
- garantir que os helpers de producao (canSee5S etc.) seguem verdes

### cfg-021 · e2e — condicional
Verificar `frontend/package.json` (`@playwright/test`?) e `frontend/tests/e2e/`. Se **não** existir → **SKIP** e reportar (não instalar). Se existir → criar `frontend/tests/e2e/configuracoes.spec.ts` (perfil editar nome/senha; usuários convite; permissões toggle+salvar; sistema criar/revogar token; guarda voluntário → redirect em `/configuracoes/usuarios`).

## Regras
1. **NÃO commitar.** Tocar SOMENTE `frontend/tests/unit/permissions.test.ts` e (condicional) `frontend/tests/e2e/configuracoes.spec.ts`. Não alterar código de produção/specs/docs. Não instalar dependências.
2. Seguir padrão dos testes vitest existentes.
3. Ao final: `npm run test` (≥150, todos verdes) + `npm run check` baseline 12/15/7.

## Relatório
- Por task ✅/❌/SKIP + evidência (nº de casos)
- Contagem final `npm run test`; `npm run check`
- Pontos de atenção/desvios
# DESPACHO — Builder (Frontend) · PRODUCAO — BLOCO 6 (prd-040..prd-042) · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **testes** da feature `producao` (unit status, unit RBAC, e2e condicional).

## Contexto (ler antes)
- Tasks: `.opencode/specs/frontend/features/producao/tasks.md` (`prd-040..prd-042`); DoD §8 do plan.md
- Blocos 1–5 concluídos (`c4406f4`, `84b643a`, `a011991`, `7bc70f5`, `6863ce3`): implementação completa das 42 tasks
  - `utils/producao-status.ts`: maps `statusMeta` (projeto/tarefa/kanban/máquina/chamado/cartão/pendência/penalidade/mesa) + `notaToCartao` (≥90 Verde · <70 Vermelho · senão Amarelo) + `gravidadeMeta`
  - `utils/permissions.ts` (bloco 4): `canSee5S` (Admin true; grant `roles.producao` + role 1..3; Recrutando/role 4 false; `null` false), `isResponsavelAtribuido(user,{responsavelId})`, `canAuditar5S`, `canSeeAdvertencias` (Admin-only), `canEditProducao` — além dos já existentes (canView/canEdit/canSeeLoans/canSeeMachines)
  - `types/auth.ts`: `Module`, `Role` (0..4), `User` (role, roles per module, responsibilities)
- Testes existentes: `frontend/tests/unit/*.test.ts` — vitest (pkg padrão, sem Playwright instalado até a data; **verificar `package.json` se há `@playwright/test`**)
- 95 testes verdes atualmente · baseline svelte-check 12/15/7 (estoque/** + ui/Chip.svelte)

## Tasks

### prd-040 · `frontend/tests/unit/producao-status.test.ts` — create
Cobrir `$lib/utils/producao-status`:
- `notaToCartao`: ≥90 → 'Verde' (incl. 90 e 100), <70 → 'Vermelho' (incl. 0 e 69), entre → 'Amarelo' (70..89, borda 70 e 89)
- Todos os `*_META` maps: chaves existem e têm `label` pt-BR + `color` válida (constante do design-system; conferir valores aceitos nos componentes)
- `gravidadeMeta`: chaves e labels
- (se houver `slugTestId`/helpers extras no módulo — ver código real — incluir caso)

### prd-041 · `frontend/tests/unit/permissions.test.ts` — modify
O arquivo já existe? **Verificar**; se sim, adicionar casos (não remover existentes); se não existe, **create**:
- `canSee5S`: Admin (role 0) true; role 4 (Recrutando) false mesmo com `roles.producao='view'`; role 1..3 com grant 'view'/'edit' true; sem grant false; `roles.producao===null` false; user null false
- `isResponsavelAtribuido`: Admin true (com/sem recurso); id presente em `responsibilities.producao` true; ausente false; recurso sem responsavelId false; user null false
- `canAuditar5S`: Admin true; auditor nomeado true; fallback role 1..2 true
- `canSeeAdvertencias`: Admin true; role 0 true; `roles.producao==='edit'` true (não-admin); role 4 false
- `canEditProducao`: role 0 true; com recurso não-atribuído false; role 1 sem recurso false (EDIT_RULES.producao=[0])
- matrix: VIEW_RULES/EDIT_RULES intactos como baseline

### prd-042 · e2e — condicional
- Verificar se suite e2e existe (`package.json` tem `@playwright/test`? `tests/e2e/` existe?). Se **não** houver, **pular e reportar como skip** (nao instalar nada). Se houver, criar `frontend/tests/e2e/producao.spec.ts`: fluxo Admin (resumo → projetos → detalhe → tarefas → máquinas) e guarda Recrutando (redirect `/producao` ao acessar `/producao/5s/setores`).

## Regras
1. **NÃO commitar.** Tocar SOMENTE: `frontend/tests/unit/producao-status.test.ts` (create/modify) e `frontend/tests/unit/permissions.test.ts` (create/modify) **e** eventual `tests/e2e/producao.spec.ts` (só se suite existir). NÃO modificar código de produção nem specs/docs.
2. Usar padrão dos testes existentes (`tests/unit/auth-store.test.ts`, etc.) para imports/estrutura; vitest; rodas `describe` em pt-BR ou EN conforme padrão do repo.
3. **NÃO instalar dependências** (`npm i`) — se precisar de lib ausente para fazer o teste rodar, reportar ao Orchestrator em vez de instalar.
4. Ao final: `npm run test` deve ficar **≥ 95** (verde, com os novos inclusos) e `npm run check` baseline inalterado.

## Relatório
- Por task ✅/❌ (+skip) + evidência (arquivo, nº de casos, wie verificado)
- Confirmar se `permissions.test.ts` já existia; contagem final de testes (`npm run test`); svelte-check
- Pontos de atenção / desvios
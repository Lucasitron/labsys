# DESPACHO — Builder (Frontend) · PRODUCAO — Bloco 4 (prd-025..prd-027) · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **layout + RBAC compartilhado** da feature `producao` —
> `menu.ts`, `permissions.ts`, scaffold das rotas `(app)/producao`. ⚠️ C1/C2: **sequencial com
> configracoes — NUNCA paralelo** (esta feature é a única a tocar esses arquivos neste momento).

---

## Contexto (ler antes)

- Tasks: `.opencode/specs/frontend/features/producao/tasks.md` (`prd-025..prd-027`)
- Plano: `.opencode/specs/frontend/features/producao/plan.md` (§5 RBAC, §8 DoD); Mockup `docs/mokups/producao/`; requirements §Decorrer (regras RBAC)
- Blocos 1–3 concluídos (`c4406f4`, `84b643a`, `a011991`): types, utils, 12 módulos API, components (CartaoBadge + 8 modais)
- Estado atual de `menu.ts`: entrada simples `{ label:'Produção', path:'/producao', module:'producao', icon:'producao' }` (item em `menuItems` L40), sem `children`; `MenuIcon` já tem `'producao'`
- Estado atual de `permissions.ts`: `VIEW_RULES.producao=[0,1,2,3]` · `EDIT_RULES.producao=[0]` · `canSeeMachines(user)` já existe (retorna role 0 ou responsibilities.producao com itens)
- `types/auth.ts`: `Module` inclui `'producao'`; `Role` 0..4 (Admin/Bolsista/Voluntário/Estagiário/Recrutando); `User.roles` por módulo `'view'|'edit'|null`; `User.responsibilities?.producao?: string[]`; `user.role===0` = Admin
- Guarda de app existente: rotas `(app)` usam `+layout.server.ts`/`+layout.ts` com `redirect('/auth/login')` se sem sessão — seguir o mesmo padrão (ver `(app)/dashboard/+page.ts` e o layout raiz)
- 95 testes verdes · baseline svelte-check 12/15/7 (estoque/** + ui/Chip.svelte) · **torre de nav**: menu (sidebar) consome `menuItems`

## Tasks do bloco (o Builder toca SOMENTE estes — 2 modify + 2 create)

### prd-025 · `frontend/src/lib/config/menu.ts` — modify ⚠️
- Transformar a entrada **Produção** em item com `children` (~9 itens): **Resumo** `/producao` ·
  **Projetos** `/producao/projetos` · **Tarefas** `/producao/tarefas` · **Máquinas** `/producao/maquinas` ·
  **5S** — mas o submenu 5S (Setores/Auditoria/Pendências/Ranking/Advertências/Mesas) entra como
  **filhos também na mesma lista** (sem submenu aninhado — a sidebar só suporta 1 nível; manter o
  padrão do Estoque): label `5S: Setores` path `/producao/5s/setores`, `5S: Auditoria` `/producao/5s/auditoria`,
  `5S: Pendências` `/producao/5s/pendencias`, `5S: Ranking` `/producao/5s/ranking`,
  `5S: Advertências` `/producao/5s/advertencias`, `5S: Mesas` `/producao/5s/mesas`.
- Ordem: Resumo, Projetos, Tarefas, Máquinas, 5S: Setores, 5S: Auditoria, 5S: Pendências, 5S: Ranking, 5S: Advertências, 5S: Mesas.
- **Item Configurações permanece** inalterado.
- **DoD:** sidebar renderiza filhos; `module: 'producao'` em todos; nada de Roles aqui (RBAC fica em permissions + layout/guarda).

### prd-026 · `frontend/src/lib/utils/permissions.ts` — modify ⚠️
Adicionar (mantendo os existentes intactos):
- `canSee5S(user): boolean` → Admin (role 0) ou `roles.producao === 'view'|'edit'` **e** role 1..3 (**Recrutando/role 4 → false**); respeitar override `user.roles.producao === null → false`.
- `isResponsavelAtribuido(user, recurso: { responsavelId?: string }): boolean` → Admin true; senão `responsibilities?.producao` contém `responsavelId`.
- `canAuditar5S(user): boolean` → Admin ou auditor nomeado (reuso `isResponsavelAtribuido` com id do auditor; fallback role 1..2 true).
- `canSeeAdvertencias(user): boolean` → **Admin-only** (role 0 ou roles.producao==='edit').
- `canEditProducao(user, recurso?): boolean` → Admin || EDIT_RULES.producao (role 0 já cobre) + se `recurso` informado, `isResponsavelAtribuido`.
- **NÃO** alterar `canSeeMachines`/`canSeeLoans`/`canView`/`canEdit`/`VIEW_RULES`/`EDIT_RULES` (mantidos). Se precisar ajustar `VIEW_RULES.producao` p/ 5S, faça nos helpers novos, não no mapa.
- **DoD:** RBAC hide (nunca disable); cobertura nos `tests/unit/permissions.test.ts` é task posterior (prd-041) — aqui só expor funções puras.

### prd-027 · `frontend/src/routes/(app)/producao/+layout.ts` + `+layout.svelte` — create
- `+layout.ts` (server?): verificar sessão (std auth guard) e RBAC `canView(user,'producao')` → senão `redirect(303,'/dashboard')`; **guard 5S**: nós 5S (`/producao/5s/**`) exigem `canSee5S` → Recrutando recebe `redirect(303,'/producao')`; `/producao/5s/advertencias**` exige `canSeeAdvertencias` → `redirect(303,'/producao/5s/ranking')` (hide via guarda, não 403).
- `+layout.svelte`: scaffold com submenu Produção ativo (reuso do padrão de layout da sidebar/breadcrumbs existente — ver como Estoque/Dashboard montam; se houver `Scaffold`/`PageHeader`/sub-nav reutilizável, usar; senão simples `<slot/>` + breadcrumbs).
- **DoD:** navegação com `href` para filhos; `data-testid` opcional `prd-layout`; pt-BR; **só scaffold — as 12 telas vêm nos blocos 5/6**.

## Regras

1. **Ordem prd-025 → 026 → 027; NÃO commitar** (Orchestrator commita ao final).
2. **NUNCA** tocar: `docs/`, specs, `client.ts`(auth), `types/auth.ts`, `estoque/**`, components/rotas de outras features, `configuracoes/**`, `menu.ts`/`permissions.ts` de OUTRA feature (só os itens de producao aqui).
3. Sem `any`; sem `console.log`; pt-BR; tokens/tipos existentes; preservar TypeScript estrito (svelte-check).
4. `menu.ts` é module TS puro (sem imports de Svelte). `+layout.server.ts` só se a sessão vier de server; caso a sessão seja client-side (ver arquitetura do auth/dashboard), usar `+layout.ts` com `browser` guard (copiar padrão do dashboard).
5. Não criar testes neste bloco (prd-040..042 no bloco final).
6. Ao final: `npm run test` (95 verdes) e `npm run check` SEM erros novos. Atenção p/ `crumbsFor`: `menuItems[].children` já é suportado na função? **Verificar** `crumbsFor`/sidebar não quebrarem com os filhos novos (se `crumbsFor` não lidar com children, ajustar mínimamente dentro deste mesmo escopo p/ manter compat); sidebar: conferir que filhos aparecem.

## Relatório esperado
- Por task: ✅/❌ + evidência (arquivo:linha, funções/filhos adicionados)
- Comportamento da sidebar/breadcrumbs com os 10 filhos (como verificou — se renderizou no check/build)
- `npm run test` + `npm run check` (resumo)
- Pontos de atenção / desvios
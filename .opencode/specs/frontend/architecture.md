# Architecture — FabLab Frontend (SvelteKit SPA)

> **Fonte:** código real em `frontend/` + `docs/mokups/*` (visuais aprovados) + `system/architecture.md`.
> Onde divergir de spec antiga, prevalece o **código real** (padrão do pipeline).

## Stack & Modo
- **Framework:** SvelteKit + TypeScript (strict, `noImplicitAny`, `noUncheckedIndexedAccess`)
- **Modo:** **SPA apenas** — `ssr = false`, `prerender = false` (global), `adapter-node` (servidor Node, sem `adapter-static`)
- **Estilização:** Tailwind CSS (tokens em `tailwind.config.ts`; sem componente de UI/library)
- **Estado:** Svelte stores + **URL como fonte de verdade** para listas/filtros/tabs/paginação
- **HTTP:** `fetch` nativo em `src/lib/api/client.ts` (base `VITE_API_BASE_URL` → monólito `:8080`)
- **Testes:** Vitest (unit) + SvelteKit `npm run check`

## Fluxo de Comunicação
```
[Browser / SPA] --HTTPS--> [Nginx] --HTTPS--> [Monólito modular :8080 /api]
                                        (JWT Authorization: Bearer <token>)
```
- Front **nunca** fala direto com o backend fora de `VITE_API_BASE_URL` (monólito `:8080`).
- **Sem WebSocket no MVP** — polling de notificações a cada 60s (pause em `document.hidden`).
- 401 → limpa `authStore` + redirect `/auth/login?redirect=<current>`.

## Estrutura (espelha `frontend/src/` real)
```
frontend/src/
├── routes/
│   ├── +layout.ts / +layout.svelte        # App Shell global
│   ├── auth/+layout.svelte                # shell público (login/recuperação)
│   └── (app)/+layout.svelte               # shell autenticado (sidebar+topbar)
│       ├── dashboard/                     # (app)/dashboard
│       ├── estoque/                       # itens, entradas, saidas, emprestimos, ...
│       ├── pessoas/                       # RH: pessoal, processo seletivo, apontamentos
│       ├── vendas/                        # clientes, orçamentos, encomendas, kanban
│       ├── financeiro/  ├── producao/  ├── notificacoes/  └── configuracoes/
├── lib/
│   ├── api/            # client.ts + auth.ts + <modulo>/*.ts (chamadas ao monólito :8080)
│   ├── components/
│   │   ├── ui/         # design-system: Button, Input, Table, Tabs, Modal, Toast, Skeleton...
│   │   └── layout/     # Sidebar, Topbar, Breadcrumb, NotificationsButton, UserMenu
│   ├── stores/         # auth.ts, notifications.ts, toast.ts
│   ├── types/          # auth.ts, dashboard.ts, stock.ts, ...
│   └── utils/          # permissions.ts, format.ts, errors.ts
```

## RBAC no Frontend
- **Backend é autoridade:** front apenas **esconde** (RBAC-inert); backend re-valida cada request.
- Regra de ouro: **hide, não disable** — se `canEdit === false`, não renderiza botão/coluna/checkbox.
- Rotas protegidas via guard em `(app)/+layout.ts`; role insuficiente → redirect `/dashboard`.

## Design System (resumo)
- Tokens: `surface`, `elevated`, `border`, `ink`, `muted`, `brand`, `brandhi`, `success`, `warn`, `danger`
- Tipografia: **UI pt-BR** (`text-muted`/`text-ink`, `text-xs`/14); Mono p/ códigos/números
- Sem hex literals em componentes — só classes token (exceto `focus:` e states no config)
- Acessibilidade: focus rings (nunca remover), toasts `role=status|alert`, modais focus-trap + Esc
- `data-testid` em list-rows, form-actions, filters, empty/error states

## Detalhes visuais
- Cada feature referencia seu mockup em `docs/mokups/<modulo>/` (requisitos.md + full.html).

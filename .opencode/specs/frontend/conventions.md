# Frontend System Specs — Conventions

> **Fonte:** código real (`frontend/src/**`) + `system/conventions.md`. Echo para o escopo frontend;
> não substitui — complementa.

## Stack
- **SvelteKit** (`ssr=false`, `prerender=false`, SPA, `adapter-node`) + **TypeScript strict**
- **Tailwind CSS** (sem biblioteca de componentes UI/design-system)
- **Stores Svelte** — sem Redux/Zustand; sem biblioteca de chart (SVG inline); sem biblioteca de estado global além de stores

## Git Flow (raro no frontend)
- `dev/frontend` é **merge-only** (recebe merges de `dev/backend`? **não** — o frontend vive em `dev/frontend` e o backend em `dev/backend`; `development` é merge-only de ambos)
- Commits frontend: `feat(ui):`, `fix(estoque):`, `refactor(rh):`, `test(dashboard):`, `docs(frontend):`
- **Sem commits mistos**: um commit de frontend NUNCA toca arquivo backend
- Branches: `feature/frontend-<nome>`, `bugfix/frontend-<nome>` (criadas de `dev/frontend`)

## Nomenclatura
| Contexto | Conv | Exemplo |
|---|---|---|
| Componentes Svelte | PascalCase | `ItemRow.svelte` |
| Arquivos de rota | kebab-case | `+page.svelte`, `+layout.svelte` |
| Módulos TS | kebab-case | `items.ts`, `auth-store.ts` |
| Componentes UI | PascalCase | `<ItemRow />` |
| Funções/vars | camelCase | `fetchItems`, `canEdit` |
| Constantes | SCREAMING_SNAKE | `API_BASE_URL` |
| Tipos/interfaces | PascalCase | `StockItem`, `LoginResponse` |
| Test IDs | kebab-case | `item-row` |

## Estrutura (`frontend/src/`)
```
src/
├── routes/
│   ├── +layout.svelte / +layout.ts      # App Shell (sidebar + topbar)
│   ├── (app)/                           # autenticado
│   │   ├── +layout.svelte               # shell app + guard
│   │   ├── dashboard/
│   │   ├── estoque/
│   │   ├── pessoas/
│   │   ├── vendas/
│   │   ├── financeiro/
│   │   ├── producao/
│   │   ├── notificacoes/
│   │   └── configuracoes/
│   └── auth/                            # público (login, recuperação)
├── lib/
│   ├── api/                             # client.ts (fetch wrapper) + features
│   ├── components/                      # ui/, layout/, domain/
│   ├── stores/                          # auth, notifications, toast
│   ├── types/                           # auth, stock, ...
│   └── utils/                           # permissions, format, errors
```

## Routing Rules
- `(app)` = shell autenticado (sidebar+topbar); `auth` = shell público
- Params dinâmicos `[id]`, `[slug]`; estado deep-linkável via `?query` (filters, tabs, page)
- **Nunca** depender de store p/ dados representáveis por URL

## API Layer (`src/lib/api/client.ts`)
- Wrapper `fetch` injeta `Authorization` (Bearer JWT); timeout 15s (AbortController)
- 401 → limpa auth store + redirect `/auth/login?redirect=<current>`
- 5xx → "Erro ao processar. Tente novamente."; rede → "Falha de conexão. Verifique sua rede."
- Erros normalizados `{ message, code?, status? }`
- Base URL: `VITE_API_BASE_URL` (monólito `:8080`); fallback `http://localhost:8080/api` dev

## Auth Flow
1. Login → POST `/auth/login` → `{ token, expiresIn, user }`
2. Persist `{ token, user }` em `authStore` + `localStorage` (`fablab.auth`)
3. Boot `(app)/+layout.ts`: sem token → redirect; token sem user → `GET /auth/me`
4. 401 em qualquer request → limpa store + redirect
5. Logout → limpa store + localStorage + redirect `/auth/login`

## States
Cada página trata: **Loading** (skeleton), **Success**, **Empty**, **Error** (+ estados de domínio)

## RBAC no Frontend
- Backend é autoridade; front **esconde (não desabilita)** recursos não autorizados
- `canEdit === false` → **não renderizar** botão/coluna/checkbox (nunca disabled)
- Route guard em `+layout.ts`; role insuficiente → redirect `/dashboard`
- Menu lateral filtra por role (remove item, não desabilita)

## Formulários
- Client-side validation no **submit** (não por keystroke); erros `text-xs text-danger mt-1`
- Disable submit durante envio; sucesso → toast + navigate/refetch; falha → mantém valores + banner
- Dirty-guard ao sair da página

## Testes
- Unit (Vitest): utils, stores, lógica pura
- Component (Vitest + @testing-library/svelte): UI interativa
- E2E (Playwright): happy path por módulo
- Arquivo de teste ao lado do source: `items.test.ts` ao lado de `items.ts`

## Acessibilidade (obrigatório)
- Tudo acessível por teclado; focus rings visíveis; landmarks (`main`, `nav aria-label`)
- Modais: `role="dialog" aria-modal="true"`, focus trap, Esc fecha
- Tabs: `role="tablist"`, navegação por setas; toasts `role="status"`/`role="alert"`
- Skip link: "Pular para o conteúdo" no App Shell

## Design-check (alinhado a docs/mokups)
- Cada tela referencia seu mockup: **`docs/mokups/<modulo>/full.html`**
- Se mockup não existir para a feature → **bloqueado** (aguardando `designer.md`), não inventa

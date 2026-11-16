# Plan: Auth — Frontend (gap analysis vs código real)

> **Fonte de verdade:** código real da branch `feature/frontend-login`
> (commits `b600fc9` login, `159de98` app shell+dashboard) + `docs/mokups/login/requisitos.md`
> (RF-01..09, RNF-01..05, contrato §4, estados §5, critérios §11, notas técnicas §13)
> + spec da feature (`.opencode/specs/frontend/features/auth/requirements.md`).
> Método: **inventariar → comparar → gaps arquivo a arquivo** — NÃO replanejamento do zero.

---

## 1. Estado real (inventário arquivo a arquivo)

| Arquivo | Papel | Estado vs mockup/spec | Observação |
| :--- | :--- | :--- | :--- |
| `src/routes/+layout.ts` | `ssr=false`, `prerender=false` | OK | §13.1/§13.2 do mockup |
| `src/routes/+layout.svelte` | importa `app.css` | OK | |
| `src/routes/auth/+layout.svelte` | shell público (sem sidebar/topbar) | OK | slot simples |
| `src/routes/auth/login/+page.svelte` | tela: layout, glow, logo, estado success | GAP | não lê `?redirect=` (G-2) |
| `src/routes/auth/login/login-form.svelte` | form + validação + erro + loading | OK | RF-01/02/03/07/09, §5, §6, §10 |
| `src/routes/(app)/+layout.ts` | guard autenticado | OK | `?redirect=`, fetchMe restore, logout em 401 do fetchMe |
| `src/routes/(app)/+layout.svelte` | shell autenticado (sidebar+topbar) | OK | fora do escopo visual do login |
| `src/lib/api/client.ts` | `apiFetch`, timeout 15s, AbortController | GAP | sem tratamento global de 401/expiração (G-1) |
| `src/lib/api/auth.ts` | `login` + `fetchMe` | OK | contrato §4; injeta Bearer no `fetchMe` |
| `src/lib/stores/auth.ts` | persist `fablab.auth`, restore, logout, setUser | OK | §8; logout não redireciona (callers redirecionam — aceitável) |
| `src/lib/types/auth.ts` | `Role`, `User`, `LoginRequest`, `LoginResponse`, `Module` | OK | contrato §4 exato (`token`/`expiresIn`/`user`) |
| `src/lib/utils/errors.ts` | `toUserMessage` | OK | mapeia `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`, 401, 423, 5xx, rede |
| `tests/unit/{format,permissions,sanity}.test.ts` | testes existentes | GAP | nenhum teste de auth (G-4) |
| `src/routes/auth/forgot-password/` | rota placeholder | FALTA | RF-08/critério §11 (G-3) |

## 2. Aderência ao mockup (`docs/mokups/login/requisitos.md`)

### Requisitos Funcionais
| ID | Status | Nota |
| :--- | :--- | :--- |
| RF-01 entrada usuário/senha | OK | inputs com label, placeholder pt-BR |
| RF-02 validação client-side | OK | no submit; trim; mín. 6; mensagens exatas do §6 |
| RF-03 toggle senha | OK | olho com aria-label dinâmico, não perde valor |
| RF-04 `POST /api/auth/login` | OK | via `apiFetch('/auth/login')` |
| RF-05 persistir JWT+user | OK | store + localStorage `fablab.auth` |
| RF-06 redirecionar pós-sucesso | GAP | vai a `/dashboard`, **ignora** `?redirect=` do guard (G-2) |
| RF-07 mensagens de erro | OK | `toUserMessage` (401/423/5xx/rede) inline `role="alert"` |
| RF-08 link esqueci senha | GAP | link existe para `/auth/forgot-password`, mas **rota 404** (G-3) |
| RF-09 bloquear reenvio | OK | `disabled=loading` + guard no submit + spinner Autenticando... |

### Requisitos Não-Funcionais
| ID | Status | Nota |
| :--- | :--- | :--- |
| RNF-01 tema dark tokens | OK | tokens Tailwind (sem hex literal em componente) |
| RNF-02 responsivo | OK | `max-w-sm` desktop / full mobile, 360–1920 |
| RNF-03 feedback em todos os estados | OK | default/loading/erro/sucesso + glow verde + ~600 ms |
| RNF-04 sem dependências além do login | OK | |
| RNF-05 teclado/Enter | OK | tab order natural, Enter submete, foco inicial no usuário |

### Contrato §4 e mapeamento de erros
| Item | Status | Nota |
| :--- | :--- | :--- |
| `POST /auth/login` body `{username,password}` | OK | `LoginRequest` |
| 200 `{token,expiresIn,user}` | OK | `LoginResponse`; `user{id,username,name,role,email}` |
| 401 `INVALID_CREDENTIALS` → Usuário ou senha inválidos | OK | `errors.ts` |
| 423 `ACCOUNT_LOCKED` → Conta bloqueada... | OK | `errors.ts` |
| 5xx → Erro ao processar login... | OK | `errors.ts` |
| rede/timeout → Falha de conexão... | OK | `errors.ts` + `NetworkError` do client |

### Critérios de aceite §11
Cobertos **exceto**: o link Esqueci minha senha navegar para uma rota que existe
(`/auth/forgot-password` → 404 hoje) e o redirecionamento pós-login respeitando a
rota original solicitada via `?redirect=`.

### Notas técnicas §13
| Item | Status |
| :--- | :--- |
| `ssr=false` herdado; sem prerender | OK |
| **Interceptor 401/expiração limpa store e redireciona** | **FALTA (G-1)** — não há em `client.ts`; hoje duplicado ad-hoc em `api/dashboard.ts` e `api/stock/request.ts` |
| `AbortController` com timeout 15s | OK |

## 3. Gaps encontrados (o que falta)

| ID | Arquivo | Gap | Impacto |
| :--- | :--- | :--- | :--- |
| G-1 | `src/lib/api/client.ts` (modify) | Ausência de tratamento **global** de 401: `architecture.md`, `conventions.md` e mockup §13 exigem limpar store + redirect `/auth/login?redirect=<atual>`. Hoje `apiFetch` apenas lança `ApiError`; cada módulo mantém handler próprio (`dashboard.ts`, `stock/request.ts`). Rota expirada em qualquer feature deixa o usuário preso em erro em vez de voltar ao login. | Alto — contrato de sistema; UX de expiração |
| G-2 | `src/routes/auth/login/+page.svelte` (modify) | Fluxo `?redirect=` montado pelo guard `(app)/+layout.ts` não é consumido: pós-login cai sempre em `/dashboard`, perdendo a rota original (espírito do RF-06 + `architecture.md`). | Médio — fluxo quebrado |
| G-3 | `src/routes/auth/forgot-password/+page.svelte` (create) | Rota inexistente → link placeholder (RF-08) navega para 404. Criar página placeholder mínima (MVP, sem fluxo real). | Baixo — critério §11 |
| G-4 | `frontend/tests/unit/` (create) | Zero testes de auth: `errors.ts`, `stores/auth.ts`, `client.ts`, `login-form.svelte` sem cobertura (conventions §Testes). | Médio — DoD/qualidade |

> **Não-gaps confirmados:** tipos e contrato §4; `api/auth.ts`; persistência/restore (`fablab.auth`);
> estados §5 da tela; acessibilidade §10; RBAC (tela pública — sem matriz a aplicar);
> guard `(app)/+layout.ts`; timeout 15s + AbortController.

## 4. Estratégia e ordem

Ordem proposta (padrão dispatch: types → API → components → routes → states → RBAC → tests → DoD):

1. **API layer** — `client.ts`: centralizar 401 (logout + redirect sanitizado) com **opt-out para
   `/auth/login`** (401 de credenciais NÃO pode redirecionar — é `INVALID_CREDENTIALS`) e guard
   anti-loop/anti-duplicidade — idempotente para conviver com os handlers ad-hoc de
   `dashboard.ts`/`stock/request.ts`, que **permanecem intactos** (remoção fica para as features donas).
2. **Routes** — `+page.svelte`: consumir `?redirect=` sanitizado (somente path interno; fallback `/dashboard`).
3. **Routes** — criar placeholder `forgot-password/+page.svelte`.
4. **Tests** — `errors`, `auth-store`, `client`, `login-form`.
5. **RBAC** — nenhuma task: feature pública; nada a esconder/desabilitar.
6. **DoD** — `npm run test` + `npm run check` + passagem Validator/Security.

Nenhuma mudança em `docs/`, mockups, specs de sistema nem código de outras features.
O **único arquivo compartilhado** tocado é `client.ts` (ver §7 Conflitos).

## 5. Checklist para o Validator

- [ ] Comparar `login.html` e `loginEstados.html` 1-a-1 (estados default/loading/erro/sucesso, textos, tokens, glow)
- [ ] Responsividade 360 px → 1920 px (RNF-02)
- [ ] Erro mantém campos preenchidos; mensagens exatas `INVALID_CREDENTIALS` / `ACCOUNT_LOCKED` / 5xx / rede (crit. §11)
- [ ] Loading: botão+inputs+olho+link desabilitados; spinner Autenticando... (RF-09)
- [ ] Sucesso: glow verde + ícone + Autenticado + ~600 ms → destino (`/dashboard` ou `?redirect=`)
- [ ] Teclado: foco inicial no usuário, Enter submete, tab order, aria-label do olho, `role="alert"` no erro
- [ ] Guard: sem token → redirect `/auth/login?redirect=`; token sem user → `GET /auth/me`; 401 no me → logout+redirect com `?redirect=`
- [ ] Interceptor 401: login (`/auth/login`) NÃO redireciona (erro de credenciais normal); rota protegida expirada → login+redirect; sem loop na própria `/auth/login`
- [ ] Store: restore válido/corrompido/ausente; logout limpa `fablab.auth`
- [ ] Testes novos verdes (`npm run test`) e `npm run check` sem erros

## 6. Checklist para o Security

- [ ] Token JWT em **localStorage** (`fablab.auth`) — risco XSS conhecido, trade-off aprovado no mockup §8/§9 (SPA sem SSR, sem cookie HttpOnly no escopo). Reconfirmar que nenhum dado sensível além do token/payload de usuário é persistido
- [ ] Credenciais **nunca** em log/console/URL/store em claro — validar que `login-form`/`toUserMessage`/interceptor não logam `password`
- [ ] **Redirect open**: ao consumir `?redirect=`, aceitar somente path interno (começa com `/`, não `//`, sem `:`/`\`/controle); caso contrário cai para `/dashboard`. Aplicar o mesmo sanitizer no redirect do interceptor 401
- [ ] Interceptor 401 must not entrar em loop (guard se já está em `/auth/login`; flag anti-duplicidade)
- [ ] `Authorization: Bearer <token>` apenas em header; nunca em query string
- [ ] `GET /auth/me` sem token: nunca enviar `Bearer null` (guard do `(app)` já filtra; confirmar)
- [ ] CSRF: app usa Bearer + JSON sem cookie de sessão → CSRF não se aplica; manter
- [ ] Logout em 401 deve remover `fablab.auth` e demais chaves de sessão (ex.: nenhuma outra deve persistir token)

## 7. Conflitos de escopo com outras features (arquivos compartilhados)

| Arquivo | Dono lógico | Conflito |
| :--- | :--- | :--- |
| `src/lib/api/client.ts` | **auth** (interceptor 401) / usado por **todas** | A feature auth modifica. Estoque (`stock/request.ts`), dashboard, rh consome — não podem tocar em paralelo. Manter compatibilidade (rethrow para callers atuais). |
| `src/lib/api/stock/request.ts` | estoque | Contém wrapper `stockFetch` + handler 401 duplicado. **Não tocar na feature auth**; remoção/consolidação futura na feature dona. Fornece 401 para rh.ts também. |
| `src/lib/api/dashboard.ts` | dashboard | Handler 401 inline duplicado. **Não tocar**; após G-1 fica redundante-inerte. |
| `src/lib/stores/auth.ts` | auth | Todas as features importam `logout`/`setUser`. Só o auth mexe. |
| `src/lib/utils/permissions.ts` + `src/lib/config/menu.ts` | RBAC/menu (configurações/produção) | Auth **não** toca: `canView`/`canEdit` já vivem em `permissions.ts` (com testes) em vez do store (evolução vs mockup §8 — OK). |
| `src/routes/(app)/+layout.ts` | auth (guard) | Criado no commit `159de98`; nenhuma outra feature deve alterar o guard. |
| `svelte.config.js` (adapter-node) | infra | Ver §8 Observações — fora do escopo da feature auth. |

## 8. Observações e riscos (não geram task nesta feature)

1. **Adapter**: `svelte.config.js` usa `@sveltejs/adapter-node`; `architecture.md` e
   `conventions.md` foram alinhados a esse alvo (SPA com `ssr=false` servida por Node). Resolvido
   no remap estrutural.
2. **Duplicação de fetch wrapper**: resolvida no remap — `stockFetch` foi removido e `apiFetch`
   (`src/lib/api/client.ts`) centraliza auth, 401 e `buildQuery`.
3. **Token drift do design-system**: `design-system.md` lista `elevated #111a2c`, mas o Tailwind real
   e o mockup de login usam `#1a2235`. O código segue o real/mockup — só o doc está divergente.
   Atualização de doc fica com o Orchestrator (não alterar specs de sistema nesta feature).
4. **Logout offline no boot**: `(app)/+layout.ts` faz logout em qualquer erro do `fetchMe`
   (incluindo falha de rede), invalidando sessão local válida. Comportamento aceitável para MVP;
   Validator deve confirmar (alternativa futura: servir `user` do cache e revalidar em background).
5. **`expiresIn` não utilizada** no client: correto — o JWT expira no servidor e o interceptor 401
   cobre a expiração; nenhuma contagem local necessária (mockup §12: sem refresh automático).

## 9. Status e decisão

- **Status da feature:** PLANEJADA (gaps G-1..G-4 mapeados; 7 tasks em `tasks.md`).
- **Próximo passo:** Builder executa `auth-001..auth-007` após revisão humana do mockup,
  depois Validator (checklist §5) e Security (checklist §6).
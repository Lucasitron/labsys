# Tasks: Auth — Frontend (gaps)

> Branch: `feature/frontend-login` · Spec da feature: `.opencode/specs/frontend/features/auth/`
> Mockup: `docs/mokups/login/` (requisitos.md, login.html, loginEstados.html)
> Regra: **1 arquivo ≡ 1 task** · `acao` = create | modify · texto UI pt-BR · tokens design-system · RBAC hide-não-disable.
> Estas tasks cobrem **apenas os gaps** (G-1..G-4 do `plan.md`) — o restante já está implementado e aderente.

---

## Resumo

| id | Titulo | Arquivo | Acao | Depende |
| :--- | :--- | :--- | :--- | :--- |
| auth-001 | Interceptor global de 401 no client | `frontend/src/lib/api/client.ts` | modify | — |
| auth-002 | Consumir redirect apos login | `frontend/src/routes/auth/login/+page.svelte` | modify | — |
| auth-003 | Rota placeholder esqueci minha senha | `frontend/src/routes/auth/forgot-password/+page.svelte` | create | — |
| auth-004 | Testes de mapeamento de erros | `frontend/tests/unit/errors.test.ts` | create | — |
| auth-005 | Testes da store de autenticacao | `frontend/tests/unit/auth-store.test.ts` | create | — |
| auth-006 | Testes do apiFetch e interceptor 401 | `frontend/tests/unit/client.test.ts` | create | auth-001 |
| auth-007 | Testes do formulario de login | `frontend/tests/unit/login-form.test.ts` | create | — |

---

## auth-001 — Interceptor global de 401 no client

- **Titulo:** Tratar 401 globalmente em `apiFetch` limpa store e redireciona
- **Arquivo:** `frontend/src/lib/api/client.ts`
- **Acao:** `modify`
- **Detalhes:**
  - Adicionar ao `apiFetch` o fluxo do mockup §13 / `architecture.md` / `conventions.md`:
    ao receber `response.status === 401`, disparar `logout()` da store e redirecionar para
    `/auth/login?redirect=<path atual sanitizado>`.
  - **Opt-out obrigatorio:** 401 em `POST /auth/login` (ou qualquer request de auth público) NÃO
    deve redirecionar nem limpar store — é `INVALID_CREDENTIALS` e a tela de login exibe o erro
    (comportamento atual preservado). Usar flag/parametrizacao (ex.: `skipAuthOn401`) ou deteccao
    pelo path.
  - Guard anti-loop/anti-duplicidade: se o redirecionamento já está em andamento (flag de módulo)
    ou se o path atual já é `/auth/login`, não navegar de novo. No login deve ir para `/dashboard`.
  - Continuar lançando `ApiError` após o tratamento (rethrow) para não quebrar o comportamento dos
    callers atuais (`dashboard.ts`, `stock/request.ts`, páginas que tratam erro).
  - Sanitização do redirect: aceitar somente path interno (inicia com `/`, não `//`, sem `:`/`\`),
    fallback `/dashboard`. Modalidades: manter `AbortController` 15 s e parse de payload de erro.
  - Não injetar Authorization automaticamente nesta task (heads: outras features injetam manualmente;
    centralização de header é consolidação futura, fora deste escopo).
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] 401 em rota protegida chama `logout()` e navega para `/auth/login?redirect=...`
  - [ ] 401 em `/auth/login` NÃO redireciona e NÃO limpa store
  - [ ] sem loop: já em `/auth/login` não re-navega; flag anti-duplicidade evita navegação dupla
    com os handlers ad-hoc existentes
  - [ ] redirect sempre sanitizado (paths `//evil`, `https://...`, `javascript:...` caem em `/dashboard`)
  - [ ] timeout 15 s → `NetworkError`; payload de erro parseado (code/message)
  - [ ] testes `client.test.ts` (auth-006) verdes

---

## auth-002 — Consumir redirect apos login

- **Titulo:** Respeitar `?redirect=` após autenticação bem-sucedida
- **Arquivo:** `frontend/src/routes/auth/login/+page.svelte`
- **Acao:** `modify`
- **Detalhes:**
  - Ler `?redirect=` de `$page.url.searchParams` (parametros já montados pelo guard
    `(app)/+layout.ts`).
  - Sanitizar: aceitar somente path interno (começa com `/`, não `//`, sem `:`/`\`/controle);
    caso inválido ou ausente → `/dashboard`.
  - Passar o destino calculado para `handleSuccess` e usar no `goto(...)` após o delay de ~600 ms
    (mantém o estado Sucesso do mockup §5 inalterado).
  - UI/textos/estados: nenhuma alteração visual; só a lógica de destino.
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] login sem `?redirect=` ainda vai para `/dashboard` (RF-06)
  - [ ] login com `?redirect=/estoque` (gerado pelo guard) volta para `/estoque`
  - [ ] `?redirect=//evil.com` ou `https://...` cai em `/dashboard` (sem open redirect)
  - [ ] delay de sucesso (~600 ms) mantido; estado Sucesso renderiza antes da navegação
  - [ ] `npm run check` sem erros de tipo

---

## auth-003 — Rota placeholder esqueci minha senha

- **Titulo:** Criar página placeholder em `/auth/forgot-password`
- **Arquivo:** `frontend/src/routes/auth/forgot-password/+page.svelte`
- **Acao:** `create`
- **Detalhes:**
  - Página mínima com o shell público herdado de `routes/auth/+layout.svelte`: título, texto de
    aviso que a recuperação estará disponível em versão futura (MVP) e link/voltar para
    `/auth/login`.
  - Texto pt-BR; tokens do design-system (`base`, `surface`, `ink`, `muted`, `brand`); visual
    consistente com o `login.html` (card `max-w-sm`, glow); responsivo mobile-first.
  - Nenhum endpoint novo (RF-08 é placeholder por decisão de escopo; somente rota cliente).
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] `GET /auth/forgot-password` renderiza 200 (sem 404) no navegador
  - [ ] link Esqueci minha senha da tela de login navega para esta rota (crit. §11)
  - [ ] voltar para `/auth/login` funcional; página segue layout público
  - [ ] sem chamadas de rede; sem inputs de credenciais persistindo qualquer dado

---

## auth-004 — Testes de mapeamento de erros

- **Titulo:** Testar `toUserMessage` (todos os códigos/estados)
- **Arquivo:** `frontend/tests/unit/errors.test.ts`
- **Acao:** `create`
- **Detalhes:**
  - Cobrir `toUserMessage` de `src/lib/utils/errors.ts`:
    - `ApiError` 401 code `INVALID_CREDENTIALS` → Usuário ou senha inválidos
    - `ApiError` 423 code `ACCOUNT_LOCKED` → Conta bloqueada. Contate o administrador.
    - `ApiError` 401 sem code → mensagem de credenciais
    - `ApiError` 423 sem code → mensagem de conta bloqueada
    - `ApiError` 5xx (qualquer code) → Erro ao processar login. Tente novamente.
    - `NetworkError` → Falha de conexão. Verifique sua rede.
    - erro desconhecido (`new Error('x')`) → Ocorreu um erro inesperado.
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] todas as combinações do §4 do mockup cobertas
  - [ ] green: `npm run test`

---

## auth-005 — Testes da store de autenticação

- **Titulo:** Testar `stores/auth.ts` (login/logout/setUser/restore)
- **Arquivo:** `frontend/tests/unit/auth-store.test.ts`
- **Acao:** `create`
- **Detalhes:**
  - Com `vitest` + jsdom, mockar `$lib/api/auth.login` e `localStorage`:
    - `login()` chama a api, seta `user/token/isAuthenticated` e persiste em `fablab.auth`
    - `login()` com erro propaga o erro e não seta estado autenticado
    - `logout()` limpa store e remove a chave do localStorage
    - `setUser()` atualiza usuário e re-persiste
    - restore: localStorage válido restaura sessão; JSON corrompido → estado vazio sem crash;
      ausência de chave → estado vazio
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] persistência e restore cobertos (chave `fablab.auth` exata)
  - [ ] estado nunca fica inconsistente (token sem user etc.)
  - [ ] green: `npm run test`

---

## auth-006 — Testes do apiFetch e interceptor 401

- **Titulo:** Testar `apiFetch` (timeout 15s, 401 global, parse de erros)
- **Arquivo:** `frontend/tests/unit/client.test.ts`
- **Acao:** `create`
- **Dependencia:** `auth-001` (interceptor precisa existir)
- **Detalhes:**
  - Mockar `fetch`:
    - monta URL `${API_BASE}${path}` e header `Content-Type: application/json`
    - timeout: aborta depois de ~15 s e lança `NetworkError` (mensagem de tempo limite)
    - falha de rede → `NetworkError`
    - resposta 401 em rota protegida → chama `logout()` + redireciona (interceptor), e rethrow de `ApiError`
    - resposta 401 em `/auth/login` → sem logout/redirect (opt-out)
    - 4xx/5xx com payload `{ code, message }` → `ApiError` com status e code
    - success → JSON tipado retornado
  - Configurar `VITE_API_BASE_URL`/env do client conforme o padrão já usado nos testes existentes.
- **Verificar (DoD):**
  - [ ] geração de URL e headers corretos
  - [ ] timeout e abort cobertos (fake timers ou `vi.spyOn` no timer)
  - [ ] interceptor 401 (com e sem opt-out) coberto
  - [ ] green: `npm run test`

---

## auth-007 — Testes do formulário de login

- **Titulo:** Testar `login-form.svelte` (validação, estados, acessibilidade)
- **Arquivo:** `frontend/tests/unit/login-form.test.ts`
- **Acao:** `create`
- **Detalhes:**
  - Com `@testing-library/svelte` + jsdom, mockar `$lib/stores/auth.login`:
    - validação: submit vazio exibe Informe seu usuário / Informe sua senha e não chama a api
    - senha < 6 exibe erro de campo
    - submit válido chama `login` uma vez e dispara `onsuccess`
    - erro de login exibe `role="alert"` com a mensagem e mantém campos preenchidos
    - durante loading: botão/inputs/olho/link desabilitados e texto Autenticando...
    - toggle do olho alterna type e aria-label (Mostrar/Ocultar senha)
    - inputs com `autocomplete=username` / `autocomplete=current-password`
    - Enter dentro do form dispara submit
- **Dependencias:** —
- **Verificar (DoD):**
  - [ ] comportamento interativo dos 4 estados (default/loading/erro/sucesso) coberto
  - [ ] marcadores de acessibilidade validados (role=alert, aria-label, autocomplete)
  - [ ] green: `npm run test`

---

## DoD da feature (após auth-001..auth-007)

- [ ] `npm run test` verde (suíte unit existente + novos testes de auth)
- [ ] `npm run check` sem erros de tipo
- [ ] Validador executa checklist do `plan.md` §5 (mockup 1-a-1, estados, a11y, guard, interceptor)
- [ ] Security executa checklist do `plan.md` §6 (localStorage, credenciais, redirect open, loop 401)
- [ ] Nenhuma alteração em `docs/`, `docs/mokups/`, specs de sistema ou arquivos de outras features
- [ ] Contrato respeitado: apenas `POST /auth/login` e `GET /auth/me` consumidos
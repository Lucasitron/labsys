# DESPACHO — Planner (Frontend) · AUTH · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **gap analysis** da feature `auth` contra o código real.
> Alvo: `.opencode/specs/frontend/features/auth/` → `plan.md` + `tasks.md` (tasks = **o que falta**, com DoD).

---

## Contexto (ler antes)

- `.opencode/state/frontend-orchestration.json` — feature `auth`: SPEC_CADASTRADA, designReady, mockup `docs/mokups/login/`; tasks vazias
- Spec da feature: `.opencode/specs/frontend/features/auth/requirements.md` (rota `/auth/login`, `POST /api/auth/login`, `GET /api/auth/me`, states)
- Mockup (fonte da verdade visual): `docs/mokups/login/` (`requisitos.md` com RF-01..09, RNF, contrato, estados, RBAC, acessibilidade, critérios)
- **Código real já existe na branch atual `feature/frontend-login`** (commits `b600fc9` login, `159de98` app shell+dashboard). O Planner NÃO re-planeja do zero: **reconcilia com o código real** (padrão do backend `auth-service`).
- Referência de formato plan/tasks: `.opencode/specs/features/auth-service/plan.md` + `tasks.md`; template frontend `_template` NÃO existe.
- Specs de sistema: `.opencode/specs/frontend/{architecture,conventions,design-system}.md`, `.opencode/specs/system/rbac-matrix.md`, `docs/frontendDatails.md`.

## Regras
1. **Sem mockup aprovado → bloqueado (`MOCKUP_REQUIRED`).** PO já aprovou o escopo v2 em conversa; se julgar alguma parte incompleta para planejar, emita `MOCKUP_REQUIRED` apontando a parte.
2. Método: inventariar código real → comparar com requirements + mockup → listar **gaps** (arquivo a arquivo) → tasks atômicas (1 arquivo ≡ 1 task; `id`, `titulo` PT, `arquivo`, `acao` create|modify, `detalhes`, `dependencias`, `verificar`) → ordem types → API → components → routes → states → RBAC → tests → DoD.
3. **Não escrever código**; **não alterar `docs/`, `docs/mokups/` nem código real**.
4. Endpoints usados: só os da spec/mockup (`/auth/login`, `/auth/me`). Não inventar.
5. UI text pt-BR; tokens design-system; RBAC hide-não-disable.

## Passos

### P1. Inventariar o código real (branch `feature/frontend-login`)
Principais arquivos já existentes:
- `frontend/src/routes/auth/login/+page.svelte` + `login-form.svelte` + `routes/auth/+layout.svelte`
- `frontend/src/lib/api/auth.ts` (login, fetchMe) + `api/client.ts` (apiFetch: base, timeout 15s, abort, 401?)
- `frontend/src/lib/stores/auth.ts` (restore localStorage `fablab.auth`, persist, logout, setUser)
- `frontend/src/lib/types/auth.ts`
- `frontend/src/routes/(app)/+layout.ts` (guard: redirect login → `?redirect=`, fetchMe restore, logout em 401)
- `frontend/src/routes/+layout.ts` (ssr=false) e `+layout.svelte`
- Testes existentes: `frontend/tests/unit/{format,permissions,sanity}.test.ts` — **não há testes de auth**

### P2. Comparar com requirements + mockup (`docs/mokups/login/requisitos.md`)
Checar os RF-01..09, RNF, estados (default/loading/erro/sucesso), validação client-side, acessibilidade, critérios de aceite e contrato (`INVALID_CREDENTIALS`/`ACCOUNT_LOCKED`/5xx/rede). Observar se `api/client.ts` trata 401 globalmente e expiração (spec §13 pede interceptor limpando store e redirecionando).

### P3. Gerar `plan.md` (criar)
- Visão do estado real (implementado x gap)
- Estratégia dos gaps e dependências (auth é base de `(app)` shell e de todas as outras; não planejar nada que toque arquivos de outra feature em paralelo)
- O que já pode ir para Validator/Security (checklist de validação para o Validator, checklist de segurança para o Security: token no localStorage, exposição de credenciais, redirect open, etc.)

### P4. Gerar `tasks.md` (criar)
- **Zero ou poucas tasks de implementação** se o código já estiver aderente — indicar explicitamente "sem gaps" e listar apenas validações recomendadas como tasks `verificar` (ou sugerir passagem Validator/Security direto).
- Se houver gaps (ex.: falta 401 interceptor, testes de auth, `ACCOUNT_LOCKED`, acessibilidade), gerar tasks atômicas com DoD.

### P5. Atualizar estado
`.opencode/state/frontend-orchestration.json`: feature `auth` → `status: PLANEJADA` (ou `VALIDAR` se sem gaps), preencher `tasks` (resumo/contagem), ajustar `proximos_passos` (auth plan/tasks prontos; próxima validação Validator/Security). JSON deve permanecer válido.

---

## Relatório esperado (responda com)
1. Inventário do que existe hoje (arquivos-chave) e status de aderência ao mockup (RF/RNF/critérios)
2. Gaps encontrados (ou "sem gaps")
3. Tasks geradas (contagem + principais) ou recomendação de Validator/Security direto
4. Conflitos de escopo com outras features (ex.: `permissions.ts`, `menu.ts`, `client.ts`)
5. Estado atualizado (JSON válido)
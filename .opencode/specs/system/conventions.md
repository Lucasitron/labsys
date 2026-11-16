# Convenções do Projeto FabLab (Consolidadas)

> Este documento consolida o que está **estabelecido na prática** (GitFlow, contrato real, infra real,
> código). Fontes: `docs/GITFLOW.md`, `docs/contract.gap.md`, `system/architecture.md`, `system/STATUS.md`.

## 1. GitFlow (reproduz `docs/GITFLOW.md` — verificado 19/09)

**Branches (vertentes por módulo, commits separados):**

| Branch | Propósito |
| :--- | :--- |
| `main` | Produção |
| `testing` | Homologação |
| `development` | **Merge-only** — recebe exclusivamente merges de `dev/backend` e `dev/frontend`. Sem commits diretos. |
| `dev/backend` | Backend PHP/Laravel: monólito modular `backend/` (módulos `Auth`, `Rh`, `Estoque`, `Vendas`, `Financeiro`, `Producao`, `Notification`, `Dashboard`, `Shared`) + `infra/`. |
| `dev/frontend` | SPA SvelteKit sob `frontend/`. |

```
main
 └─ testing
     └─ development   (merge-only)
         ├─ dev/backend
         └─ dev/frontend
```

Regras: nunca commit direto em `main/testing/development`; commits de backend → `dev/backend`;
commits de front → `dev/frontend`; commits mistos proibidos (separar back/front); `development`
só avança por merge (backend primeiro, depois front); propagação `development → testing → main`.

## 2. Commits (Conventional Commits)

Formato: `<tipo>(<escopo>): <descrição>` — tipos `feat, fix, docs, style, refactor, test, chore, security`.
Ex.: `feat(rh): valida apontamento de horas` · `fix(estoque): evita estoque negativo por concorrência`.
Commits atômicos por etapa; nunca código quebrado; rodar testes antes de commit.

## 3. Nomenclatura

### Recursos de API — **Português (canônico)** (conforme docs/0[1..08] e rotas reais)
- Rota raiz do front: `http://localhost:8080/api` → rotas `/api/auth/**`, `/api/rh/**`,
  `/api/estoque/**`, `/api/vendas/**`, `/api/financeiro/**`, `/api/producao/**`, `/api/notification/**`,
  `/api/dashboard/**`, `/api/tasks/**`.
- Backend expõe recursos em PT (ex.: `/pessoas`, `/funcionarios`, `/apontamentos-horas`, `/itens`,
  `/clientes`, `/orcamentos`, `/encomendas`, `/lancamentos`, `/projetos`, `/tarefas`, `/kanban`,
  `/notificacoes`, `/certificados`).
- Front SvelteKit define providers em EN dentro de `frontend/src/lib/api/` (ex.: `rh.ts`, `stock/`,
  `auth.ts`, `dashboard.ts`) mas chama os mesmos paths do backend no monólito. **Decisão travada:
  a fonte da verdade do recurso é o path do backend (PT); o front apenas aponta para ele.**

### PHP / Laravel (Backend)
- PSR-12; classes `PascalCase`; métodos/variáveis `camelCase`; constantes `UPPER_SNAKE_CASE`;
  namespaces `App\Modules\<Modulo>` e `App\Shared`.
- **Monólito modular MVC:** cada módulo tem `Models/` (Eloquent, `$table = '<schema>.<tabela>'`),
  `Http/Controllers` (C, só HTTP), `Http/Requests` (validação/autorização) e `Http/Resources`
  (`View` JSON). Regra de negócio em `Services/`/`Actions/` **só quando houver** — CRUD simples usa
  o `Model`; **não** criar `Repository` por hábito. `Contracts/` só quando consumido por outro módulo;
  auth/exceções/eventos transversais ficam em `App\Shared`.
- Migrações: `database/migrations/<modulo>/*.php` (schema por módulo, `Schema::create('<schema>.<tabela>', …)`).
- Eventos: padronizar `<contexto>.<entidade>.<açao>.event`; `Event`/`Listener`/`Job` do Laravel
  (fila `database` por padrão); Rabbit (`fablab.*`) só onde o contrato exigir assíncrono real.
- Testes: `tests/Unit` + `tests/Feature` (PHPUnit/Pest, `php artisan test`); cobertura mínima **80%**.

### Frontend (SvelteKit)
- Componentes `PascalCase.svelte`; arquivos TS `kebab-case`; funções `camelCase`;
  tipos `PascalCase`; stores globais em `src/lib/stores/`; API em `src/lib/api/`;
  rotas `src/routes/`. ESLint + Prettier + `tsc --noEmit` no build.

### Banco de Dados
- Nomes PT: tabelas/colunas `snake_case`; FKs `<entidade>_id`. Schema por módulo no banco `fablab`.

## 4. Estrutura de Pastas (Backend — monólito modular Laravel MVC)

```
backend/
├── composer.json
├── app/
│   ├── Events/                 (1 classe por const NAME)
│   ├── Http/Controllers/       (Controller base: principal() → Caller)
│   ├── Rules/                  (regras compartilhadas, ex.: CpfCnpjRule)
│   ├── Shared/                 (Auth/Caller, Exceptions/ApiException, Contracts)
│   └── Modules/<Modulo>/
│       ├── Models/             ├── Http/Controllers/   ├── Http/Requests/
│       ├── Http/Resources/     ├── Services|Actions/ (só se houver regra)
│       ├── Policies/  Events/  Listeners/              └── routes.php
├── bootstrap/  config/ (fablab.php: schedulers)  routes/console.php (Schedule)
├── database/migrations/<modulo>/   (migrações por schema)  ·  database/seeders/
└── tests/ (Unit | Feature)
```

Cada módulo publica: `routes.php`, `Models/`, `Http/` (Controllers/Requests/Resources),
`Policies/`, `Events/`/`Listeners/` e `Contracts/` quando consumido por outro módulo — o contrato
HTTP/eventos exato fica em `docs/contract.gap.md` e `docs/mokups/*`.

## 5. Testes e Qualidade
- Rodar `php artisan test` antes de todo commit/PR; cobertura ≥ 80%.
- Testes de segurança para endpoints sensíveis (401 sem token, 403 sem permissão).
- Validação de entrada em todos os endpoints (`FormRequest`).
- Auditoria: alterações sensíveis registram usuário e timestamp.

## 6. Segurança
- Auth de API via `jwt-auth` (guard `jwt`, `Bearer`); papéis em `Policy`/`Gate` + middleware.
- Senhas com `Hash::make` (bcrypt); nunca texto puro; credenciais via `.env` (não versionado).
- RBAC aplicado via matriz (`.opencode/specs/system/rbac-matrix.md`) e `Policy`/`Gate`.
- Comunicação externa via Tailscale + Nginx; banco/mensageria isolados na rede Docker.

## 7. Princípios de Código Mínimo (obrigatório para todos os agentes)

Antes de cada linha:
1. **É necessário?** — já existe código/nativo que resolve? Reuse (não duplique, não crie wrapper).
2. **A linguagem/plataforma resolve?** — mecanismo de PHP/Laravel/Eloquent/Svelte > abstração própria.
3. **Há forma menos verbosa?** — sem boilerplate, sem helper "por conveniência", sem prop/estado/tipo não consumido.
4. **Resolve problema real?** — especulativo/defensivo sem justificativa, re-export por hábito,
   mock/fallback sem contrato → **não escreva**. Se a task for desnecessária, **reporte** em vez de implementar.

Plano deve produzir a **menor quantidade de código**: reuse antes de criar (`task = modify`, não `create`);
cada `create` com justificativa; arquivos de conveniência (re-export, type "por previsão futura",
helper não consumido) → não planejar; ignorar itens especulativos/"preparar para o futuro".

Validação deve sinalizar o que **não** é a menor solução pertinente: código morto, wrapper
desnecessário, duplicação, view-model/type/prop sem uso, fallback/mock sem contrato → marcar para
remoção (BAIXA/MÉDIA).

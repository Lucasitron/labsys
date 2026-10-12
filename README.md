# labSys — Sistema de Gestão FabLab

Sistema gerencial completo para um Fab Lab / Makerspace. **Backend monólito modular em PHP 8.3 +
Laravel 12 (MVC)**, frontend **SvelteKit + TypeScript + Tailwind CSS** (SPA, adapter-node),
infraestrutura com **PostgreSQL** (schema por módulo), **Docker Compose** e **Tailscale**.
Ambiente local (Ubuntu Server + Docker Compose + Tailscale), sem nuvem.

## Visão Geral da Arquitetura

```
                     Internet / Tailscale
                             │
                        ┌────▼─────┐
                        │  Nginx   │ 80/443 (termina TLS)
                        └────┬─────┘
                             │  proxy_pass → app:8080
                        ┌────▼────────────────────────┐
                        │  app     (Laravel, :8080)   │
                        │  worker  (queue:work)       │
                        │  scheduler (schedule:run)   │
                        └────┬────────────────────────┘
                             │
                        ┌────▼────────────────────────┐
                        │  postgres 16 — banco fablab │
                        │  schemas: auth, rh, estoque,│
                        │  vendas, financeiro,        │
                        │  producao, notification     │
                        └──────────────────────────────┘
```

Um único backend Laravel servido em **uma** porta (`8080`) e **um** PostgreSQL (`fablab`) com
**schema por módulo**. O SPA fala com **um único endpoint HTTP**. **Sem** Eureka, API Gateway,
banco por serviço ou HTTP entre módulos. Detalhes de infraestrutura em
[`infra/docs/ARCHITECTURE.md`](infra/docs/ARCHITECTURE.md); as specs de domínio (fonte da verdade,
não versionadas) ficam em `.opencode/specs/` no ambiente de desenvolvimento.

## Módulos (bounded contexts)

Namespace `App\Modules\<Modulo>` · schema próprio · rotas `/api/<modulo>/**` (contrato com o front).

| Módulo | Schema | Responsabilidade |
| :--- | :--- | :--- |
| `Auth` | `auth` | Autenticação, JWT, RBAC, acesso RFID |
| `Rh` | `rh` | Pessoas, processo seletivo, ponto, horas, LMS |
| `Estoque` | `estoque` | Insumos, ferramentas, fornecedores, BOM, empréstimos |
| `Vendas` | `vendas` | Clientes, orçamentos, Kanban, marketplace, CRM |
| `Financeiro` | `financeiro` | Fluxo de caixa, custos (Job Order Costing) |
| `Producao` | `producao` | Projetos, tarefas, máquinas, 5S, Kanban de encomendas |
| `Notification` | `notification` | E-mails e notificações transversais |
| `Dashboard` | — | Agregador (sem persistência); chama `Contracts` in-process |

## Estrutura do Repositório

```
labSys/
├── backend/                    # Monólito Laravel (app/Modules/<Modulo>, MVC)
├── frontend/                   # SPA SvelteKit + TS + Tailwind
├── infra/                      # Docker Compose, Nginx, PostgreSQL
│   └── docs/                   # ARCHITECTURE, DEPLOY, RUNBOOK
└── *-service/ , web/           # LEGADO (Java/Spring) — referência AS-IS, a remover
```

## Pré-requisitos

*   Docker + Docker Compose
*   Node.js 20+ e npm (para o frontend)
*   (Opcional, ciclo completo) PHP 8.3 + Composer, se rodar o backend fora do Docker

## Como Executar

1.  Configurar ambiente e subir a stack (backend + Postgres):

    ```sh
    cp infra/.env.example infra/.env && chmod 600 infra/.env   # preencher APP_KEY/JWT_SECRET/POSTGRES_*
    docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d --build
    ```

    API em `http://localhost:8080` (health: `GET /up`). Seed dev: `admin@fablab.org` / `admin123`.

2.  Iniciar o frontend:

    ```sh
    cd frontend && npm install && npm run dev    # http://localhost:5173 (proxy /api → :8080)
    ```

## Testes

```sh
cd backend && php artisan test       # ou: composer test
```

## Git Flow

Branches principais: `main` (produção), `testing` (homologação), `development` (dev ativo).

*   Nunca commitar direto em `main` ou `testing`.
*   Trabalhar em `development`, `dev/backend`/`dev/frontend` ou `feature/*`/`bugfix/*`/`hotfix/*`.
*   Ao concluir uma etapa: merge para `development` → `testing` → `main`.
*   Commits seguem **Conventional Commits** (`feat`, `fix`, `docs`, `chore`, `test`, `security`, ...).

## Documentação

*   Backend: [`backend/README.md`](backend/README.md)
*   Frontend: [`frontend/README.md`](frontend/README.md)
*   Infraestrutura: [`infra/docs/`](infra/docs/) (ARCHITECTURE, DEPLOY, RUNBOOK)

## Licença

GNU General Public License v3. Ver [`LICENSE`](LICENSE).

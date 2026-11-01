# backend — Monólito modular FabLab (Laravel)

API do Sistema de Gestão FabLab. **PHP 8.3 + Laravel 12**, monólito **modular em MVC**: cada
bounded context é um módulo (`app/Modules/<Modulo>`) isolado por namespace e por **schema** no
Postgres único (`fablab`). O SPA SvelteKit consome **um único endpoint HTTP** (`/api/**`).

## Estrutura

```
app/
├── Events/                     # 1 classe por const NAME (eventos consolidados)
├── Http/Controllers/           # Controller base (principal() → Caller)
├── Rules/                      # regras compartilhadas (CpfCnpjRule)
├── Shared/
│   ├── Auth/Caller.php         # identidade do chamador (idUser/role/admin)
│   ├── Exceptions/             # ApiException + ErrorResponse
│   └── ...                     # Contracts transversais
├── Modules/<Modulo>/
│   ├── Models/                 # Eloquent ($table = '<schema>.<tabela>')
│   ├── Http/                   # Controllers · Requests (FormRequest) · Resources
│   ├── Services/               # só quando há regra de negócio real
│   ├── Policies/  Events/  Listeners/
│   └── routes.php              # rotas /api/<modulo>
└── Console/Commands/
config/fablab.php               # kill-switches de scheduler
database/
├── migrations/<modulo>/        # uma pasta por schema (auth, rh, estoque, ...)
└── seeders/
```

**Eventos:** uma classe por `const NAME` em `app/Events/`; consumidores em `Modules/*/Listeners/`
e binds em `AppServiceProvider`.
**Schedulers:** flags `SCHEDULER_ENABLED` lidas de `config('fablab.schedulers.*')`.

## Como executar (local, via Docker)

```sh
cp ../infra/.env.example ../infra/.env && chmod 600 ../infra/.env   # APP_KEY/JWT_SECRET/POSTGRES_*
docker compose -f ../infra/docker-compose.yml -f ../infra/docker-compose.dev.yml up -d --build
```

API em `http://localhost:8080` (health: `GET /up`; seed dev `admin@fablab.org` / `admin123`).

Fora do Docker (PHP 8.3 + Composer, Postgres em `:5432`):

```sh
composer install
cp .env.example .env && php artisan key:generate
php artisan migrate --seed
php artisan serve --port=8080
```

## Testes

```sh
php artisan test          # ou: composer test
php artisan test --filter=Modulo
```

A suíte roda em **PostgreSQL 16** (`phpunit.xml` aponta `DB_HOST=127.0.0.1:5433`, banco
`fablab_testing`). No CI, o banco é provisionado automaticamente pelo serviço do workflow
([`backend-ci.yml`](../.github/workflows/backend-ci.yml)).

## Operação

*   Migrações rodam automaticamente no boot do container (`docker-entrypoint.sh`).
*   Fila `database` (`php artisan queue:work`) + agendamentos (`schedule:run`).
*   Deploy, rollback e backups: [`../infra/docs/DEPLOY.md`](../infra/docs/DEPLOY.md) e
    [`../infra/docs/RUNBOOK.md`](../infra/docs/RUNBOOK.md).

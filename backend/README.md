# backend — Monólito modular FabLab (Laravel)

API do Sistema de Gestão FabLab. **PHP 8.3 + Laravel 12**, monólito **modular em MVC**: cada
bounded context é um módulo (`app/Modules/<Modulo>`) isolado por namespace e por **schema** no
Postgres único (`fablab`). O SPA SvelteKit consome **um único endpoint HTTP** (`/api/**`).

## Estrutura

```
app/
├── Shared/                     # transversal (Auth, Exceptions, Events, Contracts base)
├── Modules/<Modulo>/
│   ├── Models/                 # Eloquent ($table = '<schema>.<tabela>')
│   ├── Http/                   # Controllers · Requests (FormRequest) · Resources
│   ├── Services/ (ou Actions/) # só quando há regra de negócio real
│   ├── Policies/  Events/  Listeners/
│   └── routes.php              # rotas /api/<modulo>
└── Console/Commands/
database/
├── migrations/<modulo>/        # uma pasta por schema (auth, rh, estoque, ...)
└── seeders/
```

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

## Operação

*   Migrações rodam automaticamente no boot do container (`docker-entrypoint.sh`).
*   Fila `database` (`php artisan queue:work`) + agendamentos (`schedule:run`).
*   Deploy, rollback e backups: [`../infra/docs/DEPLOY.md`](../infra/docs/DEPLOY.md) e
    [`../infra/docs/RUNBOOK.md`](../infra/docs/RUNBOOK.md).

# Arquitetura — Infraestrutura FabLab (monólito modular Laravel)

> Fonte da verdade do sistema: `.opencode/specs/system/architecture.md`.
> Este documento descreve **só a infraestrutura** (rede, containers, persistência, borda).

## Visão geral

Um **único** backend Laravel (`backend/`) — monólito modular, MVC — servido em **uma** porta
(`8080`) com **um** PostgreSQL (`fablab`) e **schema por módulo**. O SPA **SvelteKit** (adapter-node)
fala com **um único endpoint HTTP**. Sem Eureka, sem API Gateway, sem banco por serviço, sem HTTP
entre módulos.

```
                    Internet / Tailscale
                            │
                       ┌────▼─────┐
                       │  Nginx   │  80/443 (termina TLS)
                       └────┬─────┘
                            │  proxy_pass → app:8080
                       ┌────▼───────────────────────────┐
                       │  app  (Laravel, artisan serve)  │  :8080
                       │  worker (queue:work)            │
                       │  scheduler (schedule:run)       │
                       └────┬───────────────────────────┘
                            │
                       ┌────▼───────────────────────────┐
                       │  postgres 16  (banco `fablab`)  │  5432 (só na rede Docker)
                       │  schemas: auth, rh, estoque,    │
                       │  vendas, financeiro, producao,  │
                       │  notification                   │
                       └────────────────────────────────┘
```

## Topologia de containers (Docker Compose)

Rede `fablab-net` (bridge); volume nomeado `postgres-data`.

| Serviço | Imagem | Porta | Papel |
| :--- | :--- | :--- | :--- |
| `postgres` | `postgres:16-alpine` | 5432 | Banco único `fablab`, schemas por módulo |
| `app` | `ghcr.io/fablab/backend:${TAG}` (dev: build `../backend`) | 8080 | API Laravel (`artisan serve`) |
| `worker` | mesma imagem | — | Consome a fila: `queue:work --sleep=3 --tries=3 --max-time=3600` |
| `scheduler` | mesma imagem | — | `schedule:run` a cada 60s (loop) |

- **Sem RabbitMQ, sem Redis, sem Eureka/Gateway.** Filas/cache/sessão usam o driver nativo
  **`database`** (tabelas `jobs`, `cache`).
- Overlays: `docker-compose.dev.yml` (build local, publica `5432` e `8080`, seed ON),
  `docker-compose.testing.yml` (imagem `:testing`), `docker-compose.prod.yml` (imagem `:${TAG}`,
  **não** publica `5432`)
- **Entrypoint** de qualquer container (`backend/docker-entrypoint.sh`): espera o Postgres →
  `php artisan migrate --force` → `db:seed --force` (no-op se seed off/banco não vazio) →
  `config:cache`/`route:cache` → `queue:restart`. Migrações concorrentes são serializadas pelo lock
  de migração (cache `database`).

## Persistência

- **Um** banco PostgreSQL `fablab`; **schema por módulo** (não database-per-service).
- Migrações do Laravel por módulo em `backend/database/migrations/<modulo>/`, com tabelas
  qualificadas pelo schema (ex.: `Schema::create('rh.pessoa', …)`; Eloquent `$table = 'rh.pessoa'`).
- O módulo lê/escreve **só** o próprio schema; leitura cruzada via `Contract` do módulo dono.
- Backups: `pg_dump` (ver RUNBOOK/DEPLOY).

## Comunicação

- **Síncrona (in-process):** chamada direta ao `Contract` público do módulo dono
  (`app/Shared/Contracts` ou `<Modulo>/Contracts`).
- **Assíncrona:** `Event`/`Listener`/`Job` do Laravel com fila `database`; nomes de evento
  preservados do legado (`<contexto>.<entidade>.<ação>.event`). RabbitMQ só se um contrato exigir
  entrega durável real (hoje: não).

## Borda / Segurança

- **Nginx** (`infra/nginx/nginx.conf`) termina TLS (Tailscale ou Let's Encrypt) e faz `proxy_pass`
  para `app:8080`. Produção redireciona 80→443.
- Externo **apenas** via Tailscale + Nginx.
- Auth de API via **Laravel Sanctum** (Bearer) + `Policy`/`Gate` + middleware RBAC
  (`system/rbac-matrix.md`). A antiga fronteira do Gateway é agora middleware + Policy.
- Segredos **nunca** versionados: `infra/.env` (chmod 600) ou ambiente do orquestrador
  (`APP_KEY`, `JWT_SECRET`, `POSTGRES_*`, `APP_URL`). Ver `infra/.env.example`.

## Observabilidade

- Healthcheck do `app`: `GET /up` (usado pelo compose). Logs em `storage/logs` (por container).
- (Futuro) Prometheus/Grafana + centralização de logs (Loki/ELK).

## Legado no repositório

Os diretórios `*-service/` (Java/Spring), `discovery-service/`, `gateway-service/` e `web/` ainda
existem **apenas como referência AS-IS**; o alvo é consolidá-los em `backend/` e removê-los. Nada
de runtime aponta para eles.

# Runbook — Operação do FabLab (monólito Laravel)

Guia rápido de operação e troubleshooting. Visão de infra: [`ARCHITECTURE.md`](ARCHITECTURE.md).

> Atalho: defina `DC="docker compose -f infra/docker-compose.yml -f infra/docker-compose.<env>.yml"`
> (`<env>` = `dev` | `testing` | `prod`) e use `$DC` abaixo.

## Comandos de rotina

```sh
# Subir/parar stack
$DC up -d --build        # dev/testing (build). prod: sem --build
$DC down                 # para (mantém volume)
$DC down -v              # RESET: remove dados/volume (cuidado!)

# Status e logs
$DC ps
$DC logs -f app
$DC logs -f worker

# Artisan dentro do container (migrate é automático no entrypoint; aqui é p/ tarefas avulsas)
$DC exec app php artisan about
$DC exec app php artisan migrate:status
$DC exec app php artisan queue:restart      # sinaliza workers após deploy
$DC exec app php artisan tinker
```

## Verificação de saúde

```sh
curl -fsS http://<host>:8080/up                # health da API (Laravel)
$DC ps                                          # app/worker/scheduler UP
docker compose ... exec -T postgres pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

Critérios: `app` saudável (`/up`), `worker` consumindo a fila `jobs`, `scheduler` rodando.

## Problemas comuns

### `app` não sobe / reinicia
1. Postgres acessível? (`pg_isready`; o entrypoint aguarda até ~60s).
2. `APP_KEY`/`JWT_SECRET` definidos? (o boot faz **fail-fast** sem eles).
3. Logs: `$DC logs app` (erros de migration/seed aparecem aqui).

### Migração falha no boot
1. `$DC exec app php artisan migrate:status` — veja a pendente.
2. Se for dado incompatível (ex.: duplicados em `auth.login.id_user`), ver §"Migração auth v9".
3. Corrija a migração e suba de novo (o entrypoint reaplica).

### Fila não consome
1. `QUEUE_CONNECTION=database` e tabela `jobs` existe (criada nas migrações base).
2. Reinicie o worker: `$DC restart worker` (ou `queue:restart` após deploy de código).
3. Logs: `$DC logs worker`.

### Agendamentos não rodam
1. `scheduler` está UP? (roda `schedule:run` a cada 60s).
2. Logs: `$DC logs scheduler`.

### Porta ocupada
Altere o mapeamento em `infra/docker-compose.dev.yml` (`app` publica `8080:8080`); em prod, o Nginx
aponta para `app:8080`.

## Migração `auth v9` (backfill/dedup `login.id_user`)

A migração `auth v9a` executa **automaticamente** no primeiro boot (arquiva credenciais duplicadas
em `auth.login_dedup_backup` e remove; a `auth v9` cria a UNIQUE `uk_login_id_user`). Em banco sem
duplicados é no-op.

```sh
# Auditoria read-only (recomendado ANTES do deploy)
$DC exec app php artisan auth:dedup-login-id-user

# Verificação pós-deploy
$DC exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c \
"select conname from pg_constraint where conname='uk_login_id_user';
 select count(*) from auth.login_dedup_backup;
 select id_user,count(*) from auth.login group by id_user having count(*)>1;"

# Rollback do banco (reverte v9 depois v9a)
$DC exec app php artisan migrate:rollback --step=2 --force
```

Procedimento completo (backup, dry-run, comunicação aos afetados): `.opencode/state/auth-v9-backfill-plan.md`.

## Reset do ambiente

```sh
$DC down -v      # remove o volume postgres-data (perde dados)
$DC up -d        # recria banco/vhost; o entrypoint migra do zero
```

## Contatos e alertas

*   Healthcheck de `/up` pode alimentar alertas (futuro: Prometheus/Grafana).
*   Notificações de e-mail com falha ficam `PENDENTE` (módulo Notification) para reenvio.

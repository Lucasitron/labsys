# Briefing de Infra — Deploy da migração `auth v9` (UNIQUE `login.id_user`)

> **Para:** time de infra/ops · **De:** orquestração backend · **Data:** 2026-10-12
> **Escopo:** subir o backend do monólito que contém os Ciclos 1–8 (inclui `auth v9a` + `auth v9`)
> em produção, com **backup e dedup** seguros. Nada aqui altera código.

## 0. Veredito rápido
- A migração é **aditiva e reversível**; a mudança de dado (dedup) é **automática e idempotente**.
- **Único risco real**: se o banco de produção já tiver `auth.login` com `id_user` duplicado, a
  entrada da UNIQUE falharia **sem** a `v9a`. Ela já está no pacote e roda **antes** — o deploy é seguro.
- **Ação humana obrigatória antes do deploy**: `pg_dump`. Recomendado: rodar a **auditoria read-only**
  e revisar a lista de duplicados (se houver).

## 1. Modelo de deploy atual (confirmado em `dev/backend`)
- **1 imagem** do monólito: `ghcr.io/fablab/backend:${TAG}` (`backend/Dockerfile`, PHP 8.3, `artisan serve` :8080).
- **Compose**: `postgres` (Postgres 16, banco único `fablab`, schemas por módulo) + `app` + `worker`
  (`queue:work`) + `scheduler` (`schedule:run`). Sem Eureka/Gateway/RabbitMQ.
- **Entrypoint** (`backend/docker-entrypoint.sh`), em cada container:
  1. espera o Postgres;
  2. **`php artisan migrate --force`** ← **é aqui que `v9a`+`v9` rodam automaticamente**;
  3. `php artisan db:seed --force` (seed do admin é no-op em prod: `AUTH_SEED_ENABLED=false`);
  4. `config:cache` / `route:cache` / `queue:restart`.
- Migrações concorrentes nos 3 containers são serializadas pelo **lock de migração** (cache `database`).
- Comandos (prod):
  ```sh
  docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml \
    pull && docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml up -d
  ```
  (`TAG`, `APP_KEY`, `JWT_SECRET`, `POSTGRES_USER/PASSWORD`, `APP_URL` via ambiente/`infra/.env` 0600.)

## 2. O que o deploy faz no banco
| Migração | Efeito | Reversível |
|---|---|---|
| `2026_10_12_000000_auth_v9a_dedup_login_id_user` | Cria `auth.login_dedup_backup`; arquiva **e remove** credenciais com `id_user` duplicado (mantém a `ATIVO` mais antiga); **não toca** `user_permissions`/`access_log`/`token_blacklist` | Sim (`reverter()` restaura IDs originais) |
| `2026_10_12_000001_auth_v9_login_id_user_unique` | Adiciona **constraint** `uk_login_id_user` (UNIQUE) em `auth.login(id_user)` | Sim (`down` dropa a constraint) |

Em banco **sem duplicados** (cenário normal/limpo), a `v9a` é **no-op** e só cria a tabela de backup.

## 3. Pré-deploy (obrigatório)
```sh
# 3.1 BACKUP lógico (fora do volume) — guarde o arquivo
docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > backup_pre_auth_v9_$(date +%F_%H%M).dump

# 3.2 AUDITORIA read-only (NÃO altera dados) — usa a imagem NOVA (deve conter o comando)
docker run --rm --network fablab-net \
  -e APP_KEY -e JWT_SECRET -e DB_CONNECTION=pgsql -e DB_HOST=postgres -e DB_PORT=5432 \
  -e DB_DATABASE="$POSTGRES_DB" -e DB_USERNAME="$POSTGRES_USER" -e DB_PASSWORD="$POSTGRES_PASSWORD" \
  ghcr.io/fablab/backend:<TAG-nova> php artisan auth:dedup-login-id-user
# >>> Se listar grupos: revise quem será mantido (ATIVO mais antigo) e quem será arquivado. <<<
```
Alternativa sem subir nada: SQL de auditoria em `.opencode/state/auth-v9-backfill-plan.md` §2.

> **Não** é necessário rodar `--apply` manualmente: a `v9a` faz o mesmo no `migrate`. Rodar antes só
> antecipa o efeito; a `v9a` depois vira no-op (idempotente).

## 4. Deploy
```sh
export TAG=<tag-que-contem-ciclos-1-8>
docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml pull
docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml up -d
# Acompanhar o boot (migrate/seed/caches):
docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml logs -f app
```

## 5. Verificação pós-deploy
```sh
# 5.1 Saúde da API (proxy/Tailscale na borda)
curl -fsS http://<host>:8080/up && echo OK

# 5.2 Estrutura do banco
docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c \
"select conname from pg_constraint where conname='uk_login_id_user';
 select count(*) as arquivados from auth.login_dedup_backup;
 select id_user, count(*) from auth.login group by id_user having count(*)>1;"

# 5.3 Smoke funcional: login admin (se seed ativo) ou usuário real + 1 rota autenticada.
```
Critérios de aceite: constraint presente; consulta de duplicados **vazia**; login OK; worker consumindo.

## 6. Pós-deploy — comunicação
- Os tokens JWT das credenciais **removidas** continuam válidos, mas o papel resolve para o `id_user`
  remanescente. Recomendação: **avisar os usuários afetados a refazer login** (lista = `arquivar` do §3.2).

## 7. Rollback
```sh
# 7.1 Rollback de aplicação (imagem anterior) — suficiente na maioria dos casos:
TAG=<tag-anterior> docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml up -d
```
> O código anterior ignora a constraint e a tabela de backup → **rollback de imagem é seguro sem mexer no banco**.

```sh
# 7.2 Rollback do BANCO (só se precisar desfazer a v9) — reverte v9 (dropa UNIQUE) e v9a (restaura IDs):
docker compose ... exec -T app php artisan migrate:rollback --step=2 --force
# 7.3 Pior caso: restaurar o dump do §3.1
```
Ordem garantida: `v9` (remove constraint) **antes** de `v9a` (restaura). Validação já feita em PG limpo.

## 8. Casos de borda
| Caso | Ação |
|---|---|
| `auth.login` **vazio** (instalação nova) | nada a fazer; `v9a` só cria a tabela de backup |
| Duplicados encontrados | revisar §3.2; opcional renomear/ajustar manualmente **antes** do `up` |
| `id_user` **órfão** (sem `rh.pessoa`) | **não** bloqueia a UNIQUE; reportado no SQL de auditoria §2(c). FK **não** é adicionada (fora do escopo) |
| Prod sem `AUTH_SEED_ENABLED` | admin **não** é recriado (correto); garanta credenciais de operação já existentes |
| Redeploy/retry | idempotente; sem pendência = no-op |

## 9. Pendências / avisos (não bloqueiam este deploy)
- **Docs de infra desatualizados**: `infra/docs/DEPLOY.md` e `RUNBOOK.md` (e `infra/nginx/nginx.conf`)
  ainda descrevem os **microserviços Java** (Eureka/Gateway/portas 8081-8087/RabbitMQ). O compose do
  monólito (commit `5c2f62b`) é a realidade. → **tarefa separada**: alinhar docs/nginx ao monólito.
- **Imagem de teste local nesta máquina**: `fablab-php:8.3-test` (não é a de produção).

## 10. Referências
- Plano detalhado + SQL de auditoria: `.opencode/state/auth-v9-backfill-plan.md`
- Artefatos (código): `LoginIdUserDedupService`, migração `..._auth_v9a_dedup_login_id_user`,
  comando `auth:dedup-login-id-user`, `LoginIdUserDedupServiceTest` — `dev/backend@b7fd0b8`, `development@5d3216c`.
- Validação: suíte **419 passed + 4 skipped**; PG limpo (fresh / dry-run+apply / rollback step=2).

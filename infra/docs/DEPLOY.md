# Guia de Deploy — FabLab (monólito Laravel)

Ambiente alvo: **Ubuntu Server local, Docker Compose, Tailscale, HTTPS**. Um backend Laravel
(`backend/`) em **uma** imagem e **uma** porta (`8080`) + **um** PostgreSQL (`fablab`).
Visão de infraestrutura: [`ARCHITECTURE.md`](ARCHITECTURE.md).

## 1. Pré-requisitos

*   Docker Engine + Docker Compose plugin.
*   Tailscale instalado e conectado (rede mesh).
*   (Build local de imagem) Apenas Docker — o PHP roda dentro do container.

## 2. Variáveis de ambiente

Copie o modelo e preencha os segredos (**nunca versionar**):

```sh
cp infra/.env.example infra/.env && chmod 600 infra/.env
```

Valores obrigatórios (ver `infra/.env.example`):

| Variável | Descrição |
| :--- | :--- |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | Banco único `fablab` |
| `APP_KEY` | `php artisan key:generate --show` |
| `JWT_SECRET` | `php artisan jwt:secret --show` (≥ 32 chars) |
| `APP_URL` | URL pública (ex.: `https://fablab.exemplo.br`) |
| `AUTH_SEED_ENABLED` | **dev only** — em prod manter `false` (seed do admin raiz) |
| `TAG` | Versão da imagem publicada (obrigatória em testing/prod) |

## 3. Ambientes

### Desenvolvimento (local, build)

```sh
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d --build
```

*   `app` em `http://localhost:8080` (`GET /up` = health); Postgres em `localhost:5432`.
*   Seed do admin raiz: `AUTH_SEED_ENABLED=true` → `admin@fablab.org` / `admin123`.
*   Frontend (fora do Docker):

    ```sh
    cd frontend && npm install && npm run dev   # Vite em http://localhost:5173, proxy /api → :8080
    ```

### Homologação (testing)

```sh
TAG=testing docker compose -f infra/docker-compose.yml -f infra/docker-compose.testing.yml up -d --build
```

### Produção

Imagens publicadas previamente (CI) no registry. **Sem `build`** em prod:

```sh
TAG=1.2.3 docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml pull
TAG=1.2.3 docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml up -d
```

Regras duras do overlay prod: `TAG` obrigatória; `APP_DEBUG=false`; `AUTH_SEED_ENABLED=false`;
Postgres **sem** `ports:` (5432 só na rede `fablab-net`); `app` publica `8080` para o proxy/Tailscale.

## 4. HTTPS

*   `infra/nginx/nginx.conf` termina TLS e encaminha para `app:8080`; produção redireciona 80→443.
*   Certificados: Tailscale Serve ou Certbot/Let's Encrypt. Todo tráfego externo passa pelo Nginx.

## 5. Verificação pós-deploy

```sh
curl -fsS http://<host>:8080/up && echo OK                 # health da API
docker compose ... ps                                       # app/worker/scheduler UP
docker compose ... logs -f app                              # migrate/seed/caches no boot
```

Smoke funcional: login + 1 rota autenticada (ver RUNBOOK).

## 6. Rollback

```sh
TAG=<versao-anterior> docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml up -d
```

As migrações do Laravel são **aditivas**: o código anterior ignora tabelas/constraints novas, então
o rollback de imagem é seguro sem desfazer o banco. Para desfazer migrações específicas, ver o
runbook de `auth v9` em `.opencode/state/auth-v9-backfill-plan.md`.

## 7. Backups

*   PostgreSQL (banco único `fablab`):
    ```sh
    docker compose ... exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > fablab_$(date +%F).dump
    ```
*   Restauração: `pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean fablab_<data>.dump`.

## 8. CI/CD (planejado)

Pipeline por branch (Git Flow em `docs/GITFLOW.md`):

*   `development` → build + testes (`composer test` / PHPUnit);
*   `testing` → build + testes de segurança/contrato + publicação `:testing`;
*   `main` → publicação `:<tag>` em `ghcr.io/fablab/backend`.

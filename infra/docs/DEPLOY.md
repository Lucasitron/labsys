# Guia de Deploy — FabLab

Ambiente alvo: Ubuntu Server local, Docker Compose, Tailscale, HTTPS.

## 1. Pré-requisitos

*   Docker Engine + Docker Compose plugin.
*   Tailscale instalado e conectado (rede mesh).
*   Java 21 e Maven 3.9+ (apenas para build local).

## 2. Variáveis de Ambiente

Copie o modelo de ambiente e preencha os segredos (nunca versionar):

```sh
# .env (na raiz do repositório ou em infra/)
POSTGRES_USER=fablab
POSTGRES_PASSWORD=<senha-forte>
POSTGRES_DB=fablab_auth
RABBITMQ_USER=fablab
RABBITMQ_PASS=<senha-forte>
TAG=dev
```

## 3. Ambientes

### Desenvolvimento (local)

```sh
# 1) Infra base (Postgres + RabbitMQ)
docker compose -f infra/docker-compose.yml up -d

# 2) Stack completa com build
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up --build -d

# 3) Frontend (via npm)
cd frontend && npm install && npm run dev
```

Checklist de saúde:

| Componente | URL |
| :--- | :--- |
| Eureka | http://localhost:8761 |
| Gateway | http://localhost:8080/actuator/health |
| RabbitMQ | http://localhost:15672 |
| Auth | http://localhost:8081/actuator/health |

### Homologação (testing)

```sh
TAG=testing docker compose -f infra/docker-compose.yml -f infra/docker-compose.testing.yml up --build -d
```

### Produção

Imagens publicadas em um registry (ex: GHCR). Tague o ambiente com `.env`:

```sh
TAG=1.2.3 docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml up -d
```

## 4. HTTPS

*   O `infra/nginx/nginx.conf` termina TLS. Em produção, aponte os certificados
    (Tailscale Serve ou Certbot/Let's Encrypt).
*   Todo tráfego externo passa obrigatoriamente pelo Gateway.

## 5. Rollback

1.  Identificar a TAG anterior válida.
2.  `TAG=<anterior> docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.yml up -d`.
3.  Validation smoke: health checks + login E2E.

## 6. Backups

*   PostgreSQL: `pg_dump` diário por banco (`fablab_*`).
*   RabbitMQ: as definições ficam versionadas em `infra/rabbitmq/definitions.json`.

## 7. CI/CD (planejado)

Pipeline por branch:

*   `development` → build + testes unitários/integração;
*   `testing` → build + testes de segurança/contrato + publicação `:testing`;
*   `main` → build + publicação `:<tag>` no GHCR.
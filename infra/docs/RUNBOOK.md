# Runbook — Operação do FabLab

Guia rápido para operação e troubleshooting do ambiente.

## Comandos de Rotina

```sh
# Subir/parar infra base
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml down

# Subir stack dev completa
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up --build -d

# Logs de um serviço
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml logs -f auth

# Status
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml ps
```

## Verificação de Saúde

```sh
curl -s http://localhost:8761/actuator/health        # Eureka
curl -s http://localhost:8080/actuator/health        # Gateway
curl -s http://localhost:8081/actuator/health        # Auth
curl -s http://localhost:8082/actuator/health        # RH
curl -s http://localhost:5432/...
curl -s -u fablab:fablab http://localhost:15672/api/overview  # RabbitMQ
```

Debounce de subida: **Eureka** deve responder primeiro; os clientes sobem em
seguida. Se um serviço não aparece no dashboard do Eureka, verifique
`EUREKA_URI` e os logs.

## Problemas Comuns

### Serviço não registra no Eureka
1. Eureka está `UP`? (`curl localhost:8761/actuator/health`)
2. A variável `EUREKA_URI` aponta para `http://discovery:8761/eureka` (rede Docker)?
3. Nos logs do serviço consta erro de conexão com Eureka?

### Flyway falha na subida
1. Confirmar que o banco do serviço existe (ver `infra/postgres/init-scripts/init.sql`).
2. Corrigir a migration e subir novamente. Flyway bloqueia estado inconsistente.

### RabbitMQ: fila não consome eventos
1. Verificar exchange/queue em `definitions.json` e o nome do evento publicado.
2. Conferir usuário/senha e `RABBITMQ_HOST`.

### Porta ocupada
O serviço está mapeado para sua porta padrão (8081 a 8087). Altere a variável
`PORT` no serviço/nosso compose e o `server.port` correspondente.

## Reset do Ambiente

```sh
docker compose -f infra/docker-compose.yml down -v   # remove dados (cuidado!)
docker compose -f infra/docker-compose.yml up -d     # recria bancos/vhosts
```

## Contatos e Alertas

*   Healthchecks periódicos no Gateway devem acionar alertas (futuro: Prometheus/Grafana).
*   Notificações de falha de envio de e-mail são retidas no Notification Service
    (`status = PENDENTE`) para reenvio manual.
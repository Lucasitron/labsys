# Arquitetura — Infraestrutura FabLab

## Visão de Rede

Todos os contêineres compartilham a rede `fablab-net` (bridge). A comunicação entre
serviços ocorre por nome de serviço Docker (`discovery`, `gateway`, `auth`, `rh`, ...).

```
                     Internet / Tailscale
                             │
                        ┌────▼─────┐
                        │  Nginx   │ 443/80  (termina TLS)
                        └────┬─────┘
                             │
                        ┌────▼─────┐
                        │  Gateway │ 8080  (Spring Cloud Gateway + Eureka discovery)
                        └────┬─────┘
                             │
                     ┌───────▼────────┐
                     │    Eureka      │ 8761
                     └───────┬────────┘
                             │
   ┌────────┬───────┬────────┼────────┬─────────┬─────────┬────────────┐
   ▼        ▼       ▼        ▼        ▼         ▼         ▼            ▼
  Auth     RH     Estoque   Vendas  Financeiro Produção  Notification  Frontend
  8081    8082     8083     8084      8085      8086       8087          5173/3000
   │        │       │        │        │         │          │
   └─────── Postgres 16 (5432)  ────  RabbitMQ (5672/15672) ────────────┘
```

## Topologias de Banco

*   **Database-per-service:** cada microsserviço possui seu próprio banco
    (`fablab_auth`, `fablab_rh`, ...), criado em `postgres/init-scripts/init.sql`.
*   **Migrações:** Flyway (`classpath:db/migration`) em cada serviço.
*   **Notification Service** mantém dois esquemas/estruturas no seu banco:
    ativo (`notificacao`) e histórico (`notificacao_historico`, retenção 90 dias).

## Mensageria

*   RabbitMQ é o barramento assíncrono. As definições iniciais ficam em
    `rabbitmq/definitions.json` (exchanges, queues, bindings).
*   Padrão de nomenclatura de eventos: `<dominio>.<acao>` (ex: `ponto.registrado`,
    `estoque.baixo`, `encomenda.entregue`).

## Camadas e Padrões por Serviço

Cada microsserviço segue a mesma organização de pacotes:

```
com.fablab.<servico>/
├── config/        → Security, RabbitMQ, JPA
├── controller/    → REST
├── dto/           → abstrações de entrada/saída
├── entity/        → entidades JPA
├── exception/     → exceções + @ControllerAdvice
├── mapper/        → Entity <-> DTO
├── repository/    → Spring Data JPA
└── service/       → regras de negócio
```

## Segurança

*   TLS via Tailscale (comunicações dentro da mesh).
*   Autenticação JWT e autorização RBAC na borda (Gateway) e em cada serviço.
*   Segredos nunca versionados: variáveis de ambiente + `.env` local fora do git.

## Observabilidade

*   Spring Boot Actuator expõe `health`, `info` e `metrics` em todos os serviços.
*   Rota de health do Gateway: `GET /actuator/health`.
*   (Futuro) Prometheus + Grafana e centralização de logs (Loki/ELK).
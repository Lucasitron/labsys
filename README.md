# labSys — Sistema de Gestão FabLab

Sistema gerencial completo para um Fab Lab / Makerspace. Microsserviços em **Java 21 + Spring Boot 3.x**, frontend **SvelteKit + TypeScript + Tailwind CSS**, infraestrutura com **PostgreSQL**, **RabbitMQ**, **Spring Cloud Gateway** e **Eureka**. Ambiente local (Ubuntu Server + Docker Compose + Tailscale).

## Visão Geral da Arquitetura

```
                        ┌──────────────┐
   Cliente (SPA) ─────▶ │  Nginx/HTTPS  │
                        └──────┬───────┘
                               │
                        ┌──────▼───────┐
                        │  Gateway      │  (8080)
                        └──────┬───────┘
                               │
                       ┌───────▼────────┐
                       │   Eureka        │  (8761) — Service Registry
                       └───────┬────────┘
                               │
   ┌────────┬─────────┬────────┼────────┬─────────┬──────────┐
   ▼        ▼         ▼        ▼        ▼         ▼          ▼
 Auth      RH      Estoque   Vendas  Financeiro Produção   Notification
 (8081)   (8082)    (8083)   (8084)   (8085)    (8086)      (8087)
   │        │         │        │        │         │          │
   └─────── Postgres (5432) ────┴─── RabbitMQ (5672) ────────┘
```

## Microsserviços

| Serviço | Diretório | Porta | Responsabilidade |
| :--- | :--- | :--- | :--- |
| Discovery Service | `discovery-service/` | 8761 | Service Registry (Eureka) |
| Gateway Service | `gateway-service/` | 8080 | Roteamento, balanceamento, segurança na borda |
| Auth & Identity | `auth-service/` | 8081 | Autenticação, JWT, RBAC, acesso RFID |
| Pessoas & RH | `rh-service/` | 8082 | Pessoas, processo seletivo, ponto, horas, LMS |
| Estoque & Suprimentos | `estoque-service/` | 8083 | Insumos, ferramentas, fornecedores, BOM, empréstimos |
| Vendas & CRM | `vendas-service/` | 8084 | Clientes, orçamentos, Kanban, marketplace, CRM |
| Financeiro | `financeiro-service/` | 8085 | Fluxo de caixa, custos (Job Order Costing) |
| Produção & Projetos | `producao-service/` | 8086 | Projetos, tarefas, máquinas, 5S, Kanban de encomendas |
| Notification | `notification-service/` | 8087 | E-mails e notificações transversais (RabbitMQ) |

## Estrutura do Repositório

```
labSys/
├── docs/                       # Documentação técnica (Bounded Contexts + specs)
├── discovery-service/          # Eureka Server
├── gateway-service/            # Spring Cloud Gateway
├── auth-service/               # Auth & Identity
├── rh-service/                 # Pessoas & RH
├── estoque-service/            # Estoque & Suprimentos
├── vendas-service/             # Vendas & CRM
├── financeiro-service/         # Financeiro
├── producao-service/           # Produção & Projetos
├── notification-service/       # Notification
├── frontend/                   # SPA SvelteKit + TS + Tailwind
└── infra/                      # Docker Compose, Nginx, PostgreSQL, RabbitMQ
```

## Pré-requisitos

*   Java 21 (JDK)
*   Maven 3.9+
*   Node.js 20+ e npm
*   Docker + Docker Compose

## Como Executar

1.  Subir a infraestrutura base:

    ```sh
    docker compose -f infra/docker-compose.yml up -d
    ```

2.  Iniciar o Discovery Service:

    ```sh
    cd discovery-service && mvn spring-boot:run
    ```

3.  Iniciar os demais serviços (cada um em terminal/guia próprio):

    ```sh
    cd gateway-service && mvn spring-boot:run
    cd auth-service && mvn spring-boot:run
    # ... rh, estoque, vendas, financeiro, producao, notification
    ```

4.  Iniciar o frontend:

    ```sh
    cd frontend && npm install && npm run dev
    ```

## Git Flow

Branches principais: `main` (produção), `testing` (homologação), `development` (dev ativo).

*   Nunca commitar direto em `main` ou `testing`.
*   Trabalhar em `development` ou em `feature/*`, `bugfix/*`, `hotfix/*`.
*   Ao concluir uma etapa: PR para `development` → merge → `testing` → `main`.
*   Commits seguem **Conventional Commits** (`feat`, `fix`, `docs`, `chore`, `test`, `security`, ...).

## Documentação

*   Bounded Contexts e specs técnicas: [`docs/`](docs/)
*   Infraestrutura: [`infra/docs/`](infra/docs/) (ARCHITECTURE, DEPLOY, RUNBOOK)

## Licença

GNU General Public License v3. Ver [`LICENSE`](LICENSE).
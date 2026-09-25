# vendas-service

**Vendas & CRM Service** do Sistema de Gestão FabLab. Gestão comercial: clientes
(PF/PJ), orçamentos, ciclo de vida das encomendas com Kanban, recibo interno,
vendas em marketplaces (registro manual) e rotinas internas de CRM/marketing.

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_vendas`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8084/actuator/health

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8084` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_vendas` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
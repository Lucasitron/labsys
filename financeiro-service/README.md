# financeiro-service

**Financeiro Service** do Sistema de Gestão FabLab. Gestão financeira completa:
fluxo de caixa, contas a pagar/receber, doações e recursos, compras (informativo),
cálculo de custo real de produção (Job Order Costing) e relatórios. Acesso
exclusivo do Admin.

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_financeiro`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8085/actuator/health

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8085` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_financeiro` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
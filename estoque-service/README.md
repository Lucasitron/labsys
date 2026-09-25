# estoque-service

**Estoque & Suprimentos Service** do Sistema de Gestão FabLab. Gestão de todo o
inventário: insumos, ferramentas e peças; entradas/saídas; empréstimos internos e
externos; fornecedores, preços e localização física; e a Lista de Materiais (BOM)
com baixa automática de estoque na produção.

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_estoque`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8083/actuator/health

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8083` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_estoque` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
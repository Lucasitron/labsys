# producao-service

**Produção & Projetos Service** do Sistema de Gestão FabLab. Gestão operacional:
projetos e tarefas, Kanban de encomendas com baixa automática de estoque, cadastro
e histórico de máquinas e o sistema completo de gestão 5S.

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_producao`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8086/actuator/health

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8086` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_producao` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
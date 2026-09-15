# rh-service

**Pessoas & RH Service** do Sistema de Gestão FabLab. Gestão de pessoas e ciclo
de vida no laboratório: processo seletivo, ponto (RFID), horas em encomendas e
projetos, treinamento (LMS) e evolução de níveis de acesso.

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_rh`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8082/actuator/health

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8082` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_rh` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
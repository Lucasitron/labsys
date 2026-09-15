# notification-service

**Notification Service** do Sistema de Gestão FabLab. Serviço transversal que
consome eventos de todos os microsserviços e dispara notificações (e-mail no MVP),
mantendo ativo e histórico de notificações com retenção de 90 dias.

## Responsabilidades

*   Consumir eventos (RabbitMQ) e enviar e-mails.
*   Estrutura pronta para WhatsApp (não implementado no MVP).
*   Revisão de histórico pelo Admin e expurgo automático após 90 dias.

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_notification`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8087/actuator/health

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8087` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_notification` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `SMTP_HOST` / `SMTP_PORT` | Servidor SMTP | `localhost` / `1025` |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
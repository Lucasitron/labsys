# Pessoas & RH Service

Microserviço responsável pela gestão de todas as pessoas do Fab Lab, incluindo professores, alunos (bolsistas, voluntários e estagiários) e candidatos ao processo seletivo. Controla o registro de horas, treinamentos (LMS) e a evolução dos níveis de acesso.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 15+
- RabbitMQ 3.12+

## Variáveis de Ambiente

| Variável | Descrição | Padrão |
|---|---|---|
| `PORT` | Porta HTTP | `8082` |
| `DB_URL` | JDBC URL do PostgreSQL | `jdbc:postgresql://localhost:5432/fablab_rh` |
| `DB_USERNAME` | Usuário do banco | `fablab` |
| `DB_PASSWORD` | Senha do banco | `fablab` |
| `JWT_SECRET` | Chave HMAC compartilhada com Auth Service (≥32 chars) | — |
| `JWT_ISSUER` | Emissor dos tokens JWT | `fablab` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Porta do RabbitMQ | `5672` |
| `RABBITMQ_USERNAME` | Usuário do RabbitMQ | `fablab` |
| `RABBITMQ_PASSWORD` | Senha do RabbitMQ | `fablab` |
| `EUREKA_URI` | URL do Discovery Service (Eureka) | `http://localhost:8761/eureka` |

## Rodar

```bash
export JWT_SECRET="sua-chave-secreta-min-32-caracteres-abc"
mvn spring-boot:run
```

O serviço sobe em `http://localhost:8082`.

## Testes

```bash
# Java 25 quebra Mockito — use Java 21:
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

mvn verify
```

Cobertura mínima: 80% (linha). Relatório em `target/site/jacoco/index.html`.

## Endpoints

Ver [API.md](API.md) para a especificação completa.

## Arquitetura

```
src/main/java/com/fablab/rh/
├── config/         # SecurityConfig, JwtService, RabbitMQ config
├── controller/     # REST controllers
├── dto/            # Request/Response records, events, principal
├── entity/         # JPA entities, enums, converters
├── exception/      # GlobalExceptionHandler, custom exceptions
├── mapper/         # Entity ↔ DTO mappers
├── repository/     # Spring Data JPA repositories
└── service/        # Business logic, RFID consumer, event publishers
```

## Integrações

- **Auth & Identity Service**: consome eventos RFID via RabbitMQ (`access.rfid.event`); valida JWT compartilhado via `JWT_SECRET`.
- **Financeiro Service**: publica `horas.validadas.event` quando apontamentos são validados.
- **Notification Service**: publica `nivel.alterado.event` quando o nível de acesso é alterado.

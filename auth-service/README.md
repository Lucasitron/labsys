# Auth & Identity Service

Microsserviço de autenticação, autorização (RBAC) e identidade do **Sistema de Gestão FabLab**.

Responsabilidades:

- Autenticação de usuários via e-mail/nome de usuário e senha.
- Validação de acesso físico via RFID (ESP32) e registro de ponto.
- Emissão, validação e renovação de tokens JWT (acesso 15 min / refresh 7 dias).
- Logout seguro com blacklist de tokens revogados.
- Matriz de permissões RBAC consumida pelos demais serviços.

## Tecnologias

| Camada | Tecnologia |
| :--- | :--- |
| Linguagem | Java 21 |
| Framework | Spring Boot 3.4.x (Web, Security, Data JPA, Validation, AMQP, Actuator) |
| Segurança | Spring Security + JWT (JJWT 0.12) + BCrypt |
| Banco de dados | PostgreSQL + Flyway |
| Mensageria | RabbitMQ (evento `access.rfid.event`) |
| Descoberta de serviços | Eureka |

## Pré-requisitos

- JDK 21
- Maven 3.9+
- PostgreSQL em execução
- RabbitMQ em execução
- Eureka Server em execução (para registro)

## Variáveis de ambiente

| Variável | Padrão | Descrição |
| :--- | :--- | :--- |
| `PORT` | `8081` | Porta HTTP do serviço |
| `DB_URL` | `jdbc:postgresql://localhost:5432/fablab_auth` | URL do banco de dados |
| `DB_USERNAME` | `fablab` | Usuário do banco |
| `DB_PASSWORD` | `fablab` | Senha do banco |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `RABBITMQ_USERNAME` | `fablab` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | `fablab` | Senha do RabbitMQ |
| `EUREKA_URI` | `http://localhost:8761/eureka` | URL do Eureka |
| `JWT_SECRET` | **obrigatório** | Chave HMAC de assinatura (mín. 32 caracteres) |
| `JWT_EXPIRATION_SECONDS` | `900` | Validade do token de acesso |
| `JWT_REFRESH_EXPIRATION_SECONDS` | `604800` | Validade do refresh token |
| `JWT_ISSUER` | `fablab` | Emissor dos tokens |

> **Importante:** `JWT_SECRET` não possui padrão. Defina uma chave forte (≥ 32 caracteres)
> e nunca a versione no repositório.

## Como executar

```bash
# 1. Suba a infraestrutura (PostgreSQL, RabbitMQ, Eureka)
docker compose -f infra/docker-compose.dev.yml up -d

# 2. Crie o schema do banco (se necessário)
export PGPASSWORD=fablab
psql -h localhost -U fablab -d fablab_auth -c "CREATE SCHEMA IF NOT EXISTS public;"

# 3. Defina o segredo JWT e execute
export JWT_SECRET="troque-por-uma-chave-forte-com-pelo-menos-32-caracteres"
mvn -pl auth-service spring-boot:run

# ou, com Maven direto no módulo:
cd auth-service
export JWT_SECRET="troque-por-uma-chave-forte-com-pelo-menos-32-caracteres"
mvn spring-boot:run
```

O serviço sobe em `http://localhost:8081`. As migrations Flyway criam as tabelas
`login`, `user_permissions`, `token_blacklist` e `access_log` automaticamente.

> Para testes locais com um usuário, insira as credenciais via SQL usando BCrypt
> (ex.: gerado pelo próprio serviço em `/auth/login` após registrar o registro).

## Como testar

```bash
cd auth-service
mvn test          # testes unitários, de integração e de segurança
mvn verify        # testes + relatório e validação de cobertura (mín. 80%)
```

A cobertura fica em `target/site/jacoco/index.html`.

### Suíte de testes

- **unit/** — `JwtService`, `AuthService`, `TokenBlacklistService`, `AccessLogService`,
  `RbacService` e os repositórios (`@DataJpaTest`).
- **integration/** — `/auth/login`, `/auth/validate-rfid`, rotação de tokens e blacklist,
  usando H2 (banco em memória) e RabbitMQ mockado.
- **security/** — rejeição de requisições anônimas, RBAC (Admin vs. não-Admin),
  hash de senha BCrypt e rejeição de tokens inválidos/revogados.

## Estrutura do projeto

```
auth-service/
├── docs/                    # API.md, BUGS.md, ACTION_PLAN.md, CHANGELOG.md
├── src/main/java/com/fablab/auth/
│   ├── config/              # Security, JWT, RabbitMQ, entry points
│   ├── controller/          # AuthController
│   ├── dto/                 # DTOs de requisição/resposta
│   ├── entity/              # Login, UserPermission, TokenBlacklist, AccessLog
│   ├── exception/           # Exceções customizadas + handler global
│   ├── mapper/              # Mapeamento Entity <-> DTO
│   ├── repository/          # Repositórios JPA
│   └── service/             # Regras de negócio e infra
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-testing.yml
│   └── db/migration/        # Migrations Flyway
├── pom.xml
└── README.md
```

## Documentação

- [API.md](docs/API.md) — especificação dos endpoints.
- [CHANGELOG.md](docs/CHANGELOG.md) — histórico de versões.
- [BUGS.md](docs/BUGS.md) — relatório de bugs.
- [ACTION_PLAN.md](docs/ACTION_PLAN.md) — planos de ação.
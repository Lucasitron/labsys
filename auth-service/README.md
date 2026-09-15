# auth-service

**Auth & Identity Service** do Sistema de Gestão FabLab. Autenticação de usuários
(e-mail/senha e RFID), emissão/validação de tokens JWT, RBAC e controle de sessão.

## Responsabilidades

*   Login/logout (apenas Admin edita permissões; verificação via `emailVer` descartada no MVP).
*   Validação de acesso físico via RFID (ESP32) e registro de ponto.
*   Fornecimento de identidade e permissões para os demais serviços.

> Detalhamento completo: [`docs/02`](../../docs/02-Pessoas%20&%20RH%20Service.md) e [`docs/01`](../../docs/01-Auth_&_Identity_Service.md).

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_auth`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8081/actuator/health

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8081` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_auth` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `JWT_SECRET` | Chave de assinatura do JWT | obrigatório |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
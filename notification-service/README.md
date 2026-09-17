# notification-service

**Notification Service** do Sistema de Gestão FabLab. Serviço transversal que
consome eventos de todos os microsserviços via RabbitMQ e dispara notificações
(e-mail no MVP), mantendo as notificações ativas até a revisão do Admin e o
histórico por 90 dias.

## Responsabilidades

*   Consumir eventos (RabbitMQ) e criar notificações para o destinatário do evento.
*   Enviar e-mail via SMTP (o WhatsApp tem estrutura pronta, mas é no-op no MVP).
*   Manter notificações ativas e histórico, com revisão pelo Admin e expurgo
    automático após o período de retenção (padrão 90 dias).

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_notification`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8087/actuator/health

## Como testar

```sh
mvn verify
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8087` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_notification` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | RabbitMQ | `localhost` / `5672` |
| `SMTP_HOST` / `SMTP_PORT` | Servidor SMTP | `localhost` / `1025` |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Credenciais SMTP | — (vazio) |
| `JWT_SECRET` | Segredo HMAC compartilhado com o Auth Service | — |
| `JWT_ISSUER` | Issuer esperado do JWT | `fablab` |
| `NOTIFICATION_EMAIL_FROM` | Remetente dos e-mails | `no-reply@fablab.local` |
| `NOTIFICATION_EMAIL_TO` | Destinatário padrão dos e-mails | `admin@fablab.local` |
| `NOTIFICATION_HISTORICO_RETENCAO_DIAS` | Dias de retenção do histórico | `90` |
| `NOTIFICATION_EXPURGO_SCHEDULER_ENABLED` | Liga o job de expurgo diário | `true` |
| `NOTIFICATION_EXPURGO_SCHEDULER_CRON` | Cron do job de expurgo | `0 0 3 * * *` |
| `NOTIFICATION_REENVIO_SCHEDULER_ENABLED` | Liga o job de reenvio de pendentes | `false` |
| `NOTIFICATION_REENVIO_SCHEDULER_CRON` | Cron do job de reenvio | `0 */30 * * *` |

## Controle de acesso

RBAC por `NivelAcesso` do JWT. Qualquer usuário autenticado lê e marca como
lida apenas as **suas** notificações. Operações de administração (listar todas,
revisar, consultar histórico, enviar teste e alterar configurações de canal) são
exclusivas do `ADMIN`. A autorização no nível do recurso é feita pelo
`AcessoService` (`@acesso`): `ADMIN` sempre pode; os demais apenas quando são o
destinatário.

## Premissas do MVP

*   **WhatsApp** é estrutural: `WhatsAppServiceNoop` apenas registra em log e
    mantém a notificação `PENDENTE`, sem envio.
*   **E-mail** usa remetente e destinatário padrão configurados
    (`notification.email.*`), pois o MVP não possui cadastro de e-mail por
    usuário.
*   O evento `access.rfid.event` do Auth Service é opcional e **não** é consumido
    nesta versão.
*   Eventos sem destinatário pessoal (estoque baixo, lançamento vencido, compra
    solicitada e kanban) geram notificações com destinatário nulo, visíveis
    apenas ao Admin.

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)

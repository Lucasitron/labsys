# producao-service

**Produção & Projetos Service** do Sistema de Gestão FabLab. Gerencia projetos e
tarefas, o Kanban de produção de encomendas (com baixa de estoque disparada na
entrega), máquinas e seu histórico de uso e o sistema 5S completo (setores,
checklists, sinalizações, inspeções, advertências, auditoria de mesas de projeto
e parâmetros configuráveis), incluindo geração de QR Code para totens.

## Pré-requisitos

*   Java 21, Maven 3.9+, PostgreSQL (`fablab_producao`) e RabbitMQ em execução.

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8086/actuator/health

## Como testar

```sh
mvn verify
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8086` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/fablab_producao` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `fablab` / `fablab` |
| `EUREKA_URI` | URL do Eureka | `http://localhost:8761/eureka` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `JWT_SECRET` | Segredo HMAC compartilhado com o Auth Service | — |
| `JWT_ISSUER` | Issuer esperado do JWT | `fablab` |
| `producao.auditoria.scheduler.enabled` | Liga o job de auditoria de mesas | `true` |
| `producao.auditoria.scheduler.cron` | Cron do job de auditoria | `0 0 6 * * *` |

## Controle de acesso

RBAC por `NivelAcesso` do JWT. Leitura: `ADMIN`, `BOLSISTA`, `VOLUNTARIO` e
`ESTAGIARIO`. Escrita: `ADMIN`, `BOLSISTA` e `VOLUNTARIO`, com verificação de
responsabilidade no serviço (`ADMIN` sempre pode; demais só quando são o
responsável pelo recurso). `RECRUTANDO` não possui acesso. Operações sensíveis
(registrar advertência, auditoria de mesa e alterar parâmetros 5S) são
exclusivas do `ADMIN`.

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)

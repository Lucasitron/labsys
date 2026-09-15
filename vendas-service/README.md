# vendas-service

Microsserviço de Vendas &amp; CRM do Fab Lab: cadastro de clientes (PF/PJ), orçamentos,
encomendas com Kanban de status, registro manual de vendas em marketplaces, interações
de CRM e tarefas de marketing.

## Stack

- Java 21 + Spring Boot 3.4 (Web, Data JPA, Security, Validation, AMQP, Actuator)
- PostgreSQL + Flyway (migrations em `src/main/resources/db/migration`)
- RabbitMQ (eventos comerciais e atualizações do Kanban)
- Autenticação via JWT do Auth &amp; Identity Service (chave HMAC compartilhada)
- Eureka client (Service Registry)
- JaCoCo (cobertura de linha ≥ 80% no `verify`)

## Como executar

```bash
export JWT_SECRET='<chave HMAC com mínimo 32 caracteres>'
mvn spring-boot:run
```

Porta padrão: `8084` (override com `PORT`). Banco: `fablab_vendas`.

## Estrutura

- `entity/` — entidades JPA e enums (`StatusOrcamento`, `StatusKanban`, `TipoPessoa`, etc.)
- `repository/` — repositórios Spring Data
- `service/` — regras de negócio, validação CPF/CNPJ e publicação de eventos
- `controller/` — REST (16+ endpoints, ver `docs/API.md`)
- `config/` — segurança JWT, RBAC e RabbitMQ
- `dto/` — contratos de entrada/saída e eventos de mensageria
- `util/` — `DocumentoValidator` (CPF/CNPJ)

## Testes

```bash
mvn clean verify   # 72 testes + JaCoCo ≥ 80%
```

Testes de integração usam H2 em modo PostgreSQL com RabbitMQ mockado.

## Fluxos principais

1. Cliente é cadastrado (CPF/CNPJ validado) e pode receber tags.
2. Orçamento é criado com itens; ajustes só são permitidos em `PENDENTE`/`AJUSTE`.
3. Orçamento aprovado vira encomenda em `FILA` no Kanban.
4. Cada movimentação do Kanban é auditada em `historico_status_encomenda`.
5. Entrega notifica o Financeiro (`encomenda.entregue.event`).
6. Vendas de marketplace são registradas manualmente com a taxa da plataforma.
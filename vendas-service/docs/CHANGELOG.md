<!-- Keep a Changelog: https://keepachangelog.com/pt-BR/1.1.0/ -->

## [Não publicado]

### Adicionado
- Scaffold inicial do microsserviço.
- Dependências: JPA, Flyway, AMQP e Eureka client.
- Entidades JPA: `Cliente`, `TagCliente`, `ClienteTag`, `Orcamento`, `ItemOrcamento`,
  `Encomenda`, `HistoricoStatusEncomenda`, `RegistroMarketplace`, `InteracaoCliente`,
  `TarefaMarketing` e enums de domínio.
- Migrations Flyway `V1`–`V10` (todas as tabelas do Vendas &amp; CRM).
- Segurança JWT (validação HMAC + issuer do Auth Service) e RBAC via `@PreAuthorize`.
- Tratamento de erros padronizado (`ErrorResponse` + `GlobalExceptionHandler`).
- Validação de CPF e CNPJ (`DocumentoValidator`).
- Repositórios, DTOs e mappers/views dos contratos REST.
- Services: clientes, orçamentos, encomendas (Kanban com controle otimista de
  concorrência e auditoria), marketplace, interações e tarefas de marketing.
- Controllers REST (clientes, tags, orçamentos, encomendas/kanban, marketplace,
  interações e tarefas-marketing).
- Mensageria RabbitMQ: `VendasEventPublisher` e consumo de `producao.status.event`.
- 45 testes (integração, security e unit) com JaCoCo ≥ 80% de cobertura de linha.
- Documentação: `README.md` e `docs/API.md`.

### Corrigido
- Adicionado construtor de conveniência em `ClienteTag` e `dataCadastro` obrigatório.
- Ajustado cálculo de dígitos verificadores de CPF/CNPJ.
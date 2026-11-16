# Plan: Vendas & CRM Service

## Arquitetura

Serviço Java 21 + Spring Boot com arquitetura em camadas, seguindo os padrões dos demais serviços do FabLab:

```
vendas-service (porta 8084 / ${PORT:8084})
├── Controller (REST, PT-BR) ── /clientes, /orcamentos, /encomendas, /historico-status, /marketplace, /interacoes, /tarefas-marketing, /tags
├── Service (regras de negócio: aprovação de orçamento, kanban + histórico, nova ordem, locks de concorrência)
├── Repository (Spring Data JPA)
├── Entity + DTO (Bean Validation, CPF/CNPJ)
├── Migration (Flyway V1..V10)
└── Rabbit (exchange fablab.vendas: publicadores encomenda.criada.event, encomenda.status.alterado.event, orcamento.aprovado.event; consumidor producao.status.alterado.event)
```

- **Persistência:** PostgreSQL `fablab_vendas` (fablab/fablab), schema gerenciado por Flyway (migrations V1..V10 na ordem): `cliente`, `tag_cliente`, `cliente_tag`, `orcamento`, `item_orcamento`, `encomenda`, `historico_status_encomenda`, `registro_marketplace`, `interacao_cliente`, `tarefa_marketing`.
- **Descoberta:** registro no Eureka em `http://localhost:8761/eureka` sob o nome `fablab/vendas-service`.
- **Mensageria:** RabbitMQ em `5672`; o serviço **publica** eventos na exchange `fablab.vendas` (padrão `<contexto>.<entidade>.<acao>.event`) e **consome** eventos de Kanban do Produção e (opcional) de consumo de estoque.
- **Concorrência:** `@Version`/locks otimistas sobre `encomenda.status_kanban` para impedir movimentações simultâneas conflitantes; validação de orçamento aprovado antes de criar encomenda no mesmo `@Transactional`.

## Dependências

- **Build/CI:** Maven (Spring Boot 3.x, Java 21).
- **Spring Starters:** `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security` (JWT/resource server conforme padrão Auth), `spring-boot-starter-amqp`.
- **Banco/Migração:** `org.postgresql:postgresql`, `org.flywaydb:flyway-core`, `flyway-database-postgresql`.
- **Descoberta:** `spring-cloud-starter-netflix-eureka-client`.
- **Gateway/Auth:** validação de token JWT via claims `id_user`, `role`, `setor` (consistente com Auth & Identity).
- **Testes:** `spring-boot-starter-test`, `spring-rabbit-test`, H2/testcontainers Postgres para testes de integração.
- **Infra externa (Docker Compose):** PostgreSQL `fablab_vendas`, RabbitMQ 5672, Eureka 8761.

## Ordem de Implementação

1. **Setup do projeto** — boot do serviço Spring Boot na porta `8084` (${PORT:8084}), contexto `spring.application.name=fablab/vendas-service`, config Eureka, RabbitMQ, PostgreSQL `fablab_vendas`, Flyway.
2. **Migrations e entidades (V1..V10)** — schema na ordem de dependência (`cliente` → `tag_cliente` → `cliente_tag` → `orcamento` → `item_orcamento` → `encomenda` → `historico_status_encomenda` → `registro_marketplace` → `interacao_cliente` → `tarefa_marketing`) e entidades JPA espelhando colunas/relacionamentos.
3. **Repositórios** — Spring Data JPA com consultas de filtro (clientes por nome/tags/tipo, encomendas por status/cliente/data, histórico por encomenda, tarefas por responsável/status) e locks de concorrência no Kanban.
4. **Services de domínio** — ClienteService (CPF/CNPJ), OrcamentoService (criação/aprovação/ajustes + `orcamento.aprovado.event`), EncomendaService (criação a partir de orçamento aprovado, nova ordem por alteração, Kanban + histórico + eventos), MarketplaceService, InteracaoService, TarefaMarketingService, TagService.
5. **RabbitMQ — configuração e publicadores/consumidores** — exchange `fablab.vendas`, filas/bindings de `encomenda.criada.event`, `encomenda.status.alterado.event`, `orcamento.aprovado.event`; consumidor de `producao.status.alterado.event`.
6. **Controllers REST** — endpoints PT-BR com Bean Validation e RBAC (`@PreAuthorize`) conforme matriz.
7. **Testes** — unitários (regras de aprovação/ajuste, Kanban/histórico, nova ordem, CPF/CNPJ), de integração (controllers, Rabbit) e de segurança (RBAC/JWT).
8. **Documentação** — revisão do serviço contra `docs/_archive/2026-10-19/04-Vendas_&_CRM_Services.md` e fechamento desta spec.

## Riscos e Mitigações

| Risco | Mitigação |
|---|---|
| Encomenda criada sem orçamento aprovado | Validação de `status = Aprovado` no service antes de persistir, no mesmo `@Transactional` |
| Movimentação concorrente no Kanban corrompe o cartão | `@Version`/locks otimistas sobre `status_kanban`; validação de transição válida |
| Evento perdido entre Vendas e Financeiro/Produção | Publicadores com confirmação (`publisher-confirm`); retry e DLQ configurados |
| Duplicação de encomenda a partir do mesmo orçamento | Regra de negócio: um orçamento gera uma única encomenda ativa (verificação por `id_orcamento`) |
| Alteração de encomenda perde rastreabilidade | Encerrar a atual registrando no histórico e criar nova ordem referenciando a anterior |
| CPF/CNPJ inválido ou duplicado no cadastro | Bean Validation + digest/validação de documento; índice único em `cpf_cnpj` |
| Perda de evento `producao.status.alterado.event` | Consumidor idempotente por `id_encomenda`; retry/DLQ |
| Inconsistência entre Kanban local e Produção | Sincronização bidirecional via eventos `encomenda.status.alterado.event` / `producao.status.alterado.event` |

## Integrações e Eventos

| Evento | Exchange (padrão FabLab) | Direção | Acionado por |
|---|---|---|---|
| `encomenda.criada.event` | `fablab.vendas` → Produção | Publica | Criação de encomenda a partir de orçamento aprovado |
| `encomenda.status.alterado.event` | `fablab.vendas` → Produção + Notification | Publica | Movimentação no Kanban / atualização de status |
| `orcamento.aprovado.event` | `fablab.vendas` → Financeiro | Publica | Aprovação formal de orçamento |
| `producao.status.alterado.event` | `fablab.producao` → Vendas | Consome | Sincroniza `status_kanban` da encomenda |
| `estoque.consumo.realizado.event` | `fablab.estoque` → Vendas (opcional) | Consome | Associa custo real de consumo à encomenda |

Serviços envolvidos: Produção & Projetos (criação/Kanban), Financeiro (orçamento aprovado, recebimentos, taxas), Estoque & Suprimentos (consumo opcional), Notification (status e alertas), RH (responsáveis/validação de permissões), Auth & Identity (JWT/claims), Eureka (descoberta), RabbitMQ (mensageria).

## Dependências Externas

- Aplicação dos eventos consumidos por Financeiro/Produção/Notification é responsabilidade dos respectivos serviços (fora do escopo do Vendas).
- Cliente, funcionário e produto/serviço são referenciados apenas por ID (`id_cliente`, `id_usuario`, `id_responsavel`, `id_produto_servico`) — validação via RH e Vendas quando disponível.
- Nenhuma dependência de acesso externo do cliente; operação 100% interna, todo endpoint exige autenticação.
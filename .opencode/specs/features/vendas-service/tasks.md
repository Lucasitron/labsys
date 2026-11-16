# Tasks: Vendas & CRM Service

Base: `src/main/java/com/fablab/vendas/` | Recursos em PT-BR | Tabelas/colunas conforme doc (`cliente`, `orcamento`, `encomenda`, etc.) | Porta `8084` (${PORT:8084}).

## task-001 · Setup do Projeto

- **Escopo:** raiz do projeto (pom.xml, `application.yml`, `FablabVendasApplication.java`)
- **Entradas:** Java 21 + Spring Boot; portas padrão do FabLab (Eureka 8761, RabbitMQ 5672)
- **Saídas:** serviço bootável na porta `8084`; DB PostgreSQL `fablab_vendas` (fablab/fablab); Flyway habilitado; registro no Eureka

Critérios:
- [ ] `pom.xml` com dependências web, data-jpa, validation, security, amqp, eureka-client, postgres, flyway
- [ ] `application.yml` configurado (porta `8084` via `${PORT:8084}`, datasource `fablab_vendas`, Eureka, RabbitMQ exchange `fablab.vendas`)
- [ ] `@SpringBootApplication` `FablabVendasApplication` criado
- [ ] App sobe sem erros; conecta ao banco, ao Eureka e valida o token JWT do Auth & Identity
- [ ] Pacotes base `com.fablab.vendas.{controller,service,repository,entity,config,model,dto,rabbit}` criados

**Dependências:** nenhuma (primeira).

## task-002 · Migrations Flyway V1..V10 e Entidades

- **Escopo:** `src/main/resources/db/migration/` e `com/fablab/vendas/entity/`
- **Entradas:** modelo de dados da doc (cliente, tag_cliente, cliente_tag, orcamento, item_orcamento, encomenda, historico_status_encomenda, registro_marketplace, interacao_cliente, tarefa_marketing)
- **Saídas:** schema V1..V10 aplicável + entidades JPA espelhando colunas/relacionamentos

Critérios:
- [ ] **V1 `cliente`**: `id_cliente` (Serial PK), `tipo_pessoa`, `nome_razao_social`, `cpf_cnpj` (único), `email`, `telefone`, `endereco`, `data_cadastro`
- [ ] **V2 `tag_cliente`**: `id_tag` (PK), `nome`, `cor`
- [ ] **V3 `cliente_tag`**: `id_cliente_tag` (PK), `id_cliente` (FK → `cliente`), `id_tag` (FK → `tag_cliente`), unicidade (`id_cliente`, `id_tag`)
- [ ] **V4 `orcamento`**: `id_orcamento` (PK), `id_cliente` (FK), `data_criacao`, `validade`, `valor_total`, `status`, `observacoes`
- [ ] **V5 `item_orcamento`**: `id_item_orcamento` (PK), `id_orcamento` (FK), `descricao`, `quantidade`, `valor_unitario`
- [ ] **V6 `encomenda`**: `id_encomenda` (PK), `id_orcamento` (FK opcional), `id_cliente` (FK), `data_criacao`, `data_previsao_entrega`, `status_kanban`, `valor_final`, `observacoes`
- [ ] **V7 `historico_status_encomenda`**: `id_historico` (PK), `id_encomenda` (FK), `status_anterior`, `status_novo`, `data_alteracao` (Timestamp), `id_usuario`, `observacao`
- [ ] **V8 `registro_marketplace`**: `id_registro` (PK), `id_encomenda` (FK), `plataforma`, `codigo_externo`, `data_venda`, `valor_taxa`
- [ ] **V9 `interacao_cliente`**: `id_interacao` (PK), `id_cliente` (FK), `data_interacao` (Timestamp), `tipo`, `descricao`, `id_usuario`
- [ ] **V10 `tarefa_marketing`**: `id_tarefa` (PK), `titulo`, `descricao`, `id_responsavel`, `data_inicio`, `data_fim`, `status`, `prioridade`
- [ ] Entidade `Cliente` com `@Version` quando necessário e mapa de tags N:N (via `cliente_tag`)
- [ ] Demais entidades (TagCliente, ClienteTag, Orcamento, ItemOrcamento, Encomenda, HistoricoStatusEncomenda, RegistroMarketplace, InteracaoCliente, TarefaMarketing) mapeadas
- [ ] Migrations executam clean + `flyway migrate` sem erro no PostgreSQL `fablab_vendas`

**Dependências:** task-001.

## task-003 · Repositórios

- **Escopo:** `com/fablab/vendas/repository/`
- **Entradas:** entidades do task-002
- **Saídas:** interfaces Spring Data JPA com consultas derivadas/`@Query`

Critérios:
- [ ] `ClienteRepository`: por nome (`nome_razao_social`), por tipo, por `cpf_cnpj` (único), por tag, `findByIdWithLock` (`@Lock(PESSIMISTIC_WRITE)` quando aplicável)
- [ ] `OrcamentoRepository`: por cliente, por status; verificação de orçamento Aprovado antes de criar encomenda
- [ ] `EncomendaRepository`: filtros por status/cliente/data, busca ativa por `id_orcamento`, `findByIdEager` com histórico
- [ ] `HistoricoStatusEncomendaRepository`: por `id_encomenda` ordenado por `data_alteracao`
- [ ] `RegistroMarketplaceRepository`: por encomenda, por plataforma, por `codigo_externo`
- [ ] `InteracaoClienteRepository`: por `id_cliente`; `TarefaMarketingRepository`: por responsável/status
- [ ] `TagClienteRepository`, `ClienteTagRepository` (tags de um cliente)
- [ ] Testes de integração de repositório passam

**Dependências:** task-002.

## task-004 · OrcamentoService

- **Escopo:** `com/fablab/vendas/service/` e `com/fablab/vendas/dto/`
- **Entradas:** repositórios (task-003), DTOs com Bean Validation
- **Saídas:** CRUD de orçamentos com itens, fluxo de ajustes, aprovação e publicação de `orcamento.aprovado.event`

Critérios:
- [ ] `OrcamentoRequest/Response` e `ItemOrcamentoDto` com validações (`@NotBlank`, `@Positive` em quantidades/valores, enums `status`, datas coerentes)
- [ ] Criação de orçamento vinculado a cliente com `valor_total` calculado a partir dos itens
- [ ] `PUT /orcamentos/{id}` aplica ajustes: itens/valores editáveis; status muda para `Ajuste` antes de reenvio
- [ ] Aprovação formal muda status para `Aprovado`, persiste `data_criacao`/`validade` e publica `orcamento.aprovado.event`
- [ ] Recusa muda status para `Recusado` (sem conversão em encomenda)
- [ ] Bloqueio de edição de orçamento já Aprovado e convertido (uma encomenda ativa por orçamento)
- [ ] Testes unitários das regras de ajuste/aprovação passam

**Dependências:** task-001, task-003.

## task-005 · EncomendaService com Kanban, Histórico e Eventos

- **Escopo:** `com/fablab/vendas/service/`, `com/fablab/vendas/config/` e `com/fablab/vendas/rabbit/`
- **Entradas:** repositórios (task-003), publicadores (config Rabbit `fablab.vendas`)
- **Saídas:** ciclo de vida da encomenda (criação a partir de orçamento aprovado, nova ordem por alteração), movimentação de Kanban com histórico, eventos de criação e status

Critérios:
- [ ] `RabbitMqConfig`: exchange `fablab.vendas`, filas/bindings perenes para `encomenda.criada.event`, `encomenda.status.alterado.event`, `orcamento.aprovado.event`
- [ ] `EncomendaRequest/Response` com validações (datas de previsão, valores, status Kanban)
- [ ] Criação de encomenda somente com `orcamento.status = Aprovado` (mesmo `@Transactional`); nasce com `status_kanban = Fila`
- [ ] Publica `encomenda.criada.event` (`{ idEncomenda, idCliente, valorFinal, dataCriacao }`) para o Produção
- [ ] `PUT /encomendas/{id}/kanban` valida transição, aplica lock otimista (`@Version`/`@Lock`), grava `historico_status_encomenda` (status_anterior, status_novo, data_alteracao, id_usuario, observacao) e publica `encomenda.status.alterado.event`
- [ ] Ao mover para `Entregue`, prepara dados para o Financeiro (recebimento)
- [ ] **Alteração de encomenda**: encerra a atual (status final no histórico) e cria **nova ordem** com novas estimativas e valor, referenciando a anterior
- [ ] Consumidor de `producao.status.alterado.event` atualiza `status_kanban` de forma idempotente (por `id_encomenda`, retry/DLQ)
- [ ] Testes unitários e de integração (Kanban, histórico, nova ordem, eventos publicados) passam

**Dependências:** task-001, task-003, task-004.

## task-006 · MarketplaceService

- **Escopo:** `com/fablab/vendas/service/` e `com/fablab/vendas/dto/`
- **Entradas:** repositórios (task-003), EncomendaService (task-005)
- **Saídas:** registro manual de vendas externas vinculadas a encomendas

Critérios:
- [ ] `MarketplaceRequest/Response` com validações (`@NotBlank` plataforma/código externo, `@Positive` taxa)
- [ ] `POST /marketplace` vincula registro a uma encomenda existente com `plataforma`, `codigo_externo`, `data_venda`, `valor_taxa`
- [ ] `GET /marketplace` com filtros (encomenda, plataforma)
- [ ] Código externo único por plataforma (evita duplicação de pedido importado)
- [ ] Dados de taxa disponibilizados para cálculo de lucro líquido (Financeiro)
- [ ] Testes unitários passam

**Dependências:** task-005.

## task-007 · InteracaoService e TarefaMarketingService

- **Escopo:** `com/fablab/vendas/service/` e `com/fablab/vendas/dto/`
- **Entradas:** repositórios (task-003), Notification via eventos
- **Saídas:** CRM interno (interações de clientes) e agenda de marketing

Critérios:
- [ ] `InteracaoRequest/Response` com validações (enum `tipo`, descrição, `id_usuario` responsável)
- [ ] `POST /interacoes` registra interação (`data_interacao`, `tipo`, `descricao`); `GET /interacoes/{id_cliente}` lista histórico
- [ ] `TarefaMarketingRequest/Response` com validações (enums `status`/`prioridade`, datas coerentes, `id_responsavel`)
- [ ] `POST /tarefas-marketing`, `GET /tarefas-marketing` (filtros responsável/status), `PUT /tarefas-marketing/{id}` (status/prioridade)
- [ ] Mudanças de prazos/status geram alerta via Notification (evento padrão do serviço quando aplicável)
- [ ] Testes unitários passam

**Dependências:** task-003.

## task-008 · TagService

- **Escopo:** `com/fablab/vendas/service/` e `com/fablab/vendas/dto/`
- **Entradas:** repositórios (task-003), ClienteService
- **Saídas:** cadastro de tags e vinculo N:N com clientes

Critérios:
- [ ] `TagRequest/Response` com validações (`@NotBlank` nome, cor)
- [ ] `POST /tags` cadastra `tag_cliente`; `GET /tags` lista disponíveis
- [ ] Vínculo/desvínculo de tags a cliente via `cliente_tag` (sem duplicidade)
- [ ] Filtro de clientes por tag (usado no `GET /clientes`)
- [ ] Testes unitários passam

**Dependências:** task-003.

## task-009 · Controllers REST

- **Escopo:** `com/fablab/vendas/controller/`
- **Entradas:** services (task-004..008), DTOs, matriz RBAC
- **Saídas:** endpoints PT-BR com Bean Validation e `@PreAuthorize`

Critérios:
- [ ] `/clientes`: `POST`, `GET` (filtros nome/tags/tipo), `GET /{id}`, `PUT /{id}`, `DELETE /{id}`
- [ ] `/orcamentos`: `POST`, `PUT /{id}` (ajustes)
- [ ] `/encomendas`: `POST`, `GET`, `PUT /{id}/kanban`, `GET /{id}/status`
- [ ] `/historico-status/{id_encomenda}`: histórico de movimentações
- [ ] `/marketplace`: `POST`, `GET`
- [ ] `/interacoes`: `POST`, `GET /{id_cliente}`
- [ ] `/tarefas-marketing`: `POST`, `GET`, `PUT /{id}`
- [ ] `/tags`: `POST`, `GET`
- [ ] `@PreAuthorize` aplicado conforme matriz RBAC (Admin/Bolsista/Voluntário editam; Estagiários visualizam; Recrutandos sem acesso)
- [ ] Respostas de erro (`@RestControllerAdvice`) padronizadas em PT-BR com status `409/422` (ex.: orçamento não aprovado, transição Kanban inválida, CPF duplicado)
- [ ] Testes de integração dos endpoints passam

**Dependências:** task-004, task-005, task-006, task-007, task-008.

## task-010 · Testes (Unitários, Integração e Segurança)

- **Escopo:** `src/test/java/com/fablab/vendas/` e build
- **Entradas:** todos os fluxos implementados (task-001..009)
- **Saídas:** suíte verde cobrindo regras de negócio, mensageria e RBAC

Critérios:
- [ ] **Unitários:** validação de CPF/CNPJ e unicidade; ajuste/aprovação de orçamento; criação de encomenda bloqueada sem orçamento Aprovado; nova ordem por alteração; transições válidas de Kanban
- [ ] **Unitários:** cálculo de `valor_total` do orçamento a partir dos itens; taxa de marketplace; vínculo único de `cliente_tag`; regras de prioridade/status de tarefas
- [ ] **Integração (Rabbit):** `encomenda.criada.event`, `encomenda.status.alterado.event` e `orcamento.aprovado.event` publicados na exchange `fablab.vendas`; consumo idempotente de `producao.status.alterado.event` atualizando `status_kanban`
- [ ] **Integração (DB):** migrations V1..V10 aplicam em banco limpo; histórico persistido a cada movimentação
- [ ] **Segurança/RBAC:** endpoints protegidos sem token → `401`; requests com claims de Estagiário não alteram dados; Recrutando sem acesso
- [ ] **Concorrência:** movimentações simultâneas no Kanban não corrompem `status_kanban` (locks atuam)
- [ ] `mvn test` (e build) sem erros; smoke test na porta `8084` registrado no Eureka

**Dependências:** task-009 (e todas as anteriores).

## task-011 · Documentação e Validação Final

- **Escopo:** docs do serviço e revisão contra `docs/_archive/2026-10-19/04-Vendas_&_CRM_Services.md`
- **Entradas:** implementação (task-001..010) e spec (requirements/plan/tasks)
- **Saídas:** serviço documentado e alinhado à doc estabelecida

Critérios:
- [ ] Revisar endpoints, campos e eventos contra `docs/_archive/2026-10-19/04-Vendas_&_CRM_Services.md` (prevalece a doc em caso de divergência)
- [ ] Documentar payloads dos eventos e filas/bindings do RabbitMQ (`fablab.vendas`)
- [ ] Registrar decisões (nova ordem por alteração, unicidade de orçamento→encomenda, validação CPF/CNPJ)
- [ ] Atualizar este spec (requirements/plan/tasks) refletindo o que foi implementado
- [ ] Git flow: branch `feature/vendas-service` com commits atômicos conforme `docs/GITFLOW.md`

**Dependências:** task-010.

## Resumo de Dependências

```
task-001 → task-002 → task-003 ─┬─→ task-004 → task-005 ─┬─→ task-006
                                │                        └─→ task-007 ─┐
                                └─→ task-008 ──────────────────────────┤
                                                                        ├→ task-009 → task-010 → task-011
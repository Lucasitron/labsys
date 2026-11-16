# Tasks: Estoque & Suprimentos Service

Base: `src/main/java/com/fablab/estoque/` | Recursos em PT-BR | Tabelas/colunas conforme doc (`id_item`, `id_entrada`, etc.).

## 1. Setup do Projeto

- **Escopo:** raiz do projeto (pom.xml, `application.yml`, `FablabEstoqueApplication.java`)
- **Entradas:** Java 21 + Spring Boot; portas padrão do FabLab (Eureka 8761, RabbitMQ 5672)
- **Saídas:** serviço bootável na porta `8083`; DB PostgreSQL `fablab_estoque` (fablab/fablab); Flyway habilitado; registro no Eureka

Critérios:
- [x] `pom.xml` com dependências web, data-jpa, validation, flyway, amqp, eureka-client, postgres
- [x] `application.yml` configurado (porta `8083`, datasource `fablab_estoque`, Eureka, RabbitMQ, scheduler flag)
- [x] `@SpringBootApplication` `FablabEstoqueApplication` criado
- [x] App sobe sem erros; conecta ao banco e ao Eureka
- [x] Pacotes base `com.fablab.estoque.{controller,service,repository,entity,config,model,dto,rabbit}` criados

**Dependências:** nenhuma (primeira).

## 2. Migrations Flyway V1..V8 e Entidades

- **Escopo:** `src/main/resources/db/migration/` e `com/fablab/estoque/entity/`
- **Entradas:** modelo de dados da doc (localizacao, item, fornecedor, entrada_estoque, saida_estoque, emprestimo, lista_materiais, item_bom)
- **Saídas:** schema V1..V8 aplicável + entidades JPA espelhando colunas/relacionamentos

Critérios:
- [x] **V1 `localizacao`**: `id_localizacao` (Serial PK), `armario`, `prateleira`, `caixa`, `descricao`
- [x] **V2 `item`**: `id_item` (PK), `nome`, `descricao`, `categoria`, `unidade_medida`, `quantidade_atual`, `estoque_minimo`, `localizacao_id` (FK → `localizacao`)
- [x] **V3 `fornecedor`**: `id_fornecedor` (PK), `nome`, `contato`, `cnpj`
- [x] **V4 `entrada_estoque`**: `id_entrada` (PK), `id_item` (FK), `id_fornecedor` (FK), `quantidade`, `valor_unitario`, `valor_total`, `data_entrada`, `nota_fiscal`, `observacao`
- [x] **V5 `saida_estoque`**: `id_saida` (PK), `id_item` (FK), `quantidade`, `tipo_saida`, `id_referencia`, `data_saida`, `observacao`
- [x] **V6 `emprestimo`**: `id_emprestimo` (PK), `id_item`, `id_pessoa`, `quantidade`, `data_emprestimo`, `data_devolucao_prevista`, `data_devolucao_real`, `status`, `observacao`
- [x] **V7 `lista_materiais` (BOM)**: `id_bom` (PK), `id_produto_servico`, `nome`, `versao`, `editavel`
- [x] **V8 `item_bom`**: `id_item_bom` (PK), `id_bom` (FK), `id_item` (FK), `quantidade_prevista`, `quantidade_real`
- [x] Entidade `Item` com `@Version` (id_item, nome, descricao, categoria, unidade_medida, quantidade_atual, estoque_minimo, localizacao_id)
- [x] Demais entidades (Localizacao, Fornecedor, EntradaEstoque, SaidaEstoque, Emprestimo, ListaMateriais, ItemBom) mapeadas
- [x] Migrations executam clean + `flyway migrate` sem erro

**Dependências:** T1.

## 3. Repositórios

- **Escopo:** `com/fablab/estoque/repository/`
- **Entradas:** entidades do passo 2
- **Saídas:** interfaces Spring Data JPA com consultas derivadas/`@Query`

Critérios:
- [x] `ItemRepository`: por categoria, por `localizacao_id`, estoque baixo (`quantidade_atual <= estoque_minimo`), `findByIdWithLock` com `@Lock(PESSIMISTIC_WRITE)`
- [x] `EntradaEstoqueRepository`, `SaidaEstoqueRepository` (por item, por `id_referencia`)
- [x] `EmprestimoRepository`: `findByStatusAndDataDevolucaoPrevistaBefore` / listagem de atrasados
- [x] `FornecedorRepository`, `LocalizacaoRepository` (listagem ordenada)
- [x] `ListaMateriaisRepository` (por `id_produto_servico`, por versão), `ItemBomRepository` (itens de uma BOM)
- [x] Testes de integração de repositório passam

**Dependências:** T2.

## 4. Configuração de DTOs e Validação

- **Escopo:** `com/fablab/estoque/dto/`
- **Entradas:** endpoints/doc; Bean Validation
- **Saídas:** DTOs de entrada/saída com anotações de validação em PT-BR

Critérios:
- [x] DTOs: `ItemRequest/Response`, `EntradaRequest/Response`, `SaidaRequest/Response`, `EmprestimoRequest/Response`, `FornecedorRequest/Response`, `BomRequest/Response`, `ItemBomDto`, `LocalizacaoDto`
- [x] Validações: `@NotBlank` (nomes), `@NotNull`/`@Positive` (quantidades, valores), enums `categoria`/`tipo_saida`/`status`, datas coerentes (devolução prevista futura)
- [x] Mensagens de erro em PT-BR

**Dependências:** T2.

## 5. Services de Domínio

- **Escopo:** `com/fablab/estoque/service/`
- **Entradas:** repositórios (T3), DTOs (T4)
- **Saídas:** serviços com regras de negócio, locks de concorrência e disparo de eventos

Critérios:
- [x] `ItemService`: CRUD + filtros; atualização de `quantidade_atual` via lock; mudança de localização
- [x] `EntradaService`: registra `entrada_estoque`, soma saldo no mesmo `@Transactional`, calcula `valor_total`, dispara (via publicador) eventos de compra/alerta
- [x] `SaidaService`: valida saldo disponível (nunca negativo), subtrai com lock, registra `saida_estoque`, dispara `estoque.baixo.event`
- [x] `EmprestimoService`: criação, devolução, listagem de atrasados; saldo reduzido com lock; status Atrasado quando `data_devolucao_prevista` vencida e sem devolução
- [x] `BomService`: CRUD de BOM com incremento de `versao`; `editavel` durante produção; consumo → `quantidade_real`, baixa de saldo e registro em `saida_estoque` (`tipo_saida = Consumo`, `id_referencia`)
- [x] `FornecedorService`, `LocalizacaoService`: CRUD simples
- [x] Fator de acionamento de `estoque.baixo.event` na entrada/saída/consumo quando `quantidade_atual <= estoque_minimo`
- [x] Testes unitários de regras (saldo negativo rejeitado; concorrência; atraso) passam

**Dependências:** T3, T4, T6 (eventos), T7 (scheduler).

## 6. RabbitMQ — Configuração e Publicadores

- **Escopo:** `com/fablab/estoque/config/` e `com/fablab/estoque/rabbit/`
- **Entradas:** padrão de mensageria do FabLab (exchange `fablab.*`, RabbitMQ 5672)
- **Saídas:** exchanges/queues configuradas; publicadores e listener de baixa de produção

Critérios:
- [x] Config de RabbitMQ (connection factory, `Jackson2JsonMessageConverter`, queues/exchanges/bindings)
- [x] `EventPublisher` com `convertAndSend` para `estoque.baixo.event`, `emprestimo.atrasado.event`, `compra.solicitada.event`
- [x] Listener/consumer da baixa automática de consumo vinda do Produção (idempotência por `id_referencia`)
- [x] Publicadores usados em EntradaService (compra), SaidaService/BomService (baixo), EmprestimoService (atrasado)
- [x] Indicadores de mensagem publicada (publisher confirm) e retry/DLQ configurados
- [x] Testes de integração confirmam eventos publicados na fila

**Dependências:** T1, T5.

## 7. Scheduler de Empréstimos Atrasados

- **Escopo:** `com/fablab/estoque/scheduler/` (ou `config/`)
- **Entradas:** regra de atraso (T5), publicador (T6)
- **Saídas:** job diário `@Scheduled(cron="0 0 3 * * *")` (03:00)

Critérios:
- [x] Job executa às `0 0 3 * * *` e marca `status = Atrasado` nos vencidos sem devolução
- [x] Publica `emprestimo.atrasado.event` por empréstimo atrasado
- [x] Ativável/desativável e cron configurável por properties (`estoque.alerta.emprestimo.scheduler.enabled` / `.cron`)
- [x] Execução manual/testável sem esperar 03:00 (método público chamável)

**Dependências:** T6.

## 8. Controllers REST

- **Escopo:** `com/fablab/estoque/controller/`
- **Entradas:** services (T5), DTOs (T4), matriz RBAC
- **Saídas:** endpoints PT-BR com Bean Validation e `@PreAuthorize`

Critérios:
- [x] `/itens`: `POST`, `GET` (filtros categoria/localização/estoque baixo), `GET /{id}`, `PUT /{id}`
- [x] `/entradas`: `POST`
- [x] `/saidas`: `POST`
- [x] `/emprestimos`: `POST`, `PUT /{id}/devolucao`, `GET /atrasados`
- [x] `/fornecedores`: `POST`, `GET`
- [x] `/localizacoes`: `GET`
- [x] `/boms`: `POST`, `GET /{id}`, `PUT /{id}`, `POST /{id}/consumo`
- [x] Import/export CSV de itens (upload/download em `/itens/import` e `/itens/export`)
- [x] `@PreAuthorize` aplicado conforme matriz RBAC (Admin/Bolsista editam; demais visualizam)
- [x] Respostas de erro (`@RestControllerAdvice`) padronizadas em PT-BR com status `409/422` p/ saldo insuficiente
- [x] Testes de integração dos endpoints passam

**Dependências:** T5, T6.

## 9. Testes de Integração e Validação Final

- **Escopo:** `src/test/java/com/fablab/estoque/` e build
- **Entradas:** todos os fluxos implementados (T1..T8)
- **Saídas:** suíte verde e serviço validado contra a doc

Critérios:
- [x] Fluxo completo de entrada → saída → saldo sem negativo (com concorrência simulada)
- [x] Fluxo de empréstimo → devolução → atraso → `emprestimo.atrasado.event`
- [x] Fluxo de BOM → consumo → baixa automática → `saida_estoque` com `tipo_saida = Consumo`
- [x] `estoque.baixo.event` emitido quando `quantidade_atual <= estoque_minimo`
- [x] CRUD de itens/fornecedores/localizações/BOM coberto
- [x] Import/export CSV validado
- [x] RBAC aplicado nos endpoints protegidos
- [x] `mvn test` (e build) sem erros; smoke test na porta `8083` registrado no Eureka

**Dependências:** T8 (e todas as anteriores).

## Resumo de Dependências

```
T1 → T2 → T3 → T4 ─┐
                    ├→ T5 → T6 → T7 ─┐
                    └────────────────┘→ T8 → T9
```
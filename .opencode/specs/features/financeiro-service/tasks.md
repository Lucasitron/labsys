# Tasks: Financeiro Service

Tarefas atômicas para implementação e entrega do `financeiro-service`. Critérios marcados como `[ ]` devem virar `[x]` conforme concluídos. Referência técnica: `requirements.md` e `plan.md`; fonte canônica de negócio: `docs/_archive/2026-10-19/05-Financeiro_Service.md`.

---

## task-001 — Setup do projeto (Spring Boot, config e build)

- **Escopo:** Criar a estrutura base Maven do serviço: `pom.xml` (Java 21, dependencies Spring Web, Data JPA, Security + jjwt, AMQP, Cloud Eureka, Flyway, Validation, Actuator, testes), classe `FinanceiroApplication`, e `application.yml` com as variáveis de ambiente (`PORT=8085`, `DB_URL`, `DB_USERNAME/DB_PASSWORD=fablab`, `EUREKA_URI`, `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`), `ddl-auto: validate`, Flyway habilitado e exposição do Actuator.
- **Entradas:** `docs/_archive/2026-10-19/05-Financeiro_Service.md`; padrão dos serviços irmãos (auth/estoque/rh).
- **Saídas:** Serviço que inicia em `:8085`, registra no Eureka (`:8761`) e expõe `/actuator/health` sem autenticação.
- **Critérios:**
  - [ ] `mvn spring-boot:run` sobe a aplicação sem erros.
  - [ ] `/actuator/health` retorna `UP` sem JWT.
  - [ ] Instância registrada no Eureka `http://localhost:8761/eureka`.
- **Dependências:** —

## task-002 — Migrations Flyway V1..V9

- **Escopo:** Criar o schema completo via Flyway em `src/main/resources/db/migration`:
  - `V1` `categoria_financeira` (`id_categoria`, `nome`, `tipo` CHECK `RECEITA`/`DESPESA`, `descricao`);
  - `V2` `lancamento_financeiro` (FK `id_categoria`, `tipo` CHECK `ENTRADA`/`SAIDA`, `valor NUMERIC(14,2)`, `data_vencimento`, `data_pagamento`, `status` CHECK `PENDENTE`/`PAGO`/`ATRASADO`/`CANCELADO`, `id_referencia_externa VARCHAR(100)`, `observacao`; índices por `(status,data_vencimento)` e `id_referencia_externa`);
  - `V3` `doacao_recurso` (`tipo` CHECK `DOACAO`/`PROJETO`, `origem`, `valor`, `data_recebimento`, `id_projeto_associado`);
  - `V4` `valor_hora_nivel` (`nivel_acesso` CHECK 0-3, `valor_hora NUMERIC(12,2)`, `data_vigencia`; índice `(nivel_acesso,data_vigencia)`);
  - `V5` `parametro_overhead` (`valor_taxa_hora NUMERIC(12,4)`, `data_vigencia`);
  - `V6` `fechamento_encomenda` (`id_encomenda` UNIQUE, `horas_estimadas`, `valor_fechado`, `data_fechamento`, `status` CHECK `ABERTA`/`CONCLUIDA`/`CANCELADA`, `horas_validadas` DEFAULT 0);
  - `V7` `custo_encomenda` (materiais/mão de obra/overhead/total/venda/margem `NUMERIC(14,2)`, `data_calculo`; índice por `id_encomenda`);
  - `V8` `solicitacao_compra` (`id_item_estoque`, `quantidade NUMERIC(12,2)`, `valor_estimado NUMERIC(14,2)`, `status` CHECK `REGISTRADA`/`VISUALIZADA`/`CONCLUIDA`, `data_solicitacao`);
  - `V9` `horas_encomenda` (`id_encomenda`, `id_funcionario`, `nivel_acesso`, `horas NUMERIC(12,2)`, `data_registro`; índice por `id_encomenda`).
- **Entradas:** tabelas/campos de `docs/_archive/2026-10-19/05-Financeiro_Service.md` (seção 4) + schema real das migrations.
- **Saídas:** Banco `fablab_financeiro` com as nove tabelas, constraints/`CHECK`, FKs e índices.
- **Critérios:**
  - [ ] Flyway aplica `V1`..`V9` limpos e sem erro.
  - [ ] `ddl-auto: validate` não acusa divergência entre entidades e schema.
  - [ ] Constraints/`CHECK` e índices presentes no banco.
- **Dependências:** task-001

## task-003 — Entities, enums, DTOs e mappers

- **Escopo:** Criar as entidades JPA mapeando `categoria_financeira`, `lancamento_financeiro`, `doacao_recurso`, `valor_hora_nivel`, `parametro_overhead`, `fechamento_encomenda`, `custo_encomenda`, `solicitacao_compra`, `horas_encomenda`, com enums (`NivelAcesso`, `StatusLancamento`, `StatusFechamentoEncomenda`, `StatusSolicitacaoCompra`, tipos de categoria/lançamento/doação) e DTOs de request/response e eventos (`LancamentoVencidoEvent`, `CustoCalculadoEvent`, `CompraSolicitadaEvent`, `EncomendaCriadaEvent`, `HorasValidadasEvent`, `ProducaoConcluidaEvent`), com mappers estáticos (ex.: `Response.of(entity)`).
- **Entradas:** Modelo de dados (requirements §3); contratos de eventos das exchanges `fablab.vendas`, `fablab.rh`, `fablab.producao`.
- **Saídas:** Camada de modelo/DTO/mapper compilável, pronta para repositories e services.
- **Critérios:**
  - [ ] Entidades com mapeamento JPA correto e comentários de propósito.
  - [ ] DTOs de request com Bean Validation (campos obrigatórios, tamanhos, enums, valores positivos).
  - [ ] DTOs de evento com campos necessários ao consumo/publicação assíncrona.
- **Dependências:** task-002

## task-004 — Repositories

- **Escopo:** Criar `CategoriaFinanceiraRepository`, `LancamentoFinanceiroRepository` (consulta de vencidos `PENDENTE`/`ATRASADO` anteriores à data; soma de valores por tipo e referência externa; filtros por status, período e categoria), `DoacaoRecursoRepository`, `ValorHoraNivelRepository` (vigente por nível), `ParametroOverheadRepository` (vigente), `FechamentoEncomendaRepository` (por `id_encomenda`, existência, soma/atualização de `horas_validadas`), `CustoEncomendaRepository` (último cálculo por encomenda), `SolicitacaoCompraRepository`, `HorasEncomendaRepository` (soma de horas por encomenda; linha por (encomenda, funcionário, data)).
- **Entradas:** Entities/enums (task-003).
- **Saídas:** Repositórios com queries derivadas e `@Query` nativas onde necessário.
- **Critérios:**
  - [ ] Queries de vencidos, soma de saídas por referência e soma de horas por encomenda cobertas por testes.
  - [ ] Consultas de vigência retornam o registro mais recente por nível/parâmetro.
  - [ ] Acesso a `fechamento_encomenda` por `id_encomenda` (UNIQUE) com `Optional`.
- **Dependências:** task-003

## task-005 — CategoriaFinanceiraService + LancamentoFinanceiroService

- **Escopo:** Regras de categorias (cadastro `RECEITA`/`DESPESA`, listagem com filtro de tipo) e lançamentos: criação (valida categoria, define `PENDENTE` ou `ATRASADO` se vencido e publica `lancamento.vencido.event`), listagem com filtros (`status`, período de vencimento, `idCategoria`), liquidação (`PAGO` + `data_pagamento`, rejeitando `PAGO`/`CANCELADO`) e varredura `emitirVencidos()` que publica o evento por lançamento vencido.
- **Entradas:** DTOs (task-003), repositories (task-004), publisher (task-012).
- **Saídas:** Serviços de lançamento/categoria com regras completas de contas a pagar/receber.
- **Critérios:**
  - [ ] `criar` valida categoria e persiste status inicial correto (`ATRASADO` + evento quando vencido).
  - [ ] `listar` aplica filtros (status/periodo/categoria) e default `findAll`.
  - [ ] `registrarPagamento` liquida e lança `IllegalArgumentException` para `PAGO`/`CANCELADO`.
  - [ ] `emitirVencidos` retorna o número de eventos publicados.
- **Dependências:** task-004, task-012

## task-006 — ValorHoraNivelService + ParametroOverheadService

- **Escopo:** Cadastro de valor/hora por nível (0-3) com `data_vigencia` e da taxa de overhead por hora com vigência; consulta do valor vigente (mais recente) por nível e da taxa vigente — usados no custeio da mão de obra e overhead.
- **Entradas:** DTOs (task-003), repositories (task-004).
- **Saídas:** Serviços de parâmetros de custeio prontos para `CusteioService`.
- **Critérios:**
  - [ ] Validação de `nivel_acesso` entre 0 e 3.
  - [ ] `obterValorHoraPorNivel` retorna o registro com maior `data_vigencia` (vigente).
  - [ ] `taxaVigente` retorna o overhead vigente.
  - [ ] Erros orientativos quando o parâmetro não está configurado.
- **Dependências:** task-004

## task-007 — DoacaoRecursoService + SolicitacaoCompraService

- **Escopo:** Registro/listagem de doações e recursos (`DOACAO`/`PROJETO`, filtros tipo/período). Solicitações de compra: criação (`REGISTRADA` + publicação `compra.solicitada.event` com contrato do Estoque `{idCompra, idFornecedor}`; `idFornecedor` null no MVP), listagem/visualização e conclusão (`CONCLUIDA`).
- **Entradas:** DTOs (task-003), repositories (task-004), publisher (task-012).
- **Saídas:** Serviços de doações/recursos e do fluxo informativo de compras.
- **Critérios:**
  - [ ] Doação/recurso validada e listável com filtros; campo `id_projeto_associado` opcional.
  - [ ] Criação de solicitação persiste `REGISTRADA` e publica `compra.solicitada.event`.
  - [ ] Conclusão muda o status para `CONCLUIDA` e só é permitida em estado válido.
- **Dependências:** task-004, task-012

## task-008 — FechamentoEncomendaService

- **Escopo:** Abertura de fechamento em `ABERTA` (manual — rejeita encomenda já existente — e automática a partir de `encomenda.criada.event`, **idempotente**, usando `valorFinal`/`dataCriacao` do evento), consulta por `id_encomenda` e acúmulo de horas validadas: upsert em `horas_encomenda` por (encomenda, funcionário, data) e incremento de `horas_validadas` no fechamento.
- **Entradas:** DTOs/eventos (task-003), repositories (task-004).
- **Saídas:** Fechamento de encomenda (congelamento) e acumulador de horas para custeio.
- **Critérios:**
  - [ ] Criação manual rejeita encomenda já fechada (`IllegalArgumentException`).
  - [ ] `criarDoEvento` é idempotente e persiste `valor_fechado`, datas e `horas_validadas = 0`.
  - [ ] `registrarHorasValidadas` faz upsert agregado e atualiza `horas_validadas`.
- **Dependências:** task-004

## task-009 — CusteioService (Job Order Costing)

- **Escopo:** Cálculo do custo por ordem ao gatilho `producao.concluida.event`: resolve o fechamento, soma lançamentos `SAIDA` não cancelados com `id_referencia_externa` = encomenda (materiais), monta horas por nível com default nível 2 (Voluntário) × valor/hora vigente (mão de obra), taxa de overhead vigente × total de horas, `custo_total` e `margem_lucro = valor_fechado − custo_total`; persiste em `custo_encomenda`, publica `custo.calculado.event` e expõe consulta do último cálculo por encomenda. Valida pré-condições com mensagens claras (sem fechamento, sem valor/hora do nível, sem taxa de overhead).
- **Entradas:** `FechamentoEncomendaService` (task-008), valores/hora e overhead (task-006), repositories de lançamento/horas/custo.
- **Saídas:** `CustoEncomendaResponse` calculado, persistido e reportado ao Vendas.
- **Critérios:**
  - [ ] Cálculo segue as fórmulas: materiais (ΣSAIDA ref), mão de obra (Σ horas×valor/hora), overhead (taxa×total horas), total e margem.
  - [ ] Horas sem nível custeadas pelo nível 2 (Voluntário) — assunção documentada.
  - [ ] Pré-condições ausentes geram erro orientativo e não persistem custo.
  - [ ] Consulta por encomenda retorna o registro mais recente; evento `custo.calculado.event` publicado.
- **Dependências:** task-005, task-006, task-008, task-012

## task-010 — RelatorioService

- **Escopo:** Relatórios: fluxo de caixa (entradas, saídas, saldo no período), DRE (receitas = entradas + doações, despesas, resultado), lucratividade por encomenda (venda, custo, margem, margem %), inadimplência (a receber vencidas não liquidadas), doações vs. despesas e custo por máquina (agregado por `idReferenciaExterna` das saídas).
- **Entradas:** repositories de lançamento, doação, fechamento e custo (task-004 e serviços correlatos).
- **Saídas:** DTOs de resposta por relatório.
- **Critérios:**
  - [ ] Cada relatório retorna estrutura tipada (`FluxoCaixaResponse`, `DreResponse`, `LucratividadeItemResponse`, `InadimplenciaItemResponse`, `DoacoesDespesasResponse`, `CustoMaquinaItemResponse`).
  - [ ] Períodos (`dataInicio`/`dataFim`) aplicados e default cobrindo todos os registros.
  - [ ] `custo-maquina` usa `id_referencia_externa` como identificador de máquina (convenção documentada).
- **Dependências:** task-005, task-007, task-008, task-009

## task-011 — Scheduler de lançamentos vencidos

- **Escopo:** Agendamento periódico da varredura de lançamentos vencidos: `@EnableScheduling` na aplicação e `@Scheduled(cron = "${financeiro.vencidos.scheduler.cron}")` chamando `emitirVencidos()`; propriedade para habilitar/desabilitar (`financeiro.vencidos.scheduler.enabled`) e cron configurável. Cada lançamento `PENDENTE`/`ATRASADO` com `data_vencimento` anterior à data atual deve publicar `lancamento.vencido.event` (idempotente — o evento continua sendo emitido enquanto não liquidado).
- **Entradas:** `LancamentoFinanceiroService.emitirVencidos()` (task-005).
- **Saídas:** Geração periódica de alertas de vencimento ao Notification.
- **Critérios:**
  - [ ] Scheduler dispara no cron configurado e chama `emitirVencidos`.
  - [ ] Flag de habilitação respeitada; nenhum evento publicado quando desabilitado.
  - [ ] Varredura não liquida nenhum lançamento (apenas notifica).
- **Dependências:** task-005

## task-012 — RabbitMQ (config, publisher e listeners)

- **Escopo:** `RabbitMqConfig` com exchange tópica durável `fablab.financeiro`, exchanges de consumo (`fablab.vendas`, `fablab.rh`, `fablab.producao`), filas `financeiro.fechamento.encomenda`, `financeiro.horas.encomenda`, `financeiro.custo.calculo` e bindings pelas routing keys `encomenda.criada.event`, `horas.validadas.event`, `producao.concluida.event`. `FinanceiroEventPublisher` com métodos `publishLancamentoVencido` (kry `lancamento.vencido.event`, → Notification), `publishCustoCalculado` (`custo.calculado.event`, → Vendas) e `publishCompraSolicitada` (`compra.solicitada.event`, → Estoque), todos tolerantes a falhas (try/catch + log). Listeners: `EncomendaCriadaListener`, `HorasValidadasListener`, `ProducaoConcluidaListener` delegando aos services, com tratamento de exceções (log + não derruba o consumer).
- **Entradas:** Constantes de exchange/filas/keys (contrato de eventos dos domínios), services (task-005/007/008/009).
- **Saídas:** Fluxo assíncrono completo publica/consome conforme requirements §6.
- **Critérios:**
  - [ ] Exchange/filas/bindings declarados no startup; filas duráveis.
  - [ ] Publicações recuperam-se de falha (log) sem interromper o fluxo.
  - [ ] `EncomendaCriadaListener` abre fechamento; `HorasValidadasListener` acumula horas; `ProducaoConcluidaListener` dispara custeio.
  - [ ] Consumo tolera eventos malformados (log de erro sem perda do consumer).
- **Dependências:** task-004, task-005, task-007, task-008, task-009

## task-013 — Controllers, Security e tratamento de erros

- **Escopo:** Controladores REST em PT (`/lancamentos`, `/categorias`, `/doacoes-recursos`, `/valores-hora`, `/parametros-overhead`, `/fechamento-encomenda`, `/solicitacoes-compra`, `/custos-encomenda/{idEncomenda}`, `/relatorios/*`), todos com `@PreAuthorize("hasRole('ADMIN')")`. `SecurityConfig` stateless com `JwtAuthenticationFilter`, `JwtProperties`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`; `/actuator/health` `permitAll`. Exceções mapeadas em `ErrorResponse` (com `fieldErrors` para validação).
- **Entradas:** Services (task-005..010), padrões de segurança dos serviços irmãos.
- **Saídas:** API REST PT-BR protegida e com contratos de erro uniformes.
- **Critérios:**
  - [ ] Todos os endpoints listados em requirements §5 respondem conforme especificado.
  - [ ] Requests sem JWT → 401; com JWT não-Admin → 403.
  - [ ] Erros de validação retornam `ErrorResponse` com `fieldErrors`.
  - [ ] Creators com `201`, consultas com `200`, not found com mensagem clara.
- **Dependências:** task-005, task-006, task-007, task-008, task-009, task-010

## task-014 — Abstração de pagamentos (PaymentProcessor)

- **Escopo:** Interface `PaymentProcessor` isolando o núcleo do processamento de pagamentos (métodos abstratos para registrar/introspectar pagamentos), sem implementação concreta no MVP — registro 100% manual. Deve permitir plugin futuro de Pix/Cartão/Boleto e conciliação bancária sem refatorar o núcleo.
- **Entradas:** requirements §2-F9, §8 (abstração de pagamentos).
- **Saídas:** Interface `PaymentProcessor` e integração manual no fluxo de liquidação.
- **Critérios:**
  - [ ] Nenhum service depende de provedor concreto (apenas da interface).
  - [ ] Fluxo atual permanece manual e funcional.
  - [ ] Documentado como ponto de extensão para o futuro `Payment Service`.
- **Dependências:** task-005

## task-015 — Testes

- **Escopo:** Testes unitários (services: lançamento, fechamento, custeio com fórmulas/estados, valores-hora/overhead, solicitação de compra, relatórios), de integração (Flyway aplica V1..V9; repositórios; REST), de segurança (JWT e RBAC ADMIN/403) e de mensageria (listen/processa eventos; publikação de vencido, custo calculado e compra).
- **Entradas:** Implementação (task-001..014) e padrões de teste dos serviços irmãos.
- **Saídas:** Suíte de testes em `src/test/java/com/fablab/financeiro` (`unit`, `integration`, `security`).
- **Critérios:**
  - [ ] Cobeça as principais regras: status inicial/liquidação, idempotência do fechamento, fórmulas do custeio, default nível 2, pré-condições do custeio.
  - [ ] Testes de segurança garantem `401` sem JWT e `403` para níveis não-Admin.
  - [ ] `mvn test` passa de forma consistente.
- **Dependências:** task-013

## task-016 — Documentação e entrega

- **Escopo:** `README.md` (execução, configs, health), `docs/API.md` (spec de todos os endpoints, payloads e contratos de mensageria), changelog e premissas de integração do MVP (nível default 2 nas horas; `idFornecedor` null; `idReferenciaExterna` como máquina).
- **Entradas:** Implementação final (task-001..015) e requirements.
- **Saídas:** Documentação de serviço consistente com o código e com `docs/_archive/2026-10-19/05-Financeiro_Service.md`.
- **Critérios:**
  - [ ] `API.md` espelha rotas, filtros, payloads e eventos reais.
  - [ ] Premissas de MVP documentadas (horas sem nível → nível 2; compra `{idCompra, idFornecedor}`; custo por máquina via `idReferenciaExterna`).
  - [ ] README cobre execução, variáveis de ambiente e health-check.
- **Dependências:** task-015
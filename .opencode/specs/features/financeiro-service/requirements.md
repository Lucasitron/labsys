# Requirements: Financeiro Service

## 1. Objetivo e Escopo

O `financeiro-service` é o microsserviço responsável por toda a gestão financeira do Fab Lab: fluxo de caixa (entradas e saídas), contas a pagar e a receber com vencimento e baixa, doações e recursos de projetos universitários, fluxo informativo de compras (o Admin visualiza, mas não bloqueia a operação) e cálculo do custo real de produção via **Job Order Costing** (Materiais Diretos + Mão de Obra Direta + Custos Indiretos). Também concentra a definição de valores/hora por nível de acesso, a validação dos apontamentos de horas de produção e a geração de relatórios de saúde financeira.

O acesso é **exclusivo do Admin (nível 0)**. O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale), com Java 21 + Spring Boot, PostgreSQL, REST síncrono e RabbitMQ assíncrono.

O escopo desta spec cobre o MVP implementado na branch `feature/financeiro-service`: persistência (Flyway V1..V9), endpoints REST em PT-BR, integrações assíncronas (Vendas, RH, Produção, Estoque, Notification), custeio por ordem e relatórios. O processamento de pagamentos é **100% manual** no MVP, com a interface `PaymentProcessor` preparando a integração futura com um `Payment Service` dedicado.

## 2. Funcionalidades

### F1 - Fluxo de Caixa e Contas a Pagar/Receber
- Lançamentos de entrada (a receber) e saída (a pagar) com valor, categoria, data de vencimento e status (`PENDENTE`, `PAGO`, `ATRASADO`, `CANCELADO`).
- Lançamento criado assume `PENDENTE`; se a `data_vencimento` já estiver vencida, assume `ATRASADO` e publica imediatamente `lancamento.vencido.event`.
- Liquidação manual do lançamento (`PAGO` + `data_pagamento`); lançamento já `PAGO`/`CANCELADO` não pode ser liquidado.
- Listagem com filtros: status, período de vencimento (`dataInicio`/`dataFim`) e categoria.
- Varredura de vencidos (`emitirVencidos`) que publica `lancamento.vencido.event` para todos os `PENDENTE`/`ATRASADO` com vencimento anterior à data atual.
- `id_referencia_externa` associa o lançamento a uma encomenda/máquina (usado no custeio de materiais e no relatório de custo por máquina).

### F2 - Categorias Financeiras
- Cadastro e listagem de categorias `RECEITA`/`DESPESA` (ex: Vendas, Insumos, Salários, Doações), com descrição.
- Listagem com filtro opcional por tipo.

### F3 - Doações e Recursos de Projetos
- Registro de doações ou recursos de projeto universitário (`DOACAO`/`PROJETO`) com origem (doador/órgão financiador), valor, data de recebimento e projeto associado (opcional).
- Listagem com filtros por tipo e período.

### F4 - Compras (Fluxo Informativo)
- Registro de solicitação de compra originada no Estoque & Suprimentos com status `REGISTRADA`.
- O Admin visualiza a solicitação no painel; **não há bloqueio da operação**.
- Publicação de `compra.solicitada.event` para o Estoque & Suprimentos; ao concluir a compra, o lançamento de saída é feito e o status muda para `CONCLUIDA`.
- Estados: `REGISTRADA`, `VISUALIZADA`, `CONCLUIDA`.

### F5 - Custeio por Ordem (Job Order Costing)
- Fechamento pré-produção congelando horas estimadas e valor acordado em `fechamento_encomenda` (estado `ABERTA`); a partir do fechamento os valores ficam congelados.
- Abertura automática do fechamento em `ABERTA` ao consumir `encomenda.criada.event` (idempotente); criação manual também é possível.
- Acúmulo de horas validadas por encomenda (`horas_encomenda` + `horas_validadas`) a partir de `horas.validadas.event` do RH.
- Cálculo automático na entrega (evento `producao.concluida.event`):
  - `custo_materiais` = Σ lançamentos `SAIDA` não cancelados com `id_referencia_externa` = id da encomenda;
  - `custo_mao_obra` = Σ (horas validadas × valor/hora vigente do nível);
  - `custo_overhead` = taxa de overhead vigente × total de horas validadas;
  - `custo_total` = materiais + mão de obra + overhead;
  - `margem_lucro` = `valor_fechado` − `custo_total`.
- Publica `custo.calculado.event` ao finalizar; consulta de custo detalhado por encomenda (último cálculo).
- Alteração de encomenda: encerra a ordem atual (custo calculado e congelado) e abre nova ordem com novas estimativas e valor.

### F6 - Valores/Hora e Taxa de Overhead
- Cadastro do valor/hora por nível de acesso (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário) com vigência.
- Cadastro da taxa de overhead por hora com vigência.
- Consulta dos valores vigentes (mais recentes para cada nível).
- Pré-condição de custeio: valor/hora de cada nível envolvido e a taxa de overhead vigente devem estar configurados antes do cálculo.

### F7 - Validação de Horas (Coerência)
- O Financeiro consome as horas validadas do RH e as acumula por (encomenda, funcionário, data), alimentando o custeio.
- Na contabilização da mão de obra, horas sem nível informado são custeadas pelo valor/hora do nível **2 (VOLUNTARIO)** — premissa de MVP documentada para alinhamento do contrato com o RH.
- A **coerência** entre horas de presença (RFID), encomenda e projeto é uma regra de negócio obrigatória: apontamentos que excedem as horas de presença devem ser rejeitados ou alertados (notificação ao Admin).

### F8 - Relatórios de Saúde Financeira
- `fluxo-caixa`: entradas, saídas e saldo no período.
- `dre`: receitas (entradas + doações), despesas e resultado (DRE simplificado).
- `lucratividade`: valor de venda, custo, margem e margem % por encomenda.
- `inadimplencia`: contas a receber vencidas e não liquidadas.
- `doacoes-despesas`: doações vs. despesas no período (separa doações das receitas operacionais).
- `custo-maquina`: custo agregado por máquina via `idReferenciaExterna` dos lançamentos de saída.

### F9 - Abstração de Pagamentos (Preparação)
- Interface `PaymentProcessor` (abstrata) isolando o núcleo financeiro de provedores concretos.
- MVP 100% manual; a estrutura permite plugar Pix/Cartão/Boleto e conciliação bancária em um futuro `Payment Service` sem refatorar o núcleo.

## 3. Tabelas

Esquema gerenciado por Flyway, migrations reais `V1`..`V9` (`classpath:db/migration`). Os campos abaixo seguem `docs/_archive/2026-10-19/05-Financeiro_Service.md` (fonte canônica); os enums/checks e a tabela extra `horas_encomenda` (V9) refletem o schema real da branch.

| Tabela | Campos |
|---|---|
| `categoria_financeira` | `id_categoria` (Serial/PK), `nome` (String), `tipo` (String: `RECEITA`/`DESPESA`), `descricao` (String) |
| `lancamento_financeiro` | `id_lancamento` (Serial/PK), `id_categoria` (FK → `categoria_financeira`), `tipo` (String: `ENTRADA`/`SAIDA`), `valor` (Decimal), `data_vencimento` (Date), `data_pagamento` (Date), `status` (String: `PENDENTE`/`PAGO`/`ATRASADO`/`CANCELADO`), `id_referencia_externa` (String), `observacao` (String) |
| `doacao_recurso` | `id_doacao` (Serial/PK), `tipo` (String: `DOACAO`/`PROJETO`), `origem` (String), `valor` (Decimal), `data_recebimento` (Date), `id_projeto_associado` (Inteiro, opcional) |
| `valor_hora_nivel` | `id_valor_hora` (Serial/PK), `nivel_acesso` (Inteiro 0-3), `valor_hora` (Decimal), `data_vigencia` (Date) |
| `parametro_overhead` | `id_parametro` (Serial/PK), `valor_taxa_hora` (Decimal), `data_vigencia` (Date) |
| `fechamento_encomenda` | `id_fechamento` (Serial/PK), `id_encomenda` (Inteiro, FK externa → Vendas, **UNIQUE**), `horas_estimadas` (Decimal), `valor_fechado` (Decimal), `data_fechamento` (Date), `status` (String: `ABERTA`/`CONCLUIDA`/`CANCELADA`), `horas_validadas` (Decimal, default 0) |
| `custo_encomenda` | `id_custo` (Serial/PK), `id_encomenda` (Inteiro, FK externa → Vendas), `custo_materiais` (Decimal), `custo_mao_obra` (Decimal), `custo_overhead` (Decimal), `custo_total` (Decimal), `valor_venda` (Decimal), `margem_lucro` (Decimal), `data_calculo` (Date) |
| `solicitacao_compra` | `id_solicitacao` (Serial/PK), `id_item_estoque` (Inteiro, FK externa → Estoque), `quantidade` (Decimal), `valor_estimado` (Decimal), `status` (String: `REGISTRADA`/`VISUALIZADA`/`CONCLUIDA`), `data_solicitacao` (Date) |
| `horas_encomenda` | `id_horas` (Serial/PK), `id_encomenda` (Inteiro), `id_funcionario` (Inteiro), `nivel_acesso` (Inteiro, opcional), `horas` (Decimal), `data_registro` (Date) |

Definições `DDL` reais (constraints/índices):
- `valor NUMERIC(14,2)`, `valor_hora NUMERIC(12,2)`, `valor_taxa_hora NUMERIC(12,4)`, `quantidade NUMERIC(12,2)`, `valor_estimado NUMERIC(14,2)`, `horas/valor_fechado NUMERIC(14,2)`.
- `CHECK` em enums: `categoria_financeira.tipo`, `lancamento_financeiro.tipo/status`, `doacao_recurso.tipo`, `valor_hora_nivel.nivel_acesso` (0-3), `fechamento_encomenda.status`, `solicitacao_compra.status`.
- Índices: `idx_lancamento_status_vencimento (status, data_vencimento)`, `idx_lancamento_referencia (id_referencia_externa)`, `idx_valor_hora_nivel_vigencia (nivel_acesso, data_vigencia)`, `idx_custo_encomenda_id_encomenda`, `idx_horas_encomenda_id_encomenda`.

## 4. Fluxos

### Fluxo de Contas a Pagar e Receber
1. O Admin cria um lançamento com categoria, tipo (`ENTRADA`/`SAIDA`), valor, data de vencimento e status inicial `PENDENTE` (ou `ATRASADO` se já vencido, com publicação imediata de `lancamento.vencido.event`).
2. No vencimento, a varredura periódica (`emitirVencidos`) publica `lancamento.vencido.event` para os pendentes/atrasados não liquidados → alerta via Notification Service.
3. Ao ser pago/recebido, o Admin liquida o lançamento (`PUT /lancamentos/{id}/pagamento`), gravando `data_pagamento` e mudando o status para `PAGO`.

### Fluxo de Aprovação de Compras (Informativo)
1. O `Estoque & Suprimentos Service` solicita uma compra (esperada via `compra.solicitada.event` conforme contrato `{idCompra, idFornecedor}`).
2. O Financeiro registra a solicitação com status `REGISTRADA`.
3. O Admin visualiza a solicitação no painel de resumo; **não há bloqueio** da operação.
4. Após a compra, o Admin conclui a solicitação (`PUT /solicitacoes-compra/{id}/concluir`) e o lançamento de saída é feito; o status muda para `CONCLUIDA`.

### Fluxo de Cálculo de Custo (Job Order Costing)
1. **Antes do início:** o responsável estima as horas e o Admin fecha valor e horas em `fechamento_encomenda` (estado `ABERTA`; valores congelados). O fechamento também abre automaticamente a partir de `encomenda.criada.event`.
2. **Durante a produção:** horas reais são apontadas no `RH Service`; o Financeiro consome `horas.validadas.event`, acumula em `horas_encomenda` por (encomenda, funcionário, data) e soma em `horas_validadas` do fechamento. A **coerência** (`Horas Encomenda + Horas Projeto ≤ Horas Presença` no mesmo dia) é validada; incoerência notifica o Admin.
3. **Na entrega:** ao consumir `producao.concluida.event`, o Financeiro calcula e grava `custo_encomenda` (materiais + mão de obra + overhead), com `margem_lucro = valor_fechado − custo_total`, e publica `custo.calculado.event` (Vendas & CRM).
4. **Alteração de Encomenda:** se o cliente solicitar mudança, a ordem atual é **encerrada** (custo calculado e congelado) e uma **nova ordem** é criada com novas estimativas e valor.

### Fluxo de Doações e Recursos
1. O Admin registra uma doação ou recurso de projeto com tipo (`DOACAO`/`PROJETO`), origem e valor.
2. O valor é lançado como entrada.
3. Os relatórios separam doações e recursos das receitas operacionais (`dre`, `doacoes-despesas`).

## 5. Endpoints

Todos exigem `Authorization: Bearer <JWT>` do Auth & Identity Service (exceto `/actuator/health`); RBAC **exclusivo do Admin**. Rotas em PT-BR.

| Método | Recurso | Descrição |
|---|---|---|
| POST | `/lancamentos` | Cria lançamento financeiro (entrada/saída) |
| GET | `/lancamentos` | Lista lançamentos com filtros `status`, `dataInicio`/`dataFim`, `idCategoria` |
| PUT | `/lancamentos/{idLancamento}/pagamento` | Registra pagamento/recebimento (liquida) |
| POST | `/categorias` | Cadastra categoria financeira (`RECEITA`/`DESPESA`) |
| GET | `/categorias` | Lista categorias (filtro opcional `tipo`) |
| POST | `/doacoes-recursos` | Registra doação ou recurso de projeto |
| GET | `/doacoes-recursos` | Lista doações/recursos (filtros `tipo`, `dataInicio`/`dataFim`) |
| POST | `/valores-hora` | Define valor/hora por nível (0-3) com vigência |
| GET | `/valores-hora` | Lista o valor vigente de cada nível |
| POST | `/parametros-overhead` | Define taxa de overhead por hora com vigência |
| POST | `/fechamento-encomenda` | Fecha valor e horas estimadas (valores congelados) |
| GET | `/fechamento-encomenda/{idEncomenda}` | Consulta fechamento da encomenda |
| POST | `/solicitacoes-compra` | Registra solicitação de compra (informativo) |
| PUT | `/solicitacoes-compra/{id}/concluir` | Conclui a solicitação de compra |
| GET | `/custos-encomenda/{idEncomenda}` | Retorna custo detalhado da encomenda (último cálculo) |
| GET | `/relatorios/fluxo-caixa` | Fluxo de caixa no período |
| GET | `/relatorios/dre` | DRE simplificado |
| GET | `/relatorios/lucratividade` | Lucratividade por encomenda |
| GET | `/relatorios/inadimplencia` | Contas a receber vencidas e não liquidadas |
| GET | `/relatorios/doacoes-despesas` | Doações vs. despesas no período |
| GET | `/relatorios/custo-maquina` | Custo agregado por máquina |

## 6. Integrações

| Integração | Tipo | Direção | Evento / Uso |
|---|---|---|---|
| Vendas & CRM | Mensageria (RabbitMQ 5672) | Consome / Publica | Consome `encomenda.criada.event` (abre fechamento em `ABERTA`); publica `custo.calculado.event` (custo e margem da encomenda) e usa `id_encomenda`/`valor_final` |
| Estoque & Suprimentos | Mensageria / REST | Consome / Publica | Recebe solicitações de compra (fluxo informativo) e fornece consumo/BOM para custo de materiais; publica `compra.solicitada.event` (contrato `{idCompra, idFornecedor}`) |
| RH (Pessoas & RH) | Mensageria | Consome | `horas.validadas.event` (`fablab.rh`) → cumula horas por (encomenda, funcionário, data); dados de nível/horas para mão de obra |
| Produção & Projetos | Mensageria | Consome | `producao.concluida.event` (`fablab.producao`) → dispara o cálculo de custo da encomenda |
| Notification | Mensageria | Publica | `lancamento.vencido.event` → alertas de vencimento, incoerência de horas e resumo de compras |
| Auth & Identity | REST (JWT) | Consome | Valida JWT/claims (`role`, `setor`) via filtro de autenticação |
| Service Discovery | Eureka | — | Registro em `http://localhost:8761/eureka` (prefer-ip-address) |

Exchange tópica do domínio: **`fablab.financeiro`** (durável). Filas do Financeiro: `financeiro.fechamento.encomenda`, `financeiro.horas.encomenda`, `financeiro.custo.calculo`.

## 7. RBAC

Autenticação via JWT do Auth & Identity Service (`Authorization: Bearer <JWT>`), aplicada por `@PreAuthorize("hasRole('ADMIN')")` em todos os controladores. Acesso **exclusivo do Admin (nível 0)**; qualquer outro nível autenticado recebe `403`. `/actuator/health` é `permitAll`.

| Recurso | Admin | Bolsista | Voluntário | Estagiário |
|---|---|---|---|---|
| Todos os endpoints `/lancamentos`, `/categorias`, `/doacoes-recursos`, `/valores-hora`, `/parametros-overhead`, `/fechamento-encomenda`, `/solicitacoes-compra`, `/custos-encomenda/*`, `/relatorios/*` | ✅ | — | — | — |
| `/actuator/health` | ✅ | ✅ | ✅ | ✅ |

## 8. Não-Funcionais

- **Idioma:** recursos, endpoints, códigos e mensagens em PT-BR.
- **Persistência:** PostgreSQL `fablab_financeiro` (user/pass `fablab`), `ddl-auto: validate`, Flyway habilitado (`classpath:db/migration`, V1..V9).
- **Porta:** `8085` (`${PORT:8085}`); registrado no Eureka (`http://localhost:8761/eureka`) com `prefer-ip-address: true`.
- **Mensageria:** RabbitMQ 5672 (user/pass `fablab`), exchange tópica `fablab.financeiro` durável.
- **Coerência de Horas:** regra obrigatória — rejeitar/alertar apontamentos que excedam as horas de presença (RFID) no mesmo dia.
- **Congelamento de Valores:** valor e horas estimadas de uma encomenda não são alterados após o fechamento; alterações geram nova ordem.
- **Auditoria:** todas as alterações de valores/hora, taxas de overhead e fechamentos registradas com usuário e data.
- **Abstração de Pagamentos:** `PaymentProcessor` isolado para futuro `Payment Service` (registro manual no MVP).
- **Validação:** DTOs com Bean Validation; erros retornam `ErrorResponse` JSON (`timestamp`, `status`, `error`, `message`, `path`) e `fieldErrors` por campo.
- **Observabilidade:** Actuator expõe `health`, `info`, `metrics`.

## 9. Critérios de Sucesso

- [ ] Lançamento criado com `PENDENTE`/`ATRASADO`; liquidação grava `data_pagamento` e `PAGO`; vencidos não liquidados publicam `lancamento.vencido.event`.
- [ ] Categoria `RECEITA`/`DESPESA` cadastrada/listada; doação/recurso `DOACAO`/`PROJETO` registrado/listado e lançado como entrada.
- [ ] Solicitação de compra registrada (`REGISTRADA`), informativa (sem bloqueio), publica `compra.solicitada.event`; conclusão muda para `CONCLUIDA`.
- [ ] Fechamento aberto automaticamente por `encomenda.criada.event` (idempotente) ou manualmente, com valores congelados.
- [ ] Horas validadas do RH acumuladas por (encomenda, funcionário, data) e refletidas em `horas_validadas` do fechamento.
- [ ] `producao.concluida.event` calcula `custo_encomenda` completo (materiais + mão de obra + overhead + margem) e publica `custo.calculado.event`; pré-condições (fechamento, valores/hora, overhead) validadas com mensagens claras.
- [ ] Consulta `GET /custos-encomenda/{idEncomenda}` retorna o último cálculo.
- [ ] Relatórios de fluxo de caixa, DRE, lucratividade, inadimplência, doações vs. despesas e custo por máquina retornam dados corretos.
- [ ] Coerência de horas validada; custeio de horas sem nível usa nível 2 (Voluntário) — assunção de MVP documentada.
- [ ] Todos os endpoints exigem JWT válido e somente `ADMIN` acessa (demais níveis → 403).
- [ ] `PaymentProcessor` abstraído sem acoplamento a provedor concreto.

## 10. Fora de Escopo

- Processamento/execução real de pagamentos (Pix, Cartão, Boleto) e conciliação bancária — futuro `Payment Service`.
- Cadastro de clientes, orçamentos e encomendas (Vendas & CRM).
- Apontamento de horas de projeto/presença e validação física de RFID (RH/Produção).
- Execução da produção e consumo de BOM (Produção & Projetos).
- Gestão de pessoas, permissões e autenticação (Auth & Identity / RH).
# Requirements: Vendas & CRM Service

## 1. Objetivo e Escopo

O `vendas-service` (Vendas & CRM) é o microsserviço responsável pela gestão comercial do Fab Lab. Abrange o cadastro de clientes (Pessoa Física e Pessoa Jurídica), a criação e o acompanhamento de orçamentos e encomendas, o Kanban de status dos pedidos (com histórico de movimentações), o registro manual de vendas em marketplaces e as rotinas internas de CRM e marketing da equipe.

A operação é **100% interna**: o cliente não possui acesso ao sistema, não existem endpoints públicos e nenhum endpoint deve ser exposto sem autenticação (JWT). O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale). Executa em Java 21 + Spring Boot com PostgreSQL, registra-se no Eureka (`http://localhost:8761/eureka`) na porta `8084` (override por `${PORT:8084}`), e comunica-se por REST (síncrono) e RabbitMQ (assíncrono, porta `5672`, exchange `fablab.vendas`).

## 2. Funcionalidades

### F1 - Cadastro de Clientes (PF/PJ)
- Cadastrar, consultar (com filtros por nome, tags e tipo) e atualizar clientes via `cliente` (`tipo_pessoa` PF ou PJ, `nome_razao_social`, `cpf_cnpj` único, `email`, `telefone`, `endereco`, `data_cadastro`).
- Validação de CPF e CNPJ no cadastro e na atualização.

### F2 - Tags de Clientes
- Cadastrar e listar tags via `tag_cliente` (`nome`, `cor`).
- Vincular/desvincular tags a clientes via `cliente_tag` (N:N) para segmentação de marketing.

### F3 - Orçamentos
- Criar orçamento via `orcamento` (`validade`, `valor_total`, `status` Pendente/Aprovado/Recusado/Ajuste, `observacoes`) com itens em `item_orcamento` (`descricao`, `quantidade`, `valor_unitario`).
- `PUT /orcamentos/{id}` permite ajustes: quando o cliente pede mudanças, o status vai para `Ajuste` e a equipe edita itens/valores.
- Ao aprovar, publicar `orcamento.aprovado.event` para o Financeiro e permitir a conversão em encomenda.

### F4 - Ciclo de Vida da Encomenda
- Criar encomenda a partir de orçamento **aprovado** via `encomenda` (`data_previsao_entrega`, `status_kanban` inicial = `Fila`, `valor_final`, `observacoes`). Encomenda sem orçamento formalmente aprovado é bloqueada.
- Emissão de recibo interno (não fiscal).
- **Alteração de Encomenda:** se o cliente solicitar mudança após o fechamento, a encomenda atual é **encerrada** e uma **nova ordem** é criada com novas estimativas e valor (regra alinhada ao Financeiro); a nova ordem carrega referência legada no histórico.

### F5 - Kanban de Encomendas com Histórico
- Mover a encomenda entre colunas (`Fila`, `Produção`, `Acabamento`, `Pronto`, `Entregue`) via `PUT /encomendas/{id}/kanban`.
- Cada movimento grava registro em `historico_status_encomenda` (`status_anterior`, `status_novo`, `data_alteracao`, `id_usuario`, `observacao`) — auditoria com usuário e data.
- Ao movimentar, publicar `encomenda.status.alterado.event` (Produção + Notification).
- Concorrência: locks otimistas impedem movimentações simultâneas conflitantes sobre o mesmo cartão.

### F6 - Registro Manual de Marketplace
- Registrar venda manual de pedido de plataformas externas via `registro_marketplace` (`plataforma`, `codigo_externo`, `data_venda`, `valor_taxa`) vinculado a uma encomenda.
- Fornecer dados de taxa para cálculo do lucro líquido (Financeiro).

### F7 - CRM Interno (Interações com Clientes)
- Registrar interações via `interacao_cliente` (`tipo` E-mail/Telefone/Reunião/WhatsApp, `descricao`, `id_usuario` responsável, `data_interacao`).
- Listar histórico de interações de um cliente.
- Notificar responsáveis via Notification quando necessário.

### F8 - Tarefas de Marketing
- Criar e gerenciar tarefas internas via `tarefa_marketing` (`titulo`, `descricao`, `id_responsavel`, `data_inicio`, `data_fim`, `status` Pendente/Em Andamento/Concluída, `prioridade` Baixa/Média/Alta).
- `PUT /tarefas-marketing/{id}` para atualizar status/prioridade; notificar responsáveis sobre prazos e mudanças de status.

## 3. Tabelas de Dados

Campos conforme a doc estabelecida (`docs/_archive/2026-10-19/04-Vendas_&_CRM_Services.md`).

**Tabela: cliente**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_cliente` | Serial | Identificador único |
| `tipo_pessoa` | String | Tipo (PF ou PJ) |
| `nome_razao_social` | String | Nome completo ou razão social |
| `cpf_cnpj` | String | Documento de identificação (Único) |
| `email` | String | E-mail de contato |
| `telefone` | String | Telefone de contato |
| `endereco` | String | Endereço completo |
| `data_cadastro` | Date | Data de cadastro |

**Tabela: tag_cliente**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_tag` | Serial | Identificador único |
| `nome` | String | Nome da tag (ex: VIP, 3D, Laser) |
| `cor` | String | Cor para exibição |

**Tabela: cliente_tag**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_cliente_tag` | Serial | Identificador único |
| `id_cliente` | Inteiro | Chave estrangeira para `cliente` |
| `id_tag` | Inteiro | Chave estrangeira para `tag_cliente` |

**Tabela: orcamento**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_orcamento` | Serial | Identificador único |
| `id_cliente` | Inteiro | Chave estrangeira para `cliente` |
| `data_criacao` | Date | Data de criação |
| `validade` | Date | Data de validade do orçamento |
| `valor_total` | Decimal | Valor total orçado |
| `status` | String | Status (Pendente, Aprovado, Recusado, Ajuste) |
| `observacoes` | String | Observações gerais |

**Tabela: item_orcamento**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_item_orcamento` | Serial | Identificador único |
| `id_orcamento` | Inteiro | Chave estrangeira para `orcamento` |
| `descricao` | String | Descrição do produto/serviço |
| `quantidade` | Decimal | Quantidade solicitada |
| `valor_unitario` | Decimal | Valor unitário |

**Tabela: encomenda**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_encomenda` | Serial | Identificador único |
| `id_orcamento` | Inteiro | Chave estrangeira para `orcamento` (opcional) |
| `id_cliente` | Inteiro | Chave estrangeira para `cliente` |
| `data_criacao` | Date | Data de criação |
| `data_previsao_entrega` | Date | Data prevista para entrega |
| `status_kanban` | String | Status atual no Kanban (ex: Fila, Produção, Acabamento, Pronto, Entregue) |
| `valor_final` | Decimal | Valor final da encomenda |
| `observacoes` | String | Observações |

**Tabela: historico_status_encomenda**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_historico` | Serial | Identificador único |
| `id_encomenda` | Inteiro | Chave estrangeira para `encomenda` |
| `status_anterior` | String | Status anterior |
| `status_novo` | String | Novo status |
| `data_alteracao` | Timestamp | Data e hora da alteração |
| `id_usuario` | Inteiro | Chave estrangeira para `funcionario` (quem alterou) |
| `observacao` | String | Motivo ou observação da mudança |

**Tabela: registro_marketplace**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_registro` | Serial | Identificador único |
| `id_encomenda` | Inteiro | Chave estrangeira para `encomenda` |
| `plataforma` | String | Nome do marketplace (ex: Mercado Livre, Shopee) |
| `codigo_externo` | String | Código do pedido na plataforma |
| `data_venda` | Date | Data da venda |
| `valor_taxa` | Decimal | Valor da taxa cobrada pela plataforma |

**Tabela: interacao_cliente**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_interacao` | Serial | Identificador único |
| `id_cliente` | Inteiro | Chave estrangeira para `cliente` |
| `data_interacao` | Timestamp | Data e hora da interação |
| `tipo` | String | Tipo (E-mail, Telefone, Reunião, WhatsApp) |
| `descricao` | String | Resumo da interação |
| `id_usuario` | Inteiro | Chave estrangeira para `funcionario` (responsável) |

**Tabela: tarefa_marketing**
| Coluna | Tipo | Descrição |
|---|---|---|
| `id_tarefa` | Serial | Identificador único |
| `titulo` | String | Título da tarefa |
| `descricao` | String | Descrição detalhada |
| `id_responsavel` | Inteiro | Chave estrangeira para `funcionario` |
| `data_inicio` | Date | Data de início |
| `data_fim` | Date | Data de conclusão prevista |
| `status` | String | Status (Pendente, Em Andamento, Concluída) |
| `prioridade` | String | Prioridade (Baixa, Média, Alta) |

## 4. Fluxos

### Fluxo de Orçamento e Aprovação
1. A equipe cria um orçamento vinculado a um cliente.
2. O orçamento é enviado ao cliente (fora do sistema).
3. Se o cliente solicitar ajustes, o status do orçamento muda para `Ajuste` e a equipe edita os valores/itens (`PUT /orcamentos/{id}`).
4. Após aprovação formal, o orçamento é convertido em uma encomenda com status inicial `Fila` no Kanban; o evento `orcamento.aprovado.event` é publicado para o Financeiro.
5. **Alteração de Encomenda:** se o cliente solicitar mudança após o fechamento, a encomenda atual é encerrada e uma **nova ordem** é criada com novas estimativas e valor (regra do Financeiro).

### Fluxo do Kanban de Encomendas
1. A encomenda é criada e posicionada na coluna `Fila`.
2. Conforme avança na produção, a equipe move o cartão para `Produção`, `Acabamento`, `Pronto`.
3. Cada movimento gera registro em `historico_status_encomenda` (auditoria com usuário e data) e publica `encomenda.status.alterado.event`.
4. Ao ser entregue, o status muda para `Entregue` e o Financeiro é notificado para registrar a entrada.
5. O serviço também consome atualizações de Kanban do Produção (`producao.status.alterado.event`) para manter `status_kanban` da encomenda em sincronia.

### Fluxo de Registro Manual de Marketplace
1. A equipe recebe um pedido em uma plataforma externa.
2. Cria uma encomenda no sistema e vincula um registro em `registro_marketplace` com o `codigo_externo` da plataforma.
3. O valor da taxa é registrado para cálculo do lucro líquido (Financeiro).

### Fluxo de CRM e Marketing Interno
1. A equipe registra interações com clientes (ligações, e-mails, reuniões, WhatsApp) em `interacao_cliente`.
2. Tarefas de marketing são criadas e atribuídas aos responsáveis (`tarefa_marketing`).
3. O sistema notifica os responsáveis sobre prazos e mudanças de status (via Notification).

## 5. Endpoints

Recursos em PT-BR, protegidos por JWT (`@PreAuthorize`).

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/clientes` | Cadastra um novo cliente (PF ou PJ) |
| `GET` | `/clientes` | Lista clientes com filtros (nome, tags, tipo) |
| `GET` | `/clientes/{id}` | Retorna detalhes de um cliente |
| `PUT` | `/clientes/{id}` | Atualiza dados de um cliente (CRUD) |
| `DELETE` | `/clientes/{id}` | Remove um cliente (CRUD — sem encomendas vinculadas) |
| `POST` | `/orcamentos` | Cria um novo orçamento |
| `PUT` | `/orcamentos/{id}` | Atualiza orçamento (incluindo ajustes) |
| `POST` | `/encomendas` | Cria uma encomenda a partir de um orçamento aprovado |
| `PUT` | `/encomendas/{id}/kanban` | Move a encomenda no Kanban (muda status) |
| `GET` | `/encomendas` | Lista encomendas com filtros (status, cliente, data) |
| `GET` | `/encomendas/{id}/status` | Retorna o status atual e previsão de entrega da encomenda |
| `GET` | `/historico-status/{id_encomenda}` | Lista o histórico de status de uma encomenda |
| `POST` | `/marketplace` | Registra venda manual em marketplace |
| `GET` | `/marketplace` | Lista registros de marketplace (filtro por encomenda/plataforma) |
| `POST` | `/interacoes` | Registra interação com cliente |
| `GET` | `/interacoes/{id_cliente}` | Lista interações de um cliente |
| `POST` | `/tarefas-marketing` | Cria tarefa de marketing |
| `GET` | `/tarefas-marketing` | Lista tarefas com filtros (responsável, status) |
| `PUT` | `/tarefas-marketing/{id}` | Atualiza status da tarefa |
| `POST` | `/tags` | Cadastra tags de clientes |
| `GET` | `/tags` | Lista tags disponíveis |

## 6. Integrações

| Integração | Tipo | Direção | Evento / Uso |
|---|---|---|---|
| Produção & Projetos | Mensageria (RabbitMQ 5672) | Publica | `encomenda.criada.event` (insere ordem na fila de produção) |
| Produção & Projetos | Mensageria | Consome | `producao.status.alterado.event` (sincroniza `status_kanban` da encomenda) |
| Financeiro | Mensageria | Publica | `orcamento.aprovado.event`, dados de encomenda entregue e taxas de marketplace |
| Estoque & Suprimentos | Mensageria (opcional) | Consome | `estoque.consumo.realizado.event` (associar custo real à encomenda) |
| Notification | Mensageria | Publica | `encomenda.status.alterado.event`, alertas de prazos e novas interações |
| RH (Pessoas & RH) | REST | Consome | Dados de funcionários para `id_responsavel`/`id_usuario` e validação de permissões |
| Auth & Identity | REST/JWT | Consome | Autenticação e claims (`id_user`, `role`, `setor`) |
| Service Discovery | Eureka | — | Registro em `http://localhost:8761/eureka` |

### Eventos publicados (padrão `<contexto>.<entidade>.<acao>.event`, exchange `fablab.vendas`)
| Evento | Payload | Destino |
|---|---|---|
| `encomenda.criada.event` | `{ idEncomenda, idCliente, valorFinal, dataCriacao }` | Produção |
| `encomenda.status.alterado.event` | `{ idEncomenda, statusAnterior, statusNovo, dataAlteracao }` | Produção + Notification |
| `orcamento.aprovado.event` | `{ idOrcamento, idCliente, valorTotal }` | Financeiro |

## 7. RBAC

Autenticação via JWT com claims `id_user`, `role`, `setor` (Auth & Identity). Aplicação via `@PreAuthorize`. Níveis: `0-Admin`, `1-Bolsista`, `2-Voluntário`, `3-Estagiário`, `4-Recrutando`.

| Recurso (Endpoint) | Admin | Bolsista | Voluntário | Estagiário | Recrutando |
|---|---|---|---|---|---|
| `POST/PUT/DELETE /clientes` | ✅ | ✅ | ✅ | — | — |
| `GET /clientes` | ✅ | ✅ | ✅ | ✅ | — |
| `POST/PUT /orcamentos` | ✅ | ✅ | ✅ | — | — |
| `GET /orcamentos` | ✅ | ✅ | ✅ | ✅ | — |
| `POST /encomendas` | ✅ | ✅ | ✅ | — | — |
| `PUT /encomendas/{id}/kanban` | ✅ | ✅ | ✅ | — | — |
| `GET /encomendas`, `GET /{id}/status`, `GET /historico-status` | ✅ | ✅ | ✅ | ✅ | — |
| `POST/GET /marketplace` | ✅ | ✅ | ✅ | ✅ | — |
| `POST /interacoes`, `GET /interacoes/{id_cliente}` | ✅ | ✅ | ✅ | ✅ | — |
| `POST/GET/PUT /tarefas-marketing` | ✅ | ✅ | ✅ | ✅ | — |
| `POST/GET /tags` | ✅ | ✅ | ✅ | ✅ | — |

## 8. Não-Funcionais

- **Idioma:** recursos, endpoints e mensagens em PT-BR.
- **Persistência:** PostgreSQL `fablab_vendas` (usuário/senha `fablab`), Flyway para schema (migrations V1..V10).
- **Porta:** `8084` (configurável via `${PORT:8084}`), registrado no Eureka (`fablab/vendas-service`).
- **Mensageria:** RabbitMQ em `5672`, exchange `fablab.vendas`, eventos com padrão `<contexto>.<entidade>.<acao>.event`.
- **Validação:** DTOs com Bean Validation; **CPF e CNPJ validados** no cadastro/atualização de clientes (`cpf_cnpj` único).
- **Auditoria:** todas as movimentações de Kanban e alterações de orçamento registradas com usuário e data (`historico_status_encomenda`).
- **Isolamento:** operação 100% interna; nenhum endpoint exposto sem autenticação; o cliente não acessa o sistema.
- **Consistência:** uma encomenda só entra em escopo após aprovação formal do orçamento; alterações de escopo geram nova ordem (alinhado ao Financeiro).
- **Concorrência:** `@Version`/locks no Kanban para impedir movimentações simultâneas conflitantes.

## 9. Critérios de Sucesso

- [ ] Cliente PF/PJ criado/atualizado com validação de CPF/CNPJ e `cpf_cnpj` único; filtros por nome, tags e tipo funcionais.
- [ ] Orçamento criado com itens; `PUT /orcamentos/{id}` aplica ajustes (status `Ajuste`); aprovação publica `orcamento.aprovado.event`.
- [ ] Encomenda só é criada a partir de orçamento Aprovado; nasce com `status_kanban = Fila`.
- [ ] Movimentação no Kanban grava `historico_status_encomenda` e publica `encomenda.status.alterado.event`; concorrência não corrompe o cartão.
- [ ] Alteração de encomenda encerra a atual e cria nova ordem com novas estimativas/valor.
- [ ] Registro manual de marketplace vincula `codigo_externo` e `valor_taxa` à encomenda; dados disponíveis ao Financeiro.
- [ ] Interações e tarefas de marketing registradas com responsável; mudanças de status/prazo notificam via Notification.
- [ ] `encomenda.criada.event` consumível pelo Produção; `producao.status.alterado.event` sincroniza `status_kanban`.
- [ ] JWT/claims validadas em todos os endpoints protegidos; matriz RBAC aplicada.
- [ ] Migrations V1..V10 aplicam em banco limpo (PostgreSQL `fablab_vendas`); suíte de testes verde.

## 10. Fora de Escopo

- Portal/self-service de acesso ao cliente (operação 100% interna no MVP).
- Emissão fiscal (NF-e) — apenas recibo interno não fiscal.
- Integração automática com marketplaces (API de Mercado Livre/Shopee) — registro manual no MVP.
- Execução da produção em si e movimentação física interna (Produção & Projetos).
- Cálculo financeiro (contas a receber, custo, lucro) — Financeiro consome os eventos.
- Controle de estoque/consumo físico (Estoque & Suprimentos).
- Gestão de pessoas, credenciais e permissões (RH / Auth & Identity).
# Requirements: Estoque & Suprimentos Service

## Objetivo

O `estoque-service` (Estoque & Suprimentos) gerencia todo o inventário do Fab Lab: insumos, ferramentas e peças de reposição. Controla entradas e saídas, localização física dos itens, fornecedores e preços de compra, empréstimos de equipamentos (internos e externos) com devolução e alerta de atraso, e a **Lista de Materiais (BOM)** vinculada a produtos/serviços, com baixa automática de estoque quando a produção consome itens. Executa em Java 21 + Spring Boot com PostgreSQL, registra-se no Eureka (`http://localhost:8761/eureka`) na porta `8083`, e se comunica por REST (síncrono) e RabbitMQ (assíncrono).

## Funcionalidades

### F1 - Gestão de Itens
- Cadastrar, listar e atualizar itens via `item` (`id_item`, `nome`, `descricao`, `categoria` Insumo/Ferramenta/Peça, `unidade_medida`).
- Controle de saldo em **`quantidade_atual`** e alerta de reposição via **`estoque_minimo`**.
- Mapear localização física do item via **`localizacao_id`** (FK para `localizacao`).
- `GET /itens` com filtros por categoria, localização e estoque baixo (`quantidade_atual <= estoque_minimo`).
- Import/export de itens em CSV.

### F2 - Entradas de Estoque (Compras)
- Registrar entrada via `entrada_estoque` (`id_entrada`, `id_item`, `id_fornecedor`, `quantidade`, `valor_unitario`, `valor_total`, `data_entrada`, `nota_fiscal`, `observacao`).
- Atualizar `quantidade_atual` do item de forma consistente no mesmo fluxo.
- Publicar `compra.solicitada.event` para Financeiro (contas a pagar) e Notification.

### F3 - Saídas de Estoque (Manuais)
- Registrar saída via `saida_estoque` (`id_saida`, `id_item`, `quantidade`, `tipo_saida` Consumo/Perda/Ajuste/Empréstimo, `id_referencia`, `data_saida`, `observacao`).
- Validar saldo suficiente: impedir saída que deixe `quantidade_atual` negativa (controle de concorrência com locks).

### F4 - Localização Física
- Cadastrar e listar localizações via `localizacao` (`id_localizacao`, `armario`, `prateleira`, `caixa`, `descricao`).
- Consulta de item retorna localização física completa (armário, prateleira, caixa).

### F5 - Fornecedores
- Cadastrar e listar fornecedores via `fornecedor` (`id_fornecedor`, `nome`, `contato`, `cnpj`).
- Vincular fornecedor às entradas de estoque.

### F6 - Empréstimos com Devolução e Alerta de Atraso
- Registrar empréstimo via `emprestimo` (`id_emprestimo`, `id_item`, `id_pessoa`, `quantidade`, `data_emprestimo`, `data_devolucao_prevista`, `data_devolucao_real`, `status` Ativo/Devolvido/Atrasado, `observacao`).
- `PUT /emprestimos/{id}/devolucao` registra devolução e atualiza `status`.
- `GET /emprestimos/atrasados` lista empréstimos vencidos.
- Scheduler diário `@Scheduled(cron="0 0 3 * * *")` (03:00) marca atrasados e publica `emprestimo.atrasado.event`.

### F7 - BOM / Lista de Materiais com Versões
- Criar, consultar e atualizar BOM via `lista_materiais` (`id_bom`, `id_produto_servico`, `nome`, `versao`, `editavel`).
- Itens da BOM em `item_bom` (`id_item_bom`, `id_bom`, `id_item`, `quantidade_prevista`, `quantidade_real`).
- Versão incremental da BOM com rastreabilidade; BOM editável durante a produção.

### F8 - Baixa Automática por Consumo (Fim da Produção)
- `POST /boms/{id}/consumo` registra consumo real (preenche `quantidade_real` dos itens da BOM).
- Ao concluir a produção, o Produção & Projetos envia evento de baixa; o serviço atualiza saldo dos itens e registra saídas com `tipo_saida = Consumo` e `id_referencia = id_encomenda`.
- Baixa com locks para evitar estoque negativo.

### F9 - Alerta de Estoque Mínimo
- Ao atualizar saldo (entrada/saída), se `quantidade_atual <= estoque_minimo`, publicar `estoque.baixo.event` (Notification).

## Integrações

| Integração | Tipo | Direção | Evento / Uso |
|---|---|---|---|
| Produção & Projetos | Mensageria (RabbitMQ 5672) / REST | Consome e Publica | Baixa automática de consumo (BOM) ao fim da produção; fornecimento da BOM por produto/serviço |
| Financeiro | Mensageria | Publica | `compra.solicitada.event` (contas a pagar / orçamentos) |
| Notification | Mensageria | Publica | `estoque.baixo.event`, `emprestimo.atrasado.event`, confirmações de movimentação |
| RH (Pessoas & RH) | REST | Consome | Dados de pessoas para empréstimos internos (`id_pessoa`) e validação de permissões |
| Vendas & CRM | REST | Consome | Dados de produtos/serviços para vinculação da BOM e encomendas (`id_referencia`) |
| Service Discovery | Eureka | — | Registro em `http://localhost:8761/eureka` |

## RBAC

Autenticação via JWT com claims `id_user`, `role`, `setor` (Auth & Identity). Aplicação via `@PreAuthorize`.

| Recurso (Endpoint) | Admin | Bolsista | Voluntário | Estagiário |
|---|---|---|---|---|
| `POST/PUT /itens`, `POST/GET /fornecedores`, `POST/GET /localizacoes` | ✅ | ✅ | — | — |
| `GET /itens`, `GET /itens/{id}` | ✅ | ✅ | ✅ | ✅ |
| `POST /entradas`, `POST /saidas` | ✅ | ✅ | — | — |
| `POST /emprestimos`, `PUT /emprestimos/{id}/devolucao` | ✅ | ✅ | ✅ | — |
| `GET /emprestimos/atrasados` | ✅ | ✅ | ✅ | ✅ |
| `POST/PUT /boms`, `POST /boms/{id}/consumo` | ✅ | ✅ | — | — |
| `GET /boms/{id}` | ✅ | ✅ | ✅ | ✅ |

## Não-Funcionais

- **Idioma:** recursos, endpoints e mensagens em PT-BR.
- **Persistência:** PostgreSQL `fablab_estoque` (usuário/senha `fablab`), Flyway para schema de migrations V1..V8.
- **Porta:** `8083`, registrado no Eureka (`fablab/estoque-service`).
- **Validação:** DTOs com Bean Validation (tamanhos, datas, enums, quantidades positivas).
- **Concorrência:** locks otimistas/pessimistas no saldo (`quantidade_atual`) para impedir estoque negativo em atualizações simultâneas.
- **Sincronia:** REST síncrono; assíncrono via RabbitMQ para eventos de alerta, compra e baixa.
- **Auditoria:** todas as movimentações registradas com data, hora e usuário responsável.
- **Configuração:** scheduler de atraso controlado por `estoque.alerta.emprestimo.scheduler.enabled` / `estoque.alerta.emprestimo.scheduler.cron`.
- **BOM editável:** manter histórico de versões para rastreabilidade.

## Critérios de Sucesso

- [ ] Item criado/atualizado com validação; localização física vinculada via `localizacao_id`.
- [ ] Entrada registra `entrada_estoque` e atualiza `quantidade_atual`; publica `compra.solicitada.event`.
- [ ] Saída valida saldo; operações concorrentes não geram `quantidade_atual` negativa.
- [ ] Empréstimo registrado com devolução prevista; devolução atualiza status e data real; scheduler 03:00 marca atrasados e publica `emprestimo.atrasado.event`.
- [ ] BOM com versão incremental; baixa por consumo atualiza saldo e registra saída `tipo_saida = Consumo` com `id_referencia`.
- [ ] Saldo `<= estoque_minimo` após movimentação publica `estoque.baixo.event`.
- [ ] Import/export CSV de itens funcional.
- [ ] JWT/claims validadas em todos os endpoints protegidos; matriz RBAC aplicada.

## Fora de Escopo

- Controle de lote, validade ou rastreabilidade por série (sem lote/validade no MVP).
- Execução/logística de produção em si (serviço Produção & Projetos).
- Definição de produto/serviço do Fab Lab e dados do cliente (Vendas & CRM).
- Cálculo financeiro de contas a pagar (Financeiro consome `compra.solicitada.event`).
- Gestão de pessoas e permissões (Auth & Identity / RH).

## Modelo de Dados

| Tabela | Colunas-chave |
|---|---|
| `localizacao` | `id_localizacao`, `armario`, `prateleira`, `caixa`, `descricao` |
| `item` | `id_item`, `nome`, `descricao`, `categoria`, `unidade_medida`, `quantidade_atual`, `estoque_minimo`, `localizacao_id` |
| `fornecedor` | `id_fornecedor`, `nome`, `contato`, `cnpj` |
| `entrada_estoque` | `id_entrada`, `id_item`, `id_fornecedor`, `quantidade`, `valor_unitario`, `valor_total`, `data_entrada`, `nota_fiscal`, `observacao` |
| `saida_estoque` | `id_saida`, `id_item`, `quantidade`, `tipo_saida` (Consumo/Perda/Ajuste/Empréstimo), `id_referencia`, `data_saida`, `observacao` |
| `emprestimo` | `id_emprestimo`, `id_item`, `id_pessoa`, `quantidade`, `data_emprestimo`, `data_devolucao_prevista`, `data_devolucao_real`, `status` (Ativo/Devolvido/Atrasado), `observacao` |
| `lista_materiais` | `id_bom`, `id_produto_servico`, `nome`, `versao`, `editavel` |
| `item_bom` | `id_item_bom`, `id_bom`, `id_item`, `quantidade_prevista`, `quantidade_real` |

## Regras de Negócio

- Não permitir saída/empréstimo que deixe `quantidade_atual` negativa (lock de saldo).
- Ao registrar entrada, saldo soma a `quantidade`; ao registrar saída, saldo subtrai.
- Alerta `estoque.baixo.event` disparado quando `quantidade_atual <= estoque_minimo` após movimentação.
- Empréstimo com `data_devolucao_prevista` vencida e `data_devolucao_real` nula = `status` Atrasado.
- Baixa automática usa `quantidade_real` da BOM; na ausência, usa `quantidade_prevista`.
- Cada alteração de BOM incrementa `versao` mantendo rastreabilidade.
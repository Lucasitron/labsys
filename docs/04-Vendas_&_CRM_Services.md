# Documentação Técnica - Vendas & CRM Service (Revisado)

## 1. Objetivo e Escopo
O `Vendas & CRM Service` é o microsserviço responsável pela gestão comercial do Fab Lab. Ele abrange o cadastro de clientes (PF e PJ), a criação e o acompanhamento de orçamentos e encomendas, o Kanban de status dos pedidos, o registro de vendas em marketplaces (de forma manual) e as rotinas internas de CRM e marketing da equipe. O cliente **não** possui acesso ao sistema; toda a operação é interna. O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale).

## 2. Responsabilidades
*   Cadastro e gestão de clientes (Pessoa Física e Pessoa Jurídica).
*   Criação e envio de orçamentos.
*   Gestão do ciclo de vida da encomenda: Orçamento → Aprovação (com ajustes) → Produção → Entrega → Pós-venda.
*   Emissão de recibo interno (não fiscal).
*   Gestão do Kanban de status das encomendas.
*   Registro manual de vendas realizadas em marketplaces.
*   CRM interno: registro de interações com clientes e tarefas de marketing.
*   Fornecimento de dados de encomendas para os serviços de Produção, Estoque e Financeiro.

## 3. Tecnologias e Padrões
*   **Linguagem/Framework:** Java + Spring Boot.
*   **Comunicação:** REST (síncrona) e Mensageria (assíncrona, para eventos de encomenda e atualizações de Kanban).
*   **Persistência:** PostgreSQL.
*   **Padrões:** DTOs, validação de CPF/CNPJ, controle de concorrência no Kanban.

## 4. Modelo de Dados (Tabelas)

*   **Tabela: cliente**
    *   `id_cliente`: Identificador único (Serial).
    *   `tipo_pessoa`: Tipo (PF ou PJ). (String).
    *   `nome_razao_social`: Nome completo ou razão social. (String).
    *   `cpf_cnpj`: Documento de identificação. (String, Único).
    *   `email`: E-mail de contato. (String).
    *   `telefone`: Telefone de contato. (String).
    *   `endereco`: Endereço completo. (String).
    *   `data_cadastro`: Data de cadastro. (Date).

*   **Tabela: tag_cliente**
    *   `id_tag`: Identificador único (Serial).
    *   `nome`: Nome da tag (ex: VIP, 3D, Laser). (String).
    *   `cor`: Cor para exibição. (String).

*   **Tabela: cliente_tag**
    *   `id_cliente_tag`: Identificador único (Serial).
    *   `id_cliente`: Chave estrangeira para `cliente`. (Inteiro).
    *   `id_tag`: Chave estrangeira para `tag_cliente`. (Inteiro).

*   **Tabela: orcamento**
    *   `id_orcamento`: Identificador único (Serial).
    *   `id_cliente`: Chave estrangeira para `cliente`. (Inteiro).
    *   `data_criacao`: Data de criação. (Date).
    *   `validade`: Data de validade do orçamento. (Date).
    *   `valor_total`: Valor total orçado. (Decimal).
    *   `status`: Status (Pendente, Aprovado, Recusado, Ajuste). (String).
    *   `observacoes`: Observações gerais. (String).

*   **Tabela: item_orcamento**
    *   `id_item_orcamento`: Identificador único (Serial).
    *   `id_orcamento`: Chave estrangeira para `orcamento`. (Inteiro).
    *   `descricao`: Descrição do produto/serviço. (String).
    *   `quantidade`: Quantidade solicitada. (Decimal).
    *   `valor_unitario`: Valor unitário. (Decimal).

*   **Tabela: encomenda**
    *   `id_encomenda`: Identificador único (Serial).
    *   `id_orcamento`: Chave estrangeira para `orcamento` (opcional). (Inteiro).
    *   `id_cliente`: Chave estrangeira para `cliente`. (Inteiro).
    *   `data_criacao`: Data de criação. (Date).
    *   `data_previsao_entrega`: Data prevista para entrega. (Date).
    *   `status_kanban`: Status atual no Kanban (ex: Fila, Produção, Acabamento, Pronto, Entregue). (String).
    *   `valor_final`: Valor final da encomenda. (Decimal).
    *   `observacoes`: Observações. (String).

*   **Tabela: historico_status_encomenda**
    *   `id_historico`: Identificador único (Serial).
    *   `id_encomenda`: Chave estrangeira para `encomenda`. (Inteiro).
    *   `status_anterior`: Status anterior. (String).
    *   `status_novo`: Novo status. (String).
    *   `data_alteracao`: Data e hora da alteração. (Timestamp).
    *   `id_usuario`: Chave estrangeira para `funcionario` (quem alterou). (Inteiro).
    *   `observacao`: Motivo ou observação da mudança. (String).

*   **Tabela: registro_marketplace**
    *   `id_registro`: Identificador único (Serial).
    *   `id_encomenda`: Chave estrangeira para `encomenda`. (Inteiro).
    *   `plataforma`: Nome do marketplace (ex: Mercado Livre, Shopee). (String).
    *   `codigo_externo`: Código do pedido na plataforma. (String).
    *   `data_venda`: Data da venda. (Date).
    *   `valor_taxa`: Valor da taxa cobrada pela plataforma. (Decimal).

*   **Tabela: interacao_cliente**
    *   `id_interacao`: Identificador único (Serial).
    *   `id_cliente`: Chave estrangeira para `cliente`. (Inteiro).
    *   `data_interacao`: Data e hora da interação. (Timestamp).
    *   `tipo`: Tipo (E-mail, Telefone, Reunião, WhatsApp). (String).
    *   `descricao`: Resumo da interação. (String).
    *   `id_usuario`: Chave estrangeira para `funcionario` (responsável). (Inteiro).

*   **Tabela: tarefa_marketing**
    *   `id_tarefa`: Identificador único (Serial).
    *   `titulo`: Título da tarefa. (String).
    *   `descricao`: Descrição detalhada. (String).
    *   `id_responsavel`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `data_inicio`: Data de início. (Date).
    *   `data_fim`: Data de conclusão prevista. (Date).
    *   `status`: Status (Pendente, Em Andamento, Concluída). (String).
    *   `prioridade`: Prioridade (Baixa, Média, Alta). (String).

## 5. Fluxos Principais

*   **Fluxo de Orçamento e Aprovação:**
    1.  A equipe cria um orçamento vinculado a um cliente.
    2.  O orçamento é enviado ao cliente (fora do sistema).
    3.  Se o cliente solicitar ajustes, o status do orçamento muda para "Ajuste" e a equipe edita os valores/itens.
    4.  Após aprovação, o orçamento é convertido em uma `encomenda` com status inicial no Kanban.
    5.  **Alteração de Encomenda:** Se o cliente solicitar mudança após o fechamento, a encomenda atual é encerrada e uma **nova ordem** é criada com novas estimativas e valor (conforme regra do Financeiro).

*   **Fluxo do Kanban de Encomendas:**
    1.  A encomenda é criada e posicionada na coluna "Fila".
    2.  Conforme avança na produção, a equipe move o cartão para as colunas seguintes (Produção, Acabamento, Pronto).
    3.  Cada movimento gera um registro em `historico_status_encomenda`.
    4.  Ao ser entregue, o status muda para "Entregue" e o `Financeiro Service` é notificado para registrar a entrada.

*   **Fluxo de Registro Manual de Marketplace:**
    1.  A equipe recebe um pedido em uma plataforma externa.
    2.  Cria uma encomenda no sistema e vincula um registro em `registro_marketplace` com o código externo.
    3.  O valor da taxa é registrado para cálculo do lucro líquido.

*   **Fluxo de CRM e Marketing Interno:**
    1.  A equipe registra interações com clientes (ligações, e-mails, reuniões) em `interacao_cliente`.
    2.  Tarefas de marketing (ex: "Criar post para Instagram", "Atualizar catálogo") são criadas e atribuídas aos responsáveis.
    3.  O sistema notifica os responsáveis sobre prazos e mudanças de status.

## 6. Endpoints Principais (Especificação)

*   **POST /clientes:** Cadastra um novo cliente (PF ou PJ).
*   **GET /clientes:** Lista clientes com filtros (nome, tags, tipo).
*   **GET /clientes/{id}:** Retorna detalhes de um cliente.
*   **POST /orcamentos:** Cria um novo orçamento.
*   **PUT /orcamentos/{id}:** Atualiza orçamento (incluindo ajustes).
*   **POST /encomendas:** Cria uma encomenda a partir de um orçamento aprovado.
*   **PUT /encomendas/{id}/kanban:** Move a encomenda no Kanban (muda status).
*   **GET /encomendas:** Lista encomendas com filtros (status, cliente, data).
*   **POST /marketplace:** Registra venda manual em marketplace.
*   **POST /interacoes:** Registra interação com cliente.
*   **GET /interacoes/{id_cliente}:** Lista interações de um cliente.
*   **POST /tarefas-marketing:** Cria tarefa de marketing.
*   **GET /tarefas-marketing:** Lista tarefas com filtros (responsável, status).
*   **PUT /tarefas-marketing/{id}:** Atualiza status da tarefa.
*   **POST /tags:** Cadastra tags de clientes.
*   **GET /tags:** Lista tags disponíveis.

## 7. Integrações com Outros Serviços

*   **Pessoas & RH Service:** Consulta dados de funcionários para atribuição de responsáveis (tarefas, interações) e valida permissões.
*   **Estoque & Suprimentos Service:** Consulta BOM para estimar custos no orçamento; notifica sobre consumo de itens na produção.
*   **Produção & Projetos Service:** Envia encomendas aprovadas para a fila de produção; recebe atualizações de status do Kanban de produção.
*   **Financeiro Service:** Envia dados de orçamentos aprovados, encomendas entregues e taxas de marketplace para registro de entradas.
*   **Notification Service:** Solicita envio de alertas internos (prazos de tarefas, mudanças de status, novas interações).

## 8. Segurança e Boas Práticas
*   **RBAC:** Aplicar a Matriz de Permissões. Admin edita tudo; Bolsistas/Voluntários visualizam e editam conforme atribuição; Estagiários visualizam; Recrutandos sem acesso.
*   **Validação de Documentos:** Validar CPF e CNPJ no cadastro de clientes.
*   **Isolamento de Dados:** O cliente não acessa o sistema. Nenhum endpoint deve ser exposto sem autenticação.
*   **Auditoria:** Todas as movimentações de Kanban e alterações de orçamento devem ser registradas com usuário e data.
*   **Consistência:** Garantir que uma encomenda só entre em produção após aprovação formal do orçamento.
*   **Alinhamento Financeiro:** Alterações de escopo devem gerar nova ordem, conforme regra definida no `Financeiro Service`.

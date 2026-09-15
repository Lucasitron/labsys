# Documentação Técnica - Estoque & Suprimentos Service (Revisado)

## 1. Objetivo e Escopo
O `Estoque & Suprimentos Service` é o microsserviço responsável pela gestão de todo o inventário do Fab Lab, incluindo insumos, ferramentas e peças de reposição. Ele controla entradas, saídas, empréstimos (internos e externos), localização física dos itens, fornecedores, preços de compra e a **Lista de Materiais (BOM)** vinculada a produtos e serviços. A baixa automática de estoque ocorre quando a produção consome itens conforme a BOM. O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale).

## 2. Responsabilidades
*   Cadastro e gestão de itens (insumos, ferramentas, peças de reposição).
*   Controle de quantidade total em estoque (sem lote ou validade).
*   Registro de entradas (compras) e saídas (consumo, perdas, ajustes).
*   Gestão de fornecedores e registro de preços de compra.
*   Mapeamento da localização física dos itens (armário, prateleira, caixa).
*   Gestão de empréstimos de equipamentos (internos e externos) com data de devolução e alerta de atraso.
*   Criação e manutenção da **Lista de Materiais (BOM)** por produto/serviço.
*   Fornecimento da BOM para o `Produção & Projetos Service` e recebimento de baixas automáticas conforme consumo real.
*   Emissão de alertas de estoque mínimo (quando aplicável).

## 3. Tecnologias e Padrões
*   **Linguagem/Framework:** Java + Spring Boot.
*   **Comunicação:** REST (síncrona) e Mensageria (assíncrona, para eventos de baixa de estoque e alertas).
*   **Persistência:** PostgreSQL.
*   **Padrões:** DTOs, Bean Validation, controle de concorrência para evitar estoque negativo.

## 4. Modelo de Dados (Tabelas)

*   **Tabela: item**
    *   `id_item`: Identificador único (Serial).
    *   `nome`: Nome do item. (String).
    *   `descricao`: Descrição detalhada. (String).
    *   `categoria`: Categoria (Insumo, Ferramenta, Peça). (String).
    *   `unidade_medida`: Unidade (ex: unidade, metro, kg). (String).
    *   `quantidade_atual`: Quantidade disponível em estoque. (Decimal).
    *   `estoque_minimo`: Quantidade mínima para alerta. (Decimal).
    *   `localizacao_id`: Chave estrangeira para `localizacao`. (Inteiro).

*   **Tabela: localizacao**
    *   `id_localizacao`: Identificador único (Serial).
    *   `armario`: Identificação do armário. (String).
    *   `prateleira`: Identificação da prateleira. (String).
    *   `caixa`: Identificação da caixa. (String).
    *   `descricao`: Descrição adicional do local. (String).

*   **Tabela: fornecedor**
    *   `id_fornecedor`: Identificador único (Serial).
    *   `nome`: Nome do fornecedor. (String).
    *   `contato`: Telefone/e-mail. (String).
    *   `cnpj`: CNPJ ou documento. (String).

*   **Tabela: entrada_estoque**
    *   `id_entrada`: Identificador único (Serial).
    *   `id_item`: Chave estrangeira para `item`. (Inteiro).
    *   `id_fornecedor`: Chave estrangeira para `fornecedor`. (Inteiro).
    *   `quantidade`: Quantidade recebida. (Decimal).
    *   `valor_unitario`: Valor unitário pago. (Decimal).
    *   `valor_total`: Valor total da entrada. (Decimal).
    *   `data_entrada`: Data da entrada. (Date).
    *   `nota_fiscal`: Número da nota fiscal (opcional). (String).
    *   `observacao`: Observações. (String).

*   **Tabela: saida_estoque**
    *   `id_saida`: Identificador único (Serial).
    *   `id_item`: Chave estrangeira para `item`. (Inteiro).
    *   `quantidade`: Quantidade retirada. (Decimal).
    *   `tipo_saida`: Tipo (Consumo, Perda, Ajuste, Empréstimo). (String).
    *   `id_referencia`: ID de referência externa (ex: id_encomenda, id_projeto). (Inteiro).
    *   `data_saida`: Data da saída. (Timestamp).
    *   `observacao`: Observações. (String).

*   **Tabela: emprestimo**
    *   `id_emprestimo`: Identificador único (Serial).
    *   `id_item`: Chave estrangeira para `item`. (Inteiro).
    *   `id_pessoa`: Chave estrangeira para `pessoa` (no Pessoas & RH). (Inteiro).
    *   `quantidade`: Quantidade emprestada. (Decimal).
    *   `data_emprestimo`: Data do empréstimo. (Date).
    *   `data_devolucao_prevista`: Data prevista para devolução. (Date).
    *   `data_devolucao_real`: Data efetiva da devolução. (Date).
    *   `status`: Status (Ativo, Devolvido, Atrasado). (String).
    *   `observacao`: Observações. (String).

*   **Tabela: lista_materiais (BOM)**
    *   `id_bom`: Identificador único (Serial).
    *   `id_produto_servico`: ID de referência ao produto ou serviço (no Vendas & CRM ou Produção). (Inteiro).
    *   `nome`: Nome da lista de materiais. (String).
    *   `versao`: Versão da BOM. (Inteiro).
    *   `editavel`: Indica se a BOM pode ser editada durante a produção. (Booleano).

*   **Tabela: item_bom**
    *   `id_item_bom`: Identificador único (Serial).
    *   `id_bom`: Chave estrangeira para `lista_materiais`. (Inteiro).
    *   `id_item`: Chave estrangeira para `item`. (Inteiro).
    *   `quantidade_prevista`: Quantidade necessária para produzir uma unidade. (Decimal).
    *   `quantidade_real`: Quantidade efetivamente consumida (preenchida durante a produção). (Decimal).

## 5. Fluxos Principais

*   **Fluxo de Entrada de Estoque (Compra Simples):**
    1.  O responsável pelo estoque registra uma entrada informando item, fornecedor, quantidade, valor unitário e data.
    2.  O serviço atualiza a `quantidade_atual` do item e registra a entrada em `entrada_estoque`.
    3.  Se o novo saldo ficar abaixo do `estoque_minimo`, um alerta é emitido via `Notification Service`.

*   **Fluxo de Saída Manual:**
    1.  O responsável registra uma saída por consumo, perda ou ajuste.
    2.  O serviço valida se há quantidade suficiente, atualiza o saldo e registra em `saida_estoque`.

*   **Fluxo de Empréstimo:**
    1.  O responsável registra um empréstimo informando item, pessoa, quantidade, data de devolução prevista.
    2.  O serviço reduz temporariamente a quantidade disponível (ou mantém em estoque com status "emprestado", conforme política).
    3.  Na devolução, o serviço atualiza o status para "Devolvido" e registra a data real.
    4.  Um job agendado verifica empréstimos com data prevista vencida e emite alertas de atraso via `Notification Service`.

*   **Fluxo de Baixa Automática via BOM (Kanban):**
    1.  Quando uma encomenda entra em produção, o `Produção & Projetos Service` solicita a BOM do produto/serviço ao Estoque.
    2.  Durante a produção, o responsável pode editar a BOM (adicionar, remover ou alterar quantidades) e registrar o consumo real.
    3.  Ao concluir a produção, o `Produção & Projetos Service` envia um evento de baixa com os itens e quantidades reais consumidas.
    4.  O Estoque Service atualiza o saldo dos itens e registra as saídas em `saida_estoque` com `tipo_saida = Consumo` e `id_referencia = id_encomenda`.

*   **Fluxo de Consulta de Localização:**
    1.  O usuário pesquisa um item e o sistema retorna a localização física completa (armário, prateleira, caixa).

## 6. Endpoints Principais (Especificação)

*   **POST /itens:** Cadastra um novo item.
*   **GET /itens:** Lista itens com filtros (categoria, localização, estoque baixo).
*   **GET /itens/{id}:** Retorna detalhes de um item.
*   **PUT /itens/{id}:** Atualiza dados de um item.
*   **POST /entradas:** Registra entrada de estoque (compra simples).
*   **POST /saidas:** Registra saída manual (consumo, perda, ajuste).
*   **POST /emprestimos:** Registra empréstimo de equipamento.
*   **PUT /emprestimos/{id}/devolucao:** Registra devolução.
*   **GET /emprestimos/atrasados:** Lista empréstimos em atraso.
*   **POST /fornecedores:** Cadastra fornecedor.
*   **GET /fornecedores:** Lista fornecedores.
*   **POST /boms:** Cria uma lista de materiais (BOM).
*   **GET /boms/{id}:** Retorna BOM de um produto/serviço.
*   **PUT /boms/{id}:** Atualiza BOM (inclusive durante produção).
*   **POST /boms/{id}/consumo:** Registra consumo real de itens da BOM.
*   **GET /localizacoes:** Lista localizações físicas.

## 7. Integrações com Outros Serviços

*   **Pessoas & RH Service:** Consulta dados de pessoas para empréstimos (internos) e valida permissões de acesso.
*   **Produção & Projetos Service:** Fornece BOM, recebe eventos de consumo real e baixa automática de estoque.
*   **Vendas & CRM Service:** Fornece dados de produtos/serviços para vinculação da BOM; recebe informações de encomendas para rastreamento de consumo.
*   **Financeiro Service:** Envia dados de entradas (compras) para registro de contas a pagar; recebe informações de valores para orçamentos.
*   **Notification Service:** Solicita envio de alertas de estoque baixo, atrasos de empréstimo e confirmações de movimentação.

## 8. Segurança e Boas Práticas
*   **RBAC:** Aplicar a Matriz de Permissões. Apenas Admin e responsáveis pelo estoque podem editar; bolsistas/voluntários visualizam; estagiários visualizam.
*   **Validação de Saldo:** Impedir saídas ou empréstimos que deixem o estoque negativo (exceto se política permitir).
*   **Concorrência:** Utilizar locks otimistas ou pessimistas para evitar conflitos em atualizações simultâneas de saldo.
*   **Auditoria:** Todas as movimentações (entradas, saídas, empréstimos, ajustes) devem ser registradas com data, hora e usuário responsável.
*   **BOM Editável:** Garantir que a BOM possa ser alterada durante a produção, mantendo histórico de versões para rastreabilidade.

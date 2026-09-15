# Documentação Técnica - Financeiro Service (Revisado)

## 1. Objetivo e Escopo
O `Financeiro Service` é o microsserviço responsável por toda a gestão financeira do Fab Lab. Ele controla entradas e saídas, contas a pagar e a receber, doações, recursos de projetos universitários, fluxo informativo de compras e o cálculo do custo real de produção (Custeio por Ordem de Produção). O acesso é **exclusivo do Admin**. O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale).

## 2. Responsabilidades
*   Controle completo de fluxo de caixa (entradas e saídas).
*   Gestão de contas a pagar e a receber (com vencimentos e baixas).
*   Registro e gestão de doações e recursos de projetos universitários.
*   Fluxo **informativo** de compras (o Admin visualiza, mas não bloqueia a operação).
*   Cálculo do custo real de produção (Materiais Diretos + Mão de Obra Direta + Custos Indiretos).
*   Definição centralizada dos valores/hora por nível de acesso (Admin).
*   Validação dos apontamentos de horas de produção (Admin).
*   Validação de **coerência** entre horas de presença, encomenda e projeto.
*   Geração de relatórios de saúde financeira.
*   Estrutura de abstração para futuro `Payment Service` (separado).

## 3. Tecnologias e Padrões
*   **Linguagem/Framework:** Java + Spring Boot.
*   **Comunicação:** REST (síncrona) e Mensageria (assíncrona, para eventos de encomendas e compras).
*   **Persistência:** PostgreSQL.
*   **Padrões:** DTOs, Bean Validation, Job Order Costing.

## 4. Modelo de Dados (Tabelas)

*   **Tabela: categoria_financeira**
    *   `id_categoria`: Identificador único (Serial).
    *   `nome`: Nome da categoria (ex: Vendas, Insumos, Salários, Doações). (String).
    *   `tipo`: Tipo (Receita ou Despesa). (String).
    *   `descricao`: Descrição da categoria. (String).

*   **Tabela: lancamento_financeiro**
    *   `id_lancamento`: Identificador único (Serial).
    *   `id_categoria`: Chave estrangeira para `categoria_financeira`. (Inteiro).
    *   `tipo`: Tipo (Entrada ou Saída). (String).
    *   `valor`: Valor do lançamento. (Decimal).
    *   `data_vencimento`: Data de vencimento. (Date).
    *   `data_pagamento`: Data efetiva do pagamento/recebimento. (Date).
    *   `status`: Status (Pendente, Pago, Atrasado, Cancelado). (String).
    *   `id_referencia_externa`: ID de referência (ex: id_encomenda, id_solicitacao_compra). (Inteiro).
    *   `observacao`: Observações. (String).

*   **Tabela: doacao_recurso**
    *   `id_doacao`: Identificador único (Serial).
    *   `tipo`: Tipo (Doação ou Projeto). (String).
    *   `origem`: Nome do doador ou órgão financiador. (String).
    *   `valor`: Valor recebido. (Decimal).
    *   `data_recebimento`: Data do recebimento. (Date).
    *   `id_projeto_associado`: Referência externa ao projeto universitário (opcional). (Inteiro).

*   **Tabela: valor_hora_nivel**
    *   `id_valor_hora`: Identificador único (Serial).
    *   `nivel_acesso`: Nível de acesso (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário). (Inteiro).
    *   `valor_hora`: Valor da hora trabalhada para o nível. (Decimal).
    *   `data_vigencia`: Data de início da vigência do valor. (Date).

*   **Tabela: parametro_overhead**
    *   `id_parametro`: Identificador único (Serial).
    *   `valor_taxa_hora`: Valor da taxa de custos indiretos por hora. (Decimal).
    *   `data_vigencia`: Data de início da vigência. (Date).

*   **Tabela: fechamento_encomenda**
    *   `id_fechamento`: Identificador único (Serial).
    *   `id_encomenda`: Referência externa à encomenda (Vendas & CRM). (Inteiro).
    *   `horas_estimadas`: Horas estimadas antes do início. (Decimal).
    *   `valor_fechado`: Valor acordado com o cliente. (Decimal).
    *   `data_fechamento`: Data do fechamento. (Date).
    *   `status`: Status (Aberta, Concluída, Cancelada). (String).

*   **Tabela: custo_encomenda**
    *   `id_custo`: Identificador único (Serial).
    *   `id_encomenda`: Referência externa à encomenda. (Inteiro).
    *   `custo_materiais`: Custo total dos materiais (da BOM). (Decimal).
    *   `custo_mao_obra`: Custo total da mão de obra (horas × valor/hora). (Decimal).
    *   `custo_overhead`: Custo indireto rateado (horas × taxa). (Decimal).
    *   `custo_total`: Soma dos custos. (Decimal).
    *   `valor_venda`: Valor cobrado do cliente. (Decimal).
    *   `margem_lucro`: Valor da venda - custo total. (Decimal).
    *   `data_calculo`: Data do cálculo. (Timestamp).

*   **Tabela: solicitacao_compra**
    *   `id_solicitacao`: Identificador único (Serial).
    *   `id_item_estoque`: Chave estrangeira para `item` (Estoque & Suprimentos). (Inteiro).
    *   `quantidade`: Quantidade solicitada. (Decimal).
    *   `valor_estimado`: Valor estimado da compra. (Decimal).
    *   `status`: Status (Registrada, Visualizada, Concluída). (String).
    *   `data_solicitacao`: Data da solicitação. (Date).

## 5. Fluxos Principais

*   **Fluxo de Contas a Pagar e Receber:**
    1.  Um lançamento é criado com data de vencimento e status "Pendente".
    2.  No vencimento, se não pago, um alerta é emitido via `Notification Service`.
    3.  Ao ser pago/recebido, o Admin registra a data de pagamento e o status muda para "Pago".

*   **Fluxo de Aprovação de Compras (Informativo):**
    1.  O `Estoque & Suprimentos Service` solicita uma compra.
    2.  O Financeiro registra a solicitação com status "Registrada".
    3.  O Admin visualiza no painel de resumo. Não há bloqueio.
    4.  Após a compra, o Financeiro lança a saída e o status muda para "Concluída".

*   **Fluxo de Cálculo de Custo (Job Order Costing):**
    1.  **Antes do início:** Responsável estima horas. Admin/Financeiro fecha o valor e as horas em `fechamento_encomenda`. A partir daqui, estão congelados.
    2.  **Durante a produção:** Horas reais são apontadas no `Pessoas & RH Service`. O Financeiro valida a **coerência**: `Horas Encomenda + Horas Projeto ≤ Horas Presença (RFID)` no mesmo dia. Se houver incoerência, o Admin é notificado.
    3.  **Na entrega:** O Financeiro calcula:
        *   `custo_mao_obra` = Σ (horas validadas × valor/hora do nível).
        *   `custo_overhead` = Σ (horas validadas × taxa de overhead).
        *   `custo_total` = custo_materiais + custo_mao_obra + custo_overhead.
    4.  **Alteração de Encomenda:** Se o cliente solicitar mudança, a encomenda atual é **encerrada** (custo calculado e congelado) e uma **nova ordem** é criada com novas estimativas e valor.

*   **Fluxo de Doações e Recursos:**
    1.  O Admin registra uma doação ou recurso de projeto com valor e origem.
    2.  O valor é lançado como entrada.
    3.  Relatórios separam doações de receitas operacionais.

*   **Fluxo de Pagamentos (Futuro):**
    1.  O sistema possui uma interface `PaymentProcessor` (abstrata) que pode ser implementada futuramente para Pix, Cartão, Boleto.
    2.  No MVP, o registro é 100% manual. A estrutura permite plugar um módulo de conciliação bancária sem refatorar o núcleo.

## 6. Endpoints Principais (Especificação)

*   **POST /lancamentos:** Cria um lançamento financeiro (entrada/saída).
*   **GET /lancamentos:** Lista lançamentos com filtros (data, status, categoria).
*   **PUT /lancamentos/{id}/pagamento:** Registra o pagamento/recebimento.
*   **POST /categorias:** Cadastra categoria financeira.
*   **GET /categorias:** Lista categorias.
*   **POST /doacoes-recursos:** Registra doação ou recurso de projeto.
*   **GET /doacoes-recursos:** Lista doações e recursos.
*   **POST /valores-hora:** Define valor/hora por nível (Admin).
*   **GET /valores-hora:** Lista valores/hora vigentes.
*   **POST /parametros-overhead:** Define taxa de overhead (Admin).
*   **POST /fechamento-encomenda:** Fecha valor e horas estimadas.
*   **GET /fechamento-encomenda/{id}:** Consulta fechamento.
*   **POST /solicitacoes-compra:** Registra solicitação (informativo).
*   **PUT /solicitacoes-compra/{id}/concluir:** Conclui compra.
*   **GET /custos-encomenda/{id}:** Retorna custo detalhado.
*   **GET /relatorios/fluxo-caixa:** Fluxo de caixa.
*   **GET /relatorios/dre:** DRE simplificado.
*   **GET /relatorios/lucratividade:** Lucratividade por encomenda.
*   **GET /relatorios/inadimplencia:** Contas a receber vencidas.
*   **GET /relatorios/doacoes-despesas:** Doações vs. Despesas.
*   **GET /relatorios/custo-maquina:** Custo por máquina.

## 7. Integrações com Outros Serviços

*   **Vendas & CRM Service:** Recebe dados de orçamentos aprovados, encomendas entregues e valores de venda.
*   **Estoque & Suprimentos Service:** Recebe dados de BOM e consumo real para cálculo de custo de materiais; recebe solicitações de compras para fluxo informativo.
*   **Pessoas & RH Service:** Consulta dados de funcionários (nível, horas trabalhadas) para cálculo de mão de obra; valida horas de produção.
*   **Notification Service:** Solicita alertas de vencimento, incoerência de horas e resumo de compras.

## 8. Segurança e Boas Práticas
*   **RBAC:** Acesso exclusivo do Admin (nível 0). Nenhum outro nível acessa o módulo financeiro.
*   **Coerência de Horas:** Regra de negócio obrigatória. O sistema deve rejeitar ou alertar apontamentos que excedam as horas de presença.
*   **Congelamento de Valores:** O valor e as horas estimadas de uma encomenda não podem ser alterados após o fechamento. Alterações geram nova ordem.
*   **Auditoria:** Todas as alterações de valores/hora, taxas de overhead e fechamentos devem ser registradas com usuário e data.
*   **Abstração de Pagamentos:** Manter `PaymentProcessor` isolado para futuro `Payment Service`.
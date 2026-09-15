# Documentação Técnica - Produção & Projetos Service (Revisado)

## 1. Objetivo e Escopo
O `Produção & Projetos Service` é o microsserviço responsável pela gestão operacional do Fab Lab. Ele gerencia projetos, tarefas, o Kanban de encomendas (com integração ao Estoque para baixa automática via BOM), o cadastro e histórico de uso de máquinas, e o **sistema completo de gestão 5S**. Este último foi ampliado com base no documento "Sistema de Organização do FabLab", incluindo gestão de setores, responsáveis rotativos, checklists de inspeção, advertências, penalidades, auditoria de mesas de projeto e período experimental. O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale).

## 2. Responsabilidades
*   Cadastro e gestão de projetos e tarefas.
*   Atribuição de tarefas a funcionários (com prazos e prioridades).
*   Gestão do Kanban de encomendas (status, movimentação, histórico).
*   Integração com o Estoque para baixa automática de insumos conforme BOM.
*   Cadastro de máquinas e registro de status (disponível, em uso, manutenção).
*   Registro do histórico de uso de máquinas (quem usou, quando, por quanto tempo).
*   **Gestão completa do sistema 5S:**
    *   Cadastro de setores, materiais, sinalizações e checklists (editáveis).
    *   Designação e rotação de responsáveis por setor.
    *   Realização de inspeções semanais (com turnos e checklists).
    *   Registro de não conformidades e geração de advertências.
    *   Controle de penalidades (3 advertências = suspensão).
    *   Auditoria de projetos em mesas individuais (totens, prazos, abandono).
    *   Gestão de período experimental (sem penalidades formais).
*   Emissão de eventos de atualização para os serviços de Vendas, Estoque e Pessoas.

## 3. Tecnologias e Padrões
*   **Linguagem/Framework:** Java + Spring Boot.
*   **Comunicação:** REST (síncrona) e Mensageria (assíncrona, para eventos de Kanban, baixa de estoque, notificações 5S e advertências).
*   **Persistência:** PostgreSQL.
*   **Padrões:** DTOs, Bean Validation, controle de concorrência no Kanban, geração de QR Code.

## 4. Modelo de Dados (Tabelas)

### 4.1 Produção e Projetos
*   **Tabela: projeto**
    *   `id_projeto`: Identificador único (Serial).
    *   `nome`: Nome do projeto. (String).
    *   `descricao`: Descrição detalhada. (String).
    *   `data_inicio`: Data de início. (Date).
    *   `data_fim_prevista`: Data prevista para término. (Date).
    *   `data_fim_real`: Data efetiva de término. (Date).
    *   `status`: Status (Planejado, Em Andamento, Concluído, Cancelado). (String).
    *   `id_responsavel`: Chave estrangeira para `funcionario` (Pessoas & RH). (Inteiro).

*   **Tabela: tarefa**
    *   `id_tarefa`: Identificador único (Serial).
    *   `id_projeto`: Chave estrangeira para `projeto`. (Inteiro).
    *   `titulo`: Título da tarefa. (String).
    *   `descricao`: Descrição detalhada. (String).
    *   `id_responsavel`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `data_inicio`: Data de início. (Date).
    *   `data_fim_prevista`: Data prevista para conclusão. (Date).
    *   `data_conclusao`: Data efetiva de conclusão. (Date).
    *   `status`: Status (Pendente, Em Andamento, Concluída, Atrasada). (String).
    *   `prioridade`: Prioridade (Baixa, Média, Alta). (String).

*   **Tabela: encomenda_kanban**
    *   `id_kanban`: Identificador único (Serial).
    *   `id_encomenda`: Referência externa à encomenda (Vendas & CRM). (Inteiro).
    *   `status`: Status atual (Fila, Produção, Acabamento, Pronto, Entregue). (String).
    *   `data_entrada_status`: Data e hora da entrada no status atual. (Timestamp).
    *   `id_responsavel`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `ordem`: Ordem de exibição na coluna. (Inteiro).

*   **Tabela: historico_kanban**
    *   `id_historico`: Identificador único (Serial).
    *   `id_encomenda`: Referência externa. (Inteiro).
    *   `status_anterior`: Status anterior. (String).
    *   `status_novo`: Novo status. (String).
    *   `data_alteracao`: Data e hora da alteração. (Timestamp).
    *   `id_usuario`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `observacao`: Motivo ou observação. (String).

### 4.2 Máquinas
*   **Tabela: maquina**
    *   `id_maquina`: Identificador único (Serial).
    *   `nome`: Nome da máquina. (String).
    *   `descricao`: Descrição. (String).
    *   `status`: Status (Disponível, Em Uso, Manutenção). (String).
    *   `localizacao`: Localização física. (String).

*   **Tabela: historico_uso_maquina**
    *   `id_uso`: Identificador único (Serial).
    *   `id_maquina`: Chave estrangeira para `maquina`. (Inteiro).
    *   `id_funcionario`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `data_inicio`: Data e hora de início do uso. (Timestamp).
    *   `data_fim`: Data e hora de término do uso. (Timestamp).
    *   `horas_uso`: Total de horas utilizadas. (Decimal).
    *   `observacao`: Observações. (String).

### 4.3 Sistema 5S (Baseado no PDF)
*   **Tabela: setor**
    *   `id_setor`: Identificador único (Serial).
    *   `numero`: Número do setor (ex: 1, 2, 3). (Inteiro).
    *   `nome`: Nome do setor (ex: Bancada de eletrônica). (String).
    *   `descricao`: Descrição resumida das responsabilidades. (String).
    *   `observacoes`: Dicas e observações gerais. (String).
    *   `foto_correto_url`: Link para a foto de como fazer. (String).
    *   `foto_incorreto_url`: Link para a foto de como NÃO fazer. (String).
    *   `ativo`: Se o setor está em uso. (Booleano).

*   **Tabela: setor_material**
    *   `id_material`: Identificador único (Serial).
    *   `id_setor`: Chave estrangeira para `setor`. (Inteiro).
    *   `descricao`: Nome do material/equipamento. (String).
    *   `quantidade`: Quantidade esperada. (Decimal).

*   **Tabela: setor_sinalizacao**
    *   `id_sinalizacao`: Identificador único (Serial).
    *   `id_setor`: Chave estrangeira para `setor`. (Inteiro).
    *   `texto`: Texto da sinalização impressa. (String).

*   **Tabela: setor_checklist**
    *   `id_checklist`: Identificador único (Serial).
    *   `id_setor`: Chave estrangeira para `setor`. (Inteiro).
    *   `item`: Descrição do item a ser verificado. (String).
    *   `ativo`: Se o item está ativo. (Booleano).

*   **Tabela: setor_responsavel**
    *   `id_responsavel`: Identificador único (Serial).
    *   `id_setor`: Chave estrangeira para `setor`. (Inteiro).
    *   `id_funcionario`: Chave estrangeira para `funcionario` (Pessoas & RH). (Inteiro).
    *   `data_inicio`: Data de início da responsabilidade. (Date).
    *   `data_fim`: Data de término (para rotação). (Date).
    *   `ativo`: Se é o responsável atual. (Booleano).

*   **Tabela: inspecao_5s**
    *   `id_inspecao`: Identificador único (Serial).
    *   `id_setor`: Chave estrangeira para `setor`. (Inteiro).
    *   `id_inspetor`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `data_inspecao`: Data da inspeção. (Date).
    *   `turno`: Turno da inspeção (Manhã/Tarde). (String).
    *   `status`: Status (OK, NAO_CONFORME). (String).
    *   `observacoes`: Observações gerais. (String).

*   **Tabela: item_inspecao_5s**
    *   `id_item_inspecao`: Identificador único (Serial).
    *   `id_inspecao`: Chave estrangeira para `inspecao_5s`. (Inteiro).
    *   `id_checklist`: Chave estrangeira para `setor_checklist`. (Inteiro).
    *   `conforme`: Conforme ou Não Conforme. (Booleano).
    *   `observacao`: Observação específica. (String).

*   **Tabela: advertencia_membro**
    *   `id_advertencia`: Identificador único (Serial).
    *   `id_funcionario`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `id_inspecao`: Chave estrangeira para `inspecao_5s` (opcional). (Inteiro).
    *   `data`: Data da advertência. (Date).
    *   `motivo`: Motivo da advertência. (String).
    *   `tipo`: Tipo (VERBAL, FORMAL). (String).
    *   `contador`: Número sequencial da advertência (1, 2, 3). (Inteiro).
    *   `id_admin_registrou`: Chave estrangeira para `funcionario`. (Inteiro).

*   **Tabela: projeto_mesa**
    *   `id_projeto_mesa`: Identificador único (Serial).
    *   `id_funcionario`: Chave estrangeira para `funcionario` (dono do projeto). (Inteiro).
    *   `id_mesa`: Identificador da mesa física (ex: "Mesa 1", "Sala 59"). (String).
    *   `nome_projeto`: Nome do projeto. (String).
    *   `tipo_projeto`: Tipo (TCC, Pesquisa, etc.). (String).
    *   `prazo_execucao`: Prazo de execução. (Date).
    *   `data_inicio`: Data de início na mesa. (Date).
    *   `data_ultima_evolucao`: Data da última atualização/evolução registrada. (Date).
    *   `status`: Status (ATIVO, ABANDONADO, CONCLUIDO). (String).
    *   `qr_code_totem`: Código QR gerado para o totem físico. (String).

*   **Tabela: auditoria_projeto_mesa**
    *   `id_auditoria`: Identificador único (Serial).
    *   `id_projeto_mesa`: Chave estrangeira para `projeto_mesa`. (Inteiro).
    *   `data_auditoria`: Data da auditoria. (Date).
    *   `resultado`: Resultado (ATIVO, ABANDONADO). (String).
    *   `acao_tomada`: Descrição da ação (ex: "Totem removido, materiais recolhidos"). (String).
    *   `id_admin_responsavel`: Chave estrangeira para `funcionario`. (Inteiro).

*   **Tabela: parametro_5s**
    *   `id_parametro`: Identificador único (Serial).
    *   `chave`: Nome do parâmetro (ex: "dias_para_auditoria_projeto", "dia_semana_inspecao", "periodo_experimental_ativo", "rotacao_dias"). (String).
    *   `valor`: Valor do parâmetro. (String).
    *   `descricao`: Descrição do parâmetro. (String).

## 5. Fluxos Principais

*   **Fluxo de Criação e Gestão de Projetos:** Padrão, com prazos e tarefas atribuídas.
*   **Fluxo do Kanban de Encomendas:**
    1.  Encomenda aprovada é inserida no Kanban.
    2.  Ao entrar em "Produção", consulta a BOM no Estoque.
    3.  Ao concluir, envia evento de baixa automática de estoque.
*   **Fluxo de Registro de Uso de Máquinas:** Início e fim de uso, com cálculo de horas e atualização de status.
*   **Fluxo de Rotatividade de Responsáveis (5S):**
    1.  Admin configura período de rotação em `parametro_5s`.
    2.  Sistema notifica quando o período está próximo do fim.
    3.  Admin designa novos responsáveis em `setor_responsavel`. Recrutandos são excluídos automaticamente.
*   **Fluxo de Inspeção Semanal (5S):**
    1.  Inspetor acessa o sistema no dia configurado.
    2.  Para cada setor, o sistema carrega o checklist ativo.
    3.  Inspetor marca itens como Conforme ou Não Conforme.
    4.  Se houver Não Conformes, o sistema permite registrar uma advertência para o responsável.
    5.  O sistema verifica se o membro atingiu 3 advertências e dispara alerta crítico para o Admin.
*   **Fluxo de Auditoria de Projetos em Mesas (5S):**
    1.  Membro reserva mesa criando `projeto_mesa` e gerando `qr_code_totem`.
    2.  Sistema agenda auditoria automática se `data_ultima_evolucao` > parâmetro configurado.
    3.  Admin realiza auditoria. Se abandonado, status muda para "ABANDONADO" e totem é removido.
*   **Fluxo de Período Experimental (5S):**
    1.  Admin ativa `periodo_experimental_ativo = true`.
    2.  Não conformidades são registradas, mas não contabilizam advertências formais para penalidades.
    3.  Rotação pode ser configurada para ser mais rápida (ex: 14 dias).

## 6. Endpoints Principais (Especificação)

### Produção e Projetos
*   **POST /projetos:** Cria um novo projeto.
*   **GET /projetos:** Lista projetos com filtros (status, responsável).
*   **PUT /projetos/{id}:** Atualiza projeto.
*   **POST /tarefas:** Cria uma nova tarefa.
*   **PUT /tarefas/{id}:** Atualiza tarefa.
*   **GET /tarefas:** Lista tarefas com filtros.
*   **POST /kanban:** Insere encomenda no Kanban.
*   **PUT /kanban/{id}/mover:** Move encomenda no Kanban.
*   **GET /kanban:** Lista cartões do Kanban.

### Máquinas
*   **POST /maquinas:** Cadastra máquina.
*   **GET /maquinas:** Lista máquinas.
*   **PUT /maquinas/{id}/status:** Atualiza status da máquina.
*   **POST /maquinas/{id}/uso:** Registra início de uso.
*   **PUT /maquinas/{id}/uso/{id_uso}/fim:** Registra fim de uso.
*   **GET /maquinas/{id}/historico:** Lista histórico de uso.

### Sistema 5S
*   **POST /setores:** Cadastra setor.
*   **GET /setores:** Lista setores.
*   **PUT /setores/{id}:** Atualiza setor.
*   **POST /setores/{id}/checklist:** Adiciona item ao checklist.
*   **PUT /setores/{id}/checklist/{id_item}:** Edita item do checklist.
*   **POST /setores/{id}/responsaveis:** Designa responsável.
*   **GET /setores/{id}/responsaveis:** Lista responsáveis.
*   **POST /inspecoes-5s:** Cria inspeção semanal.
*   **GET /inspecoes-5s:** Lista inspeções.
*   **POST /advertencias:** Registra advertência.
*   **GET /advertencias/{id_funcionario}:** Lista advertências de um membro.
*   **POST /projetos-mesa:** Cria projeto em mesa individual.
*   **GET /projetos-mesa:** Lista projetos em mesas.
*   **PUT /projetos-mesa/{id}/evolucao:** Registra evolução do projeto.
*   **POST /auditorias-projeto-mesa:** Realiza auditoria de projeto em mesa.
*   **GET /parametros-5s:** Lista parâmetros do 5S.
*   **PUT /parametros-5s/{id}:** Atualiza parâmetro do 5S.

## 7. Integrações com Outros Serviços

*   **Vendas & CRM Service:** Recebe encomendas aprovadas; envia atualizações de status do Kanban.
*   **Estoque & Suprimentos Service:** Consulta BOM; envia consumo real para baixa automática.
*   **Pessoas & RH Service:** Consulta funcionários para atribuição de tarefas, responsáveis de setor e donos de projeto; notifica sobre advertências e suspensões.
*   **Notification Service:** Solicita alertas de prazos, mudanças de status, inspeções 5S, advertências, auditorias de projeto e planos de ação.
*   **Financeiro Service:** Não há integração direta, pois o cálculo de custo de produção é feito pelo Financeiro com base em horas e BOM.

## 8. Segurança e Boas Práticas
*   **RBAC:** Aplicar a Matriz de Permissões. Admin edita tudo; Bolsistas/Voluntários editam conforme atribuição; Estagiários visualizam (se atribuído); Recrutandos não participam do 5S.
*   **Controle de Concorrência:** Utilizar locks otimistas para movimentação no Kanban, atualização de status de máquinas e designação de responsáveis.
*   **Auditoria:** Todas as movimentações de Kanban, uso de máquinas, inspeções 5S, advertências e auditorias de projeto devem ser registradas com usuário e data.
*   **Integração com Estoque:** Garantir que a baixa automática só ocorra após a conclusão da produção e com a BOM final editada.
*   **Notificações:** Alertas de atraso de tarefas, planos de ação 5S, advertências e auditorias de projeto devem ser enviados via `Notification Service`.
*   **QR Code:** O QR Code do totem deve ser único e vinculado ao `id_projeto_mesa` para facilitar a consulta e auditoria.

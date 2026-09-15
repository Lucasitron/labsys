# Documentação Técnica - Pessoas & RH Service (Revisado)

## 1. Objetivo e Escopo
O `Pessoas & RH Service` é o microsserviço responsável pela gestão de todas as pessoas do Fab Lab, incluindo professores, alunos (bolsistas, voluntários e estagiários) e candidatos ao processo seletivo. Ele gerencia o ciclo de vida do usuário dentro do laboratório, desde o recrutamento até o desligamento, além de controlar o registro de horas (presença, encomenda e projeto), treinamentos e a evolução dos níveis de acesso. O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale).

## 2. Responsabilidades
*   Cadastro e gestão de pessoas (professores, alunos, clientes).
*   Gestão do processo seletivo (inscrição, triagem, entrevista e resultado final), com o tutor criando o candidato e gerando um `user` com status "Recrutando".
*   Registro e consolidação de horas de presença (via RFID), horas em encomendas e horas em projetos.
*   Gestão do módulo de treinamento (LMS), incluindo guias, documentações e avaliações simples por nota (sem certificação).
*   Controle da evolução dos níveis de acesso (apenas Admin pode alterar).
*   Manutenção do histórico de funções, departamentos e alterações de nível.
*   Recebimento de eventos de ponto do `Auth & Identity Service` (via RFID).
*   Fornecimento de dados de horas para o `Financeiro Service` para validação de coerência e cálculo de custo.

## 3. Tecnologias e Padrões
*   **Linguagem/Framework:** Java + Spring Boot.
*   **Comunicação:** REST (síncrona) e Mensageria (assíncrona, para receber eventos de ponto e emitir eventos de mudança de nível).
*   **Persistência:** PostgreSQL.
*   **Padrões:** DTOs, Bean Validation, integração com Auth para criação de credenciais.

## 4. Modelo de Dados (Tabelas)

*   **Tabela: pessoa**
    *   `id_pessoa`: Identificador único (Serial).
    *   `nome_completo`: Nome da pessoa. (String).
    *   `matricula`: Número de matrícula ou registro. (String, Único).
    *   `data_admissao`: Data de entrada no laboratório. (Date).
    *   `contato`: Telefone ou e-mail de contato. (String).
    *   `turno`: Turno de trabalho/estudo (ex: Manhã, Tarde, Noite). (String).
    *   `status`: Status atual (Ativo, Inativo, Recrutando). (Inteiro).

*   **Tabela: funcionario**
    *   `id_funcionario`: Identificador único (Serial).
    *   `id_pessoa`: Chave estrangeira para `pessoa`. (Inteiro).
    *   `nivel_acesso`: Nível de acesso (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário, 4-Recrutando). (Inteiro).
    *   `departamento`: Departamento ou área de atuação. (String).

*   **Tabela: tutor**
    *   `id_tutor`: Identificador único (Serial).
    *   `id_funcionario`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `turno`: Turno de atuação como tutor. (String).
    *   `qualificacao`: Nível de qualificação técnica (0-6). (Inteiro).

*   **Tabela: registro_ponto_diario**
    *   `id_registro`: Identificador único (Serial).
    *   `id_funcionario`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `data`: Data do registro. (Date).
    *   `hora_entrada`: Horário de entrada (via RFID). (Timestamp).
    *   `hora_saida`: Horário de saída (via RFID). (Timestamp).
    *   `total_horas`: Total de horas de presença no dia. (Decimal).

*   **Tabela: apontamento_horas**
    *   `id_apontamento`: Identificador único (Serial).
    *   `id_funcionario`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `tipo`: Tipo de apontamento (ENCOMENDA, PROJETO). (String).
    *   `id_referencia`: ID da encomenda ou do projeto. (Inteiro).
    *   `data`: Data da atividade. (Date).
    *   `horas_trabalhadas`: Quantidade de horas dedicadas. (Decimal).
    *   `descricao_atividade`: Descrição do que foi feito. (String).
    *   `status`: Status (PENDENTE, VALIDADO, REJEITADO). (String).
    *   `id_admin_validador`: Chave estrangeira para `funcionario` (Admin que validou). (Inteiro).
    *   `data_validacao`: Data da validação. (Timestamp).

*   **Tabela: processo_seletivo**
    *   `id_processo`: Identificador único (Serial).
    *   `id_candidato`: Chave estrangeira para `pessoa` (candidato). (Inteiro).
    *   `id_tutor`: Chave estrangeira para `funcionario` (tutor responsável). (Inteiro).
    *   `status_processo`: Status (Inscrito, Em Triagem, Entrevista, Aprovado, Reprovado). (String).
    *   `data_inscricao`: Data de inscrição. (Date).
    *   `resultado_final`: Resultado final do processo. (String).

*   **Tabela: treinamento**
    *   `id_treinamento`: Identificador único (Serial).
    *   `titulo`: Título do treinamento. (String).
    *   `descricao`: Descrição do conteúdo. (String).
    *   `url_conteudo`: Link para o guia ou documentação. (String).
    *   `id_tutor`: Chave estrangeira para `funcionario` (tutor criador). (Inteiro).

*   **Tabela: avaliacao_treinamento**
    *   `id_avaliacao`: Identificador único (Serial).
    *   `id_treinamento`: Chave estrangeira para `treinamento`. (Inteiro).
    *   `id_funcionario`: Chave estrangeira para `funcionario` (aluno avaliado). (Inteiro).
    *   `nota`: Nota atribuída pelo tutor (ex: 0 a 10). (Decimal).
    *   `feedback`: Comentários do tutor. (String).
    *   `data_avaliacao`: Data da avaliação. (Date).

*   **Tabela: historico_nivel**
    *   `id_historico`: Identificador único (Serial).
    *   `id_funcionario`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `nivel_antigo`: Nível anterior. (Inteiro).
    *   `nivel_novo`: Novo nível. (Inteiro).
    *   `id_admin_alterou`: Chave estrangeira para `funcionario` (Admin). (Inteiro).
    *   `data_alteracao`: Data da alteração. (Timestamp).

## 5. Fluxos Principais

*   **Fluxo de Cadastro de Candidato (Processo Seletivo):**
    1.  O tutor acessa o sistema e cadastra um novo candidato.
    2.  O serviço cria um registro na tabela `pessoa` e `funcionario` com `nivel_acesso = 4` (Recrutando).
    3.  O `Auth & Identity Service` é notificado para criar as credenciais de login.
    4.  O tutor acompanha o processo na tabela `processo_seletivo`, atualizando o status.

*   **Fluxo de Registro de Ponto (RFID):**
    1.  O `Auth & Identity Service` recebe a leitura do cartão RFID e valida.
    2.  O serviço publica um evento assíncrono com o `id_funcionario` e o timestamp.
    3.  O `Pessoas & RH Service` consome o evento e atualiza a tabela `registro_ponto_diario` (calculando entrada, saída e total de horas).

*   **Fluxo de Apontamento de Horas (Encomenda/Projeto):**
    1.  O funcionário ou seu tutor registra as horas dedicadas a uma encomenda ou projeto.
    2.  O serviço cria um registro em `apontamento_horas` com status PENDENTE.
    3.  O Admin valida o apontamento (status VALIDADO ou REJEITADO).
    4.  O `Financeiro Service` consome os apontamentos validados para cálculo de custo e verificação de coerência com as horas de presença.

*   **Fluxo de Avaliação de Treinamento (LMS):**
    1.  O tutor acessa o treinamento e insere a nota e o feedback para o aluno.
    2.  O serviço registra em `avaliacao_treinamento`.
    3.  O aluno pode visualizar sua nota e feedback.

*   **Fluxo de Alteração de Nível de Acesso:**
    1.  Apenas o Admin (nível 0) acessa a funcionalidade de alteração de nível.
    2.  O Admin seleciona o funcionário e o novo nível.
    3.  O serviço atualiza o `nivel_acesso` na tabela `funcionario` e registra a alteração em `historico_nivel`.
    4.  O `Auth & Identity Service` é notificado para atualizar as permissões do token JWT.

## 6. Endpoints Principais (Especificação)

*   **POST /pessoas:** Cadastra uma nova pessoa (professor, aluno, cliente).
*   **GET /pessoas/{id}:** Retorna os dados de uma pessoa.
*   **PUT /pessoas/{id}:** Atualiza os dados de uma pessoa.
*   **GET /funcionarios:** Lista todos os funcionários (com filtros por nível e departamento).
*   **POST /funcionarios:** Cadastra um novo funcionário (vinculado a uma pessoa).
*   **PUT /funcionarios/{id}/nivel:** Altera o nível de acesso (exclusivo para Admin).
*   **GET /funcionarios/{id}/horas:** Retorna o total de horas (presença, encomenda e projeto).
*   **POST /apontamentos-horas:** Registra horas em uma encomenda ou projeto.
*   **PUT /apontamentos-horas/{id}/validar:** Valida ou rejeita um apontamento (Admin).
*   **POST /processo-seletivo:** Inicia um novo processo seletivo (cria candidato com status "Recrutando").
*   **PUT /processo-seletivo/{id}:** Atualiza o status do processo seletivo.
*   **POST /treinamentos:** Cria um novo treinamento (tutor).
*   **POST /treinamentos/{id}/avaliacoes:** Registra a avaliação de um aluno (tutor).
*   **GET /treinamentos/{id}/avaliacoes:** Lista as avaliações de um treinamento.

## 7. Integrações com Outros Serviços

*   **Auth & Identity Service:** Recebe eventos de ponto (RFID) e notifica sobre criação de credenciais e alteração de níveis.
*   **Financeiro Service:** Fornece dados de apontamentos de horas validados para cálculo de custo e verificação de coerência com horas de presença.
*   **Produção & Projetos Service:** Consulta dados de funcionários para atribuição de tarefas e responsáveis; recebe referências de projetos para alocação de horas.
*   **Notification Service:** Solicita envio de e-mails ou notificações para candidatos aprovados, novos treinamentos ou alterações de nível.

## 8. Segurança e Boas Práticas
*   **RBAC:** O serviço deve validar o token JWT e aplicar a Matriz de Permissões para cada endpoint.
*   **Validação de Dados:** Todos os campos de entrada devem ser validados (ex: formato de e-mail, tamanho de strings, datas válidas).
*   **Auditoria:** Todas as alterações de nível e registros de ponto devem ser armazenados para consulta futura.
*   **Isolamento de Dados:** O serviço não deve armazenar senhas ou tokens; isso é responsabilidade exclusiva do `Auth & Identity Service`.
*   **Coerência de Horas:** O serviço deve fornecer endpoints que permitam ao `Financeiro Service` verificar se a soma das horas de encomenda e projeto não excede as horas de presença no mesmo dia.

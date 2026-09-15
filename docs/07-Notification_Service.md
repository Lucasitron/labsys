# Documentação Técnica - Notification Service (Revisado)

## 1. Objetivo e Escopo
O `Notification Service` é o microsserviço transversal responsável por enviar notificações aos usuários do sistema quando eventos que exigem atenção ocorrem. Ele suporta múltiplos canais (e-mail e WhatsApp), sendo que o WhatsApp estará pronto para integração futura, mas não será implementado no MVP. O serviço mantém um histórico de notificações que pode ser revisado pelo Admin e, após essa revisão, é movido para um banco de histórico com retenção máxima de 90 dias, sendo excluído automaticamente depois. O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale).

## 2. Responsabilidades
*   Consumir eventos de todos os microsserviços e disparar notificações.
*   Enviar notificações por e-mail (implementado no MVP).
*   Manter a estrutura pronta para envio via WhatsApp (não implementado no MVP).
*   Armazenar notificações ativas (não revisadas) para consulta do usuário e do Admin.
*   Permitir que o Admin revise o histórico de notificações.
*   Mover notificações revisadas para um banco de histórico com prazo de retenção de 90 dias.
*   Excluir automaticamente notificações do histórico após 90 dias.
*   Garantir que todas as notificações sejam obrigatórias (sem opt-out no MVP).
*   Registrar data, hora e status de envio e leitura.

## 3. Tecnologias e Padrões
*   **Linguagem/Framework:** Java + Spring Boot.
*   **Comunicação:** Mensageria (RabbitMQ) para consumo de eventos; REST para consulta e revisão.
*   **Persistência:** PostgreSQL (banco principal e banco de histórico separado, podendo ser o mesmo SGBD com schema/tabela distinta).
*   **Padrões:** DTOs, Bean Validation, consumo de eventos assíncronos.

## 4. Modelo de Dados (Tabelas)

*   **Tabela: notificacao**
    *   `id_notificacao`: Identificador único (Serial).
    *   `id_destinatario`: Chave estrangeira para `funcionario` (Pessoas & RH). (Inteiro).
    *   `canal`: Canal de envio (EMAIL, WHATSAPP). (String).
    *   `tipo_evento`: Tipo do evento que gerou a notificação (ex: ESTOQUE_BAIXO, EMPRESTIMO_ATRASADO, MUDANCA_KANBAN, APROVACAO_HORAS, PLANO_ACAO_5S, VENCIMENTO_FINANCEIRO, ADVERTENCIA_5S, AUDITORIA_PROJETO). (String).
    *   `assunto`: Assunto da notificação (para e-mail) ou título curto. (String).
    *   `mensagem`: Corpo da mensagem. (String).
    *   `id_referencia`: ID do objeto relacionado (ex: id_item, id_encomenda, id_plano, id_advertencia). (Inteiro).
    *   `status`: Status (PENDENTE, ENVIADA, LIDA). (String).
    *   `data_criacao`: Data e hora da criação. (Timestamp).
    *   `data_envio`: Data e hora do envio efetivo. (Timestamp).
    *   `data_leitura`: Data e hora da leitura pelo usuário. (Timestamp).

*   **Tabela: notificacao_historico**
    *   `id_historico`: Identificador único (Serial).
    *   `id_notificacao_original`: ID da notificação original. (Inteiro).
    *   `id_destinatario`: Chave estrangeira para `funcionario`. (Inteiro).
    *   `canal`: Canal utilizado. (String).
    *   `tipo_evento`: Tipo do evento. (String).
    *   `assunto`: Assunto. (String).
    *   `mensagem`: Corpo da mensagem. (String).
    *   `id_referencia`: ID de referência. (Inteiro).
    *   `data_criacao`: Data e hora da criação. (Timestamp).
    *   `data_envio`: Data e hora do envio. (Timestamp).
    *   `data_leitura`: Data e hora da leitura. (Timestamp).
    *   `data_revisao_admin`: Data e hora em que o Admin revisou. (Timestamp).
    *   `id_admin_revisor`: Chave estrangeira para `funcionario` (Admin). (Inteiro).

*   **Tabela: configuracao_canal**
    *   `id_configuracao`: Identificador único (Serial).
    *   `canal`: Canal (EMAIL, WHATSAPP). (String).
    *   `habilitado`: Indica se o canal está ativo. (Booleano).
    *   `parametros`: JSON com configurações (ex: host SMTP, porta, usuário). (String).

## 5. Fluxos Principais

*   **Fluxo de Recebimento de Evento e Envio:**
    1.  Um microsserviço publica um evento no RabbitMQ (ex: `estoque.baixo`, `emprestimo.atrasado`, `advertencia.5s`).
    2.  O Notification Service consome o evento.
    3.  Cria um registro na tabela `notificacao` com status PENDENTE.
    4.  Verifica a configuração do canal (e-mail habilitado, WhatsApp desabilitado).
    5.  Envia a notificação por e-mail (via SMTP configurado).
    6.  Atualiza o status para ENVIADA e registra `data_envio`.

*   **Fluxo de Leitura pelo Usuário:**
    1.  O usuário acessa suas notificações via endpoint.
    2.  Ao abrir, o status muda para LIDA e `data_leitura` é registrada.

*   **Fluxo de Revisão pelo Admin:**
    1.  O Admin acessa a lista de notificações (ativas ou históricas).
    2.  Para notificações que não precisam mais ser mantidas ativas, o Admin realiza a revisão.
    3.  O sistema move a notificação para a tabela `notificacao_historico`, registrando `data_revisao_admin` e `id_admin_revisor`.
    4.  A notificação original é removida da tabela `notificacao` (ou marcada como revisada).

*   **Fluxo de Expurgo Automático:**
    1.  Um job agendado (ex: diário) verifica na tabela `notificacao_historico` registros com `data_revisao_admin` superior a 90 dias.
    2.  Os registros são excluídos permanentemente.

*   **Fluxo de Integração Futura com WhatsApp:**
    1.  A tabela `configuracao_canal` terá o canal WHATSAPP com `habilitado = false`.
    2.  Quando implementado, basta habilitar e implementar o provedor de envio (ex: API do WhatsApp Business).
    3.  A estrutura de dados e fluxos já suportam o novo canal.

## 6. Endpoints Principais (Especificação)

*   **GET /notificacoes:** Lista notificações do usuário autenticado (com filtros por status e tipo).
*   **PUT /notificacoes/{id}/ler:** Marca notificação como lida.
*   **GET /notificacoes/admin:** Lista todas as notificações (para Admin).
*   **POST /notificacoes/admin/revisar:** Move notificações selecionadas para o histórico (Admin).
*   **GET /notificacoes/historico:** Lista notificações no histórico (Admin).
*   **GET /configuracoes-canal:** Lista configurações dos canais (Admin).
*   **PUT /configuracoes-canal/{id}:** Atualiza configuração de canal (Admin).
*   **POST /notificacoes/teste:** Envia notificação de teste (Admin).

## 7. Integrações com Outros Serviços

*   **Todos os serviços:** Consome eventos publicados no RabbitMQ para disparar notificações.
*   **Pessoas & RH Service:** Consulta dados do funcionário destinatário (nome, e-mail).
*   **Auth & Identity Service:** Valida permissões de Admin para revisão e configuração.

## 8. Segurança e Boas Práticas
*   **RBAC:** Apenas Admin pode revisar histórico e configurar canais. Usuários comuns veem apenas suas próprias notificações.
*   **Obrigatoriedade:** Não há opt-out no MVP; todas as notificações são enviadas.
*   **Retenção:** O histórico é mantido por no máximo 90 dias após a revisão do Admin, sendo excluído automaticamente.
*   **Auditoria:** Todas as revisões e alterações de configuração devem ser registradas com usuário e data.
*   **Segurança de Credenciais:** As configurações de SMTP e futuras credenciais do WhatsApp devem ser armazenadas de forma segura (ex: variáveis de ambiente ou cofre de segredos).
*   **Tratamento de Falhas:** Se o envio falhar, a notificação permanece como PENDENTE e pode ser reenviada manualmente pelo Admin.

# Documentação Técnica - Auth & Identity Service (Revisado)

## 1. Objetivo e Escopo
O `Auth & Identity Service` é o microsserviço responsável pela autenticação, autorização e gestão de identidade dos usuários do sistema. Ele atua como a porta de entrada para todas as requisições, garantindo que apenas usuários autenticados e autorizados possam acessar os demais serviços. Além disso, é o responsável por validar o acesso físico via RFID (ESP32) e registrar os pontos de entrada e saída. O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale).

## 2. Responsabilidades
*   Autenticação de usuários via e-mail/senha e via RFID.
*   Emissão, validação e renovação de tokens JWT.
*   Gestão de sessões e logout seguro (blacklist de tokens).
*   Fornecimento dos dados de identidade e permissões (RBAC) para os demais microsserviços.
*   Validação do cartão RFID para controle de acesso físico e registro de ponto.
*   Aplicação da Matriz de Permissões (RBAC) conforme o nível de acesso do usuário.
*   **Exclusão do MVP:** O campo `emailVer` foi descartado neste momento e não fará parte da estrutura.

## 3. Tecnologias e Padrões
*   **Linguagem/Framework:** Java + Spring Boot.
*   **Segurança:** Spring Security.
*   **Autenticação:** JWT (JSON Web Token).
*   **Hash de Senha:** BCrypt.
*   **Comunicação:** REST (síncrona) e Mensageria (assíncrona, para envio de eventos de ponto).
*   **Infraestrutura:** Docker Compose, Tailscale (VPN mesh).
*   **Padrão de Autorização:** RBAC (Role-Based Access Control).

## 4. Modelo de Dados (Tabelas)

*   **Tabela: login**
    *   `id`: Identificador único (Serial).
    *   `id_user`: Chave estrangeira que referencia o usuário no `Pessoas & RH Service`.
    *   `uuid`: Identificador único do cartão RFID. Utilizado para acesso físico e registro de ponto. (String, Único).
    *   `email`: E-mail do usuário. Utilizado para login. (String, Único).
    *   `nome_usuario`: Nome de usuário para login. (String, Único).
    *   `senha_hash`: Hash da senha do usuário. (String).
    *   `setor`: Setor ou departamento do usuário. (String).

*   **Tabela: user_permissions**
    *   `id`: Identificador único (Serial).
    *   `id_user`: Chave estrangeira que referencia o usuário no `login`.
    *   `role`: Nível de acesso (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário, 4-Recrutando). (Inteiro).
    *   `active`: Indica se a permissão está ativa. (Booleano).

*   **Tabela: token_blacklist**
    *   `id`: Identificador único (Serial).
    *   `token`: Token JWT que foi invalidado (Logout). (String).
    *   `expiry_date`: Data de expiração do token. (Timestamp).

*   **Tabela: access_log**
    *   `id`: Identificador único (Serial).
    *   `id_user`: Chave estrangeira que referencia o usuário no `login`.
    *   `uuid_rfid`: UUID do cartão utilizado. (String).
    *   `timestamp`: Data e hora do acesso/ponto. (Timestamp).
    *   `type`: Tipo de registro (ENTRADA, SAIDA, ACESSO_NEGADO). (String).

## 5. Fluxos Principais

*   **Fluxo de Autenticação Padrão (Login):**
    1. O usuário envia `email`/`nome_usuario` e `senha`.
    2. O serviço valida as credenciais contra a tabela `login`.
    3. Se válido, gera um token JWT contendo `id_user`, `role` e `setor`.
    4. Retorna o token para o cliente.

*   **Fluxo de Acesso via RFID (ESP32):**
    1. O ESP32 lê o UUID do cartão e envia para o endpoint de validação.
    2. O serviço busca o UUID na tabela `login`.
    3. Se encontrado, registra o evento na tabela `access_log` e publica um evento assíncrono para o `Pessoas & RH Service` (para registro de ponto).
    4. Retorna sucesso ou falha para o ESP32.

*   **Fluxo de Autorização (RBAC):**
    1. O API Gateway ou o próprio serviço intercepta a requisição.
    2. Valida a assinatura do JWT.
    3. Extrai o `role` (nível de acesso) do token.
    4. Compara o `role` com a Matriz de Permissões para o endpoint acessado.
    5. Libera ou bloqueia a requisição.

## 6. Endpoints Principais (Especificação)

*   **POST /auth/login:** Autentica usuário via e-mail/senha e retorna token JWT.
*   **POST /auth/logout:** Invalida o token JWT atual (adiciona à blacklist).
*   **POST /auth/validate-rfid:** Recebe o UUID do cartão (ESP32) e valida o acesso/ponto.
*   **GET /auth/me:** Retorna os dados do usuário autenticado e suas permissões.
*   **POST /auth/refresh:** Renova um token JWT expirado (usando refresh token).
*   **GET /auth/permissions:** Retorna a matriz de permissões para um determinado nível de acesso (consumido pelos demais serviços).

## 7. Integrações com Outros Serviços

*   **Pessoas & RH Service:** Notifica sobre criação de credenciais e alteração de níveis; recebe dados de funcionários para validação de identidade.
*   **API Gateway:** Fornece validação de token para roteamento seguro.
*   **Notification Service:** Não possui integração direta no MVP (já que `emailVer` foi descartado), mas pode ser usado futuramente para recuperação de senha.

## 8. Segurança e Boas Práticas
*   **Senhas:** Armazenadas apenas como hash BCrypt. Nunca em texto puro.
*   **JWT:** Assinado com chave secreta forte (HMAC SHA-256). Tempo de expiração curto (ex: 15 minutos) com uso de refresh token.
*   **RFID:** O UUID é tratado como dado sensível. A comunicação entre ESP32 e o serviço deve ser feita via HTTPS ou dentro da rede Tailscale.
*   **Blacklist:** Tokens invalidados (logout) são armazenados até sua data de expiração para evitar reuso.
*   **Menor Privilégio:** O token JWT conterá apenas as informações necessárias (id, role, setor) para que os outros serviços possam validar o acesso sem consultar o banco de dados de autenticação a cada requisição.

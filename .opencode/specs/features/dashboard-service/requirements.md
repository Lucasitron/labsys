# Requirements: Dashboard Service (App Shell)

## 1. Objetivo e Escopo

O `dashboard-service` é o **8º contexto** do sistema (não consta nos `docs/0X` estabelecidos — registrado em `docs/contract.gap.md` §2.7 e §8). Seu objetivo é oferecer uma **visão única e agregada** do estado operacional do laboratório (tarefas, KPIs, encomendas por status, máquinas, atividade e notificações) para o app shell em `8088`.

Natureza do serviço:

- **Agregador apenas de leitura**: não possui escrita própria nem domínio exclusivo.
- **Sem banco próprio**: sem datasource, sem Flyway, sem migrations, sem entidades JPA.
- **Sem mensageria**: não publica nem consome eventos (sem RabbitMQ).
- Consome dados de outros serviços via REST, descobertos por **Eureka**.
- Valida **JWT** (segredo compartilhado `JWT_SECRET`, issuer `fablab`) reencaminhado pelo gateway.

## 2. Funcionalidades

### F1 — Resumo executivo (Dashboard Summary)
`GET /dashboard/summary` retorna o agregado para a tela `/dashboard`.

- **tasks**: tarefas do dia (pendentes + concluídas) para exibição/app shell.
- **kpis**: lista de indicadores com `value`, `delta`, `deltaTone` e flag `restricted`.
- **ordersByStatus**: distribuição de encomendas por status (Kanban).
- **machinesByStatus**: distribuição de status das máquinas.
- **activity**: atividade recente.
- **unreadNotifications**: contagem de notificações não lidas (consultada no notification-service).

### F2 — KPIs com delta e flag `restricted`
- Cada KPI expõe valor atual, variação (`delta`), tom da variação (`deltaTone`: positivo/neutro/negativo) e se o dado é restrito (`restricted`).
- `restricted: true` indica campos sensíveis cuja exibição depende de papel (RBAC).

### F3 — Distribuição de encomendas por status (Kanban)
- `ordersByStatus` agrupa encomendas por status para montagem de colunas Kanban.
- Origens: dados de encomendas consumidos dos serviços de domínio via REST.

### F4 — Status de máquinas
- `machinesByStatus` apresenta situação das máquinas por status de operação.

### F5 — Atividade recente
- `activity` lista eventos recentes relevantes para o app shell.

### F6 — Tarefas do dia e conclusão
- `GET /tasks` lista as tarefas (escopo do dia/MVP).
- `PATCH /tasks/{id}` com corpo contendo `done` conclui/marca uma tarefa.
- `POST /tasks` permite criação de tarefa no escopo MVP.

### F7 — Contagem de notificações não lidas
- O front consulta `GET /api/notifications/unread/count` (via gateway → notification-service).
- No `summary`, a contagem não lida é agregada para o header do app shell.

### F8 — RBAC do front para campos restritos
- O app shell esconde/degrada campos cujo KPI tem `restricted: true` conforme o papel do usuário.
- A marcação da restrição vem no payload; a decisão de exibição é do front.

## 3. Tecnologias

- **Java 21 + Spring Boot** (mesma stack dos demais contextos).
- **Sem banco de dados** (sem datasource, sem JPA, sem Flyway).
- **REST via Eureka**: descoberta e chamada a `rh-service` e `notification-service`.
- **JWT compartilhado**: validação de Bearer com `JWT_SECRET` compartilhado e issuer `fablab`, via gateway reencaminhando o header `Authorization`.
- Porta configurável: `${PORT:8088}`; application name `dashboard-service`.

## 4. Modelo de Dados

**Nenhum.** O serviço não possui modelo de dados persistente:

- Não há entidades JPA, repositories, migrations nem schema.
- Apenas **controllers + DTOs + services** que agregam respostas de outros serviços.
- Nota sobre ausência de persistência: os dados exibidos são sempre uma fotografia obtida no momento da chamada (rest de consulta), sujeitos à disponibilidade dos serviços de origem. Sem persistência, não há consistência local em caso de falha de um serviço — a degradação é vazia.

## 5. Endpoints

| Método | Rota                     | Descrição                                                |
|--------|--------------------------|----------------------------------------------------------|
| GET    | `/dashboard/summary`     | Agregado do app shell (tasks, kpis, ordersByStatus, machinesByStatus, activity, unread count) |
| GET    | `/tasks`                 | Lista tarefas (dia/MVP)                                  |
| POST   | `/tasks`                 | Cria tarefa (MVP)                                       |
| PATCH  | `/tasks/{id}`            | Conclui/marca tarefa (`done`)                           |

- `DashboardController`: `@RequestMapping("/dashboard")` → `GET /summary`.
- `TaskController`: `@RequestMapping("/tasks")` → `PATCH /{id}` (done), `GET`, `POST`.
- Via gateway: `GET /api/dashboard/summary`, `GET /api/tasks/{id}` e `GET /api/notifications/unread/count` (rota `/api/dashboard/**` → `lb://dashboard-service`).

## 6. Integrações

- **rh-service (REST)**: insumos de pessoas/recursos humanos usados no resumo (KPIs de pessoas/resumo de RH). Chamada via Eureka; não é a origem de "people" da tela, apenas insumos para o resumo.
- **notification-service (REST)**: contagem de notificações não lidas (`unread count`) para o header do app shell.
- **gateway**: reencaminha `Bearer` do usuário para o `dashboard-service`; rota `/api/dashboard/**` → `lb://dashboard-service`. O front consome sempre os caminhos `/api/**` do gateway.

## 7. Segurança / RBAC

- **Token**: o gateway valida e reencaminha o Bearer; o `dashboard-service` valida o JWT (segredo compartilhado `JWT_SECRET`, issuer `fablab`).
- **Restrição**: campos/KPIs marcados `restricted: true` são filtrados ou obscurescidos conforme o papel; decisão de exibição no front.
- **Comportamento em erros**:
  - **401** (token inválido/expirado ou não autorizado) → app shell encerra a sessão (logout).
  - **5xx** (falha de serviço de origem) → o app shell **degrada para vazio**: KPIs/campos indisponíveis aparecem vazios, sem quebrar a tela.

## 8. Critérios de Sucesso

- `GET /dashboard/summary` responde com tasks, kpis (`value`/`delta`/`deltaTone`/`restricted`), ordersByStatus, machinesByStatus, activity e unread count.
- `PATCH /tasks/{id}` com `done` conclui a tarefa; `GET /tasks` reflete a alteração.
- `GET /api/notifications/unread/count` retorna a contagem não lida.
- Menu `/dashboard` do front mostra o resumo; campos `restricted` respeitam o papel.
- Sem banco nem mensageria: sem migrations, sem entidades, sem consumo de eventos.
- Falha de `rh-service`/`notification-service` não derruba o app shell: resposta degrada para vazio nos campos afetados (sem 5xx para o front). 401 dispara logout no front.
- Progresso MVP (docs/contract.gap.md §8): dashboard-summary e tasks concluídas via dashboard (done); today tasks; KPIs `ordersActive`/`tasks` degradam para zero no MVP.

## 9. Fora de Escopo

- **Persistência própria**: nenhum banco, nem cache duradouro local.
- **Eventos/mensageria**: sem publicar ou consumir eventos (sem RabbitMQ).
- **WebSocket**: sem push em tempo real; atualizações por polling/re-consulta.
- **Domínio próprio**: não cria nem grava entidades de encomendas, máquinas ou RH — apenas agrega.
- **Migrações Flyway / evolução de schema**.
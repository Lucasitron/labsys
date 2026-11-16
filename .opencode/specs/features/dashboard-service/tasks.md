# Tasks: Dashboard Service

> Status dos checkboxes reflete o andamento da implementação (não alterar sem registrar no `contract.gap.md` §8).

## Dependências Gerais

- [ ] Eureka com `rh-service` e `notification-service` registrados e acessíveis ao `dashboard-service`.
- [ ] Gateway com rota `/api/dashboard/**` → `lb://dashboard-service` reencaminhando `Authorization`.
- [ ] `JWT_SECRET` compartilhado com issuer `fablab` disponível.

---

## task-001 — Setup do projeto sem DB

**Escopo**
- Criar projeto Spring Boot 3.x (Java 21), application name `dashboard-service`, porta `${PORT:8088}`.
- Sem datasource, sem Flyway, sem dependências de JPA/repository.
- Registrar no Eureka; habilitar `spring-cloud` (discovery client).

**Entradas**
- `JWT_SECRET`, issuer `fablab`, config de Eureka.

**Saídas**
- Build compila; aplicação sobe em `:8088` com nome `dashboard-service`; sem configuração de banco.

**Critérios**
- [ ] `mvnw spring-boot:run` inicia sem erro de datasource.
- [ ] App registrado no Eureka como `dashboard-service`.
- [ ] Nenhuma dependência de banco/mensageria no build.

**Dependências**
- Nenhuma (primeira tarefa).

---

## task-002 — DTOs

**Escopo**
- Definir DTOs de resposta: `DashboardSummary`, `Kpi` (`value`, `delta`, `deltaTone`, `restricted`), `OrderByStatus`, `MachineByStatus`, `ActivityItem`, `TaskDto`.

**Entradas**
- Campos identificados no `contract.gap.md` §8 e no front (`GET /api/dashboard/summary`).

**Saídas**
- Pacote `dto` completo, imutável (records), serializável em JSON.

**Critérios**
- [ ] Records/DTOs cobrem tasks, kpis, ordersByStatus, machinesByStatus, activity e unread count.
- [ ] `Kpi` inclui `value`, `delta`, `deltaTone` e `restricted`.
- [ ] Serialização JSON validada em teste unitário.

**Dependências**
- task-001.

---

## task-003 — DashboardService (RestClient → rh + notification)

**Escopo**
- Implementar `DashboardService` com `RestClient` descoberto via Eureka (`lb://`).
- Chamar `rh-service` para insumos de RH/pessoas do resumo e `notification-service` para `unread/count`.
- Isolar cada chamada remota.

**Entradas**
- Formato de resposta de `rh-service` (resumo RH) e `notification-service` (`unread/count`).

**Saídas**
- Métodos que retornam DTOs agregados; falhas capturadas por serviço.

**Critérios**
- [ ] Chamada a `rh-service` retorna resumo; timeout de 2s.
- [ ] Chamada a `notification-service` retorna unread count; timeout de 2s.
- [ ] Falha em um serviço não propaga exceção ao chamador (degrada vazio).
- [ ] Chamadas independentes executadas de forma paralela/tolerante.

**Dependências**
- task-001, task-002.

---

## task-004 — Summary assembler + RBAC (`restricted`)

**Escopo**
- Montar `DashboardSummary` a partir dos DTOs de task-003.
- Preencher `kpis` com `value`, `delta`, `deltaTone`, `restricted`.
- Marcá-los `restricted: true` quando sensíveis ao papel.
- `ordersActive`/`tasks` degradam para zero no MVP.

**Entradas**
- DTOs de rh-service, notification-service e tarefas (task-005).

**Saídas**
- `DashboardSummary` completo para `GET /dashboard/summary`.

**Critérios**
- [ ] `summary` contém tasks, kpis, ordersByStatus, machinesByStatus, activity e unread count.
- [ ] KPIs sem origem assumem `value=0`/vazio no MVP.
- [ ] Campos restritos marcados `restricted: true`.

**Dependências**
- task-002, task-003.

---

## task-005 — TaskController in-memory (MVP)

**Escopo**
- Implementar `TaskController` (`@RequestMapping("/tasks")`) com armazenamento em memória.
- `GET /tasks` (lista do dia), `POST /tasks` (criação MVP), `PATCH /tasks/{id}` com corpo `done`.

**Entradas**
- Contrato de tarefas do front (`GET /api/tasks/{id}` e `PATCH` done).

**Saídas**
- Endpoints de tarefas funcionais sem banco (estado em memória).

**Critérios**
- [ ] `GET /tasks` lista tarefas do dia.
- [ ] `PATCH /tasks/{id}` com `done` marca a tarefa e reflete na listagem.
- [ ] `POST /tasks` cria tarefa no MVP.
- [ ] Não usa banco nem entidade JPA.

**Dependências**
- task-001, task-002.

---

## task-006 — DashboardController

**Escopo**
- Implementar `DashboardController` (`@RequestMapping("/dashboard")`) com `GET /summary`.
- Expor a rota `/dashboard/summary`.

**Entradas**
- `DashboardService` (task-003) e assembler (task-004) e tarefas (task-005).

**Saídas**
- `GET /summary` retornando o `DashboardSummary`.

**Critérios**
- [ ] `GET /dashboard/summary` responde 200 com payload completo.
- [ ] Falha de serviços de origem não gera 5xx (campos vazios).
- [ ] 401 sem token inválido.

**Dependências**
- task-003, task-004, task-005.

---

## task-007 — SecurityConfig JWT

**Escopo**
- Configurar Spring Security para validar Bearer JWT (segredo compartilhado `JWT_SECRET`, issuer `fablab`).
- `GET /dashboard/**` e `/tasks/**` autenticados.

**Entradas**
- `JWT_SECRET` e issuer `fablab`.

**Saídas**
- Requisições sem/limpo token retornam 401; válidas seguem para controllers.

**Critérios**
- [ ] Token com `JWT_SECRET` e issuer `fablab` é aceito.
- [ ] Token inválido/expirado → 401.
- [ ] RBAC de campo permanece no front (flag `restricted`).

**Dependências**
- task-001.

---

## task-008 — Testes

**Escopo**
- Testes de controllers (summary, tasks) e de degradação das chamadas remotas.
- Teste de autorização (401) e de serialização dos DTOs.

**Entradas**
- Implementação das task-003..007.

**Saídas**
- Suíte verde (`mvnw test`).

**Critérios**
- [ ] `GET /dashboard/summary` retorna aggregate com kpis e listas.
- [ ] `rh-service` fora do ar → campos do resumo vazios, resposta não é 5xx.
- [ ] `PATCH /tasks/{id}` done funciona; 401 sem token.
- [ ] Todos os testes verdes.

**Dependências**
- task-003, task-004, task-005, task-006, task-007.

---

## task-009 — Docs / registro do gap

**Escopo**
- Registrar progresso em `docs/contract.gap.md` §8 (dashboard-summary, tasks done, today tasks, KPIs degradando para zero no MVP).
- **Não modificar** os `docs/0X` existentes nem o código de outros contextos.

**Entradas**
- Estado final da implementação (tasks concluídas, endpoints, degradação).

**Saídas**
- §8 atualizada e consistente com o comportamento real.

**Critérios**
- [ ] §8 descreve summary + tasks concluídas via dashboard.
- [ ] Registra today tasks e KPIs `ordersActive`/`tasks` degradando para zero no MVP.
- [ ] Nenhuma alteração em outras seções/artefatos.

**Dependências**
- task-001..task-008.
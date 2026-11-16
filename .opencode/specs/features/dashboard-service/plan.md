# Plan: Dashboard Service

## 1. Arquitetura

**Agregador REST sem entidade** (`dashboard-service`, `8º contexto`).

```
front (app shell /dashboard)
   │  GET /api/dashboard/summary, /api/tasks/*, /api/notifications/unread/count
   ▼
gateway (rota /api/dashboard/** → lb://dashboard-service; reencaminha Bearer)
   ▼
dashboard-service :8088  (sem DB, sem RabbitMQ, sem entidades JPA)
   │  RestClient via Eureka  ┌─────────────────┐
   ├─────────────────────────→│ rh-service       │  insumos de RH/pessoas p/ resumo
   │                          └─────────────────┘
   │  RestClient via Eureka  ┌──────────────────────┐
   └─────────────────────────→│ notification-service │  unread count
                             └──────────────────────┘
```

- **Pacotes**: apenas `controller` + `dto` + `service` (sem `entity`, `repository`, `migration`).
- **RestClient** (Spring 6.1+/Boot 3.2+) com descoberta via **Eureka** (`lb://`).
- **Degradação sem entidade**: cada chamada remota é isolada; em falha o campo agregado vai vazio — nunca há 5xx para o front.
- **Configuração**: `application name = dashboard-service`, `server.port = ${PORT:8088}`.
- **JWT**: `JWT_SECRET` compartilhado, issuer `fablab`, Bearer reencaminhado pelo gateway.

## 2. Dependências

| Dependência          | Tipo         | Uso                                                       |
|----------------------|--------------|-----------------------------------------------------------|
| `rh-service`         | REST/Eureka  | insumos de pessoas/RH para o resumo e KPIs                |
| `notification-service` | REST/Eureka  | contagem de notificações não lidas (`unread/count`)      |
| `gateway`            | rotas        | expõe `/api/dashboard/**` → `lb://dashboard-service`; valida/reencaminha JWT |
| `spring-security` + `jwt` | lib      | validação do token compartilhado (`JWT_SECRET`, `issuer=fablab`) |
| Registro Eureka      | infra        | descoberta de `rh-service` e `notification-service`        |

Nenhuma dependência de dados (sem datasource, sem Flyway) nem de mensageria.

## 3. Ordem de Implementação

1. **Setup do projeto** sem DB: `pom.xml`/build minimalista, application name, porta `${PORT:8088}`, registro Eureka, dependência JWT. (task-001)
2. **DTOs** de resposta (summary, kpi, ordersByStatus, machinesByStatus, activity, task). (task-002)
3. **`DashboardService`**: `RestClient` para `rh-service` e `notification-service` com degradação vazia. (task-003)
4. **Summary assembler + RBAC** (`restricted`): monta o aggregate a partir dos DTOs; marca KPIs restritos. (task-004)
5. **`TaskController`** MVP em memória. (task-005)
6. **`DashboardController`** (`GET /dashboard/summary`). (task-006)
7. **`SecurityConfig`** JWT (`JWT_SECRET`, issuer `fablab`). (task-007)
8. **Testes** (controllers, degradação, autorização 401). (task-008)
9. **Docs**: registro no `docs/contract.gap.md` §8 (não alterar `docs/` existentes — registrar apenas o gap). (task-009)

## 4. Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| `rh-service`/`notification-service` fora do ar | **Timeout de 2s** por chamada remota; falha **degrada para vazio** no campo afetado; `summary` responde mesmo parcial. |
| Acoplamento ao formato de payload dos serviços de origem | DTOs próprios; mapeamento defensivo (campos ausentes → default/vazio). |
| KB no front de campos restritos | Flag `restricted` por KPI no payload; decisão de exibição fica no front; 401 → **logout**. |
| JWT não validado / issuer errado | `SecurityConfig` valida assinatura `JWT_SECRET` + issuer `fablab`; erro de autenticação → 401. |
| Chamadas síncronas em cadeia lentificam o `summary` | Chamadas remotas em paralelo (paralelismo controlado) + timeout de 2s. |
| Escopo de dados crescer (não ser mais agregador) | Mantém-se sem persistência; se houver necessidade futura, tendência é criar 9º contexto dedicado. |
| KPIs sem origem no MVP | `ordersActive`/`tasks` **degradam para zero** no MVP (documentado em `contract.gap.md` §8). |
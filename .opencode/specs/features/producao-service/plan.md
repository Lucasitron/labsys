# Plan: Produção & Projetos Service

## Arquitetura

- **Stack:** Java 21 + Spring Boot, build padrão do monorepo (Maven/Gradle do projeto), executado via Docker Compose local (Ubuntu Server + Tailscale).
- **Persistência:** PostgreSQL (banco `fablab_producao`, usuário/senha `fablab`) com Flyway (`V1..V18`). `spring.jpa.hibernate.ddl-auto=validate` — schema 100% definido pelas migrations.
- **Mensageria:** RabbitMQ (`amqp://localhost:5672`, credenciais `fablab`). Exchange tópica durável **`fablab.producao`**:
  - Publicação de `kanban.status.alterado.event`, `advertencia.registrada.event`, `projeto.mesa.abandonado.event` e eventos de produção para Financeiro/Vendas.
  - Consumo de encomenda aprovada (Vendas) e confirmações/baixa via Estoque, conforme contrato das filas.
- **Service Discovery:** registro no Eureka (`http://localhost:8761/eureka`), nome da aplicação `producao-service`, porta `8086`, `prefer-ip-address: true`.
- **API:** REST síncrona, DTOs com Bean Validation, respostas padronizadas (`ErrorResponse`, `GlobalExceptionHandler`), exceções de domínio (`ResourceNotFoundException`, `BadRequestException`, `ForbiddenException`).
- **Segurança:** JWT com claims `id_user`/`role`/`setor`, filtro `JwtAuthenticationFilter` + `SecurityConfig`, autorização por `@PreAuthorize` (matriz RBAC do requirements.md).
- **QR Code:** geração de `qr_code_totem` único (biblioteca de QR no backend) vinculado ao `id_projeto_mesa`.
- **Observabilidade:** Actuator (`health,info,metrics`), logging `com.fablab.producao: INFO` (DEBUG no perfil `dev`).

## Estrutura de Pacotes

```
com.fablab.producao
├── config      (SecurityConfig, JwtAuthenticationFilter, JwtProperties, RabbitMqConfig, SchedulerConfig)
├── controller  (Projeto, Tarefa, Kanban, Maquina, Setor, Inspecao5s, Advertencia, ProjetoMesa, AuditoriaProjetoMesa, Parametro5s)
├── dto         (requests/responses e eventos de domínio)
├── entity      (entidades JPA e enums com converters)
├── exception   (GlobalExceptionHandler, exceções de domínio)
├── mapper      (entity ↔ DTO)
├── repository  (Spring Data JPA)
└── service     (Projeto, Tarefa, Kanban, BaixaEstoque, Maquina, Setor, Inspecao5s, Advertencia, ProjetoMesa, Auditoria, Parametro5s, Scheduler5s, ProducaoEventPublisher, EncomendaListener)
```

## Dependências

| Dependência | Uso |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-data-jpa` | Persistência JPA |
| `org.flywaydb:flyway-core` (+ postgres) | Migrations V1..V18 |
| `org.postgresql:postgresql` | Driver PostgreSQL |
| `spring-boot-starter-amqp` | RabbitMQ / exchange `fablab.producao` |
| `spring-cloud-starter-netflix-eureka-client` | Discovery (`8761`) |
| `spring-boot-starter-security` | Filtro JWT + RBAC |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-actuator` | Health/info/metrics |
| Librearia de QR Code (ex.: `zxing`) | Geração do `qr_code_totem` |
| `spring-boot-starter-test` + `spring-rabbit-test` | Testes unitários/integração e mensageria |

## Ordem de Implementação

1. **Setup do serviço** — scaffold Spring Boot 21, `application.yml` (porta 8086, datasource `fablab_producao`, RabbitMQ, Eureka, JWT), dependências, Actuator.
2. **Migrations Flyway V1..V18** — schema completo (projeto, tarefa, encomenda_kanban, historico_kanban, maquina, historico_uso_maquina, setor, setor_material, setor_sinalizacao, setor_checklist, setor_responsavel, inspecao_5s, item_inspecao_5s, advertencia_membro, projeto_mesa, auditoria_projeto_mesa, parametro_5s, consumo_encomenda).
3. **Entidades + enums + converters JPA** — mapping para todas as tabelas com nomes reais (underlines); enums (`StatusProjeto`, `StatusTarefa`, `StatusKanban`, `TipoTurno`, `StatusInspecao`, `TipoAdvertencia`, `StatusProjetoMesa`, `ResultadoAuditoria`).
4. **Repositórios** — Spring Data JPA para todas as entidades, com filtros e queries de consulta (historico, responsáveis ativos, inspeções do dia, contador de advertências).
5. **DTOs + Mappers** — requests/responses com Bean Validation e mapeamento entity↔DTO; `ErrorResponse`/`GlobalExceptionHandler`.
6. **Segurança JWT + RBAC** — filtro JWT, `ProducaoPrincipal` (id_user, role, setor) e matriz `@PreAuthorize`.
7. **Projeto/Tarefa** — `ProjetoService`/`TarefaService` + controllers (`/projetos`, `/tarefas`).
8. **Kanban + histórico** — `KanbanService` (inserção por encomenda aprovada, movimentação com lock otimista, histórico) + `kanban.status.alterado.event`.
9. **Baixa de estoque via BOM** — `BaixaEstoqueService` (consulta BOM no Estoque, `consumo_encomenda`, notificação de baixa no fim da produção).
10. **Máquinas e uso** — `MaquinaService` (status, abertura/fechamento de uso, `horas_uso`, histórico).
11. **Setores 5S** — `SetorService` (cadastro, materiais, sinalizações, checklist, designação/rotação de responsáveis com lock).
12. **Inspeção semanal 5S** — `Inspecao5sService` (inspeção + `ItemInspecao5sService` por item do checklist ativo).
13. **Advertências e penalidades** — `AdvertenciaService` (contador 1/2/3, regra de 3º alerta crítico, `advertencia.registrada.event`).
14. **ProjetoMesa + auditoria (QR)** — `ProjetoMesaService` (QR totem, evolução), `AuditoriaService` (resultado, abandono, `projeto.mesa.abandonado.event`).
15. **Parâmetros 5S** — `Parametro5sService` (`GET/PUT /parametros-5s`).
16. **RabbitMQ (publisher + listener)** — `RabbitMqConfig` (exchange `fablab.producao`), `ProducaoEventPublisher`, `EncomendaListener` (encomenda aprovada do Vendas).
17. **Scheduler 5S** — `Scheduler5sService` (`@Scheduled` com properties `producao.5s.scheduler.enabled`/`.cron`): inspeção por `dia_semana_inspecao`, notificação de fim de rotação (`rotacao_dias`) e agenda de auditoria (`dias_para_auditoria_projeto`).
18. **Testes** — unitários (services) e integração (controllers + RabbitMQ real embutido + JWT).
19. **Docs/Docker** — perfil docker-compose, healthcheck, README e API.md do serviço.

## Riscos e Mitigações

| Risco | Mitigação |
|---|---|
| Perda de atualização em movimentações simultâneas no Kanban | Lock otimista (`@Version`) em `encomenda_kanban` + retry amigável de conflito |
| Baixa de estoque duplicada/incorreta | Transação única na conclusão da produção (`consumo_encomenda` + evento); baixa apenas com BOM final; idempotência por encomenda |
| Estoque negativo após baixa | A baixa é notificada ao Estoque, que valida saldo com locks (contrato `tipo_saida = Consumo`) |
| Enum/status persistidos de forma instável | Converters dedicados para todos os enums; `ddl-auto=validate` garante alinhamento com migrations |
| Eventos perdidos / RabbitMQ down | Retry no `RabbitTemplate`, bindings declarativos; publicação tolerante a falhas (log + não derruba a operação) |
| Encomenda já no Kanban inserida de novo | Validação de unicidade por `id_encomenda` na inserção |
| Recrutandos na rotação 5S | Exclusão automática na designação (filtro por `nivel_acesso != 4` na consulta ao RH) |
| Mesas abandonadas sem auditoria | Scheduler audita por `dias_para_auditoria_projeto`; evolução recente (`data_ultima_evolucao`) cancela a agenda |
| Advertência com contador errado | Contador calculado na transação (máximo + 1, cap 3) com lock no funcionário |
| Acesso indevido por nível | `@PreAuthorize` por endpoint conforme matriz RBAC (requirements.md) |
| `ddl-auto` divergente do schema | `validate` em produção; migrations como fonte única do schema |

## Integrações e Eventos

| Evento | Tipo | Payload | Consumidor |
|---|---|---|---|
| `kanban.status.alterado.event` | Publica (`fablab.producao`) | `{ idEncomenda, statusAnterior, statusNovo, idUsuario, dataAlteracao }` | Vendas (sincroniza `status_kanban`), Notification |
| `advertencia.registrada.event` | Publica (`fablab.producao`) | `{ idAdvertencia, idFuncionario, contador, tipo, motivo }` | Notification (e alerta crítico no 3º contador) |
| `projeto.mesa.abandonado.event` | Publica (`fablab.producao`) | `{ idProjetoMesa, idFuncionario, idMesa, nomeProjeto }` | Notification, RH (ação sobre o dono) |
| Evento de produção/conclusão | Publica (`fablab.producao`) | `{ idEncomenda, status }` | Financeiro (dispara cálculo de custo por horas + BOM) |
| Baixa automática via BOM | Publica (Mensageria/REST) | `consumo_encomenda` (itens da BOM final) | Estoque (baixa com `tipo_saida = Consumo`) |
| Encomenda aprovada | Consome (`fablab.vendas`) | `{ idEncomenda, idCliente, ... }` | Produção → insere cartão no Kanban |

## Decisões Técnicas

- **Base URL dos recursos (PT):** `/projetos`, `/tarefas`, `/kanban`, `/maquinas`, `/setores`, `/inspecoes-5s`, `/advertencias`, `/projetos-mesa`, `/auditorias-projeto-mesa`, `/parametros-5s`.
- **IDs longos** (`BIGSERIAL`) via JPA `GenerationType.IDENTITY`.
- **Datas em UTC**: `TIMESTAMPTZ` com offsets controlados por Jackson/JVM; campos `Date` para datas de calendário (inspeção, prazos).
- **Inserção de encomenda no Kanban** exclusiva para encomenda `Aprovada` (validação com Vendas pelo `id_encomenda`).
- **QR Code** do totem: conteúdo codificado = referência ao `id_projeto_mesa` (URL de consulta) + token único; totalizado em `qr_code_totem` (String).
- **Baixa de estoque** sempre registra `consumo_encomenda` localmente como trilha de auditoria, mesmo que a notificação ao Estoque falhe (reprocesso).
- **Fonte de verdade:** `docs/_archive/2026-10-19/06` — onde o plano divergir da doc, prevalece a doc `06`.
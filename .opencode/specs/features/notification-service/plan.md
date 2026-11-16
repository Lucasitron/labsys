# Plan: Notification Service

## Arquitetura

Serviço Java 21 + Spring Boot com arquitetura em camadas, seguindo os padrões dos demais serviços do FabLab:

```
notification-service (porta 8087)
├── Controller (REST, PT-BR) ── /notificacoes, /notificacoes/admin, /notificacoes/historico,
│                                /notificacoes/{id}/ler, /notificacoes/unread/count,
│                                /notificacoes/teste, /configuracoes-canal
├── Service (regras de negócio) ── NotificacaoService, EmailService, WhatsAppService (no-op),
│                                   ConfiguracaoCanalService, AcessoService (posse do recurso)
├── Repository (Spring Data JPA)
├── Entity + DTO (Bean Validation)
├── Migration (Flyway V1..V4)
├── Messaging (NotificacaoEventListener + DTOs de evento por routing key)
├── Config (RabbitMqConfig com filas/exchanges, SecurityConfig/JWT, NotificationProperties)
└── Scheduler (@Scheduled — ExpurgoHistoricoScheduler diário 03:00; ReenvioPendentesScheduler 30 min)
```

- **Persistência:** PostgreSQL `fablab_notification` (fablab/fablab), schema gerenciado por Flyway (migrations `V1__create_notificacao.sql`, `V2__create_notificacao_historico.sql`, `V3__create_configuracao_canal.sql`, `V4__seed_configuracao_canal.sql`); `ddl-auto: validate`.
- **Descoberta:** registro no Eureka em `http://localhost:8761/eureka` sob o nome `notification-service`.
- **Mensageria:** RabbitMQ em `5672` (fablab/fablab); o serviço **apenas consome** eventos — uma fila durável por routing key (`notificacao.<evento>`) ligada à exchange `fablab.notification`.
- **Segurança:** filtro JWT stateless; `/actuator/health` permitido; demais endpoints exigem Bearer válido; RBAC por `@PreAuthorize('hasRole(''ADMIN'')')` e validação de posse via `AcessoService`.
- **Configuração:** properties `notification.*` (email remetente/destino, `historico.retencao-dias`=90, `expurgo.scheduler.*`, `reenvio.scheduler.*`), todas sobrescrevíveis por variáveis de ambiente.

## Dependências

### Externas (infra)
- RabbitMQ `5672` — exchange `fablab.notification` com os 16 routing keys consumidos.
- PostgreSQL `fablab_notification` (fablab/fablab) com Flyway.
- Eureka `http://localhost:8761/eureka`.
- SMTP (Mailpit/MailHog) em `localhost:1025` — credenciais por env (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`).
- JWT compartilhado do Auth & Identity (`JWT_SECRET`, issuer `fablab`) para validar tokens.

### De outros serviços (publicações de evento)
- **Estoque & Suprimentos:** `estoque.baixo.event`, `emprestimo.atrasado.event`.
- **Vendas & CRM:** `encomenda.criada.event`, `encomenda.status.alterado.event`, `orcamento.aprovado.event`.
- **Financeiro:** `lancamento.vencido.event`, `compra.solicitada.event`.
- **Produção & Projetos:** `advertencia.registrada.event`, `projeto.mesa.abandonado.event`, `kanban.status.alterado.event`.
- **Pessoas & RH:** `certificado.solicitado.event`, `certificado.aprovado.event`, `certificado.rejeitado.event`, `extrato.mensal.horas.event`, `nivel.alterado.event`, `horas.validadas.event`.

### De infraestrutura do projeto
- Gateway route (`/api/notification/**`, `/api/notifications/**` → `lb://notification-service`) para exposição pública dos endpoints.
- Dashboard consome `GET /notificacoes/unread/count` para o badge `notificationsUnread`.

## Ordem de Implementação

1. **Setup do projeto** — boot do serviço Spring Boot na porta `${PORT:8087}`, config datasource `fablab_notification`, Flyway, RabbitMQ, SMTP, Eureka e properties `notification.*`.
2. **Migrations Flyway V1..V4** — `notificacao`, `notificacao_historico`, `configuracao_canal` e seed (EMAIL habilitado / WHATSAPP desabilitado).
3. **Entidades JPA e enums** — `Notificacao`, `NotificacaoHistorico`, `ConfiguracaoCanal`, `CanalNotificacao`, `StatusNotificacao`, `TipoEvento`, espelhando as colunas das migrations.
4. **DTOs e validação** — DTOs de resposta (ativa/histórico), requests (revisão, teste, configuração de canal) e DTOs de evento por routing key com Bean Validation.
5. **Repositórios** — `NotificacaoRepository`, `NotificacaoHistoricoRepository`, `ConfiguracaoCanalRepository` com consultas de filtro e de expurgo (`deleteByDataRevisaoAdminBefore`).
6. **RabbitMqConfig** — declaração da exchange `fablab.notification`, uma fila durável e binding por routing key consumido (16 eventos).
7. **NotificacaoEventListener** — `@RabbitListener` por fila criando a notificação `PENDENTE` correspondente (assunto/mensagem PT-BR e destinatário por evento).
8. **NotificacaoService** — registro de notificação, envio imediato com atualização de status, listagem (usuário/Admin), leitura, revisão para histórico, teste, expurgo e reenvio de pendentes.
9. **Senders de canal** — `EmailService` (JavaMailSender, valida canal habilitado) e `WhatsAppService` (interface + no-op no MVP); `ConfiguracaoCanalService`.
10. **Schedulers** — `ExpurgoHistoricoScheduler` (diário 03:00, habilitado por padrão) e `ReenvioPendentesScheduler` (a cada 30 min, desabilitado por padrão).
11. **Segurança e acesso** — `SecurityConfig` JWT stateless, filtro de autenticação, handlers de 401/403 e `AcessoService` (Admin acessa tudo; demais só as próprias).
12. **Controllers REST** — `NotificacaoController` (endpoints PT-BR) e `ConfiguracaoCanalController` (Admin), incluindo `unread/count`.
13. **Testes** — unitários (services, listeners, schedulers), de integração (endpoints, H2), de segurança (RBAC) e de messaging (consumo de eventos).
14. **Documentação** — `README.md`, `docs/API.md`, `docs/CHANGELOG.md` e `docs/ACTION_PLAN.md`/BUGS atualizadas; validação contra `docs/_archive/2026-10-19/07` e gateway.

## Riscos e Mitigações

| Risco | Mitigação |
|---|---|
| Evento consumido duas vezes cria notificação duplicada | Consumo idempotente por `(tipoEvento, idReferencia, dataCriacao)`; mensagens com idempotency key |
| Envio de e-mail falha (SMTP fora) e notificação fica parada | Status `PENDENTE` + `ReenvioPendentesScheduler` (a cada 30 min quando habilitado) |
| Canal desabilitado silenciosamente impede envio | `EmailService` valida `configuracao_canal` e loga aviso; notificação permanece `PENDENTE` para auditoria |
| Canais futuros (WhatsApp) quebrarem o fluxo atual | Contrato `WhatsAppService` + implementação no-op; enums e estrutura já suportam o canal |
| Histórico crescer indefinidamente | `ExpurgoHistoricoScheduler` diário 03:00 removendo registros com `data_revisao_admin` > retenção (90 dias) |
| Registro histórico inconsistente com a original na revisão | Revisão copia todos os campos (incl. datas de criação/envio/leitura) no mesmo `@Transactional`; remove a ativa |
| Usuário acessando notificação de terceiros | `AcessoService.podeAcessar(idDestinatario)` — Admin sempre; demais somente se `id_user == idDestinatario` (403) |
| Desalinhamento de nomes de eventos entre serviços | Contrato único de routing keys documentado em `docs/_archive/2026-10-19/07` e no mapa de eventos; nomes fixos em `RabbitMqConfig` |
| Perda de mensagens no RabbitMQ | Filas e exchange duráveis; `@RabbitListener` com `AcknowledgeMode` padrão; retry/DLQ quando necessário |
| Credenciais SMTP/JWT vazando para o código | Todas via variáveis de ambiente (`SMTP_*`, `JWT_SECRET`, `DB_*`); properties com defaults locais apenas |

## Integrações e Eventos

| Evento | Exchange (padrão FabLab) | Fila | Direção | Tipo gerado |
|---|---|---|---|---|
| `estoque.baixo.event` | `fablab.notification` | `notificacao.estoque.baixo` | Consome | `ESTOQUE_BAIXO` |
| `emprestimo.atrasado.event` | `fablab.notification` | `notificacao.emprestimo.atrasado` | Consome | `EMPRESTIMO_ATRASADO` |
| `encomenda.criada.event` | `fablab.notification` | `notificacao.encomenda.criada` | Consome | `ENCOMENDA_CRIADA` |
| `encomenda.status.alterado.event` | `fablab.notification` | `notificacao.encomenda.status` | Consome | `ENCOMENDA_STATUS_ALTERADO` |
| `orcamento.aprovado.event` | `fablab.notification` | `notificacao.orcamento.aprovado` | Consome | `ORCAMENTO_APROVADO` |
| `lancamento.vencido.event` | `fablab.notification` | `notificacao.lancamento.vencido` | Consome | `LANCAMENTO_VENCIDO` |
| `compra.solicitada.event` | `fablab.notification` | `notificacao.compra.solicitada` | Consome | `COMPRA_SOLICITADA` |
| `advertencia.registrada.event` | `fablab.notification` | `notificacao.advertencia` | Consome | `ADVERTENCIA_REGISTRADA` |
| `projeto.mesa.abandonado.event` | `fablab.notification` | `notificacao.projeto.mesa` | Consome | `PROJETO_MESA_ABANDONADO` |
| `kanban.status.alterado.event` | `fablab.notification` | `notificacao.kanban.status` | Consome | `ENCOMENDA_STATUS_ALTERADO` |
| `certificado.solicitado.event` | `fablab.notification` | `notificacao.certificado.solicitado` | Consome | `CERTIFICADO_SOLICITADO` |
| `certificado.aprovado.event` | `fablab.notification` | `notificacao.certificado.aprovado` | Consome | `CERTIFICADO_APROVADO` |
| `certificado.rejeitado.event` | `fablab.notification` | `notificacao.certificado.rejeitado` | Consome | `CERTIFICADO_REJEITADO` |
| `extrato.mensal.horas.event` | `fablab.notification` | `notificacao.extrato.mensal.horas` | Consome | `EXTRATO_MENSAL_HORAS` |
| `nivel.alterado.event` | `fablab.notification` | `notificacao.nivel.alterado` | Consome | `NIVEL_ALTERADO` |
| `horas.validadas.event` | `fablab.notification` | `notificacao.horas.validadas` | Consome | `HORAS_VALIDADAS` |

Serviços envolvidos: todos os bounded contexts publicam na `fablab.notification`; Auth (JWT/RBAC), RH (destinatários, certificados/horas/nível), Dashboard (`unread/count`), Gateway (rotas), Eureka (descoberta), RabbitMQ (mensageria), SMTP/Mailpit (envio de e-mail).

## Sequência (resumo)

```
Setup → Migrations V1..V4 → Entidades/enums → DTOs → Repositórios → RabbitMqConfig →
EventListener → NotificacaoService → Senders → Schedulers → Segurança → Controllers → Testes → Docs
```
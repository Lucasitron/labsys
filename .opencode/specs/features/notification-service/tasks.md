# Tasks: Notification Service

Base: `src/main/java/com/fablab/notification/` | Recursos em PT-BR | Migrations V1..V4 | Exchange `fablab.notification` (RabbitMQ 5672) | Porta `${PORT:8087}` | DB `fablab_notification` (fablab/fablab).

## task-001 — Setup do Projeto

- **Escopo:** raiz do projeto (`pom.xml`, `application.yml`, `NotificationApplication.java`, pacotes base)
- **Entradas:** Java 21 + Spring Boot; padrões FabLab (Eureka 8761, RabbitMQ 5672, SMTP 1025)
- **Saídas:** serviço bootável na porta `8087`; DB `fablab_notification`; Flyway habilitado; Eureka; RabbitMQ; SMTP; properties `notification.*`

Critérios:
- [ ] `pom.xml` com dependências web, data-jpa, validation, flyway (+ postgresql), amqp, mail, actuator, eureka-client, test
- [ ] `application.yml`: porta `${PORT:8087}`, datasource `fablab_notification` (fablab/fablab), `ddl-auto: validate`, `spring.rabbitmq` (host/porta 5672), `spring.mail` (SMTP 1025), `eureka`, `management` (health/info/metrics)
- [ ] Properties `notification.*`: `email.remetente` (no-reply@fablab.local), `email.destino-padrao` (admin@fablab.local), `historico.retencao-dias` (90), `expurgo.scheduler.enabled` (true) e `.cron` (0 0 3 * * *), `reenvio.scheduler.enabled` (false) e `.cron` (0 */30 * * * *)
- [ ] `@SpringBootApplication` `NotificationApplication` criado; `@ConfigurationProperties` `NotificationProperties` registrado
- [ ] Pacotes base `com.fablab.notification.{controller,service,repository,entity,config,messaging,dto,exception}` criados
- [ ] App sobe sem erros; conecta ao banco, RabbitMQ, SMTP e Eureka; `/actuator/health` acessível

**Dependências:** nenhuma (primeira).

## task-002 — Migrations Flyway V1..V4

- **Escopo:** `src/main/resources/db/migration/`
- **Entradas:** modelo de dados de `docs/_archive/2026-10-19/07` (notificacao, notificacao_historico, configuracao_canal)
- **Saídas:** schema V1..V4 aplicável pelo `flyway migrate`

Critérios:
- [ ] **V1 `notificacao`**: `id_notificacao` (BIGSERIAL PK), `id_destinatario` BIGINT, `canal` VARCHAR(20) NOT NULL, `tipo_evento` VARCHAR(40) NOT NULL, `assunto` VARCHAR(255) NOT NULL, `mensagem` VARCHAR(2000) NOT NULL, `id_referencia` BIGINT, `status` VARCHAR(20) NOT NULL, `data_criacao` TIMESTAMP NOT NULL, `data_envio` TIMESTAMP, `data_leitura` TIMESTAMP; índices por destinatário e status
- [ ] **V2 `notificacao_historico`**: `id_historico` (BIGSERIAL PK), `id_notificacao_original`, `id_destinatario`, `canal`, `tipo_evento`, `assunto`, `mensagem`, `id_referencia`, `data_criacao` NOT NULL, `data_envio`, `data_leitura`, `data_revisao_admin` NOT NULL, `id_admin_revisor`; índices por `data_revisao_admin` e destinatário
- [ ] **V3 `configuracao_canal`**: `id_configuracao` (BIGSERIAL PK), `canal` VARCHAR(20) NOT NULL UNIQUE, `habilitado` BOOLEAN NOT NULL, `parametros` VARCHAR(2000)
- [ ] **V4 seed configurações**: EMAIL `habilitado = TRUE` com parâmetros SMTP; WHATSAPP `habilitado = FALSE` com `{"provider":"nao-implementado"}`
- [ ] Migrations executam clean + `flyway migrate` sem erro; `ddl-auto: validate` consistente

**Dependências:** task-001.

## task-003 — Entidades JPA e Enums

- **Escopo:** `src/main/java/com/fablab/notification/entity/`
- **Entradas:** migrations V1..V4 (task-002)
- **Saídas:** entidades JPA espelhando colunas + enums de domínio

Critérios:
- [ ] `Notificacao` mapeada para `notificacao` (idNotificacao, idDestinatario, canal, tipoEvento, assunto, mensagem, idReferencia, status, dataCriacao, dataEnvio, dataLeitura)
- [ ] `NotificacaoHistorico` mapeada para `notificacao_historico` (inclui dataRevisaoAdmin, idAdminRevisor, idNotificacaoOriginal)
- [ ] `ConfiguracaoCanal` mapeada para `configuracao_canal` (idConfiguracao, canal, habilitado, parametros)
- [ ] Enum `CanalNotificacao`: `EMAIL`, `WHATSAPP`
- [ ] Enum `StatusNotificacao`: `PENDENTE`, `ENVIADA`, `LIDA`
- [ ] Enum `TipoEvento`: `ESTOQUE_BAIXO`, `EMPRESTIMO_ATRASADO`, `ENCOMENDA_CRIADA`, `ENCOMENDA_STATUS_ALTERADO`, `ORCAMENTO_APROVADO`, `LANCAMENTO_VENCIDO`, `ADVERTENCIA_REGISTRADA`, `PROJETO_MESA_ABANDONADO`, `NIVEL_ALTERADO`, `HORAS_VALIDADAS`, `COMPRA_SOLICITADA`, `CERTIFICADO_SOLICITADO`, `CERTIFICADO_APROVADO`, `CERTIFICADO_REJEITADO`, `EXTRATO_MENSAL_HORAS`, `TESTE`
- [ ] Enum `NivelAcesso` (ADMIN, BOLSISTA, VOLUNTARIO, ESTAGIARIO, RECRUTANDO) para claims JWT
- [ ] Testes unitários de mapeamento/enums passam

**Dependências:** task-002.

## task-004 — DTOs, Requests e Validação

- **Escopo:** `src/main/java/com/fablab/notification/dto/`
- **Entradas:** endpoints e eventos de `docs/_archive/2026-10-19/07`; Bean Validation
- **Saídas:** DTOs de resposta, requests de revisão/teste/configuração e DTOs de evento por routing key

Critérios:
- [ ] `NotificacaoResponse` (idNotificacao, idDestinatario, canal, tipoEvento, assunto, mensagem, idReferencia, status, dataCriacao, dataEnvio, dataLeitura); `NotificacaoHistoricoResponse` (com dataRevisaoAdmin e idAdminRevisor)
- [ ] `UnreadCountResponse` para `GET /notificacoes/unread/count`
- [ ] `RevisaoRequest` (`idsNotificacoes` `@NotEmpty`); `NotificacaoTesteRequest` (`idDestinatario`, `canal`, `assunto`, `mensagem`); `ConfiguracaoCanalRequest` (`habilitado` obrigatório, `parametros` opcional)
- [ ] DTOs de evento: `EstoqueBaixoEvent`, `EmprestimoAtrasadoEvent`, `EncomendaCriadaEvent`, `EncomendaStatusAlteradoEvent`, `OrcamentoAprovadoEvent`, `LancamentoVencidoEvent`, `CompraSolicitadaEvent`, `AdvertenciaRegistradaEvent`, `ProjetoMesaAbandonadoEvent`, `KanbanStatusAlteradoEvent`, `Certificado{Solicitado,Aprovado,Rejeitado}Event`, `ExtratoMensalHorasEvent`, `NivelAlteradoEvent`, `HorasValidadasEvent`
- [ ] `ErrorResponse` unificado; mensagens de erro em PT-BR
- [ ] Testes unitários de validação de DTOs passam

**Dependências:** task-003.

## task-005 — Repositórios

- **Escopo:** `src/main/java/com/fablab/notification/repository/`
- **Entradas:** entidades (task-003)
- **Saídas:** interfaces Spring Data JPA com consultas de filtro e de expurgo

Critérios:
- [ ] `NotificacaoRepository`: por destinatário; por destinatário+status; por destinatário+tipoEvento; combinações com `OrderByDataCriacaoDesc`; todas por status/tipo (Admin); contagem de não lidas (`unread/count`)
- [ ] `NotificacaoHistoricoRepository`: por canal; por tipoEvento; combinações com `OrderByDataRevisaoAdminDesc`; `deleteByDataRevisaoAdminBefore(LocalDateTime)` para expurgo (retorno do total removido)
- [ ] `ConfiguracaoCanalRepository`: listagem ordenada; `findByCanal` para verificação de canal habilitado
- [ ] Testes de integração de repositórios passam (H2 modo PostgreSQL)

**Dependências:** task-003.

## task-006 — RabbitMqConfig (exchange, filas e bindings)

- **Escopo:** `src/main/java/com/fablab/notification/config/`
- **Entradas:** contrato de eventos de `docs/_archive/2026-10-19/07` (16 routing keys); RabbitMQ 5672
- **Saídas:** exchange `fablab.notification` declarada; uma fila durável por evento (``notificacao.<evento>``) ligada via binding

Critérios:
- [ ] Constantes de routing keys: `estoque.baixo.event`, `emprestimo.atrasado.event`, `encomenda.criada.event`, `encomenda.status.alterado.event`, `orcamento.aprovado.event`, `lancamento.vencido.event`, `compra.solicitada.event`, `advertencia.registrada.event`, `projeto.mesa.abandonado.event`, `kanban.status.alterado.event`, `certificado.solicitado.event`, `certificado.aprovado.event`, `certificado.rejeitado.event`, `extrato.mensal.horas.event`, `nivel.alterado.event`, `horas.validadas.event`
- [ ] Constantes de filas `notificacao.estoque.baixo`, `notificacao.emprestimo.atrasado`, `notificacao.encomenda.criada`, `notificacao.encomenda.status`, `notificacao.orcamento.aprovado`, `notificacao.lancamento.vencido`, `notificacao.compra.solicitada`, `notificacao.advertencia`, `notificacao.projeto.mesa`, `notificacao.kanban.status`, `notificacao.certificado.solicitado`, `notificacao.certificado.aprovado`, `notificacao.certificado.rejeitado`, `notificacao.extrato.mensal.horas`, `notificacao.nivel.alterado`, `notificacao.horas.validadas`
- [ ] Exchange `fablab.notification` durável (TopicExchange) + 16 queues duráveis + 16 bindings (routing key ↔ fila)
- [ ] `Jackson2JsonMessageConverter` para desserialização dos DTOs de evento
- [ ] Teste de integração confirma filas/bindings criados no RabbitMQ

**Dependências:** task-001, task-004.

## task-007 — NotificacaoEventListener (listeners por evento)

- **Escopo:** `src/main/java/com/fablab/notification/messaging/`
- **Entradas:** RabbitMqConfig (task-006), DTOs de evento (task-004), NotificacaoService (task-008)
- **Saídas:** `@RabbitListener` por fila criando a notificação `PENDENTE` correspondente

Critérios:
- [ ] Listener `estoque.baixo` → `ESTOQUE_BAIXO` p/ Admin (nulo), com nome/quantidade/mínimo do item
- [ ] Listener `emprestimo.atrasado` → `EMPRESTIMO_ATRASADO` p/ `idPessoa`
- [ ] Listeners `encomenda.criada` e `encomenda.status.alterado` → p/ `idCliente`
- [ ] Listener `orcamento.aprovado` → p/ `idCliente`
- [ ] Listeners `lancamento.vencido` e `compra.solicitada` → p/ Admin (nulo)
- [ ] Listeners `advertencia.registrada` e `projeto.mesa.abandonado` → p/ `idFuncionario`; `kanban.status.alterado` → Admin
- [ ] Listeners `certificado.solicitado` (Admin), `certificado.aprovado` e `certificado.rejeitado` → p/ `idFuncionario`
- [ ] Listener `extrato.mensal.horas` → corpo formatado (presença/encomendas/projetos/disponíveis) p/ `idFuncionario`
- [ ] Listeners `nivel.alterado` e `horas.validadas` → p/ `idFuncionario`
- [ ] Logs informativos por evento recebido; assunto/mensagem em PT-BR
- [ ] Testes unitários dos listeners (por evento → notificação criada com dados corretos) passam

**Dependências:** task-006, task-008.

## task-008 — NotificacaoService (regras de negócio)

- **Escopo:** `src/main/java/com/fablab/notification/service/NotificacaoService.java`
- **Entradas:** repositórios (task-005), DTOs (task-004), senders (task-009), AcessoService (task-011), properties
- **Saídas:** registro/envio, listagem, leitura, revisão, teste, expurgo e reenvio

Critérios:
- [ ] `registrar(idDestinatario, canal, tipoEvento, assunto, mensagem, idReferencia)` → cria `PENDENTE` com `data_criacao` e tenta envio imediato
- [ ] `tentarEnviar`: despacha pelo canal (`EMAIL` → emailService, `WHATSAPP` → whatsAppService); sucesso → `ENVIADA` + `data_envio`; falha → permanece `PENDENTE`
- [ ] `listarDoUsuario` e `listarAdmin` com filtros por `status`/`tipoEvento` (ordenado por data criacao desc)
- [ ] `marcarComoLida(id)` validando posse via `AcessoService` (403 se não for do usuário); registra `data_leitura`
- [ ] `revisar(ids)`: copia cada notificação para `notificacao_historico` (dataRevisaoAdmin + idAdminRevisor) e remove da ativa; retorna `{ "revisadas": n }`
- [ ] `listarHistorico` com filtros por `canal`/`tipoEvento`
- [ ] `enviarTeste(request)`: defaults (canal EMAIL, assunto/mensagem padrão) e `idDestinatario` = usuário autenticado se ausente; `TipoEvento.TESTE`
- [ ] `expurgarHistorico()`: `deleteByDataRevisaoAdminBefore(now - retencaoDias)`
- [ ] `reenviarPendentes()`: reprocessa todas as `PENDENTE` e retorna o total processado
- [ ] `canalHabilitado(canal)` delegando a `ConfiguracaoCanalService`
- [ ] Testes unitários de todas as regras passam (incl. falha de envio mantém PENDENTE; revisão preserva campos)

**Dependências:** task-005, task-009, task-011.

## task-009 — Senders de Canal (e-mail + WhatsApp no-op) e ConfiguracaoCanalService

- **Escopo:** `src/main/java/com/fablab/notification/service/`
- **Entradas:** `spring.mail` (task-001), `configuracao_canal` (task-002), ConfiguracaoCanalRepository (task-005)
- **Saídas:** `EmailService`, `WhatsAppService` (interface) + `WhatsAppServiceNoop`, `ConfiguracaoCanalService`

Critérios:
- [ ] `EmailService.enviar(notificacao)` via `JavaMailSender` (SimpleMailMessage): from = remetente configurado, to = destino padrão, subject/texto da notificação
- [ ] `EmailService` verifica `isHabilitado(EMAIL)`; canal desabilitado → log de aviso e `false`
- [ ] Exceção `MailException` tratada → log de erro e retorno `false` (mantém `PENDENTE`)
- [ ] Interface `WhatsAppService.enviar` (contrato futuro)
- [ ] `WhatsAppServiceNoop`: log "não implementado no MVP" e retorno `false`
- [ ] `ConfiguracaoCanalService`: `listar()`, `buscar(id)`, `atualizar(id, request)` e `isHabilitado(canal)`
- [ ] Testes unitários de `EmailService` (sucesso, canal desabilitado, falha), `WhatsAppServiceNoop` e `ConfiguracaoCanalService` passam

**Dependências:** task-003, task-005.

## task-010 — Schedulers (Expurgo e Reenvio de Pendentes)

- **Escopo:** `src/main/java/com/fablab/notification/service/`
- **Entradas:** NotificacaoService (task-008), properties `notification.expurgo.*`/`notification.reenvio.*`
- **Saídas:** `ExpurgoHistoricoScheduler` (diário 03:00) e `ReenvioPendentesScheduler` (30 min)

Critérios:
- [ ] `ExpurgoHistoricoScheduler` com `@Scheduled(cron="${notification.expurgo.scheduler.cron:0 0 3 * * *}")` e `@ConditionalOnProperty` (`notification.expurgo.scheduler.enabled`, `matchIfMissing=true`)
- [ ] Execução de expurgo chama `notificacaoService.expurgarHistorico()` e loga total removido
- [ ] `ReenvioPendentesScheduler` com `@Scheduled(cron="${notification.reenvio.scheduler.cron:0 */30 * * * *}")` e `@ConditionalOnProperty` (`notification.reenvio.scheduler.enabled=true` — default desabilitado)
- [ ] Execução de reenvio chama `notificacaoService.reenviarPendentes()` e loga total processado
- [ ] Métodos públicos executáveis manualmente (testáveis sem esperar 03:00/30 min)
- [ ] Testes de scheduler confirmam filtro por retenção (90 dias) e reenvio de pendentes

**Dependências:** task-008.

## task-011 — Segurança JWT e AcessoService (RBAC)

- **Escopo:** `src/main/java/com/fablab/notification/config/` e `service/AcessoService.java`
- **Entradas:** JWT compartilhado do Auth (`JWT_SECRET`, issuer `fablab`); matriz RBAC de `docs/_archive/2026-10-19/07`
- **Saídas:** `SecurityConfig` stateless, filtro JWT, handlers 401/403 e `AcessoService`

Critérios:
- [ ] `SecurityConfig`: CSRF off, stateless, `/actuator/health` permitido, demais rota autenticada; `@EnableWebSecurity` + `@EnableMethodSecurity`
- [ ] `JwtProperties` (`jwt.secret`, `jwt.issuer`) via env; `JwtAuthenticationFilter` valida assinatura/issuer e monta `AuthPrincipal` com `idUser` e `role`
- [ ] `RestAuthenticationEntryPoint` (401) e `RestAccessDeniedHandler` (403) com `ErrorResponse` PT-BR
- [ ] `AcessoService`: `idUsuario()` do principal; `podeAcessar(idDestinatario)` — ADMIN sempre true; demais true apenas se `idUser == idDestinatario`
- [ ] Testes de segurança: token inválido → 401; usuário comum acessando notificação alheia → 403; sem token no `unread/count` → 401

**Dependências:** task-001.

## task-012 — Controllers REST

- **Escopo:** `src/main/java/com/fablab/notification/controller/`
- **Entradas:** services (task-008, task-009), AcessoService (task-011), matriz RBAC
- **Saídas:** endpoints PT-BR com `@PreAuthorize` conforme `docs/_archive/2026-10-19/07`

Critérios:
- [ ] `NotificacaoController` (`/notificacoes`): `GET` (filtros status/tipoEvento), `PUT /{id}/ler`, `GET /unread/count`, `GET /admin`, `POST /admin/revisar`, `GET /historico`, `POST /teste` (response `201`)
- [ ] `@PreAuthorize("hasRole('ADMIN')")` em `/admin`, `/historico` e `/teste`
- [ ] `ConfiguracaoCanalController` (`/configuracoes-canal`): `GET`, `GET /{id}`, `PUT /{id}` — todos Admin
- [ ] `@RestControllerAdvice` (`GlobalExceptionHandler`) com `ResourceNotFoundException` → 404 e `ForbiddenException` → 403 em PT-BR
- [ ] `RevisaoRequest` validado (`@NotEmpty`); resposta `{ "revisadas": n }`
- [ ] Testes de integração dos endpoints passam (H2 + filtrar JWT)

**Dependências:** task-008, task-009, task-011.

## task-013 — Testes de Integração, Segurança e Messaging

- **Escopo:** `src/test/java/com/fablab/notification/`
- **Entradas:** fluxos implementados (task-001..task-012)
- **Saídas:** suíte verde cobrindo endpoints, RBAC, expurgo/reenvio e consumo de eventos

Critérios:
- [ ] Config de teste: H2 `MODE=PostgreSQL`, Flyway off, Rabbit/Eureka desabilitados, e-mail em `localhost:3025`, JWT de teste (token helper)
- [ ] Teste de integração: criar notificação → enviar → `ENVIADA`; falha → `PENDENTE`; leitura → `LIDA` + `data_leitura`
- [ ] Teste de integração de revisão: move selecionadas para histórico, grava revisor e remove da ativa; histórico listável e filtrável
- [ ] Teste de `unread/count`: contagem correta por usuário
- [ ] Teste de RBAC: Admin tem acesso a `/admin`, `/historico`, `/teste`, `/configuracoes-canal`; não-Admin recebe 403; posse violada → 403
- [ ] Teste de messaging: cada `@RabbitListener` cria notificação correta ao receber o respectivo evento
- [ ] `mvn test` (e build) sem erros; smoke test na porta `8087` registrado no Eureka

**Dependências:** task-012 (e anteriores).

## task-014 — Documentação e Validação Final

- **Escopo:** `README.md`, `notification-service/docs/API.md`, `docs/CHANGELOG.md`, `docs/ACTION_PLAN.md` (se aplicável)
- **Entradas:** endpoints, eventos, schedulers e configurações implementados (task-001..task-013); `docs/_archive/2026-10-19/07` como fonte
- **Saídas:** documentação do serviço consistente com a doc fonte e o gateway

Critérios:
- [ ] `README.md` com visão geral, stack, como rodar (envs `DB_*`, `RABBITMQ_*`, `SMTP_*`, `JWT_SECRET`, `EUREKA_URI`) e testes
- [ ] `docs/API.md` com tabelas de endpoints (incl. `unread/count`), RBAC, enums, eventos consumidos (exchange `fablab.notification`, filas, tipos e destinatários), retenção e schedulers
- [ ] `docs/CHANGELOG.md` atualizado (versão e features do Notification Service)
- [ ] Rotas do gateway (`/api/notification/**`, `/api/notifications/**` → `notification-service`) validadas contra os controllers
- [ ] Contrato de eventos confirmado com `docs/_archive/2026-10-19/07` (mesmos routing keys) e com os publicadores de cada serviço
- [ ] Nenhuma divergência de endpoints/tabelas com `docs/_archive/2026-10-19/07` sem registro justificado

**Dependências:** task-013 (e anteriores).

## Resumo de Dependências

```
task-001 ─→ task-002 ─→ task-003 ─→ task-005 ─┐
                                  └→ task-004 ─┤
                                               ├→ task-006 ─→ task-007
task-001 ─→ task-003 ─→ task-005 ─→ task-009 ─┴→ task-008 ─→ task-010
task-001 ─→ task-011
task-008 + task-009 + task-011 ─→ task-012 ─→ task-013 ─→ task-014
```
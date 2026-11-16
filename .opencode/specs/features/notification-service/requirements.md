# Requirements: Notification Service

## 1. Objetivo e Escopo

O `notification-service` é o microsserviço transversal responsável por notificar os usuários do sistema quando eventos que exigem atenção ocorrem. Consome eventos publicados por **todos os demais serviços** na exchange `fablab.notification` (RabbitMQ 5672) e dispara notificações por **e-mail** (implementado no MVP), mantendo a estrutura pronta para **WhatsApp** (implementação futura). 

O serviço mantém notificações ativas para consulta e leitura do destinatário, um histórico revisado pelo Admin com **retenção máxima de 90 dias** (expurgo automático diário às 03:00) e reprocessamento de envios pendentes. Executa em Java 21 + Spring Boot na porta `${PORT:8087}` (`notification-service`), persiste em PostgreSQL `fablab_notification` (usuário/senha `fablab`) e registra-se no Eureka (`http://localhost:8761/eureka`).

No MVP as notificações são **obrigatórias** (sem opt-out). O ambiente de execução é local (Ubuntu Server + Docker Compose + Tailscale), com SMTP apontando para Mailpit/MailHog em `localhost:1025`.

## 2. Funcionalidades

### F1 — Consumo de eventos e disparo de notificações
- Consumir eventos de todos os microsserviços via filas ligadas à exchange `fablab.notification` (RabbitMQ 5672).
- Para cada evento recebido, criar um registro na tabela `notificacao` com status `PENDENTE`, tipo de evento, assunto e mensagem em PT-BR.
- Tentar enviar imediatamente pelo canal configurado (e-mail) e atualizar o status para `ENVIADA` com `data_envio`; em falha ou canal desabilitado, permanecer `PENDENTE`.
- Eventos sem destinatário pessoal (ex: estoque baixo, lançamento vencido, compra solicitada, certificado solicitado) são encaminhados para revisão do Admin (destinatário nulo/Admin).

### F2 — Canais de notificação: e-mail (MVP) e WhatsApp (futuro)
- Enviar e-mails via SMTP configurado por variáveis de ambiente (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`), com remetente padrão `no-reply@fablab.local`.
- Validar, antes do envio, se o canal está habilitado em `configuracao_canal` (seed: EMAIL habilitado, WHATSAPP desabilitado).
- Manter contrato (`WhatsAppService`) com implementação no-op no MVP: o que envia por WhatsApp apenas registra em log e mantém a notificação `PENDENTE`. A estrutura de dados e fluxos já suportam o novo canal.

### F3 — Consulta e leitura pelo usuário
- `GET /notificacoes`: lista as notificações do usuário autenticado, com filtros opcionais por `status` e `tipoEvento`, ordenadas pela data de criação (mais recentes primeiro).
- `PUT /notificacoes/{id}/ler`: marca a notificação como lida (`LIDA`) e registra `data_leitura`.
- Garantir que o usuário só acessa/marca como lidas as **próprias** notificações (o Admin pode acessar qualquer uma).
- `GET /notificacoes/unread/count`: retorna o total de notificações não lidas do usuário autenticado, consumido pelo Dashboard (App Shell).

### F4 — Histórico e revisão pelo Admin
- `GET /notificacoes/admin`: lista todas as notificações ativas (Admin), com filtros por `status` e `tipoEvento`.
- `POST /notificacoes/admin/revisar`: move as notificações selecionadas (`idsNotificacoes`) para `notificacao_historico`, gravando `data_revisao_admin` e `id_admin_revisor`, e remove da tabela ativa `notificacao`.
- `GET /notificacoes/historico`: lista o histórico (Admin), com filtros por `canal` e `tipoEvento`.

### F5 — Retenção de 90 dias e expurgo automático
- Histórico é mantido por, no máximo, `notification.historico.retencao-dias` (padrão 90) após a revisão do Admin (`data_revisao_admin`).
- Job diário `ExpurgoHistoricoScheduler` (`0 0 3 * * *`, habilitado por padrão) exclui permanentemente os registros revisados há mais que o período de retenção.

### F6 — Reenvio de pendentes
- Job `ReenvioPendentesScheduler` reprocessa notificações com status `PENDENTE` a cada 30 minutos (`0 */30 * * * *`), desabilitado por padrão via `notification.reenvio.scheduler.enabled` (habilitado sob demanda).
- O reenvio percorre os pendentes e tenta o envio pelo canal configurado; sucessos viram `ENVIADA` com `data_envio`.

### F7 — Configuração de canais (Admin)
- `GET /configuracoes-canal` e `GET /configuracoes-canal/{id}`: listam/detalham as configurações de canal.
- `PUT /configuracoes-canal/{id}`: atualiza `habilitado` e `parametros` do canal (Admin).
- Seed `V4` cria EMAIL (habilitado, parâmetros SMTP) e WHATSAPP (desabilitado, `provider: nao-implementado`).

### F8 — Contador de não lidas (Dashboard)
- Contagem de notificações com status diferente de `LIDA` para o `id_user` do JWT, exposta como `GET /notificacoes/unread/count` — usada pelo Dashboard (`notificationsUnread`) com polling a cada 60s.

## 3. Modelo de Dados (Tabelas)

### `notificacao` (ativas, não revisadas)
| Coluna | Tipo | Observação |
|---|---|---|
| `id_notificacao` | BIGSERIAL PK | Identificador único |
| `id_destinatario` | BIGINT | FK lógica para funcionário (RH) / usuário (Auth); `null` = Admin |
| `canal` | VARCHAR(20) | `EMAIL`, `WHATSAPP` |
| `tipo_evento` | VARCHAR(40) | Tipo do evento que gerou a notificação |
| `assunto` | VARCHAR(255) | Assunto do e-mail / título curto |
| `mensagem` | VARCHAR(2000) | Corpo da mensagem |
| `id_referencia` | BIGINT | ID do objeto relacionado (item, encomenda, lançamento, etc.) |
| `status` | VARCHAR(20) | `PENDENTE`, `ENVIADA`, `LIDA` |
| `data_criacao` | TIMESTAMP NOT NULL | Data e hora da criação |
| `data_envio` | TIMESTAMP | Data e hora do envio efetivo |
| `data_leitura` | TIMESTAMP | Data e hora da leitura pelo usuário |

Índices: `idx_notificacao_destinatario (id_destinatario)`, `idx_notificacao_status (status)`.

### `notificacao_historico`
| Coluna | Tipo | Observação |
|---|---|---|
| `id_historico` | BIGSERIAL PK | Identificador único |
| `id_notificacao_original` | BIGINT | ID da notificação original |
| `id_destinatario` | BIGINT | Destinatário original |
| `canal` | VARCHAR(20) | Canal utilizado |
| `tipo_evento` | VARCHAR(40) | Tipo do evento |
| `assunto` | VARCHAR(255) | Assunto |
| `mensagem` | VARCHAR(2000) | Corpo da mensagem |
| `id_referencia` | BIGINT | Referência original |
| `data_criacao` | TIMESTAMP NOT NULL | Criação |
| `data_envio` | TIMESTAMP | Envio |
| `data_leitura` | TIMESTAMP | Leitura |
| `data_revisao_admin` | TIMESTAMP NOT NULL | Quando o Admin revisou |
| `id_admin_revisor` | BIGINT | Admin responsável pela revisão |

Índices: `idx_notificacao_historico_revisao (data_revisao_admin)`, `idx_notificacao_historico_destinatario (id_destinatario)`.

### `configuracao_canal`
| Coluna | Tipo | Observação |
|---|---|---|
| `id_configuracao` | BIGSERIAL PK | Identificador único |
| `canal` | VARCHAR(20) NOT NULL UNIQUE | `EMAIL`, `WHATSAPP` |
| `habilitado` | BOOLEAN NOT NULL | Se o canal está ativo |
| `parametros` | VARCHAR(2000) | JSON com configurações (ex: SMTP host/porta/remetente) |

Seed: `EMAIL` com `habilitado = TRUE` e parâmetros SMTP; `WHATSAPP` com `habilitado = FALSE` e `{"provider":"nao-implementado"}`.

## 4. Fluxos Principais

### 4.1 Recebimento de evento e envio
1. Um microsserviço publica um evento na exchange `fablab.notification` (ex: `estoque.baixo.event`).
2. O Notification Service consome o evento na fila correspondente (`notificacao.<evento>`).
3. Cria um registro em `notificacao` com status `PENDENTE`, preenchendo canal, tipo, assunto, mensagem, destinatário e referência.
4. Verifica a configuração do canal (e-mail habilitado; WhatsApp desabilitado).
5. Envia por e-mail (JavaMailSender → SMTP/Mailpit).
6. Sucesso: status `ENVIADA` + `data_envio`. Falha/canal desabilitado: permanece `PENDENTE` para reenvio.

### 4.2 Leitura pelo usuário
1. O usuário autenticado lista suas notificações (`GET /notificacoes`).
2. Ao abrir uma notificação (`PUT /notificacoes/{id}/ler`), o status muda para `LIDA` e `data_leitura` é registrada.
3. O contador de não lidas (`GET /notificacoes/unread/count`) atualiza no Dashboard.

### 4.3 Revisão pelo Admin → histórico
1. O Admin consulta todas as notificações ativas (`GET /notificacoes/admin`).
2. Seleciona notificações e chama `POST /notificacoes/admin/revisar` com `{ "idsNotificacoes": [...] }`.
3. O sistema copia cada item para `notificacao_historico` com `data_revisao_admin` e `id_admin_revisor`.
4. A notificação original é removida de `notificacao`.

### 4.4 Expurgo automático (90 dias)
1. Diariamente às 03:00, `ExpurgoHistoricoScheduler` calcula o limite `now() - retencaoDias` (padrão 90).
2. Exclui permanentemente os registros de `notificacao_historico` com `data_revisao_admin < limite`.

### 4.5 Reenvio de pendentes
1. A cada 30 minutos (quando habilitado), `ReenvioPendentesScheduler` busca notificações `PENDENTE`.
2. Para cada uma, tenta novamente o envio via canal; sucesso atualiza para `ENVIADA`.

### 4.6 Integração futura com WhatsApp
1. `configuracao_canal` já possui o canal WHATSAPP com `habilitado = false`.
2. Ao implementar, basta habilitar o canal e trocar a implementação no-op por um provedor real (ex: WhatsApp Business API).
3. A estrutura de dados e os fluxos (PENDENTE → envio → ENVIADA) já suportam o novo canal.

## 5. Endpoints (Especificação)

Todos exigem `Authorization: Bearer <JWT>` (Auth & Identity), exceto `/actuator/health`. No gateway, os endpoints são expostos sob `/api/notification/**` (e `/api/notifications/**`).

| Método | Path | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/notificacoes` | Lista notificações do usuário autenticado; filtros `status` e `tipoEvento` | Qualquer autenticado |
| `PUT` | `/notificacoes/{id}/ler` | Marca notificação do próprio usuário como lida | Qualquer autenticado |
| `GET` | `/notificacoes/unread/count` | Contador de não lidas do usuário (Dashboard) | Qualquer autenticado |
| `GET` | `/notificacoes/admin` | Lista todas as notificações ativas; filtros `status` e `tipoEvento` | Admin |
| `POST` | `/notificacoes/admin/revisar` | Move notificações selecionadas para o histórico (`idsNotificacoes`) | Admin |
| `GET` | `/notificacoes/historico` | Lista o histórico; filtros `canal` e `tipoEvento` | Admin |
| `POST` | `/notificacoes/teste` | Envia notificação de teste (`idDestinatario`, `canal`, `assunto`, `mensagem`) — resposta `201` | Admin |
| `GET` | `/configuracoes-canal` | Lista configurações de canal | Admin |
| `GET` | `/configuracoes-canal/{id}` | Detalha uma configuração de canal | Admin |
| `PUT` | `/configuracoes-canal/{id}` | Atualiza `habilitado` e `parametros` | Admin |

Enums: `CanalNotificacao` (`EMAIL`, `WHATSAPP`); `StatusNotificacao` (`PENDENTE`, `ENVIADA`, `LIDA`); `TipoEvento` (ver F1/Integrações, inclui `TESTE`).

## 6. Integrações com outros serviços

| Integração | Tipo | Direção | Evento / Uso |
|---|---|---|---|
| **Todos os serviços** | Mensageria (RabbitMQ 5672, exchange `fablab.notification`) | Consome | `estoque.baixo.event`, `emprestimo.atrasado.event`, `encomenda.criada.event`, `encomenda.status.alterado.event`, `orcamento.aprovado.event`, `lancamento.vencido.event`, `compra.solicitada.event`, `advertencia.registrada.event`, `projeto.mesa.abandonado.event`, `kanban.status.alterado.event`, `certificado.solicitado.event`, `certificado.aprovado.event`, `certificado.rejeitado.event`, `extrato.mensal.horas.event`, `nivel.alterado.event`, `horas.validadas.event` |
| Pessoas & RH | REST/Dados | Consome | Dados do destinatário (nome, e-mail) para endereçar notificações |
| Auth & Identity | JWT | Consome | Validação de token (`JWT_SECRET`, issuer `fablab`) e claims `role`/`id_user` para RBAC |
| Dashboard (App Shell) | REST | Publica | `GET /notificacoes/unread/count` (badge `notificationsUnread`) |
| Gateway | REST (Discovery) | Publica | Rotas `/api/notification/**` e `/api/notifications/**` → `lb://notification-service` |
| SMTP (Mailpit/MailHog) | SMTP | Publica | Envio de e-mails (`SMTP_HOST:localhost`, `SMTP_PORT:1025`) |
| Service Discovery | Eureka | — | Registro em `http://localhost:8761/eureka` |

### Mapa de eventos consumidos

| Routing key | Fila gerada | Tipo gerado | Destinatário |
|---|---|---|---|
| `estoque.baixo.event` | `notificacao.estoque.baixo` | `ESTOQUE_BAIXO` | Admin (nulo) |
| `emprestimo.atrasado.event` | `notificacao.emprestimo.atrasado` | `EMPRESTIMO_ATRASADO` | `idPessoa` |
| `encomenda.criada.event` | `notificacao.encomenda.criada` | `ENCOMENDA_CRIADA` | `idCliente` |
| `encomenda.status.alterado.event` | `notificacao.encomenda.status` | `ENCOMENDA_STATUS_ALTERADO` | `idCliente` |
| `orcamento.aprovado.event` | `notificacao.orcamento.aprovado` | `ORCAMENTO_APROVADO` | `idCliente` |
| `lancamento.vencido.event` | `notificacao.lancamento.vencido` | `LANCAMENTO_VENCIDO` | Admin (nulo) |
| `compra.solicitada.event` | `notificacao.compra.solicitada` | `COMPRA_SOLICITADA` | Admin (nulo) |
| `advertencia.registrada.event` | `notificacao.advertencia` | `ADVERTENCIA_REGISTRADA` | `idFuncionario` |
| `projeto.mesa.abandonado.event` | `notificacao.projeto.mesa` | `PROJETO_MESA_ABANDONADO` | `idFuncionario` |
| `kanban.status.alterado.event` | `notificacao.kanban.status` | `ENCOMENDA_STATUS_ALTERADO` | Admin (nulo) |
| `certificado.solicitado.event` | `notificacao.certificado.solicitado` | `CERTIFICADO_SOLICITADO` | Admin (nulo) |
| `certificado.aprovado.event` | `notificacao.certificado.aprovado` | `CERTIFICADO_APROVADO` | `idFuncionario` |
| `certificado.rejeitado.event` | `notificacao.certificado.rejeitado` | `CERTIFICADO_REJEITADO` | `idFuncionario` |
| `extrato.mensal.horas.event` | `notificacao.extrato.mensal.horas` | `EXTRATO_MENSAL_HORAS` | `idFuncionario` |
| `nivel.alterado.event` | `notificacao.nivel.alterado` | `NIVEL_ALTERADO` | `idFuncionario` |
| `horas.validadas.event` | `notificacao.horas.validadas` | `HORAS_VALIDADAS` | `idFuncionario` |

> O evento `access.rfid.event` (Auth) não é consumido no MVP.

## 7. RBAC

Níveis de acesso via claim `role` do JWT (`NivelAcesso`): `ADMIN` (0), `BOLSISTA` (1), `VOLUNTARIO` (2), `ESTAGIARIO` (3), `RECRUTANDO` (4). Aplicação via `@PreAuthorize` nos controllers; a posse do recurso é validada no service (`AcessoService`).

| Operação | Admin | Bolsista | Voluntário | Estagiário | Recrutando |
|---|---|---|---|---|---|
| `GET /notificacoes` (próprias) | ✅ | ✅ | ✅ | ✅ | ✅ |
| `PUT /notificacoes/{id}/ler` (próprias) | ✅ | ✅* | ✅* | ✅* | ✅* |
| `GET /notificacoes/unread/count` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `GET /notificacoes/admin` | ✅ | — | — | — | — |
| `POST /notificacoes/admin/revisar` | ✅ | — | — | — | — |
| `GET /notificacoes/historico` | ✅ | — | — | — | — |
| `POST /notificacoes/teste` | ✅ | — | — | — | — |
| `GET/PUT /configuracoes-canal` | ✅ | — | — | — | — |

\* Só pode marcar como lida notificação da qual é o próprio destinatário (`id_user` == `idDestinatario`); o Admin pode marcar qualquer uma.

## 8. Requisitos Não-Funcionais

- **Idioma:** recursos, endpoints e mensagens em PT-BR.
- **Persistência:** PostgreSQL `fablab_notification` (usuário/senha `fablab`), Flyway para migrations V1..V4, `ddl-auto: validate`.
- **Porta:** `8087` (`${PORT:8087}`), registrado no Eureka como `notification-service`.
- **Mensageria:** RabbitMQ `5672` (fablab/fablab), uma fila durável por evento ligada à exchange `fablab.notification`.
- **Segurança:** todos os endpoints exigem JWT válido do Auth (`/actuator/health` excluído); sessão stateless; `@PreAuthorize` para Admin.
- **Obrigatoriedade:** sem opt-out no MVP — todas as notificações são enviadas.
- **Retenção:** histórico máximo de 90 dias (`notification.historico.retencao-dias`, padrão 90) após revisão, com expurgo automático.
- **Auditoria:** revisões gravam `data_revisao_admin` e `id_admin_revisor`; envios/leituras gravam data e hora.
- **Credenciais:** configurações SMTP/JWT via variáveis de ambiente (nunca versionadas no código).
- **Tratamento de falhas:** se o envio falha, a notificação permanece `PENDENTE` e é reprocessada pelo job de reenvio (a cada 30 min quando habilitado).
- **Agendadores:** expurgo diário `0 0 3 * * *` (habilitado por padrão, `notification.expurgo.scheduler.*`); reenvio a cada 30 min (`0 */30 * * * *`, desabilitado por padrão, `notification.reenvio.scheduler.*`).
- **Configuração:** remetente/destinatário padrão de e-mail referenciados por properties (`notification.email.*`), com defaults `no-reply@fablab.local` / `admin@fablab.local`.
- **Testes:** banco H2 (modo PostgreSQL) com RabbitMQ e Eureka desabilitados; e-mail em porta não bloqueante (`3025`).

## 9. Critérios de Sucesso

- [ ] Cada um dos 16 eventos da exchange `fablab.notification` gera uma notificação `PENDENTE` com tipo, assunto, mensagem, destinatário e referência corretos.
- [ ] Notificação com canal habilitado é enviada por e-mail e passa para `ENVIADA` com `data_envio`; falha ou canal desabilitado mantém `PENDENTE`.
- [ ] `GET /notificacoes` lista apenas as notificações do usuário autenticado (filtros funcionando); `PUT /{id}/ler` marca como `LIDA` com `data_leitura` apenas do próprio destinatário.
- [ ] `GET /notificacoes/unread/count` retorna o total de não lidas; feed do Dashboard atualizado.
- [ ] Admin lista todas, revisa e move selecionadas para `notificacao_historico` (com `data_revisao_admin` e `id_admin_revisor`), removendo-as da tabela ativa.
- [ ] Histórico listado em `GET /notificacoes/historico` com filtros por canal/tipo.
- [ ] Job de expurgo diário 03:00 remove registros revisados há mais de 90 dias.
- [ ] Job de reenvio processa pendentes a cada 30 min quando habilitado.
- [ ] `POST /notificacoes/teste` envia notificação de teste (resposta `201`).
- [ ] `configuracoes-canal` listadas e atualizáveis apenas por Admin (EMAIL habilitado, WHATSAPP desabilitado no seed).
- [ ] RBAC validado: Admin revisa/configura; demais papéis apenas veem e leem as próprias.
- [ ] Migrations V1..V4 aplicam `flyway migrate` sem erro; `ddl-auto: validate` consistente.
- [ ] `mvn test` verde (unitários, integração, segurança, messaging); smoke test na porta `8087` registrado no Eureka.

## 10. Fora de Escopo

- **WhatsApp no MVP:** estrutura pronta, mas envio real (WhatsApp Business API) não é implementado — fica como no-op.
- **Opt-out/preferências:** não há escolha de receber ou não notificações no MVP.
- **Consumo de `access.rfid.event`** (Auth Service) para notificações de acesso — não requisitado no MVP.
- **Categorização avançada** de notificações (agrupamento, priorização, notificações push) — evolução futura.
- **Gestão de funcionários/destinatários** (Pessoas & RH) e **permissões** (Auth & Identity) — outros bounded contexts.
- **Resolução individual de e-mail do destinatário** via RH: no MVP o envio usa endereço padrão configurável; endereçamento por destinatário é evolução.
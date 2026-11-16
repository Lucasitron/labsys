# Tasks: Pessoas & RH Service

Escopo base de código: `rh-service/src/main/java/com/fablab/rh/...` e `rh-service/src/main/resources/db/migration/...`. Testes: `rh-service/src/test/java/com/fablab/rh/...`.

Nota: o serviço já existe funcional — estas tarefas representam a linha de trabalho para construir/validar/melhorar o módulo de forma incrementável. Dependências entre tarefas indicadas como "Depende de".

---

## Tarefa 001: Setup do serviço Spring Boot
- **Escopo:** `rh-service/pom.xml` (ou `build.gradle`), `rh-service/src/main/resources/application.yml`
- **Entradas:** stack Java 21 + Spring Boot, infra local (Docker Compose).
- **Saídas:** serviço `rh-service` rodando na porta `8082`; configuração de datasource, Eureka e RabbitMQ.
- **Critérios:**
  - [ ] Dependências: web, data-jpa, flyway, amqp, eureka-client, security, validation, postgres, json (ddd mappings sobre `spring.jpa`/Jackson).
  - [ ] `application.yml`: `server.port=8082`, `spring.datasource.url=jdbc:postgresql://localhost:5432/fablab_rh`, user/senha `fablab`, `spring.rabbitmq.host=localhost` (5672), `eureka.client.service-url.defaultZone=http://localhost:8761/eureka`, `spring.application.name=rh-service`, chave JWT configurável.
  - [ ] Healthcheck/actuator de pé junto do Eureka.
  - [ ] Build compila (`mvn -q compile` ou equivalente do repo).

## Tarefa 002: Migrations Flyway V1..V5
- **Escopo:** `rh-service/src/main/resources/db/migration/V1..V5`
- **Entradas:** schema do requirements.md; DB `fablab_rh` criado.
- **Saídas:** tabelas `pessoa`, `funcionario`, `tutor`, `registro_ponto_diario`, `apontamento_horas`.
- **Critérios:**
  - [ ] V1: `pessoa` com `id_pessoa` BIGSERIAL PK, `nome_completo`, `matricula` UNIQUE, `data_admissao`, `contato`, `turno`, `status` INT.
  - [ ] V2: `funcionario` com `id_funcionario` PK, `id_pessoa` FK, `nivel_acesso` INT, `departamento`.
  - [ ] V3: `tutor` com `id_tutor` PK, `id_funcionario` FK, `turno`, `qualificacao` INT.
  - [ ] V4: `registro_ponto_diario` com `id_registro` PK, `id_funcionario` FK, `data`, `hora_entrada`, `hora_saida`, `total_horas` NUMERIC.
  - [ ] V5: `apontamento_horas` com `id_apontamento` PK, `id_funcionario` FK, `tipo`, `id_referencia`, `data`, `horas_trabalhadas`, `descricao_atividade`, `status`, `id_admin_validador` FK nullable, `data_validacao` nullable.
  - [ ] Flyway roda sem erro na subida.
- **Depende de:** Tarefa 001.

## Tarefa 003: Migrations Flyway V6..V9
- **Escopo:** `rh-service/src/main/resources/db/migration/V6..V9`
- **Entradas:** cru V5.
- **Saídas:** `processo_seletivo`, `treinamento`, `avaliacao_treinamento`, `historico_nivel`.
- **Critérios:**
  - [ ] V6: `processo_seletivo` (`id_processo` PK, `id_candidato` FK pessoa, `id_tutor` FK funcionario, `status_processo`, `data_inscricao`, `resultado_final`).
  - [ ] V7: `treinamento` (`id_treinamento` PK, `titulo`, `descricao`, `url_conteudo`, `id_tutor` FK).
  - [ ] V8: `avaliacao_treinamento` (`id_avaliacao` PK, `id_treinamento` FK, `id_funcionario` FK, `nota` NUMERIC, `feedback`, `data_avaliacao`).
  - [ ] V9: `historico_nivel` (`id_historico` PK, `id_funcionario` FK, `nivel_antigo`, `nivel_novo`, `id_admin_alterou` FK, `data_alteracao` TIMESTAMPTZ).
  - [ ] Migrations acumuladas sobem limpas.

## Tarefa 004: Migration Flyway V10 (Certificados e Consolidação)
- **Escopo:** `rh-service/src/main/resources/db/migration/V10__certificado_horas.sql`
- **Entradas:** doc 08-horas.md.
- **Saídas:** `solicitacao_certificado`, `certificado_emitido`, `hora_consolidada` e coluna `consolidado`.
- **Critérios:**
  - [ ] `ALTER TABLE apontamento_horas ADD COLUMN consolidado BOOLEAN NOT NULL DEFAULT FALSE`.
  - [ ] `solicitacao_certificado` (`id_solicitacao` PK, `id_funcionario` FK, `tipo_certificado` CHECK EXTENSAO/COMPLEMENTAR/ESTAGIO, `data_solicitacao` TIMESTAMPTZ, `horas_solicitadas` NUMERIC, `status` CHECK PENDENTE/APROVADO/REJEITADO, `id_admin_aprovador` FK, `data_decisao`, `observacao`).
  - [ ] `certificado_emitido` com `id_solicitacao` UNIQUE FK, `id_funcionario` FK, `tipo_certificado`, `horas_certificadas`, `data_emissao`, `codigo_verificacao` UUID UNIQUE.
  - [ ] `hora_consolidada` (`id_consolidacao` PK, `id_certificado` FK, `id_apontamento` FK, `horas` NUMERIC, `data_consolidacao`).

## Tarefa 005: Entidades JPA e Enums
- **Escopo:** `com.fablab.rh.entity`
- **Entradas:** migrations V1..V10.
- **Saídas:** entidades `Pessoa`, `Funcionario`, `Tutor`, `RegistroPontoDiario`, `ApontamentoHoras`, `ProcessoSeletivo`, `Treinamento`, `AvaliacaoTreinamento`, `HistoricoNivel`, `SolicitacaoCertificado`, `CertificadoEmitido`, `HoraConsolidada`; enums `NivelAcesso`, `PessoaStatus`, `TipoApontamento`, `StatusApontamento`, `StatusProcesso`, `StatusSolicitacao`, `TipoCertificado`; converters (`NivelAcessoConverter`, `PessoaStatusConverter`).
- **Critérios:**
  - [ ] Mapeamento `@Entity`/`@Table` alinhado aos nomes reais (colunas com underline: `id_pessoa`, `nivel_acesso`, etc.).
  - [ ] `id` como `GenerationType.IDENTITY` (BIGSERIAL).
  - [ ] Relacionamentos FK com lazy loading e join columns corretos.
  - [ ] Enums com conversão estável (ordinal/código conforme migrations).

## Tarefa 006: Repositórios Spring Data JPA
- **Escopo:** `com.fablab.rh.repository`
- **Entradas:** entidades da Tarefa 005.
- **Saídas:** `*Repository` para todas as entidades; queries customizadas.
- **Critérios:**
  - [ ] `PessoaRepository.findByMatricula`, `FuncionarioRepository` com filtros por `nivel_acesso`/`departamento`, `findByPessoaId`.
  - [ ] `RegistroPontoDiarioRepository` por dia/funcionário (para montar turno atual).
  - [ ] `ApontamentoHorasRepository` por funcionário/status/`consolidado=false` e soma de horas por faixa.
  - [ ] `SolicitacaoCertificadoRepository` por funcionário/status; `CertificadoEmitidoRepository` por funcionário.
  - [ ] `HistoricoNivelRepository` por funcionário (ordem desc).
  - [ ] `AvaliacaoTreinamentoRepository` por treinamento/funcionário.
- **Depende de:** Tarefa 005.

## Tarefa 007: DTOs e Mappers
- **Escopo:** `com.fablab.rh.dto` e `com.fablab.rh.mapper`
- **Entradas:** endpoints listados no requirements.md.
- **Saídas:** requests/responses com Bean Validation; mappers entity↔DTO.
- **Critérios:**
  - [ ] `PessoaRequest/PessoaResponse`, `FuncionarioRequest/FuncionarioResponse`, `NivelRequest/NivelAlteradoResponse`, `ApontamentoHorasRequest/Response`, `ValidacaoApontamentoRequest`, `ProcessoSeletivoRequest/Response` + status, `TreinamentoRequest/Response`, `AvaliacaoRequest/Response`, `SolicitarCertificadoRequest`, `SolicitacaoCertificadoResponse`, `DecisaoSolicitacaoRequest`, `CertificadoEmitidoResponse`, `HorasDisponiveisResponse`, `FuncionarioHorasResponse`.
  - [ ] `RhPrincipal` extrai claims `id_user`/`role`/`setor` do JWT.
  - [ ] `ErrorResponse` padronizado + `GlobalExceptionHandler`.
  - [ ] Validações: matrícula/email formato, ranges, enums, datas.
- **Depende de:** Tarefa 006.

## Tarefa 008: Segurança JWT
- **Escopo:** `com.fablab.rh.config` (SecurityConfig, JwtAuthenticationFilter, JwtProperties, JwtService, RestAuthenticationEntryPoint, RestAccessDeniedHandler)
- **Entradas:** token JWT do Auth & Identity (claims `id_user`/`role`/`setor`).
- **Saídas:** proteção global com `@PreAuthorize` por role.
- **Critérios:**
  - [ ] Filtro valida assinatura/expiração e popula `SecurityContext`.
  - [ ] Rotas públicas quando aplicável; demais exigem autenticação.
  - [ ] `@PreAuthorize("hasRole('ADMIN')")` para validação de apontamentos, alteração de nível, aprovar/rejeitar certificados.
  - [ ] Access denied e unauthenticated retornam JSON padrão.
- **Depende de:** Tarefa 007.

## Tarefa 009: PessoaService + FuncionarioService + Nível
- **Escopo:** `com.fablab.rh.service.PessoaService`, `FuncionarioService`; `com.fablab.rh.controller.PessoaController`, `FuncionarioController`
- **Entradas:** repositórios deste domínio.
- **Saídas:** `POST/GET/PUT /pessoas`, `GET/POST /funcionarios`, `PUT /funcionarios/{id}/nivel`, `GET /funcionarios/{id}/horas`.
- **Critérios:**
  - [ ] Criar pessoa com validação de `matricula` única.
  - [ ] Criar funcionário vinculado a pessoa; criação de candidato usa `nivel_acesso=4`/status Recrutando.
  - [ ] Alterar nível: transação grava `funcionario.nivel_acesso` + `historico_nivel` e publica `nivel.alterado.event` (`RhEventPublisher`).
  - [ ] Somente Admin altera nível.
  - [ ] `GET /funcionarios/{id}/horas` agrega presença, encomenda e projeto.
- **Depende de:** Tarefas 006, 007, 008.

## Tarefa 010: Ponto RFID (RfidEventListener + PontoService)
- **Escopo:** `com.fablab.rh.service.RfidEventListener`, `PontoService`; `com.fablab.rh.config.RabbitMqConfig`
- **Entradas:** eventos `access.rfid.event` do Auth & Identity (exchange `fablab.access`).
- **Saídas:** `registro_ponto_diario` atualizado (entrada/saída/`total_horas`).
- **Critérios:**
  - [ ] `RabbitMqConfig`: exchange `fablab.access`, fila durável `rh.ponto.diario`, binding `access.rfid.event`.
  - [ ] `@RabbitListener(queues="rh.ponto.diario")` consome `RfidAccessEvent` e delega ao `PontoService`.
  - [ ] Sem registro aberto → grava `hora_entrada`; com registro aberto → fecha `hora_saida` e calcula `total_horas`.
  - [ ] Idempotência: múltiplos eventos no mesmo dia não duplicam turnos.
  - [ ] Teste de integração com RabbitMQ real embutido (ex.: `RfidPontoIntegrationTest`).
- **Depende de:** Tarefas 005, 006.

## Tarefa 011: ApontamentoHorasService
- **Escopo:** `com.fablab.rh.service.ApontamentoHorasService`; `com.fablab.rh.controller.ApontamentoHorasController`, `HorasController`
- **Entradas:** apontamentos de ENCOMENDA/PROJETO.
- **Saídas:** `POST /apontamentos-horas`, `PUT /apontamentos-horas/{id}/validar`, `GET /horas/disponiveis`.
- **Critérios:**
  - [ ] Criar apontamento com status PENDENTE e `consolidado=false`.
  - [ ] Validar/rejeitar (Admin): atualiza `status`, `id_admin_validador`, `data_validacao`.
  - [ ] Apontamento VALIDADO publica `horas.validadas.event` para o Financeiro.
  - [ ] `GET /horas/disponiveis` soma VALIDADOS + `consolidado=false` (encomenda + projeto).
  - [ ] Validação de coerência: encomenda+projeto do dia não excedem presença.
  - [ ] Testes unitários + integração (`ApontamentoHorasServiceTest`, `ApontamentoIntegrationTest`).
- **Depende de:** Tarefas 009, 010.

## Tarefa 012: ProcessoSeletivoService
- **Escopo:** `com.fablab.rh.service.ProcessoSeletivoService`; `com.fablab.rh.controller.ProcessoSeletivoController`
- **Entradas:** candidatos e tutores.
- **Saídas:** `POST /processo-seletivo`, `PUT /processo-seletivo/{id}`.
- **Critérios:**
  - [ ] `POST` cria pessoa + funcionário (nível 4, status Recrutando) e `processo_seletivo` Inscrito.
  - [ ] `PUT /{id}` atualiza `status_processo`/`resultado_final` (triagem → entrevista → aprovado/reprovado).
  - [ ] Aprovação de nível é o gatilho do Admin para mudar a `nivel_acesso` (fluxo da Tarefa 009).
  - [ ] Validação de tutor responsável válido.
  - [ ] Testes (`ProcessoSeletivoTreinamentoIntegrationTest`).
- **Depende de:** Tarefas 009, 011.

## Tarefa 013: TreinamentoService (LMS)
- **Escopo:** `com.fablab.rh.service.TreinamentoService`; `com.fablab.rh.controller.TreinamentoController`
- **Entradas:** conteúdo e alunos do Fab Lab.
- **Saídas:** `POST /treinamentos`, `POST /treinamentos/{id}/avaliacoes`, `GET /treinamentos/{id}/avaliacoes`.
- **Critérios:**
  - [ ] Criar treinamento com `titulo`, `descricao`, `url_conteudo`, `id_tutor`.
  - [ ] Registrar avaliação (`nota` 0-10 + `feedback`) por tutor.
  - [ ] Listar avaliações de um treinamento; aluno consulta sua nota/feedback.
  - [ ] Sem certificação no módulo LMS (avaliação é por nota apenas).
- **Depende de:** Tarefas 006, 008.

## Tarefa 014: CertificadoService
- **Escopo:** `com.fablab.rh.service.CertificadoService`; `com.fablab.rh.controller.CertificadoController`
- **Entradas:** horas disponíveis (VALIDADO + `consolidado=false`); doc 08-horas.md.
- **Saídas:** solicitar/consultar/aprovar/rejeitar certificados; emissão com consolidação.
- **Critérios:**
  - [ ] `POST /certificados/solicitar`: cria `solicitacao_certificado` PENDENTE com `horas_solicitadas` = horas disponíveis; publica `certificado.solicitado.event`.
  - [ ] `GET /certificados/solicitacoes`: Admin vê todas; integrante vê as suas (filtro por `id_funcionario`).
  - [ ] `PUT /certificados/solicitacoes/{id}/aprovar`: transação única emite `certificado_emitido` (UUID `codigo_verificacao`), marca apontamentos `consolidado=true`, grava `hora_consolidada`; publica `certificado.aprovado.event`.
  - [ ] `PUT /certificados/solicitacoes/{id}/rejeitar`: status REJEITADO + `observacao`; publica `certificado.rejeitado.event`.
  - [ ] `GET /certificados/emitidos` e `/emitidos/{id}` com regras Admin/próprio.
  - [ ] Horas consolidadas não reaparecem em novos certificados.
  - [ ] Testes unitários + integração (`CertificadoServiceTest`, `CertificadoIntegrationTest`).
- **Depende de:** Tarefas 011, 007, 008.

## Tarefa 015: Extrato Mensal (ExtratoService + Scheduler)
- **Escopo:** `com.fablab.rh.service.ExtratoService`, `com.fablab.rh.service.ExtratoMensalScheduler`; `com.fablab.rh.controller` (endpoint `/extrato-mensal-horas`)
- **Entradas:** doc 08-horas.md; cron configurável (default `0 0 6 1 * *`).
- **Saídas:** `extrato.mensal.horas.event` por funcionário ativo.
- **Critérios:**
  - [ ] `@Scheduled(cron="${rh.certificado.extrato.scheduler.cron:0 0 6 1 * *}")` executa 1º dia do mês 06:00.
  - [ ] Compila mês anterior (fechado): horasPresenca (RFID), horasEncomenda, horasProjeto, horasDisponiveis.
  - [ ] Exclui Recrutandos (nível 4) e inativos.
  - [ ] Publica `extrato.mensal.horas.event` com `{ idFuncionario, mesReferencia, horasPresenca, horasEncomenda, horasProjeto, horasDisponiveis }`.
  - [ ] Property `rh.certificado.extrato.scheduler.enabled` permite desligar.
  - [ ] Testes (`ExtratoServiceTest`).
- **Depende de:** Tarefa 011, 014 (estrutura de disponíveis/consolidados).

## Tarefa 016: Testes e Validação Final
- **Escopo:** `rh-service/src/test/java/com/fablab/rh/**`; config docker-compose do serviço.
- **Entradas:** módulo completo (Tarefas 001-015).
- **Saídas:** suite de testes verde + execução local validada.
- **Critérios:**
  - [ ] Unitários: `PessoaServiceTest`, `FuncionarioServiceTest`, `ApontamentoHorasServiceTest`, `CertificadoServiceTest`, `ExtratoServiceTest`, `JwtServiceTest`.
  - [ ] Integração: `PessoaFuncionarioIntegrationTest`, `ApontamentoIntegrationTest`, `CertificadoIntegrationTest`, `ProcessoSeletivoTreinamentoIntegrationTest`.
  - [ ] Mensageria: `RfidPontoIntegrationTest` (consome evento RFID real); eventos publicados verificáveis no exchange `fablab.rh`.
  - [ ] Segurança: `SecurityConfigTest` valida matriz RBAC dos endpoints.
  - [ ] `mvn test` (ou padrão do repo) verde.
  - [ ] Serviço sobe no docker-compose com Postgres/RabbitMQ/Eureka e migrations V1..V10 aplicadas.
- **Depende de:** Tarefas 008–015.
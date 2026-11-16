# Tasks: Produção & Projetos Service

Escopo base de código: `producao-service/src/main/java/com/fablab/producao/...` e `producao-service/src/main/resources/db/migration/...`. Testes: `producao-service/src/test/java/com/fablab/producao/...`.

Nota: o serviço existe como scaffold funcional — estas tarefas representam a linha de trabalho incremental (entities + migrations V1..V18 → repositories → services → Rabbit → scheduler 5S → controllers → testes → docs). Fonte de verdade: `docs/_archive/2026-10-19/06-Produção_&_Projetos_Service.md`. Dependências indicadas como "Depende de task-0XX".

---

## Tarefa 001: Setup do serviço Spring Boot
- **Escopo:** `producao-service/pom.xml` (ou build do monorepo), `producao-service/src/main/resources/application.yml`
- **Entradas:** stack Java 21 + Spring Boot, infra local (Docker Compose + Eureka + RabbitMQ).
- **Saídas:** serviço `producao-service` rodando na porta `8086`; datasource, Eureka, RabbitMQ e Actuator configurados.
- **Critérios:**
  - [ ] Dependências: web, data-jpa, flyway, amqp, eureka-client, security, validation, actuator, postgres, QR Code (ex.: zxing).
  - [ ] `application.yml`: `server.port=${PORT:8086}`, `spring.datasource.url=jdbc:postgresql://localhost:5432/fablab_producao`, user/senha `fablab`, `spring.rabbitmq.host=localhost` (5672), `eureka.client.service-url.defaultZone=http://localhost:8761/eureka`, `spring.application.name=producao-service`, chave JWT configurável.
  - [ ] `spring.jpa.hibernate.ddl-auto=validate` e `spring.flyway.locations=classpath:db/migration`.
  - [ ] Actuator expõe `health,info,metrics`; healthcheck de pé junto do Eureka.
  - [ ] Build compila (`mvn -q compile` ou equivalente do repo).

## Tarefa 002: Migrations V1..V4 (Projetos e Kanban)
- **Escopo:** `producao-service/src/main/resources/db/migration/V1..V4`
- **Entradas:** requirements.md §3.1; DB `fablab_producao` criado.
- **Saídas:** `projeto`, `tarefa`, `encomenda_kanban`, `historico_kanban`.
- **Critérios:**
  - [ ] V1: `projeto` (`id_projeto` BIGSERIAL PK, `nome`, `descricao`, `data_inicio`, `data_fim_prevista`, `data_fim_real`, `status`, `id_responsavel` FK → `funcionario`).
  - [ ] V2: `tarefa` (`id_tarefa` PK, `id_projeto` FK NOT NULL, `titulo`, `descricao`, `id_responsavel` FK, `data_inicio`, `data_fim_prevista`, `data_conclusao`, `status`, `prioridade`).
  - [ ] V3: `encomenda_kanban` (`id_kanban` PK, `id_encomenda` UNIQUE, `status`, `data_entrada_status` TIMESTAMPTZ, `id_responsavel` FK, `ordem`) com coluna de versão para lock otimista.
  - [ ] V4: `historico_kanban` (`id_historico` PK, `id_encomenda`, `status_anterior`, `status_novo`, `data_alteracao` TIMESTAMPTZ, `id_usuario` FK, `observacao`).
  - [ ] Flyway roda sem erro na subida.
- **Depende de:** task-001.

## Tarefa 003: Migrations V5..V6 (Máquinas)
- **Escopo:** `producao-service/src/main/resources/db/migration/V5..V6`
- **Entradas:** requirements.md §3.2.
- **Saídas:** `maquina`, `historico_uso_maquina`.
- **Critérios:**
  - [ ] V5: `maquina` (`id_maquina` PK, `nome`, `descricao`, `status`, `localizacao`).
  - [ ] V6: `historico_uso_maquina` (`id_uso` PK, `id_maquina` FK, `id_funcionario` FK, `data_inicio` TIMESTAMPTZ, `data_fim` TIMESTAMPTZ, `horas_uso` NUMERIC, `observacao`).
  - [ ] Migrations acumuladas V1..V6 sobem limpas.
- **Depende de:** task-002.

## Tarefa 004: Migrations V7..V11 (Setores 5S)
- **Escopo:** `producao-service/src/main/resources/db/migration/V7..V11`
- **Entradas:** requirements.md §3.3.
- **Saídas:** `setor`, `setor_material`, `setor_sinalizacao`, `setor_checklist`, `setor_responsavel`.
- **Critérios:**
  - [ ] V7: `setor` (`id_setor` PK, `numero`, `nome`, `descricao`, `observacoes`, `foto_correto_url`, `foto_incorreto_url`, `ativo`).
  - [ ] V8: `setor_material` (`id_material` PK, `id_setor` FK, `descricao`, `quantidade` NUMERIC).
  - [ ] V9: `setor_sinalizacao` (`id_sinalizacao` PK, `id_setor` FK, `texto`).
  - [ ] V10: `setor_checklist` (`id_checklist` PK, `id_setor` FK, `item`, `ativo`).
  - [ ] V11: `setor_responsavel` (`id_responsavel` PK, `id_setor` FK, `id_funcionario` FK, `data_inicio`, `data_fim`, `ativo`).
  - [ ] Migrations acumuladas V1..V11 sobem limpas.
- **Depende de:** task-002.

## Tarefa 005: Migrations V12..V18 (Inspeções, Advertências, Mesas, Parâmetros, Consumo)
- **Escopo:** `producao-service/src/main/resources/db/migration/V12..V18`
- **Entradas:** requirements.md §3.4, §3.5, §3.6.
- **Saídas:** `inspecao_5s`, `item_inspecao_5s`, `advertencia_membro`, `projeto_mesa`, `auditoria_projeto_mesa`, `parametro_5s`, `consumo_encomenda`.
- **Critérios:**
  - [ ] V12: `inspecao_5s` (`id_inspecao` PK, `id_setor` FK, `id_inspetor` FK, `data_inspecao`, `turno`, `status`, `observacoes`).
  - [ ] V13: `item_inspecao_5s` (`id_item_inspecao` PK, `id_inspecao` FK, `id_checklist` FK, `conforme` BOOLEAN, `observacao`).
  - [ ] V14: `advertencia_membro` (`id_advertencia` PK, `id_funcionario` FK, `id_inspecao` FK nullable, `data`, `motivo`, `tipo`, `contador` INT, `id_admin_registrou` FK).
  - [ ] V15: `projeto_mesa` (`id_projeto_mesa` PK, `id_funcionario` FK, `id_mesa`, `nome_projeto`, `tipo_projeto`, `prazo_execucao`, `data_inicio`, `data_ultima_evolucao`, `status`, `qr_code_totem`).
  - [ ] V16: `auditoria_projeto_mesa` (`id_auditoria` PK, `id_projeto_mesa` FK, `data_auditoria`, `resultado`, `acao_tomada`, `id_admin_responsavel` FK).
  - [ ] V17: `parametro_5s` (`id_parametro` PK, `chave`, `valor`, `descricao`) + seed com `dias_para_auditoria_projeto`, `dia_semana_inspecao`, `periodo_experimental_ativo`, `rotacao_dias`.
  - [ ] V18: `consumo_encomenda` (registro da baixa via BOM: `id_encomenda` ref. Vendas, itens/quantidades consumidos, data e usuário responsável; contrato de baixa com Estoque).
  - [ ] Migrations V1..V18 acumuladas sobem limpas em `fablab_producao`.
- **Depende de:** task-004.

## Tarefa 006: Entidades JPA (Produção, Projetos e Máquinas)
- **Escopo:** `com.fablab.producao.entity`
- **Entradas:** migrations V1..V6.
- **Saídas:** entidades `Projeto`, `Tarefa`, `EncomendaKanban`, `HistoricoKanban`, `Maquina`, `HistoricoUsoMaquina`; enums `StatusProjeto`, `StatusTarefa`, `Prioridade`, `StatusKanban`, `StatusMaquina`; converters.
- **Critérios:**
  - [ ] Mapeamento `@Entity`/`@Table` alinhado aos nomes reais (colunas com underline: `id_projeto`, `id_responsavel`, `data_entrada_status`, etc.).
  - [ ] `id` como `GenerationType.IDENTITY` (BIGSERIAL).
  - [ ] `EncomendaKanban` com `@Version` (lock otimista); `id_encomenda` UNIQUE.
  - [ ] Relacionamentos FK com lazy loading e join columns corretos.
  - [ ] Enums com conversão estável (converters dedicados).
- **Depende de:** task-005.

## Tarefa 007: Entidades JPA (Sistema 5S e Consumo)
- **Escopo:** `com.fablab.producao.entity`
- **Entradas:** migrations V7..V18.
- **Saídas:** entidades `Setor`, `SetorMaterial`, `SetorSinalizacao`, `SetorChecklist`, `SetorResponsavel`, `Inspecao5s`, `ItemInspecao5s`, `AdvertenciaMembro`, `ProjetoMesa`, `AuditoriaProjetoMesa`, `Parametro5s`, `ConsumoEncomenda`; enums `TipoTurno`, `StatusInspecao`, `TipoAdvertencia`, `StatusProjetoMesa`, `ResultadoAuditoria`; converters.
- **Critérios:**
  - [ ] Mapeamento alinhado aos nomes reais (underlines e FKs conforme migrations).
  - [ ] `SetorResponsavel.ativo` para identificar responsável atual; `SetorChecklist.ativo` filtro de checklist válido.
  - [ ] `ProjetoMesa.qr_code_totem` String (QR único); `AdvertenciaMembro.contador` Int (1..3).
  - [ ] Relacionamentos FK com lazy loading; IDs `GenerationType.IDENTITY`.
- **Depende de:** task-005.

## Tarefa 008: Repositórios (Produção, Projetos e Máquinas)
- **Escopo:** `com.fablab.producao.repository`
- **Entradas:** entidades da Tarefa 006.
- **Saídas:** `*Repository` de `Projeto`, `Tarefa`, `EncomendaKanban`, `HistoricoKanban`, `Maquina`, `HistoricoUsoMaquina` + queries customizadas.
- **Critérios:**
  - [ ] `ProjetoRepository` com filtros por `status`/`id_responsavel`.
  - [ ] `TarefaRepository` por projeto/responsável/status/prioridade; `existsByProjetoId` para validação.
  - [ ] `EncomendaKanbanRepository.findByStatusOrderByOrdem`, `findByIdEncomenda` (unicidade), por responsável.
  - [ ] `HistoricoKanbanRepository.findByIdEncomenda` (ordem desc).
  - [ ] `MaquinaRepository` por status/localização; `HistoricoUsoMaquinaRepository` por máquina/funcionário.
- **Depende de:** task-006.

## Tarefa 009: Repositórios (Sistema 5S)
- **Escopo:** `com.fablab.producao.repository`
- **Entradas:** entidades da Tarefa 007.
- **Saídas:** `*Repository` das entidades 5S + queries customizadas.
- **Critérios:**
  - [ ] `SetorRepository.findByAtivoTrue`, `SetorChecklistRepository.findByIdSetorAndAtivoTrue` (checklist ativo da inspeção).
  - [ ] `SetorResponsavelRepository` por setor/ativo e o responsável atual (`ativo=true`); exclui recrutandos no fluxo de designação.
  - [ ] `Inspecao5sRepository` por setor/período/inspetor; `ItemInspecao5sRepository` pelas não conformidades (`conforme=false`).
  - [ ] `AdvertenciaMembroRepository` por funcionário e contagem por funcionário para o cálculo do `contador`.
  - [ ] `ProjetoMesaRepository` por status/dono; mesas elegíveis a auditoria (`data_ultima_evolucao` vencida).
  - [ ] `Parametro5sRepository.findByChave`; `ConsumoEncomendaRepository` por encomenda (idempotência da baixa).
- **Depende de:** task-007.

## Tarefa 010: DTOs e Mappers
- **Escopo:** `com.fablab.producao.dto` e `com.fablab.producao.mapper`
- **Entradas:** endpoints listados no requirements.md §5.
- **Saídas:** requests/responses com Bean Validation; mappers entity↔DTO.
- **Critérios:**
  - [ ] `ProjetoRequest/Response`, `TarefaRequest/Response`, `KanbanRequest/MoverKanbanRequest/KanbanResponse`, `HistoricoKanbanResponse`, `MaquinaRequest/Response`, `UsoMaquinaRequest`, `SetorRequest/Response`, `ChecklistRequest`, `ResponsavelRequest`, `Inspecao5sRequest` (+ itens), `Inspecao5sResponse`, `AdvertenciaRequest/Response`, `ProjetoMesaRequest/Response`, `EvolucaoRequest`, `AuditoriaRequest`, `Parametro5sResponse`.
  - [ ] `ProducaoPrincipal` extrai claims `id_user`/`role`/`setor` do JWT.
  - [ ] `ErrorResponse` padronizado + `GlobalExceptionHandler` (409 no conflito de lock do Kanban).
  - [ ] Validações: tamanhos, datas, enums, quantidades positivas, `ordem` >= 0.
- **Depende de:** task-008, task-009.

## Tarefa 011: Segurança JWT + RBAC
- **Escopo:** `com.fablab.producao.config` (SecurityConfig, JwtAuthenticationFilter, JwtProperties, JwtService, RestAuthenticationEntryPoint, RestAccessDeniedHandler)
- **Entradas:** token JWT do Auth & Identity (claims `id_user`/`role`/`setor`).
- **Saídas:** proteção global com `@PreAuthorize` por role e por responsável atribuído.
- **Critérios:**
  - [ ] Filtro valida assinatura/expiração e popula `SecurityContext`.
  - [ ] `@PreAuthorize` para produção operacional restrita ao **responsável atribuído** (projeto/tarefa/Kanban/máquina) ou Admin.
  - [ ] `@PreAuthorize("hasRole('ADMIN')")` para: designação de responsáveis de setor, `POST /advertencias`, `POST /auditorias-projeto-mesa`, `PUT /parametros-5s/{id}`.
  - [ ] Recrutandos bloqueados nos endpoints 5S (inspeções, checklists, mesas).
  - [ ] Access denied e unauthenticated retornam JSON padrão; testes de segurança da matriz RBAC.
- **Depende de:** task-010.

## Tarefa 012: ProjetoService + TarefaService + controllers
- **Escopo:** `com.fablab.producao.service.ProjetoService`, `TarefaService`; `com.fablab.producao.controller.ProjetoController`, `TarefaController`
- **Entradas:** repositórios de produção/projetos.
- **Saídas:** `POST/GET/PUT /projetos`, `POST/GET/PUT /tarefas`.
- **Critérios:**
  - [ ] Criar projeto validando `id_responsavel` (consulta ao RH) e datas (fim prevista >= início).
  - [ ] Criar tarefa vinculada a projeto existente (`id_projeto` FK).
  - [ ] Atualizar projeto/tarefa com validação de `status` e responsável; tarefa Concluída grava `data_conclusao`.
  - [ ] `GET /projetos` e `GET /tarefas` com filtros (status, responsável, prioridade).
  - [ ] Em posse de permissão, apenas Admin ou responsável atribuído edita.
  - [ ] Testes unitários + integração (`ProjetoTarefaIntegrationTest`).
- **Depende de:** task-010, task-011.

## Tarefa 013: KanbanService (movimentação + histórico)
- **Escopo:** `com.fablab.producao.service.KanbanService`; `com.fablab.producao.controller.KanbanController`
- **Entradas:** encomendas aprovadas do Vendas; repositórios de Kanban/histórico.
- **Saídas:** `POST /kanban`, `PUT /kanban/{id}/mover`, `GET /kanban`.
- **Critérios:**
  - [ ] `POST /kanban` só aceita encomenda existente (contrato Vendas), com `id_encomenda` único; cria cartão `Fila` na `ordem` correta.
  - [ ] `PUT /kanban/{id}/mover` valida transição válida, atualiza `status`, `ordem`, `data_entrada_status` em transação com **lock otimista** e grava `historico_kanban` (usuário/data).
  - [ ] Transição para `Produção` dispara consulta da BOM no Estoque; para `Entregue` dispara a baixa (task-014).
  - [ ] Cada movimentação publica `kanban.status.alterado.event`.
  - [ ] `GET /kanban` agrupa por status/ordem; com filtro por responsável.
  - [ ] Conflito de concorrência retorna 409 com retry orientado.
  - [ ] Testes unitários + integração (`KanbanIntegrationTest`).
- **Depende de:** task-012.

## Tarefa 014: Baixa de Estoque via BOM (consumo_encomenda + Estoque)
- **Escopo:** `com.fablab.producao.service.BaixaEstoqueService`
- **Entradas:** conclusão da produção (task-013); BOM do Estoque; `consumo_encomenda` (V18).
- **Saídas:** `consumo_encomenda` gravado e notificação de baixa automática ao Estoque.
- **Critérios:**
  - [ ] Na conclusão, consulta a **BOM final** no Estoque e monta o consumo.
  - [ ] Transação única: grava `consumo_encomenda` (idempotente por `id_encomenda`) e publica o evento de baixa (`tipo_saida = Consumo`).
  - [ ] Falha na publicação não perde o registro (trilha local + reprocesso).
  - [ ] Baixa **não** é executada se a produção ainda não foi concluída ou a BOM não está final.
  - [ ] Testes de integração com contrato do Estoque (`BaixaEstoqueIntegrationTest`).
- **Depende de:** task-013.

## Tarefa 015: MaquinaService (status, uso, histórico)
- **Escopo:** `com.fablab.producao.service.MaquinaService`; `com.fablab.producao.controller.MaquinaController`
- **Entradas:** entidades/repositórios de `maquina`/`historico_uso_maquina`.
- **Saídas:** `POST/GET /maquinas`, `PUT /maquinas/{id}/status`, `POST /maquinas/{id}/uso`, `PUT /maquinas/{id}/uso/{id_uso}/fim`, `GET /maquinas/{id}/historico`.
- **Critérios:**
  - [ ] Criar/listar máquinas; `PUT /status` com transição válida (ex.: Disponível → Manutenção bloqueia início de uso).
  - [ ] `POST /uso` registra `data_inicio` + funcionário e seta máquina **Em Uso** (não permite 2 usos abertos).
  - [ ] `.../uso/{id_uso}/fim` fecha `data_fim`, calcula `horas_uso` (Decimal) e devolve **Disponível**.
  - [ ] `GET /maquinas/{id}/historico` lista usos com ordenação desc.
  - [ ] Testes unitários + integração (`MaquinaIntegrationTest`).
- **Depende de:** task-010, task-011.

## Tarefa 016: SetorService (cadastro, checklist, responsáveis)
- **Escopo:** `com.fablab.producao.service.SetorService`; `com.fablab.producao.controller.SetorController`
- **Entradas:** entidades/repositórios 5S (V7..V11).
- **Saídas:** `POST/GET/PUT /setores`, `POST /setores/{id}/checklist`, `PUT /setores/{id}/checklist/{id_item}`, `POST/GET /setores/{id}/responsaveis`.
- **Critérios:**
  - [ ] Criar/atualizar setor com `numero` único e `ativo`; materiais/sinalizações vinculados.
  - [ ] Checklist: adicionar/editar item (`ativo`); inativar item retira da próxima inspeção.
  - [ ] Designar responsável: encerra responsável atual (`data_fim`, `ativo=false`) e cria novo (`data_inicio`, `ativo=true`) — **lock** para evitar sobreposição.
  - [ ] **Recrutandos (nível 4) excluídos automaticamente** da designação/rotação.
  - [ ] Listar responsáveis vigentes e histórico por setor.
  - [ ] Testes unitários (`SetorServiceTest`).
- **Depende de:** task-011.

## Tarefa 017: Inspecao5sService (inspeção + itens)
- **Escopo:** `com.fablab.producao.service.Inspecao5sService`, `ItemInspecao5sService`; `com.fablab.producao.controller.Inspecao5sController`
- **Entradas:** setores/checklists ativos; `parametro_5s.dia_semana_inspecao`.
- **Saídas:** `POST /inspecoes-5s`, `GET /inspecoes-5s`.
- **Critérios:**
  - [ ] `POST /inspecoes-5s` cria `inspecao_5s` (inspetor, turno, data) carregando o checklist **ativo** do setor.
  - [ ] Cada item gera `item_inspecao_5s` com `conforme`; ao menos um não conforme ⇒ `status = NAO_CONFORME`.
  - [ ] Resposta da inspeção devolve os itens não conformes habilitando a advertência (task-018).
  - [ ] `GET /inspecoes-5s` com filtros por setor, data, status, inspetor.
  - [ ] Período experimental (`periodo_experimental_ativo = true`) não gera penalidades formais (flag no fluxo).
  - [ ] Testes unitários + integração (`Inspecao5sIntegrationTest`).
- **Depende de:** task-016.

## Tarefa 018: AdvertenciaService (contador e penalidades)
- **Escopo:** `com.fablab.producao.service.AdvertenciaService`; `com.fablab.producao.controller.AdvertenciaController`
- **Entradas:** inspeção não conforme; `parametro_5s.periodo_experimental_ativo`.
- **Saídas:** `POST /advertencias`, `GET /advertencias/{id_funcionario}`.
- **Critérios:**
  - [ ] `POST /advertencias` (Admin): calcula `contador` sequencial do funcionário (máx+1, teto 3), `tipo` VERBAL/FORMAL, `motivo`, `id_inspecao` opcional, `id_admin_registrou`.
  - [ ] Contador calculado em transação com lock no funcionário (sem correr para duplicar).
  - [ ] Contagem usa somente advertências **formais** quando `periodo_experimental_ativo = false`; período experimental não contabiliza penalidades.
  - [ ] Ao atingir `contador = 3`: publica `advertencia.registrada.event` com indicador de **alerta crítico** (Notification → Admin).
  - [ ] `GET /advertencias/{id_funcionario}` lista com contador e histórico.
  - [ ] Testes unitários (`AdvertenciaServiceTest`).
- **Depende de:** task-017.

## Tarefa 019: ProjetoMesaService + AuditoriaService (QR totem)
- **Escopo:** `com.fablab.producao.service.ProjetoMesaService`, `AuditoriaService`; `com.fablab.producao.controller.ProjetoMesaController`, `AuditoriaProjetoMesaController`
- **Entradas:** entidades V15/V16; parâmetros `dias_para_auditoria_projeto`.
- **Saídas:** `POST/GET /projetos-mesa`, `PUT /projetos-mesa/{id}/evolucao`, `POST /auditorias-projeto-mesa`.
- **Critérios:**
  - [ ] `POST /projetos-mesa` cria o vínculo mesa↔dono e gera `qr_code_totem` **único** vinculado ao `id_projeto_mesa` (conteúdo consultável no totem).
  - [ ] `PUT .../evolucao` atualiza `data_ultima_evolucao` (e status quando o projeto conclui).
  - [ ] `POST /auditorias-projeto-mesa` (Admin) grava `auditoria_projeto_mesa` (`resultado`, `acao_tomada`, `id_admin_responsavel`).
  - [ ] Abandono confirmado ⇒ `projeto_mesa.status = ABANDONADO`, totem removido e publicação de `projeto.mesa.abandonado.event` (Notification/RH).
  - [ ] Auditoria automática agendada pelo scheduler (task-021) quando `data_ultima_evolucao` vencida.
  - [ ] Testes unitários + integração (`ProjetoMesaIntegrationTest`).
- **Depende de:** task-011, task-017.

## Tarefa 020: Parametro5sService + scheduler 5S (inspeção e rotação)
- **Escopo:** `com.fablab.producao.service.Parametro5sService`, `Scheduler5sService`; `com.fablab.producao.controller.Parametro5sController`
- **Entradas:** `parametro_5s` (± seed V17); properties `producao.5s.scheduler.enabled`/`.cron`.
- **Saídas:** `GET /parametros-5s`, `PUT /parametros-5s/{id}`; agendamentos de 5S.
- **Critérios:**
  - [ ] `GET /parametros-5s` lista todos; `PUT /parametros-5s/{id}` (Admin) valida `chave`/`valor` conhecidos.
  - [ ] Scheduler (cron configurável) emite alerta quando **fim de rotação** (`rotacao_dias`) se aproxima; notifica Admin para designar novos responsáveis.
  - [ ] Scheduler agenda a **auditoria automática** de mesas (`dias_para_auditoria_projeto` vencido) e alerta os inspetores no `dia_semana_inspecao`.
  - [ ] Property `producao.5s.scheduler.enabled` permite desligar a agenda.
  - [ ] Testes (`Scheduler5sServiceTest`, `Parametro5sServiceTest`).
- **Depende de:** task-019.

## Tarefa 021: RabbitMQ — publisher e listener (exchange `fablab.producao`)
- **Escopo:** `com.fablab.producao.config.RabbitMqConfig`, `com.fablab.producao.service.ProducaoEventPublisher`, `EncomendaListener`
- **Entradas:** exchange `fablab.producao` (RabbitMQ 5672); encomendas aprovadas (Vendas).
- **Saídas:** publicação de `kanban.status.alterado.event`, `advertencia.registrada.event`, `projeto.mesa.abandonado.event`, eventos de produção/baixa; consumo de encomenda aprovada.
- **Critérios:**
  - [ ] `RabbitMqConfig`: exchange tópica durável `fablab.producao`; bindings declarativos por routing key dos eventos publicados e fila de consumo de encomenda.
  - [ ] `ProducaoEventPublisher` com métodos por evento, tolerante a falhas (try/catch + log sem derrubar a operação), retry no template.
  - [ ] `EncomendaListener` consome a encomenda aprovada e insere o cartão no Kanban (delega ao `KanbanService`), com idempotência por `id_encomenda`.
  - [ ] Teste de integração com RabbitMQ real embutido (`RabbitMqIntegrationTest`) verificando exchanges/filas.
- **Depende de:** task-013, task-018, task-019.

## Tarefa 022: Restante dos controllers e exposição da API
- **Escopo:** `com.fablab.producao.controller` (todos), ajustes de `application.yml` (healthcheck/Eureka).
- **Entradas:** services das Tarefas 012–020.
- **Saídas:** API completa do requirements.md §5 exposta e autenticada.
- **Critérios:**
  - [ ] Todos os endpoints `/projetos`, `/tarefas`, `/kanban`, `/maquinas`, `/setores`, `/inspecoes-5s`, `/advertencias`, `/projetos-mesa`, `/auditorias-projeto-mesa`, `/parametros-5s` respondem 201/200/400/403/404/409 conforme contrato.
  - [ ] `@PreAuthorize` aplicado em todos os endpoints conforme matriz RBAC.
  - [ ] Actuator/healthcheck de pé junto do Eureka; serviço registrado com nome `producao-service`.
  - [ ] Documentação `docs/API.md` do serviço alinhada aos endpoints.
- **Depende de:** task-012, task-013, task-015, task-016, task-017, task-018, task-019, task-020.

## Tarefa 023: Testes e validação final
- **Escopo:** `producao-service/src/test/java/com/fablab/producao/**`; config docker-compose do serviço.
- **Entradas:** módulo completo (Tarefas 001–022).
- **Saídas:** suite de testes verde + execução local validada.
- **Critérios:**
  - [ ] Unitários: `ProjetoTarefaServiceTest`, `KanbanServiceTest`, `MaquinaServiceTest`, `SetorServiceTest`, `Inspecao5sServiceTest`, `AdvertenciaServiceTest`, `ProjetoMesaServiceTest`, `Parametro5sServiceTest`, `JwtServiceTest`.
  - [ ] Integração: `ProjetoTarefaIntegrationTest`, `KanbanIntegrationTest`, `BaixaEstoqueIntegrationTest`, `MaquinaIntegrationTest`, `Inspecao5sIntegrationTest`, `ProjetoMesaIntegrationTest`.
  - [ ] Mensageria: `RabbitMqIntegrationTest` (exchange `fablab.producao` + consumo de encomenda); eventos publicados verificáveis.
  - [ ] Concorrência: teste de movimentação simultânea no Kanban (lock otimista, sem perda de atualização).
  - [ ] Segurança: `SecurityConfigTest` valida a matriz RBAC (incluindo bloqueio de Recrutandos no 5S).
  - [ ] `mvn test` (ou padrão do repo) verde.
  - [ ] Serviço sobe no docker-compose com Postgres/RabbitMQ/Eureka e migrations V1..V18 aplicadas.
- **Depende de:** task-011, task-013, task-015, task-017, task-019, task-021.

## Tarefa 024: Documentação e entrega
- **Escopo:** `producao-service/README.md`, `docs/API.md`, `docs/CHANGELOG.md`, perfil docker-compose.
- **Entradas:** módulo completo e validado.
- **Saídas:** documentação de execução, endpoints e decisões alinhadas a `docs/_archive/2026-10-19/06`.
- **Critérios:**
  - [ ] README com requisitos (Ubuntu Server, Docker Compose, Tailscale), subida, variáveis de ambiente (`PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RABBITMQ_*`, `EUREKA_URI`).
  - [ ] `docs/API.md` com todos os endpoints em PT e exemplos de payload.
  - [ ] `CHANGELOG.md` registrando o escopo entregue.
  - [ ] Dockerfile/healthcheck e perfil compose do `producao-service` prontos.
  - [ ] Divergências apontadas à doc `06` registradas (font: `docs/_archive/2026-10-19/06` prevalece).
- **Depende de:** task-023.
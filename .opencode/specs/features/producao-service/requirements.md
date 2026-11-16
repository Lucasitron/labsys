# Requirements: Produção & Projetos Service

## 1. Objetivo e Escopo

O `producao-service` (Produção & Projetos) é o microsserviço responsável pela **gestão operacional do Fab Lab**. Ele gerencia:

- **Projetos e tarefas** (cadastro, prazos, prioridades, responsáveis).
- **Kanban de encomendas** (inserção, movimentação, histórico) com integração ao **Estoque** para baixa automática de insumos via **BOM**.
- **Máquinas** (cadastro, status e histórico de uso com cálculo de horas).
- **Sistema completo de gestão 5S**: setores, materiais, sinalizações, checklists, responsáveis rotativos, inspeções semanais, não conformidades, **advertências com penalidades** (3 advertências = suspensão), **auditoria de mesas de projeto** (QR totem) e **período experimental**.

Executa em **Java 21 + Spring Boot**, usa **PostgreSQL** (banco `fablab_producao`), registra-se no **Eureka** (`http://localhost:8761/eureka`) na porta `8086`, e se comunica por REST (síncrono) e **RabbitMQ** (assíncrono, exchange `fablab.producao` na 5672). Ambiente de execução local (Ubuntu Server + Docker Compose + Tailscale). Recursos, endpoints e mensagens em **PT-BR**.

Fonte de verdade: `docs/_archive/2026-10-19/06-Produção_&_Projetos_Service.md`. Onde este documento divergir, prevalece a doc `06`.

## 2. Funcionalidades

### F1 - Gestão de Projetos
- Cadastrar, consultar e atualizar projetos via `projeto` (`nome`, `descricao`, `data_inicio`, `data_fim_prevista`, `data_fim_real`, `status` Planejado/Em Andamento/Concluído/Cancelado, `id_responsavel` FK → `funcionario`).
- Listar projetos com filtros por `status` e por responsável.

### F2 - Gestão de Tarefas
- Cadastrar, consultar e atualizar tarefas via `tarefa` (`titulo`, `descricao`, `id_responsavel`, `data_inicio`, `data_fim_prevista`, `data_conclusao`, `status` Pendente/Em Andamento/Concluída/Atrasada, `prioridade` Baixa/Média/Alta).
- Tarefa vinculada obrigatoriamente a um `projeto` (`id_projeto` FK).
- Listar tarefas com filtros (projeto, responsável, status, prioridade).

### F3 - Kanban de Encomendas
- Inserir encomenda aprovada (Vendas) no Kanban via `encomenda_kanban` (`id_encomenda`, `status` Fila/Produção/Acabamento/Pronto/Entregue, `ordem`, `id_responsavel`).
- **`PUT /kanban/{id}/mover`**: movimenta o cartão entre status com **controle de concorrência** (lock otimista).
- Cada movimentação registra `historico_kanban` (`status_anterior`, `status_novo`, `data_alteracao`, `id_usuario`, `observacao`) e publica `kanban.status.alterado.event`.
- Ao entrar em **Produção**, consulta a **BOM** no Estoque.
- Ao concluir a produção, dispara a **baixa automática de estoque** conforme a BOM final (registro em `consumo_encomenda`).
- Listar cartões do Kanban (`GET /kanban`).

### F4 - Máquinas e Histórico de Uso
- Cadastrar e listar máquinas via `maquina` (`nome`, `descricao`, `status` Disponível/Em Uso/Manutenção, `localizacao`).
- `PUT /maquinas/{id}/status`: atualiza status (ex.: colocar em Manutenção).
- `POST /maquinas/{id}/uso`: registra início de uso (máquina passa a **Em Uso**).
- `PUT /maquinas/{id}/uso/{id_uso}/fim`: registra fim de uso, calcula `horas_uso` e devolve a máquina a **Disponível**.
- `GET /maquinas/{id}/historico`: lista o histórico da máquina.

### F5 - Setores 5S (Cadastro Completo)
- Cadastrar, listar e atualizar setores via `setor` (`numero`, `nome`, `descricao`, `observacoes`, `foto_correto_url`, `foto_incorreto_url`, `ativo`).
- Materiais esperados de cada setor em `setor_material` (`descricao`, `quantidade`).
- Sinalizações impressas em `setor_sinalizacao` (`texto`).
- Checklist de verificação em `setor_checklist` (`item`, `ativo`), editável.
- Designação e listagem de responsáveis em `setor_responsavel` (`data_inicio`, `data_fim`, `ativo`) com **rotação**.

### F6 - Inspeções Semanais 5S
- Realizar inspeção semanal via `inspecao_5s` (`id_setor`, `id_inspetor`, `data_inspecao`, `turno` Manhã/Tarde, `status` OK/NAO_CONFORME, `observacoes`).
- Para cada setor, carregar o checklist **ativo** (`setor_checklist`) e marcar cada item em `item_inspecao_5s` (`conforme`, `observacao`).
- Havendo não conformidade, habilitar registro de advertência para o responsável do setor.
- Listar inspeções (`GET /inspecoes-5s`).

### F7 - Advertências e Penalidades 5S
- Registrar advertência via `advertencia_membro` (`id_funcionario`, `id_inspecao` opcional, `data`, `motivo`, `tipo` VERBAL/FORMAL, `contador` 1/2/3, `id_admin_registrou`).
- Contador sequencial por membro; ao atingir **3 advertências**, disparar **alerta crítico ao Admin** (via `advertencia.registrada.event`).
- Listar advertências por funcionário (`GET /advertencias/{id_funcionario}`).

### F8 - Auditoria de Mesas de Projeto (QR Totem)
- Membro reserva a mesa criando `projeto_mesa` (`id_mesa`, `nome_projeto`, `tipo_projeto`, `prazo_execucao`, `data_inicio`, `status` ATIVO/ABANDONADO/CONCLUIDO).
- Geração de **QR Code único** (`qr_code_totem`) vinculado ao `id_projeto_mesa` para o totem físico.
- `PUT /projetos-mesa/{id}/evolucao`: registra evolução e atualiza `data_ultima_evolucao`.
- Auditoria automática quando `data_ultima_evolucao` ultrapassa `dias_para_auditoria_projeto`; auditoria manual pelo Admin via `auditoria_projeto_mesa` (`resultado` ATIVO/ABANDONADO, `acao_tomada`, `id_admin_responsavel`).
- Se abandonado: status → ABANDONADO, totem removido, e `projeto.mesa.abandonado.event` notifica RH/Notification.

### F9 - Parâmetros do Sistema 5S
- Configurar via `parametro_5s` (`chave`, `valor`, `descricao`): `dias_para_auditoria_projeto`, `dia_semana_inspecao`, `periodo_experimental_ativo`, `rotacao_dias`.
- `GET /parametros-5s` e `PUT /parametros-5s/{id}` (Admin).

### F10 - Período Experimental 5S
- Com `periodo_experimental_ativo = true`, não conformidades são registradas mas **não contabilizam advertências formais** para penalidades.
- Rotação de responsáveis pode ser configurada de forma reduzida (ex.: 14 dias).

### F11 - Integração BOM / Baixa de Estoque
- Ao concluir a produção, registrar em `consumo_encomenda` os itens consumidos da BOM e notificar o **Estoque** para baixa automática do saldo (`tipo_saida = Consumo`, `id_referencia = id_encomenda`).
- A baixa só ocorre **após a conclusão da produção** e com a **BOM final editada**.

### F12 - Scheduler 5S (Rotação e Agenda)
- Scheduler verifica periodicamente prazos: fim de rotação de responsáveis (notifica Admin), `dia_semana_inspecao` (abre janela da inspeção semanal e alerta inspetores) e mesas sem evolução recente (agenda auditoria).
- Controlado por properties `producao.5s.scheduler.enabled` / `producao.5s.scheduler.cron` (ver Não-Funcionais).

## 3. Tabelas (Modelo de Dados)

Campo(s) marcados com `*` são chave primária (Serial/Identity).

### 3.1 Produção e Projetos (V1..V4)
| Tabela | Campos |
|---|---|
| `projeto` | `id_projeto`* (Serial), `nome` (String), `descricao` (String), `data_inicio` (Date), `data_fim_prevista` (Date), `data_fim_real` (Date), `status` (String: Planejado/Em Andamento/Concluído/Cancelado), `id_responsavel` (Inteiro, FK → `funcionario`) |
| `tarefa` | `id_tarefa`* (Serial), `id_projeto` (Inteiro, FK → `projeto`), `titulo` (String), `descricao` (String), `id_responsavel` (Inteiro, FK → `funcionario`), `data_inicio` (Date), `data_fim_prevista` (Date), `data_conclusao` (Date), `status` (String: Pendente/Em Andamento/Concluída/Atrasada), `prioridade` (String: Baixa/Média/Alta) |
| `encomenda_kanban` | `id_kanban`* (Serial), `id_encomenda` (Inteiro, ref. externa → Vendas & CRM), `status` (String: Fila/Produção/Acabamento/Pronto/Entregue), `data_entrada_status` (Timestamp), `id_responsavel` (Inteiro, FK → `funcionario`), `ordem` (Inteiro) |
| `historico_kanban` | `id_historico`* (Serial), `id_encomenda` (Inteiro, ref. externa), `status_anterior` (String), `status_novo` (String), `data_alteracao` (Timestamp), `id_usuario` (Inteiro, FK → `funcionario`), `observacao` (String) |

### 3.2 Máquinas (V5..V6)
| Tabela | Campos |
|---|---|
| `maquina` | `id_maquina`* (Serial), `nome` (String), `descricao` (String), `status` (String: Disponível/Em Uso/Manutenção), `localizacao` (String) |
| `historico_uso_maquina` | `id_uso`* (Serial), `id_maquina` (Inteiro, FK → `maquina`), `id_funcionario` (Inteiro, FK → `funcionario`), `data_inicio` (Timestamp), `data_fim` (Timestamp), `horas_uso` (Decimal), `observacao` (String) |

### 3.3 Sistema 5S - Setores (V7..V11)
| Tabela | Campos |
|---|---|
| `setor` | `id_setor`* (Serial), `numero` (Inteiro, ex: 1, 2, 3), `nome` (String, ex: "Bancada de eletrônica"), `descricao` (String), `observacoes` (String), `foto_correto_url` (String), `foto_incorreto_url` (String), `ativo` (Booleano) |
| `setor_material` | `id_material`* (Serial), `id_setor` (Inteiro, FK → `setor`), `descricao` (String), `quantidade` (Decimal) |
| `setor_sinalizacao` | `id_sinalizacao`* (Serial), `id_setor` (Inteiro, FK → `setor`), `texto` (String) |
| `setor_checklist` | `id_checklist`* (Serial), `id_setor` (Inteiro, FK → `setor`), `item` (String), `ativo` (Booleano) |
| `setor_responsavel` | `id_responsavel`* (Serial), `id_setor` (Inteiro, FK → `setor`), `id_funcionario` (Inteiro, FK → `funcionario`), `data_inicio` (Date), `data_fim` (Date, para rotação), `ativo` (Booleano) |

### 3.4 Sistema 5S - Inspeções e Advertências (V12..V14)
| Tabela | Campos |
|---|---|
| `inspecao_5s` | `id_inspecao`* (Serial), `id_setor` (Inteiro, FK → `setor`), `id_inspetor` (Inteiro, FK → `funcionario`), `data_inspecao` (Date), `turno` (String: Manhã/Tarde), `status` (String: OK/NAO_CONFORME), `observacoes` (String) |
| `item_inspecao_5s` | `id_item_inspecao`* (Serial), `id_inspecao` (Inteiro, FK → `inspecao_5s`), `id_checklist` (Inteiro, FK → `setor_checklist`), `conforme` (Booleano), `observacao` (String) |
| `advertencia_membro` | `id_advertencia`* (Serial), `id_funcionario` (Inteiro, FK → `funcionario`), `id_inspecao` (Inteiro, FK → `inspecao_5s`, **opcional**), `data` (Date), `motivo` (String), `tipo` (String: VERBAL/FORMAL), `contador` (Inteiro, 1/2/3), `id_admin_registrou` (Inteiro, FK → `funcionario`) |

### 3.5 Sistema 5S - Mesas de Projeto e Parâmetros (V15..V17)
| Tabela | Campos |
|---|---|
| `projeto_mesa` | `id_projeto_mesa`* (Serial), `id_funcionario` (Inteiro, FK → `funcionario`, dono do projeto), `id_mesa` (String, ex: "Mesa 1", "Sala 59"), `nome_projeto` (String), `tipo_projeto` (String: TCC/Pesquisa/etc.), `prazo_execucao` (Date), `data_inicio` (Date), `data_ultima_evolucao` (Date), `status` (String: ATIVO/ABANDONADO/CONCLUIDO), `qr_code_totem` (String, QR gerado para o totem físico) |
| `auditoria_projeto_mesa` | `id_auditoria`* (Serial), `id_projeto_mesa` (Inteiro, FK → `projeto_mesa`), `data_auditoria` (Date), `resultado` (String: ATIVO/ABANDONADO), `acao_tomada` (String, ex: "Totem removido, materiais recolhidos"), `id_admin_responsavel` (Inteiro, FK → `funcionario`) |
| `parametro_5s` | `id_parametro`* (Serial), `chave` (String, ex: `dias_para_auditoria_projeto`, `dia_semana_inspecao`, `periodo_experimental_ativo`, `rotacao_dias`), `valor` (String), `descricao` (String) |

### 3.6 Baixa de Estoque via BOM (V18)
| Tabela | Campos |
|---|---|
| `consumo_encomenda` | Registro da baixa automática de insumos por encomenda ao fim da produção: vincula `id_encomenda` (ref. Vendas), a **BOM final** e os itens/quantidades consumidos, com data e usuário responsável (campos detalhados na migration V18; contrato de baixa no Estoque) |

## 4. Fluxos

### F - Criação e Gestão de Projetos/Tarefas
1. Responsável cadastra `projeto` com prazo previsto.
2. Tarefas são criadas vinculadas ao projeto, com responsável, prazo e prioridade.
3. Conclusão de tarefa registra `data_conclusao`; conclusão do projeto registra `data_fim_real` e atualiza `status`.

### F - Kanban de Encomendas + Baixa de Estoque via BOM
1. Encomenda **aprovada** (Vendas) é inserida no Kanban (`POST /kanban`, status inicial `Fila`).
2. Ao mover para **Produção**, o sistema consulta a **BOM** no Estoque.
3. Movimentações atualizam `status`/`ordem`/`data_entrada_status`, gravam `historico_kanban` e publicam `kanban.status.alterado.event`.
4. Ao mover para **Entregue** (conclusão da produção), registra `consumo_encomenda` com a BOM final e notifica o **Estoque** para a baixa automática do saldo.

### F - Registro de Uso de Máquinas
1. `POST /maquinas/{id}/uso` abre uso: registra `data_inicio`, `id_funcionario`, status da máquina → **Em Uso**.
2. `PUT /maquinas/{id}/uso/{id_uso}/fim` fecha uso: registra `data_fim`, calcula `horas_uso` (Decimal), status → **Disponível**.
3. Histórico completo consultável (`historico_uso_maquina`).

### F - Rotação de Responsáveis (5S)
1. Admin configura período de rotação em `parametro_5s.rotacao_dias`.
2. Scheduler notifica quando o período está **próximo do fim**.
3. Admin designa novos responsáveis em `setor_responsavel` (encerra `data_fim` dos atuais, cria novos com `ativo = true`).
4. **Recrutandos são excluídos automaticamente** da rotação.

### F - Inspeção Semanal (5S)
1. No `dia_semana_inspecao` configurado, o inspetor inicia a inspeção do setor.
2. O sistema carrega o **checklist ativo** (`setor_checklist.ativo = true`).
3. O inspetor marca itens como Conforme/Não Conforme em `item_inspecao_5s`.
4. Havendo não conformidade, o sistema permite registrar **advertência** ao responsável do setor.
5. O sistema verifica o contador de advertências; ao atingir **3**, dispara alerta crítico ao Admin.

### F - Auditoria de Projetos em Mesas (5S)
1. Membro reserva a mesa criando `projeto_mesa` e gerando `qr_code_totem` (QR único vinculado ao `id_projeto_mesa`).
2. O sistema agenda **auditoria automática** quando `data_ultima_evolucao` ultrapassa `dias_para_auditoria_projeto`.
3. O Admin realiza a auditoria (`auditoria_projeto_mesa`). Se abandonado → status **ABANDONADO**, totem removido e `projeto.mesa.abandonado.event`.

### F - Período Experimental (5S)
1. Admin ativa `periodo_experimental_ativo = true`.
2. Não conformidades são registradas, mas **não contabilizam advertências formais** para penalidades.
3. Rotação pode ser configurada mais rápida (ex.: 14 dias).

## 5. Endpoints (Especificação PT)

### Produção e Projetos
- `POST /projetos` — cria novo projeto.
- `GET /projetos` — lista projetos com filtros (status, responsável).
- `PUT /projetos/{id}` — atualiza projeto.
- `POST /tarefas` — cria nova tarefa.
- `PUT /tarefas/{id}` — atualiza tarefa.
- `GET /tarefas` — lista tarefas com filtros.
- `POST /kanban` — insere encomenda no Kanban.
- `PUT /kanban/{id}/mover` — move encomenda no Kanban.
- `GET /kanban` — lista cartões do Kanban.

### Máquinas
- `POST /maquinas` — cadastra máquina.
- `GET /maquinas` — lista máquinas.
- `PUT /maquinas/{id}/status` — atualiza status da máquina.
- `POST /maquinas/{id}/uso` — registra início de uso.
- `PUT /maquinas/{id}/uso/{id_uso}/fim` — registra fim de uso.
- `GET /maquinas/{id}/historico` — lista histórico de uso.

### Sistema 5S
- `POST /setores` — cadastra setor.
- `GET /setores` — lista setores.
- `PUT /setores/{id}` — atualiza setor.
- `POST /setores/{id}/checklist` — adiciona item ao checklist.
- `PUT /setores/{id}/checklist/{id_item}` — edita item do checklist.
- `POST /setores/{id}/responsaveis` — designa responsável.
- `GET /setores/{id}/responsaveis` — lista responsáveis.
- `POST /inspecoes-5s` — cria inspeção semanal.
- `GET /inspecoes-5s` — lista inspeções.
- `POST /advertencias` — registra advertência.
- `GET /advertencias/{id_funcionario}` — lista advertências de um membro.
- `POST /projetos-mesa` — cria projeto em mesa individual.
- `GET /projetos-mesa` — lista projetos em mesas.
- `PUT /projetos-mesa/{id}/evolucao` — registra evolução do projeto.
- `POST /auditorias-projeto-mesa` — realiza auditoria de projeto em mesa.
- `GET /parametros-5s` — lista parâmetros do 5S.
- `PUT /parametros-5s/{id}` — atualiza parâmetro do 5S.

## 6. Integrações

| Integração | Tipo | Direção | Evento / Uso |
|---|---|---|---|
| Estoque & Suprimentos | REST + Mensageria (RabbitMQ 5672) | Consome e Publica | Consulta da **BOM** para produção; **baixa automática** de insumos ao fim da produção (`consumo_encomenda`, `tipo_saida = Consumo`, `id_referencia = id_encomenda`) |
| Vendas & CRM | Mensageria (exchange `fablab.producao`) | Consome e Publica | Recebe encomendas **aprovadas** para inserção no Kanban; envia atualizações de status via `kanban.status.alterado.event` (Vendas sincroniza `status_kanban` da encomenda) |
| Pessoas & RH | REST | Consome | Consulta de funcionários para atribuição de responsáveis (projetos, tarefas, setores, donos de mesa, inspetores) e para notificação de abandono de mesa |
| Financeiro | Mensageria (exchange `fablab.producao`) | Publica | Sem integração direta de cálculo: envia eventos de produção/conclusão; o **custo de produção é calculado pelo Financeiro** com base em horas e BOM |
| Notification | Mensageria (exchange `fablab.producao`) | Publica | `advertencia.registrada.event`, `projeto.mesa.abandonado.event`, alertas de prazos, inspeções 5S, rotação de responsáveis e planos de ação |
| Service Discovery | Eureka | — | Registro em `http://localhost:8761/eureka`, aplicação `producao-service`, porta `8086` |
| RabbitMQ | Mensageria | — | Exchange tópica `fablab.producao` (host `localhost`, porta `5672`) |

## 7. RBAC

Autenticação via JWT com claims `id_user`, `role`, `setor` (Auth & Identity). Aplicação via `@PreAuthorize`. **Produção operacional (projeto/tarefa/Kanban/máquina) restrita ao responsável atribuído**; fluxos 5S concentrados no **Admin/Admin**; **Recrutandos não participam do 5S**.

| Recurso (Endpoint) | Admin | Bolsista | Voluntário | Estagiário | Recrutando |
|---|---|---|---|---|---|
| `POST/GET/PUT /projetos`, `/tarefas` | ✅ | ✅ (se responsável atribuído) | ✅ (se responsável atribuído) | apenas visualiza (se atribuído) | — |
| `POST /kanban` (inserção de encomenda aprovada) | ✅ | ✅ | — | — | — |
| `PUT /kanban/{id}/mover` | ✅ | ✅ (responsável da encomenda) | ✅ (responsável da encomenda) | — | — |
| `GET /kanban` | ✅ | ✅ | ✅ | ✅ | — |
| `POST /maquinas`, `PUT /maquinas/{id}/status` | ✅ | ✅ | — | — | — |
| `POST /maquinas/{id}/uso`, `PUT .../uso/{id_uso}/fim` | ✅ | ✅ | ✅ | (se atribuído) | — |
| `GET /maquinas`, `GET /maquinas/{id}/historico` | ✅ | ✅ | ✅ | ✅ | — |
| `POST/GET/PUT /setores`, checklist | ✅ | ✅ | — | — | — |
| `POST/GET /setores/{id}/responsaveis` (designação) | ✅ | — | — | — | — |
| `POST/GET /inspecoes-5s` | ✅ | ✅ (como inspetor) | ✅ (como inspetor) | (se atribuído) | **não participa** |
| `POST /advertencias`, `POST /auditorias-projeto-mesa` | ✅ | — | — | — | — |
| `GET /advertencias/{id_funcionario}` | ✅ | ✅ (próprias) | ✅ (próprias) | ✅ (próprias) | — |
| `POST/GET /projetos-mesa`, `PUT /{id}/evolucao` | ✅ | ✅ (próprio) | ✅ (próprio) | ✅ (próprio) | — |
| `GET /parametros-5s` | ✅ | ✅ | ✅ | ✅ | — |
| `PUT /parametros-5s/{id}` | ✅ | — | — | — | — |

## 8. Não-Funcionais

- **Idioma:** recursos, endpoints e mensagens em PT-BR.
- **Persistência:** PostgreSQL `fablab_producao` (usuário/senha `fablab`), `spring.jpa.hibernate.ddl-auto=validate`, **Flyway** V1..V18 (`classpath:db/migration`).
- **Porta:** `8086` (`${PORT:8086}`), registrado no Eureka como `producao-service`.
- **Mensageria:** RabbitMQ (`localhost:5672`), exchange **`fablab.producao`**, `spring.rabbitmq` com credenciais `fablab`.
- **Validação:** DTOs com Bean Validation (tamanhos, datas, enums, quantidades positivas).
- **Concorrência:** locks otimistas para movimentação no **Kanban**, atualização de status de máquinas e designação de responsáveis de setor.
- **Auditoria:** todas as movimentações de Kanban, uso de máquinas, inspeções 5S, advertências e auditorias de projeto registradas com **usuário e data**.
- **Isolamento:** o serviço **não armazena senhas/tokens** de usuário; banco próprio (`fablab_producao`), sem compartilhamento de schema com os demais serviços.
- **Integração com Estoque:** baixa automática **somente após a conclusão da produção** e com a **BOM final editada**.
- **QR Code:** único e vinculado ao `id_projeto_mesa`, para consulta e auditoria do totem.
- **Schedulers:** controlados por properties, ex. `producao.5s.scheduler.enabled` / `producao.5s.scheduler.cron` (agenda de inspeção `dia_semana_inspecao`, rotação `rotacao_dias` e auditoria de mesas `dias_para_auditoria_projeto`).
- **Observabilidade:** Actuator com `health,info,metrics`; logging `com.fablab.producao: INFO`.

## 9. Critérios de Sucesso

- [ ] Projeto e tarefa criados/atualizados com validação; tarefa sempre vinculada a um projeto.
- [ ] Encomenda aprovada entra no Kanban; `PUT /kanban/{id}/mover` atualiza status/ordem/data, grava `historico_kanban` (usuário + data) e publica `kanban.status.alterado.event`.
- [ ] Movimentações concorrentes no Kanban não geram perda de atualização (lock otimista).
- [ ] Ao concluir a produção, `consumo_encomenda` é registrado com a BOM final e o Estoque executa a baixa automática (sem estoque negativo).
- [ ] Uso de máquina abre/fecha corretamente, calcula `horas_uso` e alterna status Disponível/Em Uso/Manutenção.
- [ ] Setor cadastrado com materiais, sinalizações, checklist ativo e histórico de responsáveis por rotação; recrutandos excluídos da rotação.
- [ ] Inspeção semanal cria `inspecao_5s` + `item_inspecao_5s` por item do checklist ativo; não conformidade habilita advertência.
- [ ] Advertência registrada por Admin com contador sequencial; **3ª advertência dispara alerta crítico** (`advertencia.registrada.event` → Notification).
- [ ] Projeto em mesa cria `qr_code_totem` único; evolução atualiza `data_ultima_evolucao`; auditoria marca ABANDONADO, remove totem e publica `projeto.mesa.abandonado.event` (RH/Notification).
- [ ] Parâmetros 5S alteráveis (Admin); período experimental não gera advertências formais.
- [ ] Scheduler dispara inspeções/rotação/auditoria conforme `parametro_5s` e properties de cron.
- [ ] JWT/claims validadas em todos os endpoints protegidos; matriz RBAC aplicada.

## 10. Fora de Escopo

- Gestão de encomendas, clientes e aprovação comercial (Vendas & CRM).
- Controle físico de estoque, fornecedores e BOM (Estoque & Suprimentos — o serviço apenas consulta a BOM e solicita baixa).
- Cálculo financeiro de custo de produção (Financeiro consome horas + BOM).
- Gestão de pessoas, níveis de acesso e credenciais (Auth & Identity / RH).
- Impressão física dos QR totem e upload de fotos de setor (o serviço persiste apenas a URL em `foto_correto_url`/`foto_incorreto_url`).
- Integração direta com comunicação externa (e-mail/SMS — sempre via Notification Service).
# Requirements: Pessoas & RH Service

## Objetivo

O `rh-service` (Pessoas & RH) gerencia todas as pessoas do Fab Lab (professores, alunos bolsistas/voluntários/estagiários e candidatos ao processo seletivo), cobrindo o ciclo de vida do integrante desde o recrutamento até o desligamento. Responsável por: cadastro de pessoas e funcionários, registro de ponto via RFID, apontamento e validação de horas (encomenda/projeto), processo seletivo, treinamentos (LMS) com avaliações, evolução dos níveis de acesso com histórico, certificados de horas e extrato mensal de horas. Executa em Java 21 + Spring Boot com PostgreSQL.

## Funcionalidades

### F1 - Cadastro de Pessoas
- Criar, consultar e atualizar pessoas (professores, alunos, clientes) via `pessoa`.
- Campos: `nome_completo`, `matricula` (único), `data_admissao`, `contato`, `turno`, `status` (Ativo, Inativo, Recrutando).

### F2 - Cadastro e Gestão de Funcionários
- Vincular pessoa a `funcionario` com `nivel_acesso` (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário, 4-Recrutando) e `departamento`.
- Listar com filtros por nível e departamento.
- Alterar nível de acesso (exclusivo Admin): atualiza `funcionario`, grava `historico_nivel` e publica `nivel.alterado.event`.

### F3 - Registro de Ponto (RFID)
- Consumir `access.rfid.event` (Auth & Identity) e atualizar `registro_ponto_diario` (entrada, saída, `total_horas`).
- Coerência de horas: permitir ao Financeiro verificar se encomenda+projeto não excede presença do dia.

### F4 - Apontamento de Horas
- Registrar horas em ENCOMENDA ou PROJETO (`apontamento_horas`) com status PENDENTE.
- Administrador valida/rejeita (`PUT /apontamentos-horas/{id}/validar`).
- Apontamentos validados publicam `horas.validadas.event` para o Financeiro.

### F5 - Processo Seletivo
- Iniciar/atualizar processo com `processo_seletivo` (Inscrito, Em Triagem, Entrevista, Aprovado, Reprovado) e `resultado_final`.
- Criação de candidato: pessoa + funcionário com `nivel_acesso = 4` (Recrutando) e `status` Recrutando.

### F6 - Treinamentos (LMS) e Avaliações
- Criar treinamentos (`treinamento`) com `titulo`, `descricao`, `url_conteudo`, `id_tutor`.
- Avaliar aluno por nota (0 a 10) e feedback em `avaliacao_treinamento`.

### F7 - Nível de Acesso e Histórico
- Somente Admin altera `nivel_acesso`; registrar em `historico_nivel` (`nivel_antigo`, `nivel_novo`, `id_admin_alterou`, `data_alteracao`).
- Notificar Auth & Identity para atualizar permissões/token.

### F8 - Certificados de Horas
- Solicitar certificado (EXTENSAO, COMPLEMENTAR, ESTAGIO) em `solicitacao_certificado` (status PENDENTE).
- Calcular horas disponíveis = apontamentos VALIDADOS e `consolidado = false`.
- Admin aprova/rejeita; na aprovação: emitir `certificado_emitido` com `codigo_verificacao` único (UUID), marcar apontamentos `consolidado = true` e gravar `hora_consolidada`.
- Publicar `certificado.solicitado.event`, `certificado.aprovado.event`, `certificado.rejeitado.event`.

### F9 - Extrato Mensal de Horas
- Scheduler `@Scheduled(cron="0 0 6 1 * *")` (1º dia do mês, 06:00) gera extrato do mês anterior para cada funcionário ativo (exceto Recrutandos).
- Publica `extrato.mensal.horas.event` consumido pelo Notification (envio por e-mail).

## Integrações

| Integração | Tipo | Direção | Evento / Uso |
|---|---|---|---|
| Auth & Identity (8081) | Mensageria (RabbitMQ 5672) | Publica | `nivel.alterado.event` (permite regeração de claims JWT) |
| Auth & Identity | Mensageria | Consome | `access.rfid.event` (exchange `fablab.access` → fila `rh.ponto.diario`) |
| Financeiro | Mensageria | Publica | `horas.validadas.event` (custo/coerência de horas) |
| Notification | Mensageria | Publica | `certificado.solicitado.event`, `certificado.aprovado.event`, `certificado.rejeitado.event`, `extrato.mensal.horas.event` |
| Produção & Projetos | REST | Consome | Consulta de funcionários/responsáveis e referências para apontamentos |
| Service Discovery | Eureka | — | Registro em `http://localhost:8761/eureka` |

## RBAC

Autenticação via JWT com claims `id_user`, `role`, `setor` (Auth & Identity). Aplicação via `@PreAuthorize`.

| Recurso (Endpoint) | Admin | Bolsista | Voluntário | Estagiário | Recrutando |
|---|---|---|---|---|---|
| `POST/GET/PUT /pessoas` | ✅ | ✅ | ✅ | ✅ | — |
| `GET/POST /funcionarios` | ✅ | ✅ | ✅ | ✅ | — |
| `PUT /funcionarios/{id}/nivel` | ✅ | — | — | — | — |
| `POST /apontamentos-horas` | ✅ | ✅ | ✅ | ✅ | — |
| `PUT /apontamentos-horas/{id}/validar` | ✅ | — | — | — | — |
| `GET /horas/disponiveis` | ✅ | ✅ | ✅ | ✅ | — |
| `POST/GET /processo-seletivo`, `PUT /{id}` | ✅ | ✅ | ✅ | ✅ | — |
| `POST /treinamentos`, `POST/GET /avaliacoes` | ✅ | ✅ | ✅ | ✅ | — |
| `POST /certificados/solicitar` | ✅ | ✅ | ✅ | ✅ | — |
| `GET /certificados/solicitacoes` | ✅ todas | ✅ próprias | ✅ próprias | ✅ próprias | — |
| `PUT /certificados/solicitacoes/{id}/aprovar\|rejeitar` | ✅ | — | — | — | — |
| `GET /certificados/emitidos` | ✅ todas | ✅ próprios | ✅ próprios | ✅ próprios | — |
| `GET /extrato-mensal-horas` | ✅ | ✅ | ✅ | ✅ | — |

## Não-Funcionais

- **Idioma:** recursos, endpoints e mensagens em PT-BR.
- **Persistência:** PostgreSQL `fablab_rh` (usuário/senha `fablab`), Flyway para schema.
- **Porta:** `8082`, registrado no Eureka (`fablab/rh-service`).
- **Validação:** DTOs com Bean Validation (e-mail, tamanhos, datas, enums).
- **Sincronia:** REST síncrono; assíncrono via RabbitMQ para eventos.
- **Isolamento:** o serviço NÃO armazena senhas/tokens do usuário.
- **Auditoria:** alterações de nível registradas em `historico_nivel`; decisões de certificado e consolidações persistidas.
- **Configuração:** scheduler controlado por `rh.certificado.extrato.scheduler.enabled` / `rh.certificado.extrato.scheduler.cron`.

## Critérios de Sucesso

- [ ] Pessoa e funcionário criados com validação e matrícula única; nível default para candidatos = 4 (Recrutando).
- [ ] Evento `access.rfid.event` produz entrada/saída coerente em `registro_ponto_diario` com `total_horas` calculado.
- [ ] Apontamento nasce PENDENTE; Admin valida/rejeita; validados disparam `horas.validadas.event`.
- [ ] Alteração de nível apenas por Admin, com `historico_nivel` gravado e `nivel.alterado.event` publicado.
- [ ] Solicitação de certificado calcula horas disponíveis; aprovação emite certificado (UUID), consolida apontamentos e grava `hora_consolidada`; horas consolidadas não reaproveitáveis.
- [ ] Scheduler mensal (1º dia, 06:00) publica `extrato.mensal.horas.event` por funcionário ativo (exceto Recrutandos).
- [ ] JWT/claims validadas em todos os endpoints protegidos; matriz RBAC aplicada.

## Fora de Escopo

- Armazenamento de senhas/credenciais (responsabilidade do Auth & Identity).
- Geração de PDF/templates `.tex` de certificado e extrato (definição futura).
- Endpoint público de verificação de autenticidade do certificado (estrutura pronta, fora do MVP).
- Gestão de encomendas e projetos em si (serviços de Produção/Vendas).
- Cálculo financeiro de custo de horas (Financeiro consome eventos).

## Modelo de Dados

| Tabela | Colunas-chave |
|---|---|
| `pessoa` | `id_pessoa`, `nome_completo`, `matricula` (único), `data_admissao`, `contato`, `turno`, `status` |
| `funcionario` | `id_funcionario`, `id_pessoa`, `nivel_acesso` (0-4), `departamento` |
| `tutor` | `id_tutor`, `id_funcionario`, `turno`, `qualificacao` (0-6) |
| `registro_ponto_diario` | `id_registro`, `id_funcionario`, `data`, `hora_entrada`, `hora_saida`, `total_horas` |
| `apontamento_horas` | `id_apontamento`, `id_funcionario`, `tipo` (ENCOMENDA/PROJETO), `id_referencia`, `data`, `horas_trabalhadas`, `descricao_atividade`, `status` (PENDENTE/VALIDADO/REJEITADO), `id_admin_validador`, `data_validacao`, `consolidado` (bool) |
| `processo_seletivo` | `id_processo`, `id_candidato`, `id_tutor`, `status_processo`, `data_inscricao`, `resultado_final` |
| `treinamento` | `id_treinamento`, `titulo`, `descricao`, `url_conteudo`, `id_tutor` |
| `avaliacao_treinamento` | `id_avaliacao`, `id_treinamento`, `id_funcionario`, `nota`, `feedback`, `data_avaliacao` |
| `historico_nivel` | `id_historico`, `id_funcionario`, `nivel_antigo`, `nivel_novo`, `id_admin_alterou`, `data_alteracao` |
| `solicitacao_certificado` | `id_solicitacao`, `id_funcionario`, `tipo_certificado`, `data_solicitacao`, `horas_solicitadas`, `status`, `id_admin_aprovador`, `data_decisao`, `observacao` |
| `certificado_emitido` | `id_certificado`, `id_solicitacao` (único), `id_funcionario`, `tipo_certificado`, `horas_certificadas`, `data_emissao`, `codigo_verificacao` (UUID único) |
| `hora_consolidada` | `id_consolidacao`, `id_certificado`, `id_apontamento`, `horas`, `data_consolidacao` |

## Regras de Negócio

- Horas disponíveis para certificado = apontamentos `status = VALIDADO` e `consolidado = false`.
- Ao aprovar, consolidar TODAS as horas disponíveis do integrante (sem certificação parcial no MVP); de rejeição, manter apontamentos disponíveis.
- Extrato mensal considera o mês anterior (fechado); Recrutandos (nível 4) não recebem extrato.
- `codigo_verificacao` único e não sequencial (UUID).
- Encomenda+projeto por dia não podem exceder as horas de presença do dia (endpoint de coerência para Financeiro).
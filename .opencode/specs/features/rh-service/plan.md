# Plan: Pessoas & RH Service

## Arquitetura

- **Stack:** Java 21 + Spring Boot, build padrão do monorepo (Maven/Gradle do projeto), monitorado/executado via Docker Compose local (Ubuntu Server + Tailscale).
- **Persistência:** PostgreSQL (banco `fablab_rh`, usuário/senha `fablab`) com Flyway (`V1..V10`).
- **Mensageria:** RabbitMQ (`amqp://localhost:5672`). Exchanges tópicas:
  - `fablab.access` → fila `rh.ponto.diario` com binding `access.rfid.event` (consumo).
  - `fablab.rh` → publicação de `nivel.alterado.event`, `horas.validadas.event`, `certificado.solicitado.event`, `certificado.aprovado.event`, `certificado.rejeitado.event`, `extrato.mensal.horas.event`.
- **Service Discovery:** registro no Eureka (`http://localhost:8761/eureka`), nome da aplicação `rh-service`, porta `8082`.
- **API:** REST síncrona, DTOs com Bean Validation, respostas padronizadas (`ErrorResponse`, `GlobalExceptionHandler`), exceções de domínio (`ResourceNotFoundException`, `ForbiddenException`).
- **Segurança:** JWT com claims `id_user`/`role`/`setor`, filtro `JwtAuthenticationFilter` + `SecurityConfig`, autorização por `@PreAuthorize`.

## Estrutura de Pacotes

```
com.fablab.rh
├── config      (SecurityConfig, Jwt*, RabbitMqConfig, handlers)
├── controller  (Pessoa, Funcionario, ProcessoSeletivo, ApontamentoHoras, Treinamento, Certificado, Horas)
├── dto         (requests/responses e eventos de domínio)
├── entity      (entidades JPA e enums com converters)
├── exception   (GlobalExceptionHandler, exceções de domínio)
├── mapper      (entity ↔ DTO)
├── repository  (Spring Data JPA)
└── service     (Pessoa, Funcionario, Ponto, ApontamentoHoras, ProcessoSeletivo, Treinamento, Certificado, Extrato, RhEventPublisher, RfidEventListener, ExtratoMensalScheduler, JwtService)
```

## Ordem de Implementação

1. **Setup do serviço** — scaffold Spring Boot 21, `application.yml` (porta 8082, datasource `fablab_rh`, RabbitMQ, Eureka, JWT), dependências.
2. **Migrations Flyway** — `V1..V10` com schema completo (ver tabelas em requirements.md).
3. **Entidades + converters JPA** — mapping para todas as tabelas, enums (`NivelAcesso`, `PessoaStatus`, `TipoApontamento`, `StatusApontamento`, `StatusProcesso`, `StatusSolicitacao`, `TipoCertificado`).
4. **Repositórios** — Spring Data JPA para todas as entidades com queries de consulta (filtros, horas disponíveis, consolidação).
5. **DTOs + Mappers** — requests/responses com Bean Validation e mapeamento entity↔DTO.
6. **Segurança** — filtro JWT, `RhPrincipal` (id_user, role, setor) e matriz `@PreAuthorize`.
7. **Cadastro de Pessoas/Funcionários** — `PessoaService`/`FuncionarioService` + controllers + `nivel.alterado.event`.
8. **Ponto RFID** — `RfidEventListener` + `PontoService` (entrada/saída/total).
9. **Apontamento de Horas** — `ApontamentoHorasService` (criar/validar/rejeitar) + `horas.validadas.event` + endpoint de coerência.
10. **Processo Seletivo** — `ProcessoSeletivoService` (cria candidato Recrutando, atualiza status).
11. **Treinamentos LMS** — `TreinamentoService` (criar treinamento, avaliar, listar avaliações).
12. **Certificados** — `CertificadoService` (solicitar, listar, aprovar com consolidação, rejeitar) + eventos.
13. **Extrato Mensal** — `ExtratoService` + `ExtratoMensalScheduler` (cron `0 0 6 1 * *`) + `extrato.mensal.horas.event`.
14. **Testes** — unitários (services), integração (controllers + RabbitMQ + JWT).
15. **Config/Docker/README** — perfil docker-compose, healthcheck, documentação de execução.

## Riscos e Mitigações

| Risco | Mitigação |
|---|---|
| Duplicidade de matrícula/e-mail | Constraints únicas + validação nos DTOs |
| Consumo duplicado/inconsistente de eventos RFID | Fila durável + idempotência por dia/funcionário no `PontoService` |
| Horas consolidadas reutilizadas | Transação na aprovação (emissão + consolidação + `hora_consolidada`); consulta sempre filtra `consolidado = false` |
| Não descoberta no Eureka / RabbitMQ down | Retry no RabbitTemplate, bindings declarativos (`RabbitMqConfig`), registro automático no Eureka |
| Nível alterado sem propagar ao Auth | Publicação do evento na mesma transação da alteração (outbox simples/commit-before-publish) |
| Extrato mensal fechado errado | Usar apenas mês anterior; excluir Recrutandos; cron configurável por property |
| Escalonamento de horas (encomenda > presença) | Endpoint de coerência consumido pelo Financeiro + validação defensiva |
| Acesso indevido por nível | `@PreAuthorize` por endpoint conforme matriz RBAC (requirements.md) |

## Integrações e Eventos

| Evento | Tipo | Payload | Consumidor |
|---|---|---|---|
| `access.rfid.event` | Consome (`fablab.access`) | `RfidAccessEvent` (id_funcionario, timestamp) | RH → `registro_ponto_diario` |
| `nivel.alterado.event` | Publica (`fablab.rh`) | `{ idFuncionario, nivelAnterior, nivelNovo }` | Auth & Identity (claims JWT) |
| `horas.validadas.event` | Publica (`fablab.rh`) | `{ idApontamento, idFuncionario, tipo, horas, status }` | Financeiro (custo/coerência) |
| `certificado.solicitado.event` | Publica (`fablab.rh`) | `{ idSolicitacao, idFuncionario, tipoCertificado, horasSolicitadas }` | Notification |
| `certificado.aprovado.event` | Publica (`fablab.rh`) | `{ idCertificado, idFuncionario, horasCertificadas }` | Notification |
| `certificado.rejeitado.event` | Publica (`fablab.rh`) | `{ idSolicitacao, idFuncionario, motivo }` | Notification |
| `extrato.mensal.horas.event` | Publica (`fablab.rh`) | `{ idFuncionario, mesReferencia, horasPresenca, horasEncomenda, horasProjeto, horasDisponiveis }` | Notification (e-mail) |

## Decisões Técnicas

- **Base URL dos recursos (PT):** `/pessoas`, `/funcionarios`, `/apontamentos-horas`, `/horas`, `/processo-seletivo`, `/treinamentos`, `/certificados`, `/extrato-mensal-horas`.
- **IDs longos** (`BIGSERIAL`) via JPA `GenerationType.IDENTITY`.
- **Enums persistidos** com converters dedicados (ex.: `NivelAcessoConverter`, `PessoaStatusConverter`).
- **Datas em UTC**: `TIMESTAMPTZ` com offsets controlados pelo JVM/Jackson.
- **Integração Auth p/ criação de credenciais** do candidato Recrutando fica a cargo do Auth & Identity (evento/contrato REST dedicado fora deste escopo).
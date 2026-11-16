# Plan: Financeiro Service

## Arquitetura

Monolito Spring Boot (Java 21, Spring Web + Spring Security + Spring Data JPA + Spring AMQP + Spring Cloud Netflix) implantado como microsserviço em Docker Compose local (Ubuntu Server + Tailscale).

```
                 ┌──────────────────────────────────────────────────────────┐
                 │                       financeiro-service                 │
                 │                       (Spring Boot, :8085)               │
                 │                                                          │
 Auth → JWT ───► │  Controllers (PT) ──► Services ──► Repositories ──► DB   │
 (role=ADMIN)    │       │                  │                │    PostgreSQL │
                 │       │                  │                │   fablab_fin  │
                 │   RabbitMQ ──── listener ┤  (financ.)    │   anceiro     │
                 │   (async)   │            │   Flyway V1..V9│               │
                 └─────────────┼────────────┼───────────────┴───────────────┘
              publica (fablab.financeiro)    │ consome eventos externos
   lancamento.vencido.event ──► Notification │ encomenda.criada.event  (fablab.vendas)
   custo.calculado.event    ──► Vendas & CRM │ horas.validadas.event   (fablab.rh)
   compra.solicitada.event  ──► Estoque      │ producao.concluida.event (fablab.producao)
                                             ▼
                                      Eureka discovery-service :8761
```

Decisões principais:
- **Pacotes:** `config`, `consumer`, `controller`, `dto`, `entity`, `exception`, `mapper`, `repository`, `service`.
- **Persistência:** PostgreSQL `fablab_financeiro` (user/pass `fablab`), `ddl-auto: validate` + Flyway (schema é a fonte de verdade), migrations `V1..V9`.
- **Segurança:** filter JWT stateless (`/actuator/health` permitAll; restante autenticado) + `@PreAuthorize("hasRole('ADMIN')")` por controlador.
- **Mensageria:** exchange tópica durável `fablab.financeiro`; filas próprias `financeiro.fechamento.encomenda`, `financeiro.horas.encomenda`, `financeiro.custo.calculo` ligadas às exchanges dos domínios produtores.
- **Custeio:** Job Order Costing com cálculo disparado por evento (`producao.concluida.event`), pré-condições resolvidas por consultas a vigências (valor/hora e overhead) e lançamentos de saída.
- **Pagamentos:** interface `PaymentProcessor` (abstração) — registro manual no MVP, sem acoplamento a provedor.

## Dependências

| Dependência | Papel |
|---|---|
| Java 21 + Maven 3.9+ | Runtime/build |
| Spring Web + Json | REST PT-BR, DTOs |
| Spring Data JPA | Repositórios, `ddl-auto: validate` |
| Flyway | Migrations `V1..V9` |
| PostgreSQL | Banco `fablab_financeiro` |
| Spring AMQP (RabbitMQ 5672) | Eventos (publica/consome) |
| Spring Security + `jjwt` | JWT e RBAC |
| Spring Cloud Netflix Eureka (8761) | Service discovery |
| Bean Validation | Validação de DTOs |
| Actuator | Health/Info/Metrics |
| JUnit 5 / Spring Boot Test / Testcontainers | Testes unit, integração, segurança |

Configuração por variáveis (padrões): `PORT=8085`, `DB_URL=jdbc:postgresql://localhost:5432/fablab_financeiro`, `DB_USERNAME/DB_PASSWORD=fablab`, `EUREKA_URI=http://localhost:8761/eureka`, `RABBITMQ_HOST/ROTT_PORT/USERNAME/PASSWORD`.

## Ordem de Implementação

1. **Setup do projeto** — `pom.xml`, `FinanceiroApplication` (`@SpringBootApplication`, `@EnableDiscoveryClient`, `@EnableConfigurationProperties(JwtProperties)`), `application.yml` (porta 8085, datasource, Flyway, RabbitMQ, Eureka, Actuator).
2. **Migrations Flyway V1..V9** — DDL das nove tabelas com enums/`CHECK`, `NUMERIC`, FKs e índices.
3. **Entities, enums, DTOs e mappers** — mapeamento das nove tabelas + DTOs de request/response (PT) + eventos (`LancamentoVencidoEvent`, `CustoCalculadoEvent`, `CompraSolicitadaEvent`, `EncomendaCriadaEvent`, `HorasValidadasEvent`, `ProducaoConcluidaEvent`).
4. **Repositories** — consultas funcionais (vencidos, soma de saídas por referência, soma de horas por encomenda, fechamento por encomenda, vigências).
5. **CategoriaFinanceiraService + LancamentoFinanceiroService** — CRUD de categorias; lançamentos (criar/listar/liquidar) + `emitirVencidos`.
6. **ValorHoraNivelService + ParametroOverheadService** — vigências de valor/hora por nível (0-3) e taxa de overhead.
7. **DoacaoRecursoService + SolicitacaoCompraService** — doações/recursos; solicitações de compra (`REGISTRADA`→`CONCLUIDA` + `compra.solicitada.event`).
8. **FechamentoEncomendaService** — abertura (manual e por evento, idempotente) e acúmulo de horas validadas em `horas_encomenda` + `horas_validadas`.
9. **CusteioService** — Job Order Costing (materiais + mão de obra + overhead + margem), pré-condições e publicação de `custo.calculado.event`.
10. **RelatorioService** — fluxo de caixa, DRE, lucratividade, inadimplência, doações vs. despesas, custo por máquina.
11. **Scheduler de lançamentos vencidos** — agendar a varredura (`emitirVencidos`) e/ou processo de liquidação; `@EnableScheduling` + `@Scheduled` (cron configurável via propriedade) chamando a varredura.
12. **RabbitMQ** — `RabbitMqConfig` (exchanges, filas, bindings), `FinanceiroEventPublisher` e listeners.
13. **Controllers + Security** — rotas PT, `@PreAuthorize` ADMIN, `JwtAuthenticationFilter`/`JwtProperties`, handlers de erro (`ErrorResponse`).
14. **Abstração de pagamentos** — interface `PaymentProcessor` e integração manual no MVP.
15. **Testes** — unitários (services/lógica de custeio), integração (migrations, repositórios, REST), segurança (RBAC ADMIN) e mensageria.
16. **Documentação** — `README.md`, `docs/API.md`, changelog.

## Riscos / Mitigações

| Risco | Impacto | Mitigação |
|---|---|---|
| Evento `horas.validadas.event` do RH não informa o nível do funcionário | Mão de obra subestimada/superestimada | Custeio com default nível 2 (Voluntário), documentado; alinhar contrato com RH |
| Pré-condições de custeio ausentes (sem fechamento, sem valor/hora, sem taxa de overhead) | Cálculo falha | Validações com mensagens claras e resolução das vigências vigentes |
| Vencidos não notificados (scheduler ausente no MVP) | Perda de receita/atraso | `emitirVencidos` idempotente + publicação imediata de `ATRASADO` no momento da criação; agendar varredura |
| Disputa de escrita no acúmulo de horas | Inconsistência em `horas_encomenda` | Regra de reagrupamento por (encomenda, funcionário, data) com criação idempotente |
| Lançamento/custo com `idReferenciaExterna` ambíguo (usado p/ encomenda e máquina) | Custo de materiais ou relatório por máquina incorretos | Convenção documentada: ref. externa = id_encomenda no custeio; = id_máquina no relatório `custo-maquina` |
| Dead letter / falha de publicador | Eventos perdidos | Publicações protegidas com try/catch + log; filas/exchanges duráveis |
| Esquema divergente entre DDL real e docs | Migrations quebradas | `ddl-auto: validate` + Flyway como fonte de verdade do schema |
| RBAC incorreto expõe dados sensíveis | Exposição financeira | Filter JWT default `authenticated` + `@PreAuthorize("hasRole('ADMIN')")` em todos os controladores |

## Integrações

- **Vendas & CRM:** consome `encomenda.criada.event` (abre fechamento `ABERTA`); publica `custo.calculado.event` (custo total + margem) para Vendas ao concluir o custeio.
- **Estoque & Suprimentos:** publica `compra.solicitada.event` (contrato `{idCompra, idFornecedor}`; `idFornecedor` null no MVP); o custo de materiais é calculado a partir dos lançamentos de saída referenciados à encomenda (consumo/BOM refletido em lançamentos).
- **RH (Pessoas & RH):** consome `horas.validadas.event` (`fablab.rh`) acumulando horas por (encomenda, funcionário, data); usa nível para custear mão de obra.
- **Produção & Projetos:** consome `producao.concluida.event` (`fablab.producao`) como gatilho do cálculo de custo por ordem.
- **Notification:** publica `lancamento.vencido.event` para alerta de contas vencidas, incoerência de horas e resumo de compras.
- **Auth & Identity / Eureka:** valida JWT (claims `role`, `setor`) e registra instância no discovery (`http://localhost:8761/eureka`).
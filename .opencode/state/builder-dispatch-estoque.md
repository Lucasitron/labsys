# Despacho Builder — ESTOQUE (estoque-001..estoque-009)

> Data: 20/09/2026 · Autorizado pelo PO ("pode seguir com a implementação de estoque-service").
> Base: branch **`dev/backend`** · nova vertente para `estoque-service`.
> Fonte da verdade: **`.opencode/specs/features/estoque-service/tasks.md`** (T1..T9, 1 seção ≡ 1 task atômica) + `requirements.md`.

## Feature
- **estoque** (status `SPEC_CADASTRADA`) — **scaffold vazio com histórico canônico**: spec PT travada
  (`docs/03-Estoque_&_Suprimentos_Service.md` + `docs/mokups/estoque/` requisitos 19 telas + `docs/contract.gap.md` §5.6).
- **Decisão PO confirmada:** naming **PT canônico** — `com/fablab/estoque/**`, endpoints `/api/estoque/**` (NUNCA `/stock/**` — Dívida 2 resolvida no despacho).
- **Rotas do gateway:** `/api/estoque/**` → `estoque-service:8083` (aplicar ao gateway-service, fora deste despacho — Builder NÃO toca `gateway-service`).

## 🔧 Tasks (cada linha = 1 task atômica; 1 seção tasks.md = 1 arquivo-alvo)

| Task | Arquivo-alvo (modify/create) | Critério de conclusão |
|---|---|---|
| estoque-001 | `pom.xml` + `application.yml` + `FablabEstoqueApplication.java` + `com/fablab/estoque/{controller,service,repository,entity,config,model,dto,rabbit}/` | setup bootável porta 8083, datasource `fablab_estoque`, Flyway, Eureka `fablab/estoque-service` |
| estoque-002 | `src/main/resources/db/migration/V1..V8__*.sql` + `com/fablab/estoque/entity/*.java` (8 entidades: localizacao,item,fornecedor,entrada_estoque,saida_estoque,emprestimo,lista_materiais,item_bom) | migrations V1..V8 aplicam limpas no `fablab_estoque` (dev); relações/FKs corretas |
| estoque-003 | `com/fablab/estoque/repository/*.java` (Item por localização/categoria/estoque-baixo, Entrada/Saida por item+referência, Emprestimo por status+atraso, Fornecedor, Localizacao ordenada, ListaMateriais por id_produto_servico+versão, ItemBom) | repositórios + `findByLocalizacaoId`, estoque baixo `<= estoque_minimo`, locks pessimistas p/ saldo |
| estoque-004 | `com/fablab/estoque/dto/*.java` + `@Valid` (ItemRequest/Response, EntradaRequest/Response, SaidaRequest/Response, EmprestimoRequest/Response, FornecedorRequest, LocalizacaoRequest, BomRequest, ItemBomDto) | validação Bean em PT-BR (nunca EN); enums categoria/tipo_saida/status; datas coerentes |
| estoque-005 | `com/fablab/estoque/service/*.java` (ItemService, EntradaService, SaidaService, EmprestimoService, BomService, FornecedorService, LocalizacaoService) | saldo nunca negativo (lock + validação), entrada atualiza `quantidade_atual`, baixa por BOM, empréstimo com devolução prevista, `estoque.baixo.event` quando `quantidade_atual <= estoque_minimo` |
| estoque-006 | `com/fablab/estoque/rabbit/*.java` + `com/fablab/estoque/config/RabbitConfig.java` | exchanges/queues RabbitMQ (fablab.*), publisher: `compra.solicitada.event`, `estoque.baixo.event`, `emprestimo.atrasado.event`; consumer da baixa automática de Produção; RabbitMQ `estoque.baixo.event` |
| estoque-007 | `com/fablab/estoque/scheduler/*.java` (EmprestimoAtrasadoScheduler) | job diário 03:00 marca empréstimos atrasados + publica `emprestimo.atrasado.event` (flag configurável) |
| estoque-008 | `com/fablab/estoque/controller/*.java` + `exception/*` + RBAC `@PreAuthorize` | endpoints PT (`/itens`, `/entradas`, `/saidas`, `/emprestimos`, `/fornecedores`, `/localizacoes`, `/boms`) com validations + `@PreAuthorize` (Admin/Bolsista editam; demais visualizam) + `@RestControllerAdvice` PT-BR p/ saldo insuficiente (409/422) |
| estoque-009 | `src/test/java/com/fablab/estoque/**` (unit + integração) | `mvn test` verde; integração: entrada→saída→saldo sem negativo, empréstimo→devolução→atraso, BOM→consumo→`saida_estoque` `tipo_saida=Consumo`, `estoque.baixo.event`, RBAC protege endpoints |

**Dependências:** estoque-001→002→003→004→005→006→007→008→009 (sequencial; 006/007 semi-paralelas após 005).

## Regras (Builder)
1. **Nunca** altera `docs/` (specs/mockups/contract.gap.md), specs de outra feature nem `gateway-service`/`frontend`.
2. **Nunca** escreve EN no código/schema (naming PT canônico: `estoque`, `itens`, `emprestimos`, `fornecedores`, `localizacoes`, `boms`, colunas `id_item`/`id_entrada`…).
3. Java 21 + Spring Boot; todos os nomes em PT-BR quando usuário-visível; código/types EN quando Java exigir.
4. Um commit por task (`feat(estoque): estoque-00X …`), mensagem PT; push na branch `dev/backend`.
5. Após cada task: `mvn test` + `mvn compile`; sem `console.log`-análogo; sem duplicação de lock (padrão do FabLab).
6. Ao terminar: preencher `## Relatório` abaixo, atualizar `.opencode/specs/features/estoque-service/tasks.md` marcando os `[ ]` de cada critério concluído, e avisar o Orchestrator (sai de `SPEC_CADASTRADA` → Validador).

## Relatório (Builder preenche ao final)
```json
{
  "tarefa": "estoque-service T1..T9",
  "status": "IMPLEMENTADO",
  "tasks_concluidas": ["estoque-001", "estoque-002", "estoque-003", "estoque-004", "estoque-005", "estoque-006", "estoque-007", "estoque-008", "estoque-009"],
  "resultado_mvn_test": "PASS",
  "porta": 8083,
  "eureka": "fablab/estoque-service",
  "blockedReason": null,
  "duvidas": []
}
```

## Notas do Builder
- **Vertente:** `feature/estoque-service` (branch-fork da `dev/backend`); 9 commits `feat(estoque): estoque-00X …`, mensagens PT, um por task.
- **Base:** implementação completa existente em 15/09 (não-canônica, ainda `/stock/**` e `id` genérico) foi usada como referência de padrões e **reescrita/corrigida** contra as fontes canônicas; backup da versão antiga na tag `pre-estoque-legacy`.
- **Correções aplicadas vs. base antiga:** PKs canônicas `id_item`/`id_localizacao`/`id_entrada`/`id_saida`/`id_emprestimo`/`id_bom`/`id_item_bom` + `versao` (@Version) em item; estoque baixo `quantidade_atual <= estoque_minimo` (era `<`); **BOLSISTA agora edita** itens/entradas/saídas/fornecedores/localizações/BOM (matriz RBAC do requirements); Estoque **publica** `compra.solicitada.event` na entrada (antes consumia do Financeiro — removido `CompraSolicitadaListener`); `SaldoInsuficienteException` → HTTP **409**; import/export CSV; Dívida 1 do contract.gap §5.6 atendida (`POST /localizacoes`, `GET /entradas/{id}`, `GET /saidas/{id}`, rota de detalhe `GET /emprestimos/{id}`); publisher confirms + DLQ no Rabbit; scheduler configurável (`estoque.alerta.emprestimo.scheduler.enabled`/`.cron`).
- **Padronização:** entidades reescritas **sem Lombok** (padrão do projeto — único serviço com security é auth); pom ganhou `spring-boot-starter-security`, `jjwt 0.12.6` e `h2` (teste).
- **Equivalência de nomes T4:** `ItemBomDto` → `BomItemResponse`/`BomItemRequest`; `LocalizacaoDto` → `LocalizacaoResponse`/`LocalizacaoRequest` (funcionalmente equivalentes, cobertos por testes).
- **Migrations:** V1..V8 aplicadas limpas em PostgreSQL 16 real (container Docker `fablab-pg-estoque`, porta 5433) além da suíte H2 dos testes.
- **Suíte:** `mvn clean test` = **69 testes, 0 falhas** (unit: services/CSV/saldo/concorrência; integração: fluxo entrada→saída sem negativo, empréstimo→devolução→atraso, BOM→consumo com `tipo_saida=CONSUMO`, idempotência da baixa por `id_referencia`, RBAC, Rabbit mockado).
- **Fora do escopo do Builder (não tocado):** rota do gateway `/api/estoque/**` → 8083 (fica para o despacho do gateway), `docs/`, `frontend/`.

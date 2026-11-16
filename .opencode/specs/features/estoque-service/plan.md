# Plan: Estoque & Suprimentos Service

## Arquitetura

Serviço Java 21 + Spring Boot com arquitetura em camadas, seguindo os padrões dos demais serviços do FabLab:

```
estoque-service (porta 8083)
├── Controller (REST, PT-BR) ── /itens, /entradas, /saidas, /emprestimos, /fornecedores, /localizacoes, /boms
├── Service (regras de negócio + locks de concorrência e validação de saldo)
├── Repository (Spring Data JPA)
├── Entity + DTO (Bean Validation)
├── Migration (Flyway V1..V8)
├── Rabbit (publicadores: estoque.baixo.event, emprestimo.atrasado.event, compra.solicitada.event; listener de baixa de produção)
└── Scheduler (@Scheduled cron="0 0 3 * * *" — verificação de empréstimos atrasados)
```

- **Persistência:** PostgreSQL `fablab_estoque` (fablab/fablab), schema gerenciado por Flyway (migrations V1..V8 na ordem: `localizacao`, `item`, `fornecedor`, `entrada_estoque`, `saida_estoque`, `emprestimo`, `lista_materiais`, `item_bom`).
- **Descoberta:** registro no Eureka em `http://localhost:8761/eureka` sob o nome `fablab/estoque-service`.
- **Mensageria:** RabbitMQ em `5672`; o serviço **publica** eventos de alerta/compra e **consome** evento de baixa automática vindo do Produção.
- **Concorrência:** `@Lock(PESSIMISTIC_WRITE)`/`@Version` sobre `item.quantidade_atual` para impedir estoque negativo em entradas/saídas/consumos simultâneos.

## Ordem de Implementação

1. **Setup do projeto** — boot do serviço Spring Boot na porta `8083`, config Eureka, RabbitMQ, PostgreSQL `fablab_estoque`, Flyway.
2. **Migrations e entidades (V1..V8)** — criar schema e entidades na ordem de dependência (localizacao → item → fornecedor → entrada_estoque → saida_estoque → emprestimo → lista_materiais → item_bom).
3. **Repositórios** — Spring Data JPA com consultas de filtro (categoria, localização, estoque baixo, atrasados).
4. **Services de domínio** — ItemService (saldo/locks), EntradaService, SaidaService, EmprestimoService (devolução/atraso), BomService (versões/consumo), FornecedorService, LocalizacaoService.
5. **RabbitMQ — configuração e publicadores** — exchanges/filas e publicadores de `estoque.baixo.event`, `emprestimo.atrasado.event`, `compra.solicitada.event`; listener de baixa de consumo (Produção).
6. **Scheduler de atraso** — job `@Scheduled(cron="0 0 3 * * *")` que marca empréstimos atrasados e publica eventos.
7. **Controllers REST** — endpoints PT-BR com Bean Validation e RBAC (`@PreAuthorize`), import/export CSV de itens.
8. **Testes** — unitários (services, regras de saldo/atraso) e de integração (controllers, Rabbit).

## Riscos e Mitigações

| Risco | Mitigação |
|---|---|
| Estoque negativo em operações concorrentes | Lock pessimista/otimista sobre `item.quantidade_atual`; validação no service antes de persistir |
| Baixa duplicada no fim da produção | Consumo idempotente por `id_referencia` (encomenda); evento com idempotency key |
| Empréstimos atrasados não detectados | Scheduler diário 03:00 + flag de configuração p/ desabilitar em dev |
| Deadlock em atualizações simultâneas | Ordenar locks por `id_item`; transações curtas e em camada única |
| Perda de eventos RabbitMQ | Publicadores com confirmação (`publisher-confirm`); retry e DLQ configurados |
| Inconsistência saldo x movimentação | Entrada/saída e alteração de saldo no mesmo `@Transactional` |

## Integrações e Eventos

| Evento | Exchange/Fila (padrão FabLab) | Direção | Acionado por |
|---|---|---|---|
| `estoque.baixo.event` | `fablab.estoque` → Notification | Publica | Saldo `<= estoque_minimo` após movimentação |
| `emprestimo.atrasado.event` | `fablab.estoque` → Notification | Publica | Scheduler 03:00 / devolução além da prevista |
| `compra.solicitada.event` | `fablab.estoque` → Financeiro + Notification | Publica | Registro de `entrada_estoque` (compra) |
| Baixa de consumo (BOM) | Produção → `fablab.estoque` | Consome → baixa saldo | Fim da produção (idem `tipo_saida = Consumo`) |

Serviços envolvidos: Produção & Projetos (BOM/consumo), Financeiro (compras), Notification (alertas), RH (pessoa do empréstimo), Vendas & CRM (produtos/serviços), Eureka (descoberta), RabbitMQ (mensageria).

## Dependências Externas

- Aplicação dos eventos `compra.solicitada.event` (Financeiro) e de alertas (Notification) fora do escopo deste serviço.
- Produto/serviço e encomenda referenciados apenas por ID (`id_produto_servico`, `id_referencia`) — entidades locais não são criadas.
- Pessoa do empréstimo referenciada por `id_pessoa`; validação via RH/REST quando disponível.
# API — financeiro-service

Todas as rotas exigem `Authorization: Bearer <JWT>` do Auth &amp; Identity Service, exceto `/actuator/health`.
RBAC: acesso **exclusivo do nível `ADMIN`** (`@PreAuthorize("hasRole('ADMIN')")`); demais níveis recebem `403`.

## Lançamentos (contas a pagar/receber)

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/lancamentos` | Cria lançamento (`PENDENTE`; `ATRASADO` se já vencido, publicando `lancamento.vencido.event`) |
| GET | `/lancamentos` | Lista com filtros `status`, `dataInicio`/`dataFim` (por vencimento), `idCategoria` |
| PUT | `/lancamentos/{id}/pagamento` | Liquida o lançamento (`PAGO`, grava `dataPagamento`) |

Exemplo de payload `POST /lancamentos`:

```json
{
  "idCategoria": 1,
  "tipo": "SAIDA",
  "valor": 150.00,
  "dataVencimento": "2026-10-01",
  "idReferenciaExterna": "10",
  "observacao": "Compra de filamento"
}
```

`tipo`: `ENTRADA` (a receber) ou `SAIDA` (a pagar). `idReferenciaExterna` associa o
lançamento a uma encomenda/máquina (usado no custo de materiais e no relatório de
custo por máquina).

## Categorias

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/categorias` | Cadastra categoria (`RECEITA`/`DESPESA`) |
| GET | `/categorias` | Lista categorias (filtro opcional `tipo`) |

## Doações e Recursos

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/doacoes-recursos` | Registra doação ou recurso de projeto (`DOACAO`/`PROJETO`) |
| GET | `/doacoes-recursos` | Lista com filtros `tipo`, `dataInicio`/`dataFim` |

```json
{
  "tipo": "DOACAO",
  "origem": "Empresa X",
  "valor": 500.00,
  "dataRecebimento": "2026-09-17",
  "idProjetoAssociado": null
}
```

## Parâmetros de Custeio

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/valores-hora` | Define valor/hora por nível de acesso (0-3) com vigência |
| GET | `/valores-hora` | Lista o valor vigente (mais recente) de cada nível |
| POST | `/parametros-overhead` | Define a taxa de overhead por hora com vigência |

## Fechamento de Encomenda

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/fechamento-encomenda` | Fecha valor e horas estimadas (`ABERTA`; valores congelados) |
| GET | `/fechamento-encomenda/{idEncomenda}` | Consulta o fechamento da encomenda |

O fechamento é criado automaticamente em `ABERTA` ao consumir
`encomenda.criada.event`; também pode ser criado manualmente. As horas validadas
recebidas do RH (`horas.validadas.event`, tipo `ENCOMENDA`) são acumuladas em
`horasValidadas`.

## Solicitações de Compra

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/solicitacoes-compra` | Registra solicitação (`REGISTRADA`) e publica `compra.solicitada.event` (informativo ao Estoque) |
| PUT | `/solicitacoes-compra/{id}/concluir` | Conclui a solicitação (`CONCLUIDA`) |

## Custeio por Ordem (Job Order Costing)

| Método | Path | Descrição |
| :--- | :--- | :--- |
| GET | `/custos-encomenda/{idEncomenda}` | Custo detalhado da encomenda (último cálculo) |

O cálculo é disparado ao consumir `producao.concluida.event`:

- `custoMateriais` = Σ lançamentos `SAIDA` não cancelados com `idReferenciaExterna` = id da encomenda;
- `custoMaoObra` = Σ (horas validadas × valor/hora vigente do nível);
- `custoOverhead` = taxa de overhead vigente × total de horas;
- `custoTotal` = materiais + mão de obra + overhead;
- `margemLucro` = `valorFechado` − `custoTotal`.

Publica `custo.calculado.event` ao finalizar.

## Relatórios

| Método | Path | Descrição |
| :--- | :--- | :--- |
| GET | `/relatorios/fluxo-caixa` | Entradas, saídas e saldo no período (`dataInicio`/`dataFim`) |
| GET | `/relatorios/dre` | Receitas (entradas + doações), despesas e resultado |
| GET | `/relatorios/lucratividade` | Valor de venda, custo, margem e margem % por encomenda |
| GET | `/relatorios/inadimplencia` | Contas a receber vencidas e não liquidadas |
| GET | `/relatorios/doacoes-despesas` | Doações vs. despesas no período |
| GET | `/relatorios/custo-maquina` | Custo agregado por máquina (`idReferenciaExterna`) |

## Mensageria

- **Publica** (exchange tópica `fablab.financeiro`):
  - `lancamento.vencido.event` — lançamento vencido (Notification Service);
  - `custo.calculado.event` — custo calculado (Vendas &amp; CRM);
  - `compra.solicitada.event` — solicitação de compra (Estoque &amp; Suprimentos).
- **Consome**:
  - `encomenda.criada.event` (exchange `fablab.vendas`, fila `financeiro.fechamento.encomenda`);
  - `horas.validadas.event` (exchange `fablab.rh`, fila `financeiro.horas.encomenda`);
  - `producao.concluida.event` (exchange `fablab.producao`, fila `financeiro.custo.calculo`).

## Erros

Todas as falhas retornam `ErrorResponse` JSON (`timestamp`, `status`, `error`, `message`, `path`).
Erros de validação incluem `fieldErrors` com a causa por campo.

## Premissas de integração (MVP)

- O evento `horas.validadas.event` do RH não informa o nível do funcionário; as
  horas sem nível informado são custeadas com o valor/hora do nível **2
  (VOLUNTARIO)**, documentado para alinhamento futuro do contrato.
- O evento `compra.solicitada.event` segue o contrato do Estoque
  (`{idCompra, idFornecedor}`); no MVP publica-se com `idFornecedor = null`.
- O relatório de custo por máquina usa `idReferenciaExterna` dos lançamentos de
  saída como identificador da máquina, pela ausência de vínculo explícito no MVP.

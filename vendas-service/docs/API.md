# API — vendas-service

Todas as rotas exigem `Authorization: Bearer <JWT>` do Auth &amp; Identity Service, exceto `/actuator/health`.
RBAC: escrita exige `ADMIN`, `BOLSISTA` ou `VOLUNTARIO`; leitura permite também `ESTAGIARIO`; `RECRUTANDO` não acessa.

## Clientes

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/clientes` | Cadastra cliente (PF/PJ) com validação de CPF/CNPJ |
| GET | `/clientes` | Lista com filtros `tipo` (PF/PJ), `idTag`, `nome` |
| GET | `/clientes/{id}` | Detalhes do cliente com tags |
| POST | `/clientes/{id}/tags/{idTag}` | Associa uma tag a um cliente |

Exemplo de payload `POST /clientes`:

```json
{
  "tipoPessoa": "PF",
  "nomeRazaoSocial": "João da Silva",
  "cpfCnpj": "52998224725",
  "email": "joao@email.com",
  "telefone": "99999-0000",
  "endereco": "...",
  "tags": [1]
}
```

## Tags

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/tags` | Cadastra tag (`nome`, `cor`) |
| GET | `/tags` | Lista tags |

## Orçamentos

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/orcamentos` | Cria orçamento com itens (recalcula `valorTotal`) |
| PUT | `/orcamentos/{id}` | Atualiza orçamento (só `PENDENTE`/`AJUSTE`) |
| PUT | `/orcamentos/{id}/status` | Altera status (ex.: `APROVADO` publica `orcamento.aprovado.event`) |
| POST | `/orcamentos/{id}/encomenda` | Converte orçamento **aprovado** em encomenda (Kanban `FILA`) |

Exemplo de payload `POST /orcamentos`:

```json
{
  "idCliente": 1,
  "validade": "2026-10-01",
  "observacoes": "...",
  "itens": [
    { "descricao": "Calha 3D", "quantidade": 2, "valorUnitario": 10 }
  ]
}
```

## Encomendas / Kanban

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/encomendas` | Cria encomenda (com `idOrcamento` aprovado ou venda direta) |
| GET | `/encomendas` | Lista com filtros `status`, `idCliente`, `dataInicio`/`dataFim` |
| GET | `/encomendas/{id}` | Busca encomenda |
| PUT | `/encomendas/{id}/kanban` | Move no Kanban (concorrência otimista + auditoria) |
| GET | `/encomendas/{id}/historico` | Histórico de movimentações do Kanban |

Ordem do Kanban: `FILA → PRODUCAO → ACABAMENTO → PRONTO → ENTREGUE`.
Ao chegar em `ENTREGUE`, publica `encomenda.entregue.event`.

Payload `PUT /encomendas/{id}/kanban`:

```json
{
  "novoStatus": "PRODUCAO",
  "idUsuario": 10,
  "observacao": "Início da produção"
}
```

## Marketplace

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/marketplace` | Registra venda manual (plataforma, `codigoExterno`, `valorTaxa`) |

Publica `marketplace.venda.event` ao registrar.

## Interações (CRM)

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/interacoes` | Registra interação (`EMAIL`, `TELEFONE`, `REUNIAO`, `WHATSAPP`) |
| GET | `/interacoes/{idCliente}` | Lista interações de um cliente |

## Tarefas de Marketing

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/tarefas-marketing` | Cria tarefa (`idResponsavel`, `prioridade`, datas) |
| GET | `/tarefas-marketing` | Lista com filtros `idResponsavel`, `status` |
| PUT | `/tarefas-marketing/{id}` | Atualiza tarefa |
| PUT | `/tarefas-marketing/{id}/status` | Atualiza somente o status |

## Mensageria

- **Publica** (exchange `fablab.vendas` / tópica): `orcamento.aprovado.event`, `encomenda.criada.event`, `encomenda.entregue.event`, `marketplace.venda.event`, `kanban.status.event`.
- **Consome**: `producao.status.event` (exchange `fablab.producao`, fila `vendas.kanban.producao`).

## Erros

Todas as falhas retornam `ErrorResponse` JSON (`timestamp`, `status`, `error`, `message`, `path`).
Erros de validação incluem `fieldErrors` com a causa por campo.
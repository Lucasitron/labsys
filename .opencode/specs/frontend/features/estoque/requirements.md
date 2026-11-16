# Estoque — Frontend Requirements

## Routes
- `/(app)/estoque/itens` (list)
- `/(app)/estoque/itens/[id]` (detail)
- `/(app)/estoque/itens/novo`
- `/(app)/estoque/itens/[id]/editar`
- `/(app)/estoque/entradas` (+ `/nova`, `/[id]`)
- `/(app)/estoque/saidas` (+ `/nova`, `/[id]`)
- `/(app)/estoque/emprestimos` (+ `/novo`, `/[id]/devolucao`)
- `/(app)/estoque/fornecedores`
- `/(app)/estoque/localizacoes`
- `/(app)/estoque/bom`

## Access
- **View:** Admin, Bolsista, Voluntário, Estagiário
- **Edit:** Admin OU responsável por Estoque
- **Blocked:** Recrutando (`/estoque` → redirect `/dashboard`)

## Screens (13)
| Grupo | Telas |
|---|---|
| Listas | Itens, Entradas, Saídas, Empréstimos, Fornecedores, Localizações, BOM |
| Detail | Item (tabs: Visão geral, Movimentações, Empréstimos, Fornecedores, Histórico) |
| Forms | Novo Item, Nova Entrada, Nova Saída, Novo Empréstimo, Devolução |

## Data Requirements

> 🔴 **Dívida 2 (contract.gap §5.6):** este módulo foi especificado em **EN** (`/api/stock/**`). O backend/gateway canônico é **PT** (`/api/estoque/**`), e o contract.gap decide **PT canônico** antes do 1º endpoint. **O spec do front abaixo já adota PT** — implementar com `/api/estoque/**` e **envio por fora da URL base** (unificar mockup `docs/mokups/estoque` quando o service for construído — Dívida 2).

- `GET /api/estoque/itens?page&pageSize&search&categoria&localizacao&status&sort`
- `GET /api/estoque/itens/{id}`
- `POST /api/estoque/itens`, `PUT /api/estoque/itens/{id}`
- `PATCH /api/estoque/itens/bulk`, `PATCH /api/estoque/itens/{id}/arquivar`, `POST /api/estoque/itens/{id}/duplicar`
- `GET /api/estoque/itens/export`
- `GET /api/estoque/itens/{id}/movimentacoes?periodo=30d`
- `GET /api/estoque/movimentacoes?tipo=entrada|saida&...`
- `POST /api/estoque/movimentacoes`
- **Assumidos pelo front (Dívida 1, contract.gap §5.6):** `GET /api/estoque/movimentacoes/{id}` (detalhe entrada/saída, telas 15/16) · `POST /api/estoque/fornecedores` (tela 18) · `POST /api/estoque/localizacoes` (tela 19) · `GET /api/estoque/emprestimos?status=ativos|atrasados|historico`, `GET /api/estoque/itens/{id}/emprestimos`
- `POST /api/estoque/emprestimos`, `POST /api/estoque/emprestimos/{id}/devolver`
- `GET /api/estoque/fornecedores`, `GET /api/estoque/localizacoes`, `GET /api/estoque/bom?projetoId`

## Status Canonical Map
| API | Label | Tone |
|---|---|---|
| `available` | Disponível | `success` |
| `low` | Baixo | `warn` |
| `out` | Esgotado | `danger` |
| `loaned` | Emprestado | `brand` |
| `maintenance` | Manutenção | `muted` |

## Movement Types
| Type | Tone |
|---|---|
| Compra | `brand` |
| Doação | `success` |
| Devolução | `warn` |
| Ajuste | `muted` |

## Exit Reasons
| Reason | Tone |
|---|---|
| Uso em projeto | `brand` |
| Consumo interno | `muted` |
| Perda | `danger` |
| Descarte | `warn` |

## Loan Status
| Status | Tone |
|---|---|
| `on_time` | `success` |
| `due_today` | `warn` |
| `overdue` | `danger` |
| `returned` | `muted` |

## States
- Loading (skeleton), Success, Empty, Empty filtrado, Error, RBAC-restricted

## URL State
- Listas: `?page=&pageSize=&search=&category=&location=&status=&sort=`
- Detail: `?tab=overview|movements|loans|suppliers|history`
- Mudanças levam à refetch; navegação back preserva

## RBAC
- `canEdit === false`: esconde "+ Novo", coluna checkbox, coluna ações, bulk bar
- Detail: esconde "Editar", "Duplicar", "Arquivar", "Excluir", "+ Nova entrada/saída", "Devolver"

## Details
- Ver `/docs/mokups/estoque/requisitos.md`, `instrunctions.md`, `full.html`, `itemDatails.html`, `estoqueEstados.html`

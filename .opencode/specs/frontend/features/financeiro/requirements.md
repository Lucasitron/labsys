# Financeiro — Frontend Requirements

## Routes
- `/(app)/financeiro/lancamentos` (list + `[id]`)
- `/(app)/financeiro/contas` (a pagar/receber)
- `/(app)/financeiro/custeio` (projetos vs custeio)
- `/(app)/financeiro/doacoes`
- `/(app)/financeiro/relatorios`

## Access
- **Admin only** — rota inteira escondida p/ demais roles
- RBAC: hide sidebar item + route guard p/ Admin

## Data Requirements
- `GET /api/financeiro/lancamentos?tipo&status&periodo&page` (+ `/{id}`, POST)
- `GET /api/financeiro/contas?status` (+ `/{id}`, POST, PATCH `/{id}/pagar`)
- `GET /api/financeiro/custeio?periodo` (Projeto vs horas custeadas; só Admin)
- `GET /api/financeiro/relatorios/extrato?periodo` (PDF)

## RBAC
- Nada renderiza p/ não-Admin (hide, não disable)

## States
- Loading, Success, Empty, Empty(filtrado), Error

## URL State
- Lancamentos: `?tipo=&status=&periodo=&page=`
- Custeio: `?periodo=`

## Mockup
- `docs/mokups/financeiro/`

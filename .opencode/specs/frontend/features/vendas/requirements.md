# Vendas & CRM — Frontend Requirements

## Routes
- `/(app)/vendas/clientes` (list + `[id]`)
- `/(app)/vendas/orcamentos` (list + `[id]`)
- `/(app)/vendas/encomendas` (Kanban + `[id]`, `/nova`)
- `/(app)/vendas/marketplace` (Admin only visual — marketplace/CRM mockup pendente)

## Access
- View: Admin/Bolsista/Voluntário/Estagiário
- Edit: Admin; Bolsista/Voluntário **se atribuído à encomenda**
- RBAC hide, nunca disable

## Data Requirements
- `GET /api/vendas/clientes?search&status&page`
- `GET /api/vendas/orcamentos?status&cliente&page` (+ `/{id}`, POST, PATCH `/{id}/aprovar|recusar`)
- `GET /api/vendas/encomendas?status&atribuido` (+ `/{id}`, POST, PATCH `/{id}/mover`)
- `GET /api/vendas/marketplace` (RBAC: Admin)

## RBAC
- Kanban: coluna de ação só se atribuído; marketplace: hide p/ não-Admin
- Marketplace edit → consume estoque (integração evento)

## States
- Loading, Success, Empty, Empty(filtrado), Error, Kanban

## URL State
- Clientes: `?search=&status=&page=`
- Orcamentos: `?status=&cliente=&page=`
- Encomendas Kanban: `?coluna=`

## Mockup
- `docs/mokups/vendas/`

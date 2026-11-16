# Notificações — Frontend Requirements

## Routes
- `/(app)/notificacoes` (lista central)

## Access
- Todos autenticados (próprias); Admin → central + config canais
- Admin: revisão de notificações (`, revisar`)

## Data Requirements
- `GET /api/notifications?page=&pageSize=&type=&read=`
- `GET /api/notifications/unread/count` (polling 60s; pausa em `document.hidden`)
- `PATCH /api/notifications/{id}/read`, `POST /api/notifications/read-all`
- Admin: `GET /api/notifications/preferences` (+ PUT), `GET /api/notifications/history`

## States
- Loading, Success, Empty, Empty(filtrado), Error
- Polling heartbeat: mostra última atualização silenciosa

## Notifications Button (Topbar)
- Badge count unread; badge escondido quando 0 (hide, não disabled)
- Dropdown: últimas 5 + "Ver todas" → `/notificacoes`

## URL State
- `?page=&type=&status=unread|all`

## Mockup
- `docs/mokups/dashboard/` (polling/notificações no topbar)

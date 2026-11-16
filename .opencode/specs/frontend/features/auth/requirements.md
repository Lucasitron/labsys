# Auth — Frontend Requirements

## Route
- `/auth/login`

## Access
- Público (sem autenticação)

## Screens
1. **Login** — `/auth/login`

## Data Requirements
- `POST /api/auth/login` → `{ token, expiresIn, user }`
- `GET /api/auth/me` (usado no boot do App Shell)

## States
- Default, Loading, Error, Success

## RBAC
- Nenhum (público)

## Design System
- Hero brand glow + card centralizado `max-w-sm`
- Token de erro abaixo do form (não só toast)
- Ver mockup: `docs/mokups/login/full.html`

## Fora de Escopo (MVP)
- MFA, social login, "lembrar de mim", recuperação de senha (link placeholder apenas)

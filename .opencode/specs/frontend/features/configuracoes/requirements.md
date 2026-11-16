# Configurações — Frontend Requirements

> **Área:** Configurações · **Análogo de mockup:** `docs/mokups/configuracoes/` (full.html · 4 telas: perfil, usuarios, permissoes, sistema)
> **Contract status:** **todos os endpoints assumidos 🟡** (contrato pendente — espelhar `docs/contract.gap.md` §2/§6; a feature consome Roupa de `auth` + `rh`, ver §4.3).

## Routes
- `/(app)/configuracoes/perfil` — **qualquer autenticado** (badge "Acesso liberado para todos os níveis")
- `/(app)/configuracoes/usuarios` — **Admin-only** (pill "Restrito ao Admin")
- `/(app)/configuracoes/permissoes` — **Admin-only** (matriz RBAC = fonte única p/ `lib/utils/permissions.ts`)
- `/(app)/configuracoes/sistema` — **Admin-only** (identidade, cadência 5S, tokens)

## Access
- **Perfil:** todos os níveis autenticados (0–4).
- **Usuários / Permissões / Sistema:** **Admin only (0)** — `isAdmin(user)` → **HIDE** (nunca disable); itens removidos da sidebar e guarda de rota redireciona não-Admin para `/configuracoes/perfil`.
- **Guarda de rota:** em `src/routes/(app)/configuracoes/+layout.ts`: rota != `/perfil` && !isAdmin → `redirect(303, '/configuracoes/perfil')`.

## Data Requirements
> 🟡 **Todos os endpoints abaixo são ASSUMIDOS** (contrato pendente — Designer marcou 🟡; não implementar chamadas reais sem confirmação do backend. Usar mocks locais até o contrato chegar.)

| Método | Rota | Uso |
|---|---|---|
| `GET` | `/api/configuracoes/perfil` | Dados do usuário autenticado (fallback: `GET /api/auth/me`) |
| `PUT` | `/api/configuracoes/perfil` | Atualiza dados pessoais / preferências |
| `PUT` | `/api/auth/senha` | Altera senha (atual + nova + confirmação; `≥ 8 chars`) |
| `GET` | `/api/usuarios` | Lista usuários (filtros `page, search, nivel[], situacao[]`) |
| `POST` | `/api/usuarios` | Cria usuário / envia convite |
| `PUT` | `/api/usuarios/{id}` | Atualiza usuário |
| `PATCH` | `/api/usuarios/{id}/status` | Ativa/pende/desativa usuário |
| `GET` | `/api/permissoes` | Matriz RBAC completa (módulo × nível) |
| `PUT` | `/api/permissoes/{modulo}/{nivel}` | Altera permissão de uma célula |
| `GET` | `/api/configuracoes/sistema` | Parâmetros globais (identidade · cadência 5S · tokens) |
| `PUT` | `/api/configuracoes/sistema` | Persiste identidade + cadências |
| `POST/DELETE` | `/api/configuracoes/tokens` | Gera / revoga token de integração |

**Conflitos de contrato reportados (espelhar `docs/contract.gap.md`):**
- `PUT /api/auth/senha` pertence ao domínio **auth** (feature `auth` já existe; endpoint novo → adicionar em camada separada `src/lib/api/auth/senha.ts`, não alterar `auth/login.ts`). Contrato pendente.
- `GET/PUT /api/permissoes` e `GET/PUT /api/configuracoes/sistema` alimentam **Produção** (parâmetros-5S — ver requirements producao §5). Backend deve validar.
- `GET /api/usuarios` espelha domínio **Pessoas & RH** (`/api/rh`); Configurações opera **contas/nível/situação**, RH opera **cadastro físico de pessoas** — contratos distintos, mas dados de identidade podem vir do `rh-service`. Reportado p/ evitar endpoint duplicado.

## RBAC
| Recurso | Admin (0) | Bolsista (1) | Voluntário (2) | Estagiário (3) | Recrutando (4) |
|---|---|---|---|---|---|
| Meu perfil (`/configuracoes/perfil`) | ✅ ver/editar | ✅ ver/editar | ✅ ver/editar | ✅ ver/editar | ✅ ver/editar |
| Usuários (`/configuracoes/usuarios`) | ✅ editar | — (hide) | — (hide) | — (hide) | — (hide) |
| Permissões (`/configuracoes/permissoes`) | ✅ editar | — (hide) | — (hide) | — (hide) | — (hide) |
| Sistema (`/configuracoes/sistema`) | ✅ editar | — (hide) | — (hide) | — (hide) | — (hide) |

- **Matriz RBAC NÃO é calculada no client**: vem de `GET /api/permissoes` e é a fonte única consumida por `lib/utils/permissions.ts` (guardas de rota + sidebar de todos os módulos).
- **HIDE, nunca disable**: usuários/permissoes/sistema são omitidos da sidebar e o layout redireciona não-Admin. Recrutando perfil: ok.
- **Reset padrão** da matriz exige confirmação.

## States
- `Loading` (skeleton de cards/tabela) · `Empty` (sem dados + CTA) · `Empty filtrado` (chips + "Limpar filtros") · `Error` (banner + retry) · `RBAC-restricted` (não-Admin: tela omitida/hide).
- Formulários: dirty-guard ao sair; erro de validação inline `text-xs text-danger mt-1`; sucesso → toast.

## URL State
- **Usuários:** `?page&search&nivel=&situacao=` (URL como fonte de verdade; deep-link).
- **Perfil:** sem filtros (cards estáticos).
- **Sistema / Permissões:** abas/segmentos em `?tab=` quando aplicável.

## Mockup
- `docs/mokups/configuracoes/` → `full.html` (4 telas), `requisitos.md`, `instrunctions.md`, `verify-config.js`
- Referência visual por tela: seções `#cfg-perfil` · `#cfg-usuarios` · `#cfg-permissoes` · `#cfg-sistema`.

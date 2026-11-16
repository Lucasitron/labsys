# Plan — Configurações (frontend, 4 telas)

> **Fonte da verdade visual:** `docs/mokups/configuracoes/` (4 telas: perfil · usuarios · permissoes · sistema).
> **Spec:** `requirements.md` nesta pasta. **Status:** `PLANEJADA` — tasks em `tasks.md`.
> **Dependência-base:** `auth` (guarda de rota, `authStore`, `isAdmin`) + **(app)/+layout (App Shell: Sidebar/Topbar)**.
> **Contrato:** todos endpoints 🟡 assumidos/PA (ver `requirements.md` §4).

---

## 1. Objetivo

Módulo **Configurações**: perfil próprio, gestão de usuários e níveis, matriz RBAC e parâmetros do sistema.
4 rotas reais sob `/(app)/configuracoes` + 4 rotas no sidebar (submenu "Configurações: Meu perfil · Usuários · Permissões · Sistema").

O mockup de Configurações define **4 telas**, mas o menu de produção v2 já prevê submenu **Produção** com
**12 telas** (produção v2). Ambos expandem o mesmo `menu.ts`/`Sidebar` → **executar em sequência, nunca em paralelo** (ver §6).

---

## 2. Camadas e Arquivos

### 2.1 Fundação compartilhada (shared — toca arquivos de outras features)

| Arquivo | Ação | Toca | Conflito? |
|---|---|---|---|
| `src/lib/types/configuracoes.ts` | create | — | — |
| `src/lib/api/configuracoes/perfil.ts` | create | `api/` | — |
| `src/lib/api/configuracoes/usuarios.ts` | create | `api/` | — |
| `src/lib/api/configuracoes/permissoes.ts` | create | `api/` | — |
| `src/lib/api/configuracoes/sistema.ts` | create | `api/` | — |
| `src/lib/api/auth/senha.ts` | create | **`api/auth/*`** | ⚠️ toca namespace de `auth` feature (novo arquivo, não conflita com login.ts) |
| `src/lib/config/menu.ts` | **modify** | **compartilhado** | ⚠️ **conflito com producao v2** (ambos adicionam submenus) |
| `src/lib/utils/permissions.ts` | **modify** | **compartilhado** | ⚠️ **conflito com producao v2** (ambos tocam guards/`isAdmin`) |

### 2.2 Componentes de módulo (novos)

| Arquivo | Ação |
|---|---|
| `src/lib/components/configuracoes/PermCell.svelte` | create — célula da matriz RBAC (select Editar/Ver/Nenhum) |
| `src/lib/components/configuracoes/ModalUsuario.svelte` | create — modal usuário criar/editar (Admin) |
| `src/lib/components/configuracoes/ModalConfirm.svelte` | create — confirmação destrutiva (desativar usuário, revogar token) |

### 2.3 Rotas (4 telas + guarda)

| Rota | Tela | Guarda |
|---|---|---|
| `/(app)/configuracoes/perfil` | Meu perfil | qualquer autenticado |
| `/(app)/configuracoes/usuarios` | Usuários | **Admin** — `isAdmin(user)` senão redirect `/perfil` |
| `/(app)/configuracoes/permissoes` | Permissões | **Admin** — idem |
| `/(app)/configuracoes/sistema` | Sistema | **Admin** — idem |

`+layout.ts` do grupo configuracoes: guarda só para as 3 rotas admin (`!= /perfil`); sidebar **oculta** Usuários/Permissões/Sistema para não-Admin (RBAC hide, nunca disable).

---

## 3. Ordem de Implementação

| # | Passo | Tasks |
|---|---|---|
| 1 | Fundação (types + api layer) | `cfg-001`…`cfg-006` |
| 2 | RBAC (menu.ts + permissions.ts) | `cfg-007`·`cfg-008` |
| 3 | Componentes de módulo | `cfg-009`…`cfg-011` |
| 4 | Guarda + Meu perfil | `cfg-012`…`cfg-015` |
| 5 | Usuários | `cfg-016`…`cfg-018` |
| 6 | Permissões | `cfg-019`·`cfg-020` |
| 7 | Sistema | `cfg-021`·`cfg-022` |
| 8 | Estados + RBAC final | `cfg-023`…`cfg-025` |
| 9 | Testes (unit + e2e) | `cfg-026`…`cfg-028` |

---

## 4. RBAC (hide, não disable)

- **Meu perfil:** todos os autenticados (nível 0–4).
- **Usuários/Permissões/Sistema:** **Admin apenas**. Itens **omitidos** da sidebar (nunca desabilitados);
  guarda de rota: `!isAdmin(user) → redirect(303, '/configuracoes/perfil')`.
- Matriz RBAC é fonte única de verdade consumida por `lib/utils/permissions.ts` (guarda de todos os módulos).

---

## 5. Estados Padrão (todas as 4 telas)

Loading (skeleton) · Success · Empty (filtrável com chips) · Empty filtrado · Error (banner + retry) · RBAC-restricted (hide).

---

## 6. Riscos & Conflitos de Escopo

| Risco | Impacto | Mitigação |
|---|---|---|
| **menu.ts e permissions.ts compartilhados** | config × producao v2 tocam os mesmos arquivos | **Nunca executar em paralelo**; ordem sugerida: **configuracoes primeiro**, producao v2 depois (ou vice-versa — nunca simultâneos); relatório ao Orchestrator |
| Endpoints 🟡 (assumidos) | Backend não confirma | Mocks locais; reportar em `contract.gap.md`; ver `requirements.md` §4 |
| `api/auth/senha.ts` toca namespace auth | Sobreposição com feature auth | Arquivo **novo** (não altera login.ts); endpoint `PUT /api/auth/senha` reportado como contrato assumido |
| Guarda `(app)/+layout` já autentica | Dupla guarda | Reutilizar `authStore` existente; não duplicar lógica de `me` |

---

## 7. Design System (reuso)

Tokens/classes de `design-system.md` (surface/elevated/border/brand/success/danger…); componentes UI já existentes
(`PageHeader`, `SearchInput`, `Select`, `Avatar`, `StatusBadge`, `EmptyState`, `TableSkeleton`, `ConfirmDialog`,
`Modal`, `Chip`, `Tabs`, `ToastHost`). **Sem novas libs externas**; pt-BR; data-testid `cfg-*`.

---

## 8. Critérios de Entrega (plan-level)

- [ ] 4 rotas funcionando com guarda RBAC correta (perfil público; demais Admin-only)
- [ ] Sidebar com submenu Configurações (4 itens), admin items **hide** p/ não-Admin
- [ ] Matriz de permissões renderizada a partir de `/api/permissoes` (🟡) e fonte única do utils
- [ ] Estados Loading/Empty/Empty-filtrado/Error nas 4 telas
- [ ] Tests unit (permissions) + e2e happy path

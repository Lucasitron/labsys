# DESPACHO — Builder (Frontend) · CONFIGURACOES — Bloco 2 (cfg-011..cfg-019) · 21/09/2026

> Preparado pelo Orchestrator. Escopo: **guarda de layout + 4 telas** (perfil, usuários, permissões, sistema).

## Contexto (ler antes)
- Tasks: `.opencode/specs/frontend/features/configuracoes/tasks.md` (`cfg-011..cfg-019`)
- Requirements §Routes/§Access/§States/§URL State; Mockup `docs/mokups/configuracoes/full.html` (`#cfg-perfil`, `#cfg-usuarios`, `#cfg-permissoes`, `#cfg-sistema`)
- Bloco 1 concluído (commit anterior): `types/configuracoes.ts`; `api/configuracoes/{perfil,senha,usuarios,permissoes,sistema}.ts`; `permissions.ts` (`isAdmin`, `canViewConfiguracoes`, `hasPermissao`); `menu.ts` submenu Configurações; `components/configuracoes/{PermCell,ModalUsuario,ModalConfirm}.svelte`
- Base: `$lib/api/client.ts` (`apiFetch`, `ApiError`), `stores/auth.ts` (authStore/user), UI kit `$lib/components/ui/*` (PageHeader, SearchInput, Select, Avatar, StatusBadge, EmptyState, TableSkeleton, ConfirmDialog/ModalConfirm, Modal, Chip, Tabs, ToastHost) — **conferir props reais**; padrão de páginas: `(app)/producao/**` (load + estados) e `(app)/estoque/**`
- Layout producao como referência de guarda client-side: `(app)/producao/+layout.ts` (sessão via authStore; redirect 303)
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme com `git branch --show-current`; se não for, PARE e reporte)
- Baseline: 150 testes verdes; svelte-check 12/15/7

## Tasks (ordem; NÃO commitar)

| Task | Arquivo | Conteúdo · DoD |
|---|---|---|
| cfg-011 | `src/routes/(app)/configuracoes/+layout.ts` (create) | guarda: rota != `/configuracoes/perfil` e `!isAdmin(user)` → `redirect(303, '/configuracoes/perfil')`; perfil exige autenticação (herdada do `(app)`). Seguir padrão client-side do producao layout (authStore). Opcional `+layout.svelte` só se necessário (scaffold) |
| cfg-012 | `perfil/+page.ts` (create) | `load` com `getPerfil(fetch)`; estados Loading/Error integrais; sem fetch no componente |
| cfg-013 | `perfil/+page.svelte` (create) | "Meu perfil": dados pessoais + avatar iniciais + "Alterar senha" (`alterarSenha`) + preferências; badge "Acesso liberado para todos os níveis"; `data-testid="cfg-profile-*"`; validação client + toast de sucesso |
| cfg-014 | `usuarios/+page.ts` (create) | `load` com `listarUsuarios({page,search,nivel,situacao})` lendo da URL (deep-link); Loading/Error |
| cfg-015 | `usuarios/+page.svelte` (create) | tabela usuários (nivel/situacao chips) + search + filtros + Pagination + Empty + Empty-filtrado (chips) + Error(retry); `ModalUsuario` criar/editar + `ModalConfirm` desativar; `data-testid="cfg-user-*"`, `cfg-users-skeleton/empty/error` |
| cfg-016 | `permissoes/+page.ts` (create) | `load` com `obterPermissoes()`; Loading/Error |
| cfg-017 | `permissoes/+page.svelte` (create) | matriz RBAC (módulos × níveis) com `PermCell`; "restaurar padrão" (com `ModalConfirm`) + salvar; dirty destacado até salvar; `data-testid="cfg-perm-matrix"`, `cfg-perm-save`, `cfg-perm-reset` |
| cfg-018 | `sistema/+page.ts` (create) | `load` com `obterSistema()` + `listarTokens()`; Loading/Error |
| cfg-019 | `sistema/+page.svelte` (create) | identidade (nome fablab, logo), cadência 5S (checklist/auditoria), tokens (criar/revogar com `ModalConfirm`); aviso de que cadência 5S afeta Produção; `data-testid="cfg-system-*"`, `cfg-token-*` |

## Regras
1. **Ordem numérica; NÃO commitar.** Só os arquivos acima; NÃO tocar menu/permissions/API/types/components/config de outras features; NÃO tocar `docs/`.
2. RBAC **hide nunca disable**: usuários/permissoes/sistema Admin-only (guarda no layout + itens já ocultos na sidebar); perfil todos.
3. Estados Loading/Empty/Empty-filtrado/Error em todas as telas; pt-BR; datas Intl; `aria-label`; data-testid `cfg-*`; sem `any`/`console.log`; tokens design-system; sem libs novas.
4. URL como fonte de verdade nos filtros de Usuários (`?page&search&nivel&situacao`).
5. Endpoints 🟡: chamadas reais via API do bloco 1; erro amigável com retry (sem gateway não há mock — reportar). Formulários: dirty-guard/erro inline/toast.
6. Ao final: `npm run test` (150 verdes) + `npm run check` baseline 12/15/7 (zero novos).

## Relatório
- Por task ✅/❌ + evidência (arquivos, data-testids, estados, como verificou)
- `npm run test` + `npm run check`
- Pontos de atenção/desvios
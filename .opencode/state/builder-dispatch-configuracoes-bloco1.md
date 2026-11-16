# DESPACHO — Builder (Frontend) · CONFIGURACOES — Bloco 1 (cfg-001..cfg-010) · 21/09/2026

> Preparado pelo Orchestrator. Escopo: **fundação (types+API) + RBAC compartilhado + componentes de módulo**.
> ⚠️ `menu.ts`/`permissions.ts` são do ConflictMap — produção já CONCLUIU; configurações é a dona atual (sequencial, sem paralelo).

## Contexto (ler antes)
- Tasks: `.opencode/specs/frontend/features/configuracoes/tasks.md` (`cfg-001..cfg-010`; **tasks.md é autoritativo** — plan.md tem numeração antiga cfg-001..028, ignore-a)
- Requirements: `.opencode/specs/frontend/features/configuracoes/requirements.md` (§Data Requirements: **todos endpoints 🟡**; §RBAC; §States; §URL State)
- Plano: `plan.md` §2/§4/§7 (design system reuso) · Mockup: `docs/mokups/configuracoes/` (full.html 4 telas: `#cfg-perfil`, `#cfg-usuarios`, `#cfg-permissoes`, `#cfg-sistema`)
- Base existente: `$lib/api/client.ts` (`apiFetch`/`ApiError`/`NetworkError` + interceptor 401 global; `bearer()` helper existe no `api/producao/client.ts` — **não importar de producao**; criar helper local ou usar o padrão do `client.ts`); `types/auth.ts` (`Module`, `Role`, `User`); `utils/permissions.ts` (canView/canEdit/canSee5S/etc. — preservar); `config/menu.ts` (`MenuItem` agora com `children?: {label,path,canSee?}[]` e `canSee?` de producao — preservar)
- UI kit: `$lib/components/ui/` (`PageHeader`, `SearchInput`, `Select`, `Avatar`, `StatusBadge`, `EmptyState`, `TableSkeleton`, `ConfirmDialog`, `Modal`, `Chip`, `Tabs`, `ToastHost`) — **conferir props reais** antes de usar; **sem libs novas**
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme com `git branch --show-current`; se não for dev/frontend, PARE e reporte)
- Baseline: 150 testes verdes; svelte-check 12/15/7 (estoque/ui)

## Tasks (ordem numérica; NÃO commitar)

### A1 — Fundação
| Task | Arquivo | Conteúdo · DoD |
|---|---|---|
| cfg-001 | `src/lib/types/configuracoes.ts` (create) | `Nivel`(0-4) · `SituacaoUsuario`('Ativo'\|'Pendente'\|'Desativado') · `Usuario`(id,nome,email,telefone,nivel,situacao,foto?,criadoEm) · `Perfil`(nome,email,telefone,funcao,avatar) · `PermissaoNivel`('Ver'\|'Editar'\|'Nenhum') · `PermissaoMatriz = Record<Module, Record<Nivel, PermissaoNivel>>` · `ParametroSistema`(identidade,cadenciaChecklist5S,cadenciaAuditoria5S?,tokens) · `TokenIntegracao`(id,nome,prefixo,criadoEm,ultimoUso,revogado). Compatível com `Module` de `types/auth.ts`; sem `any` |
| cfg-002 | `src/lib/api/configuracoes/perfil.ts` (create 🟡) | `getPerfil()` → `GET /configuracoes/perfil` (fallback `GET /auth/me` do auth) · `updatePerfil(payload)` → `PUT /configuracoes/perfil`; usa `apiFetch`; erros `ApiError` |
| cfg-003 | `src/lib/api/configuracoes/senha.ts` (create 🟡) | `alterarSenha(atual,nova,confirmacao)` → `PUT /auth/senha`; valida `nova===confirmacao` e `nova.length>=8` client-side (lançar `Error` de validação antes do fetch); **NÃO alterar `api/auth/*`** (arquivo novo em `api/configuracoes/`) |
| cfg-004 | `src/lib/api/configuracoes/usuarios.ts` (create 🟡) | `listarUsuarios({page,search,nivel,situacao})` → `GET /usuarios` (Paginado) · `criarUsuario(p)` → `POST /usuarios` · `atualizarUsuario(id,p)` → `PUT /usuarios/{id}` · `alterarStatus(id,situacao)` → `PATCH /usuarios/{id}/status` |
| cfg-005 | `src/lib/api/configuracoes/permissoes.ts` (create 🟡) | `obterPermissoes()` → `GET /permissoes` (`PermissaoMatriz`) · `atualizarPermissao(modulo,nivel,valor)` → `PUT /permissoes/{modulo}/{nivel}`; nada persistido localmente |
| cfg-006 | `src/lib/api/configuracoes/sistema.ts` (create 🟡) | `obterSistema()`/`atualizarSistema(p)` → `GET/PUT /configuracoes/sistema` · `listarTokens()`/`criarToken(p)` → `GET/POST /configuracoes/tokens` · `revogarToken(id)` → `DELETE /configuracoes/tokens/{id}`; comentário de reconciliação com `producao/parametros-5s` (cadência 5S compartilhada) |

### A2 — RBAC (⚠️ arquivos compartilhados — mudanças mínimas/aditivas)
| Task | Arquivo | Conteúdo · DoD |
|---|---|---|
| cfg-007 | `src/lib/config/menu.ts` (modify) | Submenu **Configurações** com `children`: Meu perfil `/configuracoes/perfil` · Usuários `/configuracoes/usuarios` · Permissões `/configuracoes/permissoes` · Sistema `/configuracoes/sistema`. Itens Usuários/Permissões/Sistema com `canSee: isAdmin` (**HIDE** p/ não-Admin, nunca disable); Meu perfil sempre visível a autenticados. **Preservar** a estrutura/`canSee` de Produção e Estoque |
| cfg-008 | `src/lib/utils/permissions.ts` (modify) | `isAdmin(user)` → `user?.role === 0` (alinhado ao código existente; se `roles` trouxer admin explícito, respeitar) · `canViewConfiguracoes(user)` (Admin true; demais apenas perfil — helper p/ guarda) · `hasPermissao(user, modulo, acao, matriz?)` → usa `PermissaoMatriz` quando fornecida, senão fallback `canView`/`canEdit`. **NÃO alterar** canView/canEdit/canSee5S/canSeeAdvertencias/canAuditar5S/canSeeMachines/VIEW_RULES/EDIT_RULES |

### A3 — Componentes
| Task | Arquivo | Conteúdo · DoD |
|---|---|---|
| cfg-009 | `src/lib/components/configuracoes/PermCell.svelte` (create) | célula da matriz: select Editar/Ver/Nenhum com dirty-state e `data-testid="cfg-perm-cell-{modulo}-{nivel}-{valor}"`; prop `matriz`/`modulo`/`nivel`/`valor` + callback; RBAC hide se não Admin (prop `isAdmin`) |
| cfg-010 | `src/lib/components/configuracoes/ModalUsuario.svelte` (create) | criar/editar usuário + convite (nome, email, telefone, nível, situação); `data-testid="cfg-user-modal"`, `cfg-user-save`, `cfg-user-cancel`; devolve payload via callback; validação inline pt-BR |
| (extra) | `src/lib/components/configuracoes/ModalConfirm.svelte` (create) | confirmação destrutiva genérica (desativar usuário / revogar token): props `open,titulo,mensagem,confirmLabel,tone,onConfirm,onClose`; `data-testid="cfg-confirm"`. (plan.md §2.2 exige; tasks não listam — criar junto) |

## Regras
1. **Ordem numérica; NÃO commitar** (Orchestrator commita). Só os arquivos acima; **NÃO** tocar docs/specs/rotas/outras features; `menu.ts`/`permissions.ts` mudanças mínimas/aditivas.
2. Sem `any`/`console.log`; pt-BR; tokens design-system; a11y (`aria-label`); data-testid `cfg-*`.
3. Endpoints 🟡: implementar as chamadas reais via `apiFetch` (não mockar aqui); comentários `TODO contrato 🟡`.
4. Conferir assinatura real de `apiFetch`/`bearer`/`ApiError` em `api/client.ts` antes de usar; padrão de paginação `Paginado<T>` (ver `types/producao.ts` — **não importar de producao**; se necessário definir local ou reusar um tipo genérico existente).
5. Ao final: `npm run test` (150 verdes) + `npm run check` baseline 12/15/7 (zero novos).

## Relatório
- Por task ✅/❌ + evidência (arquivo:linha + assinaturas)
- `npm run test` + `npm run check`
- Pontos de atenção/desvios (ex.: `canSee` em menu já existente de producao)
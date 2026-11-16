# Tasks — Configurações (frontend, 4 telas)

> Conteúdo derivado de `.opencode/specs/frontend/features/configuracoes/{requirements,plan}.md`. Cada task = **1 arquivo real**.
> Execução **sequencial** — `menu.ts` e `permissions.ts` são compartilhados com **producao v2** (NUNCA em paralelo).
> Legenda: 🟡 contrato pendente (endpoint assumido).

---

## CRITÉRIO GLOBAL (aplicado a todas as tasks)
- pt-BR; `aria-label`; `data-testid` prefixo `cfg-*` (mockup). Estados: Loading(skeleton) · Empty(CTA) · Empty filtrado(chips) ·
  Error(banner+retry) · RBAC-restricted(HIDE). Tokens de `design-system.md` (surface/elevated/border). Sem libs externas.
- Guarda Admin: página não-Admin → `redirect(303, '/configuracoes/perfil')` (hide, nunca disable).

---

## A1 — Fundação (types + API)

### cfg-001 · types
- **Arquivo:** `src/lib/types/configuracoes.ts` — create
- **Conteúdo:** `Nivel`(0-4), `SituacaoUsuario`(Ativo/Pendente/Desativado), `Usuario` (id, nome, email, telefone, nivel, situacao, foto?, criadoEm), `Perfil` (nome, email, telefone, funcao, avatar), `PermissaoMatriz` = `Record<Module, Record<Nivel, PermissaoNivel>>` com `PermissaoNivel` = 'Ver'|'Editar'|'Nenhum', `ParametroSistema` (identidade, cadenciaChecklist5S, cadenciaAuditoria5S?, tokens), `TokenIntegracao` (id, nome, prefixo, criadoEm, ultimoUso, revogado).
- **Saída:** types que alimentam rotas e testes.
- **DoD:** tipos refletem mockup telas 3-4; `PermissaoMatriz` compatível com `Module` de `types/auth.ts`; sem `any`.
- **Deps:** — · **Prioridade:** Alta

### cfg-002 · API perfil
- **Arquivo:** `src/lib/api/configuracoes/perfil.ts` — create 🟡
- **Conteúdo:** `getPerfil() → GET /api/configuracoes/perfil` · `updatePerfil(payload) → PUT /api/configuracoes/perfil`.
- **Saída:** funções reutilizadas em `/configuracoes/perfil`.
- **DoD:** fallback `me()` de auth (🟡); usa `apiFetch` de `client.ts`; erro lançado como `ApiError`.
- **Deps:** cfg-001 · **Prioridade:** Alta

### cfg-003 · API senha
- **Arquivo:** `src/lib/api/configuracoes/senha.ts` — create 🟡
- **Conteúdo:** `alterarSenha(atual, nova, confirmacao) → PUT /api/auth/senha`. **Não altera** arquivos de `src/lib/api/auth/*`.
- **Saída:** função do formulário "Alterar senha" (tela Perfil).
- **DoD:** valida `nova === confirmacao` e `nova.length >= 8` client-side; endpoint 🟡 reportado.
- **Deps:** cfg-002 · **Prioridade:** Alta

### cfg-004 · API usuários
- **Arquivo:** `src/lib/api/configuracoes/usuarios.ts` — create 🟡
- **Conteúdo:** `listarUsuarios({page, search, nivel, situacao}) → GET /api/usuarios` · `criarUsuario(p) → POST /api/usuarios` (convite) · `atualizarUsuario(id,p) → PUT /api/usuarios/{id}` · `alterarStatus(id,situacao) → PATCH /api/usuarios/{id}/status`.
- **Saída:** camada da tela Usuários.
- **DoD:** ponto de contrato com RH (contas vs cadastro físico) reportado; paginação na resposta.
- **Deps:** cfg-001 · **Prioridade:** Alta

### cfg-005 · API permissões
- **Arquivo:** `src/lib/api/configuracoes/permissoes.ts` — create 🟡
- **Conteúdo:** `obterPermissoes() → GET /api/permissoes` (matriz completa) · `atualizarPermissao(modulo, nivel, valor) → PUT /api/permissoes/{modulo}/{nivel}`.
- **Saída:** matriz da tela Permissões; **fonte única** consumida por `utils/permissions.ts`.
- **DoD:** não persiste nada localmente; retorno tipado `PermissaoMatriz`.
- **Deps:** cfg-001 · **Prioridade:** Alta

### cfg-006 · API sistema
- **Arquivo:** `src/lib/api/configuracoes/sistema.ts` — create 🟡
- **Conteúdo:** `obterSistema() → GET /api/configuracoes/sistema` · `atualizarSistema(p) → PUT /api/configuracoes/sistema` · tokens `GET/POST /api/configuracoes/tokens` · `revogarToken(id) → DELETE /api/configuracoes/tokens/{id}`.
- **Saída:** camada da tela Sistema.
- **DoD:** parâmetros 5S (cadência checklist) **compartilhados** com `producao/parametros-5s` — reconciliação registrada.
- **Deps:** cfg-001 · **Prioridade:** Alta

---

## A2 — RBAC compartilhado

### cfg-007 · menu.ts (Configurações)
- **Arquivo:** `src/lib/config/menu.ts` — modify
- **Conteúdo:** submenu **Configurações** `{label:'Configurações', path:'/configuracoes', children:[Meu perfil /perfil, Usuários /usuarios, Permissões /permissoes, Sistema /sistema]}`. Itens Usuários/Permissões/Sistema **omitidos (HIDE)** p/ não-Admin (nunca disable).
- **DoD:** item pai sempre "hide p/ Admin-only"; `children` seguem `MenuItem` existente.
- **Deps:** — · **Prioridade:** Alta · ⚠️ **conflito com producao v2** (mesmo arquivo)

### cfg-008 · permissions.ts
- **Arquivo:** `src/lib/utils/permissions.ts` — modify
- **Conteúdo:** `isAdmin(user)` → `user.roles?.[0] === 0 || role === 0`; `canViewConfiguracoes(user)`; guarda `configuracoes` segue matriz RBAC; `hasPermissao(user, module, acao)` baseado em `PermissaoMatriz` quando carregada.
- **DoD:** cobertura de teste unit p/ Admin vs demais níveis; **não quebra** `canView/canEdit` existentes.
- **Deps:** cfg-007 · **Prioridade:** Alta · ⚠️ **conflito com producao v2** (mesmo arquivo)

---

## A3 — Componentes de módulo

### cfg-009 · PermCell
- **Arquivo:** `src/lib/components/configuracoes/PermCell.svelte` — create
- **Conteúdo:** célula da matriz RBAC (select Editar/Ver/Nenhum com dirty-state e `data-testid="cfg-perm-cell-{modulo}-{nivel}-{valor}"`); pulso visual de mudança não salva.
- **DoD:** actualiza `PermissaoMatriz` local e marca célula "suja"; RBAC hide por `isAdmin`.
- **Deps:** cfg-005, cfg-008 · **Prioridade:** Média

### cfg-010 · ModalUsuario
- **Arquivo:** `src/lib/components/configuracoes/ModalUsuario.svelte` — create
- **Conteúdo:** criar/editar usuário + convite (nome, email, telefone, nível, situação) com estados valid/modal zebra; `data-testid="cfg-user-modal"`, `cfg-user-save`, `cfg-user-cancel`.
- **DoD:** fluxo criar/editar reutilizado; `@save` devolve payload; RBAC Admin-only.
- **Deps:** cfg-004, cfg-008 · **Prioridade:** Média

---

## A4 — Rotas + telas

### cfg-011 · layout + guarda
- **Arquivo:** `src/routes/(app)/configuracoes/+layout.ts` — create
- **Conteúdo:** guarda RBAC por rota-filha (perfil open; usuarios/permissoes/sistema → Admin senão `redirect(303,'/configuracoes/perfil')`).
- **DoD:** não-Admin acessando `/configuracoes/usuarios` é redirecionado (não recebe 403 em tela).
- **Deps:** cfg-008 · **Prioridade:** Alta

### cfg-012 · perfil page.ts
- **Arquivo:** `src/routes/(app)/configuracoes/perfil/+page.ts` — create
- **Conteúdo:** carrega `getPerfil()` (fallback `me()`); estados Loading/Error integral.
- **DoD:** página definida com `load`, sem fetch em componente.
- **Deps:** cfg-002, cfg-011 · **Prioridade:** Média

### cfg-013 · perfil page.svelte
- **Arquivo:** `src/routes/(app)/configuracoes/perfil/+page.svelte` — create
- **Conteúdo:** "Meu perfil": dados pessoais + avatar iniciáis + "Alterar senha" (PUT senha) + preferências; `data-testid="cfg-profile-*"`.
- **DoD:** qualquer autenticado acessa; badge "Acesso liberado para todos os níveis"; alterar senha valida no client e mostra toast.
- **Deps:** cfg-012, cfg-003 · **Prioridade:** Média

### cfg-014 · usuarios page.ts
- **Arquivo:** `src/routes/(app)/configuracoes/usuarios/+page.ts` — create
- **Conteúdo:** `load` com `?page&search&nivel&situacao` (URL-state); estados Loading/Error integral.
- **DoD:** filtros vêm da URL (deep-link), não de estado local.
- **Deps:** cfg-004, cfg-011 · **Prioridade:** Média

### cfg-015 · usuarios page.svelte
- **Arquivo:** `src/routes/(app)/configuracoes/usuarios/+page.svelte` — create
- **Conteúdo:** tabela usuários (nivel/situacao chips) + search + filtros + Pagination + Empty; `ModalUsuario` criar/editar + modal-confirm desativar; `data-testid="cfg-user-*"`, `cfg-users-skeleton/empty/error`.
- **DoD:** Admin hide para demais; desativar exige confirmação; convite dispara POST.
- **Deps:** cfg-014, cfg-010 · **Prioridade:** Média

### cfg-016 · permissoes page.ts
- **Arquivo:** `src/routes/(app)/configuracoes/permissoes/+page.ts` — create
- **Conteúdo:** `load` com `obterPermissoes()`; estados Loading/Error.
- **DoD:** matriz carregada no servidor (load) e repassada à UI.
- **Deps:** cfg-005, cfg-011 · **Prioridade:** Média

### cfg-017 · permissoes page.svelte
- **Arquivo:** `src/routes/(app)/configuracoes/permissoes/+page.svelte` — create
- **Conteúdo:** matriz RBAC (módulos × níveis) com `PermCell`, "restaurar padrão" (com confirmação) e salvar; `data-testid="cfg-perm-matrix"`, `cfg-perm-save`, `cfg-perm-reset`.
- **DoD:** Admin-only; alterações destacadas até salvar; default explícito por módulo.
- **Deps:** cfg-016, cfg-009 · **Prioridade:** Média

### cfg-018 · sistema page.ts
- **Arquivo:** `src/routes/(app)/configuracoes/sistema/+page.ts` — create
- **Conteúdo:** `load` com `obterSistema()` + tokens; estados Loading/Error.
- **DoD:** dados de identidade e cadência 5S carregados.
- **Deps:** cfg-006, cfg-011 · **Prioridade:** Média

### cfg-019 · sistema page.svelte
- **Arquivo:** `src/routes/(app)/configuracoes/sistema/+page.svelte` — create
- **Conteúdo:** identidade (nome fablab, logo), cadência 5S (checklist/auditoria), tokens de integração (criar/revogar com confirm); `data-testid="cfg-system-*"`, `cfg-token-*`.
- **DoD:** Admin-only; salvar Sistema notifica que cadência 5S afeta Produção; criar token copia-chave única.
- **Deps:** cfg-018 · **Prioridade:** Média

---

## A5 — Testes

### cfg-020 · unit RBAC config
- **Arquivo:** `frontend/tests/unit/permissions.test.ts` — modify
- **Conteúdo:** casos `isAdmin` + `canViewConfiguracoes` para os 5 níveis (0..4) e hide-para-Admin.
- **DoD:** rodando verde com vitest.
- **Deps:** cfg-008 · **Prioridade:** Média

### cfg-021 · e2e happy path (rg config)
- **Arquivo:** `frontend/tests/e2e/configuracoes.spec.ts` — create (se suite e2e existir; senão vitest unit/flow)
- **Conteúdo:** perfil (editar nome + alterar senha), usuarios (criar convite), permissoes (toggle célula + salvar), sistema (criar/revogar token); guarda: voluntário → redirecionado ao acessar `/usuarios`.
- **DoD:** fluxos Admin + guarda RBAC verificados.
- **Deps:** cfg-013, cfg-015, cfg-017, cfg-019 · **Prioridade:** Baixa

---

## Resumo
- **19 arquivos** (16 create · 2 modify) · **0 no template `_template`**
- **Dependências entre features (relatório):** `menu.ts` (cfg-007 ↔ producao prd-same) e `permissions.ts` (cfg-008 ↔ producao prd-same) → **executar sequencial**; `configuracoes/sistema` ↔ `producao/parametros-5s` (cadência 5S, endpoint único compartilhado).

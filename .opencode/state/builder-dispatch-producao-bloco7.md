# DESPACHO — Builder (Frontend) · PRODUCAO — BLOCO 7 (ressalvas Validator+Security) · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **correções das ressalvas** apontadas pelo Validator e Security
> na feature `producao` (blocos 1..6 já commitados). **Spec de referência:**
> `.opencode/specs/frontend/features/producao/requirements.md` §RBAC (linhas 47–55) — fonte da verdade:
>
> | Recurso | Admin | Bolsista | Voluntário | Estagiário | Recrutando |
> |---|---|---|---|---|---|
> | Resumo | ver | ver | ver | ver (se atribuído) | ver |
> | Tarefas/Projetos | editar tudo | editar se atribuído | editar se atribuído | ver | ver |
> | 5S setores/auditoria | editar direto | editar se resp./auditor | editar se resp./auditor | **HIDE** | **HIDE** |
> | 5S advertências | editar | — | — | HIDE | HIDE |
> | 5S mesas | editar | editar | editar | — | — |
> | Máquinas | tudo | registra uso | registra uso | ver | ver |

## Correções (P1 — RBAC, obrigatórias)

1. **`src/lib/utils/permissions.ts` — `canSee5S`**: hoje aceita roles 1..3. Corrigir para **Admin (0) ou Bolsista/Voluntário (1..2)** com grant `roles.producao ∈ {'view','edit'}`; **Estagiário (3) e Recrutando (4) → false** (HIDE); `roles.producao===null` → false.
2. **`canSeeAdvertencias`**: hoje aceita `role===0 || roles.producao==='edit'`. Corrigir para **Admin-only (role 0)** conforme §RBAC (Bolsista/Voluntário "—").
3. **`canAuditar5S`**: Admin ou auditor nomeado (responsabilidade), **nunca Estagiário/Recrutando**; manter roles 1..2 com responsabilidade.
4. **`src/routes/(app)/producao/5s/auditoria/+page.svelte`** (Security M-1): a ação **"Auditar"** (abrir `ModalAuditoria` → `concluirInspecao5S`) deve ser gated por `canAuditar5S` (hoje só por `status !== 'Concluída'`). Ação HIDE (não disable) quando sem permissão; alinhar com o gate de "Nomear" já existente.
5. **`src/lib/components/layout/Sidebar.svelte` + `src/lib/config/menu.ts`** (Validator ressalva 1): os filhos do submenu Produção hoje aparecem para quem não pode vê-los (viola "HIDE, nunca disable"). Adicionar em `MenuItem['children']` um campo opcional de visibilidade por usuário (ex.: `canSee?: (user: User | null) => boolean`) e aplicar o filtro no `Sidebar`/`SidebarGroup`; em `menu.ts` marcar os filhos 5S com `canSee5S` e o de Advertências com `canSeeAdvertencias` (imports de `$lib/utils/permissions`; sem ciclos). **Atenção:** `menu.ts` é arquivo do ConflictMap C1 (compartilhado com configurações) — nesta ordem, produção é a dona atual; mudança mínima e aditiva, sem quebrar Estoque/Dashboard.

## Correções (P2 — atribuição/UX)

6. **`canEditProducao` por recurso** (Validator ressalva 3 / §RBAC "editar se atribuído"): hoje as telas usam `data.canEditProducao` sem recurso → vira Admin-only. Aplicar atribuição nas telas onde a spec pede: Tarefas (`responsavelId` da tarefa), Setores 5S (responsáveis do setor), Máquinas (responsável de manutenção), Projetos (responsável) — usar `isResponsavelAtribuido(user, { responsavelId })` combinado com o flag do layout. Manter hide-não-disable. (Se o custo for alto em alguma tela, implementar nas de maior valor — Tarefas e Setores — e reportar o restante.)
7. **Resumo mockado sem aviso** (Validator ressalva 5): exibir banner/chip visível "Dados de exemplo — contrato 🟡 pendente" quando `USE_PRODUCAO_MOCK` (exportado de `$lib/api/producao/mocks`) estiver `true`, como nas outras telas.

## Correções (P3 — endurecimento Security)

8. **B-4**: `5s/advertencias/+page.ts` — filtros `tipo`/`situacao` lidos da URL sem allowlist (padrão já usado em `tarefas/+page.ts`/`projetos/+page.ts`); aplicar allowlist igual.
9. **B-3**: `api/producao/projetos.ts` — validar URL de documento (aceitar apenas `http:`/`https:` via `new URL`) antes de montar payload/exibir link (a UI de upload ainda é 🟡; aplicar validação no helper/modal se houver ponto de entrada).

## Testes
10. **`frontend/tests/unit/permissions.test.ts`** — atualizar os casos que assumem o comportamento antigo (Estagiário visível no 5S, advertências com grant) para o novo §RBAC; adicionar casos: Estagiário/Recrutando `canSee5S===false`; `canSeeAdvertencias` Admin-only (role 1 com grant 'edit' → false); `canAuditar5S` Estagiário false. **Não remover cobertura existente** válida.

## Regras
1. **Ordem: P1 → P2 → P3 → testes. NÃO commitar** (Orchestrator commita ao final).
2. NÃO tocar `docs/`, specs, código de outras features (estoque/rh/etc.), `types/auth.ts` (só ler). `menu.ts`/`permissions.ts`/`Sidebar.svelte` são compartilhados — mudanças **mínimas e aditivas**.
3. Sem `any`/`console.log`; pt-BR; hide nunca disable; preservar svelte-check baseline.
4. Ao final: `npm run test` (todos verdes, incluindo os atualizados) + `npm run check` baseline 12/15/7 (zero novos).

## Relatório
- Por item (1..10): ✅/❌ + evidência (arquivo:linha)
- Telas onde a atribuição (item 6) foi aplicada vs adiada (com motivo)
- `npm run test` (contagem) + `npm run check`
- Pontos de atenção / desvios
# DESPACHO — Builder (Frontend) · RH — Bloco 5 (B5 Níveis + Menu) · 21/09/2026

> Implemente **rh-025..rh-026** de `.opencode/specs/frontend/features/rh/tasks.md` (ler o bloco B5 integral + mockup `docs/mokups/pessoas/requisitos.md`).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-4 commitados: fundação + pessoas + PS/horas + treinamentos; guarda `pessoas/+layout.ts` já redireciona `/pessoas/niveis` se `!canSeeNiveis`
- Reuso: `api/rh/niveis.ts` (getNiveis 🔴, alterarNivel 🔴, convidarUsuario 🟡), kit (PageHeader, TableSkeleton, EmptyState, ErrorBanner, StatusBadge?, modal padrão), `utils/permissions.ts` (`canSeeNiveis = isAdmin`), `stores/toast`, `config/menu.ts` (item `Pessoas & RH` existente — conferir raiz/path/module/icon antes de editar)
- NÃO alterar specs/docs; NÃO commitar; sem libs novas; **menu.ts é compartilhado (⚠️ sequencial — livre agora)**

## Tasks
- **rh-025** `src/routes/(app)/pessoas/niveis/+page.svelte` — PageHeader + "+ Novo usuário" (`modal-convidar`: Nome\*/Email\*/Nível\* → `convidarUsuario` 🟡 D-8); matriz Nível×módulo (checks/restrições; matriz **autoritativa do backend** — nunca derivar no client); membros (Avatar+nome+matrícula, Nível badge, Grupo, select troca → `alterarNivel` 🔴 + refetch + toast); aviso "nível é autoritativo no backend". Guard `isAdmin` (redirect `/pessoas` — já ativo no `+layout.ts`; não duplicar). `data-testid="nivel-membro/nivel-convidar"`; não-Admin nunca vê conteúdo (redirect, não 403 em tela).
- **rh-026** `src/lib/config/menu.ts` ⚠️ MODIFY — no item `Pessoas & RH` existente, adicionar `children: [{Pessoas,/pessoas}, {Processo seletivo,/pessoas/processo-seletivo}, {Registro de horas,/pessoas/registro-horas}, {Treinamentos,/pessoas/treinamentos}, {Níveis & acesso,/pessoas/niveis, canSee: isAdmin}]`. Sem alterar `module`/`icon`/`path` raiz. Confirmar `isAdmin` importado.

## Regras
1. Criar/editar SÓ os 2 arquivos acima.
2. pt-BR; aria-label; data-testid do mockup; sem any; Svelte 5 runes; tokens design-system; sem cálculo de permissão no client.
3. Ao final: `npm run test` (regressão 300) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + evidência; contrato 🔴/🟡; resultados test/check/build; desvios.
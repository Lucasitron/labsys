# DESPACHO — Validator (Frontend) · RH · 21/09/2026

> Validar a feature `rh` implementada (commits 4b33407 · 9d76271 · 642a856 · b8c4a8c · 607084e · 4406cb0) contra spec/mockup.
> **Princípio código mínimo: verificar pertinência/código mínimo; sinalizar código não pertinente.**

## Contexto
- Workspace `/home/lucasitron/Documentos/05-dev/labSys`; branch `dev/frontend` (NÃO tocar em nada; somente leitura)

## Entradas
- Spec: `.opencode/specs/frontend/features/rh/{requirements,plan,tasks}.md` (30 tasks rh-001..rh-030)
- Mockup aprovado: `docs/mokups/pessoas/` (full.html + novasTelasEstados.html + requisitos.md § testids)
- Código: `src/lib/{types/rh.ts, api/rh/{pessoas,processo-seletivo,horas,treinamentos,niveis}.ts, utils/rh-status.ts, utils/permissions.ts(helpers RH), components/ui/{TypeBadge,DonutChart,KanbanBoard}.svelte, config/menu.ts(submenu)}` · `src/routes/(app)/pessoas/**` (+layout.ts guarda, lista, [id], novo, editar, processo-seletivo, registro-horas, treinamentos/**, niveis) · testes `frontend/tests/unit/rh-*.test.ts` + `rh-page-state.svelte.js`
- DoD/checklists em plan.md §5 (se existir) + tasks DoD

## Critérios
1. 30 tasks implementadas (1 arquivo=1 task); tipos sem any; APIs PT com query mínima; `searchPeople` re-export legado (nenhum path `/rh/people` novo); **zero soma de horas no client** (R-5: `rg` de somar/totalizar/consolidar em `horas.ts` e registro-horas)
2. Guarda `pessoas/+layout.ts` (canView→302; níveis 303); RBAC **hide** no menu/submenu (Níveis `canSee: isAdmin`, ausência de DOM); Estagiário só próprio registro; Kanban ações hide se não responsável/tutor; sessão hide/guarda `isInstrutor`
3. URL fonte de verdade (`?search=&setor=&nivel=&status=&page=`, `?estagio=`, `?status=&periodo=`, `?tab=`, `?tab=` detalhe); estados (loading/skeleton, success, empty, empty-filtrado, error+retry, RBAC); paginação server-side na lista (D-2)
4. Detalhe com tabs deep-linkáveis + erro parcial por aba; novo/editar com 409-inline, matrícula read-only, busy anti-duplo; preferências de rótulo **Tutor (PS)** vs **Instrutor (treinamentos)** (Dívida 6); SEM Exportar CSV (R-8); testids do mockup; pt-BR; aria-label
5. Testes: 381 verdes; cobertura coerente (81 novos); sem testes frágeis; build/check ok
6. **Código mínimo:** duplicações/speculativo → ressalva (ex.: `DELETE /api/rh/pessoas/{id}` inline na lista em vez de na API; donuts em unidade de treinamento; "Em breve" via toast)

## Produto
- **UM output completo inline** (proibido resumir/encaminhar): veredito (APROVADA/APROVADA COM RESSALVAS/REPROVADA) + checklist por critério (✅/❌/⚠️ com evidência arquivo:linha) + ressalvas por severidade (Alta/Média/Baixa), cada uma com correção mínima + verificação final: `npm run test` (381), `npm run check`, `npm run build`.

## Regras
- Somente leitura; **NÃO editar/commitar**; não instalar libs; não usar e2e (sem Playwright).
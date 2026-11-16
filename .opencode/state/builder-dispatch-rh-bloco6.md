# DESPACHO — Builder (Frontend) · RH — Bloco 6 (B6 Testes) · 21/09/2026

> Implemente **rh-027..rh-030** de `.opencode/specs/frontend/features/rh/tasks.md` (ler o bloco B6 integral).
> **Princípio código mínimo: testes só do que resolve caso real; sem duplicar cobertura existente.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-5 commitados. Baseline **300 testes verdes** (23 files). Padrão: `frontend/tests/unit/*.test.ts` (vitest + @testing-library/svelte; alias vite: svelte→index-client.js + server.deps.inline)
- e2e: **Playwright NÃO instalado** → criar `frontend/tests/e2e/rh.spec.ts` **apenas se alguma suíte e2e ativa existir** em `frontend/tests/e2e/`. Verificar; senão, documentar SKIP no relatório (sem arquivo).
- Restrições dos testes: `rejeitarHoras` legado usado na tela de horas (nova modelagem só tem validar); `DELETE /api/rh/pessoas/{id}` assumido inline na lista (fora da API — mockar `apiFetch`/`client` conforme o componente usa); paginação da lista pessoas é **server-side**; KPIs de horas vêm do extrato (`getHorasDisponiveis`); PS avalia individual; sessão só banner com `agora`.
- NÃO alterar specs/docs; NÃO commitar

## Tasks
- **rh-027** `frontend/tests/unit/rh-status.test.ts` — os 7 maps (personStatusMeta/psStageMeta/horaStatusMeta/trainingStatusMeta/disponibilidadeMeta/nivelMeta/guideStatusMeta): label+cor por valor canônico; fallback desconhecido; cobertura das unions de rh-001. Sem render.
- **rh-028** `frontend/tests/unit/rh-api.test.ts` — mock `apiFetch` (`vi.mock('$lib/api/client')`): pessoas monta `/api/rh/pessoas` com search/setor/nivel/status/page e **omite** vazios; get/create/update paths/métodos; PS grupos/mover/avaliar paths; horas validar/rejeitar + `getHorasDisponiveis`/`validarApontamento`; treinamentos detalhe/sessão/avaliar/tarefa-final; níveis alterar; **assert nenhum path `/rh/people` novo e nenhum `/stock/` em paths** (imports `$lib/api/stock/*` ok); **assert nenhum helper de soma** (`somar|totalizar|consolidar`) exportado por `horas.ts`.
- **rh-029** `frontend/tests/unit/rh-permissions.test.ts` — `isResponsavelRH` (admin✓/sem-resp✗/com-resp✓/recrutando✗) · `canEditRH` (admin✓, bolsista responsável✓, bolsista comum✗, estagiário✗) · `canSeeNiveis` (só admin) · `isTutor/isInstrutor` distintos (mesma pessoa pode ser ambos; flags independentes) · `EDIT_RULES.rh` intacto `[0,1,2]` · menu submenu: Níveis omitido p/ `!isAdmin` (hide real).
- **rh-030** `frontend/tests/unit/rh-pages.test.ts` (unit) + e2e condicional (SKIP documentado) — unit com render + `vi.mock` das APIs: lista (skeleton/success/empty/empty-filtrado com chips+limpar/error+retry; `!canEdit` sem checkbox/ações/novo); editar (banner `edit-rbac-banner` sem ações quando `!canEdit`; matrícula read-only; 409 inline); detalhe-treinamento (CTA sessão só com sessão ativa; ações hide se `!isInstrutor`); horas (validar/rejeitar chamam PATCH + refetch; KPIs do extrato mockado, sem soma); PS (avaliar individual); níveis (redirect não-admin — testar o load/guarda sem render pesado se preciso).

## Regras
1. Criar SÓ os arquivos de teste (mocks locais; sem tocar produção a menos que bug real das tasks exija — então reportar).
2. Rodar `npm run test` (todos verdes; novo total reportado) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- branch; por task ✅/❌ + nº de casos; total final; check; build; nota e2e SKIP; desvios.
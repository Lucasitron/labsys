# DESPACHO — Validator (Frontend) · FINANCEIRO · 23/09/2026

> Validar a feature `financeiro` implementada (commits 2e78ed8 · 4ccc9e3 · b9a6eb0 · 49cbfb2 · 06aa0f6) contra spec/mockup.
> **Princípio código mínimo: verificar pertinência/código mínimo; sinalizar código não pertinente.**

## Contexto
- Workspace `/home/lucasitron/Documentos/05-dev/labSys`; branch `dev/frontend` (NÃO tocar em nada; somente leitura)

## Entradas
- Spec: `.opencode/specs/frontend/features/financeiro/{requirements,plan,tasks}.md` (23 tasks fin-001..fin-023; reconciliação no plan §8)
- Mockup aprovado: `docs/mokups/financeiro/` (full.html 7 telas + requisitos.md § testids)
- Contrato: `docs/contract.gap.md` §5.4
- Código: `src/lib/{types/financeiro.ts, api/financeiro/*.ts (6), utils/financeiro-{status,format}.ts, config/menu.ts(submenu)}` · `src/routes/(app)/financeiro/**` (+layout.ts Admin-only, +page resumo, lancamentos, contas-pagar, contas-receber, doacoes, custeio, relatorios) · testes `frontend/tests/unit/financeiro-*.test.ts`
- DoD/checklists em plan.md §5 (se existir) + tasks DoD

## Critérios
1. 23 tasks (1 arquivo=1 task); tipos sem any; APIs PT query mínima; sem `calcular|soma|margem(|totalizar|agregar|somar`; zero UI novo; `permissions.ts` intacto
2. Guarda Admin-only total (303 `/dashboard` p/ qualquer `/financeiro/*`); RBAC **hide total** (item invisível via `canView` regra `[0]`; submenu sem canSee); nunca 403 em tela; nunca disable
3. URL fonte de verdade (lancamentos `?search=&status=&tipo=&categoria=&periodo=&ordenar=&page=&pageSize=`; custeio `?tab=`; relatorios `?relatorio=&periodo=`); estados completos + erro parcial por bloco; paginação server-side; contas = vistas derivadas (nenhum `/api/financeiro/contas`); 409/422 → ApiError
4. KPIs/totais/cobertura/margens servidos (nunca calculados); SEM Exportar (lançamentos/resumo/relatórios); testids do mockup; pt-BR; aria-label; notas de congelamento/auditoria/compras visíveis
5. Testes: 524 verdes; cobertura coerente (73 novos); sem frágeis; build/check ok
6. **Código mínimo:** duplicações/speculativo → ressalva (ex.: modal "Novo lançamento" duplicado no resumo; barras por contagem; filtros locais vencimento/origem)

## Produto
- **UM output completo inline** (proibido resumir/encaminhar): veredito + checklist por critério (✅/❌/⚠️ + evidência arquivo:linha) + ressalvas por severidade (Alta/Média/Baixa) com correção mínima + verificação: `npm run test` (524), `npm run check`, `npm run build`.

## Regras
- Somente leitura; **NÃO editar/commitar**; sem e2e (sem Playwright).
# VALIDAÇÃO DE ENTREGA — PRODUCAO (FRONTEND) · 20/09/2026

Feature `producao` — implementação completa (42 tasks, blocos 1..6) commitada em `dev/frontend`:
`c4406f4` (bloco 1 types/utils/client) · `84b643a` (bloco 2 api layer) · `a011991` (bloco 3 components/modais) · `7bc70f5` (bloco 4 menu/RBAC/layout) · `6863ce3` (bloco 5 12 telas) · bloco 6 testes (146 verdes)

## O que foi entregue (escopo da validação)
- `src/lib/types/producao.ts` (18+ tipos, `Paginado<T>`) · `src/lib/utils/producao-status.ts` (statusMeta/notaToCartao/gravidadeMeta)
- `src/lib/api/producao/` 12 módulos REST + `client.ts` (barrel) + `mocks.ts` (resumo, `USE_PRODUCAO_MOCK`)
- `src/lib/components/producao/` CartaoBadge + 8 modais
- `src/lib/config/menu.ts` (10 filhos Produção) · `src/lib/utils/permissions.ts` (canSee5S/isResponsavelAtribuido/canAuditar5S/canSeeAdvertencias/canEditProducao)
- `src/routes/(app)/producao/*` — layout (guardas RBAC) + 12 telas (Resumo, 5S x6, Projetos/[id], Tarefas-Kanban, Máquinas/[id])
- Testes: `producao-status.test.ts` (22) + `permissions.test.ts` (45) — total 146

## Regras do Validator
1. **Contexto zero**: usar APENAS este arquivo + código em disco (`dev/frontend`) + mockups em `docs/mokups/producao/` para conferir fidelidade. Não usar conversas passadas do Orchestrator.
2. Verificar contra DoD do plan.md §8 + requirements.md (A1..A5) + mockup visual.
3. Checklist focado em: (a) fidelidade ao mockup/requirements; (b) estados Loading/Error/Empty/RBAC; (c) RBAC hide (nunca disable); (d) pt-BR/exports/DS tokens; (e) A2 (nota/cartão nunca escrito no client); (f) kanban a11y; (g) contratos 🟡 tratados com erro amigável/nota (não blockers silenciosos).
4. Reportar: **APROVADA** ou **APROVADA COM RESSALVAS** (listar cada ressalva com arquivo e sugestão) ou **REPROVADA** (bloqueantes). Não editar código nem commitar.

## Checklist (executar itens empiricamente no codebase)
- [ ] 12 telas existem e casam com as 12 do mockup (rotas + abas)
- [ ] Guardas RBAC: redirect 5S p/ Recrutando, advertencias Admin-only, colunas kanban não-editáveis sem permissão
- [ ] `notaToCartao`/prévia A2: nenhuma escrita de nota/cartão no client (grep por set nota/cartao)
- [ ] Estados Loading/Error/Empty nas telas com dados
- [ ] Sem `any`/`console.log` nos arquivos producao
- [ ] 146 testes verdes (`npm run test` em frontend/) e svelte-check baseline
- [ ] Modais seguem decisões PO A1 (checklist travado), A3 (modal pendência), A4 (saída material c/ nota), A5 (status manual)
- [ ] `npm run check` — verificar manualmente (baseline 12/15/7)

Relatório final: status + lista de ressalvas (arquivo:linha + sugestão) + evidência dos itens conferidos.
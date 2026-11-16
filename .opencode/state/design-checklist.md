# Design Mockup — Lista de Conclusão (Revisão Humana 1 a 1)

> Gerado em 2026-09-19. Fase atual: **AGUARDANDO REVISÃO HUMANA**.
> Marque cada mockup como `✅ APROVADO` ou descreva as alterações em `✏️ ALTERAÇÕES`.
> Ao terminar, o Orchestrator materializa as alterações e libera o Builder.

## Como revisar (MODO INTERATIVO — perguntas em tempo de execução)

> Desde 20/09 a revisão é conduzida pelo Orchestrator em **modo pergunta-resposta**, igual
> aos agentes interativos do opencode: **UMA pergunta por vez** → o Orchestrator informa a
> feature e os arquivos a abrir → aguarda a resposta humana → registra no Log de revisão →
> avança. **NUNCA** despejar as 9 features de uma vez, **NUNCA** marcar `APROVADO` sem
> resposta explícita do humano.

1. O Orchestrator informa a feature atual + arquivos a abrir (`full.html` de cada módulo,
   navegável no browser: telas + estados + alternador de papel RBAC).
2. O humano abre no browser e responde **`APROVADO`** ou **`ALTERAÇÕES`** (lista objetiva).
3. Se `ALTERAÇÕES` → o Orchestrator repassa ao **Designer** (novo ciclo de mockup) e, após
   a atualização, **reabre a pergunta**. Nunca implementar "no chute".
4. Conferência padrão contra a spec `.opencode/specs/frontend/features/<modulo>/requirements.md`:
   rotas, RBAC hide (nunca disable), 4 estados (loading/success/empty/error), tokens do
   design-system, pt-BR, `verify-*.js` quando existir.
5. Resultado registrado na tabela do Checklist + Log de revisão; atualizado
   `state/frontend-orchestration.json` (humanReview/status).

---

## Checklist

| # | Feature | Mockup | Telas | Arquivos | Resultado |
|---|---------|--------|-------|----------|-----------|
| 1 | `auth` | `docs/mokups/login/` | Login + estados | `login.html`, `loginEstados.html`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |
| 2 | `dashboard` | `docs/mokups/dashboard/` | App Shell + Dashboard | `dashboard.html`, `dashboardState.html`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |
| 3 | `estoque` | `docs/mokups/estoque/` | 19 telas (13 originais + 6 novas) | `full.html`, `editar-item.html`, `entrada-detalhe.html`, `saida-detalhe.html`, `emprestimo-detalhe.html`, `fornecedor-novo.html`, `localizacao-novo.html`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |
| 4 | `rh` | `docs/mokups/pessoas/` | 12 telas (10 + 2 novas, rotas `/pessoas/*`) | `full.html`, `novasTelasEstados.html`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |
| 5 | `vendas` | `docs/mokups/vendas/` | 11 telas (inclui Marketplace, CRM) | `full.html`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |
| 6 | `financeiro` | `docs/mokups/financeiro/` | 7 telas (Admin-only) | `full.html`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |
| 7 | `producao` | `docs/mokups/producao/` | 12 telas (v2) + 24 modais | `full.html`, `verify-prod.js`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |
| 8 | `notificacoes` | `docs/mokups/notificacoes/` | 3 telas (central + preferencias + historico) | `full.html`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |
| 9 | `configuracoes` | `docs/mokups/configuracoes/` | 4 telas (perfil + usuarios + permissoes + sistema) | `full.html`, `verify-config.js`, `requisitos.md`, `instrunctions.md` | ✅ APROVADO |

---

## Pontos de atenção / decisões pendentes

### Estoque (novas telas)
- [x] **DECIDIDO (PO 19/09):** os endpoints `POST /api/stock/suppliers`, `POST /api/stock/locations`, `GET /api/stock/movements/{id}` são **responsabilidade do backend** (front só consome via gateway). Mapeados em `docs/contract.gap.md` §5.6.
- [x] **DECIDIDO (PO 19/09):** rota `/(app)/estoque/emprestimos/[id]` (detalhe) **MANTIDA**.
- [ ] Fornecedor/Localização em **página cheia** (não modal) — confirmar na revisão do mockup.

### RH (realinhado)
- [ ] Rotas agora `/(app)/pessoas/**` (alinhado à spec + menu real) — **confirmar prefixo na revisão**
- [ ] `registro-horas` e `niveis` corrigidos (antes `registro-de-horas`/`niveis-acesso`) — **confirmar**
- [x] **DECIDIDO (PO 19/09):** endpoints auxiliares (`GET /api/rh/processo-seletivo`, `GET /api/rh/treinamentos/{id}`) **mapeados** em `docs/contract.gap.md` §3.2.

### Notificacoes (novo)
- [ ] Página central + admin (preferencias/historico) geradas — **confirmar fluxo**
- [ ] Dropdown da topbar NÃO redesenhado (permanece no escopo do mockup do dashboard) — **ok?**
- [ ] Token `elevated`: novo mockup usa `#111a2c` (canônico); mockups antigos usam `#1a2235` — **PENDENTE PO** (migração)

### Vendas (spec desatualizada)
- [ ] Spec requirements.md cita "marketplace/CRM mockup pendente", mas o mockup JÁ cobre — **PENDENTE PO** (limpar nota)

### Geral
- [x] **DECIDIDO (PO 19/09):** `docs/` **permanece no `.gitignore`** — mockups **não serão versionados**.

---

## Log de revisão

| Motivo | Feature | Status | Data |
|--------|---------|--------|------|
| Geração inicial | todos EXCETO configuracoes | AGUARDANDO | 2026-09-19 |
| Decisões PO: endpoints estoque/RH (backend-owned, mapeados) + rota empréstimo mantida + docs/ não versionado | estoque, rh, geral | REGISTRADO | 2026-09-19 |
| Producao v2 (12 telas) + Configuracoes (4 telas) adicionadas ao checklist | producao, configuracoes | AGUARDANDO | 2026-09-20 |
| Revisão em MODO INTERATIVO (pergunta-resposta em tempo de execução) + auth implementada/validada (commit 5b68329) — revisão inicia | auth | EM REVISAO | 2026-09-20 |
| **Humano APROVOU** — item 1/9 | auth | ✅ APROVADO | 2026-09-20 |
| **Humano APROVOU TODOS (2/9..9/9)** — todos os mockups aprovados para esta versão | dashboard, estoque, rh, vendas, financeiro, producao, notificacoes, configuracoes | ✅ APROVADO | 2026-09-20 |
# DESPACHO — Builder (Frontend) · PRODUCAO — Bloco 3 (prd-016..prd-024) · 20/09/2026

> Preparado pelo Orchestrator. Escopo: **componentes compartilhados + modais** da feature `producao`.

---

## Contexto (ler antes)

- Tasks: `.opencode/specs/frontend/features/producao/tasks.md` (`prd-016..prd-024`, em ordem numérica)
- Plano: `.opencode/specs/frontend/features/producao/plan.md` (camadas §2, DoD §8); Mockup: `docs/mokups/producao/` + `requisitos.md` (A1–A5)
- Bloco 1 (`c4406f4`): `types/producao.ts` + `utils/producao-status.ts` (com `statusMeta` maps pt-BR + `notaToCartao` + `gravidadeMeta` + `CARTAO_META`)
- Bloco 2 (`84b643a`): 12 módulos API em `src/lib/api/producao/` + `client.ts` reexports (`obterSetores5S`, `criarSetor5S`, `atualizarSetor5S`, `obterInspecoes5S`, `concluirInspecao5S`, `nomearAuditor5S`, `obterPendencias5S`, `resolverPendencia5S`, `registrarPenalidadeParaPendencia5S`, `obterMesas`, `criarMesa`, `obterTarefas`, `criarTarefa`, `obterProjetos`, `criarProjeto`, `atualizarProjeto`, `obterMateriaisDoProjeto`, `registrarSaidaMaterial`, `obterProjetos` etc.)
- `apiFetch` global: base `/api` já inclui; paths producao = `/producao/...`; `bearer()` helper no `client.ts` producao
- 95 testes verdes; baseline svelte-check 12/15/7 (estoque/** + ui/Chip.svelte)

## Tasks do bloco (o Builder toca SOMENTE estes)

Criar em `frontend/src/lib/components/producao/` (todos `create`):

| Task | Arquivo `.svelte` | Conteúdo · DoD |
|---|---|---|
| prd-016 | `CartaoBadge.svelte` | badge cartão 5S (Verde/Amarelo/Vermelho) com cor de token; props `cartao: Cartao5S`; `data-testid="prd-cartao-{cor}"`; reuso setores/auditoria/ranking/mesas |
| prd-017 | `ModalAuditoria.svelte` | checklist 5S por item, Conforme/Não conforme; **prévia automática** x/y · % · faixa cartão (`notaToCartao`) — **SEM input manual de nota**; CTA `Concluir auditoria` → `concluirInspecao5S(id, { itens })` (backend recalcula, nunca escreve nota no client). `data-testid="aud-concluir"`; A2 |
| prd-018 | `ModalSetor.svelte` | criar/editar setor (A1): Nome · Ciclo (radio semanal/quinzenal/mensal) · Responsáveis (multi) · Auditor · seletor de papel (Admin → salva direto `criarSetor5S`/`atualizarSetor5S`; Responsável/Auditor → CTA `Enviar p/ aprovação` → `aprovarAlteracaoDoSetor` com `aprovado:false`). CTA/título dinâmicos criar/editar; `data-testid="set-criar","set-salvar"` |
| prd-019 | `ModalChecklistSetor.svelte` | checklist 5S do setor: 5S **travado** (ordem/peso fixos); itens **editáveis** (add/rename/remove) **sem reordenar e sem peso** (1 item=1 ponto); nota exibida; seletor de papel (Admin salva via `atualizarChecklistDoSetor` / outros Enviar p/ aprovação). `data-testid="set-checklist-save","set-checklist-enviar"`; A1 |
| prd-020 | `ModalDetalhePendencia.svelte` | A3: origem (auditoria/auditor/setor) · itens não conformes · anexos · prazo (danger se vencido) · gravidade badge (`gravidadeMeta`) · situação · penalidade sugerida · histórico; ações `Resolver` (`resolverPendencia5S`) e `Registrar penalidade` (**Admin-only**, `registrarPenalidadeParaPendencia5S`). `data-testid="pend-resolver","pend-penalidade"` |
| prd-021 | `ModalMesa.svelte` | criar mesa de projeto: Projeto universitário (select de `obterProjetos`) · Membro responsável · Período experimental (checkbox) · QR totem (gerado — texto/placeholder, sem lib) → `criarMesa`. `data-testid="mesa-criar","mesa-save"` |
| prd-022 | `ModalNovaTarefa.svelte` | Título · Projeto (select) · Responsável · Prioridade (radio) · Prazo · Kanban coluna inicial (Fila default) · Descrição · Checklist → `criarTarefa`; reuso em Tarefas e Projeto(aba). `data-testid="tar-nova"`; A5 (status manual, sem peso) |
| prd-023 | `ModalNovoProjeto.svelte` | Nome · Cliente · Responsável · Prazo · Descrição · status inicial Planejado → `criarProjeto`; modo editar preenchido → `atualizarProjeto(id, ...)`. `data-testid="proj-novo","proj-save"` |
| prd-024 | `ModalSaidaProjeto.svelte` | A4 🟡: saída de material: Item (do BOM — `obterMateriaisDoProjeto`) · Quantidade (≤ disponível, validação local) · Destino (fixo projeto corrente) · nota de baixa automática no Estoque (backend) → `registrarSaidaMaterial`. **Botão registra; se repositório falhar (contrato 🟡), exibir erro e reportar** — NÃO blocker silencioso. `data-testid="proj-saida-material"` |

## Regras

1. **1 task por vez, em ordem numérica; NÃO commitar** (Orchestrator commita ao final).
2. **NUNCA** tocar: `docs/`, specs, `client.ts`, `types/auth.ts`, `menu.ts`, `permissions.ts` (bloco 4 — C1/C2), `estoque/**`, routes, stores; **somente** os 9 arquivos acima em `frontend/src/lib/components/producao/`.
3. UI: pt-BR; design tokens existentes (ver `ui/` e padrão dashboard/estoque); estados Loading/Error/Empty sane nos modais que carregam dados (selects); a11y (labels, focus, Esc fecha); **RBAC hide: componentes JA recebem flags `canEdit`/`isAdmin`/`isResponsavel` do pai — não importar auth store nos modais** (decisão de camada: guarda no layout/rotas).
4. Prop Types: usar tipos `producao.ts` (`Setor5S`, `Inspecao5S`, `Pendencia5S`, `Mesa5S`, `Tarefa`, `Projeto`, `Cartao5S`, `Gravidade5S`, ...) — sem `any`; props explícitas com defaults.
5. Formulários: `on:submit|preventDefault`; disable durante submit; erros `ApiError`/`NetworkError` exibidos inline; sem `console.log`.
6. Modais podem importar `client` (via `import ... from '$lib/api/producao'`) para as chamadas — **exceto prd-024** que depende de `client.ts` conector (rotas ainda não existem: modais são standalone).
7. Ao final: `npm run test` (95 verdes, sem testes novos) e `npm run check` SEM erros novos (baseline 12/15/7 estoque/ui). Componentes `.svelte` com props de dados não precisam de data loading próprio.

## Relatório esperado (responda com)

- Por task `prd-0XX`: ✅/❌ + evidência (arquivo + props principais + data-testids + como verificou)
- Dependências das rotas do módulo que os modais esperam (para o Orchestrator alinhar bloco 5)
- `npm run test` (contagem) + `npm run check` (resumo)
- Pontos de atenção / desvios
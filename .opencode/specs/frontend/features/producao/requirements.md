# Produção & Projetos — Frontend Requirements (v2)

> **Fonte da verdade:** mockup v2 `docs/mokups/producao/full.html` + `docs/mokups/producao/instrunctions.md` (12 telas, Prompts 0–12)
> + `docs/mokups/producao/requisitos.md` (decisões PO A1–A5). Contrato de API em PT-BR via gateway (`docs/contract.gap.md` §5.5 — namespace produzido).

## Routes (12 telas)
- `/(app)/producao` — **Resumo** (KPIs, setores 5S, alertas, kanban mini, ranking mini, máquinas mini)
- `/(app)/producao/5s/setores` — **Setores 5S**
- `/(app)/producao/5s/auditoria` — **Auditoria 5S**
- `/(app)/producao/5s/pendencias` — **Pendências 5S**
- `/(app)/producao/5s/ranking` — **Ranking 5S**
- `/(app)/producao/5s/advertencias` — **Avisos, advertências e suspensões**
- `/(app)/producao/5s/mesas` — **Mesas de projeto**
- `/(app)/producao/projetos` — **Projetos**
- `/(app)/producao/projetos/[id]` — **Projeto (detalhe — 12ª tela)** 🆕
- `/(app)/producao/tarefas` — **Tarefas (Kanban)**
- `/(app)/producao/maquinas` — **Máquinas**
- `/(app)/producao/maquinas/[id]` — **Máquina (detalhe)**

## Access (RBAC — docs/frontendDatails.md §10)
- **Admin (0):** tudo, incl. advertências/penalidades 5S, suspensão máquina, penalidades
- **Bolsista (1)/Voluntário (2):** vê tudo do Produção; edita **se atribuído** (responsável do setor 5S, tarefa, projeto, máquina responsável pela manutenção)
- **Estagiário (3):** vê se atribuído; **não participa de auditoria 5S** (hide)
- **Recrutando (4):** **não participa do 5S** (hide — nunca disable)
- Regra: **HIDE, nunca disable**; RBAC de escrita via `isResponsavelAtribuido(user, recurso)`

## Decisões PO v2 (source: instrunctions.md + requisitos.md §5)
- **A1 Setores:** modal-setor vira criar/editar (título CTA dinâmicos); checklist 5S **travado** (itens específicos editáveis — adicionar/renomear/remover), **sem reordenação e sem peso**; papel no modal: Admin salva direto, Responsável/Auditor → "Enviar p/ aprovação"
- **A2 Auditoria:** **sem input manual de nota** — prévia automática (conformes ÷ total; verde ≥90 · vermelho <70 · senão amarelo); backend **recalcula/valida** em `PUT /inspecoes-5s/{id}/concluir` (nota 1h + cartão automáticos)
- **A3 Pendências:** modal-detalhe-pendencia (origem, itens não conformes, anexos, prazo, situação, penalidade sugerida, histórico) + resolver + registrar penalidade
- **A4 Projeto (detalhe, 12ª tela):** abas `?tab=geral|documentacao|materiais|tarefas`; documentação (upload/link) + BOM/materiais com **saída vinculada ao projeto** (→ modal-saida-projeto) → 🟡
- **A5 Tarefas:** modal-detalhe-tarefa ganha **descrição + checklist** (x/y + barra %); **status permanece manual** (checklist NÃO altera status)

## Data Requirements (assumidos 🟡 — contrato pendente, ver contract.gap §5.5)
- `GET /api/producao/resumo` · `GET /api/producao/projetos?status&responsavel&page` (+ `POST`, `/{id}`, `GET`) 
- `GET /api/producao/projetos/{id}/documentos` 🟡 · `POST /{id}/documentos` 🟡 · `GET /{id}/materiais` 🟡 · `POST /{id}/materiais/saida` 🟡
- `GET /api/producao/tarefas?projetoId&status&prioridade&responsavel` · `POST` · `PUT /{id}` · `PATCH /{id}/mover` · `/{id}/apontar-horas` · `/{id}/atribuir`
- `GET /api/producao/kanban?tab&status` · `PATCH /{id}/mover`
- `GET /api/producao/maquinas?page&search&categoria` (+ `/{id}`, `POST`, `PUT /{id}/status`) · `POST /{id}/uso` · `POST /{id}/chamados` · `GET /{id}/historico`
- `GET /api/producao/5s/setores` (+ `/{id}`, `PUT`) · `PUT /{id}/responsaveis` · `PUT /{id}/checklist`
- `GET /api/producao/inspecoes-5s?status&setor&page` · `PUT /{id}/concluir` (nota recalculada backend) 🟡
- `GET /api/producao/pendencias-5s?gravidade&setor&situacao` · `PUT /{id}/resolver`
- `GET /api/producao/ranking-5s?mes` · `GET /api/producao/penalidades-5s?tipo&situacao` · `POST` (Admin)
- `GET /api/producao/mesas?mes&situacao` · `POST` · `PUT /{id}/evolucao` · `POST /auditorias-mesa`
- `GET/PUT /api/producao/parametros-5s` (rotacaoDias, nota p/ metade, ms prazo, nota p/ cartao verde…)

## RBAC
| Recurso | Admin | Bolsista | Voluntário | Estagiário | Recrutando |
|---|---|---|---|---|---|
| Resumo | ver | ver | ver | ver (se atribuído) | ver |
| Tarefas/Projetos | editar tudo | editar se atribuído | editar se atribuído | ver | ver |
| 5S setores/auditoria | editar (Admin direto) | editar se resp./auditor | editar se resp./auditor | **HIDE** | **HIDE** |
| 5S advertências | editar | — | — | HIDE | HIDE |
| 5S mesas | editar | editar | editar | — | — |
| Máquinas | tudo | registra uso | registra uso | ver | ver |

## States
Loading (skeleton por bloco) · Success · Empty (CTA por bloco) · Empty filtrado (chips) · Error (banner + retry, **parcial por bloco** no resumo/detalhe) · RBAC-restricted (hide). Kanban: drag sem estado de rede (otimista + rollback); fundo bloqueante.

## URL State
- Resumo: `?tab&status` (kanban mini) · Ranking: `?mes=YYYY-MM`
- Projetos: `?status=&responsavel=&page` · Projeto: `?tab=geral|documentacao|materiais|tarefas`
- Tarefas: `?projetoId=&status=&prioridade=&responsavel=` · Kanban: `?tab=&status=`
- Máquinas: `?page=&search=&categoria=&cartao=` · Máquina: `?tab=geral|documentacao|receitas|preflight|manut|chamados`
- Pendências: `?gravidade=&setor=&situacao=` · Advertências: `?tipo=&situacao=` · Mesas: `?mes=&situacao=&search=`

## Mockup
- `docs/mokups/producao/full.html` (12 telas) + `instrunctions.md` (Prompts 0–12) + `requisitos.md` + `verify-prod.js` (12 telas · 24 modais · tag balance)
- data-testid: `set-*`, `aud-*`, `pend-*`, `pen-*`, `rank-*`, `mesa-*`, `proj-*`, `tar-*`, `maq-*`, `prd-*`; guarda `redirect(303,'/dashboard')`

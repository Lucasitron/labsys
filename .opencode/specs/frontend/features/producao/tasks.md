# Tasks — Produção & Projetos (frontend v2 · 12 telas)

> Derivado de `requirements.md` (v2) + `plan.md` (v2) desta pasta. **1 arquivo ≡ 1 task** · ordem:
> types → API → components → rotas (pai→filho) → states → RBAC (menu.ts/permissions.ts) → testes.
> **Legenda:** 🟡 = contrato pendente (assumido — ver plan §5.5) · ⚠️ = arquivo compartilhado (menu.ts/permissions.ts —
> **NUNCA em paralelo com configracoes**; sequencial).
> **Regra global:** TS strict · pt-BR · `data-testid` pt-BR (`proj-*`,`tar-*`,`maq-*`,`s5-*`,`set-*`,`aud-*`,`pend-*`,
> `pen-*`,`rank-*`,`mesa-*`,`prd-*`) · **RBAC: HIDE nunca disable** · escrita sem atribuição → `redirect(303,'/producao')` ·
> **5S HIDE p/ Recrutando e Estagiário** · advertência 5S → Admin only (HIDE).

---

## 1. Fundação — types + utils

### prd-001 · types producao
- **Arquivo:** `src/lib/types/producao.ts` — create
- **Conteúdo:** `Projeto` · `Tarefa` · `KanbanItem` · `Maquina` · `Setor5S` · `Inspecao5S` · `Pendencia5S` ·
  `Penalidade5S` · `Mesa5S` · `RankingItem` · `Parametro5S` · `ChamadoMaquina` · `Preventiva` · `ProjetoDocumento` ·
  `MaterialProjeto` · `SaidaMaterial` · `ResumoProducao` · `KanbanColuna` = union `'Fila'|'Produção'|'Acabamento'|'Pronto'|'Entregue'` ·
  `Cartao5S` = union `'Verde'|'Amarelo'|'Vermelho'` · `SituacaoMesas`. Status/prioridade como Union literais pt-BR
  (espelham `statusMeta` do mockup §8). Paginação genérica `Paginado<T>`.
- **DoD:** sem `any`; tipos casam com `Module='producao'` e `Role` de `types/auth.ts`; sem lógica de nota no client.
- **Deps:** — · **Prioridade:** Alta

### prd-002 · utils producao-status
- **Arquivo:** `src/lib/utils/producao-status.ts` — create
- **Conteúdo:** `statusMeta` maps (projeto · tarefa · kanban · máquina · chamado · cartão · pendência · penalidade ·
  mesa) → `label`/`color` (Verde/Amarelo/Vermelho/Muted · via tokens §design-system) + helper `notaToCartao(nota)`
  (≥90 Verde · <70 Vermelho · senão Amarelo) + `gravidadeMeta`. **Nota/cartão NUNCA calculados no client**
  — só exibe o que a API devolve (backend recalcula).
- **DoD:** mesmas faixas do `verify-prod.js`; exporta `data-testid`-friendly labels.
- **Deps:** prd-001 · **Prioridade:** Alta

---

## 2. API layer (todos 🟡 assumidos — ver requirements §4 + plan §5)

> Cada módulo usa `apiFetch`/`ApiError` de `client.ts`; RBAC 401 → logout; mock local enquanto gateway não expõe;
> reportar no relatório (nunca mockar no client final sem nota).

### prd-003 · api client compartilhado
- **Arquivo:** `src/lib/api/producao/client.ts` — create 🟡
- **Conteúdo:** reexporta `obterResumo`, `obterProjetos`, `obterTarefas`, `obterMaquinas`, `obterKanban` + helpers
  de paginação/filtro pt-BR (agrega os módulos abaixo).
- **DoD:** nenhuma lógica de domínio (só rede + estado RBAC); exporta `Paginado<T>`.
- **Deps:** prd-001 · **Prioridade:** Alta

### prd-004 · api resumo
- **Arquivo:** `src/lib/api/producao/resumo.ts` — create 🟡
- **Conteúdo:** `GET /api/producao/resumo` → `{ kpis, alertas, rankingTop, setores, maquinas }`.
- **DoD:** mock local até gateway; estado Loading/Error/Empty sane.
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-005 · api projetos
- **Arquivo:** `src/lib/api/producao/projetos.ts` — create 🟡
- **Conteúdo:** `GET /projetos?status&responsavel&page` · `GET /{id}` · `POST` · `PUT /{id}` · `GET /{id}/documentos` 🟡 ·
  `POST /{id}/documentos` 🟡 · `GET /{id}/materiais` 🟡 · `POST /{id}/materiais/saida` 🟡.
- **DoD:** caminhos pt-BR espelhados do plan §5.5; estados Loading/Error por bloco.
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-006 · api tarefas
- **Arquivo:** `src/lib/api/producao/tarefas.ts` — create 🟡
- **Conteúdo:** `GET /tarefas?projetoId&status&prioridade&responsavel&page` · `POST` · `PUT /{id}` · `PATCH /{id}/mover` ·
  `PATCH /{id}/apontar-horas` · `PATCH /{id}/atribuir`.
- **DoD:** Kanban move otimista com rollback; bloqueio por pendência grave vem da api (não client).
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-007 · api kanban
- **Arquivo:** `src/lib/api/producao/kanban.ts` — create 🟡
- **Conteúdo:** `GET /kanban?tab&status` · `PATCH /{id}/mover` (coluna alvo) — kanban de produção (encomendas).
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-008 · api máquinas
- **Arquivo:** `src/lib/api/producao/maquinas.ts` — create 🟡
- **Conteúdo:** `GET /maquinas?page&search&categoria&cartao` · `GET /{id}` · `POST` · `PUT /{id}/status` ·
  `POST /{id}/uso` · `POST /{id}/chamados` · `GET /{id}/preventivas` · `POST /{id}/preventivas` ·
  `POST /{id}/historico`·`GET /{id}/chamados`.
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-009 · api 5s setores
- **Arquivo:** `src/lib/api/producao/setores-5s.ts` — create 🟡
- **Conteúdo:** `GET /setores-5s?ciclo&page` · `GET /{id}` · `PUT /{id}` · `PUT /{id}/responsaveis` ·
  `PUT /{id}/checklist` · `POST /{id}/aprovar-alteracao`.
- **DoD:** A1 — checklist 5S **travado** (sem ordem/peso); seletor de papel no modal (Admin salva direto;
  Responsável/Auditor → Enviar p/ aprovação).
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-010 · api 5s inspeções
- **Arquivo:** `src/lib/api/producao/inspecoes-5s.ts` — create 🟡
- **Conteúdo:** `GET /inspecoes-5s?status&setor&auditor&page` · `POST` · `GET /{id}` · `PUT /{id}/concluir` ·
  `POST /{id}/nomear-auditor`.
- **DoD:** A2 — **sem nota manual**; prévia automática client (conformes ÷ total) + backend recalcula/valida em
  `PUT /{id}/concluir` (nota + cartão + 1h automática).
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-011 · api 5s pendências
- **Arquivo:** `src/lib/api/producao/pendencias-5s.ts` — create 🟡
- **Conteúdo:** `GET /pendencias-5s?gravidade&setor&situacao&page` · `GET /{id}` · `PUT /{id}/resolver` ·
  `POST /{id}/penalidade`.
- **DoD:** A3 — modal-detalhe-pendencia (origem, checklist não conforme, anexos, prazo, situação, penalidade
  sugerida, histórico) + resolver + registrar penalidade (Admin).
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-012 · api 5s ranking
- **Arquivo:** `src/lib/api/producao/ranking-5s.ts` — create 🟡
- **Conteúdo:** `GET /ranking-5s?mes` → `{ ranking, mediaGeral }` · `GET /ranking-5s/evolucao?setor&mes`.
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-013 · api 5s penalidades
- **Arquivo:** `src/lib/api/producao/penalidades-5s.ts` — create 🟡
- **Conteúdo:** `GET /penalidades-5s?tipo&situacao&page` · `POST` (Admin) · `PUT /{id}/cumprida`.
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-014 · api mesas
- **Arquivo:** `src/lib/api/producao/mesas.ts` — create 🟡
- **Conteúdo:** `GET /mesas?mes&situacao&search&page` · `POST` · `GET /{id}` · `PUT /{id}/evolucao` ·
  `POST /{id}/auditoria`.
- **Deps:** prd-003 · **Prioridade:** Alta

### prd-015 · api parâmetros 5S
- **Arquivo:** `src/lib/api/producao/parametros-5s.ts` — create 🟡 ⚠️
- **Conteúdo:** `GET/PUT /parametros-5s` (rotação dias · dias p/ auditoria projeto · dia semana inspeção ·
  período experimental · nota cartão verde/amarelo · prazo pendência grave).
- **DoD:** 🟡 overlap com configurações/sistema (cadência 5S compartilhada — endpoint único) · **NUNCA paralelo**.
- **Deps:** prd-003 · **Prioridade:** Média

---

## 3. Componentes compartilhados do módulo

### prd-016 · CartaoBadge
- **Arquivo:** `src/lib/components/producao/CartaoBadge.svelte` — create
- **Conteúdo:** badge cartão 5S (Verde/Amarelo/Vermelho) com cor de token; `data-testid="prd-cartao-{cor}"`.
- **DoD:** reuso em setores/auditoria/ranking/mesas; pt-BR.
- **Deps:** prd-002 · **Prioridade:** Alta

### prd-017 · ModalAuditoria (concluir)
- **Arquivo:** `src/lib/components/producao/ModalAuditoria.svelte` — create
- **Conteúdo:** checklist 5S com Conforme/Não conforme por item + **prévia de nota automática** (x/y · % · faixa
  cartão) — sem input manual; CTA `Concluir auditoria` → `PUT /inspecoes-5s/{id}/concluir` (backend recalcula).
- **DoD:** dados A2; `data-testid="aud-concluir"`; nota nunca escrita no client.
- **Deps:** prd-010 · **Prioridade:** Alta

### prd-018 · ModalSetor
- **Arquivo:** `src/lib/components/producao/ModalSetor.svelte` — create
- **Conteúdo:** criar/editar setor (A1): Nome · Ciclo (radio semanal/quinzenal/mensal) · Responsáveis (multi) ·
  Auditor · seletor de papel (Admin → salva direto; Responsável/Auditor → Enviar p/ aprovação).
- **DoD:** CTA/título dinâmicos criar/editar; `data-testid="set-criar"`, "set-salvar".
- **Deps:** prd-009, prd-019 · **Prioridade:** Alta

### prd-019 · ModalChecklistSetor
- **Arquivo:** `src/lib/components/producao/ModalChecklistSetor.svelte` — create
- **Conteúdo:** checklist 5S do setor: 5S **travado**; itens específicos **editáveis** (adicionar/renomear/remover),
  **sem reordenação e sem peso** (1 item = 1 ponto) + nota + seletor de papel (Admin salva / outros aprovam).
- **DoD:** A1; `data-testid="set-checklist-save"`, "set-checklist-enviar".
- **Deps:** prd-009 · **Prioridade:** Alta

### prd-020 · ModalDetalhePendencia
- **Arquivo:** `src/lib/components/producao/ModalDetalhePendencia.svelte` — create
- **Conteúdo:** A3 — origem (auditoria/auditor/setor) · itens não conformes · anexos · prazo (danger se vencido) ·
  gravidade badge · situação · penalidade sugerida · histórico; ações Resolver / Registrar penalidade (Admin).
- **DoD:** `data-testid="pend-resolver"`, "pend-penalidade".
- **Deps:** prd-011 · **Prioridade:** Alta

### prd-021 · ModalMesa
- **Arquivo:** `src/lib/components/producao/ModalMesa.svelte` — create
- **Conteúdo:** criar mesa de projeto: Projeto universitário (select) · Membro responsável · Período experimental
  (checkbox) · QR totem (gerado) → POST /mesas.
- **DoD:** `data-testid="mesa-criar"`, "mesa-save".
- **Deps:** prd-014 · **Prioridade:** Alta

### prd-022 · ModalNovaTarefa
- **Arquivo:** `src/lib/components/producao/ModalNovaTarefa.svelte` — create
- **Conteúdo:** Título · Projeto (select) · Responsável · Prioridade (radio) · Prazo · Kanban coluna inicial ·
  Descrição · Checklist → POST /tarefas.
- **DoD:** reuso em Tarefas e Projeto (aba tarefas); `data-testid="tar-nova"`.
- **Deps:** prd-006 · **Prioridade:** Alta

### prd-023 · ModalNovoProjeto
- **Arquivo:** `src/lib/components/producao/ModalNovoProjeto.svelte` — create
- **Conteúdo:** Nome · Cliente · Responsável · Prazo · Descrição · status inicial Planejado → POST /projetos
  (reuso modo editar preenchido → PUT /{id}).
- **DoD:** `data-testid="proj-novo"`, "proj-save".
- **Deps:** prd-005 · **Prioridade:** Alta

### prd-024 · ModalSaidaProjeto
- **Arquivo:** `src/lib/components/producao/ModalSaidaProjeto.svelte` — create 🟡
- **Conteúdo:** A4 — saída de material: Item (do BOM) · Quantidade (≤ disponível) · Destino (fixo projeto corrente) ·
  nota de baixa automática no Estoque → POST /projetos/{id}/materiais/saida.
- **DoD:** 🟡 contrato pendente (gateway Estoque) — botão registra mas reporta; `data-testid="proj-saida-material"`.
- **Deps:** prd-005 · **Prioridade:** Média

---

## 4. Layout + RBAC (compartilhado — ⚠️ menu.ts/permissions.ts)

### prd-025 · menu.ts — submenu Produção
- **Arquivo:** `src/lib/config/menu.ts` — modify ⚠️
- **Conteúdo:** submenu **Produção** com ~9 itens (Resumo · Projetos · Tarefas · Máquinas · 5S: Setores · Auditoria ·
  Pendências · Ranking · Advertências · Mesas). Item **Configurações** permanece.
- **DoD:** ⚠️ **menu.ts compartilhado com configracoes — NUNCA em paralelo**; RBAC HIDE (5S escondido p/ Recrutando/
  Estagiário; Advertências Admin-only hide).
- **Deps:** — · **Prioridade:** Alta

### prd-026 · permissions.ts — RBAC producao
- **Arquivo:** `src/lib/utils/permissions.ts` — modify ⚠️
- **Conteúdo:** `canSee5S(user)` · `isResponsavelAtribuido(user, recurso)` · `canSeeMaquinas` (já existe) ·
  `canEditProducao(user, recurso)` via atribuição; guarda `(app)/producao/+layout.ts` esconde 5S p/ Recrutando/Estagiário
  (redirect `/producao`), Advertências/penalidades → Admin-only HIDE.
- **DoD:** ⚠️ **compartilhado com configracoes — NUNCA em paralelo**; RBAC hide nunca disable.
- **Deps:** prd-025 · **Prioridade:** Alta

### prd-027 · layout producao
- **Arquivo:** `src/routes/(app)/producao/+layout.ts` + `+layout.svelte` — create
- **Conteúdo:** guarda de acesso (login + RBAC producao) + scaffold com submenu Produção ativo.
- **DoD:** RBAC hide por bloco; redirect p/ `/dashboard` se sem acesso ao módulo.
- **Deps:** prd-025, prd-026 · **Prioridade:** Alta

---

## 5. Rotas — telas (pai → filho)

### prd-028 · resumo (raiz /producao)
- **Arquivo:** `src/routes/(app)/producao/+page.ts` + `+page.svelte` — create
- **Conteúdo:** KPIs (projetos ativos · tarefas pendentes · máquinas · sistema 5S) · alertas · ranking top ·
  setores 5S card · mini-kanban encomendas · máquinas mini; atalhos `data-goto`.
- **DoD:** estados Loading/Error parcial por bloco; `data-testid="prd-kpi","prd-alerta","prd-kanban"`.
- **Deps:** prd-004, prd-027 · **Prioridade:** Alta

### prd-029 · setores 5S
- **Arquivo:** `src/routes/(app)/producao/5s/setores/+page.ts` + `+page.svelte` — create
- **Conteúdo:** card grid setores (ciclo badge · responsáveis avatar · CartaoBadge + nota · próxima auditoria) ·
  KPIs (4) · filtros (pesquisa · cartão · responsável) · ações Novo setor / Configurar 5S (Admin) · ✎ editar.
- **DoD:** A1; `data-testid="set-card","set-novo","set-config"`.
- **Deps:** prd-009, prd-018, prd-027 · **Prioridade:** Alta

### prd-030 · auditoria 5S
- **Arquivo:** `src/routes/(app)/producao/5s/auditoria/+page.ts` + `+page.svelte` — create
- **Conteúdo:** KPI (auditorias do mês · hoje · auditores · atrasadas) · filtros (status · setor) · tabela
  (setor · auditor · data · nota badge · ações Auditar / Nomear auditor) · modais concluir + nomear.
- **DoD:** A2; `data-testid="aud-row","aud-concluir","aud-nomear"`.
- **Deps:** prd-010, prd-017, prd-027 · **Prioridade:** Alta

### prd-031 · pendências 5S
- **Arquivo:** `src/routes/(app)/producao/5s/pendencias/+page.ts` + `+page.svelte` — create
- **Conteúdo:** KPIs por gravidade · filtros (gravidade · setor · situação) · tabela (gravidade badge · pendência
  clicável → modal-detalhe-pendencia · responsável · prazo · situação) · card "Como funciona" · ações Resolver /
  Registrar penalidade (Admin).
- **DoD:** A3; `data-testid="pend-row","pend-resolver","pend-penalidade"`.
- **Deps:** prd-011, prd-020, prd-027 · **Prioridade:** Alta

### prd-032 · ranking 5S
- **Arquivo:** `src/routes/(app)/producao/5s/ranking/+page.ts` + `+page.svelte` — create
- **Conteúdo:** seletor mês · NOTA REGRA (Top 1 livre 1 semana) · tabela ranking (posição · responsável · nota ·
  CartaoBadge · cartão) · evolução das notas (barras por setor/mês, sem lib chart) · metas XP/faixas.
- **DoD:** `data-testid="rank-row","rank-top1"`; nota vem da API (nunca client).
- **Deps:** prd-012, prd-027 · **Prioridade:** Alta

### prd-033 · advertências 5S
- **Arquivo:** `src/routes/(app)/producao/5s/advertencias/+page.ts` + `+page.svelte` — create
- **Conteúdo:** KPIs (avisos · advertências · suspensões · expulsões) · filtros (tipo · situação) · tabela
  (membro · tipo badge · data · motivo · status) · card ESCALA adversarial · ação Registrar penalidade (Admin).
- **DoD:** Admin-only (HIDE p/ demais); `data-testid="pen-row","pen-registrar"`.
- **Deps:** prd-013, prd-027 · **Prioridade:** Média

### prd-034 · mesas de projeto
- **Arquivo:** `src/routes/(app)/producao/5s/mesas/+page.ts` + `+page.svelte` — create
- **Conteúdo:** KPIs (mesas · em uso · pendentes · auditorias) · filtros · grid cards (mesa · projeto · membro ·
  período experimental chip · QR totem · status · última auditoria) · ação Auditar mesa / Vincular projeto.
- **DoD:** `data-testid="mesa-card","mesa-auditar","mesa-vincular"`.
- **Deps:** prd-014, prd-021, prd-027 · **Prioridade:** Alta

### prd-035 · projetos (lista)
- **Arquivo:** `src/routes/(app)/producao/projetos/+page.ts` + `+page.svelte` — create
- **Conteúdo:** KPIs (ativos · em andamento · concluídos) · filtros (status · responsável) · grid cards genérico
  (código mono · status badge · responsável · progresso barra · fim previsto) · modal-novo-projeto.
- **DoD:** linha → detalhe `[id]`; `data-testid="proj-card","proj-novo"`.
- **Deps:** prd-005, prd-023, prd-027 · **Prioridade:** Alta

### prd-036 · projeto detalhe [id]
- **Arquivo:** `src/routes/(app)/producao/projetos/[id]/+page.ts` + `+page.svelte` — create 🟡
- **Conteúdo:** abas `?tab=geral|documentacao|materiais|tarefas` · documentos (upload + links + galeria) ·
  materiais (BOM) · tarefas do projeto · modal-saida-projeto 🟡 · modal-novo-projeto (editar).
- **DoD:** A4 — 12ª tela; `data-testid="proj-detail","proj-doc","proj-materiais"`.
- **Deps:** prd-005, prd-024, prd-006, prd-027 · **Prioridade:** Alta

### prd-037 · tarefas (kanban)
- **Arquivo:** `src/routes/(app)/producao/tarefas/+page.ts` + `+page.svelte` — create
- **Conteúdo:** Kanban 5 colunas com `?tab&status` · filtros (status · prioridade · responsável) · modal-nova-tarefa ·
  modal-detalhe-tarefa (descrição + checklist x/y + barra %; **status manual** · sem peso) · modal-atribuir ·
  modal-resolver-pendencia (se bloqueada).
- **DoD:** A5; cards com papel → kanban não-editável (hide ação); `data-testid="tar-row","tar-nova","tar-card"`.
- **Deps:** prd-006, prd-022, prd-027 · **Prioridade:** Alta

### prd-038 · máquinas (lista)
- **Arquivo:** `src/routes/(app)/producao/maquinas/+page.ts` + `+page.svelte` — create
- **Conteúdo:** KPIs (cadastradas · cartões verdes · em manutenção · atrasada) · filtros (pesquisa · cartão ·
  categoria) · grid cards (código · CartaoBadge · nome · categoria · responsável manutenção · status) ·
  modal-maquina.
- **DoD:** card → detalhe `[id]`; `data-testid="maq-card","maq-nova"`.
- **Deps:** prd-008, prd-027 · **Prioridade:** Alta

### prd-039 · máquina detalhe [id]
- **Arquivo:** `src/routes/(app)/producao/maquinas/[id]/+page.ts` + `+page.svelte` — create
- **Conteúdo:** abas `?tab=geral|documentacao|receitas|preflight|manutencao|chamados` · preventivas (calendário) ·
  chamados (status + ação Solicitar compra) · modal-chamado · modal-preventiva · modal-relatorio-manutencao ·
  modal-compra · modal-suspender/liberar.
- **DoD:** `data-testid="maq-tab-geral","maq-chamado","maq-suspender"`.
- **Deps:** prd-008, prd-027 · **Prioridade:** Alta

---

## 6. Testes

### prd-040 · unit producao-status
- **Arquivo:** `frontend/tests/unit/producao-status.test.ts` — create
- **Conteúdo:** faixas `notaToCartao` (≥90 · <70 · meio) + RBF export + statusMeta chaves.
- **DoD:** vitest verde.
- **Deps:** prd-002 · **Prioridade:** Média

### prd-041 · unit permissions (producao)
- **Arquivo:** `frontend/tests/unit/permissions.test.ts` — modify
- **Conteúdo:** casos `canSee5S` (hide Recrutando/Estagiário) · `isResponsavelAtribuido` · RBAC producao matrix.
- **DoD:** vitest verde.
- **Deps:** prd-026 · **Prioridade:** Média

### prd-042 · e2e happy path producao
- **Arquivo:** `frontend/tests/e2e/producao.spec.ts` — create (se Playwright disponível — não confirmado)
- **Conteúdo:** navega resumo → projetos → detalhe → tarefas → máquinas com RBAC Admin; guarda Recrutando.
- **DoD:** verdes se suite e2e existir; senão reportar como skip.

---

## 7. Total
**42 tasks** · **Cria:** 40 · **Modify:** 2 (menu.ts, permissions.ts — ⚠️ sequenciais com configracoes) ·
**🟡 contratos pendentes:** prd-005(3) · prd-024 · prd-036(docs/materiais) — ver requirements §4.

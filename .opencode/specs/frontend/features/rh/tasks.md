# Tasks — Pessoas & RH (frontend, 12 telas)

> Derivado de `requirements.md` + `plan.md` desta pasta + mockup `docs/mokups/pessoas/` (`full.html`, `novasTelasEstados.html`, `requisitos.md` v0.4, `instrunctions.md`).
> Cada task = **1 arquivo real**. Execução por blocos (deps abaixo); B3/B4 paralelizáveis entre si após B2.
> Legenda: 🟡 contrato assumido (sem evidência de backend/gateway) · 🔴 contrato inexistente/divergente (requer decisão D-x) · ⚠️ arquivo compartilhado — **sequencial**.
> Princípio código mínimo: reuso > create (kit 22 + `format.ts` + `PeoplePicker` + stores); nada especulativo; rotas canônicas `/pessoas/**`.
> Critério global: pt-BR; Svelte 5 runes; TS estrito (sem `any`); `data-testid` kebab-case do mockup; URL fonte de verdade; RBAC **HIDE**; debounce 300ms + `AbortController`; `Intl` pt-BR; sem `{@html}`; sem `console.log`.

---

## B1 — Fundação (types + API + utils + UI + RBAC)

### rh-001 · types do módulo
- **Arquivo:** `src/lib/types/rh.ts` — create
- **Conteúdo:** `PersonType='funcionario'|'bolsista'|'voluntario'|'estagiario'`; `PersonStatus='ativo'|'inativo'|'afastado'`; `Nivel='admin'|'bolsista'|'voluntario'|'estagiario'|'recrutando'`; `PsStage='inscrito'|'triagem'|'entrevista'|'aprovado'|'reprovado'`; `HoraStatus='pendente'|'validada'|'rejeitada'`; `TrainingStatus='aberto'|'agendado'|'solicitado'|'pendente'|'em_andamento'|'concluido'`; `GuideStatus='validado'|'em_revisao'|'rascunho'`. Modelos: `Person` (id, matricula, name, type, role/instrutor, status, group, email, phone, initials, joinedAt…), `PersonDetail` (+kpis/specs/qualification), `PsGroup` (id, code, name, stage, membersCount, candidates[]), `PsEvaluation {nota, feedback}`, `HoraApontamento` + `ExtratoMensalHoras` (Dívida 4 — só tipa o que o backend serve, nunca soma), `Treinamento` (+agenda/evaluation/finalTask), `Guia`, `InstrutorDisponibilidade`, `NivelMatrix`, `Paged<T>` genérico, `RhListParams` (search/setor/nivel/status/page). Papel `tutor` em PS vs `instrutor` em treinamentos (§6 plan).
- **Saída:** tipos que alimentam as 5 APIs + 12 telas + testes.
- **DoD:** sem `any`; `import type` nos consumidores; compila (`svelte-check`).
- **Deps:** — · **Prioridade:** Alta

### rh-002 · API pessoas (+ compat legado)
- **Arquivo:** `src/lib/api/rh/pessoas.ts` — create 🟡/🔴
- **Conteúdo:** helper `bearer()` local + `apiFetch` de `client.ts`; `listPessoas({search,setor,nivel,status,page,pageSize}) → GET /api/rh/pessoas` 🔴 (só `GET /pessoas/{id}` existe — D-1/D-2) · `getPessoa(id) → GET /api/rh/pessoas/{id}` 🟡 · `createPessoa(payload) → POST /api/rh/pessoas` 🟡 (vínculo `POST /api/auth/users` a confirmar D-2) · `updatePessoa(id,payload) → PUT /api/rh/pessoas/{id}` 🟡 · `searchPeople(search)` re-export do legado `api/rh.ts` (mantém estoque funcionando; path EN congelado, sem novos paths EN). Query montada só com params não-nulos (`buildQuery`-like local ou `URLSearchParams`).
- **Saída:** camada das telas 1–4 + `PeoplePicker` dos modais.
- **DoD:** nenhum path `/rh/people` novo; erros como `ApiError` (409 → inline na tela); contrato 🔴/🟡 reportado no relatório do bloco.
- **Deps:** rh-001 · **Prioridade:** Alta

### rh-003 · API processo seletivo
- **Arquivo:** `src/lib/api/rh/processo-seletivo.ts` — create 🔴
- **Conteúdo:** `listPS({estagio}) → GET /api/rh/processo-seletivo` 🔴 · `createGrupo({name, leaderId/tutorId, memberIds}) → POST /api/rh/processo-seletivo/grupos` 🔴 (usa `tutorId`, nunca `instrutor` — §6) · `moverEstagio(id, estagio) → PATCH /api/rh/processo-seletivo/{id}/estagio` 🔴 · `avaliarMembro(id, pessoaId, {nota, feedback, resultado}) → POST …/{id}/membros/{pessoaId}/avaliar` 🔴. Tudo via `apiFetch`.
- **Saída:** camada da tela 5 (Kanban por grupos + avaliação individual).
- **DoD:** DTOs `PsGroup/PsEvaluation` de rh-001; D-4 registrado.
- **Deps:** rh-001 · **Prioridade:** Alta

### rh-004 · API horas (modelo duplo, extrato backend)
- **Arquivo:** `src/lib/api/rh/horas.ts` — create 🔴/🟡
- **Conteúdo:** legado `listHoras({status, periodo}) → GET /api/rh/horas` 🔴 · `registrarHoras(payload) → POST /api/rh/horas` 🔴 · `validarHoras(id) → PATCH …/{id}/validar` 🔴 · `rejeitarHoras(id,{reason}) → PATCH …/{id}/rejeitar` 🔴 **+** nova modelagem `getHorasDisponiveis() → GET /api/rh/horas/disponiveis` 🟡 · `listApontamentos(params) → GET /api/rh/apontamentos-horas` 🟡 · `createApontamento(payload) → POST` 🟡 · `validarApontamento(id) → PUT …/{id}/validar` 🟡. **Nenhuma função de soma/total no client** (R-5: KPIs vêm de `ExtratoMensalHorasEvent`/counts).
- **Saída:** camada da tela 6 + aba Horas do detalhe.
- **DoD:** export sem `somar|totalizar|consolidar`; D-5 registrado.
- **Deps:** rh-001 · **Prioridade:** Alta

### rh-005 · API treinamentos (+ agenda/guias/sessão)
- **Arquivo:** `src/lib/api/rh/treinamentos.ts` — create 🔴
- **Conteúdo:** `listTreinamentos({status,search}) → GET /api/rh/treinamentos` 🔴 · `getTreinamento(id) → GET …/{id}` 🔴 · `getSessao(id) → GET …/{id}/sessao` 🔴 · `avaliarTreinamento(id,{nota,feedback}) → POST …/{id}/avaliar` 🔴 · `atribuirTarefaFinal(id,payload) → POST …/{id}/tarefa-final` 🔴 · `listAgenda({tab})` / `getDisponibilidade()` / `listGuias({maquinaId})` 🔴 (agregados da mesma base; integrações `projects/machines/tasks` 🔴 D-9). Via `apiFetch`.
- **Saída:** camada das telas 7–11.
- **DoD:** campo `instrutor` (nunca `tutor`) nos DTOs; D-9 registrado.
- **Deps:** rh-001 · **Prioridade:** Alta

### rh-006 · API níveis
- **Arquivo:** `src/lib/api/rh/niveis.ts` — create 🔴 parcial
- **Conteúdo:** `getNiveis() → GET /api/rh/niveis` 🔴 (só `PUT /funcionarios/{id}/nivel` existe) · `alterarNivel(membroId, nivel) → PATCH /api/rh/niveis/{id}/membros` 🔴 · `convidarUsuario(payload) → POST` 🟡 (a confirmar D-8). Matriz tratada como **autoritativa** (nunca derivada no client).
- **Saída:** camada da tela 12.
- **DoD:** D-8 registrado; sem cálculo de permissão no client.
- **Deps:** rh-001 · **Prioridade:** Média

### rh-007 · util de status canônicos
- **Arquivo:** `src/lib/utils/rh-status.ts` — create
- **Conteúdo:** `personStatusMeta` (ativo/success, inativo/muted, afastado/warn) · `psStageMeta` (inscrito/muted, triagem/brand, entrevista/warn, aprovado/success, reprovado/danger) · `horaStatusMeta` (pendente/warn, validada/success, rejeitada/danger) · `trainingStatusMeta` (aberto+agendado/success, solicitado+em_andamento/brand, pendente/warn, concluido/muted) · `disponibilidadeMeta` (livre/success, ocupada/warn, indisponivel/danger) · `nivelMeta` (admin/danger, bolsista/brand, voluntario/success, estagiario/warn, recrutando/muted). Formato `{label, color}` (padrão `stock-status.ts`). **REUSAR** `initials/relativeTime/formatNumber` de `format.ts` (não duplicar).
- **Saída:** cores/labels das 12 telas.
- **DoD:** cobertura total das unions de rh-001; teste em rh-027.
- **Deps:** rh-001 · **Prioridade:** Alta

### rh-008 · UI `TypeBadge`
- **Arquivo:** `src/lib/components/ui/TypeBadge.svelte` — create
- **Conteúdo:** `$props { tone: 'brand'|'success'|'warn'|'danger'|'muted', label }`; badge semântica de tipo/motivo (Encomenda/Projeto, trilhas, máquina). Distinto de `StatusBadge` (dot+status). pt-BR, `aria-label`.
- **Saída:** badges de horas/treinamentos/documentação.
- **DoD:** sem quebrar `StatusBadge`; type-check ok.
- **Deps:** — · **Prioridade:** Média

### rh-009 · UI `DonutChart` (kit compartilhado)
- **Arquivo:** `src/lib/components/ui/DonutChart.svelte` — create
- **Conteúdo:** `$props { total, segments: {value, color}[], label? }`; SVG `viewBox="0 0 42 42"`, trilha `r=15.9 stroke #1a2235`, fatias `stroke-dasharray="{pct} {rest}"` + `stroke-dashoffset` acumulado negativo, overlay central `{x}/{total}` + legenda. Mesmo contrato do `dashboard/components/DonutChart.svelte` (não tocar o do dashboard).
- **Saída:** donuts de treinamentos por grupo (tela 7).
- **DoD:** 0/0 sem `NaN` (empty); a11y (`role="img"` + `aria-label`).
- **Deps:** — · **Prioridade:** Média

### rh-010 · UI `KanbanBoard` (sem drag-lib)
- **Arquivo:** `src/lib/components/ui/KanbanBoard.svelte` — create
- **Conteúdo:** `$props { columns: {id, label, tone, count, cards}[] }`; colunas + cards genéricos via `{#each}` + slots/`children` para ação ("Ver grupo", mover etapa por botões/chips). Sem lib externa; drag opcional não requerido.
- **Saída:** Kanban do processo seletivo (5 estágios).
- **DoD:** teclado operável (ações são botões); sem dependência nova no `package.json`.
- **Deps:** — · **Prioridade:** Média

### rh-011 · RBAC helpers RH ⚠️
- **Arquivo:** `src/lib/utils/permissions.ts` — modify ⚠️
- **Conteúdo:** adicionar **sem alterar** `VIEW_RULES`/`EDIT_RULES`: `isResponsavelRH(user)` (responsibilities.rh não vazio, `role!==4`) · `isTutor(user)` (= responsável RH de grupo/PS) · `isInstrutor(user)` (= responsibility/flag de instrutor ou Admin) · `canEditRH(user, recurso?)` = `isAdmin || (role 1–2 && isResponsavelRH)` (+ `roles.rh==='edit'`) · `canSeeNiveis = isAdmin` · `isProprioRegistro(user, pessoaId)` (para Estagiário/Recrutando — D-7). `canEdit(user,'rh')` passa a incluir responsável (base `EDIT_RULES.rh=[0,1,2]` intacta).
- **Saída:** RBAC das 12 telas + menu + guards.
- **DoD:** ⚠️ **sequencial** (compartilhado com estoque/produção/config já concluídos); base de `permissions.test.ts` verde; casos novos em rh-029.
- **Deps:** — · **Prioridade:** Alta

---

## B2 — Pessoas (lista · detalhe · novo · editar)

### rh-012 · guarda do módulo
- **Arquivo:** `src/routes/(app)/pessoas/+layout.ts` — create
- **Conteúdo:** `canView(user,'rh')` ou redirect `302 /dashboard?denied=` (padrão estoque); retorna `{ canEdit: canEditRH(user) }`; se path `/pessoas/niveis` e `!canSeeNiveis` → `redirect(303,'/pessoas')`. `ssr=false/prerender=false`.
- **Saída:** proteção das 12 rotas (Níveis Admin-only).
- **DoD:** Recrutando fora de gestão redirecionado (nunca 403 em tela); sem duplicar `fetchMe`.
- **Deps:** rh-011 · **Prioridade:** Alta

### rh-013 · Pessoas — lista (tela 1)
- **Arquivo:** `src/routes/(app)/pessoas/+page.svelte` (+ `+page.ts` no mesmo task) — create
- **Conteúdo:** URL `?search=&setor=&nivel=&status=&page=` (fonte de verdade); `SearchInput` debounce 300ms + 3 `Dropdown` (Status/Setor/Nível, AND entre painéis) + `Chip`s + "Limpar filtros"; tabela (Avatar+nome/email, matrícula mono, nível badge, grupo, `StatusBadge`, `RowActions` → Ver/Editar/Excluir) + `BulkActionsBar` (Alterar nível + Excluir com `ConfirmDialog`; **sem** Exportar CSV — R-8) + `Pagination` (10/25/50/100; voltar à última página válida ao excluir); `PageHeader` + "+ Novo cadastro" (hide se `!canEdit`); mobile <768 cards; linha → `/pessoas/{id}` (checkbox/ações com `stopPropagation`). Estados: TableSkeleton/Empty/Empty-filtrado/ErrorBanner+retry/RBAC (grids sem checkbox/ações). Endpoints: `listPessoas` 🔴.
- **Saída:** rota `/pessoas`.
- **DoD:** `data-testid="person-row/person-checkbox/select-all/bulk-bar"`; paginação client ou server conforme D-2 (documentar no relatório).
- **Deps:** rh-002, rh-007, rh-011, rh-012 · **Prioridade:** Alta

### rh-014 · Pessoa — detalhe (tela 2)
- **Arquivo:** `src/routes/(app)/pessoas/[id]/+page.svelte` (+loader) — create
- **Conteúdo:** voltar (preserva filtros) + "Editar" (`data-testid="edit-person-button"` → tela 3, hide se `!canEdit`) + ⋮; hero (Avatar 12, nome, matrícula mono, `grupo · nível · status`, `StatusBadge` grande + badge "Instrutor(a) · {qualificação}"); KPIs (Horas mês via **extrato backend** R-5, Treinamentos média, Pendências, Projetos ativos); `Tabs` (`?tab=` visão-geral/horas/treinamentos/histórico, `role=tablist`): visão (`<dl>` specs + 6 chips qualificação + responsabilidades), horas (sub-tabs + tabela + motivo), treinamentos (trilhas + nota + guia), histórico-nível (timeline `recrutando → …` + autor + data). Erro parcial por aba (estado #6); 404 → EmptyState + voltar. Endpoints: `getPessoa` 🟡 + abas 🟡 (D-2).
- **Saída:** rota `/pessoas/[id]`.
- **DoD:** tabs deep-linkáveis; sem soma de horas no client.
- **Deps:** rh-002, rh-004, rh-005, rh-007, rh-012 · **Prioridade:** Alta

### rh-015 · Novo cadastro (tela 4)
- **Arquivo:** `src/routes/(app)/pessoas/novo/+page.svelte` — create
- **Conteúdo:** voltar; `PageHeader`; form em cards (Dados pessoais: Nome\*/Email\* 409-inline/Telefone/CPF/Data-nasc; Função: Tipo `RadioCards` Bolsista/Voluntário/Estagiário + checkbox **"É instrutor(a)?"** (`is-instrutor` → 6 chips qualificação) + Nível inicial\*/Grupo\*(G-01..G-05)/Curso-turma; Credenciais: username do email + Senha\* mascarada + "Gerar"); footer Cancelar/"Salvar e adicionar outro"/"Salvar cadastro" (busy anti-duplo). `POST /api/rh/pessoas` 🟡 (+credencial D-2); sucesso → lista (ou reset no "adicionar outro"). Rota bloqueada p/ view-only/Recrutando (redirect `/pessoas`). `data-testid="person-name/person-email/is-instrutor/submit-person"`.
- **Saída:** rota `/pessoas/novo`.
- **DoD:** validação inline pt-BR + `novalidate`; skeleton nos selects; erro topo em falha.
- **Deps:** rh-002, rh-012 · **Prioridade:** Alta

### rh-016 · Editar pessoa (tela 3)
- **Arquivo:** `src/routes/(app)/pessoas/[id]/editar/+page.svelte` — create
- **Conteúdo:** espelho de `novasTelasEstados.html` + `full.html# pessoa-editar` (`data-testid="edit-person"`): voltar (`edit-back` → detalhe); `PageHeader "Editar pessoa"`; banner `edit-rbac-banner` quando `!canEdit` (sem ações); form (Nome\* `person-name`, Matrícula\* `person-matricula` **read-only**, Admissão `person-admission`, Email\* `person-email` 409-inline, Telefone `person-phone`; Nível `person-level`, Departamento `person-department`, Turno `person-shift`, Status `person-status`, `is-instrutor` → `person-qualification` chips; Credenciais = nota estática — link p/ Níveis); footer `person-cancel`/`person-save` → `PUT /api/rh/pessoas/{id}` 🟡 → detalhe com refetch. Estados: skeleton campos / erro parcial por bloco / RBAC.
- **Saída:** rota `/pessoas/[id]/editar`.
- **DoD:** `data-testid="person-*" kebab-case` conformes (§9 Validator); matrícula sem interação.
- **Deps:** rh-002, rh-012 · **Prioridade:** Alta

---

## B3 — Seleção + Horas

### rh-017 · Processo seletivo — Kanban por grupos (tela 5)
- **Arquivo:** `src/routes/(app)/pessoas/processo-seletivo/+page.svelte` — create
- **Conteúdo:** URL `?estagio=`; header + "Criar grupo" (`ps-criar-grupo`, hide se `!canEdit`) + "Nova inscrição" (cria recrutando via `createPessoa` 🟡) + busca; KPIs (Inscritos/Grupos/Em avaliação/Aprovados de `counts`); strip G-01..G-05 (click → `modal-grupo`); `KanbanBoard` 5 colunas (tones §7.8) com cards de **grupo** (nome, n membros, chips iniciais + dots individuais) + "Ver grupo"; modais `modal-nova-inscricao` (Nome\*/Email\*/Curso\*/Período/Disponibilidade), `modal-criar-grupo` (Nome\* + Líder/**Tutor** + members — rótulo **Tutor(a)**, §6), `modal-grupo` (membros + etapa individual + "Avaliar" → `modal-avaliar-ps`: Aprovado/Reprovado + Nota 0–10 + Feedback → avaliar individual). Mover etapa por botões (sem drag). Endpoints: todos 🔴 (D-4). `data-testid="ps-kpi-grupos/ps-avaliar/ps-criar-grupo"`.
- **Saída:** rota `/pessoas/processo-seletivo`.
- **DoD:** avaliação sempre individual; ações hide se não responsável/tutor; refetch após mover/avaliar.
- **Deps:** rh-003, rh-010, rh-011, rh-012 · **Prioridade:** Alta

### rh-018 · Registro de horas (tela 6)
- **Arquivo:** `src/routes/(app)/pessoas/registro-horas/+page.svelte` — create
- **Conteúdo:** URL `?status=&periodo=`; header + "+ Registrar horas" (modal: Pessoa\* via `PeoplePicker` reusado + Data\*/Início\*/Fim\*/Tipo radio Encomenda/Projeto + Referência search + Obs → `createApontamento` 🟡/legado 🔴); KPIs do **extrato backend** (`getHorasDisponiveis` 🟡 — nunca somar no client, R-5); `Tabs` Pendentes/Validadas/Rejeitadas (counts, `hora-tab-*`); tabela (Pessoa avatar+nome, Data dd/mm, Horário mono, Horas mono, Tipo `TypeBadge`, Referência mono, Status + ação Validar/`hora-validar` / Rejeitar+motivo/`hora-rejeitar`); rejeitada mostra motivo (texto, hover/modal); Estagiário vê só próprias (colunas/ações ocultas — hide). Estados por tab (skeleton/empty/error+retry). Endpoints: legados 🔴 + nova modelagem 🟡 (D-5).
- **Saída:** rota `/pessoas/registro-horas`.
- **DoD:** zero soma client (`rg "reduce.*horas|totalHoras ="` vazio neste arquivo); refetch após validar/rejeitar/registrar.
- **Deps:** rh-004, rh-008, rh-011, rh-012 · **Prioridade:** Alta

---

## B4 — Treinamentos (subsistema + second sidebar)

### rh-019 · Treinamentos — layout second sidebar
- **Arquivo:** `src/routes/(app)/pessoas/treinamentos/+layout.svelte` — create
- **Conteúdo:** painel `w-60 bg-surface border-r shadow-2xl z-30 translateX` sobre a principal (mockup §4.4): header "Treinamentos" + voltar (fecha/sai p/ `/pessoas`); itens Visão geral (`/pessoas/treinamentos`), Agenda, Documentação, Sessão; ativo via `$page.url.pathname`; permanece aberto entre telas do subsistema, fecha fora; toggle da principal com chevron `rotate-180`.
- **Saída:** navegação das telas 7–11.
- **DoD:** sem store de UI; foco gerenciado (Escape fecha); `aria-current` no ativo.
- **Deps:** rh-026 · **Prioridade:** Média

### rh-020 · Treinamentos — visão geral (tela 7)
- **Arquivo:** `src/routes/(app)/pessoas/treinamentos/+page.svelte` — create
- **Conteúdo:** `PageHeader` + "Novo treinamento" (`modal-treinamento`: Título\*/Descrição/**Instrutor**\*/Máquina\*/Duração/Trilha → `POST` 🔴 D-9, hide se `!canEdit`) + "Ver agenda" (→ agenda); busca; cards "em aberto" (`training-card`: título → `[id]`, instrutor avatar, badge status, `{n} concluíram`, grupo + barra); donuts por grupo (`donut-group`, `DonutChart` rh-009: Treinados/success, Em treinamento/brand, Pendentes/warn, centro `{x}/{total}`); paginação (>6). Endpoints: `listTreinamentos` 🔴.
- **Saída:** rota `/pessoas/treinamentos`.
- **DoD:** rótulo **Instrutor(a)** (nunca Tutor aqui); skeleton cards/donuts; empty + error.
- **Deps:** rh-005, rh-009, rh-012, rh-019 · **Prioridade:** Alta

### rh-021 · Treinamento — detalhe (tela 8)
- **Arquivo:** `src/routes/(app)/pessoas/treinamentos/[id]/+page.svelte` — create
- **Conteúdo:** espelho `full.html#treinamento-detalhe` (`training-detail` + variantes `novasTelasEstados.html`): voltar `training-detail-back`; hero (`training-detail-title/machine/instructor`, `StatusBadge`, CTA `training-open-session` → sessão **só com sessão ativa**); KPIs (Concluídos, Média, Presentes `training-detail-session`, Próxima); `training-detail-info` (`<dl>` Trilha/Grupo/Carga/Início/Inscrições/Instrutor); `training-detail-agenda` (Data/Horário·Sessão·Presença·Status); `training-detail-evaluation` (critérios+peso; "Avaliar" → `POST …/avaliar` 🔴 + "Atribuir tarefa final" — hide se `!isInstrutor/canEdit`); tarefa final (título + conclusão). Erro parcial por bloco. Endpoints: `getTreinamento` 🔴 + `getSessao` 🔴 (D-9).
- **Saída:** rota `/pessoas/treinamentos/[id]`.
- **DoD:** `data-testid="training-detail-*" ` conformes; 404 → EmptyState + voltar.
- **Deps:** rh-005, rh-012, rh-019 · **Prioridade:** Alta

### rh-022 · Agenda (tela 9)
- **Arquivo:** `src/routes/(app)/pessoas/treinamentos/agenda/+page.svelte` — create
- **Conteúdo:** header + "Agendar" (`modal-agendar`: Treinamento\*/Grupo\*/**Instrutor**\*/Máquina\*/Data\*/Início\*/Fim\* + bloco auto-disponibilidade verde, hide se `!canEdit`); KPIs (Pendentes/Solicitados/Agendados/Em andamento); tabs Todos/Pendentes/Solicitados/Agendados (URL `?tab=` local desta rota + counts); tabela (Treinamento/Grupo/Instrutor/Máquina/Data-Horário/Status/Ação: Abrir sessão / Confirmar / Definir turma); "Disponibilidade dos instrutores" (Instrutor+matrícula, Máquinas chips, Status máquina, Tarefas hoje `{h}h` — **só** produção/encomendas/projetos, nunca triagens PS §6, `Disponibilidade` Livre/Ocupada/Indisponível + nota de rodapé). Endpoints: agenda/disponibilidade 🔴 (D-9). `data-testid="agenda-tab-solicitados/agenda-instrutor"`.
- **Saída:** rota `/pessoas/treinamentos/agenda`.
- **DoD:** disponibilidade deriva de `tasksTodayHours` (sem inventar triagens).
- **Deps:** rh-005, rh-012, rh-019 · **Prioridade:** Média

### rh-023 · Documentação — guias por máquina (tela 10)
- **Arquivo:** `src/routes/(app)/pessoas/treinamentos/documentacao/+page.svelte` — create
- **Conteúdo:** `PageHeader` + "Novo guia" (hide se `!canEdit`; MVP toast "Em breve" — sem endpoint); `SearchInput` + `Select` máquina; grid cards por máquina (ícone tintado, nome, `{n} guias`; rows `guide-row`: título, trilha·`v{version}`·autor, status Validado/success–Em revisão/brand–Rascunho/warn, atualizado em, `+`); `modal-guia` (chips Máquina/páginas/apoio, prévia placeholder, Baixar PDF / Abrir MDX / Editar — hide editar se `!canEdit`). Só **metadados** (conteúdo PDF/MDX fora de escopo R-8). Endpoints: `listGuias` 🔴 (D-9). `data-testid="guide-row/guide-machine"`.
- **Saída:** rota `/pessoas/treinamentos/documentacao`.
- **DoD:** versão sempre `v{version}`; skeleton grid; empty por filtro.
- **Deps:** rh-005, rh-012, rh-019 · **Prioridade:** Média

### rh-024 · Sessão do instrutor (tela 11)
- **Arquivo:** `src/routes/(app)/pessoas/treinamentos/sessao/+page.svelte` — create
- **Conteúdo:** header + identidade instrutor (avatar + "· Instrutor"); banner sessão (`sessao-atual`, gradiente brand + dot pulse "EM ANDAMENTO · AGORA": título, Grupo·horário mono·máquina, turma `{present}/{total}`, "Faltam Xh Ymin", materiais grid (Documentação→modal-guia / Apoio / Modelos STL+gcode — carregados ao abrir), ações `Atribuir tarefa final` (`modal-tarefa`: Título\*/Descrição/Treinamento/Grupo/Modelo/Data-conclusão\* → `POST …/tarefa-final` 🔴) + `Avaliar` (`sessao-avaliar`)); tabs Em aberto/Agora/Passados-sem-avaliação (counts; cards + "Abrir sessão"/"Avaliar"); sem `agora` → EmptyState. **Acesso: `isInstrutor` ou Admin**, demais → redirect `/pessoas/treinamentos`. `data-testid="sessao-atual/tarefa-final/sessao-avaliar"`.
- **Saída:** rota `/pessoas/treinamentos/sessao`.
- **DoD:** guarda Instrutor real (não só `canEdit`); sem sessão ativa não renderiza banner.
- **Deps:** rh-005, rh-011, rh-012, rh-019 · **Prioridade:** Média

---

## B5 — Níveis + Menu

### rh-025 · Níveis & acesso — Admin (tela 12)
- **Arquivo:** `src/routes/(app)/pessoas/niveis/+page.svelte` — create
- **Conteúdo:** `PageHeader` + "+ Novo usuário" (`modal-convidar`: Nome\*/Email\*/Nível\* → convite 🟡 D-8); matriz Nível×módulo (checks/restrições; matriz **autoritativa do backend**); membros (Avatar+nome+matrícula, Nível badge, Grupo, select troca → `alterarNivel` 🔴 + refetch + toast); aviso "nível é autoritativo no backend". Guard `isAdmin` (redirect `/pessoas`). Endpoints: `getNiveis`/`alterarNivel` 🔴 parcial (D-8). `data-testid="nivel-membro/nivel-convidar"`.
- **Saída:** rota `/pessoas/niveis`.
- **DoD:** não-Admin nunca vê conteúdo (redirect, não 403 em tela); sem cálculo de permissão client.
- **Deps:** rh-006, rh-011, rh-012 · **Prioridade:** Alta

### rh-026 · submenu RH no menu.ts ⚠️
- **Arquivo:** `src/lib/config/menu.ts` — modify ⚠️
- **Conteúdo:** no item `Pessoas & RH` existente, `children: [{Pessoas,/pessoas}, {Processo seletivo,/pessoas/processo-seletivo}, {Registro de horas,/pessoas/registro-horas}, {Treinamentos,/pessoas/treinamentos}, {Níveis & acesso,/pessoas/niveis, canSee: isAdmin}]`. Sem alterar `module`/`icon`/`path` raiz.
- **Saída:** submenu + hide real de Níveis p/ `!isAdmin`.
- **DoD:** ⚠️ sequencial (livre agora; próxima feature de menu — vendas/financeiro — depois desta); ausência de DOM coberta em rh-029.
- **Deps:** rh-012..rh-025 (rotas existindo) · **Prioridade:** Média

---

## B6 — Testes

### rh-027 · unit status-maps
- **Arquivo:** `tests/unit/rh-status.test.ts` — create
- **Conteúdo:** `personStatusMeta/psStageMeta/horaStatusMeta/trainingStatusMeta/disponibilidadeMeta/nivelMeta`: label+cor por valor canônico (§7.8); fallback para desconhecido; cobertura das unions de rh-001.
- **DoD:** verde (vitest); sem render.
- **Deps:** rh-007 · **Prioridade:** Média

### rh-028 · unit api layer (mock fetch)
- **Arquivo:** `tests/unit/rh-api.test.ts` — create
- **Conteúdo:** mock `apiFetch` (`vi.mock('$lib/api/client')`): pessoas monta `/api/rh/pessoas` com `search/setor/nivel/status/page` e **omite** vazios; `getPessoa/createPessoa/updatePessoa` paths/métodos; PS grupos/mover/avaliar paths; horas validar/rejeitar + `getHorasDisponiveis`/`validarApontamento`; treinamentos detalhe/sessão/avaliar/tarefa-final; níveis alterar; **assert nenhum path `/rh/people` novo e nenhum `/stock/`**; **assert nenhum helper de soma** exportado por `horas.ts`.
- **DoD:** verde; garante naming PT e R-5.
- **Deps:** rh-002..rh-006 · **Prioridade:** Média

### rh-029 · unit permissions + menu RBAC
- **Arquivo:** `tests/unit/rh-permissions.test.ts` — create
- **Conteúdo:** `isResponsavelRH` (admin✓/sem-resp✗/com-resp✓/recrutando✗) · `canEditRH` (admin✓, bolsista responsável✓, bolsista sem✓→✗, estagiário✗) · `canSeeNiveis` (só admin) · `isTutor/isInstrutor` distintos (mesma pessoa pode ser ambos; flags independentes) · `EDIT_RULES.rh` intacto `[0,1,2]` · menu submenu: Níveis omitido p/ `!isAdmin` (hide real).
- **DoD:** verde; base `permissions.test.ts` intacta.
- **Deps:** rh-011, rh-026 · **Prioridade:** Média

### rh-030 · unit páginas (states + guards) + e2e condicional
- **Arquivo:** `tests/unit/rh-pages.test.ts` — create; e2e `tests/e2e/rh.spec.ts` — **create somente se suite e2e existir; senão SKIP documentado**
- **Conteúdo (unit):** render com `@testing-library/svelte` + `vi.mock` das APIs: lista (skeleton/success/empty/empty-filtrado com chips+limpar/error+retry; `!canEdit` sem checkbox/ações/novo); editar (banner `edit-rbac-banner` sem ações quando `!canEdit`; matrícula read-only; 409 inline); detalhe-treinamento (CTA sessão só com sessão ativa; ações hide se `!isInstrutor`); horas (validar/rejeitar chamam PATCH + refetch; KPIs vêm do extrato mockado, sem soma); PS (avaliar individual); níveis (redirect não-admin).
- **Conteúdo (e2e, se existir):** lista→detalhe→editar→salvar; PS criar grupo→avaliar; horas registrar→validar; treinamentos overview→detalhe→sessão; níveis trocar nível (Admin); guarda: não-Admin em `/niveis` → `/pessoas`. Sem credenciais hardcoded.
- **DoD:** unit verdes; e2e sem Playwright → `SKIP` documentado (padrão estoque/notificações).
- **Deps:** rh-013..rh-025 · **Prioridade:** Média

---

## Resumo

| Bloco | Tasks | Saída |
|---|---|---|
| B1 Fundação | rh-001..rh-011 | types PT + 5 APIs + status-maps + 3 UI kit + RBAC helpers |
| B2 Pessoas | rh-012..rh-016 | guarda + lista + detalhe + novo + editar |
| B3 Seleção+Horas | rh-017..rh-018 | Kanban grupos (Tutor) + registro-horas (extrato backend) |
| B4 Treinamentos | rh-019..rh-024 | second sidebar + overview + detalhe + agenda + documentação + sessão (Instrutor) |
| B5 Níveis+Menu | rh-025..rh-026 | Admin-only + submenu (⚠️ sequencial) |
| B6 Testes | rh-027..rh-030 | 4 units + e2e condicional (skip sem Playwright) |

- **30 tasks** (28 create · 2 modify: `permissions.ts` ⚠️, `menu.ts` ⚠️) · **0 no template**.
- **Dependências entre features:** `menu.ts`/`permissions.ts` sequenciais (livres agora; vendas/financeiro depois). `api/rh.ts` legado com re-export até estoque migrar import. `dashboard/DonutChart` intocado.
- **Endpoints 🔴/🟡 (fila backend/PO, plan §10):** pessoas lista 🔴; detalhe/criar/editar 🟡; PS todos 🔴 (D-4); horas legado 🔴 + nova modelagem 🟡 (D-5, Dívida 4); treinamentos/agenda/guias/sessão 🔴 (D-9); níveis 🔴 parcial (D-8); certificados 🟡 futuro **sem telas** (D-6); integrações projects/machines/tasks 🔴 (D-9).
- **Certificados:** nenhuma task (R-7) — aguardar mockup do Designer + v0.2.0 backend.

# DESPACHO — Planner (Frontend) · 19/09/2026

> Preparado pelo Orchestrator. Despacho é **manual** (usuário dispara o subagent `planner`).
> Alvos: **(A) Espec+Plan+Tasks de Configurações (spec nova)** · **(B) Plan+Tasks de Produção v2** (spec atualizada).

---

## Contexto comum (ler antes)

- `.opencode/state/frontend-orchestration.json` — decisões PO v2 registradas na feature `producao`; feature `configuracoes` (designReady, mockup gerado)
- Mockups = **fonte da verdade visual**: `docs/mokups/producao/` (v2, 12 telas) e `docs/mokups/configuracoes/` (novo, 4 telas) — inclui `requisitos.md` e `instrunctions.md` de cada
- `.opencode/specs/frontend/{architecture,conventions,design-system}.md`
- `.opencode/specs/system/rbac-matrix.md`
- `docs/frontendDatails.md` §10 (matriz de permissões por nível) e §4 (rotas)
- **Formato de plan/tasks**: o template frontend `_template` **não existe** — usar como REFERÊNCIA DE FORMATO os specs backend já prontos: `.opencode/specs/features/auth-service/plan.md` e `tasks.md` (estrutura de seções/camadas + tasks com escopo/critérios), adaptados ao frontend SPAs SvelteKit (1 arquivo ≡ 1 task; campos `id`, `titulo` PT, `arquivo`, `acao` create|modify, `detalhes`, `dependencias`, `verificar`).

## Regras do Planner (inalteráveis)

1. **Sem mockup aprovado → bloqueado** (`MOCKUP_REQUIRED`). Neste despacho o PO aprovou o escopo v2 e Configurações em conversa com o Orchestrator; se ao analisar o mockup julgar alguma tela incompleta para planejar, emite `MOCKUP_REQUIRED` com a tela e NÃO gera as tasks daquela parte.
2. Método: enumerar arquivos → componentes reutilizáveis → chamadas de API → URL params → tasks atômicas → ordem padrão (types → API → components → routes → states → RBAC → tests) → DoD.
3. **Não escrever código**; **não alterar `docs/` nem `docs/mokups/`**; **não alterar** specs de sistema.
4. Endpoints novos identificados como **assumidos/contrato pendente** (designados pelo Designer): marcar nos `requirements.md` com 🟡 e listar no relatório — **não tratar como endpoints reais**.
5. UI text pt-BR; tokens do design-system; RBAC hide-não-disable; URL como fonte de verdade.
6. Conflito de escopo → reportar; nunca planejar 2 features que tocam o mesmo arquivo em paralelo.

---

# (A) CONFIGURAÇÕES — spec nova + plan + tasks

A pasta `.opencode/specs/frontend/features/configuracoes/` **não existe** (folder vazio). Criar:

## A1. `requirements.md` (criar)
Sintetizar a partir de: escopo confirmado no state (`nota` de configuracoes) + `docs/mokups/configuracoes/requisitos.md` + `docs/frontendDatails.md` (rotas `/configuracoes/perfil, /usuarios, /permissoes, /sistema`).

Estrutura a seguir (padrão das features do frontend): `## Routes` · `## Access` · `## Data Requirements` · `## RBAC` · `## States` · `## URL State` · `## Mockup`.

Conteúdo mínimo:
- **Routes**: `/(app)/configuracoes/perfil` · `/usuarios` · `/permissoes` · `/sistema`
- **Access**: perfil = autenticado; usuarios/permissoes/sistema = **Admin only (HIDE)**; guarda de rota Admin para os 3; nenhum disable
- **Data Requirements**: `GET/PUT /api/configuracoes/perfil` · `PUT /api/auth/senha` · `GET/POST/PUT /api/usuarios` · `GET/PUT /api/permissoes` (matriz RBAC) · `GET/PUT /api/configuracoes/sistema` — marcar 🟡 contrato pendente conforme Designer (endpoints assumidos, gateway não confirma)
- **RBAC**: tabela Admin/Bolsista/Voluntário/Estagiário/Recrutando (perfil para 0–4 autenticados; demais telas Admin only)
- **States**: Loading/Success/Empty/Error + RBAC-restricted
- **URL State**: `?page&q&nivel&status` nas listas (usuários); abas/segmentos em `/sistema`
- **Mockup**: `docs/mokups/configuracoes/`

## A2. `plan.md` (criar)
Estratégia: camada shared (types de usuário/permissão/config, api client configuracoes), components UI reutilizados (tabela, filtros, modal, estado vazio), rotas com guard Admin, sidebar já possui item "Configurações" (mockup dashboard) — plano deve honrar o Guardo `auth`/App Shell como dependência (execução após auth+shell).

## A3. `tasks.md` (criar)
Tasks atômicas (1 arquivo ≡ 1 task), ordem types → api → components → routes → states → RBAC → tests. Incluir DoD por task (`verificar`).

# (B) PRODUÇÃO — spec v2 + plan + tasks

## B1. `requirements.md` (ATUALIZAR para v2)
`producao` já tem spec; atualizar conforme decisões PO v2 (mockup 12 telas = fonte da verdade):
- Rotas: adicionar `/(app)/producao/projetos/[id]` (detalhe com abas Geral/Documentação/Materiais/Tarefas)
- **Data Requirements** novos (🟡 contrato pendente): `GET /api/producao/projetos/{id}` · `POST /api/producao/projetos/{id}/documentos` · `GET /api/producao/projetos/{id}/materiais` · `POST /api/producao/projetos/{id}/materiais/saida`
- **5S setores**: editar setor (Admin direto; não-Admin → proposta/aprovação); questionário com itens específicos editáveis (sem peso, sem reordenação)
- **Auditoria**: nota automática (cada item vale igual; prévia no front; backend recalcula em `PUT /inspecoes-5s/{id}/concluir`); NÃO há campo manual
- **Pendências**: modal de detalhe (origem/NC/anexos/prazo/penalidade/histórico)
- **Tarefas**: descrição + checklist de atividades (progresso); **status manual** (checklist não altera status)
- RBAC/States/URL State mantidos + adições (abas `?tab=` no projeto detalhe)

## B2. `plan.md` (criar/atualizar)
Estratégia gerenciando o acréscimo da 12ª tela e modais novos (detalhe pendência, saída de material); componentes novos vs compartilhados; guard de rota e RBAC do módulo; dependência com Estoque (saída vinculada, `POST /materiais/saida`); documentação com upload+link (componente de anexos/galeria).

## B3. `tasks.md` (criar/atualizar)
Tasks atômicas cobrindo as 12 telas + fundação compartilhada (types/api/ui 5S-projeto). Marcar tasks que dependem de endpoints 🟡 como "contrato pendente" (execução condicionada ao gateway expor `/api/producao/**`).

---

## Fim do despacho — relatório esperado
- Arquivos criados/alterados (A1–A3, B1–B3)
- Resumo de conflitos de escopo (se houver) — ex.: tocam os mesmos arquivos que outra feature? (`configuracoes/usuarios` vs `rh`, `auth/senha` vs `auth`; `producao/materiais` vs `estoque`)
- Contagem de tasks por feature e DoD das principais
- Endpoints 🟡 listados (para contract.gap no backend)
- Atualizar `.opencode/state/frontend-orchestration.json`: preencher `tasks` das features `producao` e `configuracoes` (resumo) e ajustar `proximos_passos` (spec/plan/tasks prontos)
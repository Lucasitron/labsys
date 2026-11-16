# Lote 1 — correção de bloqueantes front × back (contract-review 2026-10-11)

Regra: backend PT é fonte da verdade → front adapta, exceto CF-BACK-01 (bug real no back).
Sem reescrita de telas/regras; só fronteira (paths, keys, enums, unwraps, payloads).
Verificação de cada task: `curl` contra back local + clique no fluxo da tela (sem 404/422/`undefined`).

## CF-BACK-01 — inbox/histórico sem wrapper `{data}` [lado: back, branch `dev/backend`]
- **Descrição:** `NotificacaoController@index` e `HistoricoController@index` aninham
  `NotificationResource::collection(...)` dentro de `response()->json([...])`, serializando
  `items` como `{data:[...]}` em vez de array. Mapear `->toArray($request)` (padrão já usado em
  Estoque/Vendas/RH).
- **Arquivos:** `backend/app/Modules/Notification/Http/Controllers/NotificacaoController.php`,
  `backend/app/Modules/Notification/Http/Controllers/HistoricoController.php`.
- **Aceite:** `curl GET /api/notificacoes` e `GET /api/notification/historico` (Bearer) devolvem
  `"items":[...]` (array) com keys `{id,type,title,body,read,createdAt,link}`; `total` preservado.

## CF-FRONT-01 — remover prefixo `/api` duplicado [lado: front, branch `dev/frontend`]
- **Descrição:** `apiFetch` já prefixa `API_BASE=.../api`. Trocar `/api/vendas` → `/vendas`,
  `/api/financeiro` → `/financeiro`, `/api/rh` → `/rh` em todos os `BASE`/literais (mecânico,
  sem mudar nomes PT).
- **Arquivos:** `frontend/src/lib/api/vendas/*.ts` (7), `frontend/src/lib/api/financeiro/*.ts` (6),
  `frontend/src/lib/api/rh/*.ts` (horas, niveis, pessoas, processo-seletivo, treinamentos).
- **Aceite:** nenhuma ocorrência de `'/api/vendas|/api/financeiro|/api/rh'` em `lib/api`;
  `curl` dos paths resultantes responde ≠ 404 (com Bearer).

## CF-FRONT-02 — paths PT de notificações + sino [front `dev/frontend`]
- **Descrição:** em `notifications.ts`: lista → `/notificacoes`, markRead → `PATCH
  /notification/notificacoes/{id}/ler`, markAllRead → `POST /notification/notificacoes/ler-todas`,
  prefs → `/notification/preferencias` (GET/PUT), history → `/notification/historico`;
  em `dashboard.ts`: `fetchUnreadCount` → `/notificacoes/nao-lidas/contagem`.
- **Arquivos:** `frontend/src/lib/api/notifications.ts`, `frontend/src/lib/api/dashboard.ts`.
- **Aceite:** inbox carrega (`items` array pós CF-BACK-01), marcar lida/todas OK, sino atualiza
  `count` sem erro; query `read=true/false` filtra.

## CF-FRONT-03 — estoque: números + update + entrada + BOM [front `dev/frontend`]
- **Descrição:** (a) coagir `Number()` em `quantidadeAtual/estoqueMinimo/quantidade/valor*` na borda
  das api `stock/*` (badge "baixo" hoje calculado com strings); (b) editar item inclui
  `quantidadeAtual` atual no PUT; (c) form nova-entrada exige fornecedor e valor (back: required);
  (d) consumo BOM manda `quantidadeConsumida`.
- **Arquivos:** `frontend/src/lib/api/stock/{items,movements,loans,bom}.ts`,
  `frontend/src/routes/(app)/estoque/itens/[id]/editar/+page.svelte`,
  `frontend/src/routes/(app)/estoque/entradas/nova/+page.svelte`,
  `frontend/src/routes/(app)/estoque/bom/+page.svelte`.
- **Aceite:** item qtd 5/mín 10 → badge "Baixo"; PUT item → 200; entrada sem fornecedor bloqueada
  no form; consumo BOM → 2xx.

## CF-FRONT-04 — vendas: clientes [front `dev/frontend`]
- **Descrição:** lista manda `termo` (não `search`); `getCliente` desembrulha `{cliente}`;
  create/update mapeiam `nome→nomeRazaoSocial`, `documento→cpfCnpj`, `tipoPessoa→UPPER`;
  `bulkAdicionarTag` manda `{clienteIds,tagId}`.
- **Arquivos:** `frontend/src/lib/api/vendas/clientes.ts` (+ telas que leem `codigo`/`createdBy`
  passam a tolerar ausência — guards, sem redesign).
- **Aceite:** criar PF com CPF válido → 201; listar com busca filtra; bulk-tag → 2xx.

## CF-FRONT-05 — vendas: encomendas/orçamentos + mover kanban [front `dev/frontend`]
- **Descrição:** listas mandam `status` (não `status_kanban`); `moverKanban` inclui `version`
  (lida do detalhe `GET .../encomendas/{id}`, que expõe `versao`) e trata resposta como
  **detalhe de encomenda** (não `HistoricoEncomenda`); telas toleram ausência de `codigo`/
  objeto `cliente` (usar `clienteNome`, `dataPrevisaoEntrega`).
- **Arquivos:** `frontend/src/lib/api/vendas/encomendas.ts`, `orcamentos.ts`,
  telas `(app)/vendas/encomendas/**`, `(app)/vendas/orcamentos/**`.
- **Aceite:** mover Fila→Produção → 200 e reflete; detalhe abre sem `undefined` estourado.

## CF-FRONT-06 — vendas: solicitações + interações [front `dev/frontend`]
- **Descrição:** solicitações viram payload flat `{tipo:UPPER_SNAKE, alvoTipo, alvoId, campo,
  valorAtual?, valorProposto, justificativa}` (mapas `Alteração de dados→ALTERACAO_DADOS`,
  `CLI/OC/EN→CLIENTE/ORCAMENTO/ENCOMENDA`); decisão → `PUT .../{id}/decisao {decisao:'APROVADA'|...,
  motivo?}`; interações → `GET /vendas/interacoes/{idCliente}` (create mantém body menos
  `responsavelId`).
- **Arquivos:** `frontend/src/lib/api/vendas/solicitacoes.ts`, `interacoes.ts`,
  telas `(app)/vendas/solicitacoes/**`, `(app)/vendas/clientes/[id]/**`.
- **Aceite:** criar solicitação → 201; aprovar/rejeitar → 200; aba interações lista + registra.

## CF-FRONT-07 — usuários + permissões (leitura + convite) [front `dev/frontend`]
- **Descrição:** `listarUsuarios` lê `{content→dados, page/size/total/totalPages→paginacao}` e manda
  `size` (não `pageSize`); `Usuario` mapeado de `{nomeUsuario→nome, situacao, nivel}`;
  `criarUsuario` vira form com pessoa (`nomeUsuario`), `email`, `senha` (+`idUser` — definir origem
  com Orquestrador: vínculo a pessoa RH); matriz de permissões: adaptar lista
  `[{modulo,nivel,valor}]` → `Record` + `'Visualizar'→'Ver'` (update já aceita `Ver`).
- **Arquivos:** `frontend/src/lib/api/configuracoes/{usuarios,permissoes}.ts`,
  telas `(app)/configuracoes/{usuarios,permissoes}/**`.
- **Aceite:** lista pagina (size respeitado); matriz renderiza e salva célula → 200.

## CF-FRONT-08 — sistema `nomeFablab` + perfil via `/auth/me` [front `dev/frontend`]
- **Descrição:** `sistema.ts` mapeia `identidade.nome↔nomeFablab` (load e save); `perfil.ts` deixa de
  chamar `GET /configuracoes/perfil` (inexistente) e monta perfil de `fetchMe` (`/auth/me`); update
  de perfil: gatear/desabilitar até existir endpoint (não inventar contrato).
- **Arquivos:** `frontend/src/lib/api/configuracoes/{sistema,perfil}.ts`,
  tela `(app)/configuracoes/perfil/**`.
- **Aceite:** tela sistema carrega/salva nome; perfil abre sem 404.

## CF-FRONT-09 — produção listas: unwrap `{data}` + enums de leitura [front `dev/frontend`]
- **Descrição:** `obterProjetos/Tarefas/Kanban/Maquinas/Setores/inspecoes/mesas` desembrulham
  `res.data ?? res` e aplicam paginação/filtro client-side (precedente: estoque `ItemsResult`);
  mapper UPPER→Title p/ exibição/filtro (`PLANEJADO→Planejado`, `MEDIA→Média`, etc.;
  `'Moderada'` sai das opções).
- **Arquivos:** `frontend/src/lib/api/producao/{projetos,tarefas,kanban,maquinas,setores-5s,
  inspecoes-5s,mesas,pendencias-5s,penalidades-5s,ranking-5s}.ts` (+ `+page.ts` consumidores).
- **Aceite:** listas de projetos/tarefas/kanban/máquinas renderizam com dados reais; filtros de
  status funcionam client-side.

## CF-FRONT-10 — financeiro: mapper UPPER + lançamentos [front `dev/frontend`]
- **Descrição:** mapper único Title↔UPPER (tipo/status/categoria/doação) aplicado no send e no read;
  `listLancamentos` manda `{status,tipo,idCategoria,dataInicio/dataFim}` e tolera ausência de
  `pagination` (fallback `{page:1,pageSize:total,totalItems,totalPages:1}`); `Lancamento` coage
  `valor:Number()` e `categoria:{id,nome?}`.
- **Arquivos:** novo `frontend/src/lib/utils/financeiro-map.ts` (ou bloco em `lib/api/financeiro/`),
  `frontend/src/lib/api/financeiro/{lancamentos,categorias}.ts`, telas
  `(app)/financeiro/lancamentos/**` + `NovoLancamentoModal`.
- **Aceite:** criar lançamento Entrada → 201; lista com filtro status/tipo funciona.

## CF-FRONT-11 — RH mínimo: pessoas + horas (lista e create) [front `dev/frontend`]
- **Descrição:** pessoas: payload `{nomeCompleto,nome→?,matricula,contato}` (mapear form) e leitura
  `{pessoas→people, paginacao→pagination}` + `Person` de `{nomeCompleto→name, contato→email/phone}`;
  horas: lista embrulha array → `{hours}`; apontar manda `{idFuncionario,tipo:UPPER,idReferencia,
  data,horasTrabalhadas/horaInicio/horaFim,descricaoAtividade}` (form precisa capturar
  funcionário + referência — sem isso, gatear o create).
- **Arquivos:** `frontend/src/lib/api/rh/{pessoas,horas}.ts`, telas `(app)/pessoas/**`
  (lista, detalhe via `/detalhe`, registro-horas).
- **Aceite:** lista de pessoas renderiza; apontamento válido → 201; lista de horas renderiza.

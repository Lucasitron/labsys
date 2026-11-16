# REVIEW de compatibilidade front × back — 2026-10-11

**Escopo:** frontend `origin/dev/frontend` (commit `1f1acb4`, SPA SvelteKit, `API_BASE=<host>/api`)
× backend checkout atual = `origin/dev/backend` (`c627a4a`, monólito Laravel, 8 módulos, rotas montadas
sob `/api` via `backend/routes/api.php`).
**Método:** chamadas reais em `frontend/src/lib/api/**` + consumo em `frontend/src/routes/**`
conferidas contra `routes.php` + Controllers + `rules()` + `toArray()` de cada módulo.
**Regra de lado:** backend PT é fonte da verdade → **front adapta**, salvo bug real no backend
(marcado `BACK`).

## Resumo executivo

| Severidade | Qtd | IDs |
|---|---|---|
| 🔴 BLOQUEANTE (quebra fluxo real) | 33 | F01–F33 |
| 🟡 DIVERGÊNCIA (parcial/silencioso) | 14 | D01–D14 |
| ⚪ COSMÉTICO / fora-MVP | 6 | C01–C06 |

**Padrões sistêmicos (causa raiz da maioria):**
1. **Prefixo `/api` duplicado no front (P0).** `apiFetch` já prefixa `API_BASE=.../api`, mas
   `vendas/**` (7 arqs), `financeiro/**` (6 arqs) e `rh/**` (5 arqs) chamam `/api/vendas/...`,
   `/api/financeiro/...`, `/api/rh/...` → URL final `/api/api/...` → **404 em 3 módulos inteiros**.
   (`stock/**`, `producao/**`, `auth`, `dashboard/summary`, `configuracoes/*`, `usuarios/*`,
   `permissoes` usam paths sem `/api` → corretos.)
2. **Enums: backend UPPER_SNAKE (espelha checks `ck_*` do banco) × front Title-case PT.**
   Afeta financeiro (tudo), produção (status/prioridade), RH (status/tipo) e vendas-solicitações.
   Lado: **front mapeia** (trocar backend = migração + dados; fora de questão).
3. **Wrappers de lista.** Produção retorna `Resource::collection` dentro de `response()->json`
   → `{data:[...]}`; front espera array ou `{dados,paginacao}`. Notification faz o mesmo com
   `items` (bug BACK). Vendas/financeiro/RH/estoque usam `->toArray()` manual → OK.
4. **Quantidades/valores como string no estoque/financeiro** (`(string)` nos Resources) ×
   front espera `number` e faz comparação/aritmética → status "baixo estoque" calculado errado
   (comparação lexicográfica: `"5" < "10"` é `false`).

## Tabela por módulo

### Auth (`/auth/*`) — ✅ OK, sem bloqueante
| Chamada front | Endpoint back | Veredito | Lado |
|---|---|---|---|
| `POST /auth/login` `{email\|nomeUsuario, senha}` | `POST /auth/login` (`LoginRequest`: email/nomeUsuario nullable, senha 6–72) → 200 `{accessToken,refreshToken,tokenType,expiresIn,idUser,role,setor,nomeUsuario}` | ✅ OK (já reconciliado em `1f1acb4`) | — |
| `POST /auth/refresh {refreshToken}` | `POST /auth/refresh` (`RefreshRequest`) | ✅ contrato OK | — |
| `POST /auth/logout` (Bearer + `{token}` opcional) | `POST /auth/logout` (lê input ou bearer) → `{message}` | ✅ OK | — |
| `GET /auth/me` (Bearer) | `GET /auth/me` → `MeResource{id,idUser,email,nomeUsuario,setor,permissions[{role:int,label,active}]}` | ✅ OK (`mapMeToUser` confere) | — |
| `PUT /auth/senha {senhaAtual,novaSenha,confirmacaoSenha}` | `PUT /auth/senha` (`AlterarSenhaRequest`: nova 8–72) → `{mensagem}` | ✅ OK | — |
| forgot-password (só link, sem chamada) | inexistente | ⚪ C01: página sem backend; sem quebra | — |

### Dashboard — 1 bloqueante
| Chamada front | Endpoint back | Veredito | Lado |
|---|---|---|---|
| `GET /dashboard/summary` → `DashboardSummary` | `GET /dashboard/summary` (+ alias `/resumo`) → `{tasks,kpis,ordersByStatus,machinesByStatus,activity}` | ✅ OK (keys EN 1:1; kpis admin-opcionais conferem) | — |
| `PATCH /tasks/{id} {done:true}` (MyTasks) | `PATCH /tasks/{id}` → `TarefaController@concluir` (ignora body) | ✅ OK | — |
| 🔴 **F01** `GET /notifications/unread/count` (sino, poll 60s) | **404.** Back: `GET /notificacoes/nao-lidas/contagem` (+ alias) e `GET /notification/notificacoes/nao-lidas/contagem` → `{count}` | Path inexistente; sino sempre em falha (silenciosa) | FRONT |

### Notification — 6 bloqueantes + 1 divergência
| Chamada front (`notifications.ts`, tudo `/notifications*`) | Endpoint back | Veredito | Lado |
|---|---|---|---|
| 🔴 **F02** `GET /notifications?page&pageSize&type&read` | **404.** Back: `GET /notification/notificacoes` e alias `GET /notificacoes` (aceita `page/size/pageSize/type/read`) | Path errado; inbox não carrega | FRONT |
| 🔴 **F03** shape da lista: front lê `items[]` + `total/pages` | Back devolve `'items' => NotificationResource::collection(...)` dentro de `response()->json` → `items={data:[...]}` (gotcha Laravel) | **Bug real no back**; `items.map` quebra mesmo com path certo | BACK |
| 🔴 **F04** `PATCH /notifications/{id}/read` | **404.** Back: `PATCH /notification/notificacoes/{id}/ler` (sem alias) | Marcar lida quebra | FRONT |
| 🔴 **F05** `POST /notifications/read-all` | **404.** Back: `POST /notification/notificacoes/ler-todas` (sem alias) | Marcar todas quebra | FRONT |
| 🔴 **F06** `GET/PUT /notifications/preferences` | **404.** Back: `GET/PUT /notification/preferencias` (sem alias; shape `Record<tipo,Record<canal,bool>>` ✅ confere, TIPOS/CANAIS ✅) | Só path; shape OK | FRONT |
| 🔴 **F07** `GET /notifications/history?search&type&read&channel&period` | **404** (`GET /notification/historico`, admin-only) + mesmo wrapper `{data}` do F03 | Path + wrapper | FRONT+BACK(F03) |
| 🟡 **D01** item: back `id:int`, front `id:string`; back tem `channel` extra | Tolerável em runtime; `===` entre fontes pode falhar | FRONT (coerção) |

### Estoque (`/estoque/*`, paths front ✅) — 5 bloqueantes + 3 divergências
| Chamada front | Endpoint back | Veredito | Lado |
|---|---|---|---|
| 🔴 **F08** `listarItens/buscarItem/criar` lê `quantidadeAtual/estoqueMinimo:number` → `deriveItemStatus` | `ItemResource` devolve `"quantidadeAtual":"5"` (string). `"5"<"10"===false` → badge "baixo" nunca acende; `"12"<"9"===true` → falso "baixo" | Numérico como string quebra regra de UI | FRONT (coerção `Number()` na borda; mesmo padrão p/ entradas/saídas/empréstimos/BOM) |
| 🔴 **F09** `PUT /estoque/itens/{id}` (editar page manda nome/desc/categoria/unidade/estoqueMinimo/local — **sem `quantidadeAtual`**） | `ItemRequest`: `quantidadeAtual` **required** → 422 | Edição de item sempre 422 | FRONT (incluir `quantidadeAtual` atual no payload) |
| 🔴 **F10** `POST /estoque/entradas` (page manda `idFornecedor:null`, `valorUnitario:null` quando vazios) | `EntradaRequest`: `idFornecedor` **required**, `valorUnitario` **required** → 422 | Entrada sem fornecedor/valor quebra | FRONT (exigir no form) |
| 🔴 **F11** `POST /estoque/boms/{id}/consumo {itens:[{idItem,quantidade}]}` | `BomConsumoRequest` exige `itens.*.quantidadeConsumida` → 422 | Consumo de BOM sempre 422 | FRONT (renomear key) |
| 🔴 **F12** BOM detalhe lê `itens[].nomeItem` | `BomResource` não retorna `nomeItem` (só idItem/qtds) | Nomes dos itens em branco | FRONT (resolver via `listarItens`, como faz empréstimos com pessoas) |
| 🟡 **D02** `searchPeople('') → GET /rh/people` | Rota **inexistente** (back tem `pessoas`) — mas chamada é best-effort com `try/catch` → fallback `Pessoa #id` | Degradação graciosa; não quebra | FRONT (Lote 2: apontar p/ `/rh/pessoas`) |
| 🟡 **D03** ids `int` (back) × `string` (front) em todos os Resources | Runtime OK; risco em `===` e em payloads (`idItem` string passa no `integer` do Laravel, que aceita string numérica) | FRONT (coerção pontual) |
| 🟡 **D04** `versao` BOM: back `int`, front `string`; `dataSaida` full-datetime vs `dataEntrada` só-data | Exibição OK; edição de versão pode estranhar | FRONT |

### Vendas — 7 bloqueantes + 4 divergências (todas as chamadas sob `/api/vendas/*` → 404 até F14 sair)
| Chamada front | Endpoint back | Veredito | Lado |
|---|---|---|---|
| 🔴 **F14** prefixo `/api` duplicado em `clientes|orcamentos|encomendas|interacoes|marketing|marketplace|solicitacoes` (7 arqs) | Back: `/vendas/...` (sem `/api` no path, pois `API_BASE` já tem) | **Módulo inteiro 404** | FRONT (strip `/api`, mecânico) |
| 🔴 **F15** `POST/PUT /vendas/clientes {tipoPessoa:'pf', nome, documento, ...}` | `ClienteRequest`: `tipoPessoa in:PF,PJ` (**'pf' → 422**), `nomeRazaoSocial` required (front manda `nome` → 422), `cpfCnpj` required (front manda `documento` → 422) | Create/update sempre 422 | FRONT (mapear keys + upper) |
| 🔴 **F16** `getCliente` espera `Cliente` direto; lista espera `{clientes,pagination}` | `show` devolve `{cliente, indicadores}`; `index` devolve `{clientes, paginacao}` e filtro é `termo` (front manda `search` → ignorado) | Detalhe quebra; busca não filtra | FRONT (unwrap + `termo`) |
| 🔴 **F17** `POST .../clientes/bulk-tag {ids, tagId}` | `BulkTagRequest`: `{clienteIds, tagId}` → 422 | Bulk tag quebra | FRONT |
| 🔴 **F18** `POST /vendas/solicitacoes {tipo:'Alteração de dados', alvo:{tipo:'CLI',id,nome}, ...}` + `PUT /vendas/solicitacoes/{id} {aprovada,motivo}` | `SolicitacaoRequest` flat: `tipo in:ALTERACAO_DADOS,...`, `alvoTipo in:CLIENTE,...`, `alvoId:int`, `campo`+`valorProposto` required → 422. Decisão é `PUT .../{id}/decisao {decisao,motivo}` → 404+422 | Fluxo de solicitações todo quebrado | FRONT (achatar + UPPER + novo path/body) |
| 🔴 **F19** `GET /vendas/clientes/{id}/interacoes` | **Inexistente.** Back: `GET /vendas/interacoes/{idCliente}` (+ `POST /vendas/interacoes {clienteId,tipo,descricao,dataInteracao}` — body front OK menos `responsavelId` extra, ignorado) | Aba interações do cliente quebra | FRONT (trocar path) |
| 🔴 **F20** `PUT .../encomendas/{id}/kanban {statusKanban, observacao}`, front lê `HistoricoEncomenda` | `KanbanRequest` exige **`versao`** → 422; back devolve **detalhe da encomenda**, não histórico | Mover kanban quebra | FRONT (enviar `versao` do detalhe + ler resposta como encomenda) |
| 🟡 **D05** encomendas index: front manda `status_kanban/search`, back lê `status/clienteId` | Filtro kanban/busca silenciosamente ignorados (lista carrega) | FRONT (`status`) |
| 🟡 **D06** orcamentos index: front manda `search/page`, back só `status/clienteId`, sem paginação | Busca/paginação client ignoradas | FRONT (filtrar client-side) |
| 🟡 **D07** shapes: sem `codigo` (cliente/orçamento/encomenda), `cliente:{}` (back: `clienteNome/idCliente`), `valorTotal/valorFinal` string, marketplace sem `encomenda{}`/`createdBy`, interacao sem `responsavel`, tarefa `criadoPor` vs `createdBy` + `responsavelId:int` | Displays com `undefined`; contas com string | FRONT (adaptadores de leitura) |
| 🟡 **D08** sem fn front p/ `duplicar` orçamento, `nova-ordem`, `historico-status`, vincular/desvincular tag (botões? verificar telas) — endpoints existem no back | Funcionalidade sem fio | FRONT (Lote 2) |

### Financeiro — 6 bloqueantes + 3 divergências (tudo sob `/api/financeiro/*` → 404 até F22 sair)
| Chamada front | Endpoint back | Veredito | Lado |
|---|---|---|---|
| 🔴 **F22** prefixo `/api` duplicado (6 arqs: categorias, lancamentos, doacoes, custeio, relatorios, solicitacoes-compra) | Back `/financeiro/...` | Módulo inteiro 404 | FRONT |
| 🔴 **F23** enums Title-case × UPPER em tudo: `tipo:'Entrada'` vs `in:ENTRADA,SAIDA`; `status:'Pendente'` vs `PENDENTE,...`; categoria `'Receita'` vs `RECEITA`; doação `'Doação'` vs `DOACAO`; compra `'Registrada'` vs `REGISTRADA`; fechamento `'Aberta'` vs `ABERTA` | Todos os creates 422 + filtros ignorados/422 | FRONT (mapper bidirecional único) |
| 🔴 **F24** `listLancamentos` lê `{lancamentos,pagination,counts,resumo}` | Back devolve `{lancamentos,counts,resumo}` (**sem `pagination`**); query real: `{status,tipo,idCategoria,dataInicio,dataFim,origem,vencimentoDe/Ate}` (front manda search/categoria-nome/periodo/ordenar/page) | Página quebra em `pagination.page`; filtros não funcionam | FRONT (guard + mapear query) |
| 🔴 **F25** `listDoacoes` lê `{registros,resumo,cobertura}` | Back devolve **array puro**; query `{tipo,apenas dataInicio/Fim}` | `registros` undefined → quebra | FRONT (embrulhar + agregar client-side) |
| 🔴 **F26** `createSolicitacaoCompra {item,quantidade,valorEstimado,observacao}`; `concluirCompra(id,{dataConclusao,valorReal,notaFiscal})` lê `{status,lancamentoGerado}` | `CompraRequest`: `{idItemEstoque!,quantidade,valorEstimado?}` → 422. `concluir` **ignora body** e devolve `CompraResource` (sem `lancamentoGerado`) | Compra nova + conclusão quebradas | FRONT (payload por idItem + ler `CompraResource`) |
| 🔴 **F27** relatórios: `fluxoCaixa` lê `{linhas,total}` (back: `{entradas,saidas,saldo,semanas}`); `inadimplencia` lê `{cliente,referencia,vencimento,diasAtraso,valor}` (back: `{idLancamento,referencia,valor,dataVencimento,diasEmAtraso}` **sem cliente**); `doacoesDespesas` lê `{linhas,acumulado}` (back: `{doacoes,recursosProjeto,despesas,saldo,coberturaPercentual,meses}`); `custoMaquina` lê `{linhas[{maquina,horas,custo,percentual}],total}` (back: `[{idMaquina,custo,percentualTotal}]` **sem horas**); `lucratividade` (verificar keys) | 5 de 6 relatórios quebram ou exibem `undefined` | FRONT (adaptadores; `dre` só coage strings) |
| 🟡 **D09** valores/quantidades string (`valor`, `horasEstimadas`, `custo*`, `margemLucro`) × front `number` | Somas concatenam | FRONT (coerção) |
| 🟡 **D10** `getLancamento/:id` — **rota inexistente** no back (só index/store/pagamento); `registrarPagamento` PUT `{dataPagamento,observacao}` ✅ confere | Detalhe direto 404 (telas usam lista?) | FRONT (usar lista/filtro) |
| 🟡 **D11** `periodo` enviado a inadimplência/doações-despesas/custo-máquina (back ignora extras) | Inofensivo | — |

### RH — 8 bloqueantes + 2 divergências (tudo sob `/api/rh/*` → 404 até F28 sair)
| Chamada front | Endpoint back | Veredito | Lado |
|---|---|---|---|
| 🔴 **F28** prefixo `/api` duplicado (horas, niveis, pessoas, processo-seletivo, treinamentos) | Back `/rh/...` | Módulo inteiro 404 | FRONT |
| 🔴 **F29** pessoas: front `{name,email,phone,type,nivel,nome...}` + lê `{people,pagination}` + `Person{...}` | `PessoaRequest`: `{nomeCompleto!,matricula!,dataAdmissao,contato,turno,status:0-2,cpf}`; index `{pessoas,paginacao,filtros}`; `PessoaResource{id,nomeCompleto,matricula,dataAdmissao,contato,turno,status-name,cpf}` (sem email/grupo/nivel/tipo) | CRUD + lista quebrados | FRONT (mapear + adaptar; `detalhe` com KPIs existe p/ tela `[id]`) |
| 🔴 **F30** horas: front `{personId,date,startTime,endTime,hours,type:'encomenda',projectId/orderId}` + lê `{hours,counts}` | `RegistrarApontamento`: `{idFuncionario!,tipo:ENCOMENDA/PROJETO,idReferencia!,data,horasTrabalhadas/horaInicio/horaFim,descricaoAtividade}`; index devolve **array puro**; status `PENDENTE/VALIDADO/REJEITADO` | Apontar + lista quebrados | FRONT |
| 🔴 **F31** `rejeitarHoras(id,{reason})` | `RejeitarApontamentoRequest`: `{motivo!}` → 422 | Rejeitar quebra | FRONT (`reason→motivo`) |
| 🔴 **F32** `validarApontamento(id)` sem body em `/apontamentos-horas/{id}/validar` | `ValidarApontamentoRequest` exige `{status:VALIDADO/REJEITADO}` → 422 (obs.: `/horas/{id}/validar` PATCH não exige body ✅) | Validar via apontamentos quebra | FRONT (enviar `{status:'VALIDADO'}` ou usar rota `/horas`) |
| 🔴 **F33** níveis: front `PATCH .../membros {nivel:'admin'}` + `POST convites {name,email,nivel}` + lê `NivelMatrix{modulos,niveis,matriz}` | Back: `{nivel:int 0-4}` → 422; convite `{nomeCompleto!,matricula!,...}` → 422; `matriz()` devolve `{niveis[],permissoes[]}` | Níveis todo quebrado | FRONT |
| 🔴 **F34** PS: front `POST grupos {name,tutorId,memberIds}`, `PATCH {id}/estagio {estagio:'triagem'}`, lê `{groups,counts}` | Back: `{nome!,idLider!,membroIds?}` → 422; `{etapa:INSCRITO/EM_TRIAGEM/...}` → 422; lista `{grupos[],totais{}}` com keys `nome/etapa/total/candidatos` | Grupos + mover estágio + lista quebrados | FRONT |
| 🔴 **F35** treinamentos: front chama 8 endpoints (`list/get/sessao/avaliar→POST :id/avaliar/tarefa-final/agenda/disponibilidade/guias`) | Back só tem `POST /treinamentos`, `POST+GET /treinamentos/{id}/avaliacoes` (e `avaliar` exige `{idFuncionario!,nota,feedback}`) | Telas de treinamento quase todas 404 | FRONT Lote 2 (reapontar avaliar + desabilitar/gatear resto) / BACK Lote 3 (endpoints) |
| 🟡 **D12** `getHorasDisponiveis` lê `{periodo,validadas,pendentes,rejeitadas}` | Back: `{idFuncionario,totalHorasDisponiveis}` | Tela registro-horas exibe errado | FRONT |
| 🟡 **D13** certificados/funcionários/extrato: **sem chamadas no front** (fora MVP) | Back existe e parece consistente | Sem ação | — |

### Produção (paths front `/producao/*` ✅, sem prefixo duplo) — 11 bloqueantes + 1 divergência
| Chamada front | Endpoint back | Veredito | Lado |
|---|---|---|---|
| 🔴 **F36** listas (`obterProjetos/Tarefas/Kanban/Maquinas/Setores/Inspecoes/Mesas`) lêem `Paginado{dados,paginacao}` ou array | Back: `Resource::collection` em `response()->json` → **`{data:[...]}`, sem paginação** | Todas as listas vazias (`dados ?? []`) | FRONT (unwrap `data` + paginação client, precedente do estoque) |
| 🔴 **F37** enums Title × UPPER na leitura/filtros: projeto `'Planejado'` vs `PLANEJADO`; tarefa `'Pendente'` vs `PENDENTE`; prioridade `'Média'` vs `MEDIA` (front ainda tem `'Moderada'` fantasma); kanban `'Produção'` vs `PRODUCAO`; máquina `'Disponível'` vs `DISPONIVEL` | Filtros `in:` rejeitam; comparações de tela falham | FRONT (mapper) |
| 🔴 **F38** `criarProjeto {nome,cliente?,responsavelId,prazo,status}` | `ProjetoRequest`: `{nome,dataInicio!,dataFimPrevista,idResponsavel,status?}` (sem `cliente/prazo`) → 422 | Criar projeto quebra | FRONT |
| 🔴 **F39** `criarTarefa {..., projetoId?}` (opcional) | `TarefaRequest`: `idProjeto` **required** → 422 sem projeto | Criar tarefa avulsa quebra | FRONT (exigir projeto ou gatear) |
| 🔴 **F40** `moverKanbanItem PATCH {id}/mover {coluna}` | Back: `PUT /kanban/{id}/mover {statusNovo,version!,observacao?}` → 404/422 | Mover kanban quebra | FRONT (PUT + keys + version do detalhe) |
| 🔴 **F41** `moverTarefa PATCH {id}/mover`, `apontarHoras PATCH {id}/apontar-horas`, `atribuir PATCH {id}/atribuir` | **Rotas inexistentes.** Back: `PUT {id}/status {status,dataConclusao?}`, `PATCH {id}` (concluir própria), `PUT {id}` | Três ações 404 | FRONT (reapontar p/ `/status`; remover/gatear apontar-horas e atribuir) |
| 🔴 **F42** máquinas: front lê `{codigo,categoria,responsavelManutencao,status,cartao}` + chama `/chamados`, `/preventivas`, `/historico`(POST) | Back: `{id,nome,descricao,localizacao,status}` + `/{id}/uso`, `/{id}/uso/{idUso}/fim`, `/{id}/historico` (GET). Sem código/categoria/cartão/chamados/preventivas | Lista exibe `undefined`; 3 sub-recursos 404 | FRONT Lote 2 (adaptar + gatear chamados/preventivas) / BACK Lote 3 |
| 🔴 **F43** setores: front `/producao/5s/setores*` (+ responsaveis/checklist/aprovar-alteracao) | Back: `/producao/setores*` (sem `/5s`; sem aprovar-alteracao; checklist por item) → 404 | 5S setores todo 404 | FRONT (reapontar paths; Lote 2 p/ checklist granular) |
| 🔴 **F44** inspeções: front `POST {setor,auditorId,data}` + `PUT {id}/concluir` + `POST {id}/nomear-auditor` | `InspecaoRequest`: `{idSetor!,idInspetor!,dataInspecao!,turno:MANHA/TARDE,observacoes?,itens[]...}` → 422; concluir/nomear **inexistentes** (só POST/GET/GET{id}/DELETE) | Inspeções quebradas | FRONT (payload; gatear concluir/nomear — Lote 2) |
| 🔴 **F45** `PUT /producao/parametros-5s` (coleção) | Back: `PUT /producao/parametros-5s/{id} {valor,descricao?}` (Admin) → 404/405 | Salvar parâmetros quebra | FRONT (PUT por id) |
| 🔴 **F46** mesas: front `/producao/mesas*` (+evolucao/auditoria) | Back: `/producao/projetos-mesa*` (+evolucao? só `PUT {id}/evolucao` ✅ existe; auditoria via `POST /auditorias-projeto-mesa`) → 404 | Mesas todo 404 | FRONT (reapontar) |
| 🟡 **D14** `resumo` com `USE_PRODUCAO_MOCK=true` (tela funciona com mock); pendências/penalidades/ranking sem backend | Tela raiz OK por mock; 3 sub-telas 404 | BACK Lote 3 (endpoints) ou manter mock+gates |

### Configurações (`/usuarios`, `/permissoes`, `/configuracoes/*`) — 4 bloqueantes + 2 divergências
| Chamada front | Endpoint back | Veredito | Lado |
|---|---|---|---|
| 🔴 **F47** `listarUsuarios` lê `Paginado{dados,paginacao}` + manda `pageSize` | Back: `{content,page(0-based),size,total,totalPages,contagens}` e lê **`size`** (não `pageSize`) | Lista quebra + pageSize ignorado | FRONT |
| 🔴 **F48** `criarUsuario {nome,email,telefone?,nivel,situacao?}` | `CriarUsuarioRequest`: `{idUser!,email,nomeUsuario!,senha 8-72,...}` → 422 | Convite quebra (falta vínculo pessoa/senha na UX) | FRONT (form: pessoa+usuário+senha; update usa `nomeUsuario/setor`, não `nome`) |
| 🔴 **F49** `obterPermissoes` lê `Record<Module,Record<Nivel,'Ver'|'Editar'|'Nenhum'>>` | Back: `{roles,matriz:[{modulo,nivel,code,valor:'Visualizar'|'Editar'|...}]}` (+ update `PUT {valor}` aceita `Ver` via parse case-insensitive ✅) | Matriz não renderiza | FRONT (adaptador lista→Record + `'Visualizar'→'Ver'`) |
| 🔴 **F50** sistema: front `identidade{nome,logo}` | Back: `identidade{nomeFablab,logo}` (+ cadências ✅, tokens embutidos ✅) | Nome some no save/load | FRONT (`nome↔nomeFablab`) |
| 🟡 **D15** `getPerfil/updatePerfil → /configuracoes/perfil` | **Rota inexistente** (só `/auth/me` + `/auth/senha`); front tem fallback p/ `fetchMe` só em 5xx/rede (404 estoura) | Perfil quebra | FRONT (usar `/auth/me` direto — Lote 1, trivial) |
| 🟡 **D16** tokens: shapes compatíveis (`{id,nome,prefixo,criadoEm}` ✅); `criar` devolve `{...,chave}` (front ignora — **chave exibida uma única vez?** verificar tela); `revogar` 204 ✅ | Quase OK | FRONT (exibir `chave` no criar — Lote 2) |

### Transversais
| # | Achado | Sev | Lado |
|---|---|---|---|
| T01 | Sem refresh automático: `refreshToken` guardado mas nunca usado; 401 → logout+redirect | 🟡 | FRONT Lote 2 |
| T02 | RBAC front (`VIEW_RULES/EDIT_RULES`) é aproximação local; decisão real é do back (403). Telas podem exibir ação que o back nega (ex.: fornecedor POST só Admin) | 🟡 | FRONT Lote 3 (alinhar c/ `rbac-matrix.md`) |
| T03 | Datas: back date-only `YYYY-MM-DD` (substr) × front ISO/full — `loanComputed`/`fmtDate` toleram via slice | ⚪ | — |
| T04 | `DELETE /vendas/clientes/{id}` → 204 ✅; `DELETE /usuarios` inexistente (só PATCH status) — front nem tenta ✅ | ⚪ | — |
| T05 | `POST /estoque/itens/import {arquivo}` (FormData) ✅ confere; `export` CSV ✅ | ⚪ | — |
| T06 | `PUT /auth/senha` erro de confirmação: back valida presença, não igualdade? (`confirmacaoSenha` required; igualdade checada no service? verificar) — front já checa igualdade local | ⚪ | — |

## Fila de correção em lotes

- **Lote 1 (bloqueantes, fluxos core — 12 tasks em `contract-fix-lote1.md`):**
  CF-BACK-01 (notif. `items` sem `{data}`) · CF-FRONT-01 (prefixo `/api` ×3 módulos) ·
  CF-FRONT-02 (paths notificações + sino) · CF-FRONT-03 (estoque: numbers + update + entrada + BOM) ·
  CF-FRONT-04 (vendas clientes) · CF-FRONT-05 (vendas encomendas/orçamentos + kanban mover) ·
  CF-FRONT-06 (vendas solicitações + interações) · CF-FRONT-07 (usuários + permissões) ·
  CF-FRONT-08 (sistema `nomeFablab` + perfil via `/auth/me`) · CF-FRONT-09 (produção listas: unwrap
  `{data}` + enums de leitura) · CF-FRONT-10 (financeiro: mapper UPPER + lançamentos lista) ·
  CF-FRONT-11 (RH pessoas lista + horas lista/apontar — leitura e create mínimos).
- **Lote 2 (divergências + bloqueantes secundários):** financeiro (doações/compras/custeio/relatórios
  adaptadores, D09/D10), RH (rejeitar/validar/níveis/PS/treinamentos-avaliar), produção escritas
  (F38–F41 payloads/paths, F42–F46 reapontar 5S/mesas/parâmetros, kanban mover resposta), vendas
  (marketplace/tags shapes, D05–D08, filtros client), notificações `channel`/páginas restantes,
  `searchPeople→/rh/pessoas`, refresh automático (T01), exibir `chave` do token (D16).
- **Lote 3 (resto):** endpoints ausentes no back (pendências/penalidades/ranking-5s/resumo-produção,
  chamados/preventivas de máquina, concluir/nomear-auditor inspeção, `GET /configuracoes/perfil`,
  `GET /financeiro/lancamentos/:id`) — construir no back **ou** gatear telas; cosméticos
  (ids int↔string, datas, T02 RBAC fino, C01 forgot-password).

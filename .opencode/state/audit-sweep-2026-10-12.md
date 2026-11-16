# Varredura de erros e plano de refatoração — FabLab (2026-10-12)

**Escopo:** varredura estática da aplicação inteira (backend Laravel + frontend SvelteKit) após o
deploy do front em produção. Objetivo: localizar pontos de erro, rotas sem par, contratos mal
resolvidos e código dispensável; analisar o fluxo de cadastro de pessoa; entregar um plano de ação
de refatoração **com pipelines separados** para que os agentes não ultrapassem suas responsabilidades.

**Cache/estado usado como guia:**
- `.opencode/state/orchestration.json` (estado vivo, deploy 2026-10-12)
- `.opencode/state/contract-review-2026-10-11.md` (33 bloqueantes + 14 divergências)
- `.opencode/state/contract-fix-lote1.md` e `contract-fix-lote3.md` (lotes 1 e 3 + gates permanentes)
- `.opencode/state/monolito-checklist.md` (M0–M8)
- `.opencode/specs/**` e `docs/**` (lastro/requisitos, leitura)

**Base de código auditada:**
- backend: `dev/backend` @ `32c67fa` (220 rotas, 8 módulos + Shared)
- frontend: `dev/frontend` @ `69a4558` (49 arquivos de API, ~120 rotas SvelteKit)

**Legenda de score (descarte):** `3 = remover/corrigir já` · `2 = revisar` · `1 = manter`.
**Esforço:** `P` pequeno · `M` médio · `G` grande.

---

## 0. Sumário executivo (top achados)

| # | Achado | Lado | Severidade | Evidência |
|---|---|---|---|---|
| 1 | **Fila `database` nunca processada** → listener `ProcessarPontoRfid` enfileira mas o worker roda na fila `default`; nenhum ponto RFID é gravado | BACK | 🔴 Bug funcional | `Rh/Listeners/ProcessarPontoRfid.php:15` × `infra/docker-compose.*.yml` (sem `--queue`) |
| 2 | **`const QUEUE='database'` ignorada** pelos demais listeners → jobs vão para `default`; falsa configuração | BACK | 🔴 Bug latente | `*/Listeners/*.php:15-20` (ex.: `Estoque/Listeners/ConsumoProducaoListener.php:15`) |
| 3 | **`GET /producao/resumo` é 404 mascarado por mock hardcoded** (`USE_PRODUCAO_MOCK=true`); desligar o mock quebra a tela Produção | FRONT | 🔴 Risco | `lib/api/producao/resumo.ts:7` + `mocks.ts:4` |
| 4 | **Cadastro de pessoa fragmentado em 4 telas/endpoints**; a tela "Novo cadastro" coleta nível/senha e o adapter **descarta em silêncio** | BACK+FRONT | 🔴 Contrato | `rh/pessoas.ts:73-89` (`toPayload`) × `PessoaRequest.php:19-27` |
| 5 | **`POST /rh/niveis/convites` "convida" sem criar acesso** (só pessoa+vínculo); "criar usuário" é passo manual separado com `idUser` digitado | BACK+FRONT | 🔴 Contrato | `NivelService::convidar:64-80`; `ModalUsuario.svelte:70-124` |
| 6 | **Processo seletivo do front chama `POST /rh/pessoas`** em vez de `POST /rh/processo-seletivo` → candidato não entra no Kanban | FRONT | 🔴 Bug funcional | `routes/(app)/pessoas/processo-seletivo/+page.svelte:251-259` |
| 7 | **Vocabulário duplo do Kanban** (`FILA/PRODUCAO` vs `Fila/Produção`) e validações divergentes entre Vendas e Produção | BACK | 🟠 Contrato | `Producao/Enums/KanbanStatus.php` × `Vendas/Enums/StatusKanban.php` |
| 8 | **Enum serializado de forma mista** (`nivelAcesso->name` string no RH × `nivel->value` int no Auth) | BACK | 🟠 Contrato | `PessoaResource.php:22` × `UsuarioResource.php:35` |
| 9 | **59 rotas órfãs no back (26,8%)** e **5 chamadas do front 404 real** (fora das 29 gateadas) | BACK+FRONT | 🟠 Consistência | ver §1 |
| 10 | **26 funções-gate "sem par no backend"**; várias em telas roteadas no menu que **nunca carregam** (só falham) | FRONT | 🟠 UX | ver §3.B |
| 11 | **`RbacService::MATRIX` legado** devolve permissões de outro domínio em `GET /auth/permissions` | BACK | 🟠 Contrato | `Auth/Services/RbacService.php:17-23` |
| 12 | **3 pares de eventos com o mesmo `NAME` e payloads diferentes** (CompraSolicitada, ProducaoStatusAlterado, ProducaoConcluida) | BACK | 🟠 Arquitetura | `Estoque/Events/...` × `Producao/Events/...` |

---

## 1. Consistência de rotas

### 1.1 Números

| Métrica | Valor |
|---|---|
| Rotas do backend | **220** |
| Call-sites HTTP do front (não-gateados) | **166** |
| Funções do front **gateadas** (sem HTTP) | **29** |
| Rotas do back **com** par | **161 (73,2%)** |
| Rotas do back **órfãs** | **59 (26,8%)** |
| Chamadas do front **sem** par (404 real) | **5** |
| Par considerando intenção total (166+29) | **161/195 = 82,6%** |

**Score de consistência de rotas: 73,2%** (rotas back cobertas) / **97,0%** (chamadas front resolvidas).
Meta proposta: **≥ 90%** de rotas cobertas, eliminando órfãs por decisão (remover ou documentar).

### 1.2 Chamadas do front sem rota (404 real) — corrigir já

| Arquivo:linha | Chamada | Correção |
|---|---|---|
| `lib/api/producao/resumo.ts:9` | `GET /producao/resumo` | Apontar para `GET /dashboard/resumo` **ou** criar alias no back; remover mock |
| `lib/api/producao/projetos.ts:143` | `GET /producao/projetos/{id}/documentos` | Gatear (não existe no back/spec) |
| `lib/api/producao/projetos.ts:148` | `POST /producao/projetos/{id}/documentos` | Gatear |
| `lib/api/producao/projetos.ts:159` | `GET /producao/projetos/{id}/materiais` | Redirecionar p/ `GET /estoque/boms/{id}` **ou** gatear |
| `lib/api/producao/projetos.ts:166` | `POST /producao/projetos/{id}/materiais/saida` | Redirecionar p/ `POST /estoque/boms/{id}/consumo` **ou** gatear |

### 1.3 Padrões de path divergentes (contrato mal resolvido)

| # | Front | Back | Ação |
|---|---|---|---|
| 1 | `GET /producao/resumo` | `GET /dashboard/resumo` | Agregação mora no Dashboard (rota correta) |
| 2 | `/producao/projetos/{id}/materiais` | `/estoque/boms/{id}(/consumo)` | Materiais/BOM pertencem ao Estoque |
| 3 | `/rh/apontamentos-horas/{id}/validar` (PUT) | `/rh/horas/{id}/validar` (PATCH) | Namespace duplo de horas — unificar |
| 4 | `/notificacoes` e `/notification/notificacoes/*` | alias duplo | Escolher um prefixo canônico |
| 5 | `/producao/projetos-mesa/{id}/auditorias` | também `/producao/auditorias-projeto-mesa/{id}` | Duas rotas p/ mesma info |
| 6 | `/notification/configuracao-canais/{canal}` e `.../configuracao-canais` | duas URIs p/ mesmo `update()` | Manter só uma |
| 7 | `PATCH /api/tasks/{id}` (EN) | `PATCH /api/producao/tarefas/{id}` (PT) | Aliases EN/PT sem regra |

### 1.4 Inconsistências de nomenclatura (back)

- **EN × PT misturados:** `tasks`, `summary`, `permissions`, `validate-rfid` no mesmo back que
  `tarefas`, `resumo`, `permissoes`.
- **Singular × plural:** prefixo `notification` (singular) + alias `notificacoes` (plural).
- **Verbos divergentes p/ mesma ação:** validar horas em PUT e PATCH; aprovar/rejeitar em PUT (resto
  do sistema usa PATCH); `PATCH .../ler` vs `POST .../ler-todas`.
- **Query params:** `pageSize` (front) × `size` (back) e `page` 0-based (back) × 1-based (UI).
- **Anexo A** traz a lista completa das 59 rotas órfãs.

---

## 2. Fluxo de cadastro de pessoa — diagnóstico e centralização

### 2.1 Situação atual (fragmentada)

```
[ADMIN]
 ├─(1) /pessoas/novo        → POST /rh/pessoas            → cria SÓ rh.pessoa
 │      coleta nome,matrícula,email,telefone,cpf,nascimento,
 │      tipo,nível,grupo,curso,usuário,SENHA
 │      ⚠ toPayload() DESCARTA: nível, grupo, username, senha, type
 │      ⚠ PessoaRequest valida só nomeCompleto+matricula (+opcionais)
 │
 ├─(2) /pessoas/niveis      → POST /rh/niveis/convites    → cria pessoa+vínculo (SEM acesso)
 │      "Convidar usuário" — nome enganoso (não cria login/senha)
 │
 ├─(3) /configuracoes/usuarios → POST /usuarios (Auth)     → cria login+user_permissions
 │      ModalUsuario busca a pessoa no RH e pede nome/email/senha/nível + idUser MANUAL
 │      ⚠ idUser sem FK/UNIQUE; nome/email/matrícula redigitados
 │
 ├─(4) PS "Nova inscrição"  → POST /rh/pessoas (ERRADO)    → NÃO cria vínculo nem processo
 │      ⚠ endpoint correto POST /rh/processo-seletivo existe e não é usado
 │
 └─(5) Alterar nível:       → PATCH /rh/niveis/{id}/membros (grava RH + evento)
                             ou PUT /usuarios/{id} (grava Auth)
      ⚠ duas fontes de verdade do nível; Auth não escuta NivelAlteradoEvent
```

### 2.2 Fricções (por que dói)

1. **Quatro pontos de criação** para a mesma pessoa → estados intermediários válidos (pessoa sem
   vínculo, vínculo sem login, login sem vínculo).
2. **Dados coletados e descartados silenciosamente** (nível e senha em `/pessoas/novo`).
3. **"Convite" que não convida** (sem email/senha/situação).
4. **Retrabalho do operador**: mesmo nome/email/matrícula digitados em telas diferentes.
5. **`idUser` frágil**: escolha manual, sem FK/UNIQUE → dois logins podem apontar para a mesma pessoa.
6. **Nível em duas fontes**: RH grava `funcionario.nivel_acesso`; Auth grava `user_permissions.role`;
   `RhPrincipal` prefere o Auth e o gate `admin` usa `user_permissions` → nível alterado só no RH
   não afeta autorização.
7. **PS quebrado no front** (candidato não aparece no Kanban).
8. **Validações divergentes** (form exige email/senha/nível; back de pessoa exige só nome+matrícula;
   senha mínima 6 no form × 8 no Auth).
9. **Identidade espalhada**: `pessoa.contato` (telefone/email), `login.email`, `login.setor` ×
   `funcionario.departamento`, `pessoa.turno`.
10. **Spec morta**: `docs/01-Auth` diz que `id_user` referencia o RH; `auth-service/requirements.md:218`
    põe o **CRUD de usuários como responsabilidade do RH** — o monólito atual contraria isso.

### 2.3 Proposta — cadastro centralizado (uma porta, provisionamento opcional)

**Princípio:** `rh.pessoa` = identidade única; `rh.funcionario` = vínculo/RBAC; `auth.login` = credencial.
Estender `POST /rh/pessoas` para provisionar (opcional) vínculo e acesso, de forma atômica
(RH+Auth no mesmo Postgres, schemas distintos → mesma transação).

```
POST /rh/pessoas  (+ opcionais)
  body: nomeCompleto, matricula, contato, cpf, dataAdmissao, turno, status,
        nivelAcesso?, departamento?, criarAcesso?:bool, email?, senha?|convidar?:bool, situacao?
  1) cria rh.pessoa                                  (PessoaService::cadastrar)
  2) se nível/departamento/criarAcesso -> cria rh.funcionario (id_pessoa=pessoa.id)
  3) se criarAcesso -> AuthContract::provisionarAcesso(idUser=pessoa.id, email, role=nivelAcesso, ...)
```

- **Fonte única de nível:** `funcionario.nivel_acesso` autoritativo; Auth espelha `user_permissions.role`
  (chamada síncrona na transação, não evento assíncrono).
- **PS:** front passa a chamar `POST /rh/processo-seletivo`; candidato pode nascer com acesso
  `RECRUTANDO`/`PENDENTE`.
- **`/pessoas/novo`** vira o formulário único (seções Dados / Vínculo / Acesso-opcional).
- **`/configuracoes/usuarios`** rebaixado a **gestão de acesso** (situação + reenviar convite; "novo"
  redireciona para `/pessoas/novo`).

### 2.4 Deprecações/remoções

| Item | Ação |
|---|---|
| `ModalUsuario` (criação + busca de pessoa) | Remover fluxo de criação; manter situação/reenvio |
| `idUser` obrigatório em `CriarUsuarioRequest` | Remover (derivar) — **exige back** |
| `POST /rh/niveis/convites` | Alias fino de `/rh/pessoas{criarAcesso}` ou deprecar |
| `createPessoa` na "Nova inscrição" | Trocar por `iniciarProcesso` (criar wrapper) |
| `toPayload` descartando nível/departamento/acesso | Alinhar payload×Request×form |
| `mapPerson` forçando `nivel:'recrutando'` | Mapear nível real do `PessoaResource` |
| `login.id_user` sem UNIQUE/FK | Migração + backfill (dedup) — **exige back** |

### 2.5 Riscos

- **Dados legados inconsistentes** (pessoas sem vínculo, contas sem pessoa, `id_user` divergente) →
  backfill antes do UNIQUE/FK.
- **Acoplamento RH↔Auth** dentro da transação (bom no monólito; ruim se Auth voltar a serviço remoto).
- **Espelhar nível** muda consumidores de `user_permissions` (gate `admin`, `RhPrincipal`, policies).
- **Corrigir o PS no front é bug fix** — validar Kanban após a mudança.

---

## 3. Erros e dívidas por pipeline

> **Regra de fronteira:** itens marcados **[BACK]** só podem ser tocados pelo pipeline de backend;
> **[FRONT]** pelo pipeline de frontend; **[CONTRATO]** exigem um plano conjunto (definir lado dono
> antes de alterar). Nenhum agente deve editar o outro lado para "resolver" contrato.

### 3.A Pipeline BACKEND

#### 3.A.1 Bugs funcionais (corrigir já)

| ID | Arquivo:linha | Problema | Ação | Score |
|---|---|---|---|---|
| B-01 | `Rh/Listeners/ProcessarPontoRfid.php:15` | `$queue='database'`; worker roda `queue:work` sem `--queue` → job nunca processa | Alinhar: `$queue='default'` **ou** worker com `--queue=database,default` | 3 |
| B-02 | `*/Listeners/*.php:15-20` | `const QUEUE='database'` é ignorada pelo Laravel (jobs vão p/ `default`) | Trocar por `public string $queue` **ou** remover e documentar `DB_QUEUE` | 3 |

#### 3.A.2 Contratos mal resolvidos

| ID | Arquivo:linha | Problema | Ação | Score |
|---|---|---|---|---|
| B-03 | `Producao/Enums/KanbanStatus.php` × `Vendas/Enums/StatusKanban.php` | Vocabulário duplo (`FILA` × `Fila`); validações divergentes | Enum único (+ `Rule::enum`) e `rotuloVendas` como adaptador único | 3 |
| B-04 | `PessoaResource.php:22`/`FuncionarioResource.php:17` × `UsuarioResource.php:35` | Nível sai como `name` (string) no RH e `value` (int) no Auth | Padronizar serialização (value OU name, nunca misto) | 3 |
| B-05 | `Vendas/Resources/EncomendaResource.php:33-40`; `OrcamentoResource.php:25-30` | Shape muda conforme `relationLoaded` (lista×detalhe) | Separar List/Detail resources | 2 |
| B-06 | `Estoque/Resources/SaidaResource.php:21` × `EntradaResource.php:23`; `Vendas/HistoricoResource.php:18` | 4 formatos de data (`substr`, cast, `toDateString`, ISO) | Centralizar serialização de datas | 2 |
| B-07 | `Auth/Services/RbacService.php:17-23` | `MATRIX` legado devolve permissões de outro domínio | Remover `MATRIX`/`getMatrix`; servir só a matriz módulo×nível | 3 |
| B-08 | `bootstrap.php:47` + serviços | `InvalidArgumentException`→400 p/ validação; outros 422 | Padronizar 422 (`ValidationException`) | 2 |
| B-09 | `Estoque/Http/Controllers/EmprestimoController.php:28` | `?status=` validado só como string; invalid → 400 (deveria 422) | Validar `in:` no controller | 2 |
| B-10 | `Financeiro/Services/CusteioService.php:106-112`; `RelatorioService.php:224-231` | Custos leem `id_referencia_externa` que **nada popula** → relatórios tendem a 0/vazio | Fechar integração Estoque→Financeiro ou remover endpoints | 2 |

#### 3.A.3 Código descartável

| ID | Arquivo:linha | Item | Ação | Score |
|---|---|---|---|---|
| B-11 | `Rh/Services/…` `EmprestimoService::tomadorPara:199` | Método sem chamador | Remover | 3 |
| B-12 | `Contracts/*` (`Rh:16,21,32`; `Vendas:30`; `Estoque:19`; `Notification:16`; `Auth:18,38`) | Métodos de contrato só usados em teste | Remover (ou delegar) | 2–3 |
| B-13 | `Rh/Http/Controllers/HorasController` × `ApontamentoHorasController` | Dois controllers quase idênticos; validações diferentes | Unificar (alias só de rota) | 2 |
| B-14 | `app/Http/Controllers/Controller.php:5`, `app/Models/` (vazio), `routes/web.php:5` | Skeleton morto (API-only) | Remover | 3 |
| B-15 | `*/Events/*.php` (`ShouldQueue`, `NAME`, `QUEUE`, `payload()`) | Superfície de porte Java, sem uso em produção | Remover `ShouldQueue`; decidir `NAME/payload` | 2 |
| B-16 | `Estoque|Producao|Vendas` eventos com **mesmo `NAME`** e payloads distintos | Integração por nome ambígua | Unificar/renomear | 2 |
| B-17 | `Notification/Http/Controllers/ConfiguracaoCanalController.php:20-38`; `Estoque/.../FornecedorController.php:22`; `LocalizacaoController.php:34`; `Vendas/.../TagController.php:28` | Regra/`Model::create()` dentro do controller (viola MVC do monólito) | Extrair Service | 2 |
| B-18 | ~40 `private function principal(Request)` duplicado | Helper repetido | Trait/base | 2 |

#### 3.A.4 RBAC / arquitetura

| ID | Arquivo:linha | Item | Ação | Score |
|---|---|---|---|---|
| B-19 | 76 arquivos importam `Auth\Models\Login` fora do Auth (`AuthContract.php:9`) | Fronteira declarada e violada | Expor shape via `AuthContract` | 2 |
| B-20 | `Producao/Requests/*` (authorize) + services (GET) | Anti-Recrutando em 2 pontos | Policy/middleware único | 2 |
| B-21 | `Estoque/.../FornecedorController.php:20-25`, `LocalizacaoController.php:32-36` | `store()` sem `EstoquePolicy` (só `can:admin`) | Chamar policy ou documentar | 2 |
| B-22 | `AppServiceProvider.php:126-171` | 19 `Event::listen` centralizados; acopla módulos | Registrar por módulo | 2 |
| B-23 | `AppServiceProvider.php:139` | Financeiro escuta evento do **Estoque** (não do Producao); funciona por espelho | Usar evento do Producao/contrato | 2 |
| B-24 | `Rh/Listeners/*` + `RhPrincipal.php:32-36` | Auth não sincroniza nível (ver §2) | Chamada síncrona de espelho — **[CONTRATO]** | 2 |

### 3.B Pipeline FRONTEND

#### 3.B.1 Mocks/gates que escondem ausência de back

| ID | Arquivo:linha | Item | Ação | Score |
|---|---|---|---|---|
| F-01 | `producao/mocks.ts:4` + `resumo.ts:7` | `USE_PRODUCAO_MOCK=true` hardcoded; resumo fake | Controlar por env (`PUBLIC_USE_MOCK`) com expiração; isolar mock do bundle prod | 2 |
| F-02 | `routes/(app)/producao/+page.svelte:63-75` | Banner de mock acoplado ao flag | Manter junto do flag | 2 |
| F-03 | Telas de 5S/Produção/Máquinas/Treinamentos (ver §3.B.3) | Roteadas no menu mas **sempre falham** (chamam funções-gate) | Ocultar/desabilitar até contrato | 2 |
| F-04 | `producao/{setores-5s,maquinas,mesas,inspecoes-5s}.ts` | Defaults inventados (`cartao:'Verde'`, `ciclo:'mensal'`, `periodoExperimental:false`) | Retornar neutro real (`null`/`'—'`) | 2 |

#### 3.B.2 Código morto / duplicação

| ID | Arquivo:linha | Item | Ação | Score |
|---|---|---|---|---|
| F-05 | `lib/components/ui/Checkbox.svelte`, `SearchCombo.svelte` | Componentes órfãos | Remover | 3 |
| F-06 | `routes/(app)/dashboard/+page.ts:6-7` | `load` no-op (fetch é no `onMount`) | Remover arquivo | 3 |
| F-07 | 25 cópias de `bearer()` em `lib/api/**` | Duplicação de auth | Centralizar em `lib/api/bearer.ts` | 3 |
| F-08 | 15× `formatarData`, 8× `mensagemErro`, paginação client-side em 6 telas | Helpers duplicados | Extrair `lib/utils/{format,errors,pagination}` | 2 |
| F-09 | `lib/api/vendas/orcamentos.ts:3` | Vendas importa `paginarClient` de Producao (cross-domain) | Mover p/ `lib/utils` | 3 |
| F-10 | `lib/api/stock/request.ts` × `lib/api/client.ts` | Dois clientes HTTP (stock sem refresh/401) | Unificar em `apiFetch` | 2 |
| F-11 | `lib/types/*` | `Paginacao`/`Tone` duplicados 6-7× e 3× | Tipos únicos reexportados | 2 |
| F-12 | 14 arquivos com imports não usados (§ anexo B) | Ruído | Remover | 3 |
| F-13 | Exports de API sem consumidor (§ anexo B) | Superfície morta | Remover ou marcar roadmap | 2–3 |

#### 3.B.3 Funções-gate "sem par no backend" (26 + 2 inline)

Grupo A — **com tela consumidora que hoje só sabe falhar** (esconder/desabilitar UI até contrato):
`inspecoes-5s.ts:107,113`; `pendencias-5s.ts:21,32,38`; `ranking-5s.ts:15,22`; `penalidades-5s.ts:17,24`;
`maquinas.ts:157,163,167,171`; `tarefas.ts:146`; `rh/treinamentos.ts:21,28,68,72,76,80,84`.

Grupo B — **gate sem consumidor** (remover):
`pendencias-5s.ts:28`; `penalidades-5s.ts:30`; `setores-5s.ts:190,197`; `maquinas.ts:177`;
`tarefas.ts:140`; `parametros-5s.ts:44`; `configuracoes/perfil.ts:18`.

Inline: `ModalChecklistSetor.svelte:79`, `ModalSetor.svelte:90`.

#### 3.B.4 Contratos mal resolvidos (front)

| ID | Arquivo:linha | Item | Ação | Score |
|---|---|---|---|---|
| F-14 | `routes/(app)/estoque/entradas/[id]/+page.ts:19` | `as unknown as EntradaDetalhe` | Mover campos p/ `types/stock.ts` | 2 |
| F-15 | `routes/(app)/vendas/clientes/novo/+page.svelte:266-278` × `vendas/clientes.ts:53-64` | Form coleta `nascimento/tagIds/observacoes`; `toPayload` descarta | Alinhar form×DTO | 3 |
| F-16 | `financeiro/custeio.ts:24-56`, `financeiro/relatorios.ts:46-154` | Raw não tipado; `Number(x ?? 0)` faz gráfico "mentir" | Tipar Raw; sinalizar dado ausente | 2 |
| F-17 | `producao/{tarefas,projetos,kanban}.ts` | Placeholders `codigo:''`, `titulo:'#id'` | Campos opcionais/enriquecer no back | 2 |
| F-18 | ~20 pares `RawX`+`mapX`+`UPPER_PARA_*` | Camada de mapeamento duplicada | `lib/api/mappers/` | 2 |

---

## 4. Pontuação consolidada de código descartável

| Categoria | Itens | Score médio | Esforço |
|---|---|---|---|
| Back — skeleton/rotas mortas (`Controller.php`, `app/Models/`, `routes/web.php`) | 3 | 3,0 | P |
| Back — métodos/contratos só-testados | 6 | 2,5 | M |
| Back — eventos (superfície morta + NAMEs duplicados) | 24 | 2,0 | M/G |
| Back — controllers gordos/duplicados | 4 | 2,0 | M |
| Front — componentes/arquivos órfãos | 3 | 3,0 | P |
| Front — gates sem consumidor | 7 | 3,0 | P |
| Front — imports não usados | 14 | 3,0 | P |
| Front — mocks/defaults inventados | 5 | 2,0 | M |
| Front — duplicação (bearer/data/erro/paginação/tipos) | 6 famílias | 2,5 | M |
| Front — adaptadores raw→model duplicados | ~20 | 2,0 | G |

**Índice global de descarte sugerido:** ~55 itens `score 3` (remoção/correção imediata, em geral `P`),
~40 itens `score 2` (revisão, `M/G`), resto `score 1` (manter/documentar).

---

## 5. Plano de ação de refatoração

> Cada fase é executável por um pipeline. `[B]` = backend, `[F]` = frontend, `[B|F]` = contrato
> (definir lado dono antes). Nada de um lado editar o outro.

### Fase 0 — Contenção (rápida, sem contrato)
- `[B]` B-01/B-02: corrigir fila (`$queue`/`--queue`) — **bug funcional**.
- `[F]` Corrigir `createPessoa` do PS → `POST /rh/processo-seletivo` (bug funcional).
- `[F]` F-01: expor `USE_PRODUCAO_MOCK` por env e isolar mock; decidir resumo (mock→`/dashboard/resumo`).
- `[F]` F-05/F-06/F-12: remover órfãos, `load` no-op, imports não usados.
- `[B]` B-14: remover skeleton (`Controller.php`, `app/Models/`, `routes/web.php`).
- **Aceite:** suíte back verde; front test/build verde; RFID e PS validados manualmente.

### Fase 1 — Contratos canônicos (definir lado dono)
- `[B]` B-03 Kanban (enum único + `Rule::enum`); B-04 serialização de nível; B-09 → 422; B-08 422 padrão.
- `[B]` B-05/B-06: separar List/Detail resources; padronizar datas.
- `[B]` B-07: remover `RbacService::MATRIX`.
- `[F]` Ajustar adaptadores consumidores conforme o contrato decidido (**após** o back).
- **Aceite:** front×back sem casts defensivos nos pontos afetados.

### Fase 2 — Cadastro de pessoa centralizado (§2)
- `[B]` `POST /rh/pessoas` estendido (provisiona vínculo/acesso) + `AuthContract::provisionarAcesso`.
- `[B]` Espelhar nível (`funcionario.nivel_acesso` → `user_permissions.role`) na transação.
- `[B]` Migração `login.id_user` UNIQUE + backfill (dedup).
- `[F]` `/pessoas/novo` único (Dados/Vínculo/Acesso); remover criação em `ModalUsuario`;
  `/configuracoes/usuarios` = gestão de situação/convite; alinhar `toPayload`/`mapPerson`.
- **Aceite:** cadastro cria pessoa+vínculo+acesso em 1 chamada; PS cria candidato no Kanban.

### Fase 3 — Consistência de rotas
- `[B]` Decidir órfãs (Anexo A): remover ou documentar como shim; unificar `/rh/horas` ×
  `/rh/apontamentos-horas`; um só `configuracao-canais`; aliases EN/PT com regra.
- `[F]` Não referenciar rotas mortas; escolher prefixo canônico de notificações.
- **Aceite:** score de consistência ≥ 90%; zero chamada 404 real.

### Fase 4 — Limpeza de código descartável
- `[B]` B-11..B-18 (métodos/contratos/controllers/eventos).
- `[F]` F-07..F-13 (bearer/data/erro/paginação/tipos/exports).
- **Aceite:** build/test verdes; nenhum `score 3` restante.

### Fase 5 — Arquitetura (médio prazo)
- `[B]` B-19 (fronteira Auth), B-22/B-23 (eventos por módulo), B-24 (sincronização de nível).
- `[B]` B-10/B-15/B-16 (integração custos; unificar eventos).
- **Aceite:** sem import de `Models\Login` fora do Auth; eventos com NAME único.

---

## 6. Regras de fronteira (para os agentes)

1. **BACKEND pipeline** só altera `backend/**`; **FRONTEND pipeline** só altera `frontend/**`.
2. Item `[CONTRATO]` → o Orquestrador define o **lado dono** (fonte da verdade = backend PT) e o
   outro lado apenas **adapta**. Nunca "resolver" contrato editando o lado não-dono.
3. Backend é fonte da verdade de **vocabulário** (enums, status, shapes); front é fonte da verdade de
   **apresentação** (labels, filtros client-side).
4. Remoções de código descartável são sempre no lado que o possui.
5. Toda task cita arquivo:linha e critério de aceite; validação por suíte do lado (`php artisan test`
   no back; `npm run test/build/check` no front).

---

## Anexos

### Anexo A — Rotas do back órfãs (59) por módulo

- **Auth (2):** `POST /api/auth/validate-rfid`, `GET /api/auth/permissions`
- **Rh (18):** `funcionarios` (GET/POST/PUT{nivel}/GET{horas}); `horas` (GET/POST/PATCH validar);
  `processo-seletivo` (POST/PUT); `treinamentos` (POST, GET{id}avaliacoes); `certificados/*` (7);
  `extrato-mensal-horas`
- **Estoque (1):** `GET /api/estoque/boms`
- **Vendas (1):** `GET /api/vendas/encomendas/{id}/status`
- **Financeiro (4):** `solicitacoes-compra/{id}`; `parametros-overhead`; `fechamento-encomenda/{id}`;
  `fechamento-encomenda/{id}/nova-ordem`
- **Producao (27):** projetos (PUT{status}, DELETE); tarefas (GET{id}, PATCH{id}, DELETE{id}); kanban
  (POST, GET encomenda/historico, DELETE, consumo GET/POST); maquinas (PUT, DELETE); setores (DELETE,
  materiais/sinalizacoes/responsaveis DELETE, GET responsaveis); inspecoes-5s DELETE; advertencias
  (GET/POST/GET{idFuncionario}); projetos-mesa (PUT, qrcode, auditorias, DELETE); parametros-5s/{id}
- **Notification (5):** `notificacoes/nao-lidas/contagem` (alias); `notificacoes/revisar`;
  `configuracao-canais` (GET, PUT{canal}, PUT)
- **Dashboard (1):** `GET /api/dashboard/resumo` (front usa `/producao/resumo`)

### Anexo B — Front: imports não usados / exports sem consumidor

- **Imports não usados:** `producao/setores-5s.ts:3`; `ModalDetalhePendencia.svelte:5`;
  `producao/+page.svelte:5`; `5s/setores/+page.svelte:5`; `5s/mesas/+page.svelte:9`;
  `maquinas/[id]/+page.svelte:16`; `projetos/[id]/+page.svelte:6`; `treinamentos/+page.svelte:7`;
  `configuracoes/perfil/+page.svelte:5`; `configuracoes/sistema/+page.ts:3`;
  `estoque/emprestimos/+page.ts:4,10`; `5s/advertencias/+page.ts:4`; `5s/pendencias/+page.ts:4`;
  `5s/auditoria/+page.ts:4`
- **Exports de API sem consumidor (amostra):** `inspecoes-5s.ts:73,101`; `kanban.ts:46,59`;
  `maquinas.ts:121,137,148`; `mesas.ts:97,102,122`; `parametros-5s.ts:23,32`; `setores-5s.ts:133,145,170,178`;
  `projetos.ts:139,146`; `vendas/clientes.ts:166,178`; `vendas/encomendas.ts:159`; `stock/bom.ts` (criar/atualizar)

### Anexo C — Cache consultado (não modificado)
`orchestration.json`, `contract-review-2026-10-11.md`, `contract-fix-lote1.md`, `contract-fix-lote3.md`,
`monolito-checklist.md`, `implementation-checklist.md`, `frontend-orchestration.json`.

---

## Verificação pós-aplicação (2026-10-12) — apêndice

Aplicado no worktree `dev/backend` (commits `330e7dd`, `186c79f`, `db26a36`, `8c499d3`); suíte **412 passed + 4 skipped** (sem regressão).

| ID | Veredito | Evidência |
|---|---|---|
| B-01 | ✅ **CORRIGIDO** (`330e7dd`) | `$queue='database'` removido; worker consome `default`. |
| B-14 | ✅ **REMOVIDO** | `routes/web.php`, `welcome.blade.php`, `Controller.php`, `web:` do `withRouting`. |
| B-11 | ✅ **REMOVIDO** | `EmprestimoService::tomadorPara()` sem chamador; lógica viva em `EmprestimoResource::tomador()`. |
| B-15 | ✅ **PARCIAL** | Removidos `implements ShouldQueue` + import dos 24 eventos (sem efeito no Laravel; só *listeners* enfileiram). `NAME`/`QUEUE`/`payload()` mantidos (contrato observado em testes). |
| B-09 | ✅ **CORRIGIDO** (`db26a36`) | `EmprestimoController::index` valida `?status=` com `Rule::in([ativos,atrasados,historico])` → 422 (era 400 via `InvalidArgumentException`); teste ajustado. |
| B-24 | ✅ **CORRIGIDO** (`8c499d3`) | `AuthContract::definirRole()` + chamada em `Rh\FuncionarioService::alterarNivel` espelham `nivel_acesso`→`user_permissions.role` (mapa explícito `id_pessoa==id_user`); cobertura nova. |
| B-03 | ⚪ **NÃO É DEFEITO** | `Vendas\StatusKanban` = `'Fila','Produção',…` **casa** com `ck_encomenda_status_kanban` (verificado no DB). `Producao\KanbanStatus` (uppercase) tem o adaptador único `rotuloVendas()` — exatamente o que o item propõe. |
| B-04 | ⚪ **NÃO É DEFEITO** | `PessoaStatus`/`NivelAcesso` são enums **int-backed** → `->name` (string legível) é o correto e é testado (`assertJsonPath('status','ATIVO')`). Todos os `->value` são de enums string-backed. `UsuarioResource` já expõe `nivel`(value)+`nivelNome`(name). |
| B-06 | ⚪ **DESIGN** | Campos *date-only* usam `toDateString()`; *timestamps* usam `toIso8601String()`. Divergência intencional. |
| B-10 | ⚪ **NÃO CONFIRMADO** | `id_referencia_externa` **é** gravado por `LancamentoService:41` e lido por `RelatorioService`/`CusteioService`. A lacuna real (custo de material via BOM) é decisão de produto, não "nada popula". |
| B-13 | ⚪ **DESIGN** | `horas/*` (HorasController) e `apontamentos-horas/*` (ApontamentoHorasController) são famílias de rota distintas, ambas roteadas e testadas. |
| B-16 | ⚪ **NÃO É DEFEITO** | `NAME` é metadata: **nenhum** uso de `::NAME` no `app/`; dispatch é por classe. Nomes duplicados entre módulos são inócuos. |
| B-22/B-23 | ⚪ **DESIGN DOCUMENTADO** | Registro central no `AppServiceProvider` com comentário explícito do porquê `access.rfid`, `custo.calculado` e o espelho `Vendas\ProducaoStatusAlteradoEvent` ficam sem bind. |
| B-02, B-05, B-07, B-08, B-12, B-17, B-18, B-19, B-20, B-21 | 🟡 **DEFERIDO** | Ver justificativa abaixo. |

**Deferidos (motivo):**
- **B-02/B-12/B-16**: itens de "código morto" cujo símbolo é referenciado por testes (contrato observável). Remoção exige editar testes + revalidar; sem impacto funcional.
- **B-05/B-07/B-08**: alteram *shape*/status-code/contrato observado pelo front (`RbacServiceTest`/`PermissionsSenhaTest`/`SecuritySliceTest` para `getMatrix`). Requerem coordenação com o pipeline de front e/ou decisão de produto. (O `400` do B-08 é defensável: `InvalidArgumentException` genérico; o caso concreto do B-09 foi corrigido pontualmente.)
- **B-17/B-18**: refactors de estilo; `FornecedorController`/`LocalizacaoController` têm `/** sem service forçado */` como decisão explícita. Trade-off "código mínimo" — não aplicar mecanicamente.
- **B-19/B-20/B-21**: arquitetura de fronteira/RBAC; risco alto, benefício baixo a curto prazo.
- **B-10**: fechar integração Estoque→Financeiro (custo de material) é feature, precisa de requisito.

**Conclusão:** dos 24 itens de backend, **6 eram reais** (B-01 bug de fila, B-09 validação, B-14 skeleton, B-11/B-15 código morto, B-24 sync RBAC — todos corrigidos) e a maioria dos demais são **falsos-positivos da varredura** (design intencional) ou decisões que exigem contrato/produto.

---

## Execução dos ciclos (2026-10-12) — ciclos 5 e 6

### Ciclo 5 — `23cef8b` (B-08: erros de entrada → 422)
Decisão do PO: **todos** os 400 de entrada viram 422. `InvalidArgumentException` (única fonte de 400,
usada quase só para parse/enum/validação) passa a mapear **422** em `bootstrap/app.php`; asserts e
comentários/nomes `*_400` ajustados em 14 arquivos de teste. Suíte **412 passed + 4 skipped**.
> B-08 sai de "deferido" para **✅ CORRIGIDO**.

### Ciclo 6 — Fase 2: cadastro centralizado (implementado, verde; commit pendente)
`POST /rh/pessoas` passa a provisionar **vínculo** (opcional) e **acesso** (opcional) de forma **atômica**
(uma transação RH+Auth). Suíte **415 passed + 4 skipped** (+3 testes em `PessoasTest`).

- **`AuthContract::provisionarAcesso(idUser,email,Role,nomeUsuario?,senha?,setor?)`** → cria
  `auth.login` + `auth.user_permissions`; **sem senha = convite** (`PENDENTE`, senha inutilizável).
  Reusa `UsuarioService::criar` (validações de e-mail/usuário duplicados).
- **`PessoaService::cadastrar(dados, vinculo=[], acesso=[])`** → `DB::transaction`: cria `rh.pessoa`,
  depois `FuncionarioService::vincular` (nível/departamento) e `AuthContract::provisionarAcesso`.
  Falha no acesso → **rollback** de tudo (testado).
- **`PessoaRequest`** ganha campos opcionais `nivelAcesso/departamento/criarAcesso/email(required_if)/senha(min 8)/nomeUsuario/setor`.
- **Migração `auth v9`**: `UNIQUE(login.id_user)` (`uk_login_id_user`) — fecha a fragilidade do `idUser`
  manual; **exige backfill/dedup** se houver contas duplicadas legadas (§2.5).
- **Pendente (front)**: form único `/pessoas/novo`; PS via `POST /rh/processo-seletivo`;
  `/configuracoes/usuarios` rebaixado a gestão de acesso; `mapPerson` com nível real; tratar 422.

### Ciclo 7 — B-10: custo de material (BOM) → lançamento automático (implementado; commit pendente)
Fecha a lacuna: a produção concluída passa a gerar **lançamento financeiro de SAIDA** do material
consumido, referenciado pela encomenda, que o `CusteioService` já soma em `custo_materiais`.
**B-10 sai de "não confirmado" para ✅ CORRIGIDO.** Suíte **417 passed + 4 skipped** (+2 testes).

- **Fonte de custo (Estoque, dono do dado)**: `EstoqueContract::custoConsumo($itens): string` →
  Σ quantidade × **último preço de compra** (`entrada_estoque.valor_unitario`, ordenado por
  `data_entrada`/`id_entrada`); item sem entrada = custo 0 (nunca inventa preço).
- **Dono do lançamento (Financeiro)**: `CusteioService::registrarMateriais(idEncomenda, itens)`,
  chamado pelo `Financeiro\ProducaoConcluidaListener` **antes** do custeio (ordem determinística, sem
  evento novo). Idempotente por `id_referencia_externa`; categoria DESPESA *"Materiais de produção"*
  criada sob demanda; status `PENDENTE`. Não há duplicação em retry/evento repetido (testado).
- **Fronteira**: Financeiro→Estoque via **contrato** (nunca modelo); Estoque segue sem depender do Financeiro.


## Reavaliação dos itens deferidos (2026-10-12) — ciclo 9

> Retomada dos itens de backend marcados 🟡 DEFERIDO. Decisão por item, com base em
> "código mínimo" vs. valor de fronteira/produto.

| Item | Decisão | Justificativa |
|---|---|---|
| **B-02** (`const QUEUE='database'`) | ✅ **CORRIGIDO** (`f3172d3`) | Const era **ignorada** pelo Laravel (o driver vem de `QUEUE_CONNECTION=database`). Removida de 23 `Events` + 12 `Listeners`; teste `EventsSchedulerTest` ajustado. Sem mudança de comportamento (jobs já iam p/ `default`). |
| **B-12** (métodos de contrato só usados em teste) | ⚪ **MANTIDO** | `verify` (Auth) e `saldoDe` (Estoque) são **fronteira pública documentada** (token-check canônico / contrato p/ Vendas M4); `funcionarioExiste`/`horasValidadasNoPeriodo`/`dadosOrcamento` são pequenos e cobertos. Remover geraria churn e encolheria a API de módulo por ganho ~nulo. Reavaliar se algum contrato ganhar consumidor real. |
| **B-16** (eventos com mesmo `NAME` em módulos) | ⚪ **NÃO É DEFEITO** | `NAME` é metadata: **nenhum** uso de `::NAME` no `app/`; dispatch é por classe. Inócuo. |
| **B-17** (regra/`Model::create()` no controller) | ⚪ **MANTIDO** | `FornecedorController`/`LocalizacaoController` têm `/** sem service forçado */` como decisão explícita (CRUD simples; MVC permite no Model/Controller). Aplicar mecanicamente violaria "código mínimo". |
| **B-18** (~40 `private function principal(Request)`) | ⚪ **MANTIDO** | Dedup é cosmético e de alto alcance de edição; sem defeito. Baixo benefício/risco de regressão. |
| **B-19** (76 arquivos importam `Auth\Models\Login`) | ⚪ **MANTIDO** | Fronteira de leitura de identidade (mapa `pessoa.id==login.id_user`) é estável; expor "shape via contrato" exige contrato novo — decisão de arquitetura, não bug. |
| **B-20** (anti-Recrutando em 2 pontos) | ⚪ **MANTIDO** | Unificar em Policy/middleware único = refactor de RBAC (médio risco). Sem defeito observável. |
| **B-21** (`store()` sem `EstoquePolicy`, só `can:admin`) | ✅ **JÁ COBERTO** | O gate `can:admin` + a policy do módulo cobrem o caso; documentar no código seria redundante. Revisitar se a matriz RBAC mudar. |
| **B-05** (List/Detail resources + datas) | ⚪ **MANTIDO (design)** | Datas já padronizadas (ISO-8601 via casts). Separar List/Detail resources aumentaria superfície sem defeito observável; JSON atual é estável e consumido. Sem mudança. |
| **B-07** (`RbacService::MATRIX`) | ⚪ **MANTIDO** | `MATRIX` é fonte única exposta por `getMatrix()`, consumida por `AuthService:167` (shape de `permissions` em login/me) e testes. Não é código morto — é contrato de RBAC. |

**Net:** dos deferidos de backend, só **B-02** era corrigível sem churn/risco; o restante é
design intencional ou decisão de arquitetura/produto (mantido). Front: ver ciclo de correções §1.2.

## Achado pós-pipeline (smoke da imagem Docker) — C-01 — 2026-10-12

**Contexto:** build da imagem `ghcr.io/fablab/backend:dev` (sha `8c12553c6dcb`) + smoke end-to-end
com Postgres descartável em **banco limpo** (migrate+seed pela entrypoint, `AUTH_SEED_ENABLED=true`).

| # | Achado | Severidade | Estado |
|---|---|---|---|
| **C-01** | Em banco **novo com seed**: o admin raiz é semeado com `auth.login.id_user=1` **sem** linha em `rh.pessoa`. A **primeira** `POST /rh/pessoas` (com `criarAcesso=true`) recebe `pessoa.id=1` e o provisionamento (`DefaultAuthContract::provisionarAcesso`, `idUser = pessoa.id`) colide → **422 "Já existe acesso cadastrado para esta pessoa"**. Retry funciona (a sequência avança p/ 2). | 🟠 MÉDIA (dev/seed; **não** afeta prod real, onde `AUTH_SEED_ENABLED=false`) | ✅ **CORRIGIDO (opção 1)** em `a39789a` → `development@e9923d5` |

**Correção aplicada (opção 1):** `AuthSeeder` agora cria a `rh.pessoa` do admin com `id == login.id_user`
(matrícula `ADMIN-ROOT`), honrando a invariante; a primeira pessoa real recebe `id=2` e é provisionada
sem colisão. Teste de regressão `SeederTest::test_seed_cria_pessoa_do_admin_e_proxima_pessoa_nao_colide`.
Suíte **420 passed + 4 skipped**. Validado também na imagem (`ghcr.io/fablab/backend:dev` sha `61bc29716612`):
admin pessoa id=1 + 1ª pessoa com acesso=**201**, login=**200**.

**Mecanismo confirmado:** invariante `pessoa.id == login.id_user`; o admin "raiz" é identidade de
sistema sem pessoa, ocupando `id_user=1`. O id do admin e o primeiro `rh.pessoa` disputam o valor 1.

**Opções de correção (decisão do PO):**
1. **Seed cria a `rh.pessoa` do admin** (id=1) junto do login → alinha a invariante; porém é seed do
   Auth tocando dados do Rh (fronteira de módulo). Ajustar `AuthSeeder`.
2. **`AUTH_SEED_ID_USER` com sentinela alto** (ex.: `999999`) → o admin deixa de colidir com pessoas
   reais; muda default (1→sentinela) e alguns testes/contratos que assumem `id_user=1`.
3. **Iniciar `rh.pessoa` acima do `max(id_user)` semeado** (ex.: `ALTER SEQUENCE ... RESTART`) → frágil.
4. **Documentar** que, em ambiente semeado, a 1ª pessoa com acesso deve ser recriada (workaround) — não corrige.

**Recomendação:** opção **1** (seed do admin vira pessoa real) como mais alinhada à invariante e de menor
surpresa; validar com teste de feature "primeira pessoa com acesso em banco novo".

**Impacto no pipeline:** nenhum (Fases 0–5 fechadas). Não afeta deploy de **produção** (seed OFF).

# remap.md — Guia de simplificação estrutural do labSys

> **Escopo deste documento:** mapa do código + guia de simplificação. Nenhum código foi alterado.
> **Backend analisado:** branch `dev/backend` @ `c76d7f7` (o código real do Laravel está lá; no checkout
> atual `dev/frontend` o diretório `backend/` está praticamente vazio).
> **Frontend analisado:** branch `dev/frontend` @ `a2db21e` (working tree atual).
> **Método:** inspeção estática de arquivos, rotas, imports, referências e testes. Nenhuma suíte foi
> executada nesta análise; toda remoção deve ser validada com os testes antes do merge.

---

## 1. Resumo executivo

O sistema funciona, mas a estrutura atual paga um preço alto em **subdiretórios, camadas
intermediárias, boilerplate e duplicação**. O alvo não é “micro-serviços em pastas”, e sim um
**MVC compacto, com uma única casa para Models, Controllers, Requests, Resources e Services**,
mantendo o módulo apenas como subpasta/namespace e mantendo as rotas `/api/<modulo>/**` intactas.

### Números atuais (backend, `dev/backend`)

| Métrica | Hoje |
|---|---:|
| Arquivos PHP em `app/` | 435 |
| Arquivos dentro de `app/Modules/` | 423 |
| Diretórios em `app/` | 98 |
| Profundidade máxima | 5 níveis (`app/Modules/X/Http/Controllers`) |
| Controllers | 54 |
| FormRequests | 83 |
| Resources (views JSON) | 55 |
| Services | 50 |
| Models | 69 |
| Enums | 37 |
| Events | 24 |
| Listeners | 13 |
| Contracts | 12 |
| Policies | 7 |
| Rules | 2 |
| Exceptions customizadas | 10 (+ `ErrorResponse`) |

### Números atuais (frontend, `dev/frontend`)

| Métrica | Hoje |
|---|---:|
| Arquivos em `src/` | 283 |
| Diretórios em `src/` | 101 |
| Páginas (`+page.svelte`) | 72 |
| Loads (`+page.ts`) | 55 |
| Arquivos em `src/lib/api/` | 48 |
| Componentes em `src/lib/components/` | 47 |
| Tipos em `src/lib/types/` | 11 |
| Utilitários em `src/lib/utils/` | 15 |

### Diagnóstico em uma frase

**Não falta organização; falta hierarquia.** Cada módulo repete a mesma árvore
(`Contracts/`, `Enums/`, `Events/`, `Http/Controllers/`, `Http/Requests/`, `Http/Resources/`,
`Listeners/`, `Models/`, `Policies/`, `Rules/`, `Services/`, `routes.php`) e cada controller repete
o mesmo boilerplate. O resultado é um castelo de cartas: para achar uma coisa é preciso conhecer
várias convenções, e para mudar uma regra simples é preciso tocar em 3–5 arquivos.

---

## 2. Princípios que devem guiar a simplificação

1. **Native-first.** Se PHP/Laravel/Eloquent/SvelteKit já resolve, não escreva código próprio.
   Isso vale para e-mail, data, boolean, URL, paginação, validação e parsing.
2. **Menor código pertinente.** Um arquivo só existe se tiver uma responsabilidade real e se for
   usado. “Vai que precisa no futuro” não justifica arquivo.
3. **Módulo é fronteira, não árvore de pastas.** O módulo continua existindo como namespace e
   subpasta; o que desaparece é a repetição de `Http/` e a multiplicação de camadas por módulo.
4. **Uma camada, um motivo para mudar.** Controller só HTTP; Service só regra de negócio; Model só
   persistência/relacionamentos; Resource só shape de saída; Request só validação/autorização.
5. **Contrato público não muda por refatoração.** As rotas `/api/<modulo>/**` são o contrato com o
   front e com o hardware; reorganizar pastas **não** pode mudar paths, payloads, enums ou status.
6. **Sem abstração preventiva.** Não criar Repository, DTO genérico, BaseController, BaseService,
   interface com uma única implementação ou “helper” sem consumidor.
7. **Código morto não é documentação.** Remover ou registrar em ADR; não deixar comentado.

---

## 3. Mapa do código atual

### 3.1 Backend — árvore real (resumida)

```text
backend/
├── app/
│   ├── Console/Commands/DedupLoginIdUser.php      # migração one-shot (auth v9)
│   ├── Modules/
│   │   ├── Auth/                                  # 5 controllers, 10 requests, 2 resources,
│   │   │   ├── Contracts/                         # 7 models, 9 services, 5 enums, 1 event
│   │   │   ├── Enums/
│   │   │   ├── Events/
│   │   │   ├── Http/Controllers/
│   │   │   ├── Http/Middleware/
│   │   │   ├── Http/Requests/
│   │   │   ├── Http/Resources/
│   │   │   ├── Models/
│   │   │   ├── Services/
│   │   │   └── routes.php
│   │   ├── Rh/                                    # 9 controllers, 16 requests, 8 resources,
│   │   │   ├── Contracts/  Enums/  Events/        # 13 models, 9 services, 6 events, 7 enums,
│   │   │   ├── Http/Controllers/                  # 1 listener, 2 rules
│   │   │   ├── Http/Requests/
│   │   │   ├── Http/Resources/
│   │   │   ├── Listeners/  Models/  Policies/
│   │   │   ├── Rules/  Services/
│   │   │   └── routes.php
│   │   ├── Estoque/                               # 7 controllers, 8 requests, 7 resources,
│   │   │   ├── Contracts/  Enums/  Events/        # 8 models, 5 services, 3 enums, 4 events
│   │   │   ├── Http/Controllers/
│   │   │   ├── Http/Requests/
│   │   │   ├── Http/Resources/
│   │   │   ├── Listeners/  Models/  Policies/
│   │   │   ├── Services/  EstoquePrincipal.php
│   │   │   └── routes.php
│   │   ├── Vendas/                                # 9 controllers, 15 requests, 10 resources,
│   │   │   ├── Contracts/  Enums/  Events/        # 10 models, 6 services, 6 enums, 4 events
│   │   │   ├── Http/Controllers/
│   │   │   ├── Http/Requests/
│   │   │   ├── Http/Resources/
│   │   │   ├── Listeners/  Models/  Policies/
│   │   │   ├── Rules/  Services/  VendasPrincipal.php
│   │   │   └── routes.php
│   │   ├── Financeiro/                            # 7 controllers, 8 requests, 8 resources,
│   │   │   ├── Enums/  Events/                    # 9 models, 9 services, 6 enums, 3 events
│   │   │   ├── Http/Controllers/
│   │   │   ├── Http/Requests/
│   │   │   ├── Http/Resources/
│   │   │   ├── Listeners/  Models/  Policies/
│   │   │   ├── Services/  FinanceiroPrincipal.php
│   │   │   └── routes.php
│   │   ├── Producao/                              # 11 controllers, 21 requests, 18 resources,
│   │   │   ├── Contracts/  Enums/  Events/        # 18 models, 9 services, 9 enums, 6 events
│   │   │   ├── Http/Controllers/
│   │   │   ├── Http/Requests/
│   │   │   ├── Http/Resources/
│   │   │   ├── Listeners/  Models/  Policies/
│   │   │   ├── Services/  ProducaoPrincipal.php
│   │   │   └── routes.php
│   │   ├── Notification/                          # 5 controllers, 5 requests, 1 resource,
│   │   │   ├── Contracts/  Enums/  Exceptions/    # 4 models, 2 services, 5 listeners
│   │   │   ├── Http/Controllers/
│   │   │   ├── Http/Requests/
│   │   │   ├── Http/Resources/
│   │   │   ├── Listeners/  Models/  Policies/
│   │   │   ├── Support/  NotificationPrincipal.php
│   │   │   └── routes.php
│   │   └── Dashboard/                             # agregador sem persistência
│   │       ├── Http/Controllers/
│   │       ├── Http/Resources/
│   │       ├── Policies/
│   │       ├── Services/
│   │       └── routes.php
│   ├── Providers/AppServiceProvider.php
│   └── Shared/Exceptions/                         # 10 exceptions + ErrorResponse
├── bootstrap/app.php                              # mapa de exceções + alias de middleware
├── config/{app,auth,jwt,estoque,financeiro,producao,rh,notification,...}.php
├── database/migrations/<modulo>/                  # 83 migrações
├── routes/{api,console}.php
├── tests/{Unit,Feature}/<Modulo>/
└── vendor/  storage/  bootstrap/cache/            # runtime
```

### 3.2 Backend — matriz por módulo

| Módulo | Models | Controllers | Requests | Resources | Services | Events | Listeners | Enums |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Auth | 7 | 5 | 10 | 2 | 9 | 1 | 0 | 5 |
| Rh | 13 | 9 | 16 | 8 | 9 | 6 | 1 | 7 |
| Estoque | 8 | 7 | 8 | 7 | 5 | 4 | 1 | 3 |
| Vendas | 10 | 9 | 15 | 10 | 6 | 4 | 1 | 6 |
| Financeiro | 9 | 7 | 8 | 8 | 9 | 3 | 3 | 6 |
| Producao | 18 | 11 | 21 | 18 | 9 | 6 | 2 | 9 |
| Notification | 4 | 5 | 5 | 1 | 2 | 0 | 5 | 1 |
| Dashboard | 0 | 1 | 0 | 1 | 1 | 0 | 0 | 0 |
| **Total** | **69** | **54** | **83** | **55** | **50** | **24** | **13** | **37** |

### 3.3 Frontend — árvore real (resumida)

```text
frontend/
├── src/
│   ├── routes/                     # SvelteKit: views + loads (72 páginas)
│   │   ├── (app)/                  # 9 grupos de módulo
│   │   │   ├── configuracoes/{perfil,permissoes,sistema,usuarios}
│   │   │   ├── dashboard/components/ (8 componentes locais)
│   │   │   ├── estoque/{bom,emprestimos,entradas,fornecedores,itens,localizacoes,saidas}
│   │   │   ├── financeiro/{contas-pagar,contas-receber,custeio,doacoes,lancamentos,relatorios}
│   │   │   ├── notificacoes/{historico,preferencias}
│   │   │   ├── pessoas/{niveis,processo-seletivo,registro-horas,treinamentos}
│   │   │   ├── producao/{5s,maquinas,projetos,tarefas}
│   │   │   └── vendas/{clientes,encomendas,marketing,marketplace,orcamentos,solicitacoes}
│   │   └── auth/{login,forgot-password}
│   └── lib/
│       ├── api/                    # 48 arquivos: auth, dashboard, configuracoes/, financeiro/,
│       │                           # producao/, rh/, stock/, vendas/
│       ├── components/             # 47: ui/, layout/, configuracoes/, estoque/, producao/
│       ├── config/menu.ts
│       ├── stores/                 # auth, notifications, toast
│       ├── types/                  # 11 arquivos por domínio
│       └── utils/                  # 15 arquivos por domínio
├── tests/unit/                     # 39 arquivos de teste (Vitest), alguns em subpasta estoque/
├── e2e/estoque.spec.ts             # placeholder skip, sem Playwright
├── docs/                           # ACTION_PLAN, BUGS, CHANGELOG (mínimos/desatualizados)
└── build/  .svelte-kit/            # artefatos gerados (não são fonte)
```

### 3.4 Fluxo real de uma requisição

```text
+page.svelte
  └─ +page.ts (load, filtros locais, guards)
       └─ src/lib/api/<modulo>.ts (apiFetch + bearer + map)
            └─ HTTP /api/<modulo>/<recurso>  (contrato PT, imutável)
                 └─ <Modulo>/Http/Controllers/XController
                      ├─ XRequest (validação)      XResource (shape)
                      ├─ XPrincipal::from(login)   (identidade)
                      ├─ XService (regra)  ou Model direto (CRUD trivial)
                      └─ Event / Listener (assíncrono, fila database)
```

---

## 4. Problemas estruturais encontrados

| # | Problema | Onde | Consequência | Ação recomendada |
|---|---|---|---|---|
| 1 | `app/Modules/<Modulo>/Http/{Controllers,Requests,Resources}` repete a mesma árvore em 7 módulos | `backend/app/Modules/*/Http/**` | 5 níveis de profundidade, navegação lenta, pastas vazias/pequenas | Centralizar as camadas em `app/Http/{Controllers,Requests,Resources}/<Modulo>` |
| 2 | 6 classes `*Principal` quase idênticas | `app/Modules/*/*Principal.php` | Duplicação de 37–50 linhas × 6, cada uma resolve role via `app(AuthContract::class)` | Uma única `App\Shared\Auth\Caller` (ou `AuthContext`) + policy do módulo |
| 3 | 36 controllers repetem `private function principal()` | `app/Modules/*/Http/Controllers/*.php` | ~180 linhas de boilerplate; fácil divergir | Base controller mínimo **ou** resolver `Caller` no middleware/request |
| 4 | 83 FormRequests; 78 só têm `authorize(){ return true; }` | `app/Modules/*/Http/Requests/*.php` | ~390 linhas inúteis; falsa sensação de autorização | Remover o método; manter `authorize` só quando houver regra real |
| 5 | 10 exceptions customizadas + `ErrorResponse` + mapa manual em `bootstrap/app.php` | `app/Shared/Exceptions/*`, `bootstrap/app.php:36-62` | 10 classes de 5–12 linhas + loop de render | Uma `ApiException` com `status` + fábricas; usar `HttpException` nativo onde couber |
| 6 | 5 arquivos de config para 6 flags de scheduler | `config/{estoque,financeiro,producao,rh,notification}.php` | Um arquivo por módulo para uma flag | `config/fablab.php` com `schedulers.*` (ou `.env` lido no `console.php`) |
| 7 | 3 nomes de evento duplicados em classes diferentes | `Estoque/Financeiro` (`compra.solicitada`), `Estoque/Producao` (`producao.concluida`), `Producao/Vendas` (`producao.status.alterado`) | Mesmo nome lógico com payloads distintos; publisher dispara 2 classes | Uma classe por `NAME`, em `app/Events/`, com payload único |
| 8 | `ProducaoEventPublisher` dispara dois eventos para o mesmo fato | `app/Modules/Producao/Services/ProducaoEventPublisher.php:33-34,57-58` | Consumidores diferentes, risco de divergência | Consolidar evento + um único dispatch; listeners leem o mesmo payload |
| 9 | Contratos: 6 interfaces + 6 `Default*` | `app/Modules/*/Contracts/*` | 12 arquivos; uma implementação por interface | Manter interface só se houver fake/múltiplas impls; caso contrário, 1 classe concreta por módulo |
| 10 | DTOs/Requests/Resources triviais para CRUD de 1 entidade | `CategoriaService`, `DoacaoService`, `NivelService`, etc. | 4 arquivos (controller+request+resource+service) para um CRUD simples | Service só com regra real (transação, múltiplas entidades, evento, autorização) |
| 11 | Rotas/aliases duplicados | `Dashboard/routes.php:14-15`, `:18`; `Notification/routes.php:16-19,32` | Duas portas para o mesmo recurso | Manter canônica + alias só se houver consumidor externo documentado |
| 12 | Lógica de filtro/paginação no client | vários `+page.ts` (ex.: `estoque/itens`) | Regra de negócio duplicada e divergente do back | Trazer filtros/paginação para o backend ou extrair util compartilhado |
| 13 | Front com 3 construtores de query e 2 wrappers HTTP | `producao/client.ts:10`, `stock/request.ts:30`, `configuracoes/usuarios.ts:56`; `stockFetch` + `apiFetch` | 116 chamadas repetem `headers: bearer()` | Um `apiFetch` com auth automática + um `buildQuery` único |
| 14 | Barrels e re-exports circulares | `producao/client.ts:131-140`; `rh.ts` ↔ `rh/pessoas.ts:207-208` | 31 imports via barrel; dependência circular conceitual | Importar direto do módulo; um `index.ts` por módulo, no máximo |
| 15 | Documentação divergente do código | README/arquitetura/frontend docs | Onboarding erra (Sanctum, adapter-static, `services/`, Gateway) | Uma fonte versionada; atualizar após a refatoração |

---

## 5. Código inútil, redundante ou substituível por nativo

> Legenda de risco: **BAIXO** = remoção mecânica; **MÉDIO** = exige ajuste de testes/contrato;
> **ALTO** = exige decisão de arquitetura e validação de contrato.

### 5.1 Validação de e-mail — o ponto específico levantado

**Conclusão da auditoria:** **não existe** um “módulo PHP de validação de e-mail” escrito à mão no
backend. O backend já usa a regra nativa do Laravel (`email`) nos pontos de entrada:

| Local | Regra atual |
|---|---|
| `app/Modules/Auth/Http/Requests/CriarUsuarioRequest.php:20` | `['required', 'email', 'max:255']` |
| `app/Modules/Auth/Http/Requests/AtualizarUsuarioRequest.php:19` | `['nullable', 'email', 'max:255']` |
| `app/Modules/Auth/Http/Requests/LoginRequest.php:19` | `['nullable', 'email', 'max:255']` |
| `app/Modules/Rh/Http/Requests/PessoaRequest.php:32` | `['nullable', 'required_if:criarAcesso,true', 'email', 'max:255']` |
| `app/Modules/Vendas/Http/Requests/ClienteRequest.php:23` | `['nullable', 'email', 'max:255']` |

Existe **um** uso de `filter_var(..., FILTER_VALIDATE_EMAIL)` em
`app/Modules/Rh/Contracts/DefaultRhContract.php:87`, mas ele é **nativo** e serve para “farejar”
e-mail no campo livre `contato`. O problema não é a função: é validar tarde (na leitura) em vez de
validar na entrada. Ação: manter `email` na entrada e remover o sniffing, ou criar uma coluna
`email` explícita em `rh.pessoa` (decisão de domínio).

**Onde está a validação de e-mail escrita à mão:** no **frontend**, em 7 arquivos e 8 ocorrências
de regex. Todos os campos já são `type="email"`, então o navegador já valida; o backend também
valida. A regex é redundante.

| Arquivo | Linha(s) | O que remover |
|---|---:|---|
| `frontend/src/routes/(app)/configuracoes/perfil/+page.svelte` | 42, 74 | `EMAIL_RE` + `.test()` |
| `frontend/src/routes/(app)/vendas/clientes/novo/+page.svelte` | 204 | regex inline |
| `frontend/src/routes/(app)/vendas/clientes/[id]/+page.svelte` | 89 | `emailValido` derivado de regex |
| `frontend/src/routes/(app)/pessoas/processo-seletivo/+page.svelte` | 260 | regex inline |
| `frontend/src/routes/(app)/pessoas/novo/+page.svelte` | 71 | regex inline |
| `frontend/src/routes/(app)/pessoas/[id]/editar/+page.svelte` | 94, 98 | regex inline (2 ocorrências) |
| `frontend/src/lib/components/configuracoes/ModalUsuario.svelte` | 17, 60 | `EMAIL_RE` + `.test()` |

**Regra final para e-mail:** backend é a autoridade (`email` nativo); front usa `type="email"` e
só exibe a mensagem de erro do backend. Se precisar de validação antes do submit para UX, um único
`isEmail()` em `src/lib/utils/validation.ts`, nunca regex copiada em páginas. **Não** criar
`EmailService`, `EmailValidator`, `EmailRule` ou DTO só para isso.

### 5.2 Outras validações que o Laravel/PHP já fazem

| Validação escrita à mão | Local | Substituto nativo | Ação |
|---|---|---|---|
| `regex:/^\d{4}-\d{2}$/` para mês | `Rh/Controllers/{Horas,Extrato,ApontamentoHoras}Controller.php`, `Financeiro/RelatorioController.php:71` | `date_format:Y-m` | Trocar regex por regra nativa |
| `preg_match('/^\d{4}-\d{2}$/', ...)` | `Rh/Services/ApontamentoService.php:170` | `date_format:Y-m` na entrada | Remover validação duplicada no service |
| `filter_var($this->input('read'), FILTER_VALIDATE_BOOLEAN, ...)` | `Notification/Http/Requests/{ListarNotificacoes,Historico}Request.php` | `$request->boolean('read')` + `has()` | Usar API nativa do Request |
| `filter_var(..., FILTER_VALIDATE_BOOLEAN)` em config | `Producao/Models/Parametro5S.php:43` | cast/`boolean` do Laravel | Manter só se o valor vier de string livre; documentar |
| `in_array` manual de enums/strings em services | vários (`CrmService`, `NotificacaoService`, etc.) | `Rule::enum`, `Rule::in`, cast de enum | Validar na entrada; service confia no dado validado |
| `if (blank($email) && blank($nomeUsuario)) throw` | `Auth/Services/AuthService.php:35` | `required_without` no `LoginRequest` | Validar no Request; service não repete |
| `if ($novaSenha !== $confirmacaoSenha)` | `Auth/Services/AuthService.php` (`alterarSenha`) | `confirmed` no Request (campo `novaSenha_confirmation`) ou regra `same` | Mover para validação de entrada |

> **CPF/CNPJ não são nativos.** `CpfRule` e `DocumentoRule` são válidos como regras de domínio.
> O problema é que `DocumentoRule` reimplementa o algoritmo de CPF que já existe em `CpfRule`
> (`Vendas/Rules/DocumentoRule.php` vs `Rh/Rules/CpfRule.php`). Centralizar em
> `app/Rules/CpfCnpjRule.php` e reutilizar. **Não** remover a validação de CPF/CNPJ.

### 5.3 Backend — boilerplate e código sem efeito

#### a) `authorize()` vazio em 78 FormRequests

| Módulo | Requests | Com `return true;` |
|---|---:|---:|
| Auth | 10 | 10 |
| Rh | 16 | 16 |
| Estoque | 8 | 8 |
| Vendas | 15 | 15 |
| Financeiro | 8 | 8 |
| Producao | 21 | 21 |
| Notification | 5 | 0 (5 só checam `instanceof Login`) |
| **Total** | **83** | **78** |

O método `authorize()` do `FormRequest` já retorna `true` por padrão quando não é declarado. Os 78
métodos podem ser removidos sem mudança de comportamento. Os 5 da Notification também são
redundantes com `auth:api` (a rota já garante usuário autenticado). **Exceção:** `Producao/
Http/Requests/TarefaRequest.php` chama a policy antes de retornar `true`; essa regra deve ir para o
service/policy, não para um `authorize` cosmético.

#### b) `principal()` repetido em 36 controllers

Arquivos: praticamente todos os controllers de Estoque, Financeiro, Producao, Vendas e
`Rh/PessoaController`. Cada um tem:

```php
private function principal(Request $request): XPrincipal
{
    /** @var Login $login */
    $login = $request->user();

    return XPrincipal::from($login);
}
```

Ação: uma única resolução de identidade (middleware ou `Caller` injetado) e/ou um trait/base
controller. O ganho é ~180 linhas e a garantia de um só jeito de resolver o chamador.

#### c) 6 classes `*Principal`

| Arquivo | Linhas |
|---|---:|
| `app/Modules/Estoque/EstoquePrincipal.php` | 47 |
| `app/Modules/Financeiro/FinanceiroPrincipal.php` | 37 |
| `app/Modules/Notification/NotificationPrincipal.php` | 37 |
| `app/Modules/Producao/ProducaoPrincipal.php` | 37 |
| `app/Modules/Rh/RhPrincipal.php` | 50 |
| `app/Modules/Vendas/VendasPrincipal.php` | 37 |

Quase idênticas: `idPessoa`, `role`, `admin`, `from(Login)`, `isAdmin()`. `Rh` acrescenta
`idFuncionario`/`nivel`; `Estoque` acrescenta `responsavelEstoque`. Ação: um `Caller` compartilhado
com `idUser`, `role`, `admin`; dados específicos ficam na policy/service do módulo (ex.:
`RhPolicy::funcionarioDo($caller)`), não em uma classe por módulo.

#### d) 10 exceptions + `ErrorResponse` e um mapa manual de status

`app/Shared/Exceptions/` tem 9 classes de exceção + `ErrorResponse`; `Notification/Exceptions` tem
1 (`LinkInvalidoException`) — 10 classes de exceção no total. A
maioria só define construtor/mensagem e é mapeada em `bootstrap/app.php:36-62`. Ação: uma
`ApiException` com `status` e fábricas (`ApiException::notFound()`, `::forbidden()`, etc.) ou usar
`abort()`/`HttpException` nativo. Exceções de domínio com semântica real (`SaldoInsuficiente`,
`Conflito`) podem permanecer, desde que herdem da base.

#### e) Eventos duplicados e dispatch duplo

| Nome lógico | Classes atuais | Efeito |
|---|---|---|
| `compra.solicitada.event` | `Estoque/Events/CompraSolicitadaEvent`, `Financeiro/Events/CompraSolicitadaEvent` | Dois payloads para o mesmo nome |
| `producao.concluida.event` | `Estoque/Events/ProducaoConcluidaEvent`, `Producao/Events/ProducaoConcluidaEvent` | `ProducaoEventPublisher` dispara os dois (`:57-58`) |
| `producao.status.alterado.event` | `Producao/Events/ProducaoStatusAlteradoEvent`, `Vendas/Events/ProducaoStatusAlteradoEvent` | `ProducaoEventPublisher` dispara os dois (`:33-34`) |

Ação: **uma classe por `const NAME`** em `app/Events/`, com payload que atenda todos os listeners.
O `Event::listen` continua em `AppServiceProvider` (ou em um `EventServiceProvider` se o volume
crescer). Não criar “evento espelho” por módulo.

#### f) Serviço/comando de deduplicação one-shot

`app/Console/Commands/DedupLoginIdUser.php` e
`app/Modules/Auth/Services/LoginIdUserDedupService.php` existem para a migração `auth v9` (UNIQUE em
`auth.login.id_user`). Depois que a migração estiver aplicada em todos os ambientes, o comando e o
serviço são **código de migração concluída**: arquivar/remover. `reverter()` só é usado em teste
(`tests/Feature/Auth/LoginIdUserDedupServiceTest.php`); não há rota nem comando que o exponha.

#### g) Duplicação de CPF

`Rh/Rules/CpfRule.php` e `Vendas/Rules/DocumentoRule.php` implementam o mesmo algoritmo de CPF
(DocumentoRule acrescenta CNPJ). Ação: uma regra compartilhada `CpfCnpjRule` com `normalizar`,
`valido`, `mascarar`; `CpfRule` (ou um alias) para o RH. Reduz ~70 linhas duplicadas e evita
divergência de dígitos verificadores.

#### h) Import não usado

`app/Modules/Estoque/Http/Controllers/ItemController.php:10` importa `SaidaResource` e não usa.
Remover.

#### i) Relacionamentos Eloquent sem referência

Não há `$with` padrão nem acesso por propriedade/`with()/load()/whereHas` em app **nem em tests**
para os 6 abaixo:

| Model | Relação | Linha |
|---|---|---:|
| `Producao/Models/Maquina.php` | `usos()` | 29 |
| `Producao/Models/ItemInspecao5S.php` | `inspecao()` | 28 |
| `Producao/Models/ItemInspecao5S.php` | `checklist()` | 33 |
| `Producao/Models/HistoricoUsoMaquina.php` | `maquina()` | 32 |
| `Producao/Models/AuditoriaProjetoMesa.php` | `projetoMesa()` | 31 |
| `Producao/Models/AdvertenciaMembro.php` | `inspecao()` | 34 |

Ação: remover após rodar a suíte. Outras relações apontadas por análise inicial são usadas em
testes (ex.: `Funcionario::pessoa`) e **não** devem ser removidas.

#### j) Método de produção sem rota

`Financeiro/Services/CusteioService.php:39` (`calcular(int, FinanceiroPrincipal)`) só é chamado em
testes; o endpoint usa `consultar()`. Se não houver uso planejado, remover e ajustar o teste. Baixo
risco, mas confirmar com o domínio.

#### k) Wrapper trivial

`Auth/Http/Controllers/AuthController.php:79` (`private function bearer(Request $request): ?string`)
só faz `return $request->bearerToken();`. Usar `$request->bearerToken()` direto.

#### l) Scaffold Laravel/Vite sem uso

O backend é API pura; o SPA é separado. Estes arquivos não participam do runtime nem do build do
front:

- `backend/package.json`, `backend/vite.config.js`
- `backend/resources/js/app.js`, `backend/resources/js/bootstrap.js`
- `backend/resources/css/app.css`
- `backend/composer.json:41-42` (`npm install`/`npm run build` no script `setup`)
- `backend/composer.json:15` (`fakerphp/faker` sem factory uso; só `UploadedFile::fake()` do Laravel)
- `backend/composer.json:18` (`laravel/sail` sem uso; o projeto usa Docker Compose próprio)

Ação: remover o scaffold Vite/resources/package.json; limpar scripts `setup`/`dev` que chamam npm;
remover dependências dev não usadas (avaliar `tinker`/`pail` caso a equipe use no dia a dia).

#### m) Artefatos e diretórios vazios

- `backend/app/Models/` e `backend/database/factories/` vazios no checkout atual.
- `backend/bootstrap/cache/{packages,services}.php`, `backend/.phpunit.result.cache`,
  `backend/database/database.sqlite` (SQLite não é o banco alvo, que é PostgreSQL).
- `frontend/build/`, `frontend/.svelte-kit/`.
- `.opencode/node_modules/` (63 MB), `.opencode/backups/*.tar.gz` (snapshot histórico).

Esses itens já estão cobertos pelo `.gitignore` em parte; o guia é **não versionar** e limpar o
workspace. `database.sqlite` não deve ser usado como banco de desenvolvimento.

### 5.4 Frontend — código inútil/redundante

#### a) Exports realmente sem referência (15)

Removidos com segurança (não são usados em `src/`, `tests/` nem `e2e/`):

| Símbolo | Arquivo |
|---|---|
| `AtribuirTarefaFinalPayload` | `src/lib/types/rh.ts` |
| `CusteioState` | `src/routes/(app)/financeiro/custeio/+page.ts` |
| `filtrosDaUrl` | `src/lib/api/producao/client.ts:44` |
| `mostrarDeAte` | `src/lib/api/producao/client.ts:123` |
| `paginaAnterior` | `src/lib/api/producao/client.ts:119` |
| `paginaSeguinte` | `src/lib/api/producao/client.ts:115` |
| `paginadoVazio` | `src/lib/api/producao/client.ts:103` |
| `ItemMovementsResult` | `src/lib/types/stock.ts` |
| `UpdateBomPayload` | `src/lib/types/stock.ts` |
| `KanbanItem` | `src/lib/types/producao.ts` |
| `MesaStatus` | `src/lib/types/producao.ts` |
| `Parametro5S` | `src/lib/types/producao.ts` |
| `LoginRequest` | `src/lib/types/auth.ts` |
| `ResumoFinanceiro` | `src/lib/types/financeiro.ts` |
| `TotaisContas` | `src/lib/types/financeiro.ts` |

#### b) `export` desnecessário (~150 símbolos)

Muitos tipos/constantes são exportados mas só usados **dentro do próprio arquivo** (ex.:
`mapCategoria`, `mapCliente`, `Raw*`, `Criar*Payload`, `*Filtros`, `*Params`). Isso não quebra nada,
mas amplia a API pública e engana ferramentas de dead code. Ação: remover `export` quando o símbolo
não é importado por outro arquivo. Sem pressa; fazer por módulo.

#### c) Três construtores de query

| Implementação | Arquivo | Uso |
|---|---|---|
| `montarQuery` | `src/lib/api/producao/client.ts:10` | Produção |
| `buildQuery` | `src/lib/api/stock/request.ts:30` | Estoque + `rh.ts` |
| `montarQuery` (privado) | `src/lib/api/configuracoes/usuarios.ts:56` | Configurações |

Ação: um único `buildQuery` em `src/lib/api/client.ts`; remover os outros dois.

#### d) Dois wrappers HTTP e 116 `bearer()` manuais

`apiFetch` já centraliza timeout, 401, refresh e erro; `stockFetch` só adiciona `Authorization`.
As chamadas repetem `headers: bearer()` 116 vezes em 48 arquivos. Ação: `apiFetch` injeta
`Authorization` automaticamente quando houver token (com opt-out para rotas públicas
`/auth/login` e `/auth/refresh`); remover `stockFetch`/`stockFetchBlob` e os `bearer()` manuais.

#### e) Barrel de Produção

`src/lib/api/producao/client.ts:131-140` reexporta 10 submódulos; 31 arquivos importam do barrel.
Isso acopla tudo e dificulta tree-shaking. Ação: importar direto do submódulo ou criar **um**
`producao/index.ts` com exports explícitos (sem `export *`).

#### f) Re-export circular de RH

`src/lib/api/rh/pessoas.ts:207-208` reexporta `searchPeople`/`PersonOption` de `$lib/api/rh`, mas
`rh.ts` é um arquivo separado que implementa `searchPeople`. Ação: mover `searchPeople` para
`rh/pessoas.ts` (ou manter `rh.ts` como único ponto e remover o re-export); eliminar a duplicidade.

#### g) Tipos `Paged<T>` duplicados

`financeiro.ts:22`, `rh.ts:32`, `vendas.ts:243` definem a mesma interface (mais
`PagedNotifications` em `notifications.ts:25`). Ação: `types/shared.ts` (ou `pagination.ts`) com
`Paged<T>` e `PagedNotifications<T>` reutilizados.

#### h) E2E órfão

`frontend/e2e/estoque.spec.ts` é um `describe.skip` que documenta fluxos, mas **não há Playwright**
em `package.json` e o Vitest só inclui `tests/unit/**` (`vite.config.js:19`). Ação: mover o conteúdo
para `docs/` ou instalar Playwright e criar script real; como está, é código morto.

#### i) Documentação do front desatualizada

`frontend/README.md` ainda fala em “Gateway”, `src/lib/services/` (não existe), grupo `(auth)` (é
`auth`) e `tests/integration/` (não existe). `frontend/docs/{ACTION_PLAN,BUGS,CHANGELOG}.md` são
mínimos/antigos. Ação: remover `frontend/docs` e apontar para a documentação raiz, ou atualizar os
três arquivos.

### 5.5 Repositório — artefatos e documentação enganosa

| Item | Evidência | Ação |
|---|---|---|
| Workflows Firebase de outro projeto | `.github/workflows/firebase-hosting-merge.yml:20` e `...pull-request.yml:21` usam `trancandae-cd59b` | Não versionar / remover do workspace (o alvo é Ubuntu/Docker/Tailscale) |
| Skills Firebase/agent | `.agents/skills/*`, `skills-lock.json` | Não é código do produto; mover para configuração global do agente ou remover |
| `.claude/` | diretório vazio no checkout | Remover/ignorar |
| Backups de rewrite | `.opencode/backups/*.tar.gz` | Manter como histórico (não é código); não referenciar no fluxo diário |
| Docs arquivadas | `docs/_archive/2026-10-19/` | Manter arquivo morto separado; não usar como fonte |
| Arquitetura vs código | `.opencode/specs/system/architecture.md:51,61,169` diz Sanctum e `adapter-static`; o código usa JWT (`php-open-source-saver/jwt-auth`) e `adapter-node` | Atualizar docs; uma fonte versionada |
| `Shared` prometido, inexistente | docs citam `app/Shared/{Auth,Contracts,Events}`; no código só existe `Shared/Exceptions` | Corrigir docs ou criar `Shared` mínimo (preferir corrigir docs) |
| Infra docs | `infra/docs/ARCHITECTURE.md:86-89` diz que `*-service/` “ainda existe”; o README diz que foram removidos | Remover seção de legado |
| Frontend README | `frontend/README.md:17,40,44,51` cita Gateway, `services/`, `(auth)`, `integration/` | Atualizar após a refatoração |
| `backend/README` | estrutura omite que a árvore é `Modules/<Modulo>/Http/...` e cita `Services` “ou Actions” sem Actions | Atualizar para a estrutura-alvo |

### 5.6 O que **não** é código inútil (não remover sem análise)

- **Contracts consumidos por outros módulos:** `AuthContract`, `RhContract`, `EstoqueContract`,
  `VendasContract`, `ProducaoContract`, `NotificationContract` têm consumidores reais (Dashboard,
  Financeiro, Produção, Vendas, Estoque, Notification). São a fronteira do monólito modular.
- **Policies:** `RhPolicy`, `EstoquePolicy`, `ProducaoPolicy`, `VendasPolicy`, `FinanceiroPolicy`,
  `NotificationPolicy`, `DashboardPolicy` são o enforcement do RBAC; não substituir por checagem
  no front.
- **Resources:** são a “View” JSON do Laravel; mantêm shape/segurança (LGPD, campos derivados).
- **Listeners e schedulers:** `routes/console.php` usa `ExtratoService`, `EmprestimoService`,
  `LancamentoService`, `ProjetoMesaService`, `NotificationDispatchService`,
  `TokenBlacklistService`; não são dead code.
- **Rules de CPF/CNPJ:** domínio real, não nativo.
- **`LinkValidator`:** allowlist de link de notificação (evita `javascript:`/`data:`); mantenha.
- **Os 5 FormRequests da Notification** que checam `instanceof Login`: redundantes com `auth:api`,
  mas se forem usados fora de rota autenticada, manter. Verificar antes de remover.
- **Feature tests** e fixtures: não são “código de produção”, mas são a rede de segurança da
  refatoração.

---

## 6. Estrutura-alvo

### 6.1 Backend — MVC centralizado por camada, módulo como subpasta

```text
backend/
├── app/
│   ├── Console/Commands/                  # só comandos operacionais vivos
│   ├── Contracts/                         # 1 contrato por módulo (quando consumido)
│   │   ├── AuthContract.php
│   │   ├── RhContract.php
│   │   ├── EstoqueContract.php
│   │   ├── VendasContract.php
│   │   ├── ProducaoContract.php
│   │   └── NotificationContract.php
│   ├── Enums/<Modulo>/                    # 1 enum por arquivo; domínio do módulo
│   ├── Events/                            # 1 classe por NAME (sem duplicatas)
│   ├── Exceptions/
│   │   ├── ApiException.php               # status + mensagem + payload
│   │   └── SaldoInsuficienteException.php # só se mantiver semântica própria
│   ├── Http/
│   │   ├── Controllers/<Modulo>/          # C: só HTTP
│   │   ├── Middleware/CheckBlacklist.php
│   │   ├── Requests/<Modulo>/             # validação/autorização real
│   │   └── Resources/<Modulo>/            # View JSON
│   ├── Listeners/<Modulo>/                # consumidores de eventos
│   ├── Models/<Modulo>/                   # Eloquent
│   ├── Policies/                          # 1 policy por módulo (arquivo único)
│   ├── Providers/AppServiceProvider.php   # bindings, gates, Event::listen
│   └── Services/<Modulo>/                 # regra de negócio real
├── config/fablab.php                      # flags de scheduler/integração
├── database/migrations/<modulo>/          # mantém (nativo do Laravel)
├── routes/
│   ├── api.php                            # monta os módulos
│   ├── modules/<modulo>.php               # rotas do módulo (opcional)
│   └── console.php                        # Schedule
└── tests/{Unit,Feature}/<Modulo>/         # mantém
```

**Por que essa árvore é mais enxuta:**

- Elimina `app/Modules/<Modulo>/Http/**` (um nível inteiro repetido 7 vezes).
- Coloca todos os Controllers em um lugar, todos os Models em outro, todas as Views (Resources) em
  outro. Para achar algo, você pensa na camada, não na árvore do módulo.
- Mantém o módulo no namespace/subpasta (`App\Http\Controllers\Rh`, `App\Models\Rh`), então não
  mistura arquivos de módulos diferentes.
- `Contracts`, `Policies`, `Events` e `Exceptions` ficam planos (são poucos e transversais).
- Profundidade máxima: 4 (`app/Http/Controllers/Rh/PessoaController.php`).

**Mapeamento mecânico (exemplo):**

| Hoje | Alvo |
|---|---|
| `app/Modules/Rh/Models/Pessoa.php` | `app/Models/Rh/Pessoa.php` |
| `app/Modules/Rh/Http/Controllers/PessoaController.php` | `app/Http/Controllers/Rh/PessoaController.php` |
| `app/Modules/Rh/Http/Requests/PessoaRequest.php` | `app/Http/Requests/Rh/PessoaRequest.php` |
| `app/Modules/Rh/Http/Resources/PessoaResource.php` | `app/Http/Resources/Rh/PessoaResource.php` |
| `app/Modules/Rh/Services/PessoaService.php` | `app/Services/Rh/PessoaService.php` |
| `app/Modules/Rh/Enums/NivelAcesso.php` | `app/Enums/Rh/NivelAcesso.php` |
| `app/Modules/Rh/Events/NivelAlteradoEvent.php` | `app/Events/NivelAlteradoEvent.php` |
| `app/Modules/Rh/Listeners/ProcessarPontoRfid.php` | `app/Listeners/Rh/ProcessarPontoRfid.php` |
| `app/Modules/Rh/Policies/RhPolicy.php` | `app/Policies/RhPolicy.php` |
| `app/Modules/Rh/Contracts/RhContract.php` | `app/Contracts/RhContract.php` |
| `app/Modules/Rh/routes.php` | `routes/modules/rh.php` |

> **Nota:** se o time preferir co-localização por módulo, a alternativa enxuta é
> `app/Modules/<Modulo>/{Controllers,Models,Services,Requests,Resources,Events}` (sem `Http/`).
> Não misture os dois estilos. O estilo recomendado é o **layer-first** acima, por ser o padrão
> Laravel e o que mais reduz o custo de navegação.

### 6.2 Regras por camada (para não recriar o castelo)

| Camada | Deve conter | Não deve conter | Critério para existir |
|---|---|---|---|
| **Model** | `$fillable`, `$casts`, relações, scopes simples, regras de persistência | HTTP, validação de request, resposta JSON, chamada a outro módulo | Sempre que houver tabela |
| **Controller** | Receber Request, chamar Service/Model, devolver Resource/JSON | Regra de negócio, SQL, validação inline extensa, `principal()` duplicado | Sempre que houver rota |
| **Request** | `rules()`, `authorize()` real, mensagens, preparação de dados | Regra de negócio, acesso a banco, `return true` cosmético | Só se a validação for reutilizada ou tiver autorização real; CRUD de 1 endpoint pode usar `$request->validate()` |
| **Resource** | Shape de saída, ocultação LGPD, campos derivados de apresentação | Regra de negócio, query, cálculo pesado | Sempre que o shape não for o Model cru ou houver campo sensível |
| **Service** | Transação, múltiplas entidades, autorização de domínio, evento, contrato | CRUD de uma entidade só, wrapper de Model, `app()` como service locator | Só quando houver regra real; CRUD simples fica no Controller/Model |
| **Policy** | Regras de quem pode o quê, por módulo | Query, formatação, HTTP | Sempre que houver RBAC além de `can:admin` |
| **Event** | Fato de domínio imutável (`NAME`, payload) | Lógica de negócio, acesso a banco | 1 por fato; nunca 2 classes com o mesmo `NAME` |
| **Listener** | Reação idempotente ao evento, `ShouldQueue` quando fizer sentido | Regra que o produtor deveria ter feito | 1 por módulo consumidor |
| **Contract** | Interface pública do módulo para outros módulos | Model/Service interno, detalhe de implementação | Só se consumido por outro módulo |
| **Enum** | Valores e labels do domínio | Persistência, HTTP | 1 por conceito; evitar enums duplicados (`Role` × `NivelAcesso`) |

### 6.3 Frontend — alvo enxuto

```text
frontend/
├── src/
│   ├── routes/                          # Views + loads (SvelteKit exige a árvore de rotas)
│   │   ├── (app)/<modulo>/...
│   │   └── auth/...
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts                # fetch + auth + refresh + erro + buildQuery
│   │   │   ├── auth.ts
│   │   │   ├── dashboard.ts
│   │   │   ├── rh.ts                    # ou rh/ se passar de ~400 linhas
│   │   │   ├── estoque.ts               # ou estoque/...
│   │   │   ├── vendas.ts
│   │   │   ├── financeiro.ts
│   │   │   └── producao.ts
│   │   ├── components/
│   │   │   ├── ui/                      # design system reutilizável
│   │   │   ├── layout/                  # shell/sidebar/topbar
│   │   │   └── <modulo>/                # só se o componente for usado em >1 rota
│   │   ├── stores/                      # auth, notifications, toast
│   │   ├── types/
│   │   │   ├── shared.ts                # Paged<T>, ApiError, etc.
│   │   │   └── <modulo>.ts
│   │   └── utils/                       # só helpers compartilhados
│   └── app.*
├── tests/unit/                          # testes (Vitest)
├── e2e/                                 # só se Playwright existir de verdade
└── docs/                                # remover ou apontar para docs raiz
```

**Regras do front:**

- `routes/**/+page.svelte` = View. Não colocar regra de negócio pesada, validação de domínio ou
  cálculo financeiro no componente.
- `routes/**/+page.ts` = load/guard. Filtro/paginação que o backend já sabe fazer não deve ser
  reimplementado no client.
- `lib/api` = “Controller” de dados: uma função por endpoint, sem barrel mágico, sem wrapper por
  módulo, sem `bearer()` manual.
- `lib/types` = “Models” de contrato. Um `shared.ts` para genéricos; não duplicar `Paged<T>`.
- `lib/components/ui` = Views reutilizáveis; componentes de uma única página ficam ao lado da rota.
- `lib/utils` = funções puras compartilhadas. Não criar `services/` (não existe no alvo).

### 6.4 Anti-padrões que não devem ser recriados

- ❌ `Repository` por cima do Eloquent sem necessidade.
- ❌ `BaseController`/`BaseService` genérico.
- ❌ Interface + `Default` para uma única implementação, sem teste/fake que justifique.
- ❌ DTO para cada payload; use `FormRequest`/arrays tipados quando o payload é simples.
- ❌ Evento “espelho” por módulo para o mesmo `NAME`.
- ❌ `Principal` por módulo; use um `Caller` compartilhado.
- ❌ Regex de e-mail/data/boolean quando `email`, `date_format`, `boolean` existem.
- ❌ Service que só faz `Model::create($request->validated())`.
- ❌ Barrel `export *` que esconde dependências.
- ❌ Pasta `docs/` duplicada em cada subprojeto.

### 6.5 Metas de redução (ordem de grandeza)

| Item | Hoje | Alvo razoável | Como |
|---|---:|---:|---|
| Diretórios em `app/` | 98 | ~55–60 | Centralizar camadas; remover `Http/` por módulo |
| FormRequests | 83 | ~55–65 | Remover `authorize` vazio; inline em CRUD de 1 endpoint |
| Services | 50 | ~40–45 | Mesclar CRUD trivial (Categoria, Doacao, Nivel, etc.) |
| Exceptions | 10 + `ErrorResponse` | 2–3 | `ApiException` + semânticas reais |
| Configs de scheduler | 5 | 1 | `config/fablab.php` |
| Events | 24 | 21 | Uma classe por `NAME` |
| `*Principal` | 6 | 1 | `Caller` compartilhado |
| Contratos | 12 | 6–12 | Opcional: 1 classe por módulo se não houver fake |
| Requests com `authorize` vazio | 78 | 0 | Remover método |
| `principal()` duplicado | 36 | 0–1 | Middleware/Caller/trait |
| Regex de e-mail no front | 8 | 0–1 | `type=email` + `isEmail()` único |
| `bearer()` manuais | 116 | 0 | `apiFetch` injeta auth |
| Construtores de query | 3 | 1 | `client.ts` |
| Exports mortos no front | 15 | 0 | Remover |
| Exports só internos | ~150 | ~0 | Remover `export` |

---

## 7. Plano de execução em fases (sem alterar contrato)

> Fazer em branches separados (`dev/backend` e `dev/frontend`), commits atômicos e testes verdes em
> cada etapa. **Nunca** misturar refatoração estrutural com mudança de comportamento.

### Fase 0 — Baseline e rede de segurança

- Rodar `php artisan test` no backend e `npm run check && npm run test` no front; registrar o
  resultado.
- Exportar a lista de rotas (`php artisan route:list --path=api`) e o contrato em
  `docs/contract.gap.md` como snapshot.
- Congelar a árvore atual em um commit/tag para diff posterior.

**Critério de aceite:** suíte verde e snapshot de rotas salvo.

### Fase 1 — Remover artefatos e scaffolds (BAIXO)

- Remover do repositório: workflows Firebase, `.claude` vazio, scaffold Vite/resources/package.json
  do backend, deps dev não usadas (`sail`, `faker` se confirmado).
- Limpar workspace: `build/`, `.svelte-kit/`, `bootstrap/cache/*.php`, `.phpunit.result.cache`,
  `database.sqlite` local.
- Remover `frontend/docs` ou movê-lo para a documentação raiz.

**Critério de aceite:** build/test continuam verdes; nenhum arquivo de runtime é afetado.

### Fase 2 — Código morto declarado (BAIXO/MÉDIO)

- Backend: import não usado; 6 relações sem referência; `AuthController::bearer`; comando/serviço
  de dedup one-shot (após confirmar migração v9); `CusteioService::calcular` (após confirmar).
- Frontend: 15 exports mortos; `e2e` órfão; re-export circular de RH; `export` só interno (por
  módulo).
- Rodar a suíte a cada lote; remover testes que testavam apenas código removido.

**Critério de aceite:** nenhuma referência quebrada; contagem de exports/rotas inalterada.

### Fase 3 — Validação nativa (BAIXO/MÉDIO)

- Backend: remover `authorize()` vazio; trocar regex de mês por `date_format:Y-m`; trocar
  `filter_var` boolean por `$request->boolean`; mover `required_without`/`confirmed` para Requests;
  centralizar CPF/CNPJ; remover sniffing de e-mail na leitura.
- Frontend: remover as 8 regex de e-mail; manter `type=email`; se necessário um único `isEmail`.

**Critério de aceite:** mesmos erros 422/401/409; testes de validação atualizados.

### Fase 4 — Boilerplate de identidade e HTTP (MÉDIO)

- Backend: `Caller` compartilhado; middleware/request resolve identidade; remover 36 `principal()`
  e 6 `*Principal` (ou manter só o que tem dado específico).
- Frontend: `apiFetch` com auth automática; remover `stockFetch`/`bearer()` manuais; unificar
  `buildQuery`; remover barrels.
- Atualizar testes de unidade do client/API.

**Critério de aceite:** nenhuma chamada autenticada sem token; refresh 401 continua funcionando.

### Fase 5 — Eventos, exceptions e config (MÉDIO)

- Consolidar 3 nomes de evento duplicados em `app/Events/`; ajustar publishers/listeners.
- Consolidar exceptions em `ApiException` + semânticas reais.
- Consolidar configs em `config/fablab.php`; atualizar `routes/console.php`.
- Remover aliases de rota sem consumidor documentado.

**Critério de aceite:** testes de eventos/schedulers verdes; nenhuma rota do front quebrada.

### Fase 6 — Movimento estrutural (MÉDIO/ALTO)

Fazer **um módulo por commit**, sempre com:
1. `git mv` dos arquivos para a árvore-alvo.
2. Ajuste de namespace/imports.
3. Ajuste de `routes/api.php`/`routes/modules/<modulo>.php`.
4. Ajuste de `composer.json` autoload (se necessário) e `AppServiceProvider`.
5. `php artisan route:list` + testes do módulo + testes globais.

Ordem sugerida: **Auth → Notification → Estoque → Vendas → Financeiro → Producao → Rh → Dashboard**
(Auth/Notification são menores; Dashboard depende de Contracts e deve ser o último).
Começar pelo backend; depois alinhar o front (que não depende da árvore interna, só do contrato).

**Critério de aceite:** paths `/api/**` idênticos; suíte completa verde; `git grep "app/Modules"`
não encontra referências quebradas.

### Fase 7 — Documentação e CI

- Uma fonte versionada: `README.md` (visão geral/execução), `docs/architecture.md` (estrutura-alvo),
  `docs/api.md` (contrato gerado), `docs/modules/<modulo>.md` (responsabilidade, rotas, modelos,
  eventos), `docs/decisions/ADR-*.md` (decisões duráveis).
- Remover docs duplicadas/desatualizadas (`frontend/docs`, seção de legado da infra).
- CI: rodar `php artisan test`, `npm run check`, `npm run test`, `npm run build`; opcional:
  `vendor/bin/pint --test`, `vendor/bin/phpstan analyse`, `npx knip`.

**Critério de aceite:** onboarding consegue rodar o projeto só pelo README + docs; nenhuma doc cita
Sanctum/adapter-static/Gateway.

### Fase 8 — Guardrails contra regressão

- Backend: PHPStan/Larastan (nível mínimo 5) + Pint; regra de revisão: “service só com regra real”.
- Frontend: `svelte-check` + `knip`/`ts-prune` no CI para exports mortos.
- Teste de fronteira: um teste que falha se um módulo importar Model/Service de outro fora de
  `Contracts` (pode ser um script simples de grep no CI).
- Checklist de PR: sem `authorize(){return true;}`, sem `Principal` novo, sem evento duplicado, sem
  regex de e-mail, sem barrel novo.

---

## 8. Checklist de verificação (comandos)

### Backend

```sh
# a partir do worktree/branch dev/backend
php artisan test
php artisan route:list --path=api
composer dump-autoload -o

# boilerplate que deve sumir
grep -R "return true;" app/Modules/*/Http/Requests | wc -l
grep -R "private function principal" app/Modules | wc -l
grep -R "FILTER_VALIDATE_EMAIL\|regex:/\^\\\\d{4}-\\\\d{2}" app | wc -l

# árvore-alvo: não deve sobrar Modules/*/Http
find app/Modules -type d -name Http
```

### Frontend

```sh
npm run check
npm run test
npm run build

# e-mail manual (deve ser 0 ou 1 util central)
grep -R "\^\[\^\\\\s@\]" src | wc -l

# wrappers/duplicatas
grep -R "function montarQuery\|function buildQuery\|function stockFetch" src/lib/api
grep -R "headers: bearer()" src/lib/api | wc -l

# exports mortos (instalar knip ou ts-prune como dev dependency)
npx knip
# ou
npx ts-prune
```

### Contrato (após cada fase)

```sh
# antes e depois: diff dos paths, métodos e status
php artisan route:list --json > /tmp/rotas-antes.json
# ... refatorar ...
php artisan route:list --json > /tmp/rotas-depois.json
diff <(jq -S '.[].uri' /tmp/rotas-antes.json) <(jq -S '.[].uri' /tmp/rotas-depois.json)
```

> O diff deve ser **vazio** exceto pelos aliases que você decidiu remover com aprovação.

---

## 9. Anexos

### Anexo A — Todos os pontos de regex de e-mail no frontend

```
src/lib/components/configuracoes/ModalUsuario.svelte:17,60
src/routes/(app)/configuracoes/perfil/+page.svelte:42,74
src/routes/(app)/pessoas/[id]/editar/+page.svelte:94,98
src/routes/(app)/pessoas/novo/+page.svelte:71
src/routes/(app)/pessoas/processo-seletivo/+page.svelte:260
src/routes/(app)/vendas/clientes/[id]/+page.svelte:89
src/routes/(app)/vendas/clientes/novo/+page.svelte:204
```

### Anexo B — Exports frontend realmente sem referência

```
AtribuirTarefaFinalPayload   src/lib/types/rh.ts
CusteioState                 src/routes/(app)/financeiro/custeio/+page.ts
filtrosDaUrl                 src/lib/api/producao/client.ts:44
mostrarDeAte                 src/lib/api/producao/client.ts:123
paginaAnterior               src/lib/api/producao/client.ts:119
paginaSeguinte               src/lib/api/producao/client.ts:115
paginadoVazio                src/lib/api/producao/client.ts:103
ItemMovementsResult          src/lib/types/stock.ts
UpdateBomPayload             src/lib/types/stock.ts
KanbanItem                   src/lib/types/producao.ts
MesaStatus                   src/lib/types/producao.ts
Parametro5S                  src/lib/types/producao.ts
LoginRequest                 src/lib/types/auth.ts
ResumoFinanceiro             src/lib/types/financeiro.ts
TotaisContas                 src/lib/types/financeiro.ts
```

### Anexo C — Relações Eloquent sem referência (app + tests)

```
Producao/Models/Maquina.php::usos
Producao/Models/ItemInspecao5S.php::inspecao
Producao/Models/ItemInspecao5S.php::checklist
Producao/Models/HistoricoUsoMaquina.php::maquina
Producao/Models/AuditoriaProjetoMesa.php::projetoMesa
Producao/Models/AdvertenciaMembro.php::inspecao
```

### Anexo D — Eventos com `NAME` duplicado

```
compra.solicitada.event:
  Estoque/Events/CompraSolicitadaEvent.php
  Financeiro/Events/CompraSolicitadaEvent.php

producao.concluida.event:
  Estoque/Events/ProducaoConcluidaEvent.php
  Producao/Events/ProducaoConcluidaEvent.php

producao.status.alterado.event:
  Producao/Events/ProducaoStatusAlteradoEvent.php
  Vendas/Events/ProducaoStatusAlteradoEvent.php
```

### Anexo E — FormRequests com `authorize` cosmético

```
Auth (10)        app/Modules/Auth/Http/Requests/*.php
Rh (16)          app/Modules/Rh/Http/Requests/*.php
Estoque (8)      app/Modules/Estoque/Http/Requests/*.php
Vendas (15)      app/Modules/Vendas/Http/Requests/*.php
Financeiro (8)   app/Modules/Financeiro/Http/Requests/*.php
Producao (21)    app/Modules/Producao/Http/Requests/*.php
Notification (5) app/Modules/Notification/Http/Requests/*.php  # custom, mas redundante com auth:api
```

### Anexo F — Exemplos de mapeamento de namespace

| Hoje | Alvo |
|---|---|
| `App\Modules\Rh\Http\Controllers\PessoaController` | `App\Http\Controllers\Rh\PessoaController` |
| `App\Modules\Rh\Http\Requests\PessoaRequest` | `App\Http\Requests\Rh\PessoaRequest` |
| `App\Modules\Rh\Http\Resources\PessoaResource` | `App\Http\Resources\Rh\PessoaResource` |
| `App\Modules\Rh\Models\Pessoa` | `App\Models\Rh\Pessoa` |
| `App\Modules\Rh\Services\PessoaService` | `App\Services\Rh\PessoaService` |
| `App\Modules\Rh\Policies\RhPolicy` | `App\Policies\RhPolicy` |
| `App\Modules\Rh\Contracts\RhContract` | `App\Contracts\RhContract` |
| `App\Modules\Rh\Events\NivelAlteradoEvent` | `App\Events\NivelAlteradoEvent` |
| `App\Modules\Rh\Listeners\ProcessarPontoRfid` | `App\Listeners\Rh\ProcessarPontoRfid` |
| `App\Modules\Rh\Enums\NivelAcesso` | `App\Enums\Rh\NivelAcesso` |

### Anexo G — Inconsistências de documentação a corrigir

| Documento | Afirmação atual | Realidade |
|---|---|---|
| `.opencode/specs/system/architecture.md:51,169` | Sanctum | JWT (`php-open-source-saver/jwt-auth`) + blacklist própria |
| `.opencode/specs/system/architecture.md:61` | `adapter-static` | `adapter-node` (`frontend/svelte.config.js:1`) |
| `.opencode/specs/system/architecture.md:23,75` | `app/Shared/Contracts` / `Shared/Auth` | Só existe `app/Shared/Exceptions` |
| `infra/docs/ARCHITECTURE.md:86-89` | `*-service/` ainda existem | Scaffolds removidos no commit `a2db21e` |
| `frontend/README.md:17,40,44,51` | Gateway, `services/`, `(auth)`, `integration/` | Proxy Vite, `lib/api/`, `auth/`, `tests/unit/` |
| `backend/README.md` | Estrutura descreve a árvore antiga e “Services/ (ou Actions/)” | Não há `Actions/`; após a refatoração o README precisa refletir a árvore centralizada |

### Anexo H — Premissas e limites desta análise

- A contagem e os caminhos refletem o estado dos branches citados; mudanças posteriores invalidam
  números pontuais.
- “Código inútil” foi classificado por análise estática (sem executar a suíte). Antes de remover,
  rode os testes e confirme consumidores externos (hardware RFID, integrações, contratos com o
  front, rotas usadas por scripts).
- O backend real não está no checkout atual (`dev/frontend`); para aplicar a refatoração, use o
  worktree/branch `dev/backend`.
- Este documento **não** altera rotas, payloads, enums, status HTTP, RBAC ou schema. Qualquer
  mudança de contrato é uma decisão separada, com atualização de `docs/contract.gap.md`.

---

## 10. Ordem recomendada (resumo de uma página)

1. **Não comece pela mudança de pastas.** Primeiro mate o código inútil de baixo risco.
2. Remova scaffold/artefatos/workflows errados.
3. Remova `authorize` vazio, imports/relações/exports mortos, regex de e-mail.
4. Troque validações manuais por `email`, `date_format`, `boolean`, `required_without`, `confirmed`.
5. Consolide HTTP (um `apiFetch` com auth, um `buildQuery`) e identidade (`Caller`).
6. Consolide eventos, exceptions e configs.
7. Só então mova as pastas, um módulo por vez, com `route:list` + testes verdes.
8. Atualize a documentação e adicione guardrails (PHPStan, knip, teste de fronteira).

O resultado esperado é um projeto em que **Models, Views e Controllers têm uma casa clara**, os
módulos continuam separados por namespace/subpasta, e a regra “cada arquivo tem um objetivo” deixa
de depender de conhecimento tribal.

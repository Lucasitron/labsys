# Arquitetura do Sistema FabLab — Monólito Modular Laravel (MVC)

> **Arquitetura vigente:** **um monólito modular em MVC, em PHP com Laravel** (`backend/`).
> Fontes: este documento, `system/conventions.md`, `system/rbac-matrix.md`, `system/STATUS.md` e o
> código real (models, migrações, controllers). Ambiente: local (Ubuntu + Docker Compose + Tailscale).
> O **frontend é SPA SvelteKit** (`specs/frontend/`); fala com **um único endpoint HTTP** (`/api`).
>
> > O material antigo (bounded contexts descritos como microsserviços Java/Spring) está arquivado em
> > `docs/_archive/2026-10-19/` e no snapshot `.opencode/backups/2026-10-19-pre-rewrite.tar.gz`.

## Visão Geral

Um único app **Laravel** (PHP 8.3) — `backend/` — servido por **um** PHP-FPM/Nginx em **uma** porta
(`8080`) e **um** banco PostgreSQL (`fablab`) com **schema por módulo**. Cada bounded context é um
**módulo** isolado por namespace + schema e, internamente, em **MVC**
(`Model` Eloquent → `Controller` → `Resource`/`View`).

Motivo: menos partes móveis (menos infraestrutura, configuração e contrato entre processos),
preservando as fronteiras de domínio. **Removidos do alvo:** Eureka, API Gateway, um banco por
serviço e HTTP entre serviços.

Comunicação:
- **Síncrona entre módulos:** chamada direta ao `Contract` público (`app/Shared/Contracts` ou
  `<Modulo>/Contracts`) do módulo dono, in-process.
- **Assíncrona:** `Event` do Laravel (com fila nativa, driver `database` por padrão). RabbitMQ só
  quando um contrato exigir entrega assíncrona/desacoplada real — nunca para simular HTTP entre módulos.

## Módulos (bounded contexts)

| # | Módulo (namespace) | Contexto | Schema | Rotas (mantidas p/ o front) |
| :--- | :--- | :--- | :--- | :--- |
| 0 | `App\Shared` | Config, auth, exceções, eventos | — | — |
| 1 | `App\Modules\Auth` | Auth & Identity | `auth` | `/api/auth/**` |
| 2 | `App\Modules\Rh` | Pessoas & RH | `rh` | `/api/rh/**` |
| 3 | `App\Modules\Estoque` | Estoque & Suprimentos | `estoque` | `/api/estoque/**` |
| 4 | `App\Modules\Vendas` | Vendas & CRM | `vendas` | `/api/vendas/**` |
| 5 | `App\Modules\Financeiro` | Financeiro | `financeiro` | `/api/financeiro/**` |
| 6 | `App\Modules\Producao` | Produção & Projetos | `producao` | `/api/producao/**` |
| 7 | `App\Modules\Notification` | Notification | `notification` | `/api/notification/**` |
| 8 | `App\Modules\Dashboard` | Dashboard (agregador, sem persistência) | — | `/api/dashboard/**` |

> As rotas `/api/<modulo>/**` são o contrato com o front e **não mudam**. A fronteira antes feita
> pelo gateway é o middleware de auth + `Policy` do Laravel.

## Stack Tecnológica

### Backend (alvo)
- **Linguagem:** PHP 8.3 (LTS) · **Framework:** Laravel 12 · **Deps:** Composer
- **Servidor:** PHP-FPM + Nginx (porta `8080`)
- **MVC:** Eloquent (`Model`) · `Controller` · API `Resource`/Blade (`View`); validação com `FormRequest`
- **Auth:** `jwt-auth` (guard `jwt`, Bearer) + `Policy`/`Gate`; bcrypt (padrão)
- **Async:** `Event`/`Listener`/`Job` com fila `database` (`php artisan queue:work`)
- **Persistência:** PostgreSQL único (`fablab`), script de migração do Laravel, schema por módulo
- **Filas/cache/sessão:** drivers nativos do Laravel; Redis/RabbitMQ só se o contrato exigir

### Fora do alvo (histórico)
Eureka/`discovery-service`, `gateway-service`, um banco por serviço, HTTP entre módulos e a stack
Java/Spring/Maven/Flyway/JPA. Camadas forçadas (`Repository`/`Service` "por hábito") são evitadas (§MVC).

### Frontend (inalterado)
SvelteKit + TypeScript (SPA, `ssr=false`, `adapter-node`), Tailwind CSS, REST em `VITE_API_BASE_URL`
(padrão `http://localhost:8080/api`). Detalhes em `specs/frontend/architecture.md`.

### Infraestrutura
Ubuntu Server 22.04/24.04 LTS · Docker + Docker Compose · Tailscale · Nginx
(futuro: Prometheus/Grafana, Loki).

## Estrutura do Monólito (MVC)

```
backend/
├── composer.json
├── app/
│   ├── Events/                     # 1 classe por const NAME (eventos consolidados)
│   ├── Http/Controllers/           # Controller base (principal() → Caller)
│   ├── Rules/                      # regras compartilhadas (ex.: CpfCnpjRule)
│   ├── Shared/                     # transversal — não depende de módulos
│   │   ├── Auth/Caller.php         # identidade do chamador (idUser/role/admin)
│   │   ├── Exceptions/             # ApiException + ErrorResponse
│   │   └── Contracts/              # interfaces públicas entre módulos (só o consumido)
│   └── Modules/<Modulo>/
│       ├── Models/                 # Eloquent — schema do módulo ($table = '<schema>.<tabela>')
│       ├── Http/
│       │   ├── Controllers/        # C — só HTTP. Delega ao Model/Service
│       │   ├── Requests/           # FormRequest (validação/autorização)
│       │   └── Resources/          # View (JSON) — shape de saída
│       ├── Services/ (ou Actions/) # só quando há regra de negócio real (não forçar p/ CRUD)
│       ├── Policies/               # RBAC do módulo
│       ├── Events/  Listeners/     # eventos internos
│       └── routes.php              # rotas /api/<modulo>
├── bootstrap/  config/  routes/    # app/console.php (agendamentos)
├── database/
│   ├── migrations/<modulo>/        # migrações por schema
│   └── seeders/
└── tests/ (Unit | Feature)
```

Regras de fronteira:
- O módulo lê/escreve **só** o próprio schema.
- **MVC**: `Controller` só orquestra HTTP (valida via `FormRequest`, delega e devolve `Resource`);
  **regra de negócio** fica em `Service`/`Action` **ou** no `Model` quando for simples — não criar
  camada nem `Repository` só por hábito (Eloquent já é o acesso a dados).
- Dependências: módulo → `Shared`; módulo → outro módulo **apenas** via `Contract` ou `Event` (sem ciclos).
- `config`/`exceptions`/middleware **não** se duplicam por módulo — vivem em `Shared`/`app/`.

## Persistência

- Um Postgres `fablab`; schema por módulo (`auth`, `rh`, `estoque`, `vendas`, `financeiro`,
  `producao`, `notification`).
- Migrações do Laravel por módulo em `database/migrations/<modulo>/`, tabelas qualificadas por
  schema (ex.: `Schema::create('rh.pessoa', …)`); Eloquent `protected $table = 'rh.pessoa'`.
- Sem join entre schemas por conveniência: leitura cruzada via `Contract` do módulo dono.
- `$fillable` explícito (evita mass assignment); casts de tipo no `Model`.

### Migrações Reais por Módulo (reaproveitadas)

- **auth:** V1 `login` · V2 `user_permissions` · V3 `token_blacklist` · V4 `access_log` (índice `uuid_rfid`, `timestamp DESC`) · V5 `situacao` em `login` (default ATIVO) · V6 `permissao_matriz` + seed 8 módulos × 5 roles · V7 `parametro_sistema` + 4 seeds · V8 `token_integracao` (só hash+prefixo).
- **rh:** V1 `pessoa` · V2 `funcionario` · V3 `tutor` · V4 `registro_ponto_diario` · V5 `apontamento_horas` ·
  V6 `processo_seletivo` · V7 `treinamento` · V8 `avaliacao_treinamento` · V9 `historico_nivel` · V10 (certificado:
  `solicitacao_certificado`, `certificado_emitido`, `hora_consolidada`).
- **estoque:** V1 `item` · V2 `localizacao` · V3 `fornecedor` · V4 `entrada_estoque` · V5 `saida_estoque` ·
  V6 `emprestimo` · V7 `lista_materiais` (BOM) · V8 `item_bom`.
- **vendas:** V1 `cliente` · V2 `tag_cliente` · V3 `cliente_tag` · V4 `orcamento` · V5 `item_orcamento` ·
  V6 `encomenda` · V7 `historico_status_encomenda` · V8 `registro_marketplace` · V9 `interacao_cliente` · V10 `tarefa_marketing`.
- **financeiro:** V1 `categoria_financeira` · V2 `lancamento_financeiro` · V3 `doacao_recurso` · V4 `valor_hora_nivel` ·
  V5 `parametro_overhead` · V6 `fechamento_encomenda` · V7 `custo_encomenda` · V8 `solicitacao_compra` · V9 `horas_encomenda`.
- **producao:** V1 `projeto` · V2 `tarefa` · V3 `encomenda_kanban` · V4 `historico_kanban` · V5 `maquina` ·
  V6 `historico_uso_maquina` · V7 `setor` · V8 `setor_material` · V9 `setor_sinalizacao` · V10 `setor_checklist` ·
  V11 `setor_responsavel` · V12 `inspecao_5s` · V13 `item_inspecao_5s` · V14 `advertencia_membro` · V15 `projeto_mesa` ·
  V16 `auditoria_projeto_mesa` · V17 `parametro_5s` · V18 `consumo_encomenda`.
- **notification:** V1 `notificacao` · V2 `notificacao_historico` · V3 `configuracao_canal` · V4 seed
  `configuracao_canal` (EMAIL habilitado, WHATSAPP desabilitado).

## Comunicação

### Síncrona (in-process)
- Chamada direta ao `Contract` do módulo dono (substitui HTTP/API Gateway).
- Ex.: `Dashboard` agrega `Rh` + `Notification` chamando seus `Contracts`.

### Assíncrona (eventos)
- Padrão de nome mantido: `<contexto>.<entidade>.<ação>.event`.
- Padrão do alvo: `Event` + `Listener`/`Job` do Laravel (`ShouldQueue`, fila `database`).
- RabbitMQ (`fablab.*`) só onde o contrato exigir entrega durável/assíncrona real.

### Eventos Consolidados (reais — 16 do MVP, reaproveitados)
| Evento | Origem → Destino |
| :--- | :--- |
| `access.rfid.event` | Auth → RH (ponto) |
| `nivel.alterado.event` | RH → Auth, Notification |
| `horas.validadas.event` | RH → Financeiro |
| `estoque.baixo.event` | Estoque → Notification |
| `emprestimo.atrasado.event` | Estoque → Notification |
| `compra.solicitada.event` | Estoque → Notification/Financeiro (informativo) |
| `encomenda.criada.event` | Vendas → Produção |
| `encomenda.status.alterado.event` | Vendas/Produção → Notification |
| `orcamento.aprovado.event` | Vendas → Financeiro, Notification |
| `kanban.status.alterado.event` | Produção/Vendas → Notification |
| `advertencia.registrada.event` | Produção → Notification |
| `projeto.mesa.abandonado.event` | Produção → Notification, RH |
| `certificado.solicitado.event` | RH → Notification |
| `certificado.aprovado.event` | RH → Notification |
| `certificado.rejeitado.event` | RH → Notification |
| `extrato.mensal.horas.event` | RH → Notification |
| `lancamento.vencido.event` | Financeiro → Notification |

> No monólito, "origem → destino" vira chamada de `Contract` ou `Event`; o nome do evento é
> preservado para não reescrever contratos.

## Segurança

- Auth de API via **`jwt-auth`** (guard `jwt`, Bearer token); papéis em `Policy`/`Gate` +
  middleware (`system/rbac-matrix.md`).
- Senhas com `Hash::make` (bcrypt); nunca texto puro. Credenciais via `.env` (nunca versionado).
- `FormRequest` valida e autoriza toda entrada; `$fillable` evita mass assignment.
- Eloquent usa bindings (sem SQL concatenado); Blade escapa por padrão (`{{ }}`).
- Blacklist de token no logout; purge agendado no módulo `Auth`.
- RFID/UUID tratado como dado sensível. Externo via Tailscale + Nginx.

## Agendamentos (reaproveitados — `routes/console.php` / `Schedule`)

| Módulo | Cron | Propósito |
| :--- | :--- | :--- |
| `Rh` | `0 6 1 * *` | 1º dia do mês 06:00 — `ExtratoMensalHorasEvent` (mês anterior) |
| `Auth` | `0 3 * * *` | Diário 03:00 — purge de tokens expirados |
| `Estoque` | `0 3 * * *` | Diário 03:00 — empréstimos vencidos → `emprestimo.atrasado.event` |
| `Notification` | `0 3 * * *` | Diário 03:00 — expurgo de histórico > 90 dias |
| `Notification` | `*/30 * * * *` | Reenvio de notificações `PENDENTE` a cada 30 min |

## Decisões Registradas

- `tutor` vs `instrutor`: tutor responde por grupo/PS/ensaio; instrutor ministra treinamento.
- Naming de recursos em PT (backend) vs EN (front): o path do backend (PT) é a fonte da verdade.
- "Horas disponíveis" = não consolidadas (RH) (histórico em `docs/_archive/2026-10-19/08-horas.md`).

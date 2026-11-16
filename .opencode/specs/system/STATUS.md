# STATUS / Linha do tempo do FabLab

> **Este é o documento-âncora do estado atual.** A linha do tempo começa no **Ponto 1** (o presente)
> — não reconstruímos o passado aqui. Todo o histórico anterior está congelado em
> `.opencode/backups/2026-10-19-pre-rewrite.tar.gz` (ver `MANIFEST.md`) e resumido no
> `.opencode/state/orchestration.json` (`resumo_cache`) e nos relatórios de `.opencode/reports/`.
>
> Regra: a cada entrega relevante, **acrescenta-se um Ponto** (nunca se reescreve o Ponto anterior).

---

## Ponto 1 — 2026-10-19 · Estado atual

### Arquitetura
- **Backend:** monólito modular **PHP 8.3 + Laravel 12** (`backend/`, porta `8080`), **um** Postgres
  `fablab` com **schema por módulo**. 8 módulos: `Auth`, `Rh`, `Estoque`, `Vendas`, `Financeiro`,
  `Producao`, `Notification`, `Dashboard` (+ `App\Shared`). Sem Eureka/Gateway/banco por serviço.
- **Frontend:** SPA **SvelteKit + TypeScript + Tailwind** (`frontend/`, `adapter-node`).
  `VITE_API_BASE_URL` **inclui `/api`**.
- Referência: `system/architecture.md` · `system/conventions.md` · `system/rbac-matrix.md`.

### Qualidade (última verificação)
- **Backend:** suíte **429 passed + 4 skipped**. Coverage ≥ 96%.
- **Frontend:** **556 testes** (37 arquivos), `npm run check` **0 erros** (17 warnings pré-existentes),
  build `adapter-node` OK.

### Deploy (servidor `fablabserver` — Tailscale `100.91.193.84`)
- Backend `:8080` · Front `:3000` · imagem do backend **`ghcr.io/fablab/backend:2026.10.17-3`**.
- Postgres container `fablab-postgres` (db `fablab`). Admin semeado com vínculo de funcionário (id=3).
- Health: `/up` = 200; front `/` = 200.

### Git
- `dev/backend` @ `c76d7f7` · `development` @ `0eeab43` · `dev/frontend` @ `6228ba9` (local; push pendente).
- Worktree: `dev/frontend` na árvore principal · `dev/backend` em `05-dev/labSys-back`.
- Regras em `docs/GITFLOW.md` (branches por vertente; `development` merge-only).
- **Propagação concluída:** `dev/backend → development` (`0eeab43`) e `development → dev/frontend` (`6228ba9`);
  único conflito foi `README.md` (mantido o do front, reescrito para o monólito). Legado Java removido em toda a cadeia.

### Última entrega funcional
- **RH — vínculo/acesso em `PUT /rh/pessoas`**: nível (Admin-only) + departamento (com limpeza via `''`)
  + provisionamento de acesso; admin semeado ganhou vínculo. Back `16ccba8`/`f2fbe84`, front
  `12d8f77`/`1e16bfc`. E2E validado no servidor.

### Pipeline de agentes
- 17 agentes reconfigurados (modelos válidos: `deepseek`/`big-pickle`/`glm`/`beam`). Codificação
  (`coder` e `frontend/coder`) em **`opencode/beam-free`**; `frontend/designer` em `big-pickle`. Build = `builder`
  (exclusivo); deploy = `deployer` (exclusivo); código = `coder`/`frontend/coder`. Ver
  `system/agents-pipeline.md`.

### Pendências abertas (herdadas)
- Desabilitar `fablab-front.service` antigo (porta `4173`) — requer `sudo`.
- Instabilidade do Tailscale (relay `sao`) durante deploys.
- Homologar o checklist do `front-handoff-2026-10-12.md`.
- **Feature futura registrada:** `temAcesso` (omitir campo de acesso quando a pessoa já tem login).
- Itens de contrato/produto: ver `docs/contract.gap.md` §5 e `orchestration.json` → `proximos_passos`.

---

## Ponto 2 — 2026-11-08 · Remap estrutural (fases 0–8)

### Entregue
- **Backend layer-first:** `app/Http/{Controllers,Requests,Resources}/<Modulo>`, `app/Models/<Modulo>`,
  `app/Services/<Modulo>`, `app/Enums/<Modulo>`, `app/Listeners/<Modulo>`, `app/Contracts`,
  `app/Events`, `app/Policies`, `app/Rules`, `app/Exceptions`, `routes/modules/*.php`.
  `app/Modules/` foi eliminado.
- **Frontend:** `apiFetch` com auth automática + `buildQuery` único; sem `stockFetch`/`bearer()`
  manuais/barrels `export *`; `isEmail` único; `Paged<T>` morto removido; README e CI atualizados.
- **Guardrails:** `ArchitectureBoundaryTest`, `EventRegistrationTest`, `vendor/bin/pint --test` no
  CI backend e checklist de PR.

### Evidências
- Backend: **433 passed + 4 skipped (2488 assertions)**; 223 rotas com diff vazio; Pint OK.
- Frontend: `npm run check` **0 erros / 17 warnings**; **563 testes (39 arquivos)**; build
  `adapter-node` OK.
- Contratos: rotas/payloads/enums/status/RBAC/eventos sem alteração observável.
- Git integrado em `development`, `dev/backend`, `dev/frontend`, `refactor/remap-backend` e
  `refactor/remap-frontend` @ **`894621d`** (sem push).

### Pendências
- Reaplicar/avaliar o WIP de RH/RBAC `8d50487` (preservado em `wip/backend-pre-remap`) sobre a
  árvore layer-first; corrigir `CertificadosTest::test_decidir_duas_vezes_e_corrida_por_horas`.
- Migrar 14 `URLSearchParams` manuais do front para `buildQuery`; tooling `knip`/PHPStan nível 5.
- Divergências front↔back pré-existentes (paths `/stock/*`, kanban `PATCH`/`PUT`, `/rh/people`,
  endpoints 5S) — backlog de contrato.
- `git push` e deploy: aguardam decisão do PO / `deployer`.

### Relatório
`.opencode/reports/0006-remap-simplificacao.md`

---

## Ponto 3 — 2026-11-16 · WIP RH/RBAC reaplicado (vínculo, matriz e Admin total)

### Entregue
- **RBAC:** Admin nunca é reduzido — `PUT /permissoes/{modulo}/ADMIN` != `EDITAR` → 422 e leitura
  força `ADMIN = EDITAR`.
- **RH/vínculo:** `PessoaResource` expõe `idFuncionario`, `nivelAcesso` e `departamento`;
  `FuncionarioService::vincular` espelha `role` do Auth e impede rebaixar Admin; `GET /rh/niveis`
  passa a retornar `membros`; eager load de `funcionario` nas pessoas.
- **Segurança:** `POST /rh/pessoas` agora exige Admin para `nivelAcesso` e `criarAcesso`
  (antes qualquer autenticado podia criar conta Admin); `ApontamentoService` retorna 403 explícito
  (não 500) para Admin sem vínculo. Certificados seguem 403 sem vínculo (paridade Java).
- **Front:** `getNiveis` mapeia `membros`; `/pessoas/novo` trata nível e acesso como Admin-only
  (como `/pessoas/[id]/editar`).

### Evidências
- Backend: **444 passed + 4 skipped (2537 assertions)**; Pint OK (624 arquivos); 223 rotas com diff
  `(method, uri, name, middleware)` vazio.
- Frontend: `npm run check` **0 erros / 17 warnings**; **569 testes (39 arquivos)**; build
  `adapter-node` OK (5,9M).
- Git integrado em `development`, `dev/backend`, `dev/frontend`, `refactor/remap-backend` e
  `refactor/remap-frontend` @ **`80dc8bb`** (sem push).
- WIP original `8d50487` avaliado: hunks que permitiam Admin sem vínculo decidir/validar foram
  rejeitados (auditoria nula/paridade Java); o restante foi reaplicado.

### Pendências
- Ocultar/desabilitar a linha **Admin** em `/configuracoes/permissoes` (hoje recebe 422 na UI).
- Avaliar paginação/limite de `NivelService::matriz` se a base de funcionários crescer.
- Migrar 14 `URLSearchParams` manuais; tooling `knip`/PHPStan nível 5; backlog de contrato.
- `git push` e deploy: aguardam decisão do PO / `deployer`.

### Relatório
`.opencode/reports/0007-wip-rh-rbac.md`

---

## Próximos Pontos (modelo)

```
## Ponto N — AAAA-MM-DD · <título>
- Entregue: ...
- Evidências: ... (commits, testes, imagens)
- Pendências: ...
- Relatório: .opencode/reports/<NNNN>-<slug>.md
```

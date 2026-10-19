# labSys — Sistema de Gestão FabLab

Sistema gerencial (ERP) para um Fab Lab / Makerspace: **um monólito modular em PHP/Laravel (MVC)**
no backend e uma **SPA em SvelteKit** no frontend, sobre **PostgreSQL** único com schema por módulo.
Execução local/servidor (Ubuntu + Docker Compose + Tailscale). Sem nuvem, sem microsserviços.

## Arquitetura

```
   Cliente (SPA SvelteKit)
            │  HTTP  /api  (Bearer token)
            ▼
   ┌───────────────────────────────┐
   │  backend/  — Laravel 12 (:8080)│
   │  módulos: Auth · Rh · Estoque  │
   │  Vendas · Financeiro · Producao│
   │  Notification · Dashboard      │
   └───────────────┬───────────────┘
                   ▼
      PostgreSQL `fablab` (schema por módulo)
```

- **Comunicação:** dentro do processo — chamada direta ao `Contract` do módulo dono; assíncrono por
  `Event`/`Listener`/`Job` do Laravel (fila `database`). Sem Eureka, Gateway ou banco por serviço.
- **Front:** `frontend/` (SvelteKit + TypeScript + Tailwind, `adapter-node`), fala com **um** endpoint
  `/api`. `VITE_API_BASE_URL` **deve incluir `/api`**.

## Estrutura do repositório

```
labSys/
├── backend/            # monólito modular Laravel (PHP 8.3) — porta 8080
├── frontend/           # SPA SvelteKit
├── infra/              # Docker Compose, Nginx, entrypoint
├── docs/               # documentação local (não versionada): contract, mockups, archive
└── .opencode/          # specs, estado vivo e agentes (pipeline multiagente)
```

> Diretórios `*-service/` restantes são resquícios do scaffold Java antigo (não fazem parte do alvo).

## Como executar

```sh
# 1) infraestrutura (Postgres + app + worker + scheduler)
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d

# 2) frontend
cd frontend && npm ci
VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

- App: `http://localhost:8080/api` · Front: `http://localhost:5173`
- Admin de desenvolvimento (seed): `admin` / `admin123` — **dev-only**.

## Git Flow

`main` (produção) · `testing` (homologação) · `development` (merge-only) → `dev/backend` + `dev/frontend`.
Nunca commitar direto em `main`/`testing`/`development`; commits de back e front são separados.
Detalhes em [`docs/GITFLOW.md`](docs/GITFLOW.md).

## Documentação

- **Estado e linha do tempo:** `.opencode/specs/system/STATUS.md`
- **Arquitetura / convenções / RBAC / agentes:** `.opencode/specs/system/`
- **Contrato Backend × Frontend:** [`docs/contract.gap.md`](docs/contract.gap.md)
- **Mockups por módulo:** `docs/mokups/`
- **Histórico (arquivado):** `docs/_archive/2026-10-19/`

## Licença

GNU General Public License v3. Ver [`LICENSE`](LICENSE).

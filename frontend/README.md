# frontend — FabLab SPA

Interface web do Sistema de Gestão FabLab. **SvelteKit + TypeScript + Tailwind CSS**.

## Pré-requisitos

*   Node.js 20+ e npm

## Como executar

```sh
npm install
npm run dev
```

A aplicação roda em http://localhost:5173. `VITE_API_BASE_URL` deve incluir o
prefixo `/api` (ex.: `http://localhost:8080/api`). Em dev, o proxy do Vite
encaminha `/api` para o backend.

## Como testar

```sh
npm run test        # testes unitários (Vitest)
npm run check       # typecheck (svelte-check)
```

## Como buildar

```sh
npm run build       # gera o bundle adaptado para Node (adapter-node)
npm run preview     # serve o bundle em modo produção
```

## Estrutura

```text
src/
├── lib/
│   ├── api/          # clientes HTTP por módulo (client.ts = fetch + auth + buildQuery)
│   ├── components/   # componentes reutilizáveis
│   ├── config/       # configuração de UI
│   ├── stores/       # stores Svelte (estado global)
│   ├── types/        # tipos TypeScript
│   └── utils/        # utilitários puros (formatação, validação)
├── routes/
│   ├── (auth)/       # rotas de autenticação
│   ├── (app)/        # rotas autenticadas
│   └── +layout.svelte
├── app.html
└── app.css
tests/
└── unit/             # Vitest
```

Documentação do sistema: [`../README.md`](../README.md) e `.opencode/specs/` (local).
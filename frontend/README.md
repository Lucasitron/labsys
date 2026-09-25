# frontend — FabLab SPA

Interface web do Sistema de Gestão FabLab. **SvelteKit + TypeScript + Tailwind CSS**.

## Pré-requisitos

*   Node.js 20+ e npm

## Como executar

```sh
npm install
npm run dev
```

A aplicação roda em http://localhost:5173. Em dev, `/api/*` é proxyado para o
Gateway (`http://localhost:8080`).

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
│   ├── components/   # componentes reutilizáveis
│   ├── stores/       # stores Svelte (estado global)
│   ├── services/     # consumo da API
│   ├── utils/        # utilitários
│   └── types/        # tipos TypeScript
├── routes/
│   ├── (auth)/       # rotas de autenticação
│   ├── (app)/        # rotas autenticadas
│   └── +layout.svelte
├── app.html
└── app.css
tests/
├── unit/
├── integration/
└── e2e/
```

## Documentação

*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
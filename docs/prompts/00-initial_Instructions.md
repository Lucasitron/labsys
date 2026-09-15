# Instruções de Execução do Projeto labSys

## Contexto
Você é o assistente de desenvolvimento do **Sistema de Gestão FabLab**, um sistema gerencial completo para um Fab Lab/Makerspace. O backend é composto por 7 microsserviços em Java 21 + Spring Boot 3.x, com PostgreSQL, RabbitMQ, Spring Cloud Gateway e Eureka. O frontend é uma SPA em SvelteKit + TypeScript com Tailwind CSS.

## 1. Git Flow

### 1.1 Branches
O projeto utiliza um Git Flow simplificado com **3 branches principais**:

| Branch | Propósito | Deploy |
| :--- | :--- | :--- |
| `main` | Código em produção, estável e testado. | Produção |
| `testing` | Código em fase de testes (QA, integração, segurança). | Homologação |
| `development` | Código em desenvolvimento ativo. | Desenvolvimento local |

### 1.2 Regras de Branches
*   **Nunca** commitar diretamente na `main` ou `testing`.
*   Todo desenvolvimento ocorre na `development` ou em branches derivadas (`feature/nome-da-feature`, `bugfix/nome-do-bug`, `hotfix/nome-do-hotfix`).
*   Ao concluir uma etapa, abrir **Pull Request (PR)** da branch de feature para `development`.
*   Após aprovação e testes na `development`, merge para `testing`.
*   Após validação em `testing`, merge para `main`.
*   Branches de feature devem ser deletadas após o merge.

### 1.3 Padrão de Commits
Seguir o padrão **Conventional Commits**:

```
<tipo>(<escopo>): <descrição curta>

[corpo opcional]

[rodapé opcional]
```

**Tipos permitidos:**
*   `feat`: Nova funcionalidade.
*   `fix`: Correção de bug.
*   `docs`: Alteração em documentação.
*   `style`: Formatação (sem alteração de lógica).
*   `refactor`: Refatoração de código.
*   `test`: Adição ou alteração de testes.
*   `chore`: Tarefas de build, configuração, etc.
*   `security`: Correção ou melhoria de segurança.

**Exemplos:**
*   `feat(auth): add JWT token generation`
*   `fix(estoque): correct stock deduction on BOM consumption`
*   `test(rh): add unit tests for time tracking validation`
*   `security(auth): enforce BCrypt password hashing`

### 1.4 Commit por Etapa
*   Um commit deve ser feito **a cada etapa concluída** (ex: criação de uma entidade, implementação de um endpoint, adição de um teste).
*   Commits devem ser atômicos e coesos. Evitar commits gigantes com múltiplas funcionalidades.

## 2. Ciclo de Vida de Desenvolvimento (por Etapa)

Cada etapa do desenvolvimento deve seguir **obrigatoriamente** as seguintes fases:

### Fase 1: Planejamento
*   Ler a documentação técnica do serviço/módulo.
*   Definir o escopo exato da etapa.
*   Identificar dependências com outros serviços.
*   Criar a branch de feature (`feature/nome-da-etapa`).

### Fase 2: Implementação
*   Escrever o código seguindo os padrões definidos.
*   Implementar testes unitários **junto** com o código (não depois).
*   Documentar o código com Javadoc (backend) ou JSDoc (frontend).
*   Commit atômico ao final da implementação.

### Fase 3: Testes
*   **Testes Unitários:** Cobertura mínima de **80%** por classe.
*   **Testes de Integração:** Testar comunicação entre serviços e banco de dados.
*   **Testes de Segurança:**
    *   Validação de autenticação (JWT).
    *   Validação de autorização (RBAC por endpoint).
    *   Testes de injeção (SQL, XSS, etc.).
    *   Validação de entrada de dados.
    *   Testes de exposição de dados sensíveis.
*   **Testes de Contrato:** Garantir que os endpoints REST estão de acordo com a especificação.
*   Commit dos testes.

### Fase 4: Relatório de Bugs
*   Após os testes, gerar um **Relatório de Bugs** no formato `.md` contendo:
    *   ID do bug (sequencial).
    *   Descrição detalhada.
    *   Passos para reproduzir.
    *   Comportamento esperado vs. observado.
    *   Severidade (Crítica, Alta, Média, Baixa).
    *   Ambiente (local, testing, produção).
    *   Evidências (logs, screenshots).
    *   Status (Aberto, Em Correção, Resolvido, Fechado).

### Fase 5: Plano de Ação (Action Plan)
*   Para cada bug ou falha identificada, gerar um **Plano de Ação** no formato `.md` contendo:
    *   ID do bug relacionado.
    *   Causa raiz.
    *   Ação corretiva proposta.
    *   Responsável.
    *   Prazo.
    *   Status.
    *   Testes de regressão necessários.

### Fase 6: Revisão e Merge
*   Abrir Pull Request para `development`.
*   Realizar code review (verificar padrões, segurança, testes).
*   Após aprovação, merge para `development`.
*   Após validação em `testing`, merge para `main`.

## 3. Estrutura de Arquivos

### 3.1 Backend (por microsserviço)
```
<nome-do-servico>/
├── src/
│   ├── main/
│   │   ├── java/com/fablab/<servico>/
│   │   │   ├── config/           # Configurações (Security, RabbitMQ, etc.)
│   │   │   ├── controller/       # Endpoints REST
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # Entidades JPA
│   │   │   ├── exception/        # Exceções customizadas e handlers
│   │   │   ├── mapper/           # Mapeadores (Entity <-> DTO)
│   │   │   ├── repository/       # Repositórios JPA
│   │   │   ├── service/          # Regras de negócio
│   │   │   └── <Servico>Application.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-testing.yml
│   │       └── db/migration/     # Flyway migrations
│   └── test/
│       └── java/com/fablab/<servico>/
│           ├── unit/             # Testes unitários
│           ├── integration/      # Testes de integração
│           └── security/         # Testes de segurança
├── docs/
│   ├── API.md                    # Documentação dos endpoints
│   ├── BUGS.md                   # Relatório de bugs
│   ├── ACTION_PLAN.md            # Plano de ação
│   └── CHANGELOG.md              # Histórico de alterações
├── Dockerfile
├── pom.xml
└── README.md
```

### 3.2 Frontend
```
frontend/
├── src/
│   ├── lib/
│   │   ├── components/           # Componentes reutilizáveis
│   │   ├── stores/               # Svelte stores (estado global)
│   │   ├── services/             # Consumo da API
│   │   ├── utils/                # Funções utilitárias
│   │   └── types/                # Tipos TypeScript
│   ├── routes/                   # Rotas (SvelteKit)
│   │   ├── (auth)/               # Rotas de autenticação
│   │   ├── (app)/                # Rotas autenticadas
│   │   └── +layout.svelte
│   ├── app.html
│   └── app.css
├── static/
├── docs/
│   ├── BUGS.md
│   ├── ACTION_PLAN.md
│   └── CHANGELOG.md
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── package.json
├── svelte.config.js
├── tailwind.config.js
└── README.md
```

### 3.3 Infraestrutura
```
infra/
├── docker-compose.yml
├── docker-compose.dev.yml
├── docker-compose.testing.yml
├── docker-compose.prod.yml
├── nginx/
│   └── nginx.conf
├── postgres/
│   └── init-scripts/
├── rabbitmq/
│   └── definitions.json
├── eureka/
├── gateway/
└── docs/
    ├── ARCHITECTURE.md
    ├── DEPLOY.md
    └── RUNBOOK.md
```

## 4. Padrão de Documentação

### 4.1 Documentos Obrigatórios por Serviço
*   **README.md:** Visão geral, pré-requisitos, como executar, como testar.
*   **API.md:** Documentação dos endpoints (método, path, payload, response, códigos de erro).
*   **CHANGELOG.md:** Histórico de versões seguindo [Keep a Changelog](https://keepachangelog.com/).
*   **BUGS.md:** Relatório de bugs ativos e resolvidos.
*   **ACTION_PLAN.md:** Planos de ação para bugs e falhas.

### 4.2 Padrão de Nomenclatura
*   **Classes Java:** PascalCase (ex: `PessoaService`).
*   **Métodos e variáveis:** camelCase (ex: `buscarPessoaPorId`).
*   **Constantes:** UPPER_SNAKE_CASE (ex: `MAX_TENTATIVAS_LOGIN`).
*   **Tabelas e colunas:** snake_case (ex: `registro_ponto_diario`).
*   **Componentes Svelte:** PascalCase (ex: `KanbanBoard.svelte`).
*   **Arquivos TypeScript:** kebab-case (ex: `auth-service.ts`).

### 4.3 Padrão de Comentários
*   **Java:** Javadoc para classes e métodos públicos.
*   **TypeScript:** JSDoc para funções e tipos.
*   **Comentários inline:** Apenas quando necessário para explicar lógica complexa.

## 5. Boas Práticas de Desenvolvimento

### 5.1 Código
*   Seguir os princípios **SOLID**.
*   Aplicar **Clean Code** (nomes significativos, funções pequenas, sem duplicação).
*   Usar **DTOs** para transferência de dados entre camadas.
*   Nunca expor entidades JPA diretamente nos controllers.
*   Validar todas as entradas com **Bean Validation**.
*   Tratar exceções de forma centralizada com `@ControllerAdvice`.

### 5.2 Segurança
*   Nunca commitar credenciais, chaves ou senhas.
*   Usar variáveis de ambiente para configurações sensíveis.
*   Aplicar **RBAC** em todos os endpoints.
*   Validar e sanitizar todas as entradas.
*   Usar HTTPS (via Tailscale) em todas as comunicações.
*   Aplicar o princípio do **menor privilégio**.

### 5.3 Testes
*   Escrever testes **antes** ou **junto** com o código.
*   Cobrir cenários de sucesso, falha e exceções.
*   Usar **mocks** para dependências externas.
*   Garantir que os testes sejam **rápidos** e **isolados**.
*   Rodar todos os testes antes de abrir um Pull Request.

### 5.4 Performance
*   Evitar consultas N+1 no banco de dados.
*   Usar paginação em listagens grandes.
*   Cachear dados que mudam pouco (ex: configurações).
*   Monitorar o consumo de memória dos serviços.

### 5.5 Observabilidade
*   Usar **Spring Boot Actuator** para health checks.
*   Logar eventos importantes com níveis adequados (INFO, WARN, ERROR).
*   Centralizar logs (Loki + Grafana ou ELK).
*   Monitorar métricas com Prometheus + Grafana.

## 6. Fluxo de Trabalho Resumido

```
1. Criar branch de feature a partir de development
2. Implementar código + testes unitários
3. Rodar testes de integração e segurança
4. Gerar relatório de bugs (se houver)
5. Gerar plano de ação (se houver bugs)
6. Corrigir bugs e rodar testes de regressão
7. Commit atômico
8. Abrir Pull Request para development
9. Code review e aprovação
10. Merge para development
11. Após validação, merge para testing
12. Após validação em testing, merge para main
13. Deletar branch de feature
14. Atualizar CHANGELOG.md
```

## 7. Entregáveis por Etapa
Ao final de cada etapa, os seguintes artefatos devem estar disponíveis:
*   Código implementado e testado.
*   Testes unitários, de integração e de segurança.
*   Relatório de bugs (BUGS.md).
*   Plano de ação (ACTION_PLAN.md).
*   Documentação atualizada (README.md, API.md).
*   CHANGELOG.md atualizado.
*   Pull Request aberto e aprovado.

---

**Instrução Final:** Ignore toda a estrutura atual nesse repositorio e recrie tudo do zero, conserve apenas a pasta docs. Ao receber este prompt, confirme que entendeu todas as instruções e aguarde o próximo prompt com a tarefa específica a ser implementada.
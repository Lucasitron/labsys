# Matriz de Permissões (RBAC) — consolidada

> **Fonte:** código real (`user_permissions` com role 0..4 + `usuario_responsabilidade` granular),
> `docs/contract.gap.md` e `system/STATUS.md`. Onde divergir de spec antiga, **prevalece esta** (reflete o real).

## Níveis de Acesso (`user_permissions.role`)

| Nível | Nome | Tabela/Semântica |
| :--- | :--- | :--- |
| 0 | Admin | Acesso total; gerencia usuários/níveis/RBAC; acessa Financeiro (exclusivo); aprova/rejeita certificados, 5S-advertências, compras. |
| 1 | Bolsista | Acesso geral exceto Financeiro; edita conforme responsabilidade. |
| 2 | Voluntário | Acesso geral exceto Financeiro; edita conforme responsabilidade. |
| 3 | Estagiário | Visualiza próprio/atribuído. Sem RBAC de escrita. |
| 4 | Recrutando | Acesso mínimo (próprio). **Não participa do 5S, não recebe extrato mensal.** |

> No código real o role é inteiro 0..4 em `user_permissions`; a mesma escala
> 0-Admin…4-Recrutando vale em todos os módulos.
> O `login`/`pessoa` também carrega `setor` e `nivel_acesso` (histórico em `historico_nivel`).

## Matriz por Módulo

| Módulo / Funcionalidade | Admin (0) | Bolsista (1) | Voluntário (2) | Estagiário (3) | Recrutando (4) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Auth & Acesso** | | | | | |
| Login/Logout/Refresh | ✅ | ✅ | ✅ | ✅ | ✅ (login p/ candidato) |
| Validar RFID/Registrar ponto | ✅ | ✅ | ✅ | ✅ | ❌ |
| RBAC/RBAC granular (`user_permissions`) | Editar | – | – | – | – |
| Histórico de nível | Editar | Visualizar | Visualizar | Visualizar | Visualizar (próprio) |
| **Pessoas & RH** | | | | | |
| Cadastro de Pessoas | Editar | Visualizar | Visualizar | Visualizar (próprio) | Visualizar (próprio) |
| Funcionários / níveis | Editar | Visualizar | Visualizar | Visualizar (próprio) | – |
| Registro de Ponto | Editar | Editar (próprio) | Editar (próprio) | Editar (próprio) | – |
| Apontamentos de Horas | Editar | Editar (próprio) / Validar se tutor | Editar (próprio) | Visualizar (próprio) | – |
| Processo Seletivo / Recrutamento | Editar | Visualizar (se tutor) | Visualizar (se tutor) | Visualizar | Visualizar (próprio) |
| Treinamentos (LMS) | Editar | Editar (se tutor) | Visualizar | Visualizar | Visualizar |
| Certificados | Aprovar/Rejeitar | Solicitar/Visualizar (próprio) | Solicitar/Visualizar (próprio) | Solicitar/Visualizar (próprio) | – |
| **Estoque & Suprimentos** | | | | | |
| Itens / Entradas / Saídas | Editar | Editar (se responsável) | Editar (se responsável) | Visualizar | – |
| Fornecedores / Preços | Editar | Visualizar | Visualizar | Visualizar | – |
| Empréstimos (interno/externo) | Editar | Editar (se responsável) | Editar (se responsável) | Visualizar | – |
| BOM / Lista de Materiais | Editar | Editar (se responsável) | Visualizar | Visualizar | – |
| Localizações | Editar | Visualizar | Visualizar | Visualizar | – |
| **Vendas & CRM** | | | | | |
| Clientes / Orçamentos | Editar | Visualizar | Visualizar | Visualizar | – |
| Encomendas / Kanban | Editar | Editar (se atribuído) | Editar (se atribuído) | Visualizar (se atribuído) | – |
| Marketplace (manual) | Editar | Visualizar | Visualizar | Visualizar | – |
| CRM / Interações / Tarefas Mkt | Editar | Editar (se responsável) | Visualizar | Visualizar | – |
| **Financeiro** | | | | | |
| Lançamentos / Contas a Pagar | **Exclusivo Admin** | – | – | – | – |
| Fechamento de Encomenda / Custeio | **Exclusivo Admin** | – | – | – | – |
| Relatórios | **Exclusivo Admin** | – | – | – | – |
| **Produção & Projetos** | | | | | |
| Projetos / Tarefas | Editar | Editar (se atribuído) | Editar (se atribuído) | Visualizar (se atribuído) | – |
| Kanban de Encomendas | Editar | Editar (se atribuído) | Editar (se atribuído) | Visualizar (se atribuído) | – |
| Máquinas / Histórico de Uso | Editar | Visualizar / Registrar uso | Visualizar / Registrar uso | Visualizar | – |
| 5S (Setores/Checklist/Inspeção) | Editar | Editar (se responsável) | Editar (se responsável) | Visualizar | **– (não participa)** |
| 5S (Advertências/Penalidades) | Editar | – | – | – | – |
| Mesas / Auditoria de Projeto-Mesa | Editar | Visualizar (se atribuído) | Visualizar | Visualizar | – |
| **Notification** | | | | | |
| Minhas notificações | ✅ | ✅ | ✅ | ✅ | ✅ |
| Revisar/Histórico (Admin) | Editar | – | – | – | – |
| Configuração de Canal | Editar | – | – | – | – |
| **Dashboard** (aggregator) | | | | | |
| Summary / KPIs | ✅ | ✅ | ✅ | ✅ (restrito) | ❌ |
| Concluir tarefa (tasks) | ✅ | ✅ (se atribuído) | ✅ (se atribuído) | ✅ (se atribuído) | – |

## Regras Obrigatórias
1. Apenas **Admin (0)** altera níveis de acesso / RBAC granular.
2. Apenas **Admin (0)** acessa o módulo Financeiro (completamente isolado).
3. Apenas **Admin (0)** aprova/rejeita certificados, advertências 5S e compras.
4. **Recrutandos (4)** não participam do 5S nem recebem extrato mensal de horas.
5. Penumbras na edição: `editar se responsável/atribuído` exige vínculo em
   `usuario_responsabilidade` (contexto/setor) — sem esse vínculo, o role 1/2 degrada para visualização.
6. Dashboard/KPIs com campos `restricted`: omitidos para role ≠ 0 e ajustados por papel (deltaTone).
7. Princípio do menor privilégio em todas as `Policy`/`Gate` (e middleware) do Laravel.

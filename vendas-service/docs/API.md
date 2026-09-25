# API — vendas-service

> Em construção. Endpoints planejados conforme especificação técnica:

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/clientes` | Cadastra cliente (PF/PJ) |
| GET | `/clientes` | Lista clientes |
| GET | `/clientes/{id}` | Detalhes do cliente |
| POST | `/orcamentos` | Cria orçamento |
| PUT | `/orcamentos/{id}` | Atualiza orçamento (ajustes) |
| POST | `/encomendas` | Cria encomenda a partir de orçamento aprovado |
| PUT | `/encomendas/{id}/kanban` | Move encomenda no Kanban |
| GET | `/encomendas` | Lista encomendas com filtros |
| POST | `/marketplace` | Registra venda manual em marketplace |
| POST | `/interacoes` | Registra interação com cliente |
| GET | `/interacoes/{id_cliente}` | Lista interações |
| POST | `/tarefas-marketing` | Cria tarefa de marketing |
| GET | `/tarefas-marketing` | Lista tarefas |
| PUT | `/tarefas-marketing/{id}` | Atualiza status da tarefa |
| POST | `/tags` | Cadastra tag de cliente |
| GET | `/tags` | Lista tags |
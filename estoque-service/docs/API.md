# API — estoque-service

> Em construção. Endpoints planejados conforme especificação técnica:

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/itens` | Cadastra item |
| GET | `/itens` | Lista itens (filtros por categoria, localização, estoque baixo) |
| GET | `/itens/{id}` | Detalhes de um item |
| PUT | `/itens/{id}` | Atualiza item |
| POST | `/entradas` | Registra entrada de estoque |
| POST | `/saidas` | Registra saída manual |
| POST | `/emprestimos` | Registra empréstimo |
| PUT | `/emprestimos/{id}/devolucao` | Registra devolução |
| GET | `/emprestimos/atrasados` | Lista empréstimos em atraso |
| POST | `/fornecedores` | Cadastra fornecedor |
| GET | `/fornecedores` | Lista fornecedores |
| POST | `/boms` | Cria BOM |
| GET | `/boms/{id}` | Retorna BOM |
| PUT | `/boms/{id}` | Atualiza BOM |
| POST | `/boms/{id}/consumo` | Registra consumo real da BOM |
| GET | `/localizacoes` | Lista localizações físicas |
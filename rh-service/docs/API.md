# API — rh-service

> Em construção. Endpoints planejados conforme especificação técnica:

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/pessoas` | Cadastra nova pessoa |
| GET | `/pessoas/{id}` | Retorna dados da pessoa |
| PUT | `/pessoas/{id}` | Atualiza pessoa |
| GET | `/funcionarios` | Lista funcionários (filtros por nível e departamento) |
| POST | `/funcionarios` | Cadastra funcionário |
| PUT | `/funcionarios/{id}/nivel` | Altera nível de acesso (Admin) |
| GET | `/funcionarios/{id}/horas` | Total de horas (presença, encomenda, projeto) |
| POST | `/apontamentos-horas` | Registra horas em encomenda/projeto |
| PUT | `/apontamentos-horas/{id}/validar` | Valida/rejeita apontamento (Admin) |
| POST | `/processo-seletivo` | Inicia processo seletivo |
| PUT | `/processo-seletivo/{id}` | Atualiza status do processo |
| POST | `/treinamentos` | Cria treinamento (tutor) |
| POST | `/treinamentos/{id}/avaliacoes` | Registra avaliação |
| GET | `/treinamentos/{id}/avaliacoes` | Lista avaliações |
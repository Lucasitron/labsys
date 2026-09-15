# API — producao-service

> Em construção. Endpoints planejados conforme especificação técnica:

## Produção e Projetos
| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/projetos` | Cria projeto |
| GET | `/projetos` | Lista projetos |
| PUT | `/projetos/{id}` | Atualiza projeto |
| POST | `/tarefas` | Cria tarefa |
| PUT | `/tarefas/{id}` | Atualiza tarefa |
| GET | `/tarefas` | Lista tarefas |
| POST | `/kanban` | Insere encomenda no Kanban |
| PUT | `/kanban/{id}/mover` | Move encomenda no Kanban |
| GET | `/kanban` | Lista cartões do Kanban |

## Máquinas
| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/maquinas` | Cadastra máquina |
| GET | `/maquinas` | Lista máquinas |
| PUT | `/maquinas/{id}/status` | Atualiza status |
| POST | `/maquinas/{id}/uso` | Registra início de uso |
| PUT | `/maquinas/{id}/uso/{id_uso}/fim` | Registra fim de uso |
| GET | `/maquinas/{id}/historico` | Lista histórico de uso |

## Sistema 5S
| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/setores` | Cadastra setor |
| GET | `/setores` | Lista setores |
| PUT | `/setores/{id}` | Atualiza setor |
| POST | `/setores/{id}/checklist` | Adiciona item ao checklist |
| PUT | `/setores/{id}/checklist/{id_item}` | Edita item do checklist |
| POST | `/setores/{id}/responsaveis` | Designa responsável |
| GET | `/setores/{id}/responsaveis` | Lista responsáveis |
| POST | `/inspecoes-5s` | Cria inspeção semanal |
| GET | `/inspecoes-5s` | Lista inspeções |
| POST | `/advertencias` | Registra advertência |
| GET | `/advertencias/{id_funcionario}` | Lista advertências de um membro |
| POST | `/projetos-mesa` | Cria projeto em mesa |
| GET | `/projetos-mesa` | Lista projetos em mesas |
| PUT | `/projetos-mesa/{id}/evolucao` | Registra evolução |
| POST | `/auditorias-projeto-mesa` | Realiza auditoria |
| GET | `/parametros-5s` | Lista parâmetros do 5S |
| PUT | `/parametros-5s/{id}` | Atualiza parâmetro |
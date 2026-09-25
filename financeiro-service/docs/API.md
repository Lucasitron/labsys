# API — financeiro-service

> Em construção. Endpoints planejados conforme especificação técnica:

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/lancamentos` | Cria lançamento financeiro |
| GET | `/lancamentos` | Lista lançamentos com filtros |
| PUT | `/lancamentos/{id}/pagamento` | Registra pagamento/recebimento |
| POST | `/categorias` | Cadastra categoria |
| GET | `/categorias` | Lista categorias |
| POST | `/doacoes-recursos` | Registra doação/recurso |
| GET | `/doacoes-recursos` | Lista doações e recursos |
| POST | `/valores-hora` | Define valor/hora por nível (Admin) |
| GET | `/valores-hora` | Lista valores vigentes |
| POST | `/parametros-overhead` | Define taxa de overhead (Admin) |
| POST | `/fechamento-encomenda` | Fecha valor e horas estimadas |
| GET | `/fechamento-encomenda/{id}` | Consulta fechamento |
| POST | `/solicitacoes-compra` | Registra solicitação (informativo) |
| PUT | `/solicitacoes-compra/{id}/concluir` | Conclui compra |
| GET | `/custos-encomenda/{id}` | Custo detalhado da encomenda |
| GET | `/relatorios/fluxo-caixa` | Fluxo de caixa |
| GET | `/relatorios/dre` | DRE simplificado |
| GET | `/relatorios/lucratividade` | Lucratividade por encomenda |
| GET | `/relatorios/inadimplencia` | Contas a receber vencidas |
| GET | `/relatorios/doacoes-despesas` | Doações vs. despesas |
| GET | `/relatorios/custo-maquina` | Custo por máquina |
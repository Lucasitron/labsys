# API — producao-service

Todas as rotas exigem `Authorization: Bearer <JWT>` do Auth &amp; Identity Service, exceto `/actuator/health`.

## RBAC

Os níveis vêm da claim `role` (`NivelAcesso`): `ADMIN` (0), `BOLSISTA` (1), `VOLUNTARIO` (2), `ESTAGIARIO` (3), `RECRUTANDO` (4).

| Operação | `@PreAuthorize` |
| :--- | :--- |
| Leitura | `hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')` |
| Escrita | `hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')` + responsabilidade verificada no serviço |
| Registrar inspeção 5S | `hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO') and @acesso.podeEditar(#request.idInspetor())` |
| Registrar advertência / auditoria de mesa | `hasRole('ADMIN')` |
| Alterar parâmetro 5S | `hasRole('ADMIN')` |

Na escrita, o `AcessoService` (`@acesso`) garante que o usuário só edita recursos
dos quais é responsável: `ADMIN` sempre pode; os demais apenas quando
`idResponsavel` do recurso é igual ao `id_user` do JWT. `RECRUTANDO` não acessa
nenhuma rota (recebe `403`).

## Projetos

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/projetos` | Cria projeto (`PLANEJADO` por padrão) |
| GET | `/projetos` | Lista; filtros opcionais `status` e `idResponsavel` |
| GET | `/projetos/{id}` | Detalha projeto |
| PUT | `/projetos/{id}` | Atualiza projeto |
| PUT | `/projetos/{id}/status` | Altera status (`dataFimReal` gravada em `CONCLUIDO`) |
| DELETE | `/projetos/{id}` | Remove projeto |

`ProjetoStatus`: `PLANEJADO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO`.

```json
{
  "nome": "Braço Robótico",
  "descricao": "Protótipo de manipulador",
  "dataInicio": "2026-09-17",
  "dataFimPrevista": "2026-10-30",
  "idResponsavel": 1
}
```

`PUT /projetos/{id}/status`:

```json
{ "status": "CONCLUIDO", "dataFimReal": "2026-10-25" }
```

## Tarefas

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/tarefas` | Cria tarefa (`PENDENTE` por padrão) |
| GET | `/tarefas` | Lista; filtros opcionais `idProjeto` e `status` |
| GET | `/tarefas/{id}` | Detalha tarefa |
| PUT | `/tarefas/{id}` | Atualiza tarefa |
| PUT | `/tarefas/{id}/status` | Altera status (`dataConclusao` gravada em `CONCLUIDA`) |
| DELETE | `/tarefas/{id}` | Remove tarefa |

`TarefaStatus`: `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `ATRASADA`.
`PrioridadeTarefa`: `BAIXA`, `MEDIA`, `ALTA`.

```json
{
  "idProjeto": 1,
  "titulo": "Montar chassi",
  "descricao": "Fixar servo-motores",
  "idResponsavel": 1,
  "dataInicio": "2026-09-18",
  "dataFimPrevista": "2026-09-25",
  "prioridade": "ALTA"
}
```

## Kanban de encomendas

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/kanban` | Inclui cartão de encomenda (coluna `FILA`) |
| GET | `/kanban` | Lista cartões; filtro opcional `status` |
| GET | `/kanban/encomenda/{idEncomenda}` | Cartão de uma encomenda |
| GET | `/kanban/encomenda/{idEncomenda}/historico` | Histórico de movimentações |
| PUT | `/kanban/{id}/mover` | Move o cartão de coluna |
| DELETE | `/kanban/{id}` | Remove o cartão |

`KanbanStatus`: `FILA` → `PRODUCAO` → `ACABAMENTO` → `PRONTO` → `ENTREGUE`.

Ao mover um cartão (somente o responsável ou `ADMIN`) o serviço:
1. grava o histórico;
2. publica `producao.status.alterado.event` (sincroniza o Vendas);
3. publica `kanban.status.alterado.event` (notificação);
4. ao entrar em `ENTREGUE` (vindo de outra coluna), publica `producao.concluida.event`
   com os itens consumidos acumulados (o Estoque baixa o estoque e o Financeiro calcula o custo).

```json
{ "idEncomenda": 100, "idResponsavel": 1, "ordem": 1 }
```

`PUT /kanban/{id}/mover`:

```json
{ "statusNovo": "ENTREGUE", "idUsuario": 1, "observacao": "finalizado" }
```

## Máquinas

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/maquinas` | Cadastra máquina (`DISPONIVEL` por padrão) |
| GET | `/maquinas` | Lista; filtro opcional `status` |
| GET | `/maquinas/{id}` | Detalha máquina |
| PUT | `/maquinas/{id}` | Atualiza máquina (status preservado se omitido) |
| PUT | `/maquinas/{id}/status` | Altera status |
| POST | `/maquinas/{id}/uso` | Inicia uso (máquina vai para `EM_USO`) |
| PUT | `/maquinas/{id}/uso/{idUso}/fim` | Encerra uso (calcula `horasUso`, volta para `DISPONIVEL`) |
| GET | `/maquinas/{id}/historico` | Histórico de uso |
| DELETE | `/maquinas/{id}` | Remove máquina |

`MaquinaStatus`: `DISPONIVEL`, `EM_USO`, `MANUTENCAO`.

```json
{ "nome": "Impressora 3D", "descricao": "FDM 220x220", "localizacao": "Sala 2" }
```

`POST /maquinas/{id}/uso`:

```json
{ "idFuncionario": 7, "dataInicio": "2026-09-17T09:00:00", "observacao": "impressão do protótipo" }
```

`PUT /maquinas/{id}/uso/{idUso}/fim`:

```json
{ "dataFim": "2026-09-17T15:30:00", "observacao": "concluído" }
```

## Setores 5S

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/setores` | Cria setor |
| GET | `/setores` | Lista; filtro opcional `ativo` |
| GET | `/setores/{id}` | Detalha setor (materiais, sinalizações, checklist e responsáveis) |
| PUT | `/setores/{id}` | Atualiza setor |
| DELETE | `/setores/{id}` | Remove setor |
| POST | `/setores/{id}/materiais` | Adiciona material |
| DELETE | `/setores/{id}/materiais/{idMaterial}` | Remove material |
| POST | `/setores/{id}/sinalizacoes` | Adiciona sinalização |
| DELETE | `/setores/{id}/sinalizacoes/{idSinalizacao}` | Remove sinalização |
| POST | `/setores/{id}/checklist` | Adiciona item de checklist |
| PUT | `/setores/{id}/checklist/{idItem}` | Atualiza item de checklist |
| DELETE | `/setores/{id}/checklist/{idItem}` | Remove item de checklist |
| POST | `/setores/{id}/responsaveis` | Adiciona responsável |
| GET | `/setores/{id}/responsaveis` | Lista responsáveis |
| DELETE | `/setores/{id}/responsaveis/{idResponsavel}` | Remove responsável |

```json
{ "numero": 1, "nome": "Marcenaria", "descricao": "Bancadas e serras", "ativo": true }
```

`POST /setores/{id}/materiais`: `{ "descricao": "Luvas", "quantidade": 10 }`
`POST /setores/{id}/sinalizacoes`: `{ "texto": "Usar EPI" }`
`POST /setores/{id}/checklist`: `{ "item": "Bancada limpa", "ativo": true }`
`POST /setores/{id}/responsaveis`:

```json
{ "idFuncionario": 9, "dataInicio": "2026-09-17", "dataFim": null, "ativo": true }
```

## Inspeções 5S

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/inspecoes-5s` | Registra inspeção (avalia os itens do checklist) |
| GET | `/inspecoes-5s` | Lista; filtro opcional `idSetor` |
| GET | `/inspecoes-5s/{id}` | Detalha inspeção |
| DELETE | `/inspecoes-5s/{id}` | Remove inspeção (somente `ADMIN`) |

Se qualquer item estiver não conforme, a inspeção fica `NAO_CONFORME` e o
serviço aplica automaticamente uma advertência ao responsável ativo do setor.

```json
{
  "idSetor": 1,
  "idInspetor": 1,
  "dataInspecao": "2026-09-17",
  "turno": "MANHA",
  "observacoes": "Bancada com resíduos",
  "itens": [
    { "idChecklist": 1, "conforme": false, "observacao": "sujo" }
  ]
}
```

`TurnoInspecao`: `MANHA`, `TARDE`. `StatusInspecao`: `OK`, `NAO_CONFORME`.

## Advertências

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/advertencias` | Registra advertência (somente `ADMIN`) |
| GET | `/advertencias` | Lista todas |
| GET | `/advertencias/{idFuncionario}` | Advertências de um membro |

Cada registro incrementa o `contador` do membro. Ao atingir
`LIMITE_PENALIDADE` (3), além de `advertencia.registrada.event` é publicado
`advertencia.limite.atingido.event` para o RH/Notificação.

```json
{ "idFuncionario": 9, "idInspecao": 1, "data": "2026-09-17", "motivo": "Não conformidade 5S", "tipo": "FORMAL" }
```

`TipoAdvertencia`: `VERBAL`, `FORMAL`.

## Projetos de mesa

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/projetos-mesa` | Registra projeto de mesa (`ATIVO`) e gera `qrCodeTotem` |
| GET | `/projetos-mesa` | Lista; filtro opcional `status` |
| GET | `/projetos-mesa/{id}` | Detalha projeto de mesa |
| PUT | `/projetos-mesa/{id}` | Atualiza projeto de mesa |
| PUT | `/projetos-mesa/{id}/evolucao` | Marca evolução (reativa se abandonado) |
| GET | `/projetos-mesa/{id}/qrcode` | Retorna o QR Code do totem (`image/png`) |
| GET | `/projetos-mesa/{id}/auditorias` | Lista auditorias do projeto |
| DELETE | `/projetos-mesa/{id}` | Remove projeto de mesa |

`StatusProjetoMesa`: `ATIVO`, `ABANDONADO`, `CONCLUIDO`. O conteúdo codificado
no QR Code é `fablab://projeto-mesa/{idProjetoMesa}`.

```json
{ "idFuncionario": 5, "idMesa": 1, "nomeProjeto": "Drone", "tipoProjeto": "Aeroespacial", "prazoExecucao": "2026-11-30" }
```

## Auditoria de projetos de mesa

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/auditorias-projeto-mesa` | Registra auditoria (somente `ADMIN`) |
| GET | `/auditorias-projeto-mesa/{idProjetoMesa}` | Lista auditorias do projeto |

Ao auditar, o status do projeto é atualizado para o `resultado`. Se o resultado
for `ABANDONADO`, publica `projeto.mesa.abandonado.event`.

```json
{ "idProjetoMesa": 1, "resultado": "ABANDONADO", "acaoTomada": "Recolher material para o estoque" }
```

## Parâmetros 5S

| Método | Path | Descrição |
| :--- | :--- | :--- |
| GET | `/parametros-5s` | Lista parâmetros |
| GET | `/parametros-5s/{id}` | Detalha parâmetro |
| PUT | `/parametros-5s/{id}` | Atualiza parâmetro (somente `ADMIN`) |

Parâmetros semeados: `diasParaAuditoriaProjeto` (padrão `15`),
`diaSemanaInspecao`, `periodoExperimentalAtivo`, `rotacaoDias`.

```json
{ "valor": "30", "descricao": "Dias sem evolução para alerta de auditoria" }
```

## Job de auditoria de mesas

`ProjetoMesaAuditoriaScheduler` roda diariamente (`producao.auditoria.scheduler.cron`,
padrão `0 0 6 * * *`, desligável com `producao.auditoria.scheduler.enabled=false`),
lê o parâmetro `diasParaAuditoriaProjeto` e registra em log os projetos de mesa
`ATIVO` sem evolução há mais que o limite.

## Mensageria

**Consome**

| Exchange | Routing key | Fila | Evento |
| :--- | :--- | :--- | :--- |
| `fablab.vendas` | `encomenda.criada.event` | `producao.kanban.encomenda` | `EncomendaCriadaEvent(idEncomenda, idCliente, statusKanban, valorFinal, dataCriacao)` |
| `fablab.estoque` | `estoque.consumo.realizado.event` | `producao.estoque.consumo` | `EstoqueConsumoEvent(idEncomenda, idItem, quantidadeConsumida)` |
| `fablab.rh` | `nivel.alterado.event` | `producao.rh.nivel` | `NivelAlteradoEvent(idFuncionario, nivelAntigo, nivelNovo, data)` |

`encomenda.criada.event` cria o cartão na coluna `FILA`. `estoque.consumo.realizado.event`
acumula o consumo real por encomenda (BOM efetivo), usado em
`producao.concluida.event`. `nivel.alterado.event` é registrado para
rastreamento.

**Publica**

| Exchange | Routing key | Payload |
| :--- | :--- | :--- |
| `fablab.producao` | `producao.status.alterado.event` | `ProducaoStatusEvent(idEncomenda, statusNovo, idUsuario, observacao)` |
| `fablab.producao` | `producao.concluida.event` | `ProducaoConcluidaEvent(idEncomenda, idProdutoServico, itens[], dataConclusao)` |
| `fablab.notificacao` | `kanban.status.alterado.event` | `KanbanStatusAlteradoEvent(idEncomenda, statusAnterior, statusNovo, dataAlteracao)` |
| `fablab.notificacao` | `advertencia.registrada.event` | `AdvertenciaRegistradaEvent(idFuncionario, contador, motivo)` |
| `fablab.notificacao` | `advertencia.limite.atingido.event` | `AdvertenciaLimiteEvent(idFuncionario, contador, motivo)` |
| `fablab.notificacao` | `projeto.mesa.abandonado.event` | `ProjetoMesaAbandonadoEvent(idProjetoMesa, idFuncionario, acaoTomada)` |

`ItemConsumido(idItem, quantidadeConsumida)` compõe `itens[]`.

## Erros

Formato padronizado `ErrorResponse`: `401` sem/inválido JWT, `403` nível
insuficiente ou fora da responsabilidade, `404` recurso inexistente, `400`
validação de payload, `409` conflito (ex.: encomenda já no Kanban).

## Premissas de integração

*   O Produção não referencia `idSetor`/`idFuncionario`/`idMesa` como FKs
    físicas; são identificadores de outros serviços.
*   `kanban.status.alterado.event` (exchange `fablab.notificacao`) é publicado
    em paralelo a `producao.status.alterado.event` para atender tanto ao
    contrato real do Vendas quanto ao payload descrito no prompt do módulo.
*   `producao.concluida.event` mantém o contrato consumido por Estoque e
    Financeiro (lista de `itens`) e adiciona `dataConclusao` (superset do
    payload do prompt).

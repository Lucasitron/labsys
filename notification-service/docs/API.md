# API — notification-service

Todas as rotas exigem `Authorization: Bearer <JWT>` do Auth &amp; Identity Service, exceto `/actuator/health`.

## RBAC

Os níveis vêm da claim `role` (`NivelAcesso`): `ADMIN` (0), `BOLSISTA` (1), `VOLUNTARIO` (2), `ESTAGIARIO` (3), `RECRUTANDO` (4).

| Operação | Regra |
| :--- | :--- |
| Listar/marcar as próprias notificações | Qualquer usuário autenticado (somente as suas) |
| Listar todas, revisar, histórico, teste e configurações de canal | `hasRole('ADMIN')` |

No nível do serviço, o `AcessoService` (`@acesso`) garante que o usuário só
marca como lida uma notificação da qual é destinatário: `ADMIN` sempre pode; os
demais apenas quando `idDestinatario` é igual ao `id_user` do JWT (caso
contrário recebem `403`).

## Notificações

| Método | Path | Descrição |
| :--- | :--- | :--- |
| GET | `/notificacoes` | Lista as notificações do usuário autenticado; filtros opcionais `status` e `tipoEvento` |
| PUT | `/notificacoes/{id}/ler` | Marca uma notificação do próprio usuário como lida |
| GET | `/notificacoes/admin` | Lista todas as notificações ativas (Admin); filtros opcionais `status` e `tipoEvento` |
| POST | `/notificacoes/admin/revisar` | Move as notificações selecionadas para o histórico (Admin) |
| GET | `/notificacoes/historico` | Lista o histórico (Admin); filtros opcionais `canal` e `tipoEvento` |
| POST | `/notificacoes/teste` | Envia uma notificação de teste (Admin) |

`CanalNotificacao`: `EMAIL`, `WHATSAPP`.
`StatusNotificacao`: `PENDENTE`, `ENVIADA`, `LIDA`.
`TipoEvento`: `ESTOQUE_BAIXO`, `EMPRESTIMO_ATRASADO`, `ENCOMENDA_CRIADA`,
`ENCOMENDA_STATUS_ALTERADO`, `ORCAMENTO_APROVADO`, `LANCAMENTO_VENCIDO`,
`ADVERTENCIA_REGISTRADA`, `PROJETO_MESA_ABANDONADO`, `NIVEL_ALTERADO`,
`HORAS_VALIDADAS`, `COMPRA_SOLICITADA`, `CERTIFICADO_SOLICITADO`,
`CERTIFICADO_APROVADO`, `CERTIFICADO_REJEITADO`, `EXTRATO_MENSAL_HORAS`,
`TESTE`.

Resposta de uma notificação ativa:

```json
{
  "idNotificacao": 1,
  "idDestinatario": 1,
  "canal": "EMAIL",
  "tipoEvento": "NIVEL_ALTERADO",
  "assunto": "Seu nível de acesso mudou",
  "mensagem": "Seu nível de acesso foi alterado de BOLSISTA para ADMIN.",
  "idReferencia": 1,
  "status": "ENVIADA",
  "dataCriacao": "2026-09-17T10:00:00",
  "dataEnvio": "2026-09-17T10:00:00",
  "dataLeitura": null
}
```

`POST /notificacoes/admin/revisar`:

```json
{ "idsNotificacoes": [1, 2, 3] }
```

Resposta: `{ "revisadas": 3 }`. Cada item é copiado para o histórico com
`dataRevisaoAdmin` e `idAdminRevisor`, e então removido da tabela ativa.

`POST /notificacoes/teste`:

```json
{ "idDestinatario": 1, "canal": "EMAIL", "assunto": "Oi", "mensagem": "Tudo bem?" }
```

`idDestinatario` e `canal` (padrão `EMAIL`) são opcionais; sem `idDestinatario`
usa o usuário autenticado. Resposta `201` com a notificação criada.

## Configurações de canal

| Método | Path | Descrição |
| :--- | :--- | :--- |
| GET | `/configuracoes-canal` | Lista as configurações (Admin) |
| GET | `/configuracoes-canal/{id}` | Detalha uma configuração (Admin) |
| PUT | `/configuracoes-canal/{id}` | Atualiza `habilitado` e `parametros` (Admin) |

```json
{ "habilitado": true, "parametros": "{}" }
```

O canal `EMAIL` vem habilitado pelo seed; `WHATSAPP` inicia desabilitado.

## Eventos consumidos

Uma fila por evento é declarada e ligada à exchange de origem. Todo evento vira
uma notificação `PENDENTE` que é enviada imediatamente; em caso de falha (ou canal
desabilitado) permanece `PENDENTE`.

| Exchange | Routing key | Fila | Tipo gerado | Destinatário |
| :--- | :--- | :--- | :--- | :--- |
| `fablab.estoque` | `estoque.baixo.event` | `notificacao.estoque.baixo` | `ESTOQUE_BAIXO` | Admin (nulo) |
| `fablab.estoque` | `emprestimo.atrasado.event` | `notificacao.emprestimo.atrasado` | `EMPRESTIMO_ATRASADO` | `idPessoa` |
| `fablab.vendas` | `encomenda.criada.event` | `notificacao.encomenda.criada` | `ENCOMENDA_CRIADA` | `idCliente` |
| `fablab.vendas` | `encomenda.status.alterado.event` | `notificacao.encomenda.status` | `ENCOMENDA_STATUS_ALTERADO` | `idCliente` |
| `fablab.vendas` | `orcamento.aprovado.event` | `notificacao.orcamento.aprovado` | `ORCAMENTO_APROVADO` | `idCliente` |
| `fablab.financeiro` | `lancamento.vencido.event` | `notificacao.lancamento.vencido` | `LANCAMENTO_VENCIDO` | Admin (nulo) |
| `fablab.financeiro` | `compra.solicitada.event` | `notificacao.compra.solicitada` | `COMPRA_SOLICITADA` | Admin (nulo) |
| `fablab.notificacao` | `advertencia.registrada.event` | `notificacao.advertencia` | `ADVERTENCIA_REGISTRADA` | `idFuncionario` |
| `fablab.notificacao` | `projeto.mesa.abandonado.event` | `notificacao.projeto.mesa` | `PROJETO_MESA_ABANDONADO` | `idFuncionario` |
| `fablab.notificacao` | `kanban.status.alterado.event` | `notificacao.kanban.status` | `ENCOMENDA_STATUS_ALTERADO` | Admin (nulo) |
| `fablab.rh` | `nivel.alterado.event` | `notificacao.nivel.alterado` | `NIVEL_ALTERADO` | `idFuncionario` |
| `fablab.rh` | `horas.validadas.event` | `notificacao.horas.validadas` | `HORAS_VALIDADAS` | `idFuncionario` |
| `fablab.rh` | `certificado.solicitado.event` | `notificacao.certificado.solicitado` | `CERTIFICADO_SOLICITADO` | Admin (nulo) |
| `fablab.rh` | `certificado.aprovado.event` | `notificacao.certificado.aprovado` | `CERTIFICADO_APROVADO` | `idFuncionario` |
| `fablab.rh` | `certificado.rejeitado.event` | `notificacao.certificado.rejeitado` | `CERTIFICADO_REJEITADO` | `idFuncionario` |
| `fablab.rh` | `extrato.mensal.horas.event` | `notificacao.extrato.mensal.horas` | `EXTRATO_MENSAL_HORAS` | `idFuncionario` |

> O evento `access.rfid.event` (Auth Service) não é consumido no MVP.

O corpo do e-mail do `EXTRATO_MENSAL_HORAS` é gerado pelo
`NotificacaoEventListener.formatarExtratoMensal`:

```
Olá {nome},
Segue o extrato de horas do mês {mesReferencia}:
- Presença: {horasPresenca}h
- Encomendas: {horasEncomenda}h
- Projetos: {horasProjeto}h
- Total disponível para certificado: {horasDisponiveis}h
```

## Retenção e agendadores

*   `ExpurgoHistoricoScheduler` — diário (`0 0 3 * * *`), remove do histórico os
    registros revisados há mais que `notification.historico.retencao-dias`
    (padrão 90).
*   `ReenvioPendentesScheduler` — desabilitado por padrão; quando habilitado
    (`notification.reenvio.scheduler.enabled=true`), reprocessa as notificações
    `PENDENTE` a cada 30 minutos.

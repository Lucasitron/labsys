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

## Certificados de horas (v0.2.0)

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/certificados/solicitar` | Funcionário solicita certificado de horas |
| GET | `/certificados/solicitacoes` | Lista solicitações (Admin vê todas; funcionário vê as próprias) |
| PUT | `/certificados/solicitacoes/{id}/aprovar` | Admin aprova e emite o certificado, consolidando as horas |
| PUT | `/certificados/solicitacoes/{id}/rejeitar` | Admin rejeita a solicitação |
| GET | `/certificados/emitidos` | Lista certificados emitidos (Admin vê todos) |
| GET | `/certificados/emitidos/{id}` | Detalha certificado com as horas consolidadas |
| GET | `/horas/disponiveis` | Total de horas disponíveis (não consolidadas) do usuário autenticado |

### Exemplos

**POST `/certificados/solicitar`**

```json
{
  "tipoCertificado": "COMPLEMENTAR",
  "horasSolicitadas": 10.00
}
```

**PUT `/certificados/solicitacoes/{id}/aprovar`**

```json
{
  "observacao": "Horas conferidas pelo Admin"
}
```

**PUT `/certificados/solicitacoes/{id}/rejeitar`**

```json
{
  "observacao": "Dados insuficientes"
}
```

**GET `/certificados/emitidos/{id}`**

```json
{
  "idCertificado": 1,
  "idSolicitacao": 1,
  "idFuncionario": 2,
  "nomeFuncionario": "Maria",
  "tipoCertificado": "COMPLEMENTAR",
  "horasCertificadas": 10.00,
  "dataEmissao": "2026-09-17T10:00:00",
  "codigoVerificacao": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "horas": [
    {
      "idConsolidacao": 1,
      "idApontamento": 12,
      "dataApontamento": "2026-09-10",
      "tipoApontamento": "ENCOMENDA",
      "horas": 6.00,
      "dataConsolidacao": "2026-09-17T10:00:00"
    }
  ]
}
```

### Eventos publicados (RabbitMQ — exchange `fablab.rh`)

| Routing key | Payload |
| :--- | :--- |
| `certificado.solicitado.event` | solicitação de certificado (avisa o Admin) |
| `certificado.aprovado.event` | certificado aprovado (avisa o funcionário) |
| `certificado.rejeitado.event` | solicitação rejeitada (avisa o funcionário) |
| `extrato.mensal.horas.event` | extrato mensal de horas (job do 1º dia de cada mês às 06:00) |
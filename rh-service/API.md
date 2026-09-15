# API Reference — Pessoas & RH Service

Base URL: `http://localhost:8082`

Todas as requisições exigem header `Authorization: Bearer <JWT>` (exceto `GET /actuator/health`).

---

## Pessoas

### POST /pessoas
Cadastra uma nova pessoa (professor, aluno, cliente).

**Roles permitidas:** `ADMIN`, `BOLSISTA`, `VOLUNTARIO`

```json
Request:
{
  "nomeCompleto": "Maria Silva",
  "matricula": "MAT-001",
  "dataAdmissao": "2026-03-01",
  "contato": "maria@fablab.com",
  "turno": "Manhã",
  "status": "ATIVO"
}

Response 201:
{
  "id": 1,
  "nomeCompleto": "Maria Silva",
  "matricula": "MAT-001",
  "dataAdmissao": "2026-03-01",
  "contato": "maria@fablab.com",
  "turno": "Manhã",
  "status": "ATIVO"
}
```

### GET /pessoas/{id}
Retorna os dados de uma pessoa. Não-admins só acessam seus próprios dados.

### PUT /pessoas/{id}
Atualiza os dados de uma pessoa. Não-admins só atualizam seus próprios dados.

---

## Funcionários

### GET /funcionarios
Lista todos os funcionários com filtros opcionais.

**Roles permitidas:** `ADMIN`

**Query params:** `nivel` (ADMIN/BOLSISTA/VOLUNTARIO/ESTAGIARIO/RECRUTANDO), `departamento`

### POST /funcionarios
Vincula uma pessoa como funcionário.

**Roles permitidas:** `ADMIN`

```json
Request:
{
  "idPessoa": 1,
  "nivelAcesso": "BOLSISTA",
  "departamento": "Eletrônica"
}
```

### PUT /funcionarios/{id}/nivel
Altera o nível de acesso de um funcionário. Apenas Admin.

**Roles permitidas:** `ADMIN`

```json
Request:
{
  "nivelNovo": "ESTAGIARIO"
}
```

Publica evento `nivel.alterado.event` no RabbitMQ e registra em `historico_nivel`.

### GET /funcionarios/{id}/horas
Retorna totais de horas (presença, encomenda, projeto) e incoerências diárias. Não-admins só consultam seus próprios dados.

---

## Apontamento de Horas

### POST /apontamentos-horas
Registra horas em uma encomenda ou projeto.

**Roles permitidas:** `ADMIN`, `BOLSISTA`, `VOLUNTARIO` (não-admins só para si)

```json
Request:
{
  "idFuncionario": 1,
  "tipo": "PROJETO",
  "idReferencia": 5,
  "data": "2026-03-15",
  "horasTrabalhadas": 3.50,
  "descricaoAtividade": "Montagem do protótipo"
}
```

### PUT /apontamentos-horas/{id}/validar
Valida ou rejeita um apontamento. Apenas Admin.

**Roles permitidas:** `ADMIN`

```json
Request:
{
  "status": "VALIDADO"
}
```

Publica evento `horas.validadas.event` quando VALIDADO. Verifica coerência: encomenda + projeto ≤ presença no mesmo dia.

---

## Processo Seletivo

### POST /processo-seletivo
Inicia um novo processo seletivo. Cria pessoa (status RECRUTANDO) e funcionário (nível RECRUTANDO). Admin ou tutor.

**Roles permitidas:** `ADMIN`, `BOLSISTA`, `VOLUNTARIO` (tutor validado no service)

```json
Request:
{
  "nomeCompleto": "Candidato João",
  "matricula": "MAT-CAND-001",
  "contato": "joao@email.com",
  "turno": "Noite",
  "idTutor": 3
}
```

### PUT /processo-seletivo/{id}
Atualiza o status do processo seletivo. Admin ou tutor.

```json
Request:
{
  "statusProcesso": "APROVADO",
  "resultadoFinal": "Aprovado na entrevista técnica"
}
```

---

## Treinamento (LMS)

### POST /treinamentos
Cria um novo treinamento. Tutor cria com seu próprio ID; Admin pode indicar qualquer tutor.

**Roles permitidas:** `ADMIN`, `BOLSISTA`, `VOLUNTARIO`

```json
Request:
{
  "titulo": "Segurança em Impressoras 3D",
  "descricao": "Guia básico de segurança",
  "urlConteudo": "https://docs.fablab.com/3d-safety",
  "idTutor": 3
}
```

### POST /treinamentos/{id}/avaliacoes
Registra avaliação de um aluno. Nota 0-10.

```json
Request:
{
  "idFuncionario": 5,
  "nota": 8.50,
  "feedback": "Ótimo desempenho na prática",
  "dataAvaliacao": "2026-03-20"
}
```

### GET /treinamentos/{id}/avaliacoes
Lista todas as avaliações de um treinamento.

---

## Endpoints Públicos

### GET /actuator/health
Health check (não requer autenticação).

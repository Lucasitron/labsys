# API - Auth & Identity Service

Endpoint público: `http://localhost:8081` (padrão, ver `PORT`).

Formato dos erros (padrão):

```json
{
  "timestamp": "2026-09-15T01:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciais inválidas",
  "path": "/auth/login",
  "fieldErrors": {}
}
```

### Códigos de erro comuns

| HTTP | Significado |
| :--- | :--- |
| `400` | Requisição inválida (Bean Validation), corpo ilegível ou papel inválido |
| `401` | Credenciais/token inválidos, expirados ou revogados |
| `403` | Usuário autenticado sem permissão (RBAC) |
| `404` | Recurso não encontrado |
| `500` | Erro interno |

---

## POST /auth/login

Autentica por **e-mail** ou **nome de usuário** e senha. Retorna o par de tokens.

**Requisição**

```json
{
  "email": "admin@fablab.io",
  "nomeUsuario": null,
  "senha": "Senha@123"
}
```

> Informe `email` *ou* `nomeUsuario`. `senha` é obrigatória (6-72 caracteres).

**Resposta `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "idUser": 7,
  "role": "ADMIN",
  "setor": "Direção",
  "nomeUsuario": "admin"
}
```

**Erros:** `400` (validação), `401` (credenciais inválidas), `403` (sem permissão ativa).

---

## POST /auth/logout  *(autenticado)*

Invalida o token JWT atual (adiciona à blacklist até a expiração).

**Requisição** (token no cabeçalho `Authorization: Bearer …`; pode ser enviado no corpo)

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Resposta `200 OK`**

```json
{
  "message": "Logout realizado com sucesso"
}
```

**Erros:** `401` (token ausente, inválido ou já revogado).

---

## POST /auth/validate-rfid  *(público)*

Valida o UUID de um cartão RFID lido pelo ESP32. Registra o acesso em `access_log`
e publica o evento `access.rfid.event` no RabbitMQ quando o cartão é reconhecido.

**Requisição**

```json
{
  "uuid": "CARD-ADMIN"
}
```

**Resposta `200 OK`** (cartão reconhecido — tipo alterna ENTRADA/SAIDA a cada leitura)

```json
{
  "allowed": true,
  "message": "Acesso registrado",
  "idUser": 7,
  "uuidRfid": "CARD-ADMIN",
  "nomeUsuario": "admin",
  "setor": "Direção",
  "type": "ENTRADA",
  "timestamp": "2026-09-15T09:30:00Z"
}
```

**Resposta `200 OK`** (cartão desconhecido — tipo `ACESSO_NEGADO`, sem evento)

```json
{
  "allowed": false,
  "message": "Cartão RFID não reconhecido",
  "idUser": null,
  "uuidRfid": "CARD-XYZ",
  "nomeUsuario": null,
  "setor": null,
  "type": "ACESSO_NEGADO",
  "timestamp": "2026-09-15T09:31:00Z"
}
```

**Erros:** `400` (`uuid` obrigatório).

---

## GET /auth/me  *(autenticado)*

Retorna os dados do usuário autenticado e suas permissões.

Cabeçalho: `Authorization: Bearer <accessToken>`

**Resposta `200 OK`**

```json
{
  "id": 1,
  "idUser": 7,
  "email": "admin@fablab.io",
  "nomeUsuario": "admin",
  "setor": "Direção",
  "permissions": [
    { "role": "ADMIN", "label": "Admin", "active": true }
  ]
}
```

**Erros:** `401` (sem token/token inválido), `404` (usuário não encontrado).

---

## POST /auth/refresh  *(público)*

Renova o par de tokens usando o refresh token. O refresh token antigo é rotacionado
(adicionado à blacklist).

**Requisição**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Resposta `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Erros:** `400` (validação), `401` (token de acesso não é aceito, token revogado ou usuário inexistente).

---

## GET /auth/permissions?role=ADMIN  *(somente Admin)*

Retorna a matriz de permissões RBAC de um papel. `role` é opcional (padrão `ADMIN`).
Valores: `ADMIN`, `BOLSISTA`, `VOLUNTARIO`, `ESTAGIARIO`, `RECRUTANDO`.

**Resposta `200 OK`**

```json
{
  "role": "ADMIN",
  "code": 0,
  "label": "Admin",
  "permissions": ["*"]
}
```

**Erros:** `401` (sem token), `403` (papel não-Admin), `400` (papel inválido).

---

## Evento RabbitMQ

| Exchange | Routing key | Payload |
| :--- | :--- | :--- |
| `fablab.access` | `access.rfid.event` | `{ idUser, uuidRfid, timestamp, type }` |

Exemplo:

```json
{
  "idUser": 7,
  "uuidRfid": "CARD-ADMIN",
  "timestamp": "2026-09-15T09:30:00Z",
  "type": "ENTRADA"
}
```

Publicado apenas para cartões reconhecidos (tipos `ENTRADA`/`SAIDA`). Consumido pelo
Pessoas & RH Service para registro de ponto.

## Health check

`GET /actuator/health` — público.
# API — auth-service

> Em construção. Endpoints planejados conforme especificação técnica:

| Método | Path | Descrição |
| :--- | :--- | :--- |
| POST | `/auth/login` | Autentica via e-mail/senha e retorna JWT |
| POST | `/auth/logout` | Invalida o token (blacklist) |
| POST | `/auth/validate-rfid` | Valida cartão RFID (ESP32) e registra ponto |
| GET | `/auth/me` | Dados do usuário autenticado e permissões |
| POST | `/auth/refresh` | Renova token expirado |
| GET | `/auth/permissions` | Matriz de permissões por nível (consumido pelos serviços) |
# API — notification-service

> Em construção. Endpoints planejados conforme especificação técnica:

| Método | Path | Descrição |
| :--- | :--- | :--- |
| GET | `/notificacoes` | Lista notificações do usuário autenticado |
| PUT | `/notificacoes/{id}/ler` | Marca notificação como lida |
| GET | `/notificacoes/admin` | Lista todas as notificações (Admin) |
| POST | `/notificacoes/admin/revisar` | Move notificações para o histórico (Admin) |
| GET | `/notificacoes/historico` | Lista histórico (Admin) |
| GET | `/configuracoes-canal` | Lista configurações de canais (Admin) |
| PUT | `/configuracoes-canal/{id}` | Atualiza configuração de canal (Admin) |
| POST | `/notificacoes/teste` | Envia notificação de teste (Admin) |
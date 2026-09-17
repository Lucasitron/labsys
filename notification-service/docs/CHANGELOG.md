<!-- Keep a Changelog: https://keepachangelog.com/pt-BR/1.1.0/ -->

## [Não publicado]

### Adicionado
- Scaffold inicial do microsserviço.
- Segurança JWT (HMAC jjwt 0.12.6) com RBAC via `@PreAuthorize` e
  `AcessoService` para autorização no nível do recurso.
- DTOs de evento de todos os serviços (Vendas, Estoque, Financeiro, Produção e RH).
- Entidades `Notificacao`, `NotificacaoHistorico` e `ConfiguracaoCanal`, seus
  repositórios e migrations Flyway (`V1`–`V4`, com seed de canais).
- Serviços `NotificacaoService`, `EmailService`, `WhatsAppService`/`WhatsAppServiceNoop`
  e `ConfiguracaoCanalService`.
- Listener RabbitMQ com uma fila por evento (`notificacao.*`) e exchange
  `fablab.notificacao`.
- Endpoints de notificações (ativas, leitura, revisão, histórico e teste) e de
  configurações de canal.
- `ExpurgoHistoricoScheduler` (retenção de 90 dias) e `ReenvioPendentesScheduler`
  (opcional, desabilitado por padrão).
- Testes unitários, de integração e de segurança (64 testes, cobertura JaCoCo
  LINE ≥ 80%).
- Documentação (`README`, `API`, `CHANGELOG`).

### Notas
- WhatsApp é estrutural (no-op) no MVP; notificações permanecem `PENDENTE`.
- E-mail utiliza remetente/destinatário padrão configurados.
- O evento `access.rfid.event` não é consumido nesta versão.

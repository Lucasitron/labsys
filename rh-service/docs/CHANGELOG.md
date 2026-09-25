<!-- Keep a Changelog: https://keepachangelog.com/pt-BR/1.1.0/ -->

## [0.2.0] - 2026-09-17

### Adicionado
- Entidades e migrations para **certificados de horas**: `SolicitacaoCertificado`,
  `CertificadoEmitido` e `HoraConsolidada`, além do campo `consolidado` em
  `ApontamentoHoras` (Flyway `V10__certificado_horas.sql`).
- Endpoints de certificado:
  - `POST /certificados/solicitar` — solicita certificado a partir das horas disponíveis.
  - `GET /certificados/solicitacoes` — lista solicitações (Admin vê todas).
  - `PUT /certificados/solicitacoes/{id}/aprovar` — aprova e emite o certificado,
    consolidando os apontamentos disponíveis e gerando `HoraConsolidada`.
  - `PUT /certificados/solicitacoes/{id}/rejeitar` — rejeita a solicitação.
  - `GET /certificados/emitidos` e `GET /certificados/emitidos/{id}` — consulta de
    certificados emitidos.
  - `GET /horas/disponiveis` — total de horas não consolidadas do usuário.
- Serviços `CertificadoService` e `ExtratoService`.
- Job agendado mensal (`ExtratoMensalScheduler`) no 1º dia de cada mês às 06:00 que
  compila presença, encomendas, projetos e horas disponíveis de cada funcionário ativo
  (excluindo Recrutandos) e publica `extrato.mensal.horas.event`.
- Eventos publicados no exchange `fablab.rh`:
  `certificado.solicitado.event`, `certificado.aprovado.event`,
  `certificado.rejeitado.event` e `extrato.mensal.horas.event`.
- `codigoVerificacao` UUID em cada certificado emitido.
- Sincronização com bloqueio pessimista (`findByIdForUpdate`) para impedir a decisão
  dupla da mesma solicitação.
- Testes unitários (`CertificadoServiceTest`, `ExtratoServiceTest`), de integração
  (`CertificadoIntegrationTest`) cobrindo endpoints, segurança e mensageria.

### Corrigido
- Nenhum.

## [Não publicado]

### Adicionado
- Scaffold inicial do microsserviço.
- Dependências: JPA, Flyway, AMQP e Eureka client.
<!-- Keep a Changelog: https://keepachangelog.com/pt-BR/1.1.0/ -->

## [Não publicado]

### Adicionado
- Scaffold inicial do microsserviço.
- Dependências: JPA, Flyway, AMQP, Eureka client e ZXing (QR Code).
- Segurança JWT (validação HMAC + issuer do Auth Service) e RBAC via
  `@PreAuthorize`, com verificação de responsabilidade no serviço
  (`AcessoService`).
- Tratamento de erros padronizado (`ErrorResponse` + `GlobalExceptionHandler`).
- Entidades JPA: `Projeto`, `Tarefa`, `EncomendaKanban`, `HistoricoKanban`,
  `Maquina`, `HistoricoUsoMaquina`, `Setor`, `SetorMaterial`, `SetorSinalizacao`,
  `SetorChecklist`, `SetorResponsavel`, `Inspecao5S`, `ItemInspecao5S`,
  `AdvertenciaMembro`, `ProjetoMesa`, `AuditoriaProjetoMesa`, `Parametro5S` e
  `ConsumoEncomenda`, além dos enums de domínio.
- Migrations Flyway `V1`–`V18` (todas as tabelas do Produção e 5S) e seed de
  `parametro_5s`.
- Repositórios, DTOs de request/response e eventos.
- Services: projetos, tarefas, Kanban, máquinas, setores 5S, inspeções 5S,
  advertências, projetos de mesa, parâmetros 5S, geração de QR Code e publicação
  de eventos.
- Controllers REST de todos os endpoints especificados.
- Mensageria RabbitMQ: publicação de `producao.status.alterado.event`,
  `producao.concluida.event`, `kanban.status.alterado.event`,
  `advertencia.registrada.event`, `advertencia.limite.atingido.event` e
  `projeto.mesa.abandonado.event`; consumo de `encomenda.criada.event`,
  `estoque.consumo.realizado.event` e `nivel.alterado.event`.
- Job agendado (`ProjetoMesaAuditoriaScheduler`) para sinalizar projetos de mesa
  sem evolução.
- Advertência automática ao responsável ativo do setor em inspeções
  não conformes, com alerta crítico ao atingir o limite de 3.
- 127 testes (unit, integração e security) com JaCoCo ≥ 80% de cobertura de linha.
- Documentação: `README.md` e `docs/API.md`.

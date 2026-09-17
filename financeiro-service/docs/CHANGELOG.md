<!-- Keep a Changelog: https://keepachangelog.com/pt-BR/1.1.0/ -->

## [Não publicado]

### Adicionado
- Scaffold inicial do microsserviço.
- Dependências: JPA, Flyway, AMQP e Eureka client.
- Segurança JWT (validação HMAC + issuer do Auth Service) e RBAC via
  `@PreAuthorize`, com acesso exclusivo do nível `ADMIN`.
- Tratamento de erros padronizado (`ErrorResponse` + `GlobalExceptionHandler`).
- Entidades JPA: `CategoriaFinanceira`, `LancamentoFinanceiro`, `DoacaoRecurso`,
  `ValorHoraNivel`, `ParametroOverhead`, `FechamentoEncomenda`, `CustoEncomenda`,
  `SolicitacaoCompra`, `HorasEncomenda` e enums de domínio.
- Migrations Flyway `V1`–`V9` (todas as tabelas do Financeiro).
- Repositórios e DTOs de request/response, eventos e relatórios.
- Services: categorias, lançamentos (status `PENDENTE`/`ATRASADO` e varredura de
  vencidos), doações/recursos, valor/hora por nível, overhead, fechamento de
  encomenda, custeio por ordem (Job Order Costing), solicitações de compra e
  relatórios (fluxo de caixa, DRE, lucratividade, inadimplência, doações x
  despesas e custo por máquina).
- Controllers REST de todos os endpoints especificados.
- Mensageria RabbitMQ: `FinanceiroEventPublisher` (`lancamento.vencido.event`,
  `custo.calculado.event`, `compra.solicitada.event`) e consumo de
  `encomenda.criada.event`, `horas.validadas.event` e `producao.concluida.event`.
- Contrato `PaymentProcessor` para futura integração de pagamentos (sem
  implementação no MVP).
- 78 testes (unit, integração e security) com JaCoCo ≥ 80% de cobertura de linha.
- Documentação: `README.md` e `docs/API.md`.

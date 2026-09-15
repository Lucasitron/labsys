# Changelog

Todas as alterações notáveis neste projeto são documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [0.1.0] - 2026-09-15

### Added
- **Entidades JPA:** Pessoa, Funcionario, Tutor, RegistroPontoDiario, ApontamentoHoras, ProcessoSeletivo, Treinamento, AvaliacaoTreinamento, HistoricoNivel.
- **Enums:** NivelAcesso (0-4), PessoaStatus, TipoApontamento, StatusApontamento, StatusProcesso.
- **Repositórios:** 9 repositórios Spring Data JPA com queries derivadas e JPQL customizado.
- **Migrations Flyway:** V1–V9 com schema completo do banco PostgreSQL.
- **Segurança:** Validação JWT compartilhada com Auth Service; filtro OncePerRequestFilter; RBAC por @PreAuthorize; entry points customizados (401/403).
- **Endpoints REST (14):** CRUD de pessoas, funcionários, nível de acesso, apontamento de horas, processo seletivo e treinamento (LMS).
- **Mensageria RabbitMQ:** Consumer de eventos RFID (`access.rfid.event`) com consolidação de ponto diário; publishers de `nivel.alterado.event` e `horas.validadas.event`.
- **Validação de coerência:** Horas encomenda + projeto ≤ horas presença por dia.
- **Testes:** 47 testes (unitários, integração, segurança, mensageria) com cobertura JaCoCo ≥ 80% (BUNDLE).
- **Documentação:** README.md, API.md, CHANGELOG.md, BUGS.md, ACTION_PLAN.md.

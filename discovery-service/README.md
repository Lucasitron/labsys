# discovery-service

Service Registry (Netflix Eureka) do Sistema de Gestão FabLab. Responsável pelo
registro e descoberta dinâmica de todos os microsserviços.

## Pré-requisitos

*   Java 21
*   Maven 3.9+

## Como executar

```sh
mvn spring-boot:run
```

Dashboard: http://localhost:8761

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8761` |
| `EUREKA_HOST` | Hostname do da instância | `localhost` |
| `EUREKA_URI` | URL do defaultZone | `http://localhost:8761/eureka` |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
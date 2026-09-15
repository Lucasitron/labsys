# gateway-service

API Gateway (Spring Cloud Gateway) do Sistema de Gestão FabLab. Ponto único de
entrada: roteamento dinâmico via Eureka, CORS e balanceamento de carga.

## Pré-requisitos

*   Java 21
*   Maven 3.9+
*   Eureka (`discovery-service`) em execução

## Como executar

```sh
mvn spring-boot:run
```

Health: http://localhost:8080/actuator/health

## Como testar

```sh
mvn test
```

## Configuração

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP | `8080` |
| `EUREKA_URI` | URL do defaultZone do Eureka | `http://localhost:8761/eureka` |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas (separadas por vírgula) | `http://localhost:5173,http://localhost:3000` |

## Documentação

*   [API](docs/API.md)
*   [Relatório de bugs](docs/BUGS.md)
*   [Plano de ação](docs/ACTION_PLAN.md)
*   [Changelog](docs/CHANGELOG.md)
# API — gateway-service

> Em construção. Rotas explícitas configuradas em `application.yml`; o provedor
> dinâmico (`discovery.locator`) expõe os demais serviços via Eureka.

## Rotas

| Método | Path | Destino |
| :--- | :--- | :--- |
| `*` | `/api/auth/**` | `lb://auth-service` |
| `*` | `/api/rh/**` | `lb://rh-service` |
| `*` | `/api/estoque/**` | `lb://estoque-service` |
| `*` | `/api/vendas/**` | `lb://vendas-service` |
| `*` | `/api/financeiro/**` | `lb://financeiro-service` |
| `*` | `/api/producao/**` | `lb://producao-service` |
| `*` | `/api/notification/**` | `lb://notification-service` |

## Health e métricas

*   `GET /actuator/health`
*   `GET /actuator/gateway/routes` — rotas ativas.
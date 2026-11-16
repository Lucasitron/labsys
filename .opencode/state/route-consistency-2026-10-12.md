# Consistência de rotas — decisões (Fase 3) — 2026-10-12

> Fecho da Fase 3 do plano de auditoria (`audit-sweep-2026-10-12.md §5`).
> Método: cruzamento rotas do backend × call-sites do front + smoke HTTP end-to-end.
> Regra de fronteira: **backend PT é a fonte da verdade dos paths**; o front adapta.
> Aceite da fase: **zero 404 real** + divergências **resolvidas ou documentadas**.

## 1. Chamadas do front sem rota (404 real) — ✅ resolvidas (front)

| Arquivo | Antes | Decisão | Estado |
|---|---|---|---|
| `lib/api/producao/resumo.ts` | `GET /producao/resumo` | apontar p/ `GET /dashboard/resumo` | ✅ |
| `producao/projetos.ts` (documentos GET/POST) | `/producao/projetos/{id}/documentos` | **removidas** (não existem no back/spec) | ✅ |
| `producao/projetos.ts` (materiais GET) | `/producao/projetos/{id}/materiais` | **removidas** (BOM pertence ao Estoque) | ✅ |
| `producao/projetos.ts` (materiais saída POST) | `/producao/projetos/{id}/materiais/saida` | **removidas** | ✅ |

**Smoke (dev):** login → `GET /dashboard/resumo`, `/dashboard/summary`, `/notificacoes/nao-lidas/contagem`,
`/notification/notificacoes`, `/rh/pessoas`, `/estoque/boms`, `/auth/permissions`, `/producao/projetos`,
`/vendas/encomendas`, `/financeiro/lancamentos` → **todos 200**. `POST /rh/pessoas` (cadastro+acesso
atômico) → 201 e o novo usuário loga (**200**). **Zero 404.**

## 2. Divergências de path (§1.3) — decisões

| # | Divergência | Decisão | Motivo |
|---|---|---|---|
| 1 | `/producao/resumo` × `/dashboard/resumo` | **`/dashboard/resumo`** | Agregação mora no Dashboard; front adaptado. Alias `/dashboard/summary` mantido (compat EN). |
| 2 | `/producao/projetos/{id}/materiais` × `/estoque/boms/{id}` | **`/estoque/boms`** | Materiais/BOM pertencem ao Estoque; front não usa mais o path de Producao. |
| 3 | `/rh/apontamentos-horas/{id}/validar` (PUT) × `/rh/horas/{id}/validar` (PATCH) | **ambos mantidos (documentado)** | Duas famílias roteadas e testadas (B-13): `apontamentos-horas` = apontamentos (F4), `horas` = fachada de horas (disponíveis/validar/rejeitar). Deduplicar exigiria refactor de service sem defeito observável — fora do "código mínimo". |
| 4 | `/notificacoes` × `/notification/notificacoes/*` | **ambos mantidos** | O alias curto `/notificacoes(+nao-lidas/contagem)` é **consumido** pelo App Shell/sino (comentário explícito em `Notification/routes.php`). Remover quebraria o front. |
| 5 | `/producao/projetos-mesa/{id}/auditorias` × `/producao/auditorias-projeto-mesa/{id}` | **`/producao/auditorias-projeto-mesa`** (canônico) | O front usa o canônico (`mesas.ts`). O outro permanece como **shim** de leitura; sem consumidor hoje. |
| 6 | `/notification/configuracao-canais/{canal}` × `/notification/configuracao-canais` (PUT) | **ambos mantidos** | `ConfiguracaoCanalController::update(?canal)` aceita path **ou** body `canal` (design dual-mode). Sem consumidor no front (gestão via outro fluxo) — documentado. |
| 7 | `PATCH /api/tasks/{id}` (EN) × `/api/producao/tarefas/{id}` (PT) | **ambos mantidos (compat)** | `tasks/{id}` é alias de compat M8 (Dashboard/routes.php) → `TarefaController::concluir`. Regra: **PT canônico**; EN só como shim documentado. |

**Política adotada:** um path **canônico** (PT, por módulo) + *shims* de compat **documentados** no
`routes.php` de cada módulo. Nenhum shim remove funcionalidade; nenhum é a única forma de uma ação.

## 3. Rotas órfãs (Anexo A) — decisão

As 59 rotas sem par no front **não** são defeito: são a **API pública** dos módulos (CRUD completo:
`DELETE`/`PUT`/`GET {id}`/sub-recursos) usada por telas ainda não construídas. Decisão: **manter**
(e não "remover"), pois:
- remover apagaria **funcionalidade** (ex.: `DELETE /maquinas/{id}`, `PUT /setores/{id}/checklist/{idItem}`);
- a spec exige o CRUD por módulo; a cobertura do front é incremental.

**Ação:** inventário abaixo serve de *backlog de front* (o front deve cobrir estas rotas nas telas
correspondentes). Nenhuma será removida do backend sem decisão de produto.

- **Auth (2):** `validate-rfid`, `permissions` *(consumidas indiretamente: RFID/ponto e tela de permissões)*
- **Rh (18):** funcionários, horas, processo-seletivo, treinamentos, certificados/*, extrato-mensal
- **Estoque (1):** `GET /estoque/boms` *(smoke OK)*
- **Vendas (1):** `GET /vendas/encomendas/{id}/status`
- **Financeiro (4):** solicitações-compra/{id}, parâmetros-overhead, fechamento-encomenda/*
- **Producao (27):** CRUD de projetos/tarefas/kanban/máquinas/setores/inspeções/advertências/mesas/parâmetros
- **Notification (5):** alias nao-lidas, revisar, configuração de canais
- **Dashboard (1):** `GET /dashboard/resumo` *(agora consumida pelo front)*

## 4. Conclusão

- **Zero 404 real** (aceite atendido).
- Divergências §1.3: 2 resolvidas (front) + 5 **documentadas** como canônico+shim.
- Órfãs: **mantidas** como API pública (documentadas), com backlog de cobertura no front.
- Fase 3 **concluída por decisão**; sem alteração de rota (nenhum defeito a remover com segurança).

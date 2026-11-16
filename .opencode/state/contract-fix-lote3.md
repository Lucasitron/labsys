# Contract fix — LOTE 3 (2026-10-11)

**Escopo:** resíduos do review `contract-review-2026-10-11.md` §"Lote 3 (resto)" + backlog do
despacho do Orquestrador. Base: backend `origin/dev/backend` (`32c67fa`) × frontend
`origin/dev/frontend` (`b6c47b5`, Lote 2 aplicado). Lastro verificado no Java (`*-service/`),
em `docs/` e em `.opencode/specs/features/*/requirements.md`.

**Regra de lado:** backend PT é fonte da verdade → front adapta. Sem lastro no Java e sem
requisito real → **GATE PERMANENTE** (front exibe mensagem, sem endpoint), não construir.

## Vereditos de lastro (resumo)

| # | Item | Java | Requirements/docs | Veredito |
|---|---|---|---|---|
| 1 | Chamados + preventivas de máquina | `MaquinaController`: só CRUD/status/uso/historico. Sem | requirements producao/docs06: sem | **GATE** (front já gateado — JÁ-OK) |
| 2 | Concluir inspeção 5S + nomear-auditor | `Inspecao5SController`: só POST/GET/GET{id}/DELETE; `StatusInspecao`=OK/NAO_CONFORME (resultado, não workflow). Sem | requirements: sem | **GATE** (front já gateado — JÁ-OK) |
| 3 | `aprovar-alteracao` de setor | `SetorController`: só CRUD + materiais/sinalizações/checklist. Sem workflow proposta/aprovação persistido | requirements: sem | **GATE** (front já gateado — JÁ-OK) |
| 4 | Treinamentos: `list/get/sessao/tarefa-final/agenda/disponibilidade/guias` | `TreinamentoController`: só `POST /treinamentos` + `POST/GET /{id}/avaliacoes`. Sem | requirements rh: só criar + avaliar; docs02: "guias" = campo `url_conteudo`, não endpoints | **GATE** (5 fns já gateadas JÁ-OK; `list/get` ainda chamam rotas inexistentes → L3-FRONT-01) |
| 5 | `GET /financeiro/lancamentos/:id` | `LancamentoController`: só POST/GET-lista/PUT-pagamento. Sem | docs05/requirements: mesmo trio | **GATE** (front já usa lista/filtro — JÁ-OK) |
| 6 | `POST /api/auth/forgot-password` (C01) | `AuthController`: sem. Docs: "`emailVer` foi descartado"; requirements auth §10 Fora de Escopo: "Recuperação de senha / reset de senha" | — | **GATE PERMANENTE** (tela já informa — JÁ-OK; nenhum endpoint) |
| 7 | Mover-grupo PS (grupo×processo) | Java tem AMBOS: `GrupoProcessoSeletivo.etapa` + `ProcessoSeletivo.grupo` (FK); mover = `PATCH /{id}/estagio` no **processo**. Back Laravel espelha; front `moverEstagio` já converte p/ grupo-1-membro (comentário no código) | — | **JÁ-OK** (só documentar — 3B) |
| 8 | `Suspensa` em máquinas | `MaquinaStatus` Java = DISPONIVEL/EM_USO/MANUTENCAO; back `MaquinaStatus.php` idem. Sem `SUSPensa` | — | **GATE** (front envia `SUSPENSA` → 422 → L3-FRONT-02 remover) |
| 9 | Lista de membros p/ níveis | `NivelController` Java: só matriz/alterarMembro/convidar. Sem list | requirements: sem | **JÁ-OK** (front não chama list; alterar/convidar OK; só documentar — 3B) |
| D14 | pendências/penalidades/ranking-5s/resumo | Sem controller Java correspondente | requirements: sem | **GATE** (resumo-mock manter; demais → L3-FRONT-08 gatear) |

**Conclusão 3A × 3B:** nenhum item do BACK tem lastro → **zero tasks de construção no backend**.
O Lote 3A é 100% `dev/frontend`; o 3B é registro de gates permanentes (quase tudo JÁ-OK).

---

## LOTE 3A — correções com lastro/contrato (implementar agora, `dev/frontend`)

### L3-FRONT-01 — Gatear `list/get` de treinamentos (rotas inexistentes no back)
- **Lado+branch:** FRONT, `dev/frontend`.
- **Arquivos:** `frontend/src/lib/api/rh/treinamentos.ts`; telas
  `frontend/src/routes/(app)/pessoas/treinamentos/+page.svelte`,
  `.../treinamentos/[id]/+page.svelte`, `.../treinamentos/agenda/+page.svelte`,
  `.../treinamentos/sessao/+page.svelte`.
- **Descrição:** o back só tem `POST /rh/treinamentos` + `POST/GET /rh/treinamentos/:id/avaliacoes`
  (lastro Java + requirements). `listTreinamentos` (`GET /rh/treinamentos`) e `getTreinamento`
  (`GET /rh/treinamentos/:id`) retornam 404. Gatear ambas com `throw new Error('...indisponível — sem par no backend (Lote 3).')`
  no padrão das 5 fns já gateadas; telas de lista/detalhe/agenda/sessão exibem a mensagem
  (empty-state), sem chamada de rede. Manter `avaliarTreinamento` (com par) intacto.
- **Aceite:** [ ] `rg "rh/treinamentos[$\"]" frontend/src/lib` só ocorre em `avaliacoes`;
  [ ] as 4 telas renderizam mensagem amigável, zero 404 no console;
  [ ] `avaliar` continua POSTando em `/rh/treinamentos/:id/avaliacoes`.

### L3-FRONT-02 — Remover `Suspensa` do status de máquina (enum sem par)
- **Lado+branch:** FRONT, `dev/frontend`.
- **Arquivos:** `frontend/src/lib/api/producao/maquinas.ts` (`MAQUINA_STATUS_PARA_UPPER`),
  `frontend/src/lib/types/producao.ts` (`MaquinaStatus`), telas/filtros que listam o status.
- **Descrição:** Java `MaquinaStatus` e back `MaquinaStatus.php` têm só
  DISPONIVEL/EM_USO/MANUTENCAO. O mapa `Suspensa→'SUSPENSA'` gera PUT 422. Remover a entrada
  (o `throw 'sem par no backend'` de `atualizarStatusMaquina` passa a cobrir) e remover
  `Suspensa` do type + selects/filtros (mapear dado legado p/ `Em manutenção` na leitura, sem enviar).
- **Aceite:** [ ] nenhum `SUSPENSA` no código; [ ] filtro/criação de máquina sem opção Suspensa;
  [ ] PUT status só envia os 3 valores do back.

### L3-FRONT-03 — D05: filtros de encomendas (status + search client-side)
- **Lado+branch:** FRONT, `dev/frontend`.
- **Arquivos:** `frontend/src/lib/api/vendas/encomendas.ts` (`listEncomendas`).
- **Descrição:** back `EncomendaController` lê só `status` (Title-case `in:Fila,Produção,...`) +
  `clienteId`; `search` é ignorado. Enviar só `status`/`clienteId`; aplicar `search` como filtro
  client-side (nome do cliente) sobre o retorno, no padrão de `obterMaquinas`.
- **Aceite:** [ ] `search` não vai à query; [ ] filtro por texto funciona sobre a lista;
  [ ] `status` enviado no vocabulário Title-case do back.

### L3-FRONT-04 — D06: busca/paginação client-side de orçamentos
- **Lado+branch:** FRONT, `dev/frontend`.
- **Arquivos:** `frontend/src/lib/api/vendas/orcamentos.ts` (`listOrcamentos`).
- **Descrição:** back `OrcamentoController` lê só `status`/`clienteId`, sem paginação; `search`/
  `page`/`pageSize` atuais são ignorados. Enviar só `status`/`clienteId`; `search` filtra
  client-side; `page`/`pageSize` via `paginarClient` (precedente estoque/produção).
- **Aceite:** [ ] query só com `status`/`clienteId`; [ ] busca e paginação funcionam client-side.

### L3-FRONT-05 — D07: adaptadores restantes de vendas (marketplace/interação/tarefa)
- **Lado+branch:** FRONT, `dev/frontend`.
- **Arquivos:** `frontend/src/lib/api/vendas/marketplace.ts`,
  `frontend/src/lib/api/vendas/interacoes.ts` (ou onde `responsavel` é lido),
  `frontend/src/lib/api/vendas/*tarefa*` (`criadoPor` vs `createdBy`), types/consumidores.
- **Descrição:** `mapEncomenda`/`mapOrcamento` já tratam `codigo`/`cliente{}`/`valorTotal|Final`
  (JÁ-OK, não tocar). Completar: marketplace sem `encomenda{}`/`createdBy` (guard `??`),
  interação sem `responsavel` (omitir/''), tarefa `criadoPor` (ler `criadoPor`, alias de `createdBy`).
  Coagir `valorTotal/valorFinal` string→`Number` onde ainda faltar.
- **Aceite:** [ ] telas de marketplace/interações/tarefas sem `undefined`;
  [ ] `rg "createdBy|responsavel\.nome" frontend/src/lib/api/vendas` revisado item a item.

### L3-FRONT-06 — D16: exibir a chave do token uma única vez
- **Lado+branch:** FRONT, `dev/frontend`.
- **Arquivos:** `frontend/src/lib/api/configuracoes/sistema.ts` (`criarToken`, `RawToken`/`TokenIntegracao`
  + campo `chave?`), `frontend/src/routes/(app)/configuracoes/sistema/+page.svelte`.
- **Descrição:** contrato: `POST /configuracoes/tokens` devolve `{...,chave}` exibida 1x; hoje o
  front descarta (`mapToken` sem chave) e a tela só toastifica. Capturar `chave` no retorno,
  abrir modal pós-criação com a chave + botão copiar, descartar ao fechar (nunca reexibir/persistir).
- **Aceite:** [ ] modal mostra a chave 1x com copiar; [ ] fechar limpa da memória;
  [ ] lista nunca exibe chave; [ ] revogar 204 intacto.

### L3-FRONT-07 — T02: alinhar RBAC fino do front com `rbac-matrix.md`
- **Lado+branch:** FRONT, `dev/frontend`.
- **Arquivos:** `frontend/src/lib/utils/permissions.ts` (`VIEW_RULES`/`EDIT_RULES`, `canSee5S`, etc.).
- **Descrição:** decisão real é do back (403), mas as regras locais divergem da matriz em ao menos:
  `canSee5S` nega Estagiário (matriz: 5S = Visualizar p/ Estagiário; só Recrutando é excluído);
  conferir `VIEW_RULES` (ex.: `rh` inclui Recrutando — matriz diz "Visualizar (próprio)", ok se a
  tela filtra próprio) e `EDIT_RULES` vs regra 5 (vínculo `usuario_responsabilidade`).
  Ajustar p/ espelhar a matriz; onde o front não sabe o vínculo, preferir exibir + deixar o back
  negar (403 com toast) a esconder ação legítima.
- **Aceite:** [ ] checklist regra-a-regra matriz × `permissions.ts` resolvido;
  [ ] Estagiário vê 5S (leitura); [ ] Recrutando segue sem 5S/extrato; [ ] sem `VIEW/EDIT_RULES` mais
  permissivo que a matriz em escrita.

### L3-FRONT-08 — D14: gatear pendências/penalidades/ranking-5s (sem back; manter mock do resumo)
- **Lado+branch:** FRONT, `dev/frontend`.
- **Arquivos:** `frontend/src/lib/api/producao/pendencias-5s.ts`, `penalidades-5s.ts`,
  `ranking-5s.ts`; telas consumidoras. NÃO tocar em `resumo.ts`/`mocks.ts` (`USE_PRODUCAO_MOCK` mantido).
- **Descrição:** sem controller Java nem rota Laravel (`/producao/pendencias-5s`, `/penalidades-5s`,
  `/ranking-5s` → 404). Gatear as 3 fns de leitura (+ criar/resolver/penalizar, se exportadas) com
  `throw '...indisponível — sem par no backend (Lote 3).'`; telas exibem empty-state com a mensagem.
  Resumo continua no mock até decisão de produto.
- **Aceite:** [ ] zero chamadas a `/producao/(pendencias|penalidades|ranking)-5s` (rg limpo fora do throw);
  [ ] telas mostram mensagem, sem 404; [ ] resumo-mock intacto.

---

## LOTE 3B — gates permanentes / documentação (sem código novo no back)

| ID | Item | Estado | Ação |
|---|---|---|---|
| L3-DOC-01 | Chamados/preventivas, concluir/nomear-auditor, aprovar-alteracao, extras de treinamento (5 fns), `GET lancamento/:id`, forgot-password | Front JÁ gateado / tela informativa | Nenhum código. Registrar aqui como **GATE PERMANENTE**: só reabrir com requisito + lastro Java. |
| L3-DOC-02 | Mover-grupo PS | JÁ-OK (front `moverEstagio` documenta a semântica no comentário) | Nada a fazer; semântica canônica: **etapa vive no processo; grupo é agrupamento** (Java `GrupoProcessoSeletivo.etapa` + `ProcessoSeletivo.grupo`). |
| L3-DOC-03 | Membros p/ níveis | JÁ-OK (front usa matriz/alterar/convidar; sem list nos dois lados) | Nada a fazer; se produto pedir diretório de membros, abrir requisito novo (fora Lote 3). |
| L3-DOC-04 | C01 forgot-password | JÁ-OK (tela "versão futura + procurar admin") | Nenhum endpoint `POST /api/auth/forgot-password` — proibido sem NFR de e-mail (emailVer descartado). |

## Decisões de enxugamento
- **Reusado:** todos os gates do Lote 2 (máquinas, inspeções, setores, 5 treino-fns, lancamento-via-lista,
  forgot-tela, mover-PS) — nenhuma task nova para eles.
- **NÃO planejado (explicitamente):** qualquer endpoint novo no back (9/9 itens sem lastro Java);
  `GET /configuracoes/perfil` (já resolvido no Lote 1 via `/auth/me`); `GET /financeiro/lancamentos/:id`;
  `POST /api/auth/forgot-password`; CRUD de chamados/preventivas; workflow de aprovação de setor;
  transição de inspeção; LMS além de criar/avaliar; listagem de membros; pendências/penalidades/ranking.
- Cada `create` acima é FRONT-adapter com consumidor real (tela que hoje quebra ou mente); nenhum
  arquivo de conveniência, nenhum contrato novo.

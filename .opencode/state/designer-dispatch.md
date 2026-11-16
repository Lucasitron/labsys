# DESPACHO — Designer (Frontend) · 19/09/2026

> Preparado pelo Orchestrator. Despacho é **manual** (usuário dispara o subagent `frontend/designer`).
> Alvos: **(A) Atualizar mockup Produção v2** · **(B) Criar mockup Configurações**.

---

## Contexto comum (ler antes)

- `.opencode/state/frontend-orchestration.json` — decisões PO v2 registradas na feature `producao` e nova feature `configuracoes`
- `.opencode/specs/frontend/design-system.md` — tokens (nunca hex literal: brand/success/warn/danger/elevated/border/etc.)
- `.opencode/specs/frontend/conventions.md` — padrões SvelteKit (mockup espelha só HTML/CSS, não Svelte)
- `docs/frontendDatails.md` §10 — matriz de permissões por nível (Admin 0 · Bolsista 1 · Voluntário 2 · Estagiário 3 · Recrutando 4)
- `docs/mokups/treinamentos/` — **exemplo canônico de formato** (requisitos.md + instrunctions.md + full.html)
- Mockup Produção atual: `docs/mokups/producao/` (`full.html` 3734 linhas, `requisitos.md` 11 telas, `instrunctions.md`)
- Sidebar/nav base: `docs/mokups/dashboard/dashboard.html` (item "Configurações" já existe na sidebar, sem rota)

## Regras do Designer (inalteráveis)

1. **Nunca** cria código Svelte — HTML+CSS puro (Tailwind CDN) no mockup.
2. **Nunca** altera `docs/other`, specs de feature nem código frontend real.
3. UI text **pt-BR**; código/types EN quando houver.
4. Todos os estados: default, loading, empty, error, RBAC-restricted.
5. **Não inventa endpoints**: endpoints novos necessários (documentação/upload, saída-BOM, nota auto 5S validada) devem ser **marcados como "assumidos/contrato pendente"** no `requisitos.md` do mockup e sinalizados ao Orchestrator para o `contract.gap.md` — não tratados como reais.
6. `data-testid` coerente com a feature real nos elementos-chave.
7. Ao terminar: atualizar `mockups`/`nota` no estado `frontend-orchestration.json` e avisar o Orchestrator (sai de `blocked`).

---

# (A) PRODUÇÃO — ATUALIZAÇÃO MOCKUP v2

Arquivos: editar `docs/mokups/producao/full.html`, `requisitos.md`, `instrunctions.md` (manter navegável, sem console errors).

## A1. Setores 5S — editar setor + questionário

- `modal-setor` passa a ser **criar + editar** (título dinâmico "Novo setor"/"Editar setor", botão "Salvar alterações").
- Ação **Editar** no card de cada setor (ícone `✎` / `⋮` → "Editar").
- Campos: nome, cadência (semanal/quinzenal/mensal), responsáveis (N:N), PS (processo seletivo), auditores.
- RBAC: **Admin salva direto**; não-Admin (responsável/auditor) → proposta vai para o fluxo de aprovação já existente (`modal-aprovar-alteracao`).
- `modal-checklist-setor` (questionário): manter **5 S padrão obrigatório e travado**; itens **específicos** do setor editáveis (adicionar/remover/editar texto). **SEM reordenação e SEM peso** (cada item vale igual). Não-Admin → "Enviar p/ aprovação" (fluxo atual); Admin salva direto.

## A2. Auditoria — nota automática

- `modal-concluir-auditoria`: **REMOVER o campo "Nota da auditoria" manual**.
- **Nota calculada automaticamente** em tempo real: item marcado / total de itens (cada item vale igual, inclusive os específicos do setor).
- Exibir prévia dinâmica no modal (ex.: "Nota calculada: **80** · cartão **amarelo**" — faixas atuais: verde ≥90, vermelho <70, senão amarelo).
- Manter aviso: backend **recalcula/valida** ao concluir (`PUT /inspecoes-5s/{id}/concluir`); 1h automática e cartão são do backend.

## A3. Pendências — detalhe em modal

- Novo **`modal-detalhe-pendencia`** (padrão do app, largura `max-w-lg`): 
  - **Origem**: auditoria que gerou (código/data), auditor, setor.
  - **Itens não conformes** (lista dos itens do checklist reprovados na auditoria).
  - **Fotos/anexos** (thumbnails + mini-galeria; no mockup usar placeholders).
  - **Prazo** (danger se vencida) e **situação**.
  - **Penalidade sugerida/aplicada** (link/contexto p/ `modal-registrar-penalidade`).
  - **Histórico da resolução** (resolvido por, o que foi feito, data — preenchido após resolver).
- Ação "abrir detalhe" na tabela (título clicável ou `⋮` → "Detalhes"). Inventário continua **11 telas** (modal, não rota).

## A4. Projetos — nova tela `/producao/projetos/[id]` (12ª tela)

- Inventário do `requisitos.md` passa para **12 telas** (adicionar linha §2 + ordem de implementação).
- Card de projeto na listagem navega para o detalhe (`data-goto`).
- Estrutura da tela (header + abas `?tab=`):
  - **Geral**: código, nome, cliente, responsável (Avatar), área, caderno/mesa, força-tarefa, status, progresso, prazo; ações editar (`modal-novo-projeto` reusado como editar).
  - **Documentação**: adicionar **texto (descrição longa/notas)**, **links** (URLs com rótulo) e **imagens** — suporta **upload E link/URL** (ambos): botão "Adicionar arquivo/imagem" e campo "Adicionar link". Galeria de imagens (thumbnails). Endpoints de upload marcados como contrato pendente.
  - **Materiais (Estoque/BOM)**: tabela de materiais do projeto (item, código, quantidade, disponibilidade) consultando a BOM via gateway; ação **"Registrar saída"** → modal de saída **vinculada ao projeto** (baixa no Estoque). Marcar endpoint como assumido/contrato pendente.
  - Aba opcional **Tarefas** do projeto (atalho p/ /producao/tarefas?projetoId=).

## A5. Tarefas — descrição + checklist

- `modal-detalhe-tarefa`: adicionar bloco **Descrição** (texto completo) + **Checklist de atividades** (checkboxes com progresso "2/5 concluídas", barra `%`).
- **Status permanece manual**: marcar itens não altera status automaticamente (apenas apoio visual/progresso).

---

# (B) CONFIGURAÇÕES — MOCKUP NOVO (feature 9)

Criar `docs/mokups/configuracoes/` com `requisitos.md`, `instrunctions.md`, `full.html` (+ estados necessários). Prefixo de rotas: `/(app)/configuracoes`.

## Telas (confirmadas PO)

| # | Tela | Rota | Acesso |
|---|---|---|---|
| 1 | Perfil | `/configuracoes/perfil` | qualquer autenticado (dados próprios) |
| 2 | Usuários | `/configuracoes/usuarios` | **Admin only** (hide p/ demais) |
| 3 | Permissões | `/configuracoes/permissoes` | **Admin only** (hide p/ demais) |
| 4 | Sistema | `/configuracoes/sistema` | **Admin only** (hide p/ demais) |

## Conteúdo por tela

- **Perfil**: dados do próprio usuário (nome, e-mail, avatar, telefone) + **alterar senha** (senha atual/nova/confirmação) + preferências (tema, notificações).
- **Usuários**: tabela (nome, e-mail, nível/RBAC, status ativo/inativo) + "Novo usuário" (modal com nome/e-mail/nível) + editar/ativar-desativar. Filtros: busca + nível + status.
- **Permissões**: **matriz RBAC por nível** (linhas = permissões/ações por módulo; colunas = níveis 0–4), espelhando `frontendDatails.md` §10; visual somente-leitura com p/ editar somente Admin (switches). 
- **Sistema**: parâmetros globais do lab: nome/identidade do FabLab, cadência padrão 5S, período de rotação de setores, faixas de nota por cartão, prazo pendência grave, token/secrets (mascarados), logs/auditoria (seção informativa).

## Navegação/RBAC

- Sidebar item "Configurações" ativo com breadcrumb `/ Configurações / <tela>`.
- Acesso padrão: usuários/permissoes/sistema sumidos (`hide`) para não-Admin; **nenhum disable**.

---

## Critérios de conclusão (Designer — checklist padrão)

- [ ] Producao `full.html` navegável sem console errors (v2 aplicada nas 4 frentes: setor, auditoria, pendência, projeto [id], tarefa)
- [ ] `requisitos.md` Produção com 12 telas + endpoints novos marcados como contrato pendente
- [ ] Configurações: `requisitos.md` + `instrunctions.md` + `full.html` gerados (4 telas, RBAC hide)
- [ ] Estados principais representados (loading/empty/error para listas novas)
- [ ] Tokens do design-system (sem hex literal)
- [ ] `data-testid` nos elementos-chave
- [ ] Estado `frontend-orchestration.json` atualizado (producao `mockupPath` v2, configuracoes sai de blocked) e Orchestrator avisado
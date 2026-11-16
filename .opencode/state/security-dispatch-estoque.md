# DESPACHO — Security (auditoria) · ESTOQUE · 21/09/2026

> Audite **sem alterar código** e **sem merge**. Working tree: branch `dev/frontend` (NÃO rode git checkout/switch).
> Feature `estoque` (commits `f7b3be5` · `192004c` · `f16d0c7` · `f300d83` + testes).

## Escopo
- `frontend/src/lib/api/stock/**` (itens/entradas/saidas/emprestimos/fornecedores/localizacoes/bom/request) — PT `/api/estoque/**`
- `frontend/src/lib/utils/permissions.ts` (`isResponsavelEstoque`/`canEdit('estoque')`/`canVerFornecedores`)
- `frontend/src/routes/(app)/estoque/**` (todas as rotas)
- `frontend/src/lib/components/estoque/**` (ItemPicker/MovementList/PeoplePicker)
- `frontend/tests/unit/estoque/**`

## Focos
1. **RBAC/responsabilidade** (D-2): `canEdit('estoque')` ampliado — não escalar privilégio de forma incorreta; guardas de rota (itens/novo, entradas/nova, saidas/nova, emprestimos/novo, devolucao, editar, fornecedores/novo, localizacoes/novo) consistentes; Recrutando barrado.
2. **Escrita no client**: criar/saída com 422/409 — mensagens sensíveis não expostas; saldo negativo não passa a responsabilidade de validação (backend deve validar saldo).
3. **Empréstimo/devolução**: `devolverEmprestimo` PUT com body `{}` — avaliar; campo prazo `FutureOrPresent` client; quantidade ≤ disponível é só UX (validação server-side obrigatória).
4. **Import/export CSV**: upload multipart — validar tipo/tamanho? (front mínimo); download blob — sem injeção de nome; CSV injection (fórmulas `=`, `+`, `-`, `@` em células exportadas) — checar escape? (importante!)
5. **XSS**: `{@html}`? URLs de itens/fornecedores? `cnpj`/`contato` renderizados com escape padrão; `@render`/`{@html}` grep.
6. **IDOR/IDs em URL**: `itens/[id]`, `emprestimos/[id]`, `entradas/[id]`, `saidas/[id]` — IDs opacos; backend deve validar recurso do usuário onde aplicável.
7. **Redirecionamentos**: guardas usam `redirect` fixo (sem open-redirect); `sanitizeRedirect` preservado.
8. **Dados sensíveis**: `searchPeople` expõe pessoas — limite de campos?; tomador por id; zero logs de dados pessoais.
9. **Segredos**: fallback `http://localhost:8080` herdado; sem chaves hardcoded.
10. **Dependências/regressão**: sem libs novas; `request.ts` FormData sem Content-Type (correto).

## Formato
- Veredito: **APROVADA** | **APROVADA COM RESSALVAS** | **REPROVADA**
- Achados por severidade: **Alto / Médio / Baixo / Informativo** × arquivo:linha × evidência × impacto × recomendação (front vs backend/PO)
- Ressalvas herdadas (não regressão)
- Comandos usados
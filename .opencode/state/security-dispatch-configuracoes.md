# DESPACHO — Security (auditoria) · CONFIGURACOES · 21/09/2026

> Audite **sem alterar código** e **sem merge**. Working tree: branch `dev/frontend` (NÃO rode git checkout/switch).

## Escopo
Feature `configuracoes` (commits `d06833d`, `d9d0875`, testes). Arquivos:
- `frontend/src/lib/api/configuracoes/**` (perfil, senha, usuarios, permissoes, sistema)
- `frontend/src/lib/config/menu.ts` · `frontend/src/lib/utils/permissions.ts`
- `frontend/src/lib/components/configuracoes/**`
- `frontend/src/routes/(app)/configuracoes/**`
- `frontend/tests/unit/permissions.test.ts`

## Focos (padrão das features anteriores)
1. **RBAC no client**: guarda de rota Admin-only em `+layout.ts` — pode ser burlada? (é client-side; documento a expectativa de enforcement no backend). Hide vs disable na sidebar.
2. **Autorização de operações**: criar/editar usuário, alterar status, matriz de permissões (`PUT /api/permissoes/{modulo}/{nivel}`), tokens de integração (criar/revogar), alterar senha — o front impede UI, mas o backend valida? Reportar o gap.
3. **Senha** (`api/configuracoes/senha.ts`): validação client (`>=8`, confirmação) não substitui backend; verificar que a senha **não é logada/persistida** no client (sem localStorage/sessionStorage/console).
4. **Tokens de integração** (`sistema.ts`/`sistema/+page.svelte`): token/chave única exibida/copiada — não persistir em storage; verificar tratamento; revogação com confirmação.
5. **Permissões carregadas do servidor**: `hasPermissao`/matriz não deve ser "auto-concedida" no client (ex.: fallback que libere demais). Verificar fallback sem matriz (usa canView/canEdit — não deve escalar privilégio).
6. **Injeção/XSS**: `{@html}`? URLs (`logo-url`) — sanitização; `href`/`src` de fontes não confiáveis.
7. **Redirect**: `redirect(303,'/configuracoes/perfil')` — destino fixo (sem open-redirect).
8. **Dados sensíveis**: email/telefone/avatar — exposição mínima; sem vazar em erro/log.
9. **Segredos**: sem chaves/tokens hardcoded; uso de `VITE_API_BASE_URL`/fallback localhost (herdado) — reiterar em prod.
10. **Dependências**: sem libs novas.

## Formato
- Veredito: **APROVADA** | **APROVADA COM RESSALVAS** | **REPROVADA**
- Achados por severidade: **Alto / Médio / Baixo / Informativo** × arquivo:linha × evidência × impacto × recomendação (front vs backend/PO)
- Ressalvas herdadas (não regressão): fallback `http://localhost:8080`, contratos 🟡, enforcement server-side
- Comandos usados
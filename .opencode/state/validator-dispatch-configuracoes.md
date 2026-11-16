# DESPACHO — Validator (contexto zero) · CONFIGURACOES · 21/09/2026

> Você **não** participou da implementação. Valide **do zero** contra spec + mockup. **Não altere código.**
> Working tree: branch `dev/frontend` (NÃO rode git checkout/switch).

## Artefatos
- Spec (fonte da verdade): `.opencode/specs/frontend/features/configuracoes/{requirements,plan,tasks}.md` (tasks.md = cfg-001..021, autoritativo)
- Mockup: `docs/mokups/configuracoes/full.html` (+ `requisitos.md`, `instrunctions.md`, `verify-config.js`) — 4 telas `#cfg-perfil`/`#cfg-usuarios`/`#cfg-permissoes`/`#cfg-sistema`
- Implementação (commits `d06833d`, `d9d0875`, teste):
  - `frontend/src/lib/types/configuracoes.ts`
  - `frontend/src/lib/api/configuracoes/{perfil,senha,usuarios,permissoes,sistema}.ts`
  - `frontend/src/lib/config/menu.ts` (submenu Configurações, `canSee: isAdmin`)
  - `frontend/src/lib/utils/permissions.ts` (`isAdmin`/`canViewConfiguracoes`/`hasPermissao`; helpers producao intactos)
  - `frontend/src/lib/components/configuracoes/{PermCell,ModalUsuario,ModalConfirm}.svelte`
  - `frontend/src/routes/(app)/configuracoes/**` (layout+guarda + 4 telas)
  - `frontend/tests/unit/permissions.test.ts` (61 casos)

## Checklist (DoD plan §8 + requirements)
1. **4 rotas**: `/configuracoes/{perfil,usuarios,permissoes,sistema}` existem; perfil acessível a todos autenticados; demais Admin-only com `redirect(303,'/configuracoes/perfil')` p/ não-Admin (hide, nunca disable).
2. **Sidebar**: submenu Configurações com 4 itens; Usuários/Permissões/Sistema ocultos p/ não-Admin (verificar `menu.ts` + render da Sidebar); nada de "disable".
3. **RBAC por nível** (req §RBAC): matriz Admin(0) editar; Bolsista(1)/Voluntário(2)/Estagiário(3)/Recrutando(4) hide nas 3 telas admin; perfil ok p/ todos.
4. **Matriz RBAC**: renderizada de `GET /api/permissoes` (🟡) via `PermCell`; dirty-state/diff até salvar; "restaurar padrão" com confirmação; não persistida localmente.
5. **Estados** em todas as 4 telas: Loading(skeleton) · Empty · Empty filtrado(chips) · Error(banner+retry).
6. **URL state** Usuários: `?page&search&nivel&situacao` como fonte de verdade (deep-link).
7. **Perfil**: dados pessoais + avatar iniciais + alterar senha (valida `nova===confirmacao`, `>=8`) + preferências; badge "Acesso liberado para todos os níveis"; toast de sucesso.
8. **Usuários**: tabela com chips nivel/situacao, filtros, paginação, `ModalUsuario` criar/editar, desativar com confirmação.
9. **Sistema**: identidade + cadência 5S (aviso afeta Produção) + tokens criar/revogar com confirmação.
10. **Tipos** (`cfg-001`) refletem o mockup; `PermissaoMatriz` compatível com `Module` de `types/auth.ts`; sem `any`; sem `console.log`.
11. **Endpoints 🟡** reportados/comentados; erro amigável sem gateway.
12. **Sem libs externas novas**; pt-BR; data-testid `cfg-*`; tokens design-system; a11y (`aria-label`).
13. **Não regressão**: `npm run test` (162 verdes) + `npm run check` baseline 12/15/7.
14. **Fora de escopo**: nenhum arquivo de outra feature tocado além dos declarados (menu.ts/permissions.ts do ConflictMap — mudanças aditivas).

## Formato do relatório
- Veredito: **APROVADA** | **APROVADA COM RESSALVAS** | **REPROVADA**
- Tabela: item × status × evidência (arquivo:linha) × observação
- Ressalvas com severidade (BLOQUEANTE / MÉDIA / BAIXA) e ação sugerida
- Comandos rodados e resultados literais
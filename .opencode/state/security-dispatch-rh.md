# DESPACHO — Security (Frontend) · RH · 21/09/2026

> Auditoria de segurança da feature `rh` implementada (commits 4b33407 · 9d76271 · 642a856 · b8c4a8c · 607084e · 4406cb0).
> **Princípio código mínimo: sinalizar também código desnecessário/duplicado encontrado.**

## Contexto
- Workspace `/home/lucasitron/Documentos/05-dev/labSys`; branch `dev/frontend` (somente leitura)
- Abordagem "contexto zero" (não confiar no backend)

## Escopo
- `src/lib/api/rh/*` (5 módulos, endpoints 🔴/🟡 + `searchPeople` legado) · `src/lib/types/rh.ts` · `src/lib/utils/rh-status.ts` · `src/lib/utils/permissions.ts` (helpers RH) · `src/routes/(app)/pessoas/**` (12 rotas + guarda) · `src/lib/config/menu.ts` (submenu com `canSee: isAdmin`) · `ui/{TypeBadge,DonutChart,KanbanBoard}`

## Pontos de checagem (ex.:)
- RBAC: guardas 302/303 client-side; Níveis Admin-only (ausência de DOM, nunca disable); Kanban/avaliação/sessão/níveis ocultos para não-autorizados (hide real); Estagiário só próprio (filtro `personId` + hide de colunas/ações); `isInstrutor` marcador
- XSS: sem `{@html}`; nome/email/feedback/motivo escapados; avatar/iniciais sem injeção
- Autorização de escrita: criar/editar/excluir pessoas, avaliar, validar/rejeitar horas, alterar nível, convidar — disponíveis a qualquer autenticado no front (backend deve isolar — avaliar impacto; ex.: `DELETE /api/rh/pessoas/{id}` inline)
- Exposição: matriz de níveis, membros, avaliações, motivos de rejeição; `personId` de terceiros acessível por URL (backend isola?)
- Práticas: query só params não-nulos; sem credenciais em URL (senha/username no body — verificar); Content-Type json; sem console.log/any; `crypto.getRandomValues` para gerar senha (ok); nota 0-10 validada client (backend revalida?)
- Error handling: mensagens pt-BR sem vazar detalhe; 404 tratado
- Código mínimo: helpers justificados; paginação server-side; sem duplicação

## Produto
- **UM output completo inline** (proibido resumir): veredito (APROVADA/APROVADA COM RESSALVAS/REPROVADA) + contagem por severidade (Alto/Médio/Baixo/Informativo) + lista itemizada com evidência arquivo:linha + correção mínima + verificação (`npm run check`; `rg "{@html}|console\\.log|: any"` no escopo rh).

## Regras
- Somente leitura; **NÃO editar/commitar**; sem libs.
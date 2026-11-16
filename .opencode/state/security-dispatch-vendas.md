# DESPACHO — Security (Frontend) · VENDAS · 23/09/2026

> Auditoria de segurança da feature `vendas` implementada (commits ed7dbcd · 7e3eb8b · 0a00ccf · 83ff024 · 3ad1396 · 7beb58b).
> **Princípio código mínimo: sinalizar também código desnecessário/duplicado.**

## Contexto
- Workspace `/home/lucasitron/Documentos/05-dev/labSys`; branch `dev/frontend` (somente leitura); abordagem "contexto zero"

## Escopo
- `src/lib/api/vendas/*` (7 módulos, quase tudo 🔴) · `src/lib/types/vendas.ts` · `src/lib/utils/vendas-{status,format}.ts` · `src/lib/utils/permissions.ts` (helpers criador) · `src/routes/(app)/vendas/**` (11 rotas + guarda + modal) · `src/lib/config/menu.ts` (submenu sem canSee) · `ui/{Timeline,MoneyInput}`

## Pontos de checagem (ex.:)
- RBAC: guardas 302/303; Editar vs "Sugerir alteração" por `canEditVendas(record)` (hide real, ausência de DOM); solicitacoes `canDecide`; criação liberada a autenticados; Recrutando barrado; Kanban mover sem gate dedicado (avaliar)
- Autorização de escrita: CRUD clientes/orçamentos/encomendas, mover Kanban, marketplace, marketing, decidir solicitações — hide-only (backend deve autorizar por criador+Admin; avaliar impacto)
- XSS: sem `{@html}`; nome/descrição/justificativa/motivo escapados; `mailto:`/`tel:` (scheme fixo, ok?); MoneyInput sem injeção
- Exposição: valores, documentos (CPF/CNPJ mascarado?), motivos de rejeição, `createdBy` alheio
- Práticas: query mínima; sem credenciais em URL; Content-Type json; sem console.log/any; CPF/CNPJ validação client (backend revalida?); ViaCEP tolerante a falha (avaliar SSRF/privacidade — é client→API pública, ok?); nota/lock otimista do Kanban (409)
- Error handling: mensagens pt-BR sem vazar detalhe; 404 tratado
- Código mínimo: helpers justificados; `?editar=` em vez de rota extra (ok?); sem duplicação

## Produto
- **UM output completo inline** (proibido resumir): veredito + contagem (Alto/Médio/Baixo/Informativo) + itens com evidência arquivo:linha + correção mínima + verificação (`npm run check`; `rg "{@html}|console\\.log|: any"` no escopo vendas).

## Regras
- Somente leitura; **NÃO editar/commitar**; sem libs.
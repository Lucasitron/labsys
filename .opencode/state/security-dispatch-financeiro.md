# DESPACHO — Security (Frontend) · FINANCEIRO · 23/09/2026

> Auditoria de segurança da feature `financeiro` implementada (commits 2e78ed8 · 4ccc9e3 · b9a6eb0 · 49cbfb2 · 06aa0f6).
> **Princípio código mínimo: sinalizar também código desnecessário/duplicado.**

## Contexto
- Workspace `/home/lucasitron/Documentos/05-dev/labSys`; branch `dev/frontend` (somente leitura); abordagem "contexto zero"

## Escopo
- `src/lib/api/financeiro/*` (6 módulos, tudo 🔴) · `src/lib/types/financeiro.ts` · `src/lib/utils/financeiro-{status,format}.ts` · `src/routes/(app)/financeiro/**` (7 rotas + guarda Admin-only) · `src/lib/config/menu.ts` (submenu sem canSee — pai hide-total)

## Pontos de checagem (ex.:)
- RBAC: guarda 303 p/ todo `/financeiro/**`; item invisível via `canView` `[0]`; submenu sem canSee (avaliar — pai basta?); nenhuma ação visível a não-Admin (ausência de DOM); modais só montam p/ Admin
- Autorização de escrita: CRUD lançamentos/categorias/doações/fechamentos/valores/overhead/compras, registrar pagamento — hide-only (backend `@PreAuthorize` Admin; avaliar impacto)
- XSS: sem `{@html}`; descrições/observações/motivos escapados; valores MoneyInput
- Exposição: valores, saldos, inadimplentes, margens, cobertura (tudo Admin-only por rota — ok?); `idReferenciaExterna`
- Práticas: query mínima; sem credenciais em URL; Content-Type json; sem console.log/any; `?periodo=` validado (regex `^\d{4}-\d{2}$`?); congelamento respeitado (sem recalcular)
- Error handling: mensagens pt-BR sem vazar detalhe; 404 tratado
- Código mínimo: helpers justificados; modal duplicado (ok?); sem soma client

## Produto
- **UM output completo inline** (proibido resumir): veredito + contagem (Alto/Médio/Baixo/Informativo) + itens com evidência arquivo:linha + correção mínima + verificação (`npm run check`; `rg "{@html}|console\\.log|: any"` no escopo financeiro).

## Regras
- Somente leitura; **NÃO editar/commitar**; sem libs.
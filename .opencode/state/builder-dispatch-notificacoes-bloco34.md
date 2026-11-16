# DESPACHO — Builder (Frontend) · NOTIFICACOES — Bloco 3 (B3 Admin) + Bloco 4 (B4 Menu) · 21/09/2026

> Implemente **not-008..not-010** de `.opencode/specs/frontend/features/notificacoes/tasks.md` (ler tasks + plan + mockup requisitos.md).
> **Princípio código mínimo: reuso > create; nada especulativo.**

## Contexto
- Branch `dev/frontend` — **NÃO rode git checkout/switch** (confirme; se não for, PARE e reporte)
- Blocos 1-2 implementados: guarda not-005 já ativo em `routes/(app)/notificacoes/+layout.ts`
- Reuso: UI kit (Skeleton/TableSkeleton/EmptyState/ErrorBanner/SearchInput/Select/Tabs/Chip/Pagination/PageHeader), `api/notifications.ts`
- NÃO alterar specs/docs; NÃO commitar; NÃO alterar permissions.ts

## Tasks
- **not-008** `src/routes/(app)/notificacoes/preferencias/+page.svelte` — coluna 1 "Canais disponíveis" (in-app/e-mail/push com explicação) + matriz tipo×canal (3 colunas desktop) com toggle `peer-checked` (`bg-brand` ativo), badge "2 de 3 canais" por linha; "Restaurar padrão" (todos ON, push de `sistema` OFF) marca dirty; "Salvar preferências" habilita dirty → savePreferences → toast; falha → banner mantendo valores; pill "Restrito ao Admin"; data-testid notif-pref-list/toggle/save/reset; loading skeleton do card.
- **not-009** `src/routes/(app)/notificacoes/historico/+page.svelte` — filtros (SearchInput + Selects tipo/canal/status + periodo + "Limpar filtros" + chips); params via URL quando contrato confirmar (D-4) senão estado local de página; tabela Notificação/Tipo/Destinatário/Canal/Status/Enviada com badges canal e status (Lida muted / Não lida brand+dot), hover `bg-elevated/40`; paginação 10; loading TableSkeleton / empty "Nenhum envio..." / error+retry; SEM botão "Exportar CSV" (R-10); data-testid history-table/row/filters/clear-filters/pagination; "Enviada em" mono tabnums.
- **not-010** `src/lib/config/menu.ts` — MODIFY: no item `Notificações` (linha ~64) adicionar `children: [{label:'Minhas notificações', path:'/notificacoes'}, {label:'Preferências de canais', path:'/notificacoes/preferencias', canSee: isAdmin}, {label:'Histórico', path:'/notificacoes/historico', canSee: isAdmin}]`. Não alterar module/icon. Confirmar `isAdmin` importado.

## Regras
1. Criar/editar SÓ os 3 arquivos acima.
2. pt-BR; aria-label; sem any; sem libs; Svelte 5 runes; tokens design-system.
3. Ao final: `npm run test` (regressão 252+) + `npm run check` (0 errors; registrar warnings) + `npm run build`.

## Relatório
- Por task ✅/❌ + evidência; mudanças (+/−); contrato 🟡 observado (D-3/D-4); resultados test/check/build.
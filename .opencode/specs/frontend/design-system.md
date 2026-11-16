# Frontend System Specs — Design System

> **Fonte:** `docs/mokups/*/full.html` + Tailwind real (`frontend/tailwind.config.ts`).
> Tokens canônicos abaixo; **nunca** hex literal em componente.

## Theme
Dark, elegante, azul. Glow radial azul atrás de hero cards. **Sem modo light no MVP.**

## Color Tokens (Tailwind `theme.extend.colors`)
| Token | Valor | Uso |
|---|---|---|
| `brand` | `#1c80de` | ação primária, foco, ativo |
| `brandhi` | `#3a95e8` | hover de brand |
| `ink` | `#e6edf3` | texto primário |
| `muted` | `#8b95a7` | texto secundário |
| `surface` | `#111725` | cards/panels |
| `elevated` | `#111a2c` | inputs, hover, nested |
| `border` | `#252d42` | bordas sutis |
| `bg` | `#0a0e17` | fundo de página |
| `success` | `#0ea641` | confirmar, positivo |
| `warn` | `#f59e0b` | pendente, near-limit |
| `danger` | `#f2060a` | erro, destrutivo |

> `docs/mokups/estoque/full.html` usa `#111a2c`/`#0a0e17` como tokens visuais — convertidos aqui
> para o par `elevated`/`bg`. Se mudar no mockup, atualizar token, não o componente.

## Typography
- Font: **Inter** (Google), fallback `system-ui`
- Mono: `ui-monospace, SFMono-Regular, Menlo, monospace` — códigos, quantidades, timestamps

| Papel | Classe |
|---|---|
| Título de página | `text-xl font-semibold tracking-tight` |
| Hero (detail) | `text-2xl font-semibold tracking-tight` |
| Título de seção | `text-sm font-semibold` |
| Label de campo | `text-xs font-medium text-muted uppercase tracking-wide` |
| Corpo | `text-sm text-ink` |
| Secundário | `text-xs text-muted` |
| Micro | `text-[10px] uppercase tracking-wide` |
| Código | `font-mono text-xs text-muted` |
| Números (tabela) | `font-mono tabnums text-sm` |

`tabnums` = `font-variant-numeric: tabular-nums` — sempre em colunas numéricas e KPIs.

## Spacing & Radii
- Base unit 4px; card padding `p-4|p-5|p-6`; gap grid `gap-4`; section `space-y-5|6`
- Cards `rounded-xl`; inputs/buttons `rounded-md`; pills/badges `rounded-full`
- Input padding `px-3 py-2.5`; input padrão `bg-elevated border border-border rounded-md`

## Shadows
- Card default: none (borda)
- Hero/focus: `shadow-2xl shadow-black/40`
- Botão primário: `shadow-lg shadow-brand/20`
- Dropdown/modal: `shadow-2xl shadow-black/50`

## Motion
- Transições `transition` (150ms); dropdown fade + translate-y(2px); skeleton `animate-pulse`
- Sem animações bouncy; sidebar collapse `transition-all duration-200`

## Componentes de Base

### Button Primary
```
bg-brand hover:bg-brandhi text-white px-3.5 py-2 rounded-md text-sm font-medium
transition shadow-lg shadow-brand/20 disabled:opacity-50 disabled:cursor-not-allowed
```

### Button Secondary
```
bg-elevated hover:bg-elevated/70 border border-border text-ink px-3.5 py-2 rounded-md
text-sm font-medium transition
```

### Button Ghost
```
text-muted hover:text-ink px-3 py-2 rounded-md hover:bg-elevated transition
```

### Button Danger
```
bg-danger/15 hover:bg-danger/25 text-danger border border-danger/30 px-3 py-2 rounded-md
text-sm font-medium transition
```

### Input
```
w-full bg-elevated border border-border rounded-md px-3 py-2.5 text-sm
placeholder-muted/60 focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/30 transition
```
Erro: `border-danger/60 focus:border-danger focus:ring-danger/30`

### Card
```
bg-surface border border-border rounded-xl p-5
```

### Badge semântico
```
inline-flex items-center gap-1.5 text-xs px-2 py-0.5 rounded bg-{tone}/15 text-{tone} border border-{tone}/30
```

### Dot + status
```
inline-flex items-center gap-1.5 text-xs text-{tone}
  <span class="w-1.5 h-1.5 rounded-full bg-{tone}"></span>
  {label}
```

### Sidebar item
- Inativo: `text-muted hover:text-ink hover:bg-elevated`
- Ativo: `bg-brand/10 text-brand border-l-2 border-brand`
- Layout: `flex items-center gap-3 px-3 py-2 mx-2 rounded-md`

### Tabela
- Header: `bg-elevated/50 text-[11px] text-muted uppercase tracking-wide font-medium border-b border-border`
- Row: `hover:bg-elevated/40 border-b border-border transition`
- Selected: `bg-brand/5 border-l-2 border-brand`

### Tabs
- Bar: `border-b border-border flex items-center gap-1 px-2 overflow-x-auto`
- Ativa: `text-brand border-b-2 border-brand`; inativa: `text-muted hover:text-ink border-b-2 border-transparent`

### Dropdown panel
```
absolute mt-2 bg-surface border border-border rounded-md shadow-2xl shadow-black/50 z-30 p-2
```

### Modal
```
fixed inset-0 z-50 flex items-center justify-center
backdrop: absolute inset-0 bg-black/60
card: relative bg-surface border border-border rounded-xl max-w-md w-full p-5 shadow-2xl mx-4
```

### Skeleton
```
bg-elevated rounded animate-pulse
```

### Empty block
- Centrado flex-col: ícone `w-12 h-12` em `bg-brand/10 border border-brand/30 rounded-2xl` + título `text-sm font-semibold` + msg `text-xs text-muted max-w-xs` + CTA opcional

### Donut chart
- SVG `viewBox="0 0 42 42"`, `r=15.9`, `stroke-width=5`, `-rotate-90`
- Track `stroke="#1a2235"`; slices `stroke-dasharray="{pct} {100-pct}"`, offset cumulativo negativo
- Centro: valor overlay (`absolute inset-0 flex items-center justify-center`); legenda `space-y-2 text-xs`

## Brand Glow (hero/login)
```css
background: radial-gradient(circle at 50% 40%,
  rgba(28,128,222,0.18) 0%,
  rgba(10,14,23,0) 60%);
```

## Iconografia
Heroicons v2 outline, stroke-width 1.5–2, `w-4 h-4` padrão.

## Densidade
- Compacta (tabelas/filtros): `py-2 / py-2.5`
- Confortável (formulários/hero): `py-3 / p-5 / p-6`

## Acessibilidade (obrigatório)
- Focus ring: `focus:ring-2 focus:ring-brand/40` (nunca remover)
- Disabled: `disabled:opacity-40` (nunca `cursor-not-allowed` sozinho)
- Touch targets ≥ 40×40px mobile
- Cor sozinha nunca expressa significado (sempre com ícone ou texto)

# Despacho Builder — AUTH (auth-001..auth-007)

> Data: 2026-09-20 · Autorizado pelo PO ("despache").
> Base: branch **`dev/frontend`** (vertente migrada — NOVA localização do código).
> OBS: plan.md/tasks.md referenciam `feature/frontend-login`; o código agora vive em
> `dev/frontend` — commits `19385cc` (login), `b5fde2c` (app shell+dashboard), `e56ea03` (estoque/UI kit).

## Feature
- **auth** (status PLANEJADA) — executa **apenas gaps** G-1..G-4; o restante já é aderente.

## Fonte da verdade
- Tasks: `.opencode/specs/frontend/features/auth/tasks.md` (`auth-001..auth-007`, 1 arquivo ≡ 1 task)
- Estratégia/gaps/checklists: `.opencode/specs/frontend/features/auth/plan.md` (§3 gaps, §4 ordem, §5 Validator, §6 Security)
- Visual: `docs/mokups/login/` (requisitos.md, login.html, loginEstados.html)
- Padrões: `.opencode/specs/frontend/system/{conventions,design-system}.md` + código real em `frontend/src/`

## Escopo de execução (em ordem)
| Task | Arquivo | Acao |
|---|---|---|
| auth-001 | `frontend/src/lib/api/client.ts` | modify — interceptor global 401 (logout+redirect sanitizado, **opt-out em `/auth/login`**, anti-loop) |
| auth-002 | `frontend/src/routes/auth/login/+page.svelte` | modify — consumir `?redirect=` sanitizado (fallback `/dashboard`) |
| auth-003 | `frontend/src/routes/auth/forgot-password/+page.svelte` | create — placeholder MVP |
| auth-004 | `frontend/tests/unit/errors.test.ts` | create — toUserMessage |
| auth-005 | `frontend/tests/unit/auth-store.test.ts` | create — stores/auth |
| auth-006 | `frontend/tests/unit/client.test.ts` | create — apiFetch/interceptor (depende auth-001) |
| auth-007 | `frontend/tests/unit/login-form.test.ts` | create — login-form |

## Regras (Builder)
- **NÃO tocar** em: `docs/`, `docs/mokups/`, specs de sistema, `opencode.json`, `stock/request.ts`, `dashboard.ts` (handlers ad-hoc devem permanecer — G-1 é idempotente), `(app)/+layout.ts`, `stores/auth.ts` (funcionam) e qualquer arquivo fora da task.
- RBAC na renderização: hide-nunca-disable (feature pública — sem tasks de RBAC).
- Textos pt-BR; Tailwind só tokens (nunca hex); TS strict (sem `any`); `data-testid` onde o mockup especificar.
- Estados loading/success/empty/error sempre tratados.
- Rodar `npm run check` (em `frontend/`) antes do relatório; `npx vitest run <file>` p/ testes da task.
- **Não abrir PR, não merge, não commit** — só implementar e relatar (o Orchestrator commita/mergea).

## Relatório final esperado (1 por task + resumo)
```json
{
  "tarefa_id": "auth-00X",
  "arquivos": ["..."],
  "comandos": ["npm run check", "npx vitest run ..."],
  "resultado_testes": "PASS|FAIL",
  "bloqueios": [],
  "duvidas": []
}
```
No final: consolidado das 7 tasks + status de `npm run test` (suíte completa) e `npm run check`.
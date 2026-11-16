# Briefing de handoff — Backend → Front (2026-10-12)

> Base: varredura do app inteiro + **Ciclos 1–7** aplicados e pushed em `dev/backend`
> (`330e7dd,186c79f,db26a36,8c499d3,23cef8b,5d2c15e,dfb4915`). Suíte **417 passed + 4 skipped**;
> migrações validadas em PG limpo. Backend é a fonte da verdade — o front adapta.
> Ação do front: **contrato + tratamento de erro + remoção de fluxos depreciados**.

---

## 1. Erros de entrada agora são **422** (Ciclo 5 — B-08)

- Toda validação de entrada que antes devolvia **400** (`InvalidArgumentException`) agora devolve **422**
  (`bootstrap/app.php`). FormRequest já devolvia 422.
- **Front**: trocar tratamento de 400 → 422 em todos os formulários/chamadas. O corpo de erro do
  422 (`{message, errors?}`) é o mesmo que o front já consome.

## 2. `POST /rh/pessoas` — payload estendido (Ciclo 6 — Fase 2)

Passa a provisionar, **opcionalmente**, vínculo (RH) + acesso (Auth) numa **única transação**.

**Campos aceitos** (todos opcionais, além dos atuais `nomeCompleto`, `matricula`, `dataAdmissao`,
`contato`, `turno`, `status`, `cpf`):

| Campo | Tipo | Efeito |
|---|---|---|
| `nivelAcesso` | int 0–4 | cria `rh.funcionario` com o nível |
| `departamento` | string | cria/define departamento do vínculo |
| `criarAcesso` | bool | provisiona credencial em `auth.login` |
| `email` | email | **obrigatório se `criarAcesso=true`** |
| `senha` | string (min 8) | com senha → conta **ATIVO** |
| `nomeUsuario` | string | opcional; default = parte local do e-mail |
| `setor` | string | opcional (login.setor) |

**Semântica**:
- `criarAcesso=true` **sem `senha`** → **convite**: conta criada com `situacao=PENDENTE` e senha inutilizável.
- Rol RBAC do acesso = `nivelAcesso` (mesma escala 0..4). Sem `nivelAcesso` → default **BOLSISTA (1)**.
- `criarAcesso=true` implica vínculo criado (mesmo sem `nivelAcesso`/`departamento`).
- **Atômico**: falha no provisionamento (ex.: e-mail já cadastrado) → **rollback** de pessoa+vínculo+acesso.
- **Resposta**: inalterada (`PessoaResource`, 201). Erros de negócio → **422**.

**Front a fazer** (consolidação — §2.3/§2.4 da varredura):
- `/pessoas/novo` vira **formulário único** com seções **Dados / Vínculo / Acesso (opcional)**.
- Remover o descarte silencioso: enviar `nivelAcesso`/`criarAcesso`/`email`/`senha` de verdade.
- **PS** ("Nova inscrição"): parar de chamar `POST /rh/pessoas`; usar **`POST /rh/processo-seletivo`**.
- `/configuracoes/usuarios`: rebaixar a **gestão de acesso** (situação + reenviar convite); "novo"
  redireciona para `/pessoas/novo`. Remover o `idUser` digitado manualmente no `ModalUsuario`.
- `mapPerson`: parar de forçar `nivel:'recrutando'` — mapear o nível real.
- `POST /rh/niveis/convites` (alias de vínculo): **depreciado** — migrar para `/rh/pessoas`.

## 3. `UNIQUE(login.id_user)` (migração `auth v9`)

- Uma credencial por pessoa. `POST /usuarios` com `idUser` duplicado → **422** ("Já existe acesso…").
- **Front**: nunca criar/login para pessoa que já tem acesso; o fluxo canônico é `criarAcesso` no cadastro.

## 4. Filtro `?status=` de empréstimos (Ciclo 3 — B-09)

- `GET /estoque/emprestimos?status=` valida com **allowlist** `ativos|atrasados|historico` → valor inválido = **422**
  (antes 400). **Front**: enviar só esses valores.

## 5. Nível espelha RBAC (Ciclo 4 — B-24)

- Alterar `nivel_acesso` no RH agora **espelha** `user_permissions.role` (autorização). Front pode
  ver mudança de papel/rota após alterar nível — refetch de permissões/sessão se necessário.

## 6. Custo de material (BOM) gera lançamento automático (Ciclo 7 — B-10)

- Ao **concluir produção**, o backend cria um lançamento **SAIDA** (`status=PENDENTE`, categoria DESPESA
  *"Materiais de produção"*) referenciado pela encomenda, com valor = Σ consumo × último preço de compra.
- **Front (Financeiro)**: a lista de lançamentos passará a exibir essas **SAIDAs automáticas** (observação
  *"Consumo de materiais (BOM) — encomenda N"*). Não criar/editar manualmente; é idempotente por encomenda.
- Encomenda sem preço de compra registrado → custo de material = 0 (sem lançamento).

## 7. Correções de front pendentes (§1.2 da varredura — front-only)

- `lib/api/producao/resumo.ts`: `GET /producao/resumo` (404) → **`GET /dashboard/resumo`**; remover
  `USE_PRODUCAO_MOCK=true` hardcoded.
- `lib/api/producao/projetos.ts` (documentos/materiais, 404): **gatear** ou redirecionar para
  `GET /estoque/boms/{id}`.

---

## Checklist rápido para o front

- [ ] 400 → **422** em todos os forms.
- [ ] Form único `/pessoas/novo` (Dados/Vínculo/Acesso) usando o payload da §2.
- [ ] PS → `POST /rh/processo-seletivo`.
- [ ] `/configuracoes/usuarios` rebaixado a gestão de acesso; sem `idUser` manual.
- [ ] `mapPerson` com nível real (sem `'recrutando'` fixo).
- [ ] Remover fluxo de criação do `ModalUsuario` e alias `/rh/niveis/convites`.
- [ ] `?status=` de empréstimos só `ativos|atrasados|historico`.
- [ ] Financeiro: exibir SAIDAs automáticas de material (somente leitura).
- [ ] `?status=`/resumo de produção → `/dashboard/resumo`; remover mock.

# Plano de backfill/dedup — `auth.login.id_user` (migração `auth v9`)

> Objetivo: aplicar com segurança a **UNIQUE `login.id_user`** (`uk_login_id_user`) em dados legados
> que podem ter contas duplicadas para a mesma pessoa (o antigo `POST /usuarios` aceitava `idUser`
> manual, sem FK/UNIQUE). Entregue em `dev/backend` (Ciclo 8).
> **Backend é a fonte da verdade. Toda ação de escrita em produção é explícita e reversível.**

## 1. Contexto
- `rh.pessoa` é a identidade única; `auth.login` é a credencial. O mapa canônico é `login.id_user = pessoa.id`.
- Até a v9, nada impedia **duas credenciais para o mesmo `id_user`**.
- `auth.user_permissions` é **chaveado por `id_user`** (o papel é da pessoa, compartilhado entre credenciais).
  `auth.access_log.id_user` é histórico (nullable). `auth.token_blacklist` é por token (não por usuário).
- **Regra de negócio**: 1 `id_user` ⇒ 1 credencial.

## 2. Auditoria pré-deploy (read-only)
Rodar **antes** de qualquer escrita em produção. Nenhuma destas consultas altera dados.

```sql
-- a) Grupos com id_user duplicado (o que a UNIQUE vai barrar)
select id_user, count(*) as total,
       array_agg(id order by id) as login_ids,
       array_agg(situacao order by id) as situacoes
from auth.login
group by id_user having count(*) > 1
order by id_user;

-- b) Quantos em cada situação (para priorizar revisão)
select count(*) from auth.login;

-- c) Credenciais órfãs (id_user sem rh.pessoa correspondente) — NÃO bloqueia a UNIQUE, mas revisar
select l.id, l.id_user, l.email
from auth.login l
left join rh.pessoa p on p.id = l.id_user
where p.id is null
order by l.id_user;

-- d) id_user com múltiplas permissões ativas (histórico; não bloqueia)
select id_user, count(*) filter (where active) as ativas
from auth.user_permissions group by id_user having count(*) filter (where active) > 1;
```

Alternativa empacotada (mesma leitura, saída tabular):
```bash
php artisan auth:dedup-login-id-user        # dry-run: lista grupos e o candidato a manter
```

## 3. Política de escolha (qual credencial manter)
Dentro de cada `id_user` duplicado, o **candidato mantido** é escolhido por:
1. `situacao = 'ATIVO'` primeiro (DEPOIS PENDENTE/DESATIVADO);
2. entre empates, a **credencial mais antiga** (menor `id`).

Os demais são **arquivados** em `auth.login_dedup_backup` e **removidos** de `auth.login`.
- `user_permissions` **não é tocado** (segue ligado ao `id_user` mantido).
- Tokens JWT antigos das credenciais removidas: `verify()` continua válido, mas o papel resolve para o
  `id_user` remanescente → recomendação: **forçar re-login** dos afetados após o deploy.
- A escolha automática pode não ser a "certa" do ponto de vista de negócio. Por isso o backup existe:
  revise a auditoria §2(a) e, se necessário, ajuste manualmente antes do `--apply`.

## 4. Runbook (deploy)
```bash
# 0) BACKUP do banco (obrigatório)
pg_dump ... > backup_pre_auth_v9.sql

# 1) Auditoria (read-only)
php artisan auth:dedup-login-id-user

# 2) (Opcional) aplicar o backfill explicitamente antes do migrate
#    — a migração auth v9a também executa isto automaticamente.
php artisan auth:dedup-login-id-user --apply

# 3) Aplicar migrações (v9a = dedup/backup; v9 = UNIQUE)
php artisan migrate --force

# 4) Verificar
psql -c "select conname from pg_constraint where conname='uk_login_id_user';"          # deve existir
psql -c "select count(*) from auth.login_dedup_backup;"                                # nº arquivados
psql -c "select id_user,count(*) from auth.login group by id_user having count(*)>1;"   # deve ser vazio
```
> Observação: se a **v9 já estiver aplicada** (ex.: deploy parcial anterior), a `v9a` roda igual e é
> **no-op** — a UNIQUE torna duplicados impossíveis, então nada é arquivado.

## 5. Rollback
```bash
php artisan migrate:rollback --step=2 --force   # reverte v9 (dropa UNIQUE) e depois v9a (restaura backup)
```
- A ordem do lote garante: **v9.down** (remove a UNIQUE) → **v9a.down** (`reverter()` re-insere as
  credenciais arquivadas com o `id` original e reposiciona a sequence; dropa `auth.login_dedup_backup`).
- `reverter()` é best-effort (pula o que já existir). Validado em PG limpo.

## 6. Casos de borda
| Caso | Tratamento |
|---|---|
| Contas com `id_user` duplicado | arquiva+remove (backup), preserva `user_permissions` |
| `id_user` sem `rh.pessoa` (órfão) | **não** tratado pela dedup; reportado na auditoria §2(c). FK **não** é adicionada (fora do escopo aprovado) |
| Múltiplas `user_permissions` por `id_user` | inalterado (não bloqueia a UNIQUE) |
| `access_log.id_user` | histórico, inalterado |
| `token_blacklist` | por token, inalterado |
| Reexecução/retry | idempotente (audita antes; sem duplicados = no-op) |

## 7. Evidência de validação (2026-10-12)
- Suíte: **419 passed + 4 skipped** (inclui `LoginIdUserDedupServiceTest`).
- PG limpo (`fablab_mig_check`): `migrate:fresh` cria `v9a`→`v9`; constraint `uk_login_id_user` e tabela
  `auth.login_dedup_backup` presentes; duplicados simulados → dry-run e `--apply` corretos (3→1, backup=2,
  permissão preservada=1); `migrate:rollback --step=2` remove constraint e tabela; `migrate` re-aplica.

## 8. Arquivos
- `app/Modules/Auth/Services/LoginIdUserDedupService.php` — `auditar()/executar()/reverter()`.
- `database/migrations/auth/2026_10_12_000000_auth_v9a_dedup_login_id_user.php` — backfill (antes da v9).
- `app/Console/Commands/DedupLoginIdUser.php` — `auth:dedup-login-id-user [--apply]`.
- `tests/Feature/Auth/LoginIdUserDedupServiceTest.php` — cobertura.

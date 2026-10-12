<?php

namespace App\Console\Commands;

use App\Modules\Auth\Services\LoginIdUserDedupService;
use Illuminate\Console\Command;

/**
 * Auditoria/backfill de `auth.login.id_user` antes da UNIQUE (auth v9).
 * Dry-run por padrão; `--apply` arquiva em `auth.login_dedup_backup` e remove.
 */
class DedupLoginIdUser extends Command
{
    protected $signature = 'auth:dedup-login-id-user {--apply : Arquiva e remove os duplicados}';

    protected $description = 'Audita (dry-run) ou aplica a deduplicação de auth.login.id_user (pré auth v9).';

    public function handle(LoginIdUserDedupService $dedup): int
    {
        $grupos = $dedup->auditar();

        if ($grupos === []) {
            $this->info('Nenhum id_user duplicado — nada a fazer.');

            return self::SUCCESS;
        }

        $linhas = array_map(fn ($g) => [
            $g['idUser'],
            $g['total'],
            $g['manter']['id'],
            $g['manter']['situacao'],
            $g['manter']['email'],
            implode(', ', array_map(fn ($p) => "#{$p['id']}({$p['situacao']})", $g['perdedores'])),
        ], $grupos);

        $this->table(
            ['id_user', 'logins', 'manter #', 'situação', 'e-mail (mantido)', 'arquivar'],
            $linhas,
        );

        if (! $this->option('apply')) {
            $this->warn('Dry-run. Rode `php artisan auth:dedup-login-id-user --apply` para arquivar/remover.');

            return self::SUCCESS;
        }

        $resultado = $dedup->executar();
        $this->info("Aplicado: {$resultado['removidos']} credencial(is) removida(s) em {$resultado['grupos']} grupo(s).");
        $this->line('Backup: auth.login_dedup_backup (reverter via rollback da migração auth v9a).');

        return self::SUCCESS;
    }
}

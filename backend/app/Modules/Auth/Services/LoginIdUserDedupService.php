<?php

namespace App\Modules\Auth\Services;

use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

/**
 * Backfill/dedup de `auth.login.id_user` para a migração `auth v9` (UNIQUE).
 *
 * `rh.pessoa.id` deve ter **uma** credencial (`id_user` 1:1). Contas legadas
 * podem ter duplicado `id_user` (o antigo `POST /usuarios` aceitava `idUser`
 * manual, sem FK/UNIQUE). Aqui os duplicados são **arquivados** em
 * `auth.login_dedup_backup` e removidos — **reversível** (`reverter()`).
 *
 * `auth.user_permissions` é chaveado por `id_user` (compartilhado entre os
 * duplicados); permanece intacto — o papel RBAC é da pessoa, não da credencial.
 * `auth.access_log` também é por `id_user` (histórico) — não é tocado.
 */
class LoginIdUserDedupService
{
    public const TABELA = 'auth.login_dedup_backup';

    /**
     * Candidatos de um `id_user`: ATIVO primeiro, depois credencial mais antiga
     * (menor `id`). O 1º é o mantido; os demais são arquivados/removidos.
     *
     * @return list<array<string,mixed>>
     */
    private function candidatos(int $idUser): array
    {
        return DB::table('auth.login')
            ->where('id_user', $idUser)
            ->orderByRaw("(situacao = 'ATIVO') DESC")
            ->orderBy('id')
            ->get()
            ->map(fn ($linha) => (array) $linha)
            ->all();
    }

    /**
     * Relatório (read-only) dos grupos duplicados — para o dry-run do comando.
     *
     * @return list<array{idUser:int,total:int,manter:array<string,mixed>,perdedores:list<array<string,mixed>>}>
     */
    public function auditar(): array
    {
        $grupos = DB::table('auth.login')
            ->select('id_user', DB::raw('count(*) as total'))
            ->groupBy('id_user')
            ->havingRaw('count(*) > 1')
            ->orderBy('id_user')
            ->get();

        $relatorio = [];
        foreach ($grupos as $grupo) {
            $candidatos = $this->candidatos((int) $grupo->id_user);
            $relatorio[] = [
                'idUser' => (int) $grupo->id_user,
                'total' => (int) $grupo->total,
                'manter' => $candidatos[0],
                'perdedores' => array_slice($candidatos, 1),
            ];
        }

        return $relatorio;
    }

    public function garantirTabelaBackup(): void
    {
        if (Schema::hasTable(self::TABELA)) {
            return;
        }

        Schema::create(self::TABELA, function (Blueprint $table): void {
            $table->id();
            $table->unsignedBigInteger('original_login_id');
            $table->bigInteger('id_user');
            $table->string('uuid');
            $table->string('email');
            $table->string('nome_usuario');
            $table->string('senha_hash');
            $table->string('setor')->nullable();
            $table->string('situacao', 20);
            $table->unsignedBigInteger('duplicate_of_login_id');
            $table->timestampTz('arquivado_em')->useCurrent();
        });
    }

    /**
     * Arquiva (backup) e remove as credenciais duplicadas. Idempotente.
     *
     * @return array{grupos:int,removidos:int}
     */
    public function executar(): array
    {
        $this->garantirTabelaBackup();

        $grupos = 0;
        $removidos = 0;

        foreach ($this->auditar() as $grupo) {
            $grupos++;
            foreach ($grupo['perdedores'] as $perdedor) {
                DB::table(self::TABELA)->insert([
                    'original_login_id' => $perdedor['id'],
                    'id_user' => $perdedor['id_user'],
                    'uuid' => $perdedor['uuid'],
                    'email' => $perdedor['email'],
                    'nome_usuario' => $perdedor['nome_usuario'],
                    'senha_hash' => $perdedor['senha_hash'],
                    'setor' => $perdedor['setor'],
                    'situacao' => $perdedor['situacao'],
                    'duplicate_of_login_id' => $grupo['manter']['id'],
                ]);

                DB::table('auth.login')->where('id', $perdedor['id'])->delete();
                $removidos++;
            }
        }

        return ['grupos' => $grupos, 'removidos' => $removidos];
    }

    /**
     * Reverte o backfill: restaura as credenciais arquivadas e descarta o
     * backup. Best-effort: pula o que já existir. Deve rodar **depois** de a
     * UNIQUE (auth v9) ter sido removida.
     *
     * @return int nº de credenciais restauradas
     */
    public function reverter(): int
    {
        if (! Schema::hasTable(self::TABELA)) {
            return 0;
        }

        $restaurados = 0;
        foreach (DB::table(self::TABELA)->orderBy('id')->get() as $linha) {
            if (DB::table('auth.login')->where('id', $linha->original_login_id)->exists()) {
                continue;
            }

            DB::table('auth.login')->insert([
                'id' => $linha->original_login_id,
                'id_user' => $linha->id_user,
                'uuid' => $linha->uuid,
                'email' => $linha->email,
                'nome_usuario' => $linha->nome_usuario,
                'senha_hash' => $linha->senha_hash,
                'setor' => $linha->setor,
                'situacao' => $linha->situacao,
            ]);
            $restaurados++;
        }

        DB::statement(
            "SELECT setval(pg_get_serial_sequence('auth.login','id'), "
            .'coalesce((SELECT max(id) FROM auth.login), 1))'
        );

        Schema::dropIfExists(self::TABELA);

        return $restaurados;
    }
}

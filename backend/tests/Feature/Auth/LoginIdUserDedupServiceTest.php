<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use App\Modules\Auth\Services\LoginIdUserDedupService;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/** Backfill/dedup de auth.login.id_user (pré auth v9 / UNIQUE). */
class LoginIdUserDedupServiceTest extends TestCase
{
    public function test_dedup_arquiva_duplicados_e_preserva_permissao_e_reverte(): void
    {
        // Simula o estado legado: sem a UNIQUE de id_user (criada como constraint).
        DB::statement('ALTER TABLE auth.login DROP CONSTRAINT IF EXISTS uk_login_id_user');

        $desativado = $this->criarLogin(['id_user' => 500, 'situacao' => SituacaoUsuario::DESATIVADO]);
        $ativoAntigo = $this->criarLogin(['id_user' => 500, 'situacao' => SituacaoUsuario::ATIVO]);
        $ativoNovo = $this->criarLogin(['id_user' => 500, 'situacao' => SituacaoUsuario::ATIVO]);

        UserPermission::create(['id_user' => 500, 'role' => Role::BOLSISTA, 'active' => true]);

        $service = app(LoginIdUserDedupService::class);

        // Auditoria (dry-run): 1 grupo, mantém o ATIVO mais antigo.
        $auditoria = $service->auditar();
        $this->assertCount(1, $auditoria);
        $this->assertSame(500, $auditoria[0]['idUser']);
        $this->assertSame(3, $auditoria[0]['total']);
        $this->assertSame($ativoAntigo->getKey(), $auditoria[0]['manter']['id']);
        $this->assertSame([$ativoNovo->getKey(), $desativado->getKey()], array_column($auditoria[0]['perdedores'], 'id'));

        // Aplicar: arquiva 2 e mantém 1; permissão (por id_user) intacta.
        $resultado = $service->executar();
        $this->assertSame(1, $resultado['grupos']);
        $this->assertSame(2, $resultado['removidos']);

        $this->assertSame(1, Login::where('id_user', 500)->count());
        $this->assertSame($ativoAntigo->getKey(), Login::where('id_user', 500)->firstOrFail()->getKey());
        $this->assertSame(2, DB::table(LoginIdUserDedupService::TABELA)->count());
        $this->assertSame(1, UserPermission::where('id_user', 500)->count());

        // Idempotente: nada mais a fazer.
        $this->assertSame(0, $service->executar()['removidos']);

        // Reverter restaura as 2 credenciais arquivadas.
        $this->assertSame(2, $service->reverter());
        $this->assertSame(3, Login::where('id_user', 500)->count());
    }

    public function test_sem_duplicados_nao_faz_nada(): void
    {
        $this->criarLogin(['id_user' => 600]);
        $this->criarLogin(['id_user' => 601]);

        $service = app(LoginIdUserDedupService::class);
        $this->assertSame([], $service->auditar());
        $this->assertSame(0, $service->executar()['removidos']);
    }
}

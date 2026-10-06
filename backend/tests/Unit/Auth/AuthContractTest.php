<?php

namespace Tests\Unit\Auth;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Services\TokenBlacklistService;
use Tests\TestCase;

class AuthContractTest extends TestCase
{
    public function test_verify_retorna_identidade_e_papel(): void
    {
        $login = $this->criarLogin(['id_user' => 7, 'setor' => 'ESTOQUE']);
        $token = $this->tokenPara($login, Role::VOLUNTARIO);

        $dados = app(AuthContract::class)->verify($token);

        $this->assertSame(7, $dados['idUser']);
        $this->assertSame('VOLUNTARIO', $dados['role']);
        $this->assertSame('ESTOQUE', $dados['setor']);
    }

    public function test_verify_null_para_invalido_ou_revogado(): void
    {
        $contract = app(AuthContract::class);
        $this->assertNull($contract->verify('invalido'));

        $login = $this->criarLogin();
        $token = $this->tokenPara($login);
        app(TokenBlacklistService::class)->blacklist($token, now()->addMinutes(15));
        $this->assertNull($contract->verify($token));
    }

    public function test_is_admin_so_com_permissao_ativa(): void
    {
        $contract = app(AuthContract::class);

        $admin = $this->criarAdmin(['id_user' => 11]);
        $this->assertTrue($contract->isAdmin(11));

        $login = $this->criarLogin(['id_user' => 12]);
        $this->comPermissao($login, Role::BOLSISTA);
        $this->assertFalse($contract->isAdmin(12));

        $inativo = $this->criarLogin(['id_user' => 13]);
        $this->comPermissao($inativo, Role::ADMIN, false);
        $this->assertFalse($contract->isAdmin(13));
    }
}

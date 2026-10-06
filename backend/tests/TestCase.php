<?php

namespace Tests;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use App\Modules\Auth\Services\TokenService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\TestCase as BaseTestCase;
use Illuminate\Support\Facades\Hash;

/** Base dos testes M1: banco limpo (migrate:fresh) + fábricas mínimas do Auth. */
abstract class TestCase extends BaseTestCase
{
    use RefreshDatabase;

    protected int $seq = 0;

    protected function criarLogin(array $over = []): Login
    {
        $this->seq++;

        return Login::create(array_merge([
            'id_user' => 1000 + $this->seq,
            'uuid' => 'rfid-'.$this->seq,
            'email' => "user{$this->seq}@fablab.org",
            'nome_usuario' => 'user'.$this->seq,
            'senha_hash' => Hash::make('senha123'),
            'setor' => 'PRODUCAO',
            'situacao' => SituacaoUsuario::ATIVO,
        ], $over));
    }

    protected function comPermissao(Login $login, Role $role = Role::ADMIN, bool $active = true): UserPermission
    {
        return UserPermission::create([
            'id_user' => $login->id_user,
            'role' => $role,
            'active' => $active,
        ]);
    }

    protected function criarAdmin(array $over = []): Login
    {
        $login = $this->criarLogin($over);
        $this->comPermissao($login, Role::ADMIN);

        return $login;
    }

    protected function tokenPara(Login $login, Role $role = Role::ADMIN, string $type = TokenService::TYPE_ACCESS): string
    {
        /** @var TokenService $tokens */
        $tokens = app(TokenService::class);

        return $type === TokenService::TYPE_REFRESH
            ? $tokens->generateRefreshToken($login, $role)
            : $tokens->generateAccessToken($login, $role);
    }

    /** @return array<string, string> */
    protected function authHeader(Login $login, Role $role = Role::ADMIN): array
    {
        return ['Authorization' => 'Bearer '.$this->tokenPara($login, $role)];
    }
}

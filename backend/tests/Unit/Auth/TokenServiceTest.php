<?php

namespace Tests\Unit\Auth;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Services\TokenService;
use App\Shared\Exceptions\TokenInvalidException;
use Tests\TestCase;

class TokenServiceTest extends TestCase
{
    public function test_access_tem_claims_e_ttl_900s(): void
    {
        $login = $this->criarLogin(['id_user' => 42, 'setor' => 'RH']);
        $svc = app(TokenService::class);

        $claims = $svc->parse($svc->generateAccessToken($login, Role::BOLSISTA));

        $this->assertSame('fablab', $claims['iss']);
        $this->assertSame(42, $claims['id_user']);
        $this->assertSame('BOLSISTA', $claims['role']);
        $this->assertSame('RH', $claims['setor']);
        $this->assertSame('access', $claims['type']);
        $this->assertSame((int) $login->getKey(), (int) $claims['sub']);
        $this->assertEqualsWithDelta(900, $claims['exp'] - $claims['iat'], 5);
        $this->assertFalse($svc->isRefreshToken($claims));
    }

    public function test_refresh_tem_type_refresh_e_ttl_7_dias(): void
    {
        $login = $this->criarLogin();
        $svc = app(TokenService::class);

        $claims = $svc->parse($svc->generateRefreshToken($login, Role::ADMIN));

        $this->assertTrue($svc->isRefreshToken($claims));
        $this->assertEqualsWithDelta(604800, $claims['exp'] - $claims['iat'], 5);
    }

    public function test_token_invalido_lanca_401(): void
    {
        $this->expectException(TokenInvalidException::class);
        app(TokenService::class)->parse('nao-e-um-token');
    }
}

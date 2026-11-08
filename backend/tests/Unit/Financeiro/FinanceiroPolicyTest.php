<?php

namespace Tests\Unit\Financeiro;

use App\Enums\Auth\Role;
use App\Shared\Auth\Caller;
use App\Policies\FinanceiroPolicy;
use App\Exceptions\ForbiddenException;
use PHPUnit\Framework\TestCase;

/** FinanceiroPolicy: só Admin passa (sem vínculo, sem próprio). */
class FinanceiroPolicyTest extends TestCase
{
    public function test_admin_passa(): void
    {
        $principal = new Caller(1, Role::ADMIN, true);

        $this->assertTrue($principal->isAdmin());
        FinanceiroPolicy::exigeAdmin($principal);
        $this->assertTrue(true);
    }

    /** @dataProvider papeisNegados */
    public function test_nao_admin_nega(Role $role): void
    {
        $this->expectException(ForbiddenException::class);
        FinanceiroPolicy::exigeAdmin(new Caller(2, $role, false));
    }

    /** @return array<string, array{0:Role}> */
    public static function papeisNegados(): array
    {
        return [
            'bolsista' => [Role::BOLSISTA],
            'voluntario' => [Role::VOLUNTARIO],
            'estagiario' => [Role::ESTAGIARIO],
            'recrutando' => [Role::RECRUTANDO],
        ];
    }

    public function test_custeio_default_nivel_dois(): void
    {
        $this->assertSame(2, \App\Services\Financeiro\CusteioService::NIVEL_DEFAULT);
    }
}

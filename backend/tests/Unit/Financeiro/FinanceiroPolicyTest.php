<?php

namespace Tests\Unit\Financeiro;

use App\Modules\Auth\Enums\Role;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Policies\FinanceiroPolicy;
use App\Shared\Exceptions\ForbiddenException;
use PHPUnit\Framework\TestCase;

/** FinanceiroPolicy: só Admin passa (sem vínculo, sem próprio). */
class FinanceiroPolicyTest extends TestCase
{
    public function test_admin_passa(): void
    {
        $principal = new FinanceiroPrincipal(1, Role::ADMIN, true);

        $this->assertTrue($principal->isAdmin());
        FinanceiroPolicy::exigeAdmin($principal);
        $this->assertTrue(true);
    }

    /** @dataProvider papeisNegados */
    public function test_nao_admin_nega(Role $role): void
    {
        $this->expectException(ForbiddenException::class);
        FinanceiroPolicy::exigeAdmin(new FinanceiroPrincipal(2, $role, false));
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
        $this->assertSame(2, \App\Modules\Financeiro\Services\CusteioService::NIVEL_DEFAULT);
    }
}

<?php

namespace Tests\Unit\Estoque;

use App\Modules\Auth\Enums\Role;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Policies\EstoquePolicy;
use App\Shared\Exceptions\ForbiddenException;
use PHPUnit\Framework\TestCase;

class EstoquePolicyTest extends TestCase
{
    private function principal(Role $role, bool $vinculo, int $id = 7): EstoquePrincipal
    {
        return new EstoquePrincipal(
            idPessoa: $id,
            role: $role,
            admin: $role === Role::ADMIN,
            responsavelEstoque: $vinculo,
        );
    }

    public function test_leitura_bloqueia_recrutando(): void
    {
        $this->expectException(ForbiddenException::class);

        EstoquePolicy::exigeLeitura($this->principal(Role::RECRUTANDO, false));
    }

    public function test_leitura_libera_demais_papeis(): void
    {
        foreach ([Role::ADMIN, Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO] as $role) {
            EstoquePolicy::exigeLeitura($this->principal($role, false));
        }

        $this->assertTrue(true);
    }

    public function test_edicao_movimentacao_exige_vinculo(): void
    {
        $this->assertTrue(EstoquePolicy::podeEditarMovimentacao($this->principal(Role::ADMIN, false)));
        $this->assertTrue(EstoquePolicy::podeEditarMovimentacao($this->principal(Role::BOLSISTA, true)));
        $this->assertTrue(EstoquePolicy::podeEditarMovimentacao($this->principal(Role::VOLUNTARIO, true)));

        $this->assertFalse(EstoquePolicy::podeEditarMovimentacao($this->principal(Role::BOLSISTA, false)));
        $this->assertFalse(EstoquePolicy::podeEditarMovimentacao($this->principal(Role::VOLUNTARIO, false)));
        $this->assertFalse(EstoquePolicy::podeEditarMovimentacao($this->principal(Role::ESTAGIARIO, true)));
        $this->assertFalse(EstoquePolicy::podeEditarMovimentacao($this->principal(Role::RECRUTANDO, true)));
    }

    public function test_edicao_bom_voluntario_nem_com_vinculo(): void
    {
        $this->assertTrue(EstoquePolicy::podeEditarBom($this->principal(Role::ADMIN, false)));
        $this->assertTrue(EstoquePolicy::podeEditarBom($this->principal(Role::BOLSISTA, true)));

        $this->assertFalse(EstoquePolicy::podeEditarBom($this->principal(Role::VOLUNTARIO, true)));
        $this->assertFalse(EstoquePolicy::podeEditarBom($this->principal(Role::BOLSISTA, false)));
        $this->assertFalse(EstoquePolicy::podeEditarBom($this->principal(Role::ESTAGIARIO, false)));
    }

    public function test_can_see_loans_e7(): void
    {
        $admin = $this->principal(Role::ADMIN, false, 1);
        $dono = $this->principal(Role::BOLSISTA, true, 7);

        $this->assertTrue(EstoquePolicy::canSeeLoans($admin, 99));
        $this->assertTrue(EstoquePolicy::canSeeLoans($dono, 7));
        $this->assertFalse(EstoquePolicy::canSeeLoans($dono, 99));

        $this->expectException(ForbiddenException::class);
        EstoquePolicy::exigeAcessoEmprestimo($dono, 99);
    }
}

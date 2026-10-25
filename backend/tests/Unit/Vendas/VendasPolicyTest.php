<?php

namespace Tests\Unit\Vendas;

use App\Modules\Auth\Enums\Role;
use App\Modules\Vendas\Policies\VendasPolicy;
use App\Modules\Vendas\VendasPrincipal;
use App\Shared\Exceptions\ForbiddenException;
use PHPUnit\Framework\TestCase;

class VendasPolicyTest extends TestCase
{
    private function principal(Role $role, int $id = 7): VendasPrincipal
    {
        return new VendasPrincipal(
            idPessoa: $id,
            role: $role,
            admin: $role === Role::ADMIN,
        );
    }

    public function test_leitura_bloqueia_recrutando(): void
    {
        $this->expectException(ForbiddenException::class);

        VendasPolicy::exigeLeitura($this->principal(Role::RECRUTANDO));
    }

    public function test_leitura_libera_demais_papeis(): void
    {
        foreach ([Role::ADMIN, Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO] as $role) {
            VendasPolicy::exigeLeitura($this->principal($role));
        }

        $this->assertTrue(true);
    }

    public function test_escrita_so_admin(): void
    {
        VendasPolicy::exigeEscrita($this->principal(Role::ADMIN));

        foreach ([Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO, Role::RECRUTANDO] as $role) {
            try {
                VendasPolicy::exigeEscrita($this->principal($role));
                $this->fail("escrita liberada para {$role->name}");
            } catch (ForbiddenException) {
            }
        }

        $this->assertTrue(true);
    }

    public function test_kanban_criador_ou_admin(): void
    {
        // Admin passa mesmo sem ser criador.
        VendasPolicy::exigirCriadorOuAdmin(99, $this->principal(Role::ADMIN));

        // Criador não-Admin passa.
        VendasPolicy::exigirCriadorOuAdmin(7, $this->principal(Role::BOLSISTA, 7));

        // Não-criador não-Admin → 403.
        try {
            VendasPolicy::exigirCriadorOuAdmin(99, $this->principal(Role::BOLSISTA, 7));
            $this->fail('kanban liberado para não-criador não-Admin');
        } catch (ForbiddenException) {
        }

        $this->assertTrue(true);
    }
}

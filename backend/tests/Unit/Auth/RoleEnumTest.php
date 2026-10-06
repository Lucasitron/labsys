<?php

namespace Tests\Unit\Auth;

use App\Modules\Auth\Enums\Role;
use Tests\TestCase;

class RoleEnumTest extends TestCase
{
    public function test_parse_por_codigo_e_nome(): void
    {
        $this->assertSame(Role::ADMIN, Role::parse(0));
        $this->assertSame(Role::RECRUTANDO, Role::parse(4));
        $this->assertSame(Role::BOLSISTA, Role::parse('BOLSISTA'));
        $this->assertSame(Role::VOLUNTARIO, Role::parse('voluntario'));
        $this->assertSame('Admin', Role::ADMIN->label());
    }

    public function test_parse_invalido_lanca_400(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        Role::parse(9);
    }
}

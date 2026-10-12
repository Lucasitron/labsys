<?php

namespace Tests\Unit\Auth;

use App\Modules\Auth\Enums\PermissaoNivel;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\PermissaoMatriz;
use App\Modules\Auth\Services\RbacService;
use App\Shared\Exceptions\PermissaoInvalidaException;
use Tests\TestCase;

class RbacServiceTest extends TestCase
{
    public function test_matriz_por_papel(): void
    {
        $rbac = app(RbacService::class);

        $admin = $rbac->getMatrix(Role::ADMIN);
        $this->assertSame(['*'], $admin['permissions']);
        $this->assertSame(0, $admin['code']);

        $recrutando = $rbac->getMatrix(Role::RECRUTANDO);
        $this->assertSame(['catalogo:read'], $recrutando['permissions']);
    }

    public function test_nivel_padrao_admin_edita_recrutando_nenhum_demais_ver(): void
    {
        $this->assertSame(PermissaoNivel::EDITAR, RbacService::defaultNivel(Role::ADMIN));
        $this->assertSame(PermissaoNivel::NENHUM, RbacService::defaultNivel(Role::RECRUTANDO));
        $this->assertSame(PermissaoNivel::VER, RbacService::defaultNivel(Role::BOLSISTA));
    }

    public function test_matriz_completa_tem_40_celulas_e_enums(): void
    {
        $full = app(RbacService::class)->getFullMatrix();

        $this->assertCount(5, $full['roles']);
        $this->assertCount(40, $full['matriz']);
        $this->assertSame(
            ['dashboard', 'rh', 'estoque', 'vendas', 'financeiro', 'producao', 'notificacoes', 'configuracoes'],
            $full['enums']['modulos']
        );
    }

    public function test_update_cell_persiste_e_sobrevive_a_releitura(): void
    {
        $rbac = app(RbacService::class);

        $cell = $rbac->updateCell('estoque', 'BOLSISTA', 'EDITAR');
        $this->assertSame('Editar', $cell['valor']);

        $full = $rbac->getFullMatrix();
        $editadas = array_filter(
            $full['matriz'],
            fn (array $c) => $c['modulo'] === 'estoque' && $c['nivel'] === 'BOLSISTA'
        );
        $this->assertSame('Editar', array_values($editadas)[0]['valor']);

        $row = PermissaoMatriz::where('modulo', 'estoque')->where('role', 1)->first();
        $this->assertSame(PermissaoNivel::EDITAR, $row->nivel);
    }

    public function test_update_cell_aceita_codigo_numerico_e_rejeita_invalido(): void
    {
        $rbac = app(RbacService::class);

        $cell = $rbac->updateCell('rh', '1', 'VER');
        $this->assertSame('BOLSISTA', $cell['nivel']);

        $this->expectException(PermissaoInvalidaException::class);
        $rbac->updateCell('modulo-inexistente', 'ADMIN', 'VER');
    }

    public function test_update_cell_valor_invalido_lanca_excecao(): void
    {
        $this->expectException(PermissaoInvalidaException::class);
        app(RbacService::class)->updateCell('rh', 'ADMIN', 'DESTRUIR');
    }
}

<?php

namespace Tests\Feature\Auth;

use App\Enums\Auth\PermissaoNivel;
use App\Enums\Auth\Role;
use App\Models\Auth\PermissaoMatriz;
use Tests\TestCase;

class PermissoesTest extends TestCase
{
    public function test_matriz_completa_e_edicao_de_celula(): void
    {
        $admin = $this->criarAdmin();
        $headers = $this->authHeader($admin);

        $matriz = $this->getJson('/api/permissoes', $headers)
            ->assertOk()
            ->assertJsonStructure(['roles', 'matriz', 'enums'])
            ->json();

        $this->assertCount(40, $matriz['matriz']);

        $this->putJson('/api/permissoes/estoque/BOLSISTA', ['valor' => 'EDITAR'], $headers)
            ->assertOk()
            ->assertJson(['modulo' => 'estoque', 'nivel' => 'BOLSISTA', 'valor' => 'Editar']);

        $depois = $this->getJson('/api/permissoes', $headers)->assertOk()->json();
        $celula = array_values(array_filter(
            $depois['matriz'],
            fn (array $c) => $c['modulo'] === 'estoque' && $c['nivel'] === 'BOLSISTA'
        ))[0];
        $this->assertSame('Editar', $celula['valor']);
    }

    public function test_celula_invalida_da_422(): void
    {
        $admin = $this->criarAdmin();
        $headers = $this->authHeader($admin);

        $this->putJson('/api/permissoes/invalido/ADMIN', ['valor' => 'VER'], $headers)
            ->assertStatus(422)
            ->assertJson(['status' => 422]);

        $this->putJson('/api/permissoes/rh/ADMIN', ['valor' => 'DESTRUIR'], $headers)
            ->assertStatus(422);
    }

    public function test_admin_nao_pode_ser_reduzido_na_matriz(): void
    {
        $admin = $this->criarAdmin();
        $headers = $this->authHeader($admin);

        $this->putJson('/api/permissoes/rh/ADMIN', ['valor' => 'VER'], $headers)
            ->assertStatus(422);
    }

    public function test_celula_divergente_persistida_nao_reduz_admin_na_leitura(): void
    {
        $admin = $this->criarAdmin();
        $headers = $this->authHeader($admin);

        PermissaoMatriz::query()->where('modulo', 'rh')->where('role', Role::ADMIN->value)->delete();
        PermissaoMatriz::create([
            'modulo' => 'rh',
            'role' => Role::ADMIN,
            'nivel' => PermissaoNivel::NENHUM,
        ]);

        $depois = $this->getJson('/api/permissoes', $headers)->assertOk()->json();
        $celula = array_values(array_filter(
            $depois['matriz'],
            fn (array $c) => $c['modulo'] === 'rh' && $c['nivel'] === 'ADMIN'
        ))[0];

        $this->assertSame('Editar', $celula['valor']);
    }

    public function test_permissoes_exige_admin(): void
    {
        $this->getJson('/api/permissoes')->assertUnauthorized();

        $bolsista = $this->criarLogin();
        $this->comPermissao($bolsista, Role::BOLSISTA);
        $this->getJson('/api/permissoes', $this->authHeader($bolsista, Role::BOLSISTA))
            ->assertForbidden();
    }
}

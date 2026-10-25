<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Enums\Role;
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

    public function test_permissoes_exige_admin(): void
    {
        $this->getJson('/api/permissoes')->assertUnauthorized();

        $bolsista = $this->criarLogin();
        $this->comPermissao($bolsista, Role::BOLSISTA);
        $this->getJson('/api/permissoes', $this->authHeader($bolsista, Role::BOLSISTA))
            ->assertForbidden();
    }
}

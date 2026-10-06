<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Enums\Role;
use Tests\TestCase;

class UsuariosTest extends TestCase
{
    public function test_listar_com_contagens_e_filtros(): void
    {
        $admin = $this->criarAdmin();
        $outro = $this->criarLogin(['email' => 'busca@fablab.org', 'nome_usuario' => 'buscavel']);
        $this->comPermissao($outro, Role::BOLSISTA);

        $res = $this->getJson('/api/usuarios', $this->authHeader($admin))
            ->assertOk()
            ->assertJsonStructure(['content', 'page', 'size', 'total', 'totalPages', 'contagens'])
            ->json();

        $this->assertSame(2, $res['total']);
        $this->assertSame(2, $res['contagens']['ativos']);
        $this->assertSame(1, $res['contagens']['porNivel']['ADMIN']);

        $filtrada = $this->getJson('/api/usuarios?search=busca', $this->authHeader($admin))
            ->assertOk()->json();
        $this->assertSame(1, $filtrada['total']);
        $this->assertSame('buscavel', $filtrada['content'][0]['nomeUsuario']);
    }

    public function test_criar_com_defaults_pendente_recrutando(): void
    {
        $admin = $this->criarAdmin();

        $res = $this->postJson('/api/usuarios', [
            'idUser' => 500,
            'email' => 'novo@fablab.org',
            'nomeUsuario' => 'novo',
            'senha' => 'senha1234',
        ], $this->authHeader($admin))
            ->assertCreated()
            ->assertJson(['situacao' => 'PENDENTE', 'nivel' => 4, 'nivelNome' => 'RECRUTANDO'])
            ->json();

        $this->assertSame('novo@fablab.org', $res['email']);
    }

    public function test_criar_duplicado_da_400(): void
    {
        $admin = $this->criarAdmin();

        $payload = ['idUser' => 501, 'email' => $admin->email, 'nomeUsuario' => 'outro', 'senha' => 'senha1234'];
        $this->postJson('/api/usuarios', $payload, $this->authHeader($admin))->assertStatus(400);
    }

    public function test_atualizar_e_alterar_status(): void
    {
        $admin = $this->criarAdmin();
        $alvo = $this->criarLogin();

        $this->putJson("/api/usuarios/{$alvo->id}", [
            'setor' => 'ESTOQUE', 'nivel' => 1, 'situacao' => 'ATIVO',
        ], $this->authHeader($admin))
            ->assertOk()
            ->assertJson(['setor' => 'ESTOQUE', 'nivel' => 1, 'situacao' => 'ATIVO']);

        $this->patchJson("/api/usuarios/{$alvo->id}/status", [
            'situacao' => 'DESATIVADO',
        ], $this->authHeader($admin))
            ->assertOk()
            ->assertJson(['situacao' => 'DESATIVADO']);

        $this->putJson('/api/usuarios/999999', ['setor' => 'X'], $this->authHeader($admin))
            ->assertNotFound();
    }

    public function test_usuarios_exige_admin(): void
    {
        $this->getJson('/api/usuarios')->assertUnauthorized();

        $bolsista = $this->criarLogin();
        $this->comPermissao($bolsista, Role::BOLSISTA);
        $headers = $this->authHeader($bolsista, Role::BOLSISTA);

        $this->getJson('/api/usuarios', $headers)->assertForbidden();
        $this->postJson('/api/usuarios', [], $headers)->assertForbidden();
    }
}

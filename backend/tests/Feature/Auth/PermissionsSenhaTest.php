<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Enums\Role;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class PermissionsSenhaTest extends TestCase
{
    public function test_permissions_so_admin(): void
    {
        $this->getJson('/api/auth/permissions')->assertUnauthorized();

        $bolsista = $this->criarLogin();
        $this->comPermissao($bolsista, Role::BOLSISTA);
        $this->getJson('/api/auth/permissions', $this->authHeader($bolsista, Role::BOLSISTA))
            ->assertForbidden();

        $admin = $this->criarAdmin();
        $this->getJson('/api/auth/permissions', $this->authHeader($admin))
            ->assertOk()
            ->assertJson(['role' => 'ADMIN', 'code' => 0])
            ->assertJsonStructure(['permissions']);

        $this->getJson('/api/auth/permissions?role=BOLSISTA', $this->authHeader($admin))
            ->assertOk()
            ->assertJson(['role' => 'BOLSISTA', 'code' => 1]);
    }

    public function test_alterar_senha_revoga_token_atual(): void
    {
        $login = $this->criarLogin(['senha_hash' => Hash::make('antiga123')]);
        $this->comPermissao($login, Role::BOLSISTA);
        $headers = $this->authHeader($login, Role::BOLSISTA);

        $this->putJson('/api/auth/senha', [
            'senhaAtual' => 'antiga123',
            'novaSenha' => 'novasenha123',
            'confirmacaoSenha' => 'novasenha123',
        ], $headers)
            ->assertOk()
            ->assertJson(['mensagem' => 'Senha alterada com sucesso']);

        // token atual foi revogado
        $this->getJson('/api/auth/me', $headers)->assertUnauthorized();

        // login com a nova senha funciona
        $this->postJson('/api/auth/login', ['email' => $login->email, 'senha' => 'novasenha123'])
            ->assertOk();
    }

    public function test_alterar_senha_atual_errada_da_401_e_mismatch_da_400(): void
    {
        $login = $this->criarLogin(['senha_hash' => Hash::make('certa1234')]);
        $this->comPermissao($login, Role::BOLSISTA);
        $headers = $this->authHeader($login, Role::BOLSISTA);

        $this->putJson('/api/auth/senha', [
            'senhaAtual' => 'errada1234',
            'novaSenha' => 'novasenha123',
            'confirmacaoSenha' => 'novasenha123',
        ], $headers)->assertUnauthorized();

        $this->putJson('/api/auth/senha', [
            'senhaAtual' => 'certa1234',
            'novaSenha' => 'novasenha123',
            'confirmacaoSenha' => 'outra12345',
        ], $headers)->assertStatus(400);
    }
}

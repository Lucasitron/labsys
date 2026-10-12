<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class LoginTest extends TestCase
{
    public function test_login_por_email_retorna_par_de_tokens(): void
    {
        $login = $this->criarLogin(['senha_hash' => Hash::make('senha123')]);
        $this->comPermissao($login, Role::BOLSISTA);

        $res = $this->postJson('/api/auth/login', [
            'email' => $login->email,
            'senha' => 'senha123',
        ]);

        $res->assertOk()
            ->assertJsonStructure(['accessToken', 'refreshToken', 'tokenType', 'expiresIn', 'idUser', 'role', 'setor', 'nomeUsuario'])
            ->assertJson(['tokenType' => 'Bearer', 'expiresIn' => 900, 'role' => 'BOLSISTA']);
    }

    public function test_login_por_nome_usuario(): void
    {
        $login = $this->criarAdmin();
        // troca a senha conhecida do helper
        $login->senha_hash = Hash::make('admin123');
        $login->save();

        $this->postJson('/api/auth/login', [
            'nomeUsuario' => $login->nome_usuario,
            'senha' => 'admin123',
        ])->assertOk()->assertJson(['role' => 'ADMIN']);
    }

    public function test_credencial_invalida_da_401(): void
    {
        $login = $this->criarLogin();
        $this->comPermissao($login);

        $this->postJson('/api/auth/login', ['email' => $login->email, 'senha' => 'errada!!'])
            ->assertUnauthorized()
            ->assertJson(['status' => 401]);

        $this->postJson('/api/auth/login', ['email' => 'ninguem@fablab.org', 'senha' => 'senha123'])
            ->assertUnauthorized();
    }

    public function test_sem_permissao_ativa_da_403(): void
    {
        $login = $this->criarLogin();

        $this->postJson('/api/auth/login', ['email' => $login->email, 'senha' => 'senha123'])
            ->assertForbidden()
            ->assertJson(['status' => 403]);
    }

    public function test_conta_nao_ativa_da_403(): void
    {
        $login = $this->criarLogin(['situacao' => SituacaoUsuario::PENDENTE]);
        $this->comPermissao($login);

        $this->postJson('/api/auth/login', ['email' => $login->email, 'senha' => 'senha123'])
            ->assertForbidden();
    }

    public function test_validacao_rejeita_antes_do_service(): void
    {
        $this->postJson('/api/auth/login', ['email' => 'a@b.org'])
            ->assertStatus(422);

        $this->postJson('/api/auth/login', ['email' => 'a@b.org', 'senha' => 'curta'])
            ->assertStatus(422);
    }
}

<?php

namespace Tests\Feature\Auth;

use Tests\TestCase;

class LogoutRefreshTest extends TestCase
{
    public function test_logout_revoga_e_me_falha_em_seguida(): void
    {
        $login = $this->criarAdmin();
        $headers = $this->authHeader($login);

        $this->postJson('/api/auth/logout', [], $headers)
            ->assertOk()
            ->assertJson(['message' => 'Logout realizado com sucesso']);

        $this->getJson('/api/auth/me', $headers)->assertUnauthorized();
    }

    public function test_logout_sem_token_da_401(): void
    {
        // sem Bearer e sem body: auth:api barra antes do service
        $this->postJson('/api/auth/logout')->assertUnauthorized();

        $admin = $this->criarAdmin();
        $this->postJson('/api/auth/logout', [], $this->authHeader($admin))
            ->assertOk();
    }

    public function test_logout_via_body_token(): void
    {
        $login = $this->criarAdmin();
        $token = $this->tokenPara($login);

        $this->postJson('/api/auth/logout', ['token' => $token], $this->authHeader($login))
            ->assertOk();

        $this->getJson('/api/auth/me', ['Authorization' => 'Bearer '.$token])
            ->assertUnauthorized();
    }

    public function test_refresh_rotaciona_e_invalida_o_antigo(): void
    {
        $login = $this->criarAdmin();
        $tokens = $this->postJson('/api/auth/login', [
            'email' => $login->email, 'senha' => 'senha123',
        ])->assertOk()->json();

        $novo = $this->postJson('/api/auth/refresh', ['refreshToken' => $tokens['refreshToken']])
            ->assertOk()
            ->assertJsonStructure(['accessToken', 'refreshToken', 'tokenType', 'expiresIn'])
            ->json();

        $this->assertNotSame($tokens['refreshToken'], $novo['refreshToken']);

        // antigo foi para a blacklist
        $this->postJson('/api/auth/refresh', ['refreshToken' => $tokens['refreshToken']])
            ->assertUnauthorized();

        // novo access funciona
        $this->getJson('/api/auth/me', ['Authorization' => 'Bearer '.$novo['accessToken']])
            ->assertOk();
    }

    public function test_refresh_com_access_token_e_rejeitado(): void
    {
        $login = $this->criarAdmin();

        $this->postJson('/api/auth/refresh', ['refreshToken' => $this->tokenPara($login)])
            ->assertUnauthorized();
    }

    public function test_me_retorna_dados_e_permissoes(): void
    {
        $login = $this->criarAdmin();

        $this->getJson('/api/auth/me', $this->authHeader($login))
            ->assertOk()
            ->assertJsonStructure(['id', 'idUser', 'email', 'nomeUsuario', 'setor', 'permissions'])
            ->assertJson(['email' => $login->email]);
    }

    public function test_me_sem_token_da_401(): void
    {
        $this->getJson('/api/auth/me')->assertUnauthorized();
    }
}

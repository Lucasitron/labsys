<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Enums\Role;
use Tests\TestCase;

/** Slice de segurança: 401 sem token e 403 com papel ≠ ADMIN por rota sensível. */
class SecuritySliceTest extends TestCase
{
    /** @return array<string, array{0:string,1:string}> */
    public static function rotasSensiveis(): array
    {
        return [
            'me' => ['GET', '/api/auth/me'],
            'permissions' => ['GET', '/api/auth/permissions'],
            'senha' => ['PUT', '/api/auth/senha'],
            'usuarios list' => ['GET', '/api/usuarios'],
            'usuarios create' => ['POST', '/api/usuarios'],
            'usuarios update' => ['PUT', '/api/usuarios/1'],
            'usuarios status' => ['PATCH', '/api/usuarios/1/status'],
            'permissoes list' => ['GET', '/api/permissoes'],
            'permissoes update' => ['PUT', '/api/permissoes/rh/ADMIN'],
            'cfg sistema get' => ['GET', '/api/configuracoes/sistema'],
            'cfg sistema put' => ['PUT', '/api/configuracoes/sistema'],
            'tokens list' => ['GET', '/api/configuracoes/tokens'],
            'tokens create' => ['POST', '/api/configuracoes/tokens'],
            'tokens delete' => ['DELETE', '/api/configuracoes/tokens/1'],
        ];
    }

    /** @dataProvider rotasSensiveis */
    public function test_sem_token_da_401(string $method, string $uri): void
    {
        $this->json($method, $uri)->assertUnauthorized();
    }

    /** @dataProvider rotasSensiveis */
    public function test_papel_nao_admin_da_403(string $method, string $uri): void
    {
        // me/senha são qualquer-autenticado (sem can:admin): não dão 403.
        if (in_array($uri, ['/api/auth/me', '/api/auth/senha'], true)) {
            $this->markTestSkipped('rota autenticada, não admin-only');
        }

        $bolsista = $this->criarLogin();
        $this->comPermissao($bolsista, Role::BOLSISTA);

        $this->json($method, $uri, [], $this->authHeader($bolsista, Role::BOLSISTA))
            ->assertForbidden();
    }

    public function test_rotas_autenticadas_nao_admin_passam_do_gate(): void
    {
        $bolsista = $this->criarLogin();
        $this->comPermissao($bolsista, Role::BOLSISTA);
        $headers = $this->authHeader($bolsista, Role::BOLSISTA);

        // me: qualquer autenticado
        $this->getJson('/api/auth/me', $headers)->assertOk();

        // senha: qualquer autenticado (422 aqui é validação, não 403)
        $this->putJson('/api/auth/senha', [], $headers)->assertStatus(422);
    }

    /** @dataProvider rotasSensiveis */
    public function test_recrutando_da_403(string $method, string $uri): void
    {
        $recrutando = $this->criarLogin();
        $this->comPermissao($recrutando, Role::RECRUTANDO);

        // me/senha são qualquer-autenticado: recrutando passa (401 não, 403 não)
        if (in_array($uri, ['/api/auth/me', '/api/auth/senha'], true)) {
            $this->markTestSkipped('rota autenticada, não admin-only');
        }

        $this->json($method, $uri, [], $this->authHeader($recrutando, Role::RECRUTANDO))
            ->assertForbidden();
    }
}

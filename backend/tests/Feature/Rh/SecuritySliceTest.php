<?php

namespace Tests\Feature\Rh;

use App\Modules\Auth\Enums\Role;
use Tests\TestCase;

/** Slice de segurança RH: 401 sem token e 403 com papel ≠ ADMIN por rota Admin-only. */
class SecuritySliceTest extends TestCase
{
    /** @return array<string, array{0:string,1:string}> */
    public static function rotasAdmin(): array
    {
        return [
            'pessoas delete' => ['DELETE', '/api/rh/pessoas/1'],
            'funcionarios list' => ['GET', '/api/rh/funcionarios'],
            'funcionarios create' => ['POST', '/api/rh/funcionarios'],
            'funcionarios nivel' => ['PUT', '/api/rh/funcionarios/1/nivel'],
            'niveis matriz' => ['GET', '/api/rh/niveis'],
            'niveis membros' => ['PATCH', '/api/rh/niveis/1/membros'],
            'niveis convites' => ['POST', '/api/rh/niveis/convites'],
            'cert aprovar' => ['PUT', '/api/rh/certificados/solicitacoes/1/aprovar'],
            'cert rejeitar' => ['PUT', '/api/rh/certificados/solicitacoes/1/rejeitar'],
        ];
    }

    /** @dataProvider rotasAdmin */
    public function test_sem_token_da_401(string $method, string $uri): void
    {
        $this->json($method, $uri)->assertUnauthorized();
    }

    /** @dataProvider rotasAdmin */
    public function test_papel_nao_admin_da_403(string $method, string $uri): void
    {
        $bolsista = $this->criarFuncionario(null, Role::BOLSISTA);

        $this->json($method, $uri, [], $this->authHeader($bolsista['login'], Role::BOLSISTA))
            ->assertForbidden();
    }

    /** @dataProvider rotasAdmin */
    public function test_recrutando_da_403(string $method, string $uri): void
    {
        $recrutando = $this->criarFuncionario(null, Role::RECRUTANDO);

        $this->json($method, $uri, [], $this->authHeader($recrutando['login'], Role::RECRUTANDO))
            ->assertForbidden();
    }

    public function test_rotas_proprias_passam_para_nao_admin_com_vinculo(): void
    {
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $headers = $this->authHeader($membro['login'], Role::BOLSISTA);

        $this->getJson('/api/rh/horas/disponiveis', $headers)->assertOk();
        $this->getJson('/api/rh/certificados/emitidos', $headers)->assertOk();
        $this->getJson('/api/rh/extrato-mensal-horas', $headers)->assertOk();
    }
}

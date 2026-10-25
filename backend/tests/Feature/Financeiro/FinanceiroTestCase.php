<?php

namespace Tests\Feature\Financeiro;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Modules\Financeiro\Models\CategoriaFinanceira;
use Tests\TestCase;

/** Base dos testes do Financeiro: Admin + fábricas mínimas do módulo. */
abstract class FinanceiroTestCase extends TestCase
{
    protected function loginPapel(Role $role): Login
    {
        $login = $this->criarLogin();
        $this->comPermissao($login, $role);

        return $login;
    }

    /** @return array<string, string> */
    protected function headersPapel(Role $role): array
    {
        return $this->authHeader($this->loginPapel($role), $role);
    }

    /** @return array<string, string> */
    protected function headersAdmin(): array
    {
        $admin = $this->criarAdmin();

        return $this->authHeader($admin, Role::ADMIN);
    }

    protected function criarCategoria(array $over = []): CategoriaFinanceira
    {
        $this->seq++;

        return CategoriaFinanceira::create(array_merge([
            'nome' => "Categoria {$this->seq}",
            'tipo' => 'DESPESA',
        ], $over));
    }

    /** @return array<string, mixed> */
    protected function lancamentoPayload(int $categoriaId, array $over = []): array
    {
        return array_merge([
            'idCategoria' => $categoriaId,
            'tipo' => 'SAIDA',
            'valor' => '100.00',
            'dataVencimento' => today()->addDays(5)->toDateString(),
        ], $over);
    }

    /** @return array<string, mixed> */
    protected function fechamentoPayload(int $encomenda, array $over = []): array
    {
        return array_merge([
            'idEncomenda' => $encomenda,
            'horasEstimadas' => '10.00',
            'valorFechado' => '1000.00',
        ], $over);
    }
}

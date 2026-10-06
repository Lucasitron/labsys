<?php

namespace Tests\Feature\Estoque;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\Enums\Categoria;
use App\Modules\Estoque\Models\Fornecedor;
use App\Modules\Estoque\Models\Item;
use Tests\TestCase;

/** Base dos testes do Estoque: papéis/vínculos + fábricas mínimas. */
abstract class EstoqueTestCase extends TestCase
{
    protected function loginPapel(Role $role, string $setor = 'PRODUCAO'): Login
    {
        $login = $this->criarLogin(['setor' => $setor]);
        $this->comPermissao($login, $role);

        return $login;
    }

    /** @return array<string, string> */
    protected function headersPapel(Role $role, string $setor = 'PRODUCAO'): array
    {
        return $this->authHeader($this->loginPapel($role, $setor), $role);
    }

    /** @return array{login:Login,headers:array<string,string>} */
    protected function responsavel(Role $role = Role::BOLSISTA): array
    {
        $login = $this->loginPapel($role, 'ESTOQUE');

        return ['login' => $login, 'headers' => $this->authHeader($login, $role)];
    }

    /** @return array<string, string> */
    protected function headersAdmin(): array
    {
        $admin = $this->criarAdmin();

        return $this->authHeader($admin, Role::ADMIN);
    }

    protected function criarItem(array $over = []): Item
    {
        return Item::create(array_merge([
            'nome' => 'Parafuso M4',
            'descricao' => 'Parafuso de bancada',
            'categoria' => Categoria::INSUMO,
            'unidade_medida' => 'UN',
            'quantidade_atual' => '100.00',
            'estoque_minimo' => '10.00',
            'versao' => 0,
        ], $over));
    }

    protected function criarFornecedor(array $over = []): Fornecedor
    {
        return Fornecedor::create(array_merge([
            'nome' => 'Ferragens SA',
            'contato' => 'contato@ferragens.test',
            'cnpj' => '12345678000199',
        ], $over));
    }

    /** @return array<string, mixed> */
    protected function itemPayload(array $over = []): array
    {
        return array_merge([
            'nome' => 'Chapa de MDF',
            'descricao' => 'Chapa 15mm',
            'categoria' => 'INSUMO',
            'unidadeMedida' => 'UN',
            'quantidadeAtual' => '50.00',
            'estoqueMinimo' => '5.00',
        ], $over);
    }
}

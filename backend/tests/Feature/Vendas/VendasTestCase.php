<?php

namespace Tests\Feature\Vendas;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\Enums\Categoria;
use App\Modules\Estoque\Models\Item;
use App\Modules\Vendas\Models\Cliente;
use App\Modules\Vendas\Models\TagCliente;
use Tests\TestCase;

/** Base dos testes do Vendas: papéis + fábricas mínimas + documentos válidos. */
abstract class VendasTestCase extends TestCase
{
    public const CPF_VALIDO = '52998224725';

    public const CNPJ_VALIDO = '11444777000161';

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

    /** @return array<string, mixed> */
    protected function clientePayload(array $over = []): array
    {
        $this->seq++;

        return array_merge([
            'tipoPessoa' => 'PF',
            'nomeRazaoSocial' => "Cliente {$this->seq}",
            'cpfCnpj' => self::CPF_VALIDO,
            'email' => "cliente{$this->seq}@teste.org",
            'telefone' => '62999990000',
            'endereco' => 'Rua Teste, 1',
        ], $over);
    }

    protected function criarCliente(array $over = []): Cliente
    {
        $this->seq++;

        return Cliente::create(array_merge([
            'tipo_pessoa' => 'PF',
            'nome_razao_social' => "Cliente {$this->seq}",
            'cpf_cnpj' => self::CPF_VALIDO,
            'email' => "cliente{$this->seq}@teste.org",
            'data_cadastro' => today()->toDateString(),
            'criado_por' => 1001,
        ], $over));
    }

    protected function criarTag(array $over = []): TagCliente
    {
        $this->seq++;

        return TagCliente::create(array_merge([
            'nome' => "TAG-{$this->seq}",
            'cor' => '#fff',
        ], $over));
    }

    /** @return array<string, mixed> */
    protected function itemOrcamentoPayload(array $over = []): array
    {
        return array_merge([
            'descricao' => 'Corte a laser',
            'quantidade' => '2.00',
            'valorUnitario' => '50.00',
        ], $over);
    }

    /** @return array<string, mixed> */
    protected function orcamentoPayload(int $clienteId, array $over = []): array
    {
        return array_merge([
            'clienteId' => $clienteId,
            'validade' => today()->addMonth()->toDateString(),
            'observacoes' => 'Orçamento teste',
            'itens' => [$this->itemOrcamentoPayload()],
        ], $over);
    }

    protected function criarItemEstoque(): Item
    {
        return Item::create([
            'nome' => 'Filamento PLA',
            'categoria' => Categoria::INSUMO,
            'unidade_medida' => 'KG',
            'quantidade_atual' => '100.00',
            'estoque_minimo' => '10.00',
            'versao' => 0,
        ]);
    }
}

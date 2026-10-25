<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\Enums\Categoria;
use App\Modules\Estoque\Models\Item;
use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Models\EncomendaKanban;
use App\Modules\Producao\Models\Projeto;
use Tests\TestCase;

/** Base dos testes do Producao: papéis, CPF válido, fábricas mínimas e fluxo Vendas. */
abstract class ProducaoTestCase extends TestCase
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
        return $this->authHeader($this->criarAdmin(), Role::ADMIN);
    }

    /** CPF válido (dígitos verificadores reais) p/ criar clientes do Vendas. */
    protected function cpfValido(): string
    {
        $this->seq++;
        $base = str_pad((string) (100000001 + (($this->seq * 7919) % 899999998)), 9, '0', STR_PAD_LEFT);
        if (count(array_unique(str_split($base))) === 1) {
            $base = '123456789';
        }
        $d = array_map('intval', str_split($base));
        $s = 0;
        foreach ($d as $i => $v) {
            $s += $v * (10 - $i);
        }
        $r = $s % 11;
        $d[] = $r < 2 ? 0 : 11 - $r;
        $s = 0;
        foreach ($d as $i => $v) {
            $s += $v * (11 - $i);
        }
        $r = $s % 11;
        $d[] = $r < 2 ? 0 : 11 - $r;

        return implode('', $d);
    }

    /** Encomenda real do Vendas (cliente→orçamento→aprovar→converter). */
    protected function criarEncomendaVendas(array $headers): int
    {
        $this->seq++;
        $clienteId = $this->postJson('/api/vendas/clientes', [
            'tipoPessoa' => 'PF',
            'nomeRazaoSocial' => "Cliente {$this->seq}",
            'cpfCnpj' => $this->cpfValido(),
            'email' => "cliprod{$this->seq}@teste.org",
            'telefone' => '62999990000',
        ], $headers)->assertCreated()->json('id');

        $orcId = $this->postJson('/api/vendas/orcamentos', [
            'clienteId' => $clienteId,
            'validade' => today()->addMonth()->toDateString(),
            'itens' => [[
                'descricao' => 'Corte a laser',
                'quantidade' => '2.00',
                'valorUnitario' => '50.00',
            ]],
        ], $headers)->assertCreated()->json('id');

        $this->putJson("/api/vendas/orcamentos/{$orcId}", ['status' => 'Aprovado'], $headers)->assertOk();

        return $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)
            ->assertCreated()->json('id');
    }

    protected function criarItemEstoque(string $quantidade = '100.00'): Item
    {
        $this->seq++;

        return Item::create([
            'nome' => "Filamento {$this->seq}",
            'categoria' => Categoria::INSUMO,
            'unidade_medida' => 'KG',
            'quantidade_atual' => $quantidade,
            'estoque_minimo' => '10.00',
            'versao' => 0,
        ]);
    }

    protected function criarProjeto(int $idResponsavel, array $over = []): Projeto
    {
        return Projeto::create(array_merge([
            'nome' => 'Mesa CNC',
            'descricao' => 'Construir mesa',
            'data_inicio' => today()->toDateString(),
            'status' => 'PLANEJADO',
            'id_responsavel' => $idResponsavel,
        ], $over));
    }

    protected function criarCartao(int $idEncomenda, ?int $idResponsavel = null): EncomendaKanban
    {
        return EncomendaKanban::create([
            'id_encomenda' => $idEncomenda,
            'id_responsavel' => $idResponsavel,
            'status' => KanbanStatus::FILA->value,
            'data_entrada_status' => now()->toDateTimeString(),
            'ordem' => 0,
            'version' => 0,
        ]);
    }
}

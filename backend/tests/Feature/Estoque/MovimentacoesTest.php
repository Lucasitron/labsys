<?php

namespace Tests\Feature\Estoque;

use App\Modules\Auth\Enums\Role;
use App\Modules\Estoque\Events\CompraSolicitadaEvent;
use App\Modules\Estoque\Events\EstoqueBaixoEvent;
use App\Modules\Estoque\Models\EntradaEstoque;
use App\Modules\Estoque\Models\Item;
use Illuminate\Support\Facades\Event;

class MovimentacoesTest extends EstoqueTestCase
{
    public function test_entrada_soma_saldo_calcula_total_e_publica_compra(): void
    {
        Event::fake([CompraSolicitadaEvent::class, EstoqueBaixoEvent::class]);

        ['headers' => $headers] = $this->responsavel();
        $item = $this->criarItem(['quantidade_atual' => '10.00', 'estoque_minimo' => '5.00']);
        $fornecedor = $this->criarFornecedor();

        $this->postJson('/api/estoque/entradas', [
            'idItem' => $item->getKey(), 'idFornecedor' => $fornecedor->getKey(),
            'quantidade' => '4.00', 'valorUnitario' => '2.50',
            'notaFiscal' => 'NF-1', 'responsavel' => 'João',
        ], $headers)
            ->assertCreated()
            ->assertJsonPath('valorTotal', '10.00')
            ->assertJsonPath('nomeItem', 'Parafuso M4')
            ->assertJsonPath('nomeFornecedor', 'Ferragens SA')
            ->assertJsonPath('responsavel', 'João');

        $this->assertSame('14.00', Item::find($item->getKey())->quantidade_atual);

        Event::assertDispatched(CompraSolicitadaEvent::class, fn ($e) => $e->payload()['valorTotal'] === '10.00');
        Event::assertNotDispatched(EstoqueBaixoEvent::class);
    }

    public function test_entrada_dispara_estoque_baixo_quando_no_minimo(): void
    {
        Event::fake([CompraSolicitadaEvent::class, EstoqueBaixoEvent::class]);

        ['headers' => $headers] = $this->responsavel();
        $item = $this->criarItem(['quantidade_atual' => '0.00', 'estoque_minimo' => '5.00']);
        $fornecedor = $this->criarFornecedor();

        $this->postJson('/api/estoque/entradas', [
            'idItem' => $item->getKey(), 'idFornecedor' => $fornecedor->getKey(),
            'quantidade' => '1.00', 'valorUnitario' => '1.00',
        ], $headers)->assertCreated();

        Event::assertDispatched(EstoqueBaixoEvent::class, fn ($e) => $e->payload()['idItem'] === (int) $item->getKey());
    }

    public function test_saida_subtrai_e_negativa_da_409_com_saldo_intacto(): void
    {
        ['headers' => $headers] = $this->responsavel();
        $item = $this->criarItem(['quantidade_atual' => '10.00']);

        $this->postJson('/api/estoque/saidas', [
            'idItem' => $item->getKey(), 'quantidade' => '4.00', 'tipoSaida' => 'PERDA',
        ], $headers)->assertCreated()->assertJsonPath('tipoSaida', 'PERDA');

        $this->assertSame('6.00', Item::find($item->getKey())->quantidade_atual);

        $this->postJson('/api/estoque/saidas', [
            'idItem' => $item->getKey(), 'quantidade' => '7.00', 'tipoSaida' => 'CONSUMO',
        ], $headers)->assertStatus(409);

        $this->assertSame('6.00', Item::find($item->getKey())->quantidade_atual);
    }

    public function test_listar_filtra_por_item_e_ignora_status_e1(): void
    {
        $headers = $this->headersAdmin();
        $a = $this->criarItem();
        $b = $this->criarItem(['nome' => 'Outro']);
        $fornecedor = $this->criarFornecedor();

        foreach ([$a, $b] as $item) {
            EntradaEstoque::create([
                'id_item' => $item->getKey(), 'id_fornecedor' => $fornecedor->getKey(),
                'quantidade' => '1.00', 'valor_unitario' => '1.00', 'valor_total' => '1.00',
                'data_entrada' => '2026-01-05',
            ]);
        }

        $this->getJson("/api/estoque/entradas?idItem={$a->getKey()}", $headers)
            ->assertOk()->assertJsonCount(1);
        // Sem ?status= no contrato: parâmetro desconhecido não filtra nem quebra.
        $this->getJson('/api/estoque/entradas?status=ativos', $headers)
            ->assertOk()->assertJsonCount(2);
        $this->getJson('/api/estoque/saidas?status=ativos', $headers)->assertOk();

        $id = EntradaEstoque::first()->getKey();
        $this->getJson("/api/estoque/entradas/{$id}", $headers)->assertOk();
        $this->getJson('/api/estoque/entradas/999999', $headers)->assertNotFound();
    }

    public function test_fk_inexistente_da_404_no_service(): void
    {
        $headers = $this->headersAdmin();
        $fornecedor = $this->criarFornecedor();

        $this->postJson('/api/estoque/entradas', [
            'idItem' => 999999, 'idFornecedor' => $fornecedor->getKey(),
            'quantidade' => '1.00', 'valorUnitario' => '1.00',
        ], $headers)->assertNotFound();

        $item = $this->criarItem();
        $this->postJson('/api/estoque/entradas', [
            'idItem' => $item->getKey(), 'idFornecedor' => 999999,
            'quantidade' => '1.00', 'valorUnitario' => '1.00',
        ], $headers)->assertNotFound();

        $this->postJson('/api/estoque/saidas', [
            'idItem' => 999999, 'quantidade' => '1.00', 'tipoSaida' => 'PERDA',
        ], $headers)->assertNotFound();
    }

    public function test_saida_detalhe_e_404(): void
    {
        $headers = $this->headersAdmin();
        $item = $this->criarItem();

        $saida = \App\Modules\Estoque\Models\SaidaEstoque::create([
            'id_item' => $item->getKey(), 'quantidade' => '2.00',
            'tipo_saida' => \App\Modules\Estoque\Enums\TipoSaida::AJUSTE,
            'data_saida' => now(),
        ]);

        $this->getJson("/api/estoque/saidas/{$saida->getKey()}", $headers)
            ->assertOk()->assertJsonPath('nomeItem', 'Parafuso M4');
        $this->getJson('/api/estoque/saidas/999999', $headers)->assertNotFound();
        $this->getJson('/api/estoque/boms/999999', $headers)->assertNotFound();
    }

    public function test_fornecedor_localizacao_escrita_so_admin(): void
    {
        $admin = $this->headersAdmin();
        ['headers' => $bolsista] = $this->responsavel();

        $this->postJson('/api/estoque/fornecedores', ['nome' => 'F1'], $bolsista)->assertForbidden();
        $this->postJson('/api/estoque/localizacoes', ['armario' => 'A'], $bolsista)->assertForbidden();

        $this->postJson('/api/estoque/fornecedores', ['nome' => 'F1'], $admin)
            ->assertCreated()->assertJsonPath('nome', 'F1');
        $this->postJson('/api/estoque/localizacoes', ['armario' => 'A'], $admin)->assertCreated();

        // Leitura: bolsista vê, recrutando não.
        $this->getJson('/api/estoque/fornecedores', $bolsista)->assertOk()->assertJsonCount(1);
        $this->getJson('/api/estoque/localizacoes', $bolsista)->assertOk()->assertJsonCount(1);
        $this->getJson('/api/estoque/fornecedores', $this->headersPapel(Role::RECRUTANDO))->assertForbidden();
        $this->getJson('/api/estoque/fornecedores')->assertUnauthorized();
    }
}

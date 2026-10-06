<?php

namespace Tests\Feature\Estoque;

use App\Modules\Auth\Enums\Role;
use App\Modules\Estoque\Contracts\EstoqueContract;
use App\Modules\Estoque\Events\ProducaoConcluidaEvent;
use App\Modules\Estoque\Models\Item;
use App\Modules\Estoque\Models\ListaMateriais;
use App\Modules\Estoque\Models\SaidaEstoque;
use App\Modules\Rh\Contracts\RhContract;

class BomTest extends EstoqueTestCase
{
    public function test_criar_versao_default_listar_e_buscar(): void
    {
        $headers = $this->headersAdmin();
        $a = $this->criarItem();
        $b = $this->criarItem(['nome' => 'Porca M4']);

        $id = $this->postJson('/api/estoque/boms', [
            'idProdutoServico' => 11, 'nome' => 'BOM Mesa',
            'itens' => [
                ['idItem' => $a->getKey(), 'quantidadePrevista' => '4.00'],
                ['idItem' => $b->getKey(), 'quantidadePrevista' => '8.00'],
            ],
        ], $headers)
            ->assertCreated()
            ->assertJsonPath('versao', 1)
            ->assertJsonPath('editavel', true)
            ->json('id');

        $this->getJson('/api/estoque/boms?projetoId=11', $headers)->assertOk()->assertJsonCount(1);
        $this->getJson('/api/estoque/boms?projetoId=99', $headers)->assertOk()->assertJsonCount(0);
        $this->getJson('/api/estoque/boms', $headers)->assertOk()->assertJsonCount(1);

        $this->getJson("/api/estoque/boms/{$id}", $headers)
            ->assertOk()
            ->assertJsonCount(2, 'itens');

        // Relações do ItemBom (bom ↔ item).
        $itemBom = \App\Modules\Estoque\Models\ItemBom::where('id_bom', $id)->first();
        $this->assertSame('BOM Mesa', $itemBom->bom->nome);
        $this->assertNotNull($itemBom->item->nome);
    }

    public function test_atualizar_incrementa_versao_e_reconcilia_itens(): void
    {
        $headers = $this->headersAdmin();
        $a = $this->criarItem();
        $b = $this->criarItem(['nome' => 'B']);
        $c = $this->criarItem(['nome' => 'C']);

        $id = $this->postJson('/api/estoque/boms', [
            'idProdutoServico' => 5, 'nome' => 'BOM',
            'itens' => [
                ['idItem' => $a->getKey(), 'quantidadePrevista' => '1.00'],
                ['idItem' => $b->getKey(), 'quantidadePrevista' => '2.00'],
            ],
        ], $headers)->json('id');

        $this->putJson("/api/estoque/boms/{$id}", [
            'idProdutoServico' => 5, 'nome' => 'BOM v2',
            'itens' => [
                ['idItem' => $a->getKey(), 'quantidadePrevista' => '10.00'],
                ['idItem' => $c->getKey(), 'quantidadePrevista' => '3.00'],
            ],
        ], $headers)
            ->assertOk()
            ->assertJsonPath('versao', 2)
            ->assertJsonCount(2, 'itens');

        $bom = ListaMateriais::with('itens')->find($id);
        $this->assertSame(2, (int) $bom->versao);
        $this->assertSame('10.00', (string) $bom->itens->firstWhere('id_item', $a->getKey())->quantidade_prevista);
        $this->assertNull($bom->itens->firstWhere('id_item', $b->getKey()));
    }

    public function test_consumo_baixa_exato_e_item_estranho_nao_baixa_nada(): void
    {
        $headers = $this->headersAdmin();
        $a = $this->criarItem(['quantidade_atual' => '20.00']);
        $estranho = $this->criarItem(['nome' => 'Estranho', 'quantidade_atual' => '20.00']);

        $id = $this->postJson('/api/estoque/boms', [
            'idProdutoServico' => 7, 'nome' => 'BOM',
            'itens' => [['idItem' => $a->getKey(), 'quantidadePrevista' => '5.00']],
        ], $headers)->json('id');

        $this->postJson("/api/estoque/boms/{$id}/consumo", [
            'itens' => [['idItem' => $a->getKey(), 'quantidadeConsumida' => '6.00']],
        ], $headers)
            ->assertOk()
            ->assertJsonPath('itens.0.quantidadeReal', '6.00');

        $this->assertSame('14.00', Item::find($a->getKey())->quantidade_atual);
        $this->assertTrue(SaidaEstoque::where('tipo_saida', 'CONSUMO')
            ->where('id_referencia', $id)->exists());

        // Item estranho → 400 e nenhuma baixa parcial.
        $this->postJson("/api/estoque/boms/{$id}/consumo", [
            'itens' => [
                ['idItem' => $a->getKey(), 'quantidadeConsumida' => '1.00'],
                ['idItem' => $estranho->getKey(), 'quantidadeConsumida' => '1.00'],
            ],
        ], $headers)->assertStatus(400);

        $this->assertSame('14.00', Item::find($a->getKey())->quantidade_atual);
        $this->assertSame('20.00', Item::find($estranho->getKey())->quantidade_atual);
    }

    public function test_bom_escrita_bolsista_com_vinculo_voluntario_nem_com_vinculo(): void
    {
        $item = $this->criarItem();
        $payload = [
            'idProdutoServico' => 3, 'nome' => 'BOM',
            'itens' => [['idItem' => $item->getKey(), 'quantidadePrevista' => '1.00']],
        ];

        $this->postJson('/api/estoque/boms', $payload, $this->responsavel(Role::BOLSISTA)['headers'])
            ->assertCreated();
        $this->postJson('/api/estoque/boms', $payload, $this->responsavel(Role::VOLUNTARIO)['headers'])
            ->assertForbidden();
        $this->postJson('/api/estoque/boms', $payload, $this->headersPapel(Role::BOLSISTA))
            ->assertForbidden();
    }

    public function test_listener_producao_concluida_baixa_idempotente(): void
    {
        $item = $this->criarItem(['quantidade_atual' => '30.00']);

        $evento = new ProducaoConcluidaEvent(42, 7, [
            ['idItem' => (int) $item->getKey(), 'quantidadeConsumida' => '5.00'],
        ]);

        event($evento);
        $this->assertSame('25.00', Item::find($item->getKey())->quantidade_atual);

        // Duplo dispatch = 1 baixa.
        event($evento);
        $this->assertSame('25.00', Item::find($item->getKey())->quantidade_atual);
        $this->assertSame(1, SaidaEstoque::where('tipo_saida', 'CONSUMO')
            ->where('id_referencia', 42)->count());

        // Itens vazios = no-op.
        event(new ProducaoConcluidaEvent(43, 7, []));
        $this->assertSame(0, SaidaEstoque::where('id_referencia', 43)->count());

        $this->assertSame('producao.concluida.event', ProducaoConcluidaEvent::NAME);
        $this->assertSame(42, $evento->payload()['idEncomenda']);
    }

    public function test_estoque_contract_tres_metodos_e_rh_nome_pessoa(): void
    {
        $item = $this->criarItem(['quantidade_atual' => '12.00']);
        $contract = app(EstoqueContract::class);

        $this->assertTrue($contract->itemExiste((int) $item->getKey()));
        $this->assertFalse($contract->itemExiste(999999));
        $this->assertSame('12.00', $contract->saldoDe((int) $item->getKey()));
        $this->assertNull($contract->saldoDe(999999));

        $ids = $contract->baixarConsumo(
            [['idItem' => (int) $item->getKey(), 'quantidadeConsumida' => '2.00']],
            77,
        );
        $this->assertCount(1, $ids);
        $this->assertSame([], $contract->baixarConsumo(
            [['idItem' => (int) $item->getKey(), 'quantidadeConsumida' => '2.00']],
            77,
        ));
        $this->assertSame('10.00', Item::find($item->getKey())->quantidade_atual);

        $rh = app(RhContract::class);
        $pessoa = $this->criarPessoa(['nome_completo' => 'João Teste']);
        $this->assertSame('João Teste', $rh->nomePessoa((int) $pessoa->getKey()));
        $this->assertNull($rh->nomePessoa(999999));
    }
}

<?php

namespace Tests\Feature\Vendas;

use App\Modules\Vendas\Models\Orcamento;
use Illuminate\Support\Facades\Event;
use App\Modules\Vendas\Events\OrcamentoAprovadoEvent;

/** Orçamentos: total, ajustes, aprovação+evento, duplicar, trava pós-conversão, estoque. */
class OrcamentosTest extends VendasTestCase
{
    public function test_criar_calcula_total_no_service(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');

        $res = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId, [
            'itens' => [
                $this->itemOrcamentoPayload(['quantidade' => '2.00', 'valorUnitario' => '50.00']),
                $this->itemOrcamentoPayload(['descricao' => 'Impressão', 'quantidade' => '1.00', 'valorUnitario' => '30.00']),
            ],
        ]), $headers);

        $res->assertCreated()
            ->assertJsonPath('valorTotal', '130.00')
            ->assertJsonPath('status', 'Pendente')
            ->assertJsonPath('itens.0.subtotal', '100.00');
    }

    public function test_atualizar_itens_vira_ajuste(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $id = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');

        $this->putJson("/api/vendas/orcamentos/{$id}", [
            'itens' => [$this->itemOrcamentoPayload(['valorUnitario' => '80.00'])],
        ], $headers)->assertOk()
            ->assertJsonPath('status', 'Ajuste')
            ->assertJsonPath('valorTotal', '160.00');
    }

    public function test_aprovar_publica_evento(): void
    {
        Event::fake();

        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $id = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');

        $this->putJson("/api/vendas/orcamentos/{$id}", ['status' => 'Aprovado'], $headers)
            ->assertOk()->assertJsonPath('status', 'Aprovado');

        Event::assertDispatched(OrcamentoAprovadoEvent::class, fn ($e) => $e->idOrcamento === $id);
    }

    public function test_aprovado_convertido_trava_edicao(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $id = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');

        $this->putJson("/api/vendas/orcamentos/{$id}", ['status' => 'Aprovado'], $headers)->assertOk();
        $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $id], $headers)->assertCreated();

        $this->putJson("/api/vendas/orcamentos/{$id}", [
            'itens' => [$this->itemOrcamentoPayload()],
        ], $headers)->assertConflict();
    }

    public function test_duplicar_copia_pendente_com_itens(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $id = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');

        $this->putJson("/api/vendas/orcamentos/{$id}", ['status' => 'Recusado'], $headers)->assertOk();

        $res = $this->postJson("/api/vendas/orcamentos/{$id}/duplicar", [], $headers);

        $res->assertCreated()
            ->assertJsonPath('status', 'Pendente')
            ->assertJsonPath('valorTotal', '100.00');

        $this->assertNotSame($id, $res->json('id'));
        $this->assertCount(1, $res->json('itens'));
    }

    public function test_listar_filtra_e_conta_por_status(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $id = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');

        $this->getJson('/api/vendas/orcamentos?status=Pendente', $headers)
            ->assertOk()->assertJsonCount(1, 'orcamentos')
            ->assertJsonPath('counts.Pendente', 1);

        $this->putJson("/api/vendas/orcamentos/{$id}", ['status' => 'Aprovado'], $headers)->assertOk();

        $this->getJson("/api/vendas/orcamentos?clienteId={$clienteId}&status=Aprovado", $headers)
            ->assertOk()->assertJsonCount(1, 'orcamentos');
    }

    public function test_item_estoque_inexistente_da_422(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');

        $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId, [
            'itens' => [$this->itemOrcamentoPayload(['idItemEstoque' => 999999])],
        ]), $headers)->assertStatus(422);

        $this->assertSame(0, Orcamento::count());
    }

    public function test_item_estoque_existente_passa_sem_coluna(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $itemId = $this->criarItemEstoque()->getKey();

        $res = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId, [
            'itens' => [$this->itemOrcamentoPayload(['idItemEstoque' => $itemId])],
        ]), $headers);

        $res->assertCreated();
        $this->assertArrayNotHasKey('idItemEstoque', $res->json('itens.0'));
    }
}

<?php

namespace Tests\Feature\Vendas;

use App\Modules\Auth\Enums\Role;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use App\Modules\Vendas\Events\EncomendaStatusAlteradoEvent;
use App\Modules\Vendas\Models\Encomenda;
use Illuminate\Support\Facades\Event;

/** Encomendas: conversão, venda direta, Kanban+versão, nova ordem, criador+Admin, eventos. */
class EncomendasTest extends VendasTestCase
{
    private function fluxoAprovado(array $headers): array
    {
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $orcId = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');
        $this->putJson("/api/vendas/orcamentos/{$orcId}", ['status' => 'Aprovado'], $headers)->assertOk();

        return [$clienteId, $orcId];
    }

    public function test_converter_orcamento_aprovado_publica_evento(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();
        [$clienteId, $orcId] = $this->fluxoAprovado($headers);

        $res = $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers);

        $res->assertCreated()
            ->assertJsonPath('statusKanban', 'Fila')
            ->assertJsonPath('valorFinal', '100.00')
            ->assertJsonPath('versao', 0)
            ->assertJsonCount(1, 'historico');

        Event::assertDispatched(EncomendaCriadaEvent::class,
            fn ($e) => $e->idCliente === $clienteId && $e->valorFinal === '100.00');
    }

    public function test_converter_nao_aprovado_ou_duplo_da_409(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $orcId = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');

        $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)->assertConflict();

        $this->putJson("/api/vendas/orcamentos/{$orcId}", ['status' => 'Aprovado'], $headers)->assertOk();
        $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)->assertCreated();
        $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)->assertConflict();
    }

    public function test_venda_direta_exige_cliente_e_valor(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');

        $this->postJson('/api/vendas/encomendas', [
            'clienteId' => $clienteId, 'valorFinal' => '250.00',
        ], $headers)->assertCreated()
            ->assertJsonPath('statusKanban', 'Fila')
            ->assertJsonPath('idOrcamento', null);

        $this->postJson('/api/vendas/encomendas', ['valorFinal' => '10.00'], $headers)->assertBadRequest();
        $this->postJson('/api/vendas/encomendas', ['clienteId' => $clienteId], $headers)->assertBadRequest();
    }

    public function test_kanban_move_historico_e_evento(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();
        [, $orcId] = $this->fluxoAprovado($headers);
        $id = $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)
            ->assertCreated()->json('id');

        $res = $this->putJson("/api/vendas/encomendas/{$id}/kanban", [
            'statusKanban' => 'Produção', 'versao' => 0, 'observacao' => 'Iniciou',
        ], $headers);

        $res->assertOk()
            ->assertJsonPath('statusKanban', 'Produção')
            ->assertJsonPath('versao', 1)
            ->assertJsonCount(2, 'historico');

        Event::assertDispatched(EncomendaStatusAlteradoEvent::class,
            fn ($e) => $e->idEncomenda === $id && $e->statusNovo === 'Produção');

        $this->getJson("/api/vendas/historico-status/{$id}", $headers)
            ->assertOk()->assertJsonCount(2);
    }

    public function test_kanban_versao_antiga_da_409_e_entregue_trava(): void
    {
        $headers = $this->headersAdmin();
        [, $orcId] = $this->fluxoAprovado($headers);
        $id = $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)
            ->assertCreated()->json('id');

        $this->putJson("/api/vendas/encomendas/{$id}/kanban", [
            'statusKanban' => 'Produção', 'versao' => 0,
        ], $headers)->assertOk();

        // Versão antiga (0) após mover (atual 1) → 409 sem mover.
        $this->putJson("/api/vendas/encomendas/{$id}/kanban", [
            'statusKanban' => 'Pronto', 'versao' => 0,
        ], $headers)->assertConflict();
        $this->assertSame('Produção', Encomenda::find($id)->status_kanban->value);

        foreach (['Acabamento', 'Pronto', 'Entregue'] as $i => $alvo) {
            $this->putJson("/api/vendas/encomendas/{$id}/kanban", [
                'statusKanban' => $alvo, 'versao' => 1 + $i,
            ], $headers)->assertOk();
        }

        $this->putJson("/api/vendas/encomendas/{$id}/kanban", [
            'statusKanban' => 'Pronto', 'versao' => 4,
        ], $headers)->assertConflict();
    }

    public function test_kanban_nao_criador_nao_admin_403(): void
    {
        $headersCriador = $this->headersAdmin();
        [, $orcId] = $this->fluxoAprovado($headersCriador);
        $id = $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headersCriador)
            ->assertCreated()->json('id');

        $headersOutro = $this->headersPapel(Role::BOLSISTA);

        $this->putJson("/api/vendas/encomendas/{$id}/kanban", [
            'statusKanban' => 'Produção', 'versao' => 0,
        ], $headersOutro)->assertForbidden();

        $this->postJson("/api/vendas/encomendas/{$id}/nova-ordem", [
            'valorFinal' => '10.00',
        ], $headersOutro)->assertForbidden();
    }

    public function test_nova_ordem_referencia_origem(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();
        [, $orcId] = $this->fluxoAprovado($headers);
        $id = $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)
            ->assertCreated()->json('id');

        $res = $this->postJson("/api/vendas/encomendas/{$id}/nova-ordem", [
            'valorFinal' => '150.00', 'observacoes' => 'Ajuste de escopo',
        ], $headers);

        $res->assertCreated()
            ->assertJsonPath('statusKanban', 'Fila')
            ->assertJsonPath('valorFinal', '150.00')
            ->assertJsonPath('encomendaOrigemId', $id);

        Event::assertDispatched(EncomendaCriadaEvent::class,
            fn ($e) => $e->idEncomenda === $res->json('id'));
    }

    public function test_status_e_listar_com_counts(): void
    {
        $headers = $this->headersAdmin();
        [$clienteId, $orcId] = $this->fluxoAprovado($headers);
        $id = $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)
            ->assertCreated()->json('id');

        $this->getJson("/api/vendas/encomendas/{$id}/status", $headers)
            ->assertOk()->assertJsonPath('statusKanban', 'Fila');

        $this->getJson("/api/vendas/encomendas?status=Fila&clienteId={$clienteId}", $headers)
            ->assertOk()->assertJsonCount(1, 'encomendas')
            ->assertJsonPath('counts.Fila', 1);
    }
}

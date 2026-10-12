<?php

namespace Tests\Feature\Vendas;

use App\Modules\Vendas\Contracts\VendasContract;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use App\Modules\Vendas\Events\EncomendaStatusAlteradoEvent;
use App\Modules\Vendas\Events\OrcamentoAprovadoEvent;
use App\Modules\Vendas\Events\ProducaoStatusAlteradoEvent;
use App\Modules\Vendas\Models\Encomenda;
use App\Modules\Vendas\Models\HistoricoStatusEncomenda;

/** Eventos (3 emitidos + consumo da produção) e VendasContract (2 métodos). */
class EventsContractTest extends VendasTestCase
{
    private function encomendaPronta(array $headers): int
    {
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload([
            'cpfCnpj' => self::CNPJ_VALIDO, 'tipoPessoa' => 'PJ',
        ]), $headers)->assertCreated()->json('id');
        $orcId = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');
        $this->putJson("/api/vendas/orcamentos/{$orcId}", ['status' => 'Aprovado'], $headers)->assertOk();

        return $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)
            ->assertCreated()->json('id');
    }

    public function test_fluxo_completo_emite_3_eventos(): void
    {
        \Illuminate\Support\Facades\Event::fake();
        $headers = $this->headersAdmin();

        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $orcId = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');
        $this->putJson("/api/vendas/orcamentos/{$orcId}", ['status' => 'Aprovado'], $headers)->assertOk();

        \Illuminate\Support\Facades\Event::assertDispatched(OrcamentoAprovadoEvent::class);
        $this->assertSame('orcamento.aprovado.event', OrcamentoAprovadoEvent::NAME);

        $encId = $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)
            ->assertCreated()->json('id');

        \Illuminate\Support\Facades\Event::assertDispatched(EncomendaCriadaEvent::class);
        $this->assertSame('encomenda.criada.event', EncomendaCriadaEvent::NAME);

        $this->putJson("/api/vendas/encomendas/{$encId}/kanban", [
            'statusKanban' => 'Produção', 'versao' => 0,
        ], $headers)->assertOk();

        \Illuminate\Support\Facades\Event::assertDispatched(EncomendaStatusAlteradoEvent::class);
        $this->assertSame('encomenda.status.alterado.event', EncomendaStatusAlteradoEvent::NAME);
    }

    public function test_producao_status_listener_sincroniza_idempotente(): void
    {
        $headers = $this->headersAdmin();
        $id = $this->encomendaPronta($headers);

        // Dispatch manual (produtor M6 ainda não existe).
        event(new ProducaoStatusAlteradoEvent($id, 'Produção', 'Setup concluído'));

        $this->assertSame('Produção', Encomenda::find($id)->status_kanban->value);
        $historico = HistoricoStatusEncomenda::where('id_encomenda', $id)->orderBy('id_historico')->get();
        $this->assertCount(2, $historico);
        $this->assertSame(0, (int) $historico->last()->id_usuario);
        $this->assertSame('Setup concluído', $historico->last()->observacao);

        // Status igual = no-op (sem nova linha de histórico).
        event(new ProducaoStatusAlteradoEvent($id, 'Produção'));
        $this->assertCount(2, HistoricoStatusEncomenda::where('id_encomenda', $id)->get());

        // Desconhecido = ignore, sem 500.
        event(new ProducaoStatusAlteradoEvent($id, 'HIPERESPAÇO'));
        $this->assertSame('Produção', Encomenda::find($id)->status_kanban->value);
    }

    public function test_vendas_contract_dados_e_nulos(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $orcId = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');
        $encId = $this->encomendaPronta($headers);

        $contract = app(VendasContract::class);

        $enc = $contract->dadosEncomenda($encId);
        $this->assertSame($encId, $enc['id']);
        $this->assertSame('Fila', $enc['statusKanban']);
        $this->assertSame('100.00', $enc['valorFinal']);
        $this->assertArrayHasKey('dataPrevisao', $enc);

        $orc = $contract->dadosOrcamento($orcId);
        $this->assertSame($orcId, $orc['id']);
        $this->assertSame($clienteId, $orc['idCliente']);
        $this->assertSame('Pendente', $orc['status']);

        $this->assertNull($contract->dadosEncomenda(999999));
        $this->assertNull($contract->dadosOrcamento(999999));
    }
}

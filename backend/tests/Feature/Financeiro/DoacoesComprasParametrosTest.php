<?php

namespace Tests\Feature\Financeiro;

use App\Modules\Financeiro\Events\CompraSolicitadaEvent;
use Illuminate\Support\Facades\Event;

/** Doações, compras e parâmetros (append-only + vigentes). */
class DoacoesComprasParametrosTest extends FinanceiroTestCase
{
    public function test_doacoes_registrar_listar_filtros(): void
    {
        $headers = $this->headersAdmin();

        $this->postJson('/api/financeiro/doacoes-recursos', [
            'tipo' => 'DOACAO', 'origem' => 'Doador A', 'valor' => '500.00',
            'dataRecebimento' => '2026-02-01',
        ], $headers)->assertCreated()->assertJsonPath('origem', 'Doador A');

        $this->postJson('/api/financeiro/doacoes-recursos', [
            'tipo' => 'PROJETO', 'origem' => 'Edital B', 'valor' => '1500.00',
            'dataRecebimento' => '2026-03-01', 'idProjetoAssociado' => 42,
        ], $headers)->assertCreated()->assertJsonPath('idProjetoAssociado', 42);

        $this->getJson('/api/financeiro/doacoes-recursos?tipo=DOACAO', $headers)
            ->assertOk()->assertJsonCount(1);
        $this->getJson('/api/financeiro/doacoes-recursos?dataInicio=2026-03-01&dataFim=2026-03-31', $headers)
            ->assertOk()->assertJsonCount(1)
            ->assertJsonPath('0.origem', 'Edital B');

        $this->postJson('/api/financeiro/doacoes-recursos', [
            'tipo' => 'DOACAO', 'origem' => 'X', 'valor' => '0',
            'dataRecebimento' => '2026-01-01',
        ], $headers)->assertUnprocessable();
    }

    public function test_compra_registrar_concluir_duplo_409_e_evento(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();

        $id = $this->postJson('/api/financeiro/solicitacoes-compra', [
            'idItemEstoque' => 5, 'quantidade' => '3.00', 'valorEstimado' => '90.00',
        ], $headers)->assertCreated()
            ->assertJsonPath('status', 'REGISTRADA')->json('id');

        Event::assertDispatched(CompraSolicitadaEvent::class,
            fn ($e) => $e->idCompra === $id && $e->idFornecedor === null);

        $this->getJson('/api/financeiro/solicitacoes-compra?status=REGISTRADA', $headers)
            ->assertOk()->assertJsonCount(1);
        $this->getJson("/api/financeiro/solicitacoes-compra/{$id}", $headers)->assertOk();
        $this->getJson('/api/financeiro/solicitacoes-compra/9999', $headers)->assertNotFound();

        $this->putJson("/api/financeiro/solicitacoes-compra/{$id}/concluir", [], $headers)->assertOk()
            ->assertJsonPath('status', 'CONCLUIDA');
        $this->putJson("/api/financeiro/solicitacoes-compra/{$id}/concluir", [], $headers)->assertConflict();
    }

    public function test_parametros_append_only_e_vigentes(): void
    {
        $headers = $this->headersAdmin();

        $this->postJson('/api/financeiro/valores-hora', [
            'nivelAcesso' => 1, 'valorHora' => '15.00', 'dataVigencia' => '2026-01-01',
        ], $headers)->assertCreated();
        $this->postJson('/api/financeiro/valores-hora', [
            'nivelAcesso' => 1, 'valorHora' => '18.00', 'dataVigencia' => '2026-06-01',
        ], $headers)->assertCreated();

        // Vigente = maior vigência (nunca edita).
        $this->getJson('/api/financeiro/valores-hora', $headers)->assertOk()
            ->assertJsonCount(1)
            ->assertJsonPath('0.valorHora', '18.00');

        $this->postJson('/api/financeiro/valores-hora', [
            'nivelAcesso' => 9, 'valorHora' => '10.00', 'dataVigencia' => '2026-01-01',
        ], $headers)->assertUnprocessable();

        // Overhead: GET devolve o vigente.
        $this->getJson('/api/financeiro/parametros-overhead', $headers)->assertStatus(422);
        $this->postJson('/api/financeiro/parametros-overhead', [
            'valorTaxaHora' => '1.5000', 'dataVigencia' => '2026-01-01',
        ], $headers)->assertCreated();
        $this->getJson('/api/financeiro/parametros-overhead', $headers)->assertOk()
            ->assertJsonPath('valorTaxaHora', '1.5000');
    }
}

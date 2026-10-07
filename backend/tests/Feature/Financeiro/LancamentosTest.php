<?php

namespace Tests\Feature\Financeiro;

use App\Modules\Financeiro\Events\LancamentoVencidoEvent;
use App\Modules\Financeiro\Models\LancamentoFinanceiro;
use App\Modules\Financeiro\Services\LancamentoService;
use Illuminate\Support\Facades\Event;

/** Lançamentos: criar, filtros D-4/D-8, counts/resumo D-2, pagar, vencidos. */
class LancamentosTest extends FinanceiroTestCase
{
    public function test_criar_pendente_e_listar_com_counts_e_resumo(): void
    {
        $headers = $this->headersAdmin();
        $catId = $this->postJson('/api/financeiro/categorias', [
            'nome' => 'Materiais', 'tipo' => 'DESPESA',
        ], $headers)->assertCreated()->json('id');

        $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId), $headers)
            ->assertCreated()
            ->assertJsonPath('status', 'PENDENTE')
            ->assertJsonPath('valor', '100.00');

        $this->getJson('/api/financeiro/lancamentos', $headers)->assertOk()
            ->assertJsonPath('counts.PENDENTE', 1)
            ->assertJsonPath('counts.PAGO', 0)
            ->assertJsonPath('counts.ATRASADO', 0)
            ->assertJsonPath('counts.CANCELADO', 0)
            ->assertJsonPath('resumo.entradas', '0.00')
            ->assertJsonPath('resumo.saidas', '100.00')
            ->assertJsonPath('resumo.pendente', '100.00')
            ->assertJsonPath('resumo.saldo', '-100.00');
    }

    public function test_criar_vencido_nasce_atrasado_e_publica_evento(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();
        $catId = $this->postJson('/api/financeiro/categorias', [
            'nome' => 'Aluguel', 'tipo' => 'DESPESA',
        ], $headers)->assertCreated()->json('id');

        $id = $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'dataVencimento' => today()->subDay()->toDateString(),
            'idReferenciaExterna' => 'ENC-9',
        ]), $headers)->assertCreated()->assertJsonPath('status', 'ATRASADO')->json('id');

        Event::assertDispatched(LancamentoVencidoEvent::class,
            fn ($e) => $e->idLancamento === $id && $e->idReferenciaExterna === 'ENC-9');
    }

    public function test_criar_com_categoria_inexistente_da_404_e_valor_invalido_422(): void
    {
        $headers = $this->headersAdmin();

        $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload(9999), $headers)
            ->assertNotFound();

        $catId = $this->postJson('/api/financeiro/categorias', [
            'nome' => 'X', 'tipo' => 'RECEITA',
        ], $headers)->assertCreated()->json('id');

        $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'valor' => '0',
        ]), $headers)->assertUnprocessable();
    }

    public function test_filtros_combinados_d4_d8(): void
    {
        $headers = $this->headersAdmin();
        $catId = $this->criarCategoria()->getKey();

        $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'tipo' => 'ENTRADA', 'idReferenciaExterna' => 'CLIENTE-ACME-1',
            'dataVencimento' => '2026-03-10',
        ]), $headers)->assertCreated();
        $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'tipo' => 'SAIDA', 'idReferenciaExterna' => 'FORN-BETA-2',
            'dataVencimento' => '2026-04-10',
        ]), $headers)->assertCreated();

        $this->getJson('/api/financeiro/lancamentos?tipo=ENTRADA', $headers)
            ->assertOk()->assertJsonCount(1, 'lancamentos');
        $this->getJson('/api/financeiro/lancamentos?origem=ACME', $headers)
            ->assertOk()->assertJsonCount(1, 'lancamentos')
            ->assertJsonPath('lancamentos.0.idReferenciaExterna', 'CLIENTE-ACME-1');
        $this->getJson('/api/financeiro/lancamentos?vencimentoDe=2026-04-01&vencimentoAte=2026-04-30', $headers)
            ->assertOk()->assertJsonCount(1, 'lancamentos');
        // Alias dataInicio/dataFim delimita o vencimento (D-8).
        $this->getJson('/api/financeiro/lancamentos?dataInicio=2026-04-01&dataFim=2026-04-30', $headers)
            ->assertOk()->assertJsonCount(1, 'lancamentos');
        $this->getJson("/api/financeiro/lancamentos?status=ATRASADO&tipo=SAIDA&idCategoria={$catId}", $headers)
            ->assertOk()->assertJsonCount(1, 'lancamentos');
    }

    public function test_pagar_liquida_e_duplo_da_409(): void
    {
        $headers = $this->headersAdmin();
        $catId = $this->criarCategoria()->getKey();
        $id = $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId), $headers)
            ->assertCreated()->json('id');

        $this->putJson("/api/financeiro/lancamentos/{$id}/pagamento", [], $headers)->assertOk()
            ->assertJsonPath('status', 'PAGO')
            ->assertJsonPath('dataPagamento', today()->toDateString());

        $this->putJson("/api/financeiro/lancamentos/{$id}/pagamento", [], $headers)->assertConflict();

        $this->putJson('/api/financeiro/lancamentos/9999/pagamento', [], $headers)->assertNotFound();
    }

    public function test_emitir_vencidos_marca_e_publica_sem_liquidar(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();
        $catId = $this->criarCategoria()->getKey();

        $vencido = $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'dataVencimento' => today()->subDays(2)->toDateString(),
        ]), $headers)->assertCreated()->json('id');
        $futuro = $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId), $headers)
            ->assertCreated()->json('id');

        // O criar-vencido já publicou 1 evento; limpa para medir só a varredura.
        Event::fake();

        $count = app(LancamentoService::class)->emitirVencidos();
        $this->assertSame(1, $count);

        Event::assertDispatched(LancamentoVencidoEvent::class, 1);
        Event::assertDispatched(LancamentoVencidoEvent::class,
            fn ($e) => $e->idLancamento === $vencido);

        $this->assertSame('ATRASADO', LancamentoFinanceiro::find($vencido)->status->value);
        $this->assertSame('PENDENTE', LancamentoFinanceiro::find($futuro)->status->value);

        // Idempotente na segunda varredura quanto a status (segue ATRASADO, republica).
        $this->assertSame(1, app(LancamentoService::class)->emitirVencidos());
    }

    public function test_pagar_aceita_data_e_observacao(): void
    {
        $headers = $this->headersAdmin();
        $catId = $this->criarCategoria()->getKey();
        $id = $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId), $headers)
            ->assertCreated()->json('id');

        $this->putJson("/api/financeiro/lancamentos/{$id}/pagamento", [
            'dataPagamento' => today()->toDateString(), 'observacao' => 'Pago em espécie',
        ], $headers)->assertOk()
            ->assertJsonPath('observacao', 'Pago em espécie');
    }

    public function test_emitir_vencidos_transiciona_pendente_para_atrasado(): void
    {
        Event::fake();
        $catId = $this->criarCategoria()->getKey();

        // PENDENTE vencido inserido direto (o criar já nasceria ATRASADO).
        $pendente = LancamentoFinanceiro::create([
            'id_categoria' => $catId, 'tipo' => 'SAIDA', 'valor' => '75.00',
            'data_vencimento' => today()->subDay()->toDateString(), 'status' => 'PENDENTE',
        ]);

        $this->assertSame(1, app(LancamentoService::class)->emitirVencidos());
        $this->assertSame('ATRASADO', $pendente->refresh()->status->value);
        Event::assertDispatched(LancamentoVencidoEvent::class, 1);
    }

    public function test_cancelado_fora_das_somas(): void
    {
        $headers = $this->headersAdmin();
        $catId = $this->criarCategoria()->getKey();
        $id = $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'valor' => '50.00',
        ]), $headers)->assertCreated()->json('id');

        $lancamento = LancamentoFinanceiro::find($id);
        $lancamento->status = \App\Modules\Financeiro\Enums\StatusLancamento::CANCELADO;
        $lancamento->save();

        $this->getJson('/api/financeiro/lancamentos', $headers)->assertOk()
            ->assertJsonPath('counts.CANCELADO', 1)
            ->assertJsonPath('resumo.saidas', '0.00')
            ->assertJsonPath('resumo.pendente', '0.00');
    }
}

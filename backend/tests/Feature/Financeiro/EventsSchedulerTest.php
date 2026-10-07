<?php

namespace Tests\Feature\Financeiro;

use App\Modules\Estoque\Events\ProducaoConcluidaEvent;
use App\Modules\Financeiro\Events\CompraSolicitadaEvent;
use App\Modules\Financeiro\Events\CustoCalculadoEvent;
use App\Modules\Financeiro\Events\LancamentoVencidoEvent;
use App\Modules\Financeiro\Models\CustoEncomenda;
use App\Modules\Financeiro\Models\FechamentoEncomenda;
use App\Modules\Financeiro\Models\HorasEncomenda;
use App\Modules\Financeiro\Services\LancamentoService;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Events\HorasValidadasEvent;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use Illuminate\Support\Facades\Event;

/** Listeners (3 consumidos), emissões (3) e scheduler manual. */
class EventsSchedulerTest extends FinanceiroTestCase
{
    public function test_encomenda_criada_abre_fechamento_idempotente(): void
    {
        event(new EncomendaCriadaEvent(31, 7, '1500.00', '2026-09-01'));
        event(new EncomendaCriadaEvent(31, 7, '1500.00', '2026-09-01'));

        $this->assertSame(1, FechamentoEncomenda::where('id_encomenda', 31)->count());
        $fechamento = FechamentoEncomenda::where('id_encomenda', 31)->first();
        $this->assertSame('ABERTA', $fechamento->status->value);
        $this->assertSame('1500.00', (string) $fechamento->valor_fechado);
        $this->assertSame('2026-09-01', substr((string) $fechamento->data_fechamento, 0, 10));
    }

    public function test_horas_validadas_acumula_e_projeto_ignora(): void
    {
        FechamentoEncomenda::create([
            'id_encomenda' => 32, 'horas_estimadas' => '10.00', 'valor_fechado' => '500.00',
            'data_fechamento' => '2026-09-01', 'status' => 'ABERTA', 'horas_validadas' => '0.00',
        ]);

        event(new HorasValidadasEvent(51, TipoApontamento::ENCOMENDA, 32, '2.50', '2026-09-05'));
        event(new HorasValidadasEvent(51, TipoApontamento::ENCOMENDA, 32, '1.50', '2026-09-05'));
        // PROJETO não é hora de encomenda — ignorado.
        event(new HorasValidadasEvent(51, TipoApontamento::PROJETO, 99, '8.00', '2026-09-05'));

        $this->assertSame(1, HorasEncomenda::where('id_encomenda', 32)->count());
        $this->assertSame('4.00', (string) HorasEncomenda::where('id_encomenda', 32)->first()->horas);
        $this->assertSame('4.00', (string) FechamentoEncomenda::where('id_encomenda', 32)->first()->horas_validadas);
        $this->assertSame(0, HorasEncomenda::where('id_encomenda', 99)->count());
    }

    public function test_horas_para_encomenda_sem_fechamento_nao_quebra(): void
    {
        // Sem fechamento: warn/ignore, sem exceção (protege o produtor RH).
        event(new HorasValidadasEvent(52, TipoApontamento::ENCOMENDA, 9998, '2.00', '2026-09-05'));

        $this->assertSame(0, HorasEncomenda::where('id_encomenda', 9998)->count());
    }

    public function test_producao_concluida_dispara_custeio_e_sem_fechamento_nao_quebra(): void
    {
        $headers = $this->headersAdmin();
        $this->postJson('/api/financeiro/valores-hora', [
            'nivelAcesso' => 2, 'valorHora' => '10.00', 'dataVigencia' => '2026-01-01',
        ], $headers)->assertCreated();
        $this->postJson('/api/financeiro/parametros-overhead', [
            'valorTaxaHora' => '1.0000', 'dataVigencia' => '2026-01-01',
        ], $headers)->assertCreated();
        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload(33), $headers)
            ->assertCreated();

        event(new ProducaoConcluidaEvent(33, null, []));

        $custo = CustoEncomenda::where('id_encomenda', 33)->first();
        $this->assertNotNull($custo);
        $this->assertSame('0.00', (string) $custo->custo_total);
        $this->assertSame('1000.00', (string) $custo->margem_lucro);

        // Sem fechamento: warn/ignore, sem exceção (produtor real chega em M6).
        event(new ProducaoConcluidaEvent(9997, null, []));

        $this->assertSame(0, CustoEncomenda::where('id_encomenda', 9997)->count());
    }

    public function test_emissoes_tem_nomes_e_payloads_preservados(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();
        $catId = $this->criarCategoria()->getKey();

        $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'dataVencimento' => today()->subDay()->toDateString(),
        ]), $headers)->assertCreated();
        $this->postJson('/api/financeiro/solicitacoes-compra', [
            'idItemEstoque' => 1, 'quantidade' => '1.00',
        ], $headers)->assertCreated();

        Event::assertDispatched(LancamentoVencidoEvent::class,
            fn ($e) => $e::NAME === 'lancamento.vencido.event'
                && array_keys($e->payload()) === ['idLancamento', 'valor', 'dataVencimento', 'idReferenciaExterna']);
        Event::assertDispatched(CompraSolicitadaEvent::class,
            fn ($e) => $e::NAME === 'compra.solicitada.event'
                && array_keys($e->payload()) === ['idCompra', 'idFornecedor']);

        // Nomes preservados (custo calculado é coberto no FechamentoCusteioTest).
        $this->assertSame('custo.calculado.event', CustoCalculadoEvent::NAME);
        $this->assertSame('database', CustoCalculadoEvent::QUEUE);
    }

    public function test_scheduler_manual_publica_um_evento_por_vencido(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();
        $catId = $this->criarCategoria()->getKey();

        $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'dataVencimento' => today()->subDay()->toDateString(),
        ]), $headers)->assertCreated();
        $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId), $headers)
            ->assertCreated();

        Event::fake();
        $this->assertSame(1, app(LancamentoService::class)->emitirVencidos());
        Event::assertDispatched(LancamentoVencidoEvent::class, 1);
    }
}

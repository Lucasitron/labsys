<?php

namespace Tests\Feature\Financeiro;

use App\Modules\Financeiro\Events\CustoCalculadoEvent;
use App\Modules\Financeiro\Models\FechamentoEncomenda;
use App\Modules\Financeiro\Models\HorasEncomenda;
use App\Modules\Financeiro\Services\CusteioService;
use App\Modules\Financeiro\Services\FechamentoService;
use Illuminate\Support\Facades\Event;

/** Fechamento imutável + custeio por ordem (fórmula em centavos). */
class FechamentoCusteioTest extends FinanceiroTestCase
{
    private function parametros(array $headers): void
    {
        $this->postJson('/api/financeiro/valores-hora', [
            'nivelAcesso' => 0, 'valorHora' => '20.00', 'dataVigencia' => today()->toDateString(),
        ], $headers)->assertCreated();
        $this->postJson('/api/financeiro/valores-hora', [
            'nivelAcesso' => 2, 'valorHora' => '12.50', 'dataVigencia' => today()->toDateString(),
        ], $headers)->assertCreated();
        $this->postJson('/api/financeiro/parametros-overhead', [
            'valorTaxaHora' => '2.0000', 'dataVigencia' => today()->toDateString(),
        ], $headers)->assertCreated();
    }

    private function cenarioCusteio(array $headers, int $encomenda = 7): void
    {
        $this->parametros($headers);
        $catId = $this->criarCategoria()->getKey();

        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload($encomenda), $headers)
            ->assertCreated();

        // Materiais: 100 + 50 (SAIDA por referência); CANCELADO e outra ref fora.
        foreach (['100.00', '50.00'] as $valor) {
            $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
                'valor' => $valor, 'idReferenciaExterna' => (string) $encomenda,
            ]), $headers)->assertCreated();
        }
        $outro = $this->postJson('/api/financeiro/lancamentos', $this->lancamentoPayload($catId, [
            'valor' => '999.00', 'idReferenciaExterna' => 'outra',
        ]), $headers)->assertCreated()->json('id');
        $cancel = \App\Modules\Financeiro\Models\LancamentoFinanceiro::find($outro);
        $cancel->status = \App\Modules\Financeiro\Enums\StatusLancamento::CANCELADO;
        $cancel->save();

        // Mão de obra: 5h nível 0 (20) + 3h sem nível (default 2 → 12.50).
        HorasEncomenda::create([
            'id_encomenda' => $encomenda, 'id_funcionario' => 11, 'nivel_acesso' => 0,
            'horas' => '5.00', 'data_registro' => '2026-09-01',
        ]);
        HorasEncomenda::create([
            'id_encomenda' => $encomenda, 'id_funcionario' => 12, 'nivel_acesso' => null,
            'horas' => '3.00', 'data_registro' => '2026-09-02',
        ]);
    }

    public function test_criar_duplicado_da_409_e_consultar_listar(): void
    {
        $headers = $this->headersAdmin();

        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload(3), $headers)
            ->assertCreated()->assertJsonPath('status', 'ABERTA')
            ->assertJsonPath('horasValidadas', '0.00');

        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload(3), $headers)
            ->assertConflict();

        $this->getJson('/api/financeiro/fechamento-encomenda/3', $headers)->assertOk()
            ->assertJsonPath('idEncomenda', 3);
        $this->getJson('/api/financeiro/fechamento-encomenda/999', $headers)->assertNotFound();
        $this->getJson('/api/financeiro/fechamento-encomenda', $headers)->assertOk()
            ->assertJsonCount(1);
    }

    public function test_nova_ordem_encerra_atual_e_abre_nova_imutavel(): void
    {
        $headers = $this->headersAdmin();
        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload(4), $headers)
            ->assertCreated();

        $this->postJson('/api/financeiro/fechamento-encomenda/4/nova-ordem', $this->fechamentoPayload(5, [
            'valorFechado' => '2000.00',
        ]), $headers)->assertCreated()->assertJsonPath('idEncomenda', 5);

        // A ordem antiga segue congelada (CONCLUIDA), nunca editada.
        $this->assertSame('CONCLUIDA', FechamentoEncomenda::where('id_encomenda', 4)->first()->status->value);
        $this->getJson('/api/financeiro/fechamento-encomenda/5', $headers)->assertOk()
            ->assertJsonPath('status', 'ABERTA')
            ->assertJsonPath('valorFechado', '2000.00');
    }

    public function test_nova_ordem_para_encomenda_com_fechamento_da_409(): void
    {
        $headers = $this->headersAdmin();
        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload(41), $headers)
            ->assertCreated();
        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload(42), $headers)
            ->assertCreated();

        // Alvo 42 já tem fechamento: nem a ordem 41 é encerrada.
        $this->postJson('/api/financeiro/fechamento-encomenda/41/nova-ordem', $this->fechamentoPayload(42), $headers)
            ->assertConflict();
        $this->assertSame('ABERTA', FechamentoEncomenda::where('id_encomenda', 41)->first()->status->value);
    }

    public function test_horas_upsert_soma_na_mesma_chave(): void
    {
        $headers = $this->headersAdmin();
        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload(6), $headers)
            ->assertCreated();

        $service = app(FechamentoService::class);
        $service->registrarHorasValidadas(6, 21, 1, '2.00', '2026-09-10');
        $service->registrarHorasValidadas(6, 21, 1, '3.00', '2026-09-10');

        $this->assertSame(1, HorasEncomenda::where('id_encomenda', 6)->count());
        $this->assertSame('5.00', (string) HorasEncomenda::where('id_encomenda', 6)->first()->horas);
        $this->assertSame('5.00', (string) FechamentoEncomenda::where('id_encomenda', 6)->first()->horas_validadas);
    }

    public function test_custeio_bate_formula_item_a_item_e_publica_evento(): void
    {
        Event::fake();
        $headers = $this->headersAdmin();
        $this->cenarioCusteio($headers);

        $custo = app(CusteioService::class)->calcular(
            7, \App\Modules\Financeiro\FinanceiroPrincipal::from($this->criarAdmin()),
        );

        // Materiais 150.00 + mão de obra (5×20 + 3×12.50 = 137.50) + overhead (8×2 = 16.00).
        $this->assertSame('150.00', (string) $custo->custo_materiais);
        $this->assertSame('137.50', (string) $custo->custo_mao_obra);
        $this->assertSame('16.00', (string) $custo->custo_overhead);
        $this->assertSame('303.50', (string) $custo->custo_total);
        $this->assertSame('696.50', (string) $custo->margem_lucro);

        Event::assertDispatched(CustoCalculadoEvent::class,
            fn ($e) => $e->idEncomenda === 7 && $e->custoTotal === '303.50' && $e->margemLucro === '696.50');

        // GET devolve o último cálculo congelado.
        $this->getJson('/api/financeiro/custos-encomenda/7', $headers)->assertOk()
            ->assertJsonPath('custoTotal', '303.50')
            ->assertJsonPath('margemLucro', '696.50');
        $this->getJson('/api/financeiro/custos-encomenda/999', $headers)->assertNotFound();
    }

    public function test_custeio_sem_precondicao_falha_orientativo_sem_persistir(): void
    {
        $headers = $this->headersAdmin();
        $principal = function () {
            $admin = $this->criarAdmin();

            return \App\Modules\Financeiro\FinanceiroPrincipal::from($admin);
        };

        // Sem valor/hora vigente → 422 orientativo, sem persistir parcial.
        $this->postJson('/api/financeiro/fechamento-encomenda', $this->fechamentoPayload(8), $headers)
            ->assertCreated();
        \App\Modules\Financeiro\Models\HorasEncomenda::create([
            'id_encomenda' => 8, 'id_funcionario' => 30, 'nivel_acesso' => 2,
            'horas' => '2.00', 'data_registro' => '2026-09-01',
        ]);
        try {
            app(CusteioService::class)->calcular(8, $principal());
            $this->fail('Deveria falhar sem valor/hora');
        } catch (\App\Shared\Exceptions\ConfiguracaoInvalidaException $e) {
            $this->assertStringContainsString('nível', $e->getMessage());
        }
        $this->assertSame(0, \App\Modules\Financeiro\Models\CustoEncomenda::where('id_encomenda', 8)->count());

        // Com valor/hora mas sem overhead → 422 orientativo.
        $this->postJson('/api/financeiro/valores-hora', [
            'nivelAcesso' => 2, 'valorHora' => '10.00', 'dataVigencia' => today()->toDateString(),
        ], $headers)->assertCreated();
        try {
            app(CusteioService::class)->calcular(8, $principal());
            $this->fail('Deveria falhar sem overhead');
        } catch (\App\Shared\Exceptions\ConfiguracaoInvalidaException $e) {
            $this->assertStringContainsString('overhead', $e->getMessage());
        }
        $this->assertSame(0, \App\Modules\Financeiro\Models\CustoEncomenda::where('id_encomenda', 8)->count());
    }
}

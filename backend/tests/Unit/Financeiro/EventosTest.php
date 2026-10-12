<?php

namespace Tests\Unit\Financeiro;

use App\Modules\Financeiro\Events\CompraSolicitadaEvent;
use App\Modules\Financeiro\Events\CustoCalculadoEvent;
use App\Modules\Financeiro\Events\LancamentoVencidoEvent;
use PHPUnit\Framework\TestCase;

/** Eventos emitidos: nomes preservados + payloads do contrato. */
class EventosTest extends TestCase
{
    public function test_nomes_preservados(): void
    {
        $this->assertSame('lancamento.vencido.event', LancamentoVencidoEvent::NAME);
        $this->assertSame('custo.calculado.event', CustoCalculadoEvent::NAME);
        $this->assertSame('compra.solicitada.event', CompraSolicitadaEvent::NAME);
    }

    public function test_payloads(): void
    {
        $vencido = new LancamentoVencidoEvent(1, '100.00', '2026-01-01', 'ENC-1');
        $this->assertSame(
            ['idLancamento' => 1, 'valor' => '100.00', 'dataVencimento' => '2026-01-01', 'idReferenciaExterna' => 'ENC-1'],
            $vencido->payload(),
        );

        $custo = new CustoCalculadoEvent(7, '303.50', '696.50', '2026-10-07');
        $this->assertSame(
            ['idEncomenda' => 7, 'custoTotal' => '303.50', 'margemLucro' => '696.50', 'dataCalculo' => '2026-10-07'],
            $custo->payload(),
        );

        $compra = new CompraSolicitadaEvent(9, null);
        $this->assertSame(['idCompra' => 9, 'idFornecedor' => null], $compra->payload());
    }
}

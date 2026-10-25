<?php

namespace Tests\Unit\Producao;

use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Events\AdvertenciaLimiteAtingidoEvent;
use App\Modules\Producao\Events\AdvertenciaRegistradaEvent;
use App\Modules\Producao\Events\KanbanStatusAlteradoEvent;
use App\Modules\Producao\Events\ProducaoConcluidaEvent;
use App\Modules\Producao\Events\ProducaoStatusAlteradoEvent;
use App\Modules\Producao\Events\ProjetoMesaAbandonadoEvent;
use PHPUnit\Framework\TestCase;

/** Nomes preservados + payloads dos 6 eventos emitidos. */
class ProducaoEventosTest extends TestCase
{
    public function test_nomes_e_payloads(): void
    {
        $this->assertSame('producao.status.alterado.event', ProducaoStatusAlteradoEvent::NAME);
        $this->assertSame('producao.concluida.event', ProducaoConcluidaEvent::NAME);
        $this->assertSame('kanban.status.alterado.event', KanbanStatusAlteradoEvent::NAME);
        $this->assertSame('advertencia.registrada.event', AdvertenciaRegistradaEvent::NAME);
        $this->assertSame('advertencia.limite.atingido.event', AdvertenciaLimiteAtingidoEvent::NAME);
        $this->assertSame('projeto.mesa.abandonado.event', ProjetoMesaAbandonadoEvent::NAME);

        $this->assertSame(
            ['idEncomenda' => 5, 'statusNovo' => 'PRODUCAO', 'idUsuario' => 7, 'observacao' => 'ok'],
            (new ProducaoStatusAlteradoEvent(5, 'PRODUCAO', 7, 'ok'))->payload(),
        );
        $this->assertSame(
            ['idEncomenda' => 5, 'idProdutoServico' => null, 'itens' => [], 'dataConclusao' => '2026-10-07'],
            (new ProducaoConcluidaEvent(5, null, [], '2026-10-07'))->payload(),
        );
        $this->assertSame(
            ['idEncomenda' => 5, 'statusAnterior' => 'FILA', 'statusNovo' => 'PRODUCAO', 'dataAlteracao' => '2026-10-07 10:00:00'],
            (new KanbanStatusAlteradoEvent(5, 'FILA', 'PRODUCAO', '2026-10-07 10:00:00'))->payload(),
        );
        $this->assertSame(
            ['idFuncionario' => 9, 'contador' => 3, 'motivo' => 'x'],
            (new AdvertenciaRegistradaEvent(9, 3, 'x'))->payload(),
        );
        $this->assertSame(
            ['idFuncionario' => 9, 'contador' => 3, 'motivo' => 'x'],
            (new AdvertenciaLimiteAtingidoEvent(9, 3, 'x'))->payload(),
        );
        $this->assertSame(
            ['idProjetoMesa' => 4, 'idFuncionario' => 9, 'acaoTomada' => null],
            (new ProjetoMesaAbandonadoEvent(4, 9, null))->payload(),
        );

        $this->assertSame('Produção', KanbanStatus::PRODUCAO->rotuloVendas());
    }
}

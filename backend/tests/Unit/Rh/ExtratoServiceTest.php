<?php

namespace Tests\Unit\Rh;

use App\Modules\Auth\Enums\Role;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Events\ExtratoMensalHorasEvent;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\RegistroPontoDiario;
use App\Modules\Rh\Services\ExtratoService;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class ExtratoServiceTest extends TestCase
{
    public function test_gerar_emite_um_evento_por_ativo_exceto_recrutando(): void
    {
        Event::fake([ExtratoMensalHorasEvent::class]);

        ['funcionario' => $bolsista] = $this->criarFuncionario(null, Role::BOLSISTA);
        $recrutando = $this->criarFuncionario(null, Role::RECRUTANDO);

        RegistroPontoDiario::create([
            'id_funcionario' => $bolsista->getKey(), 'data' => '2026-08-04', 'total_horas' => '4.00',
        ]);
        ApontamentoHoras::create([
            'id_funcionario' => $bolsista->getKey(), 'tipo' => TipoApontamento::ENCOMENDA,
            'id_referencia' => 1, 'data' => '2026-08-04', 'horas_trabalhadas' => '3.00',
            'status' => StatusApontamento::VALIDADO, 'consolidado' => false,
        ]);

        $total = app(ExtratoService::class)->gerarExtratoMensal('2026-08');

        $this->assertSame(1, $total);
        Event::assertDispatched(ExtratoMensalHorasEvent::class, 1);
        Event::assertDispatched(
            ExtratoMensalHorasEvent::class,
            fn (ExtratoMensalHorasEvent $e) => $e->idFuncionario === (int) $bolsista->getKey()
                && $e->mesReferencia === '08/2026'
                && $e->horasPresenca === '4.00'
                && $e->horasEncomenda === '3.00'
        );

        // Recrutando não recebe extrato.
        Event::assertNotDispatched(
            ExtratoMensalHorasEvent::class,
            fn (ExtratoMensalHorasEvent $e) => $e->idFuncionario === (int) $recrutando['funcionario']->getKey()
        );
    }
}

<?php

namespace Tests\Unit\Rh;

use App\Modules\Auth\Enums\Role;
use App\Modules\Rh\Contracts\RhContract;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\RegistroPontoDiario;
use App\Modules\Rh\Services\NivelService;
use Tests\TestCase;

class NivelRhContractTest extends TestCase
{
    public function test_matriz_conta_membros_por_nivel(): void
    {
        $this->criarFuncionario(null, Role::ADMIN);
        $this->criarFuncionario(null, Role::BOLSISTA);

        $matriz = app(NivelService::class)->matriz();

        $porCodigo = collect($matriz['niveis'])->keyBy('codigo');
        $this->assertSame(1, $porCodigo[0]['total']);
        $this->assertSame(1, $porCodigo[1]['total']);
        $this->assertSame(9, count($matriz['permissoes']));
    }

    public function test_contract_coerencia_e_horas_para_financeiro(): void
    {
        ['funcionario' => $func] = $this->criarFuncionario();
        $id = (int) $func->getKey();

        RegistroPontoDiario::create([
            'id_funcionario' => $id, 'data' => '2026-08-04', 'total_horas' => '4.00',
        ]);
        ApontamentoHoras::create([
            'id_funcionario' => $id, 'tipo' => TipoApontamento::ENCOMENDA,
            'id_referencia' => 7, 'data' => '2026-08-04', 'horas_trabalhadas' => '6.00',
            'status' => StatusApontamento::VALIDADO, 'consolidado' => false,
        ]);

        $contract = app(RhContract::class);

        $this->assertTrue($contract->funcionarioExiste($id));
        $this->assertFalse($contract->funcionarioExiste(999999));
        $this->assertSame('6.00', $contract->horasValidadasNoPeriodo($id, 'ENCOMENDA', '2026-08-01', '2026-08-31'));
        $this->assertSame('0.00', $contract->horasValidadasNoPeriodo($id, 'PROJETO', '2026-08-01', '2026-08-31'));

        $incoerencias = $contract->incoerencias($id);
        $this->assertCount(1, $incoerencias);
        $this->assertSame('2026-08-04', $incoerencias[0]['data']);
    }
}

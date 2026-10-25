<?php

namespace Tests\Unit\Rh;

use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\ApontamentoService;
use Tests\TestCase;

class ApontamentoResolverTest extends TestCase
{
    public function test_horario_recalcula_no_servidor(): void
    {
        $service = app(ApontamentoService::class);

        $this->assertSame(3.0, $service->resolverHoras([
            'horaInicio' => '14:00', 'horaFim' => '17:00', 'horasTrabalhadas' => 99,
        ]));
    }

    public function test_horario_incoerente_e_parcial_rejeitados(): void
    {
        $service = app(ApontamentoService::class);

        $this->expectException(\InvalidArgumentException::class);
        $service->resolverHoras(['horaInicio' => '17:00', 'horaFim' => '14:00']);
    }

    public function test_sem_horario_exige_horas_trabalhadas_positivas(): void
    {
        $service = app(ApontamentoService::class);

        $this->assertSame(2.5, $service->resolverHoras(['horasTrabalhadas' => 2.5]));

        $this->expectException(\InvalidArgumentException::class);
        $service->resolverHoras(['horasTrabalhadas' => 0]);
    }

    public function test_horario_parcial_rejeitado(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        app(ApontamentoService::class)->resolverHoras(['horaInicio' => '14:00']);
    }

    public function test_validar_direto_bordas_do_service(): void
    {
        ['login' => $login, 'funcionario' => $func] = $this->criarFuncionario();
        $this->criarTutor($func);
        $principal = RhPrincipal::from($login);
        $service = app(ApontamentoService::class);

        $ap = ApontamentoHoras::create([
            'id_funcionario' => $func->getKey(), 'tipo' => 'PROJETO',
            'id_referencia' => 1, 'data' => '2026-08-04', 'horas_trabalhadas' => '1.00',
            'status' => StatusApontamento::PENDENTE, 'consolidado' => false,
        ]);

        try {
            $service->validar((int) $ap->getKey(), StatusApontamento::PENDENTE, null, $principal);
            $this->fail('deveria rejeitar status PENDENTE');
        } catch (\InvalidArgumentException $e) {
            $this->assertSame('Status deve ser VALIDADO ou REJEITADO', $e->getMessage());
        }

        try {
            $service->validar((int) $ap->getKey(), StatusApontamento::REJEITADO, null, $principal);
            $this->fail('deveria exigir motivo');
        } catch (\InvalidArgumentException $e) {
            $this->assertSame('Motivo da rejeição é obrigatório', $e->getMessage());
        }

        try {
            $service->listar(null, 'periodo-invalido', $principal);
            $this->fail('deveria rejeitar período');
        } catch (\InvalidArgumentException $e) {
            $this->assertSame('Período deve estar no formato yyyy-MM', $e->getMessage());
        }
    }

    public function test_principal_resolve_vinculo_e_nivel(): void
    {
        ['login' => $login, 'funcionario' => $func] = $this->criarFuncionario();

        $principal = RhPrincipal::from($login);

        $this->assertSame($login->id_user, $principal->idPessoa);
        $this->assertSame((int) $func->getKey(), $principal->idFuncionario);
        $this->assertSame(NivelAcesso::BOLSISTA, $principal->nivel);
        $this->assertFalse($principal->isAdmin());
    }

    public function test_principal_sem_vinculo_degrada_para_admin_only(): void
    {
        $login = $this->criarAdmin();

        $principal = RhPrincipal::from($login);

        $this->assertNull($principal->idFuncionario);
        $this->assertTrue($principal->isAdmin());
    }
}

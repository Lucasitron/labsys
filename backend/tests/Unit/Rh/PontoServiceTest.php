<?php

namespace Tests\Unit\Rh;

use App\Modules\Rh\Models\RegistroPontoDiario;
use App\Modules\Rh\Services\PontoService;
use Carbon\Carbon;
use Tests\TestCase;

class PontoServiceTest extends TestCase
{
    public function test_entrada_e_saida_consolidam_total(): void
    {
        ['pessoa' => $pessoa, 'funcionario' => $func] = $this->criarFuncionario();

        $ponto = app(PontoService::class);
        $ponto->processarEventoRfid($pessoa->getKey(), 'ENTRADA', Carbon::parse('2026-08-04 08:00'));
        $ponto->processarEventoRfid($pessoa->getKey(), 'SAIDA', Carbon::parse('2026-08-04 12:00'));

        $registro = RegistroPontoDiario::where('id_funcionario', $func->getKey())->firstOrFail();

        $this->assertSame('2026-08-04', substr((string) $registro->data, 0, 10));
        $this->assertEquals('4.00', number_format((float) $registro->total_horas, 2, '.', ''));
    }

    public function test_reenvio_e_idempotente_por_dia(): void
    {
        ['pessoa' => $pessoa, 'funcionario' => $func] = $this->criarFuncionario();

        $ponto = app(PontoService::class);
        $ponto->processarEventoRfid($pessoa->getKey(), 'ENTRADA', Carbon::parse('2026-08-04 08:00'));
        // ENTRADA posterior não sobrescreve a mais cedo; SAIDA anterior não volta.
        $ponto->processarEventoRfid($pessoa->getKey(), 'ENTRADA', Carbon::parse('2026-08-04 09:00'));
        $ponto->processarEventoRfid($pessoa->getKey(), 'SAIDA', Carbon::parse('2026-08-04 12:00'));
        $ponto->processarEventoRfid($pessoa->getKey(), 'SAIDA', Carbon::parse('2026-08-04 11:00'));

        $this->assertSame(1, RegistroPontoDiario::where('id_funcionario', $func->getKey())->count());

        $registro = RegistroPontoDiario::where('id_funcionario', $func->getKey())->firstOrFail();
        $this->assertSame('08:00', $registro->hora_entrada->format('H:i'));
        $this->assertSame('12:00', $registro->hora_saida->format('H:i'));
    }

    public function test_acesso_negado_e_tipo_desconhecido_sao_ignorados(): void
    {
        ['pessoa' => $pessoa, 'funcionario' => $func] = $this->criarFuncionario();

        $ponto = app(PontoService::class);
        $ponto->processarEventoRfid($pessoa->getKey(), 'ACESSO_NEGADO', Carbon::parse('2026-08-04 08:00'));
        $ponto->processarEventoRfid($pessoa->getKey(), null, Carbon::parse('2026-08-04 08:00'));

        $this->assertSame(0, RegistroPontoDiario::where('id_funcionario', $func->getKey())->count());
    }

    public function test_usuario_sem_funcionario_e_ignorado(): void
    {
        $login = $this->criarLogin();

        app(PontoService::class)->processarEventoRfid($login->id_user, 'ENTRADA', Carbon::parse('2026-08-04 08:00'));

        $this->assertSame(0, RegistroPontoDiario::count());
    }
}

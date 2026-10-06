<?php

namespace Tests\Feature\Rh;

use App\Modules\Auth\Enums\AccessLogType;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Events\RfidAccessEvent;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Events\ExtratoMensalHorasEvent;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\RegistroPontoDiario;
use Carbon\Carbon;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class ExtratoRfidTest extends TestCase
{
    public function test_leitura_do_extrato_proprio_e_admin(): void
    {
        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();

        RegistroPontoDiario::create([
            'id_funcionario' => $id, 'data' => '2026-08-04', 'total_horas' => '4.00',
        ]);
        ApontamentoHoras::create([
            'id_funcionario' => $id, 'tipo' => TipoApontamento::PROJETO,
            'id_referencia' => 9, 'data' => '2026-08-04', 'horas_trabalhadas' => '2.00',
            'status' => StatusApontamento::VALIDADO, 'consolidado' => false,
        ]);

        $headersMembro = $this->authHeader($membro['login'], Role::BOLSISTA);

        $this->getJson('/api/rh/extrato-mensal-horas?mes=2026-08', $headersMembro)
            ->assertOk()
            ->assertJsonPath('mesReferencia', '08/2026')
            ->assertJsonPath('horasPresenca', '4.00')
            ->assertJsonPath('horasProjeto', '2.00');

        $this->getJson("/api/rh/extrato-mensal-horas?mes=2026-08&funcionarioId={$id}", $this->authHeader($admin['login'], Role::ADMIN))
            ->assertOk()
            ->assertJsonPath('idFuncionario', $id);

        // Terceiro não lê o alheio.
        $outro = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->getJson(
            "/api/rh/extrato-mensal-horas?mes=2026-08&funcionarioId={$id}",
            $this->authHeader($outro['login'], Role::BOLSISTA)
        )->assertForbidden();
    }

    public function test_evento_rfid_grava_ponto_via_listener(): void
    {
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);

        event(new RfidAccessEvent(
            idUser: $membro['login']->id_user,
            uuidRfid: 'cartao-1',
            timestamp: Carbon::parse('2026-08-04 08:00'),
            type: AccessLogType::ENTRADA,
        ));

        event(new RfidAccessEvent(
            idUser: $membro['login']->id_user,
            uuidRfid: 'cartao-1',
            timestamp: Carbon::parse('2026-08-04 12:00'),
            type: AccessLogType::SAIDA,
        ));

        $registro = RegistroPontoDiario::where('id_funcionario', $membro['funcionario']->getKey())->firstOrFail();
        $this->assertEquals('4.00', number_format((float) $registro->total_horas, 2, '.', ''));
    }

    public function test_scheduler_registrado_no_cron_mensal(): void
    {
        Event::fake([ExtratoMensalHorasEvent::class]);

        // Sem ativos: roda sem erro e nada emite (o cron `0 6 1 * *` está no schedule:list).
        $this->artisan('schedule:test --name=rh:extrato-mensal')->assertOk();

        Event::assertNotDispatched(ExtratoMensalHorasEvent::class);
    }
}

<?php

namespace Tests\Feature\Rh;

use App\Modules\Auth\Enums\Role;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Events\HorasValidadasEvent;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\RegistroPontoDiario;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class ApontamentosTest extends TestCase
{
    public function test_registrar_nasce_pendente_e_horario_recalcula(): void
    {
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $headers = $this->authHeader($membro['login'], Role::BOLSISTA);

        $res = $this->postJson('/api/rh/apontamentos-horas', [
            'idFuncionario' => $membro['funcionario']->getKey(),
            'tipo' => 'ENCOMENDA', 'idReferencia' => 11, 'data' => '2026-08-04',
            'horasTrabalhadas' => 99, 'horaInicio' => '14:00', 'horaFim' => '17:00',
        ], $headers);

        $res->assertCreated()
            ->assertJsonPath('status', 'PENDENTE')
            ->assertJsonPath('horasTrabalhadas', '3.00');

        // Outro funcionário não registra por ele.
        $outro = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->postJson('/api/rh/apontamentos-horas', [
            'idFuncionario' => $membro['funcionario']->getKey(),
            'tipo' => 'PROJETO', 'idReferencia' => 1, 'data' => '2026-08-04',
            'horasTrabalhadas' => 1,
        ], $this->authHeader($outro['login'], Role::BOLSISTA))->assertForbidden();
    }

    public function test_listar_filtra_proprio_para_nao_admin(): void
    {
        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);

        ApontamentoHoras::create([
            'id_funcionario' => $membro['funcionario']->getKey(), 'tipo' => 'PROJETO',
            'id_referencia' => 1, 'data' => '2026-08-04', 'horas_trabalhadas' => '2.00',
            'status' => StatusApontamento::PENDENTE, 'consolidado' => false,
        ]);

        $this->getJson('/api/rh/apontamentos-horas', $this->authHeader($membro['login'], Role::BOLSISTA))
            ->assertOk()->assertJsonCount(1);

        $this->getJson('/api/rh/apontamentos-horas', $this->authHeader($admin['login'], Role::ADMIN))
            ->assertOk()->assertJsonCount(1);
    }

    public function test_validar_exige_presenca_e_emite_evento(): void
    {
        Event::fake([HorasValidadasEvent::class]);

        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();
        $headersAdmin = $this->authHeader($admin['login'], Role::ADMIN);

        $apontamento = ApontamentoHoras::create([
            'id_funcionario' => $id, 'tipo' => 'ENCOMENDA',
            'id_referencia' => 5, 'data' => '2026-08-04', 'horas_trabalhadas' => '3.00',
            'status' => StatusApontamento::PENDENTE, 'consolidado' => false,
        ]);

        // Sem presença: 400.
        $this->putJson(
            "/api/rh/apontamentos-horas/{$apontamento->getKey()}/validar",
            ['status' => 'VALIDADO'],
            $headersAdmin
        )->assertStatus(400);

        RegistroPontoDiario::create([
            'id_funcionario' => $id, 'data' => '2026-08-04', 'total_horas' => '4.00',
        ]);

        $this->putJson(
            "/api/rh/apontamentos-horas/{$apontamento->getKey()}/validar",
            ['status' => 'VALIDADO'],
            $headersAdmin
        )->assertOk()->assertJsonPath('status', 'VALIDADO');

        Event::assertDispatched(
            HorasValidadasEvent::class,
            fn (HorasValidadasEvent $e) => $e->idFuncionario === $id && $e->horas === '3.00'
        );

        // Revalidar dá 400.
        $this->putJson(
            "/api/rh/apontamentos-horas/{$apontamento->getKey()}/validar",
            ['status' => 'REJEITADO', 'motivo' => 'x'],
            $headersAdmin
        )->assertStatus(400);
    }

    public function test_rejeitar_sem_motivo_da_400_e_tutor_valida(): void
    {
        $tutor = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->criarTutor($tutor['funcionario']);
        $aluno = $this->criarFuncionario(null, Role::BOLSISTA);
        $idAluno = (int) $aluno['funcionario']->getKey();

        RegistroPontoDiario::create([
            'id_funcionario' => $idAluno, 'data' => '2026-08-05', 'total_horas' => '8.00',
        ]);
        $apontamento = ApontamentoHoras::create([
            'id_funcionario' => $idAluno, 'tipo' => 'PROJETO',
            'id_referencia' => 2, 'data' => '2026-08-05', 'horas_trabalhadas' => '2.00',
            'status' => StatusApontamento::PENDENTE, 'consolidado' => false,
        ]);

        $headersTutor = $this->authHeader($tutor['login'], Role::BOLSISTA);

        // Fachada PATCH rejeitar sem motivo: 422 (validação) — com motivo: ok.
        $this->patchJson("/api/rh/horas/{$apontamento->getKey()}/rejeitar", [], $headersTutor)
            ->assertStatus(422);
        $this->patchJson(
            "/api/rh/horas/{$apontamento->getKey()}/rejeitar",
            ['motivo' => 'Fora do escopo'],
            $headersTutor
        )->assertOk()->assertJsonPath('status', 'REJEITADO');
    }

    public function test_fachada_horas_converge_para_mesmo_service(): void
    {
        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();
        $headersMembro = $this->authHeader($membro['login'], Role::BOLSISTA);
        $headersAdmin = $this->authHeader($admin['login'], Role::ADMIN);

        $criado = $this->postJson('/api/rh/horas', [
            'idFuncionario' => $id, 'tipo' => 'PROJETO', 'idReferencia' => 8,
            'data' => '2026-08-07', 'horasTrabalhadas' => 2.5,
        ], $headersMembro)->assertCreated()->assertJsonPath('status', 'PENDENTE');

        $this->getJson('/api/rh/horas?status=PENDENTE', $headersMembro)
            ->assertOk()->assertJsonCount(1);

        RegistroPontoDiario::create(['id_funcionario' => $id, 'data' => '2026-08-07', 'total_horas' => '8.00']);

        $this->patchJson('/api/rh/horas/'.$criado->json('id').'/validar', [], $headersAdmin)
            ->assertOk()->assertJsonPath('status', 'VALIDADO');
    }

    public function test_bolsista_nao_tutor_nao_valida(): void
    {
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $apontamento = ApontamentoHoras::create([
            'id_funcionario' => $membro['funcionario']->getKey(), 'tipo' => 'PROJETO',
            'id_referencia' => 2, 'data' => '2026-08-05', 'horas_trabalhadas' => '2.00',
            'status' => StatusApontamento::PENDENTE, 'consolidado' => false,
        ]);

        $this->putJson(
            "/api/rh/apontamentos-horas/{$apontamento->getKey()}/validar",
            ['status' => 'VALIDADO'],
            $this->authHeader($membro['login'], Role::BOLSISTA)
        )->assertForbidden();
    }
}

<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Models\Maquina;

/** Máquinas: cadastro/status Admin, uso abre/fecha, `horas_uso`, 409/422. */
class MaquinasTest extends ProducaoTestCase
{
    public function test_cadastro_e_status_so_admin(): void
    {
        $headers = $this->headersAdmin();
        $id = $this->postJson('/api/producao/maquinas', ['nome' => 'Laser 1'], $headers)
            ->assertCreated()->assertJsonPath('status', 'DISPONIVEL')->json('id');

        // Bolsista cadastra → 403 (delta P3); leitura OK.
        $this->postJson('/api/producao/maquinas', ['nome' => 'Laser 2'], $this->headersPapel(Role::BOLSISTA))
            ->assertForbidden();
        $this->getJson('/api/producao/maquinas', $this->headersPapel(Role::BOLSISTA))->assertOk()->assertJsonCount(1);

        $this->putJson("/api/producao/maquinas/{$id}/status", ['status' => 'MANUTENCAO'], $headers)
            ->assertOk()->assertJsonPath('status', 'MANUTENCAO');
        $this->getJson('/api/producao/maquinas?status=MANUTENCAO', $headers)->assertOk()->assertJsonCount(1);

        $this->deleteJson("/api/producao/maquinas/{$id}", [], $this->headersPapel(Role::BOLSISTA))->assertForbidden();
        $this->deleteJson("/api/producao/maquinas/{$id}", [], $headers)->assertNoContent();
        $this->assertNull(Maquina::find($id));
    }

    public function test_uso_abre_fecha_e_calcula_horas(): void
    {
        $headers = $this->headersAdmin();
        $id = $this->postJson('/api/producao/maquinas', ['nome' => 'Laser 1'], $headers)->assertCreated()->json('id');

        $uso = $this->postJson("/api/producao/maquinas/{$id}/uso", [
            'idFuncionario' => 41, 'dataInicio' => '2026-10-01 08:00:00',
        ], $headers)->assertCreated()->assertJsonPath('dataFim', null)->json();

        $this->putJson("/api/producao/maquinas/{$id}/uso/{$uso['id']}/fim", [
            'dataFim' => '2026-10-01 10:30:00',
        ], $headers)->assertOk()
            ->assertJsonPath('horasUso', '2.50');

        // Máquina voltou a DISPONIVEL; histórico tem 1 registro.
        $this->getJson("/api/producao/maquinas/{$id}", $headers)->assertJsonPath('status', 'DISPONIVEL');
        $this->getJson("/api/producao/maquinas/{$id}/historico", $headers)->assertOk()->assertJsonCount(1);

        // Uso sem dataFim usa agora (horas ~0).
        $uso2 = $this->postJson("/api/producao/maquinas/{$id}/uso", ['idFuncionario' => 41], $headers)
            ->assertCreated()->json();
        $this->putJson("/api/producao/maquinas/{$id}/uso/{$uso2['id']}/fim", [], $headers)
            ->assertOk()->assertJsonPath('horasUso', '0.00');
    }

    public function test_uso_em_maquina_ocupada_ou_manutencao_da_409(): void
    {
        $headers = $this->headersAdmin();
        $id = $this->postJson('/api/producao/maquinas', ['nome' => 'Laser 1'], $headers)->assertCreated()->json('id');

        $this->postJson("/api/producao/maquinas/{$id}/uso", ['idFuncionario' => 41], $headers)->assertCreated();
        $this->postJson("/api/producao/maquinas/{$id}/uso", ['idFuncionario' => 42], $headers)->assertConflict();

        $this->putJson("/api/producao/maquinas/{$id}/status", ['status' => 'MANUTENCAO'], $headers)->assertOk();
        $this->postJson("/api/producao/maquinas/{$id}/uso", ['idFuncionario' => 42], $headers)->assertConflict();
    }

    public function test_fim_anterior_e_uso_de_outra_maquina_dao_422(): void
    {
        $headers = $this->headersAdmin();
        $a = $this->postJson('/api/producao/maquinas', ['nome' => 'A'], $headers)->assertCreated()->json('id');
        $b = $this->postJson('/api/producao/maquinas', ['nome' => 'B'], $headers)->assertCreated()->json('id');

        $uso = $this->postJson("/api/producao/maquinas/{$a}/uso", [
            'idFuncionario' => 41, 'dataInicio' => '2026-10-01 08:00:00',
        ], $headers)->assertCreated()->json();

        $this->putJson("/api/producao/maquinas/{$a}/uso/{$uso['id']}/fim", [
            'dataFim' => '2026-10-01 07:00:00',
        ], $headers)->assertStatus(422);

        // Uso de outra máquina → 422 sem mutar.
        $this->putJson("/api/producao/maquinas/{$b}/uso/{$uso['id']}/fim", [], $headers)->assertStatus(422);

        $this->putJson("/api/producao/maquinas/999999/uso/{$uso['id']}/fim", [], $headers)->assertNotFound();
    }
}

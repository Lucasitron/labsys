<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Models\Tarefa;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use Illuminate\Support\Facades\Event;

/** Cobertura dos fluxos restantes: update/status/destroy de tarefa, mesa e máquina, destroy do kanban. */
class FluxosRestantesTest extends ProducaoTestCase
{
    public function test_tarefa_update_status_destroy(): void
    {
        $dono = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($dono, Role::BOLSISTA);
        $projeto = $this->criarProjeto($dono->id_user);
        $projeto2 = $this->criarProjeto($dono->id_user, ['nome' => 'Outro']);

        $id = $this->postJson('/api/producao/tarefas', [
            'idProjeto' => $projeto->getKey(), 'titulo' => 'Cortar', 'idResponsavel' => $dono->id_user,
        ], $headers)->assertCreated()->json('id');

        $this->getJson("/api/producao/tarefas/{$id}", $headers)->assertOk()->assertJsonPath('titulo', 'Cortar');

        $this->putJson("/api/producao/tarefas/{$id}", [
            'idProjeto' => $projeto2->getKey(), 'titulo' => 'Cortar MDF', 'prioridade' => 'ALTA',
        ], $headers)->assertOk()->assertJsonPath('titulo', 'Cortar MDF')->assertJsonPath('prioridade', 'ALTA');

        $this->putJson("/api/producao/tarefas/{$id}/status", ['status' => 'EM_ANDAMENTO'], $headers)
            ->assertOk()->assertJsonPath('status', 'EM_ANDAMENTO');
        $this->putJson("/api/producao/tarefas/{$id}/status",
            ['status' => 'CONCLUIDA', 'dataConclusao' => '2026-10-06'], $headers)
            ->assertOk()->assertJsonPath('dataConclusao', '2026-10-06');

        $this->deleteJson("/api/producao/tarefas/{$id}", [], $headers)->assertNoContent();
        $this->assertNull(Tarefa::find($id));
        $this->getJson("/api/producao/tarefas/{$id}", $headers)->assertNotFound();
    }

    public function test_mesa_update_destroy_e_maquina_update(): void
    {
        $dono = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($dono, Role::BOLSISTA);
        $admin = $this->headersAdmin();

        $id = $this->postJson('/api/producao/projetos-mesa', [
            'idFuncionario' => $dono->id_user, 'idMesa' => 7, 'nomeProjeto' => 'Drone',
        ], $headers)->assertCreated()->json('id');

        $this->putJson("/api/producao/projetos-mesa/{$id}", [
            'idFuncionario' => $dono->id_user, 'idMesa' => 9, 'nomeProjeto' => 'Drone v2',
        ], $headers)->assertOk()->assertJsonPath('nomeProjeto', 'Drone v2')->assertJsonPath('idMesa', 9);
        $this->deleteJson("/api/producao/projetos-mesa/{$id}", [], $headers)->assertNoContent();

        $maq = $this->postJson('/api/producao/maquinas', ['nome' => 'Laser'], $admin)->assertCreated()->json('id');
        $this->putJson("/api/producao/maquinas/{$maq}", ['nome' => 'Laser Pro', 'localizacao' => 'Sala 2'], $admin)
            ->assertOk()->assertJsonPath('nome', 'Laser Pro');
    }

    public function test_kanban_destroy(): void
    {
        Event::fake([EncomendaCriadaEvent::class]);
        $admin = $this->headersAdmin();
        $idEncomenda = $this->criarEncomendaVendas($admin);

        $id = $this->postJson('/api/producao/kanban', ['idEncomenda' => $idEncomenda], $admin)
            ->assertCreated()->json('id');

        $this->deleteJson("/api/producao/kanban/{$id}", [], $admin)->assertNoContent();
        $this->getJson("/api/producao/kanban/encomenda/{$idEncomenda}", $admin)->assertNotFound();
    }
}

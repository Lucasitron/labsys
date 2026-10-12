<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Models\Projeto;

/** Projetos/tarefas: vínculo do responsável, carimbos, `PATCH` próprio. */
class ProjetosTarefasTest extends ProducaoTestCase
{
    public function test_bolsista_cria_so_proprio_projeto_e_admin_tudo(): void
    {
        $bolsista = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($bolsista, Role::BOLSISTA);

        $payload = [
            'nome' => 'Mesa CNC', 'dataInicio' => today()->toDateString(),
            'idResponsavel' => $bolsista->id_user,
        ];

        $this->postJson('/api/producao/projetos', $payload, $headers)
            ->assertCreated()->assertJsonPath('status', 'PLANEJADO')
            ->assertJsonPath('idResponsavel', $bolsista->id_user);

        $this->postJson('/api/producao/projetos', array_merge($payload, ['idResponsavel' => 9999]), $headers)
            ->assertForbidden();

        $this->postJson('/api/producao/projetos', ['nome' => 'X'], $headers)->assertStatus(422);
    }

    public function test_concluir_projeto_carimba_data_e_listar_filtra(): void
    {
        $headers = $this->headersAdmin();
        $id = $this->postJson('/api/producao/projetos', [
            'nome' => 'Mesa CNC', 'dataInicio' => today()->toDateString(), 'idResponsavel' => 11,
        ], $headers)->assertCreated()->json('id');

        $this->putJson("/api/producao/projetos/{$id}/status", ['status' => 'CONCLUIDO'], $headers)
            ->assertOk()->assertJsonPath('dataFimReal', today()->toDateString());

        $this->getJson('/api/producao/projetos?status=CONCLUIDO', $headers)
            ->assertOk()->assertJsonCount(1);
        $this->getJson('/api/producao/projetos?status=PLANEJADO', $headers)->assertOk()->assertJsonCount(0);
        $this->getJson('/api/producao/projetos?idResponsavel=11', $headers)->assertOk()->assertJsonCount(1);
    }

    public function test_tarefa_exige_projeto_e_patch_so_responsavel(): void
    {
        $bolsista = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($bolsista, Role::BOLSISTA);
        $projeto = $this->criarProjeto($bolsista->id_user);

        $tarefaId = $this->postJson('/api/producao/tarefas', [
            'idProjeto' => $projeto->getKey(), 'titulo' => 'Cortar MDF', 'idResponsavel' => $bolsista->id_user,
        ], $headers)->assertCreated()
            ->assertJsonPath('status', 'PENDENTE')
            ->assertJsonPath('prioridade', 'MEDIA')
            ->json('id');

        // Projeto alheio → 403.
        $outro = $this->loginPapel(Role::BOLSISTA);
        $headersOutro = $this->authHeader($outro, Role::BOLSISTA);
        $this->postJson('/api/producao/tarefas', [
            'idProjeto' => $projeto->getKey(), 'titulo' => 'X',
        ], $headersOutro)->assertForbidden();

        // PATCH próprio conclui; repetido = no-op 200; alheio = 403.
        $this->patchJson("/api/producao/tarefas/{$tarefaId}", [], $headers)
            ->assertOk()->assertJsonPath('status', 'CONCLUIDA');
        $this->patchJson("/api/producao/tarefas/{$tarefaId}", [], $headers)
            ->assertOk()->assertJsonPath('status', 'CONCLUIDA');
        $this->patchJson("/api/producao/tarefas/{$tarefaId}", [], $headersOutro)->assertForbidden();

        // Filtros.
        $this->getJson('/api/producao/tarefas?status=CONCLUIDA', $headers)->assertOk()->assertJsonCount(1);
        $this->getJson("/api/producao/tarefas?idProjeto={$projeto->getKey()}", $headers)->assertOk()->assertJsonCount(1);
    }

    public function test_atualizar_e_remover_exigem_responsavel_do_projeto(): void
    {
        $dono = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($dono, Role::BOLSISTA);
        $projeto = $this->criarProjeto($dono->id_user);

        $alheio = $this->headersPapel(Role::BOLSISTA);
        $payload = ['nome' => 'Outro', 'dataInicio' => today()->toDateString(), 'idResponsavel' => $dono->id_user];

        $this->putJson("/api/producao/projetos/{$projeto->getKey()}", $payload, $alheio)->assertForbidden();
        $this->deleteJson("/api/producao/projetos/{$projeto->getKey()}", [], $alheio)->assertForbidden();

        $this->putJson("/api/producao/projetos/{$projeto->getKey()}", $payload, $headers)
            ->assertOk()->assertJsonPath('nome', 'Outro');
        $this->deleteJson("/api/producao/projetos/{$projeto->getKey()}", [], $headers)->assertNoContent();
        $this->assertNull(Projeto::find($projeto->getKey()));
    }
}

<?php

namespace Tests\Feature\Dashboard;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Enums\MaquinaStatus;
use App\Modules\Producao\Enums\TarefaStatus;

/** Resumo agregado (401/403, filtro restricted por papel, aliases, sem regressão). */
class DashboardTest extends DashboardTestCase
{
    public function test_sem_token_401_em_summary_resumo_e_tasks(): void
    {
        $this->getJson('/api/dashboard/summary')->assertUnauthorized();
        $this->getJson('/api/dashboard/resumo')->assertUnauthorized();
        $this->patchJson('/api/tasks/1', ['done' => true])->assertUnauthorized();
    }

    public function test_recrutando_403_no_modulo(): void
    {
        $headers = $this->headersPapel(Role::RECRUTANDO);

        $this->getJson('/api/dashboard/summary', $headers)->assertForbidden();
        $this->getJson('/api/dashboard/resumo', $headers)->assertForbidden();
        $this->patchJson('/api/tasks/999', ['done' => true], $headers)->assertForbidden();
    }

    public function test_admin_shape_completo_e_resumo_equivalente(): void
    {
        $admin = $this->adminHeaders();
        $idAdmin = $admin['login']->id_user;

        $projeto = $this->criarProjeto($idAdmin);
        $tarefa = $this->criarTarefa($projeto->getKey(), $idAdmin);
        $this->criarCartao(101, KanbanStatus::FILA);
        $this->criarCartao(102, KanbanStatus::PRODUCAO);
        $this->criarCartao(103, KanbanStatus::ENTREGUE);
        $this->criarMaquina(MaquinaStatus::DISPONIVEL);
        $this->criarMaquina(MaquinaStatus::MANUTENCAO);
        $this->criarEmprestimoAberto();
        $solicitacao = $this->criarSolicitacao($admin['funcionario']->getKey());
        $this->criarEmitido($solicitacao);
        $this->criarNotificacaoNaoLida($idAdmin);

        $resumo = $this->getJson('/api/dashboard/summary', $admin['headers'])->assertOk()->json();

        $this->assertCount(1, $resumo['tasks']);
        $this->assertSame((string) $tarefa->getKey(), $resumo['tasks'][0]['id']);
        $this->assertSame('producao', $resumo['tasks'][0]['module']);
        $this->assertSame('Hoje', $resumo['tasks'][0]['dueLabel']);
        $this->assertTrue($resumo['tasks'][0]['urgent']);

        $this->assertSame(2, $resumo['kpis']['ordersActive']['value']);
        $this->assertSame(1, $resumo['kpis']['notificationsUnread']['value']);
        $this->assertSame(1, $resumo['kpis']['loansOpen']['value']);
        $this->assertTrue($resumo['kpis']['loansOpen']['restricted']);
        $this->assertSame(1, $resumo['kpis']['machinesActive']['value']);
        $this->assertSame(2, $resumo['kpis']['machinesActive']['total']);

        $this->assertCount(5, $resumo['ordersByStatus']);
        $this->assertSame(1, $resumo['ordersByStatus'][0]['count']);
        $this->assertCount(2, $resumo['machinesByStatus']);

        $ids = array_column($resumo['activity'], 'id');
        $this->assertContains('act-sol-'.$solicitacao->getKey(), $ids);
        $this->assertContains('act-emit-'.$solicitacao->getKey(), $ids);

        $alias = $this->getJson('/api/dashboard/resumo', $admin['headers'])->assertOk()->json();
        $this->assertSame($resumo, $alias);
    }

    public function test_bolsista_voluntario_estagiario_200_restrito_e_so_proprio(): void
    {
        foreach ([Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO] as $role) {
            $meu = $this->funcionarioHeaders($role);
            $outro = $this->funcionarioHeaders($role);

            $projeto = $this->criarProjeto($meu['login']->id_user);
            $this->criarTarefa($projeto->getKey(), $meu['login']->id_user);
            $this->criarTarefa($projeto->getKey(), $outro['login']->id_user, ['titulo' => 'Alheia']);
            $this->criarSolicitacao($meu['funcionario']->getKey());
            $this->criarSolicitacao($outro['funcionario']->getKey());

            $resumo = $this->getJson('/api/dashboard/summary', $meu['headers'])->assertOk()->json();

            $this->assertArrayNotHasKey('loansOpen', $resumo['kpis']);
            $this->assertArrayNotHasKey('machinesActive', $resumo['kpis']);
            $this->assertSame([], $resumo['machinesByStatus']);
            $this->assertCount(1, $resumo['tasks']);
            $this->assertCount(1, $resumo['activity']);
            $this->assertArrayHasKey('ordersActive', $resumo['kpis']);
            $this->assertArrayHasKey('notificationsUnread', $resumo['kpis']);
        }
    }

    public function test_patch_tasks_alias_conclui_equivale_a_rota_producao(): void
    {
        $dono = $this->funcionarioHeaders(Role::BOLSISTA);
        $projeto = $this->criarProjeto($dono['login']->id_user);
        $viaAlias = $this->criarTarefa($projeto->getKey(), $dono['login']->id_user);
        $viaProducao = $this->criarTarefa($projeto->getKey(), $dono['login']->id_user, ['titulo' => 'Outra']);

        $alias = $this->patchJson("/api/tasks/{$viaAlias->getKey()}", ['done' => true], $dono['headers'])
            ->assertOk()->json();
        $direta = $this->patchJson("/api/producao/tarefas/{$viaProducao->getKey()}", [], $dono['headers'])
            ->assertOk()->json();

        $this->assertSame($viaAlias->getKey(), $alias['id']);
        $this->assertSame(array_keys($direta), array_keys($alias));
        $this->assertSame(TarefaStatus::CONCLUIDA->value, $viaAlias->refresh()->status->value);

        $resumo = $this->getJson('/api/dashboard/summary', $dono['headers'])->assertOk()->json();
        $this->assertSame([], $resumo['tasks']);
    }

    public function test_patch_tasks_403_alheio_e_idempotente(): void
    {
        $dono = $this->funcionarioHeaders(Role::BOLSISTA);
        $alheio = $this->funcionarioHeaders(Role::BOLSISTA);
        $projeto = $this->criarProjeto($dono['login']->id_user);
        $tarefa = $this->criarTarefa($projeto->getKey(), $dono['login']->id_user);

        $this->patchJson("/api/tasks/{$tarefa->getKey()}", ['done' => true], $alheio['headers'])->assertForbidden();

        $this->patchJson("/api/tasks/{$tarefa->getKey()}", ['done' => true], $dono['headers'])->assertOk();
        $this->patchJson("/api/tasks/{$tarefa->getKey()}", ['done' => true], $dono['headers'])->assertOk();
        $this->assertSame(TarefaStatus::CONCLUIDA->value, $tarefa->refresh()->status->value);
    }
}

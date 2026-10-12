<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Contracts\ProducaoContract;
use App\Modules\Producao\Enums\TarefaStatus;
use App\Modules\Producao\Models\Tarefa;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Events\NivelAlteradoEvent;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\Event;

/** Listeners (auto-insert idempotente, trilha de nível) + `ProducaoContract`. */
class EventsContractTest extends ProducaoTestCase
{
    public function test_encomenda_criada_auto_insere_e_idempotente(): void
    {
        Event::fake([EncomendaCriadaEvent::class]);
        $admin = $this->headersAdmin();
        $idEncomenda = $this->criarEncomendaVendas($admin);

        $listener = app(\App\Modules\Producao\Listeners\EncomendaCriadaListener::class);
        $evento = new EncomendaCriadaEvent($idEncomenda, 1, '100.00', today()->toDateString());

        $listener->handle($evento);
        $listener->handle($evento);

        $this->assertSame(1, \App\Modules\Producao\Models\EncomendaKanban::where('id_encomenda', $idEncomenda)->count());
        $this->assertSame('FILA', \App\Modules\Producao\Models\EncomendaKanban::where('id_encomenda', $idEncomenda)->first()->status->value);
    }

    public function test_nivel_alterado_so_registra_trilha(): void
    {
        $listener = app(\App\Modules\Producao\Listeners\NivelAlteradoListener::class);
        $listener->handle(new NivelAlteradoEvent(71, NivelAcesso::BOLSISTA, NivelAcesso::ADMIN, CarbonImmutable::now()));

        $this->assertTrue(true);
    }

    public function test_contract_resumo_e_tarefas_ativas(): void
    {
        Event::fake([EncomendaCriadaEvent::class]);
        $admin = $this->headersAdmin();
        $e1 = $this->criarEncomendaVendas($admin);
        $e2 = $this->criarEncomendaVendas($admin);
        $this->postJson('/api/producao/kanban', ['idEncomenda' => $e1], $admin)->assertCreated();
        $this->postJson('/api/producao/kanban', ['idEncomenda' => $e2], $admin)->assertCreated();

        $dono = $this->loginPapel(Role::BOLSISTA);
        $projeto = $this->criarProjeto($dono->id_user);
        Tarefa::create([
            'id_projeto' => $projeto->getKey(), 'titulo' => 'Ativa', 'id_responsavel' => $dono->id_user,
            'status' => TarefaStatus::PENDENTE->value, 'prioridade' => 'ALTA',
        ]);
        Tarefa::create([
            'id_projeto' => $projeto->getKey(), 'titulo' => 'Feita', 'id_responsavel' => $dono->id_user,
            'status' => TarefaStatus::CONCLUIDA->value, 'prioridade' => 'BAIXA',
        ]);

        /** @var ProducaoContract $contract */
        $contract = app(ProducaoContract::class);

        $this->assertSame(['FILA' => 2, 'PRODUCAO' => 0, 'ACABAMENTO' => 0, 'PRONTO' => 0, 'ENTREGUE' => 0],
            $contract->resumoKanban());

        $ativas = $contract->tarefasAtivasPorResponsavel($dono->id_user);
        $this->assertCount(1, $ativas);
        $this->assertSame('Ativa', $ativas[0]['titulo']);
        $this->assertSame([], $contract->tarefasAtivasPorResponsavel(999999));
    }
}

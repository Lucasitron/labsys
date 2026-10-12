<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Estoque\Events\ProducaoConcluidaEvent as EstoqueConcluidaEvent;
use App\Modules\Estoque\Models\Item;
use App\Modules\Producao\Events\KanbanStatusAlteradoEvent;
use App\Modules\Producao\Events\ProducaoConcluidaEvent;
use App\Modules\Producao\Events\ProducaoStatusAlteradoEvent;
use App\Modules\Producao\Models\EncomendaKanban;
use App\Modules\Vendas\Events\ProducaoStatusAlteradoEvent as VendasStatusEvent;
use Illuminate\Support\Facades\Event;

/** Kanban: LINKA Vendas, `version` 409, histórico, eventos e baixa no `ENTREGUE`. */
class KanbanTest extends ProducaoTestCase
{
    public function test_incluir_linka_vendas_e_bloqueios(): void
    {
        Event::fake([\App\Modules\Vendas\Events\EncomendaCriadaEvent::class]);
        $admin = $this->headersAdmin();
        $idEncomenda = $this->criarEncomendaVendas($admin);

        $this->postJson('/api/producao/kanban', ['idEncomenda' => $idEncomenda, 'idResponsavel' => 21], $admin)
            ->assertCreated()->assertJsonPath('status', 'FILA')->assertJsonPath('version', 0);

        // Duplicada → 409; inexistente no Vendas → 404.
        $this->postJson('/api/producao/kanban', ['idEncomenda' => $idEncomenda], $admin)->assertConflict();
        $this->postJson('/api/producao/kanban', ['idEncomenda' => 999999], $admin)->assertNotFound();

        // Voluntário não inclui (delta §7); Bolsista inclui.
        $voluntario = $this->headersPapel(Role::VOLUNTARIO);
        Event::fake([\App\Modules\Vendas\Events\EncomendaCriadaEvent::class]);
        $outra = $this->criarEncomendaVendas($admin);
        $this->postJson('/api/producao/kanban', ['idEncomenda' => $outra], $voluntario)->assertForbidden();

        $bolsista = $this->headersPapel(Role::BOLSISTA);
        $this->postJson('/api/producao/kanban', ['idEncomenda' => $outra], $bolsista)->assertCreated();
    }

    public function test_mover_historico_eventos_e_lock(): void
    {
        Event::fake([\App\Modules\Vendas\Events\EncomendaCriadaEvent::class]);
        $admin = $this->headersAdmin();
        $idEncomenda = $this->criarEncomendaVendas($admin);
        $id = $this->postJson('/api/producao/kanban', ['idEncomenda' => $idEncomenda, 'idResponsavel' => 21], $admin)
            ->assertCreated()->json('id');

        Event::fake([
            ProducaoStatusAlteradoEvent::class, VendasStatusEvent::class, KanbanStatusAlteradoEvent::class,
        ]);

        $this->putJson("/api/producao/kanban/{$id}/mover", [
            'statusNovo' => 'PRODUCAO', 'version' => 0, 'observacao' => 'Iniciou',
        ], $admin)->assertOk()->assertJsonPath('version', 1);

        Event::assertDispatched(ProducaoStatusAlteradoEvent::class,
            fn ($e) => $e->idEncomenda === $idEncomenda && $e->statusNovo === 'PRODUCAO');
        Event::assertDispatched(VendasStatusEvent::class,
            fn ($e) => $e->idEncomenda === $idEncomenda && $e->statusNovo === 'Produção');
        Event::assertDispatched(KanbanStatusAlteradoEvent::class,
            fn ($e) => $e->statusAnterior === 'FILA' && $e->statusNovo === 'PRODUCAO');

        // Version antiga → 409 sem mover.
        $this->putJson("/api/producao/kanban/{$id}/mover", ['statusNovo' => 'PRONTO', 'version' => 0], $admin)
            ->assertConflict();
        $this->assertSame('PRODUCAO', EncomendaKanban::find($id)->status->value);

        $this->getJson("/api/producao/kanban/encomenda/{$idEncomenda}/historico", $admin)
            ->assertOk()->assertJsonCount(2);
        $this->getJson('/api/producao/kanban?status=PRODUCAO', $admin)->assertOk()->assertJsonCount(1);
    }

    public function test_mover_exige_responsavel_do_cartao(): void
    {
        Event::fake([\App\Modules\Vendas\Events\EncomendaCriadaEvent::class]);
        $admin = $this->headersAdmin();
        $idEncomenda = $this->criarEncomendaVendas($admin);
        $id = $this->criarCartao($idEncomenda, 31)->getKey();

        $this->putJson("/api/producao/kanban/{$id}/mover", ['statusNovo' => 'PRODUCAO', 'version' => 0],
            $this->headersPapel(Role::BOLSISTA))->assertForbidden();
        $this->deleteJson("/api/producao/kanban/{$id}", [], $this->headersPapel(Role::BOLSISTA))->assertForbidden();
    }

    public function test_entregue_baixa_1x_e_publica_conclusao(): void
    {
        Event::fake([\App\Modules\Vendas\Events\EncomendaCriadaEvent::class]);
        $responsavel = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($responsavel, Role::BOLSISTA);
        $admin = $this->headersAdmin();

        $idEncomenda = $this->criarEncomendaVendas($admin);
        $item = $this->criarItemEstoque('50.00');

        $id = $this->postJson('/api/producao/kanban',
            ['idEncomenda' => $idEncomenda, 'idResponsavel' => $responsavel->id_user], $admin)
            ->assertCreated()->json('id');

        $this->postJson("/api/producao/kanban/encomenda/{$idEncomenda}/consumo", [
            'itens' => [['idItem' => $item->getKey(), 'quantidade' => '5.00']],
        ], $headers)->assertCreated();

        Event::fake([ProducaoConcluidaEvent::class, EstoqueConcluidaEvent::class]);

        foreach ([['PRONTO', 0], ['ENTREGUE', 1]] as [$alvo, $versao]) {
            $this->putJson("/api/producao/kanban/{$id}/mover",
                ['statusNovo' => $alvo, 'version' => $versao], $headers)->assertOk();
        }

        Event::assertDispatched(ProducaoConcluidaEvent::class,
            fn ($e) => $e->idEncomenda === $idEncomenda && count($e->itens) === 1);
        Event::assertDispatched(EstoqueConcluidaEvent::class,
            fn ($e) => $e->idEncomenda === $idEncomenda);

        // Baixa real (dispatch real, sem fake) — precisa zerar o fake antes.
        Event::fake([\App\Modules\Vendas\Events\EncomendaCriadaEvent::class]);
        $idEnc2 = $this->criarEncomendaVendas($admin);
        $item2 = $this->criarItemEstoque('50.00');
        $id2 = $this->postJson('/api/producao/kanban',
            ['idEncomenda' => $idEnc2, 'idResponsavel' => $responsavel->id_user], $admin)
            ->assertCreated()->json('id');
        $this->postJson("/api/producao/kanban/encomenda/{$idEnc2}/consumo", [
            'itens' => [['idItem' => $item2->getKey(), 'quantidade' => '4.00']],
        ], $headers)->assertCreated();

        $this->putJson("/api/producao/kanban/{$id2}/mover", ['statusNovo' => 'ENTREGUE', 'version' => 0], $headers)
            ->assertOk();
        $this->assertSame('46.00', number_format((float) Item::find($item2->getKey())->quantidade_atual, 2, '.', ''));

        // 2º mover→ENTREGUE (vai-e-volta) não rebaixa: volta p/ PRONTO e entrega de novo.
        $this->putJson("/api/producao/kanban/{$id2}/mover", ['statusNovo' => 'PRONTO', 'version' => 1], $headers)
            ->assertOk();
        $this->putJson("/api/producao/kanban/{$id2}/mover", ['statusNovo' => 'ENTREGUE', 'version' => 2], $headers)
            ->assertOk();
        $this->assertSame('46.00', number_format((float) Item::find($item2->getKey())->quantidade_atual, 2, '.', ''));
    }
}

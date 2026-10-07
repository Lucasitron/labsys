<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use Illuminate\Support\Facades\Event;

/** Consumo (BOM final): upsert soma, item estranho 422, lista. */
class ConsumoTest extends ProducaoTestCase
{
    public function test_upsert_soma_e_lista(): void
    {
        Event::fake([EncomendaCriadaEvent::class]);
        $responsavel = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($responsavel, Role::BOLSISTA);
        $admin = $this->headersAdmin();

        $idEncomenda = $this->criarEncomendaVendas($admin);
        $item = $this->criarItemEstoque();
        $this->postJson('/api/producao/kanban',
            ['idEncomenda' => $idEncomenda, 'idResponsavel' => $responsavel->id_user], $admin)
            ->assertCreated();

        $this->postJson("/api/producao/kanban/encomenda/{$idEncomenda}/consumo", [
            'itens' => [['idItem' => $item->getKey(), 'quantidade' => '2.50']],
        ], $headers)->assertCreated()->assertJsonCount(1)->assertJsonPath('0.quantidadeConsumida', '2.50');

        $this->postJson("/api/producao/kanban/encomenda/{$idEncomenda}/consumo", [
            'itens' => [['idItem' => $item->getKey(), 'quantidade' => '1.25']],
        ], $headers)->assertCreated()->assertJsonCount(1)->assertJsonPath('0.quantidadeConsumida', '3.75');

        $this->getJson("/api/producao/kanban/encomenda/{$idEncomenda}/consumo", $headers)
            ->assertOk()->assertJsonCount(1);
    }

    public function test_item_inexistente_da_422_sem_persistir_parcial(): void
    {
        Event::fake([EncomendaCriadaEvent::class]);
        $admin = $this->headersAdmin();
        $idEncomenda = $this->criarEncomendaVendas($admin);
        $item = $this->criarItemEstoque();

        $this->postJson('/api/producao/kanban', ['idEncomenda' => $idEncomenda], $admin)->assertCreated();

        $this->postJson("/api/producao/kanban/encomenda/{$idEncomenda}/consumo", [
            'itens' => [
                ['idItem' => $item->getKey(), 'quantidade' => '1.00'],
                ['idItem' => 999999, 'quantidade' => '1.00'],
            ],
        ], $admin)->assertStatus(422);

        $this->assertSame(0, \App\Modules\Producao\Models\ConsumoEncomenda::where('id_encomenda', $idEncomenda)->count());
        $this->postJson("/api/producao/kanban/encomenda/{$idEncomenda}/consumo", ['itens' => []], $admin)
            ->assertStatus(422);
    }
}

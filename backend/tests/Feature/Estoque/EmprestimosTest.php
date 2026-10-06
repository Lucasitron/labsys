<?php

namespace Tests\Feature\Estoque;

use App\Modules\Auth\Enums\Role;
use App\Modules\Estoque\Enums\StatusEmprestimo;
use App\Modules\Estoque\Events\EmprestimoAtrasadoEvent;
use App\Modules\Estoque\Models\Emprestimo;
use App\Modules\Estoque\Models\Item;
use App\Modules\Estoque\Models\SaidaEstoque;
use App\Modules\Estoque\Services\EmprestimoService;
use Carbon\Carbon;
use Illuminate\Support\Facades\Event;

class EmprestimosTest extends EstoqueTestCase
{
    public function test_criar_para_si_baixa_estoque_e_gera_saida_emprestimo(): void
    {
        ['login' => $login, 'headers' => $headers] = $this->responsavel();
        $item = $this->criarItem(['quantidade_atual' => '10.00']);

        $id = $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => $login->id_user,
            'quantidade' => '3.00',
            'dataDevolucaoPrevista' => Carbon::today()->addDays(5)->toDateString(),
        ], $headers)
            ->assertCreated()
            ->assertJsonPath('status', 'ATIVO')
            ->json('id');

        $this->assertSame('7.00', Item::find($item->getKey())->quantidade_atual);
        $this->assertTrue(SaidaEstoque::where('tipo_saida', 'EMPRESTIMO')
            ->where('id_referencia', $id)->exists());
    }

    public function test_nao_admin_nao_empresta_para_terceiro_e_prevista_passada_rejeita(): void
    {
        ['login' => $login, 'headers' => $headers] = $this->responsavel();
        $outro = $this->loginPapel(Role::BOLSISTA, 'ESTOQUE');
        $item = $this->criarItem();

        $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => $outro->id_user,
            'quantidade' => '1.00',
            'dataDevolucaoPrevista' => Carbon::today()->addDays(2)->toDateString(),
        ], $headers)->assertForbidden();

        // Admin empresta para outro.
        $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => $outro->id_user,
            'quantidade' => '1.00',
            'dataDevolucaoPrevista' => Carbon::today()->addDays(2)->toDateString(),
        ], $this->headersAdmin())->assertCreated();

        $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => $login->id_user,
            'quantidade' => '1.00',
            'dataDevolucaoPrevista' => Carbon::today()->subDay()->toDateString(),
        ], $headers)->assertStatus(422);
    }

    public function test_devolver_soma_saldo_e_redevolucao_da_400(): void
    {
        ['login' => $login, 'headers' => $headers] = $this->responsavel();
        $item = $this->criarItem(['quantidade_atual' => '10.00']);

        $id = $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => $login->id_user,
            'quantidade' => '4.00',
            'dataDevolucaoPrevista' => Carbon::today()->addDays(3)->toDateString(),
        ], $headers)->json('id');

        $this->putJson("/api/estoque/emprestimos/{$id}/devolucao", [], $headers)
            ->assertOk()->assertJsonPath('status', 'DEVOLVIDO');

        $this->assertSame('10.00', Item::find($item->getKey())->quantidade_atual);

        $this->putJson("/api/estoque/emprestimos/{$id}/devolucao", [], $headers)->assertStatus(400);

        // Terceiro não devolve o alheio.
        $outro = $this->responsavel();
        $id2 = $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => $outro['login']->id_user,
            'quantidade' => '1.00',
            'dataDevolucaoPrevista' => Carbon::today()->addDays(3)->toDateString(),
        ], $outro['headers'])->json('id');

        $this->putJson("/api/estoque/emprestimos/{$id2}/devolucao", [], $headers)->assertForbidden();
    }

    public function test_e7_admin_ve_todos_demais_so_proprios(): void
    {
        $dono = $this->responsavel();
        $alheio = $this->responsavel();
        $item = $this->criarItem();

        $futura = Carbon::today()->addDays(3)->toDateString();
        $payload = fn (int $pessoa) => [
            'idItem' => $item->getKey(), 'idPessoa' => $pessoa,
            'quantidade' => '1.00', 'dataDevolucaoPrevista' => $futura,
        ];

        $idAlheio = $this->postJson('/api/estoque/emprestimos', $payload($alheio['login']->id_user), $alheio['headers'])->json('id');
        $this->postJson('/api/estoque/emprestimos', $payload($dono['login']->id_user), $dono['headers'])->assertCreated();

        $this->getJson('/api/estoque/emprestimos', $dono['headers'])->assertOk()->assertJsonCount(1);
        $this->getJson('/api/estoque/emprestimos', $this->headersAdmin())->assertOk()->assertJsonCount(2);

        $this->getJson("/api/estoque/emprestimos/{$idAlheio}", $dono['headers'])->assertForbidden();
        $this->getJson("/api/estoque/emprestimos/{$idAlheio}", $this->headersAdmin())->assertOk();
    }

    public function test_status_ativos_atrasados_historico_e_invalido(): void
    {
        ['login' => $login, 'headers' => $headers] = $this->responsavel();
        $item = $this->criarItem();

        $vencido = Emprestimo::create([
            'id_item' => $item->getKey(), 'id_pessoa' => $login->id_user,
            'quantidade' => '1.00', 'data_emprestimo' => '2026-01-02',
            'data_devolucao_prevista' => Carbon::today()->subDays(2)->toDateString(),
            'status' => StatusEmprestimo::ATIVO,
        ]);

        $this->getJson('/api/estoque/emprestimos?status=ativos', $headers)
            ->assertOk()->assertJsonCount(1);
        $this->getJson('/api/estoque/emprestimos?status=atrasados', $headers)
            ->assertOk()->assertJsonCount(1);
        $this->getJson('/api/estoque/emprestimos?status=historico', $headers)
            ->assertOk()->assertJsonCount(0);
        $this->getJson('/api/estoque/emprestimos/atrasados', $headers)
            ->assertOk()->assertJsonPath('0.id', (int) $vencido->getKey());
        $this->getJson('/api/estoque/emprestimos?status=perdidos', $headers)->assertStatus(400);
    }

    public function test_verificar_atrasados_marca_so_vencido_ativo_e_publica_evento(): void
    {
        Event::fake([EmprestimoAtrasadoEvent::class]);

        $dono = $this->responsavel();
        $item = $this->criarItem();

        $ontem = Carbon::today()->subDay()->toDateString();
        $vencido = Emprestimo::create([
            'id_item' => $item->getKey(), 'id_pessoa' => $dono['login']->id_user,
            'quantidade' => '1.00', 'data_emprestimo' => $ontem,
            'data_devolucao_prevista' => $ontem, 'status' => StatusEmprestimo::ATIVO,
        ]);
        $devolvido = Emprestimo::create([
            'id_item' => $item->getKey(), 'id_pessoa' => $dono['login']->id_user,
            'quantidade' => '1.00', 'data_emprestimo' => $ontem,
            'data_devolucao_prevista' => $ontem, 'data_devolucao_real' => $ontem,
            'status' => StatusEmprestimo::DEVOLVIDO,
        ]);

        $this->assertSame(1, app(EmprestimoService::class)->verificarAtrasados());

        $this->assertSame('ATRASADO', Emprestimo::find($vencido->getKey())->status->value);
        $this->assertSame('DEVOLVIDO', Emprestimo::find($devolvido->getKey())->status->value);

        Event::assertDispatched(EmprestimoAtrasadoEvent::class, 1);
        Event::assertDispatched(EmprestimoAtrasadoEvent::class,
            fn ($e) => $e->payload()['idEmprestimo'] === (int) $vencido->getKey());
    }

    public function test_tomador_nome_real_e_fail_soft_pessoa_hash_id(): void
    {
        $admin = $this->headersAdmin();
        $item = $this->criarItem();
        $futura = Carbon::today()->addDays(3)->toDateString();

        $pessoa = $this->criarPessoa(['nome_completo' => 'Maria Silva']);

        $id = $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => $pessoa->getKey(),
            'quantidade' => '1.00', 'dataDevolucaoPrevista' => $futura,
        ], $admin)->assertCreated()->assertJsonPath('tomador', 'Maria Silva')->json('id');

        $this->getJson("/api/estoque/emprestimos/{$id}", $admin)
            ->assertOk()->assertJsonPath('tomador', 'Maria Silva');

        // RH sem a pessoa → rótulo estável, nunca 500.
        $fantasma = 987654321;
        $id2 = $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => $fantasma,
            'quantidade' => '1.00', 'dataDevolucaoPrevista' => $futura,
        ], $admin)->assertCreated()->assertJsonPath('tomador', "Pessoa #{$fantasma}")->json('id');

        $this->getJson("/api/estoque/emprestimos/{$id2}", $admin)
            ->assertOk()->assertJsonPath('tomador', "Pessoa #{$fantasma}");
    }

    public function test_tomador_fail_soft_quando_rh_lanca_erro(): void
    {
        $admin = $this->headersAdmin();
        $item = $this->criarItem();

        $id = Emprestimo::create([
            'id_item' => $item->getKey(), 'id_pessoa' => 555,
            'quantidade' => '1.00', 'data_emprestimo' => '2026-01-02',
            'data_devolucao_prevista' => Carbon::today()->addDays(2)->toDateString(),
            'status' => StatusEmprestimo::ATIVO,
        ])->getKey();

        $quebrado = \Mockery::mock(\App\Modules\Rh\Contracts\RhContract::class);
        $quebrado->shouldReceive('nomePessoa')->andThrow(new \RuntimeException('RH fora'));
        app()->instance(\App\Modules\Rh\Contracts\RhContract::class, $quebrado);

        try {
            $this->getJson("/api/estoque/emprestimos/{$id}", $admin)
                ->assertOk()->assertJsonPath('tomador', 'Pessoa #555');
        } finally {
            app()->forgetInstance(\App\Modules\Rh\Contracts\RhContract::class);
        }
    }
}

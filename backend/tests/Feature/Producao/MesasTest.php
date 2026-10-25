<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Events\ProjetoMesaAbandonadoEvent;
use App\Modules\Producao\Models\Parametro5S;
use App\Modules\Producao\Services\ProjetoMesaService;
use Illuminate\Support\Facades\Event;

/** Mesas: dono, evolução, QR string, auditoria, scheduler manual e parâmetros. */
class MesasTest extends ProducaoTestCase
{
    public function test_criar_dono_qr_e_evolucao_reativa(): void
    {
        $dono = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($dono, Role::BOLSISTA);

        $mesa = $this->postJson('/api/producao/projetos-mesa', [
            'idFuncionario' => $dono->id_user, 'idMesa' => 7, 'nomeProjeto' => 'Drone',
        ], $headers)->assertCreated()
            ->assertJsonPath('status', 'ATIVO')
            ->assertJsonPath('dataUltimaEvolucao', today()->toDateString())
            ->json();
        $this->assertSame("fablab://projeto-mesa/{$mesa['id']}", $mesa['qrCodeTotem']);

        // Outro não cria p/ o dono nem evolui; dono evolui.
        $this->postJson('/api/producao/projetos-mesa', [
            'idFuncionario' => $dono->id_user, 'idMesa' => 8, 'nomeProjeto' => 'X',
        ], $this->headersPapel(Role::BOLSISTA))->assertForbidden();
        $this->putJson("/api/producao/projetos-mesa/{$mesa['id']}/evolucao", [],
            $this->headersPapel(Role::BOLSISTA))->assertForbidden();
        $this->putJson("/api/producao/projetos-mesa/{$mesa['id']}/evolucao", [], $headers)->assertOk();

        $this->getJson("/api/producao/projetos-mesa/{$mesa['id']}/qrcode", $headers)
            ->assertOk()->assertJsonPath('conteudo', "fablab://projeto-mesa/{$mesa['id']}");
        $this->getJson('/api/producao/projetos-mesa?status=ATIVO', $headers)->assertOk()->assertJsonCount(1);
    }

    public function test_auditoria_abandonado_publica_e_ativo_nao(): void
    {
        Event::fake([ProjetoMesaAbandonadoEvent::class]);
        $admin = $this->headersAdmin();
        $dono = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($dono, Role::BOLSISTA);

        $id = $this->postJson('/api/producao/projetos-mesa', [
            'idFuncionario' => $dono->id_user, 'idMesa' => 7, 'nomeProjeto' => 'Drone',
        ], $headers)->assertCreated()->json('id');

        // Auditoria por não-Admin → 403.
        $this->postJson('/api/producao/auditorias-projeto-mesa', [
            'idProjetoMesa' => $id, 'resultado' => 'ABANDONADO',
        ], $headers)->assertForbidden();

        $this->postJson('/api/producao/auditorias-projeto-mesa', [
            'idProjetoMesa' => $id, 'resultado' => 'ATIVO', 'acaoTomada' => 'Tudo certo',
        ], $admin)->assertCreated();
        Event::assertNotDispatched(ProjetoMesaAbandonadoEvent::class);

        $this->postJson('/api/producao/auditorias-projeto-mesa', [
            'idProjetoMesa' => $id, 'resultado' => 'ABANDONADO', 'acaoTomada' => 'Sem evolução',
        ], $admin)->assertCreated();
        Event::assertDispatched(ProjetoMesaAbandonadoEvent::class,
            fn ($e) => $e->idProjetoMesa === $id && $e->idFuncionario === $dono->id_user);

        // Espelha o status; evolução reativa ABANDONADO→ATIVO.
        $this->getJson("/api/producao/projetos-mesa/{$id}", $headers)->assertJsonPath('status', 'ABANDONADO');
        $this->putJson("/api/producao/projetos-mesa/{$id}/evolucao", [], $headers)->assertOk()
            ->assertJsonPath('status', 'ATIVO');
        $this->getJson("/api/producao/auditorias-projeto-mesa/{$id}", $headers)->assertOk()->assertJsonCount(2);
        $this->getJson("/api/producao/projetos-mesa/{$id}/auditorias", $headers)->assertOk()->assertJsonCount(2);
    }

    public function test_projetos_sem_evolucao_e_scheduler_manual(): void
    {
        $admin = $this->headersAdmin();
        $dono = $this->loginPapel(Role::BOLSISTA);
        $headers = $this->authHeader($dono, Role::BOLSISTA);

        $velha = $this->postJson('/api/producao/projetos-mesa', [
            'idFuncionario' => $dono->id_user, 'idMesa' => 7, 'nomeProjeto' => 'Antiga',
        ], $headers)->assertCreated()->json('id');
        $nova = $this->postJson('/api/producao/projetos-mesa', [
            'idFuncionario' => $dono->id_user, 'idMesa' => 8, 'nomeProjeto' => 'Nova',
        ], $headers)->assertCreated()->json('id');

        \App\Modules\Producao\Models\ProjetoMesa::whereKey($velha)->update([
            'data_ultima_evolucao' => today()->subDays(20)->toDateString(),
        ]);

        $dias = Parametro5S::inteiroDe('diasParaAuditoriaProjeto', 15);
        $this->assertSame(15, $dias);

        $pendentes = app(ProjetoMesaService::class)->projetosSemEvolucao(today()->subDays($dias));
        $ids = array_map(fn ($m) => (int) $m->getKey(), $pendentes);
        $this->assertContains($velha, $ids);
        $this->assertNotContains($nova, $ids);
    }

    public function test_parametros_leitura_e_put_admin(): void
    {
        $admin = $this->headersAdmin();
        $lista = $this->getJson('/api/producao/parametros-5s', $admin)->assertOk()->assertJsonCount(4)->json();
        $id = $lista[0]['id'];

        $this->putJson("/api/producao/parametros-5s/{$id}", ['valor' => '20'],
            $this->headersPapel(Role::BOLSISTA))->assertForbidden();
        $this->putJson("/api/producao/parametros-5s/{$id}", ['valor' => '20'], $admin)
            ->assertOk()->assertJsonPath('valor', '20');
        $this->getJson("/api/producao/parametros-5s/{$id}", $admin)->assertOk()->assertJsonPath('valor', '20');
    }
}

<?php

namespace Tests\Feature\Notification;

use App\Modules\Auth\Enums\Role;
use App\Modules\Notification\Models\ConfiguracaoCanal;
use App\Modules\Notification\Models\Notificacao;
use Illuminate\Support\Facades\Event;

/** Admin: preferências, histórico integral, revisão e canais. */
class AdminTest extends NotificationTestCase
{
    public function test_preferencias_get_put_e_matriz_invalida_400(): void
    {
        Event::fake();
        $admin = $this->headersAdmin();

        $this->getJson('/api/notification/preferencias', $admin)
            ->assertOk()->assertJsonPath('pessoas.inapp', true)
            ->assertJsonPath('sistema.push', false);

        $this->putJson('/api/notification/preferencias', [
            'pessoas' => ['inapp' => true, 'email' => false, 'push' => true],
        ], $admin)->assertOk()->assertJsonPath('pessoas.email', false);

        foreach ([
            ['x' => ['inapp' => true]],
            ['pessoas' => ['zap' => true]],
            ['pessoas' => ['inapp' => null]],
        ] as $matriz) {
            $this->putJson('/api/notification/preferencias', $matriz, $admin)->assertBadRequest();
        }

        $bolsista = $this->headersPapel(Role::BOLSISTA);
        $this->getJson('/api/notification/preferencias', $bolsista)->assertForbidden();
        $this->putJson('/api/notification/preferencias', ['pessoas' => ['inapp' => true]], $bolsista)->assertForbidden();
    }

    public function test_historico_integral_com_filtros_sem_paginacao_server_side(): void
    {
        Event::fake();
        $admin = $this->headersAdmin();

        $this->postJson('/api/notification/notificacoes/revisar', ['ids' => [999999]], $admin)
            ->assertOk()->assertJson(['revisadas' => 0]);

        $ids = [];
        foreach ([['Aviso estoque', 'estoque', 'inapp'], ['Horas validadas', 'pessoas', 'email']] as [$titulo, $tipo, $canal]) {
            $ids[] = Notificacao::create([
                'id_usuario' => 50, 'titulo' => $titulo, 'mensagem' => "corpo {$titulo}",
                'tipo' => $tipo, 'canal' => $canal, 'lida' => false,
                'criada_em' => now(), 'status' => 'ENVIADA',
            ])->getKey();
        }

        $this->postJson('/api/notification/notificacoes/revisar', ['ids' => $ids], $admin)
            ->assertOk()->assertJson(['revisadas' => 2]);

        $this->getJson('/api/notification/notificacoes', $admin)->assertOk()->assertJsonPath('total', 0);

        $historico = $this->getJson('/api/notification/historico', $admin)
            ->assertOk()->assertJsonPath('total', 2)->json('items');
        $this->assertSame(['id', 'type', 'title', 'body', 'read', 'createdAt', 'link', 'channel'], array_keys($historico[0]));

        $this->getJson('/api/notification/historico?search=estoque', $admin)->assertOk()->assertJsonPath('total', 1);
        $this->getJson('/api/notification/historico?type=pessoas&channel=email', $admin)->assertOk()->assertJsonPath('total', 1);
        $this->getJson('/api/notification/historico?period=24h', $admin)->assertOk()->assertJsonPath('total', 2);
        $this->getJson('/api/notification/historico?period=ontem', $admin)->assertBadRequest();

        $bolsista = $this->headersPapel(Role::BOLSISTA);
        $this->getJson('/api/notification/historico', $bolsista)->assertForbidden();
        $this->postJson('/api/notification/notificacoes/revisar', ['ids' => $ids], $bolsista)->assertForbidden();
    }

    public function test_configuracao_canais_toggle(): void
    {
        Event::fake();
        $admin = $this->headersAdmin();

        $this->getJson('/api/notification/configuracao-canais', $admin)
            ->assertOk()->assertJsonCount(2)
            ->assertJsonFragment(['canal' => 'EMAIL', 'habilitado' => true])
            ->assertJsonFragment(['canal' => 'WHATSAPP', 'habilitado' => false]);

        $this->putJson('/api/notification/configuracao-canais/EMAIL', ['habilitado' => false], $admin)
            ->assertOk()->assertJson(['canal' => 'EMAIL', 'habilitado' => false]);
        $this->assertFalse(ConfiguracaoCanal::where('canal', 'EMAIL')->first()->habilitado);

        $this->putJson('/api/notification/configuracao-canais', [
            'canal' => 'EMAIL', 'habilitado' => true, 'parametros' => ['x' => 1],
        ], $admin)->assertOk()->assertJson(['canal' => 'EMAIL', 'habilitado' => true]);

        $this->putJson('/api/notification/configuracao-canais/ZAP', ['habilitado' => true], $admin)->assertNotFound();

        $bolsista = $this->headersPapel(Role::BOLSISTA);
        $this->getJson('/api/notification/configuracao-canais', $bolsista)->assertForbidden();
        $this->putJson('/api/notification/configuracao-canais/EMAIL', ['habilitado' => true], $bolsista)->assertForbidden();
    }
}

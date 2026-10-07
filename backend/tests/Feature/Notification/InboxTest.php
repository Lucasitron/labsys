<?php

namespace Tests\Feature\Notification;

use App\Modules\Auth\Enums\Role;
use App\Modules\Notification\Models\Notificacao;
use Illuminate\Support\Facades\Event;

/** Inbox: lista escopada, contagem do sino, ler/ler-todas, aliases, shapes EN. */
class InboxTest extends NotificationTestCase
{
    public function test_lista_escopada_com_filtros_paginacao_e_alias(): void
    {
        Event::fake();
        $func = $this->funcionarioComEmail();
        $headers = $this->headersFuncionario($func);
        $idUsuario = $func['login']->id_user;

        foreach (['encomenda', 'estoque', 'pessoas'] as $i => $tipo) {
            Notificacao::create([
                'id_usuario' => $idUsuario, 'titulo' => "T{$i}", 'tipo' => $tipo,
                'canal' => 'inapp', 'lida' => $i === 0,
                'criada_em' => now(), 'status' => 'ENVIADA',
            ]);
        }

        // Outro usuário não vaza.
        $outro = $this->funcionarioComEmail();
        Notificacao::create([
            'id_usuario' => $outro['login']->id_user, 'titulo' => 'Alheia',
            'tipo' => 'pessoas', 'canal' => 'inapp', 'lida' => false,
            'criada_em' => now(), 'status' => 'ENVIADA',
        ]);

        $lista = $this->getJson('/api/notification/notificacoes', $headers)
            ->assertOk()
            ->assertJsonPath('total', 3)
            ->assertJsonPath('page', 1)
            ->assertJsonPath('size', 10)
            ->assertJsonPath('pageSize', 10)
            ->assertJsonPath('totalPages', 1)
            ->json('items');

        $this->assertSame(['id', 'type', 'title', 'body', 'read', 'createdAt', 'link', 'channel'], array_keys($lista[0]));
        $tipos = array_column($lista, 'type');
        sort($tipos);
        $this->assertSame(['encomenda', 'estoque', 'pessoas'], $tipos);

        // Filtros + aliases de paginação.
        $this->getJson('/api/notification/notificacoes?type=estoque', $headers)
            ->assertOk()->assertJsonPath('total', 1)->assertJsonCount(1, 'items');
        $this->getJson('/api/notification/notificacoes?read=true', $headers)
            ->assertOk()->assertJsonPath('total', 1);
        $this->getJson('/api/notification/notificacoes?page=1&pageSize=2', $headers)
            ->assertOk()->assertJsonPath('total', 3)->assertJsonCount(2, 'items')
            ->assertJsonPath('totalPages', 2);
        $this->getJson('/api/notification/notificacoes?size=2', $headers)
            ->assertOk()->assertJsonCount(2, 'items');

        // Alias do App Shell/sino.
        $this->getJson('/api/notificacoes', $headers)->assertOk()->assertJsonPath('total', 3);

        // Faixas inválidas → 400 (service); lixo de tipo → 422 (Request).
        $this->getJson('/api/notification/notificacoes?page=0', $headers)->assertBadRequest();
        $this->getJson('/api/notification/notificacoes?size=101', $headers)->assertBadRequest();
        $this->getJson('/api/notification/notificacoes?page=abc', $headers)->assertStatus(422);
    }

    public function test_contagem_ler_e_ler_todas(): void
    {
        Event::fake();
        $func = $this->funcionarioComEmail();
        $headers = $this->headersFuncionario($func);
        $idUsuario = $func['login']->id_user;

        $ids = [];
        for ($i = 0; $i < 3; $i++) {
            $ids[] = Notificacao::create([
                'id_usuario' => $idUsuario, 'titulo' => "N{$i}", 'tipo' => 'pessoas',
                'canal' => 'inapp', 'lida' => false,
                'criada_em' => now(), 'status' => 'ENVIADA',
            ])->getKey();
        }

        $this->getJson('/api/notification/notificacoes/nao-lidas/contagem', $headers)
            ->assertOk()->assertJson(['count' => 3]);
        $this->getJson('/api/notificacoes/nao-lidas/contagem', $headers)
            ->assertOk()->assertJson(['count' => 3]);

        $this->patchJson("/api/notification/notificacoes/{$ids[0]}/ler", [], $headers)
            ->assertOk()->assertJsonPath('read', true)->assertJsonPath('type', 'pessoas');
        $this->getJson('/api/notification/notificacoes/nao-lidas/contagem', $headers)
            ->assertOk()->assertJson(['count' => 2]);

        $this->postJson('/api/notification/notificacoes/ler-todas', [], $headers)
            ->assertOk()->assertJson(['lidas' => 2]);
        $this->getJson('/api/notification/notificacoes/nao-lidas/contagem', $headers)
            ->assertOk()->assertJson(['count' => 0]);
    }

    public function test_isolamento_dono_e_admin(): void
    {
        Event::fake();
        $dono = $this->funcionarioComEmail();
        $outro = $this->funcionarioComEmail(Role::VOLUNTARIO);
        $adminHeaders = $this->headersAdmin();

        $id = Notificacao::create([
            'id_usuario' => $dono['login']->id_user, 'titulo' => 'Minha',
            'tipo' => 'pessoas', 'canal' => 'inapp', 'lida' => false,
            'criada_em' => now(), 'status' => 'ENVIADA',
        ])->getKey();

        // Terceiro não lê (403); Admin lê (bypass 1:1); ausente → 404.
        $this->patchJson("/api/notification/notificacoes/{$id}/ler", [], $this->headersFuncionario($outro, Role::VOLUNTARIO))
            ->assertForbidden();
        $this->patchJson("/api/notification/notificacoes/{$id}/ler", [], $adminHeaders)
            ->assertOk()->assertJsonPath('read', true);
        $this->patchJson('/api/notification/notificacoes/999999/ler', [], $adminHeaders)
            ->assertNotFound();
    }

    public function test_recrutando_le_as_proprias(): void
    {
        Event::fake();
        $func = $this->funcionarioComEmail(Role::RECRUTANDO);
        $headers = $this->headersFuncionario($func, Role::RECRUTANDO);

        Notificacao::create([
            'id_usuario' => $func['login']->id_user, 'titulo' => 'R',
            'tipo' => 'pessoas', 'canal' => 'inapp', 'lida' => false,
            'criada_em' => now(), 'status' => 'ENVIADA',
        ]);

        $this->getJson('/api/notification/notificacoes', $headers)->assertOk()->assertJsonPath('total', 1);
        $this->getJson('/api/notification/notificacoes/nao-lidas/contagem', $headers)
            ->assertOk()->assertJson(['count' => 1]);
    }
}

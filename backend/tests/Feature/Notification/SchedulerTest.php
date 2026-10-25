<?php

namespace Tests\Feature\Notification;

use App\Modules\Notification\Enums\StatusEntrega;
use App\Modules\Notification\Models\Notificacao;
use App\Modules\Notification\Models\NotificacaoHistorico;
use App\Modules\Notification\Services\NotificationDispatchService;
use Illuminate\Support\Facades\Mail;

/** Schedulers manuais: reenvio PENDENTE→ENVIADA e expurgo só >90d. */
class SchedulerTest extends NotificationTestCase
{
    public function test_reenvio_so_pendente_e_atualiza(): void
    {
        Mail::fake();
        $func = $this->funcionarioComEmail();
        $idUser = $func['login']->id_user;
        $dispatch = app(NotificationDispatchService::class);

        $pendente = Notificacao::create([
            'id_usuario' => $idUser, 'titulo' => 'Retry', 'tipo' => 'pessoas',
            'canal' => 'email', 'lida' => false, 'criada_em' => now(),
            'status' => StatusEntrega::PENDENTE,
        ]);
        $enviada = Notificacao::create([
            'id_usuario' => $idUser, 'titulo' => 'Ok', 'tipo' => 'pessoas',
            'canal' => 'email', 'lida' => false, 'criada_em' => now(),
            'status' => StatusEntrega::ENVIADA, 'data_envio' => now(),
        ]);
        Notificacao::create([
            'id_usuario' => $idUser, 'titulo' => 'Inapp', 'tipo' => 'pessoas',
            'canal' => 'inapp', 'lida' => false, 'criada_em' => now(),
            'status' => StatusEntrega::ENVIADA, 'data_envio' => now(),
        ]);

        $this->assertSame(1, $dispatch->reenviarPendentes());
        $this->assertSame('ENVIADA', $pendente->fresh()->status->value);
        $this->assertNotNull($pendente->fresh()->data_envio);
        $this->assertSame('ENVIADA', $enviada->fresh()->status->value);

        // Segunda chamada: nada a reenviar.
        $this->assertSame(0, $dispatch->reenviarPendentes());
    }

    public function test_expurgar_remove_so_maior_que_90d(): void
    {
        $dispatch = app(NotificationDispatchService::class);

        $velho = NotificacaoHistorico::create([
            'id_notificacao_original' => 1, 'id_usuario' => 60, 'titulo' => 'Velha',
            'criada_em' => now()->subDays(100), 'data_revisao_admin' => now()->subDays(91),
            'id_admin_revisor' => 1,
        ]);
        $limite = NotificacaoHistorico::create([
            'id_notificacao_original' => 2, 'id_usuario' => 60, 'titulo' => 'Limite',
            'criada_em' => now()->subDays(100), 'data_revisao_admin' => now()->subDays(89),
            'id_admin_revisor' => 1,
        ]);

        $this->assertSame(1, $dispatch->expurgarHistorico());
        $this->assertNull(NotificacaoHistorico::find($velho->getKey()));
        $this->assertNotNull(NotificacaoHistorico::find($limite->getKey()));
    }
}

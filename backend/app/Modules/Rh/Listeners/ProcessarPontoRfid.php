<?php

namespace App\Modules\Rh\Listeners;

use App\Modules\Auth\Events\RfidAccessEvent;
use App\Modules\Rh\Services\PontoService;
use Illuminate\Contracts\Queue\ShouldQueue;

/**
 * Consome `access.rfid.event` (Auth, fila `database`) → ponto diário.
 * Idempotente por (funcionário, data). Sem broker.
 */
class ProcessarPontoRfid implements ShouldQueue
{
    public string $queue = 'database';

    public function __construct(private PontoService $ponto) {}

    public function handle(RfidAccessEvent $event): void
    {
        $this->ponto->processarEventoRfid(
            $event->idUser,
            $event->type->value,
            $event->timestamp,
        );
    }
}

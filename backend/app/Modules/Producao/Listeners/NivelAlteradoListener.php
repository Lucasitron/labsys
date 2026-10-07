<?php

namespace App\Modules\Producao\Listeners;

use App\Modules\Rh\Events\NivelAlteradoEvent;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome `nivel.alterado.event` do RH (fila `database`, sem broker). O nível
 * efetivo já vale nas requisições via JWT; aqui só trilha de auditoria, sem
 * persistência (como no Java).
 */
class NivelAlteradoListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function handle(NivelAlteradoEvent $event): void
    {
        Log::info("Nível de acesso do funcionário {$event->idFuncionario} alterado de {$event->nivelAntigo->value} para {$event->nivelNovo->value}");
    }
}

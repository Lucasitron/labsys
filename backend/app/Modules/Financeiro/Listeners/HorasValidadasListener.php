<?php

namespace App\Modules\Financeiro\Listeners;

use App\Modules\Financeiro\Services\FechamentoService;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Events\HorasValidadasEvent;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome `horas.validadas.event` (fila `database`, sem broker) e acumula as
 * horas por (encomenda, funcionário, data). Só `ENCOMENDA` interessa (o
 * `idReferencia` do RH é o id da encomenda); `PROJETO` = ignore. Sem nível no
 * evento do RH — o custeio aplica o default nível 2. Falha nunca propaga
 * (protege o produtor RH em fila sync).
 */
class HorasValidadasListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private FechamentoService $fechamentos) {}

    public function handle(HorasValidadasEvent $event): void
    {
        if ($event->tipo !== TipoApontamento::ENCOMENDA) {
            return;
        }

        try {
            $this->fechamentos->registrarHorasValidadas(
                $event->idReferencia,
                $event->idFuncionario,
                null,
                number_format((float) $event->horas, 2, '.', ''),
                substr($event->data, 0, 10),
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar horas.validadas.event: {$e->getMessage()}");
        }
    }
}

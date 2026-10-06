<?php

namespace App\Modules\Auth\Events;

use App\Modules\Auth\Enums\AccessLogType;
use Carbon\Carbon;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/**
 * Leitura RFID válida (contrato com o RH: registro de ponto).
 * Nome do evento preservado: "access.rfid.event" (exchange fablab.access).
 * Fila nativa "database" (sem broker no caminho crítico M1).
 */
class RfidAccessEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'access.rfid.event';

    public function __construct(
        public readonly int $idUser,
        public readonly string $uuidRfid,
        public readonly Carbon $timestamp,
        public readonly AccessLogType $type,
    ) {}

    /** @return array{idUser:int,uuidRfid:string,timestamp:string,type:string} */
    public function payload(): array
    {
        return [
            'idUser' => $this->idUser,
            'uuidRfid' => $this->uuidRfid,
            'timestamp' => $this->timestamp->toIso8601String(),
            'type' => $this->type->value,
        ];
    }
}

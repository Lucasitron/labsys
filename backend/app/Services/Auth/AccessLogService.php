<?php

namespace App\Services\Auth;

use App\Enums\Auth\AccessLogType;
use App\Models\Auth\AccessLog;

/** Registro do acesso físico / ponto (auth.access_log). */
class AccessLogService
{
    /**
     * Alterna ENTRADA↔SAIDA pelo último registro do cartão.
     * Sem anterior → ENTRADA.
     */
    public function resolveType(string $uuidRfid): AccessLogType
    {
        $previous = AccessLog::where('uuid_rfid', $uuidRfid)
            ->orderByDesc('timestamp')
            ->orderByDesc('id')
            ->first();

        if ($previous === null || $previous->type !== AccessLogType::ENTRADA) {
            return AccessLogType::ENTRADA;
        }

        return AccessLogType::SAIDA;
    }

    public function record(?int $idUser, string $uuidRfid, AccessLogType $type): AccessLog
    {
        return AccessLog::create([
            'id_user' => $idUser,
            'uuid_rfid' => $uuidRfid,
            'timestamp' => now(),
            'type' => $type,
        ]);
    }
}

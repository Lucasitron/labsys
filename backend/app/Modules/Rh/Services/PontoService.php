<?php

namespace App\Modules\Rh\Services;

use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\RegistroPontoDiario;
use Carbon\CarbonInterface;
use Illuminate\Support\Facades\Log;

/**
 * Ponto (F3): consolida `access.rfid.event` em registro diário.
 * Idempotente por (funcionário, data): ENTRADA guarda a mais cedo, SAÍDA a mais tarde.
 */
class PontoService
{
    public function processarEventoRfid(int $idUser, ?string $type, CarbonInterface $timestamp): void
    {
        if (! in_array($type, ['ENTRADA', 'SAIDA'], true)) {
            Log::debug('Evento RFID ignorado para ponto', ['idUser' => $idUser, 'type' => $type]);

            return;
        }

        $funcionario = Funcionario::where('id_pessoa', $idUser)->first();

        if ($funcionario === null) {
            Log::warning('Evento RFID de usuário sem funcionário vinculado', ['idUser' => $idUser]);

            return;
        }

        $data = $timestamp->toDateString();

        $registro = RegistroPontoDiario::firstOrNew([
            'id_funcionario' => (int) $funcionario->getKey(),
            'data' => $data,
        ]);

        if ($type === 'ENTRADA') {
            if ($registro->hora_entrada === null || $timestamp->lt($registro->hora_entrada)) {
                $registro->hora_entrada = $timestamp;
            }
        } elseif ($registro->hora_saida === null || $timestamp->gt($registro->hora_saida)) {
            $registro->hora_saida = $timestamp;
        }

        if ($registro->hora_entrada !== null && $registro->hora_saida !== null) {
            // Carbon 3 devolve diff com sinal por padrão — abs() garante duração positiva.
            $minutos = abs($registro->hora_saida->diffInMinutes($registro->hora_entrada, false));
            $registro->total_horas = number_format($minutos / 60, 2, '.', '');
        }

        $registro->save();
    }
}

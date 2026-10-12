<?php

namespace App\Modules\Notification\Http\Controllers;

use App\Modules\Notification\Http\Requests\CanalRequest;
use App\Modules\Notification\Models\ConfiguracaoCanal;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Http\JsonResponse;

/** Configuração de canais — só HTTP (Admin; D9/docs/07 §6). CRUD trivial no Model. */
class ConfiguracaoCanalController
{
    public function index(): JsonResponse
    {
        return response()->json(
            ConfiguracaoCanal::orderBy('canal')->get()->map(fn (ConfiguracaoCanal $c) => $this->shape($c))->all()
        );
    }

    public function update(CanalRequest $request, ?string $canal = null): JsonResponse
    {
        $nome = strtoupper((string) ($canal ?? $request->input('canal')));
        $config = ConfiguracaoCanal::where('canal', $nome)->first();

        if ($config === null) {
            throw new ResourceNotFoundException("Canal {$nome} não encontrado");
        }

        $config->habilitado = (bool) $request->input('habilitado');

        if ($request->has('parametros')) {
            $config->parametros = $request->input('parametros');
        }

        $config->save();

        return response()->json($this->shape($config));
    }

    /** @return array{canal:string,habilitado:bool,parametros:?array} */
    private function shape(ConfiguracaoCanal $config): array
    {
        return [
            'canal' => (string) $config->canal,
            'habilitado' => (bool) $config->habilitado,
            'parametros' => $config->parametros,
        ];
    }
}

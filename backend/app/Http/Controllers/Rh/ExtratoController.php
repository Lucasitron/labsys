<?php

namespace App\Http\Controllers\Rh;

use App\Exceptions\ForbiddenException;
use App\Http\Controllers\Controller;
use App\Services\Rh\ExtratoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Leitura do extrato mensal (a geração é o scheduler R10). Só HTTP. */
class ExtratoController extends Controller
{
    use ResolvesRhPrincipal;

    public function __construct(private ExtratoService $extrato) {}

    public function show(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'funcionarioId' => ['nullable', 'integer'],
            'mes' => ['nullable', 'date_format:Y-m'],
        ]);

        $principal = $this->principal($request);

        $idFuncionario = isset($filtros['funcionarioId'])
            ? (int) $filtros['funcionarioId']
            : $principal->idFuncionario;

        if ($idFuncionario === null) {
            throw new ForbiddenException('Operação restrita a usuários vinculados a um funcionário');
        }

        return response()->json($this->extrato->leitura(
            $idFuncionario,
            $filtros['mes'] ?? today()->startOfMonth()->subMonthNoOverflow()->format('Y-m'),
            $principal,
        ));
    }
}

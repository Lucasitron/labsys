<?php

namespace App\Modules\Rh\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\ExtratoService;
use App\Shared\Exceptions\ForbiddenException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Leitura do extrato mensal (a geração é o scheduler R10). Só HTTP. */
class ExtratoController
{
    public function __construct(private ExtratoService $extrato) {}

    public function show(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'funcionarioId' => ['nullable', 'integer'],
            'mes' => ['nullable', 'regex:/^\d{4}-\d{2}$/'],
        ]);

        /** @var Login $login */
        $login = $request->user();
        $principal = RhPrincipal::from($login);

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

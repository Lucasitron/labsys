<?php

namespace App\Modules\Financeiro\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Services\RelatorioService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * 6 relatórios de saúde financeira (shapes servidos — D-2). Sem PDF/CSV/export.
 * `periodo` com allowlist `yyyy-MM` (422 fora do formato); `dataInicio`/`dataFim`
 * têm precedência. Lucratividade/inadimplência/custo-máquina sem período.
 */
class RelatorioController
{
    public function __construct(private RelatorioService $relatorios) {}

    public function fluxoCaixa(Request $request): JsonResponse
    {
        $periodo = $this->periodo($request);

        return response()->json($this->relatorios->fluxoCaixa(
            $periodo['dataInicio'], $periodo['dataFim'], $periodo['periodo'],
            $this->principal($request),
        ));
    }

    public function dre(Request $request): JsonResponse
    {
        $periodo = $this->periodo($request);

        return response()->json($this->relatorios->dre(
            $periodo['dataInicio'], $periodo['dataFim'], $periodo['periodo'],
            $this->principal($request),
        ));
    }

    public function lucratividade(Request $request): JsonResponse
    {
        return response()->json($this->relatorios->lucratividade($this->principal($request)));
    }

    public function inadimplencia(Request $request): JsonResponse
    {
        return response()->json($this->relatorios->inadimplencia($this->principal($request)));
    }

    public function doacoesDespesas(Request $request): JsonResponse
    {
        $periodo = $this->periodo($request);

        return response()->json($this->relatorios->doacoesDespesas(
            $periodo['dataInicio'], $periodo['dataFim'], $periodo['periodo'],
            $this->principal($request),
        ));
    }

    public function custoMaquina(Request $request): JsonResponse
    {
        return response()->json($this->relatorios->custoMaquina($this->principal($request)));
    }

    /** @return array{dataInicio:?string,dataFim:?string,periodo:?string} */
    private function periodo(Request $request): array
    {
        $filtros = $request->validate([
            'dataInicio' => ['nullable', 'date'],
            'dataFim' => ['nullable', 'date'],
            'periodo' => ['nullable', 'string', 'regex:/^\d{4}-(0[1-9]|1[0-2])$/'],
        ]);

        return [
            'dataInicio' => $filtros['dataInicio'] ?? null,
            'dataFim' => $filtros['dataFim'] ?? null,
            'periodo' => $filtros['periodo'] ?? null,
        ];
    }

    private function principal(Request $request): FinanceiroPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return FinanceiroPrincipal::from($login);
    }
}

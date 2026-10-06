<?php

namespace App\Modules\Estoque\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Http\Requests\EmprestimoRequest;
use App\Modules\Estoque\Http\Resources\EmprestimoResource;
use App\Modules\Estoque\Services\EmprestimoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Empréstimos — só HTTP (`?status=` só aqui; escopo E-7 no service). */
class EmprestimoController
{
    public function __construct(private EmprestimoService $emprestimos) {}

    public function store(EmprestimoRequest $request): JsonResponse
    {
        $emprestimo = $this->emprestimos->criar($request->validated(), $this->principal($request));

        return response()->json((new EmprestimoResource($emprestimo))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'max:32'],
        ]);

        $emprestimos = $this->emprestimos->listar(
            $filtros['status'] ?? null,
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($e) => (new EmprestimoResource($e))->toArray($request),
            $emprestimos,
        ));
    }

    public function atrasados(Request $request): JsonResponse
    {
        $emprestimos = $this->emprestimos->listarAtrasados($this->principal($request));

        return response()->json(array_map(
            fn ($e) => (new EmprestimoResource($e))->toArray($request),
            $emprestimos,
        ));
    }

    public function show(Request $request, int $id): EmprestimoResource
    {
        return new EmprestimoResource($this->emprestimos->buscar($id, $this->principal($request)));
    }

    public function devolver(Request $request, int $id): JsonResponse
    {
        $emprestimo = $this->emprestimos->devolver($id, $this->principal($request));

        return response()->json((new EmprestimoResource($emprestimo))->toArray($request));
    }

    private function principal(Request $request): EstoquePrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return EstoquePrincipal::from($login);
    }
}

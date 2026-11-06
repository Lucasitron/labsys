<?php

namespace App\Http\Controllers\Estoque;

use App\Http\Controllers\Controller;
use App\Http\Controllers\Estoque\Concerns\ResolvesEstoquePrincipal;

use App\Http\Requests\Estoque\EmprestimoRequest;
use App\Http\Resources\Estoque\EmprestimoResource;
use App\Services\Estoque\EmprestimoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

/** Empréstimos — só HTTP (`?status=` só aqui; escopo E-7 no service). */
class EmprestimoController extends Controller
{
    use ResolvesEstoquePrincipal;
    public function __construct(private EmprestimoService $emprestimos) {}

    public function store(EmprestimoRequest $request): JsonResponse
    {
        $emprestimo = $this->emprestimos->criar($request->validated(), $this->principal($request));

        return response()->json((new EmprestimoResource($emprestimo))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', Rule::in(['ativos', 'atrasados', 'historico'])],
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
}

<?php

namespace App\Http\Controllers\Estoque;

use App\Http\Controllers\Controller;
use App\Http\Requests\Estoque\FornecedorRequest;
use App\Http\Resources\Estoque\FornecedorResource;
use App\Models\Estoque\Fornecedor;
use App\Policies\EstoquePolicy;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Fornecedores — CRUD trivial direto no Eloquent (sem service forçado).
 * Escrita só Admin (matriz); leitura bloqueia Recrutando.
 */
class FornecedorController extends Controller
{
    use ResolvesEstoquePrincipal;

    public function store(FornecedorRequest $request): JsonResponse
    {
        $fornecedor = Fornecedor::create($request->validated());

        return response()->json((new FornecedorResource($fornecedor))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        EstoquePolicy::exigeLeitura($this->principal($request));

        $fornecedores = Fornecedor::orderBy('nome')->orderBy('id_fornecedor')->get();

        return response()->json($fornecedores->map(
            fn ($f) => (new FornecedorResource($f))->toArray($request),
        )->all());
    }
}

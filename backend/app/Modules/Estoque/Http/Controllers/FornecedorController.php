<?php

namespace App\Modules\Estoque\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Http\Requests\FornecedorRequest;
use App\Modules\Estoque\Http\Resources\FornecedorResource;
use App\Modules\Estoque\Models\Fornecedor;
use App\Modules\Estoque\Policies\EstoquePolicy;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Fornecedores — CRUD trivial direto no Eloquent (sem service forçado).
 * Escrita só Admin (matriz); leitura bloqueia Recrutando.
 */
class FornecedorController
{
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

    private function principal(Request $request): EstoquePrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return EstoquePrincipal::from($login);
    }
}

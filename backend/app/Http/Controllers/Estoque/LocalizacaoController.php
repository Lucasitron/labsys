<?php

namespace App\Http\Controllers\Estoque;

use App\Http\Controllers\Controller;
use App\Http\Requests\Estoque\LocalizacaoRequest;
use App\Http\Resources\Estoque\LocalizacaoResource;
use App\Models\Estoque\Localizacao;
use App\Policies\EstoquePolicy;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Localizações — CRUD trivial direto no Eloquent (sem service forçado).
 * Escrita só Admin (matriz); leitura bloqueia Recrutando.
 */
class LocalizacaoController extends Controller
{
    use ResolvesEstoquePrincipal;

    public function index(Request $request): JsonResponse
    {
        EstoquePolicy::exigeLeitura($this->principal($request));

        $localizacoes = Localizacao::orderBy('armario')->orderBy('prateleira')
            ->orderBy('caixa')->orderBy('id_localizacao')->get();

        return response()->json($localizacoes->map(
            fn ($l) => (new LocalizacaoResource($l))->toArray($request),
        )->all());
    }

    public function store(LocalizacaoRequest $request): JsonResponse
    {
        $localizacao = Localizacao::create($request->validated());

        return response()->json((new LocalizacaoResource($localizacao))->toArray($request), 201);
    }
}

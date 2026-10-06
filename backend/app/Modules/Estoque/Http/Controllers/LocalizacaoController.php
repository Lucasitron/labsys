<?php

namespace App\Modules\Estoque\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Http\Requests\LocalizacaoRequest;
use App\Modules\Estoque\Http\Resources\LocalizacaoResource;
use App\Modules\Estoque\Models\Localizacao;
use App\Modules\Estoque\Policies\EstoquePolicy;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Localizações — CRUD trivial direto no Eloquent (sem service forçado).
 * Escrita só Admin (matriz); leitura bloqueia Recrutando.
 */
class LocalizacaoController
{
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

    private function principal(Request $request): EstoquePrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return EstoquePrincipal::from($login);
    }
}

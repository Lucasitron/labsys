<?php

namespace App\Modules\Vendas\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Vendas\Http\Requests\TagRequest;
use App\Modules\Vendas\Http\Resources\TagResource;
use App\Modules\Vendas\Models\TagCliente;
use App\Modules\Vendas\Policies\VendasPolicy;
use App\Modules\Vendas\VendasPrincipal;
use App\Shared\Exceptions\ConflitoException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Tags de clientes — CRUD trivial direto no Model (sem service forçado). */
class TagController
{
    public function store(TagRequest $request): JsonResponse
    {
        VendasPolicy::exigeEscrita($this->principal($request));

        $dados = $request->validated();

        if (TagCliente::where('nome', $dados['nome'])->exists()) {
            throw new ConflitoException('Tag já cadastrada');
        }

        $tag = TagCliente::create(['nome' => $dados['nome'], 'cor' => $dados['cor'] ?? null]);

        return response()->json((new TagResource($tag))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        VendasPolicy::exigeLeitura($this->principal($request));

        $tags = TagCliente::query()->orderBy('nome')->get();

        return response()->json(array_map(
            fn ($t) => (new TagResource($t))->toArray($request),
            $tags->all(),
        ));
    }

    private function principal(Request $request): VendasPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return VendasPrincipal::from($login);
    }
}

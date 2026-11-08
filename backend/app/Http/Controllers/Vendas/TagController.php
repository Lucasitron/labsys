<?php

namespace App\Http\Controllers\Vendas;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Http\Requests\Vendas\TagRequest;
use App\Http\Resources\Vendas\TagResource;
use App\Models\Vendas\TagCliente;
use App\Policies\VendasPolicy;
use App\Shared\Auth\Caller;
use App\Exceptions\ConflitoException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Tags de clientes — CRUD trivial direto no Model (sem service forçado). */
class TagController extends Controller
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

}

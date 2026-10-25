<?php

namespace App\Modules\Auth\Http\Controllers;

use App\Modules\Auth\Http\Requests\CriarTokenRequest;
use App\Modules\Auth\Services\TokenIntegracaoService;
use Illuminate\Http\JsonResponse;

/** Tokens de integração (Admin-only) — só HTTP. */
class TokenIntegracaoController
{
    public function __construct(private TokenIntegracaoService $tokens) {}

    public function index(): JsonResponse
    {
        return response()->json($this->tokens->listarAtivos());
    }

    public function store(CriarTokenRequest $request): JsonResponse
    {
        return response()->json($this->tokens->criar($request->input('nome')), 201);
    }

    public function destroy(int $id): JsonResponse
    {
        $this->tokens->revogar($id);

        return response()->json(null, 204);
    }
}

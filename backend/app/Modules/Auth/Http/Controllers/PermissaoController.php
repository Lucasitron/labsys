<?php

namespace App\Modules\Auth\Http\Controllers;

use App\Modules\Auth\Http\Requests\AtualizarPermissaoRequest;
use App\Modules\Auth\Services\RbacService;
use Illuminate\Http\JsonResponse;

/** Matriz RBAC completa + edição de célula (Admin-only) — só HTTP. */
class PermissaoController
{
    public function __construct(private RbacService $rbac) {}

    public function index(): JsonResponse
    {
        return response()->json($this->rbac->getFullMatrix());
    }

    public function update(AtualizarPermissaoRequest $request, string $modulo, string $nivel): JsonResponse
    {
        return response()->json($this->rbac->updateCell($modulo, $nivel, $request->input('valor')));
    }
}

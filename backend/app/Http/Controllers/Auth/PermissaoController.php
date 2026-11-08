<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Http\Requests\Auth\AtualizarPermissaoRequest;
use App\Services\Auth\RbacService;
use Illuminate\Http\JsonResponse;

/** Matriz RBAC completa + edição de célula (Admin-only) — só HTTP. */
class PermissaoController extends Controller
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

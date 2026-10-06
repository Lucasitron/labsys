<?php

namespace App\Modules\Auth\Http\Controllers;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Http\Requests\AlterarSenhaRequest;
use App\Modules\Auth\Http\Requests\LoginRequest;
use App\Modules\Auth\Http\Requests\RefreshRequest;
use App\Modules\Auth\Http\Requests\ValidateRfidRequest;
use App\Modules\Auth\Http\Resources\MeResource;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Services\AuthService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Auth & Identity — só HTTP: valida (FormRequest), delega ao service, devolve JSON/Resource. */
class AuthController
{
    public function __construct(private AuthService $auth) {}

    public function login(LoginRequest $request): JsonResponse
    {
        return response()->json($this->auth->login(
            $request->input('email'),
            $request->input('nomeUsuario'),
            $request->input('senha'),
        ));
    }

    public function logout(Request $request): JsonResponse
    {
        $token = $request->input('token') ?? $this->bearer($request);
        $this->auth->logout($token);

        return response()->json(['message' => 'Logout realizado com sucesso']);
    }

    public function validateRfid(ValidateRfidRequest $request): JsonResponse
    {
        return response()->json($this->auth->validateRfid($request->input('uuid')));
    }

    public function me(Request $request): MeResource
    {
        /** @var Login $login */
        $login = $request->user();
        $data = $this->auth->me($login);

        return new MeResource($data['login'], $data['permissions']);
    }

    public function refresh(RefreshRequest $request): JsonResponse
    {
        return response()->json($this->auth->refresh($request->input('refreshToken')));
    }

    public function permissions(Request $request): JsonResponse
    {
        $role = Role::parse($request->query('role', 'ADMIN'));

        return response()->json($this->auth->permissions($role));
    }

    public function alterarSenha(AlterarSenhaRequest $request): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();
        $this->auth->alterarSenha(
            $login,
            $request->input('senhaAtual'),
            $request->input('novaSenha'),
            $request->input('confirmacaoSenha'),
            $this->bearer($request),
        );

        return response()->json(['mensagem' => 'Senha alterada com sucesso']);
    }

    private function bearer(Request $request): ?string
    {
        return $request->bearerToken();
    }
}

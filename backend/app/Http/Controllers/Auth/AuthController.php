<?php

namespace App\Http\Controllers\Auth;

use App\Enums\Auth\Role;
use App\Http\Controllers\Controller;
use App\Http\Requests\Auth\AlterarSenhaRequest;
use App\Http\Requests\Auth\LoginRequest;
use App\Http\Requests\Auth\RefreshRequest;
use App\Http\Requests\Auth\ValidateRfidRequest;
use App\Http\Resources\Auth\MeResource;
use App\Models\Auth\Login;
use App\Services\Auth\AuthService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Auth & Identity — só HTTP: valida (FormRequest), delega ao service, devolve JSON/Resource. */
class AuthController extends Controller
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
        $token = $request->input('token') ?? $request->bearerToken();
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
            $request->bearerToken(),
        );

        return response()->json(['mensagem' => 'Senha alterada com sucesso']);
    }
}

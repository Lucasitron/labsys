<?php

namespace App\Modules\Auth\Contracts;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use App\Modules\Auth\Services\TokenBlacklistService;
use App\Modules\Auth\Services\TokenService;
use App\Modules\Auth\Services\UsuarioService;
use App\Shared\Exceptions\TokenInvalidException;
use Illuminate\Support\Str;

class DefaultAuthContract implements AuthContract
{
    public function __construct(
        private TokenService $tokens,
        private TokenBlacklistService $blacklist,
    ) {}

    public function verify(string $token): ?array
    {
        try {
            $claims = $this->tokens->parse($token);
        } catch (TokenInvalidException) {
            return null;
        }

        if ($this->blacklist->isBlacklisted($token)) {
            return null;
        }

        return [
            'id' => (int) ($claims['sub'] ?? 0),
            'idUser' => (int) ($claims[TokenService::CLAIM_ID_USER] ?? 0),
            'role' => (string) ($claims[TokenService::CLAIM_ROLE] ?? ''),
            'setor' => $claims[TokenService::CLAIM_SETOR] ?? null,
        ];
    }

    public function isAdmin(int $idUser): bool
    {
        return $this->roleOf($idUser) === Role::ADMIN;
    }

    public function roleOf(int $idUser): ?Role
    {
        $permission = UserPermission::where('id_user', $idUser)
            ->where('active', true)
            ->first();

        return $permission?->role;
    }

    public function adminIds(): array
    {
        return UserPermission::where('role', Role::ADMIN)
            ->where('active', true)
            ->pluck('id_user')
            ->map(fn ($id) => (int) $id)
            ->unique()
            ->values()
            ->all();
    }

    public function definirRole(int $idUser, Role $role): bool
    {
        if (! Login::where('id_user', $idUser)->exists()) {
            return false;
        }

        $permission = UserPermission::where('id_user', $idUser)
            ->where('active', true)
            ->first();

        if ($permission === null) {
            UserPermission::create(['id_user' => $idUser, 'role' => $role, 'active' => true]);

            return true;
        }

        $permission->role = $role;
        $permission->save();

        return true;
    }

    public function provisionarAcesso(
        int $idUser,
        string $email,
        Role $role,
        ?string $nomeUsuario = null,
        ?string $senha = null,
        ?string $setor = null,
    ): array {
        $email = trim($email);

        if (Login::where('id_user', $idUser)->exists()) {
            throw new \InvalidArgumentException('Já existe acesso cadastrado para esta pessoa');
        }

        $convite = blank($senha);
        $nome = blank($nomeUsuario)
            ? (string) (strstr($email, '@', true) ?: $email)
            : trim($nomeUsuario);

        $login = app(UsuarioService::class)->criar([
            'idUser' => $idUser,
            'email' => $email,
            'nomeUsuario' => $nome,
            'senha' => $convite ? Str::random(48) : $senha,
            'setor' => $setor,
            'nivel' => $role->value,
            'situacao' => $convite ? null : SituacaoUsuario::ATIVO->value,
        ]);

        return [
            'id' => (int) $login->getKey(),
            'idUser' => (int) $login->id_user,
            'email' => (string) $login->email,
            'situacao' => $login->situacao->value,
        ];
    }
}

<?php

namespace App\Modules\Auth\Contracts;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\UserPermission;
use App\Modules\Auth\Services\TokenBlacklistService;
use App\Modules\Auth\Services\TokenService;
use App\Shared\Exceptions\TokenInvalidException;

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
}

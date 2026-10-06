<?php

namespace App\Modules\Auth\Services;

use App\Modules\Auth\Models\TokenBlacklist;
use Carbon\Carbon;

/** Blacklist de tokens JWT (auth.token_blacklist). Operações idempotentes. */
class TokenBlacklistService
{
    public function isBlacklisted(string $token): bool
    {
        return TokenBlacklist::where('token', $token)->exists();
    }

    public function blacklist(string $token, Carbon $expiry): void
    {
        TokenBlacklist::firstOrCreate(
            ['token' => $token],
            ['expiry_date' => $expiry]
        );
    }

    /** Remove só expirados (purge diário 03:00). Retorna nº removidos. */
    public function cleanupExpired(): int
    {
        return TokenBlacklist::where('expiry_date', '<', now())->delete();
    }
}

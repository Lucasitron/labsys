<?php

namespace App\Modules\Auth\Http\Middleware;

use App\Modules\Auth\Models\TokenBlacklist;
use App\Shared\Exceptions\TokenBlacklistedException;
use Closure;
use Illuminate\Http\Request;

/**
 * Rejeita com 401 tokens revogados (auth.token_blacklist). Complementa o
 * guard JWT — a blacklist vive na tabela do módulo, não no cache.
 */
class CheckBlacklist
{
    public function handle(Request $request, Closure $next): mixed
    {
        $token = $request->bearerToken();

        if ($token !== null && TokenBlacklist::where('token', $token)->exists()) {
            throw new TokenBlacklistedException;
        }

        return $next($request);
    }
}

<?php

namespace App\Modules\Auth\Services;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Shared\Exceptions\TokenInvalidException;
use Carbon\Carbon;
use PHPOpenSourceSaver\JWTAuth\Exceptions\JWTException;
use PHPOpenSourceSaver\JWTAuth\Facades\JWTAuth;

/**
 * Emissão e leitura de JWT (equivale a JwtService/JwtProperties: HMAC SHA-256,
 * issuer "fablab", claims id_user/role/setor/type, access 900s, refresh 7 dias,
 * clock skew 5s via config jwt.leeway).
 */
class TokenService
{
    public const CLAIM_ID_USER = 'id_user';

    public const CLAIM_ROLE = 'role';

    public const CLAIM_SETOR = 'setor';

    public const CLAIM_TYPE = 'type';

    public const TYPE_ACCESS = 'access';

    public const TYPE_REFRESH = 'refresh';

    public const ISSUER = 'fablab';

    public const ACCESS_TTL_SECONDS = 900;

    public const REFRESH_TTL_SECONDS = 604800;

    public function generateAccessToken(Login $login, Role $role): string
    {
        return $this->generate($login, $role, self::TYPE_ACCESS, 15);
    }

    public function generateRefreshToken(Login $login, Role $role): string
    {
        return $this->generate($login, $role, self::TYPE_REFRESH, 10080);
    }

    /** @return array<string, mixed> claims decodificadas e validadas */
    public function parse(string $token): array
    {
        try {
            $payload = JWTAuth::setToken($token)->getPayload();
        } catch (JWTException $e) {
            throw new TokenInvalidException;
        }

        $claims = $payload->toArray();

        if (($claims['iss'] ?? null) !== self::ISSUER) {
            throw new TokenInvalidException;
        }

        return $claims;
    }

    public function isRefreshToken(array $claims): bool
    {
        return ($claims[self::CLAIM_TYPE] ?? null) === self::TYPE_REFRESH;
    }

    public function expiry(array $claims): Carbon
    {
        return Carbon::createFromTimestamp((int) $claims['exp']);
    }

    private function generate(Login $login, Role $role, string $type, int $ttlMinutes): string
    {
        return auth('api')->claims([
            'iss' => self::ISSUER,
            self::CLAIM_ID_USER => $login->id_user,
            self::CLAIM_ROLE => $role->name,
            self::CLAIM_SETOR => $login->setor,
            self::CLAIM_TYPE => $type,
        ])->setTTL($ttlMinutes)->login($login);
    }
}

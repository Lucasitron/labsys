<?php

use PHPOpenSourceSaver\JWTAuth\Providers\Auth\Illuminate;
use PHPOpenSourceSaver\JWTAuth\Providers\JWT\Lcobucci;

return [

    /*
    |--------------------------------------------------------------------------
    | JWT Authentication (php-open-source-saver/jwt-auth, guard "jwt")
    |--------------------------------------------------------------------------
    |
    | Contrato herdado do auth-service (JwtService/JwtProperties): HMAC SHA-256,
    | issuer "fablab", claims id_user/role/setor/type, access 900s (15min),
    | refresh 604800s (7 dias), clock skew 5s. A blacklist é gerenciada pela
    | tabela auth.token_blacklist (TokenBlacklistService), não pelo cache.
    |
    */

    'secret' => env('JWT_SECRET'),

    'keys' => [
        'public' => env('JWT_PUBLIC_KEY'),
        'private' => env('JWT_PRIVATE_KEY'),
        'passphrase' => env('JWT_PASSPHRASE'),
    ],

    'ttl' => (int) env('JWT_TTL', 15), // minutos (15min = 900s)

    'refresh_ttl' => (int) env('JWT_REFRESH_TTL', 10080), // minutos (7 dias = 604800s)

    'algo' => env('JWT_ALGO', 'HS256'),

    'required_claims' => ['iss', 'iat', 'exp', 'nbf', 'sub', 'jti'],

    'persistent_claims' => [],

    'lock_subject' => true,

    'leeway' => (int) env('JWT_LEEWAY', 5), // segundos (clock skew)

    'blacklist_enabled' => env('JWT_BLACKLIST_ENABLED', false),

    'blacklist_grace_period' => env('JWT_BLACKLIST_GRACE_PERIOD', 0),

    'decrypt_cookies' => false,

    'providers' => [
        'jwt' => Lcobucci::class,
        'auth' => Illuminate::class,
        'storage' => PHPOpenSourceSaver\JWTAuth\Providers\Storage\Illuminate::class,
    ],

];

<?php

namespace App\Modules\Auth\Models;

use App\Modules\Auth\Enums\SituacaoUsuario;
use Illuminate\Foundation\Auth\User as Authenticatable;
use PHPOpenSourceSaver\JWTAuth\Contracts\JWTSubject;

/** Credencial e identidade do usuário (auth.login). */
class Login extends Authenticatable implements JWTSubject
{
    protected $table = 'auth.login';

    public $timestamps = false;

    protected $fillable = [
        'id_user',
        'uuid',
        'email',
        'nome_usuario',
        'senha_hash',
        'setor',
        'situacao',
    ];

    protected $hidden = ['senha_hash'];

    protected $casts = [
        'situacao' => SituacaoUsuario::class,
    ];

    public function getAuthPassword(): string
    {
        return $this->senha_hash;
    }

    public function getJWTIdentifier(): mixed
    {
        return $this->getKey();
    }

    public function getJWTCustomClaims(): array
    {
        return [];
    }
}

<?php

namespace App\Modules\Auth\Models;

use Illuminate\Database\Eloquent\Model;

/**
 * Token de integração (auth.token_integracao). Persiste SOMENTE hash
 * SHA-256 + prefixo — nunca a chave em claro.
 */
class TokenIntegracao extends Model
{
    protected $table = 'auth.token_integracao';

    public $timestamps = false;

    protected $fillable = ['nome', 'prefixo', 'hash', 'criado_em', 'ultimo_uso', 'revogado'];

    protected $casts = [
        'criado_em' => 'datetime',
        'ultimo_uso' => 'datetime',
        'revogado' => 'boolean',
    ];
}

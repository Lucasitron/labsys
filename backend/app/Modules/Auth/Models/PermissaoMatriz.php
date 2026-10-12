<?php

namespace App\Modules\Auth\Models;

use App\Modules\Auth\Enums\PermissaoNivel;
use App\Modules\Auth\Enums\Role;
use Illuminate\Database\Eloquent\Model;

/** Célula da matriz RBAC (auth.permissao_matriz, PK composta modulo+role). */
class PermissaoMatriz extends Model
{
    protected $table = 'auth.permissao_matriz';

    public $timestamps = false;

    public $incrementing = false;

    protected $primaryKey = null;

    protected $fillable = ['modulo', 'role', 'nivel'];

    protected $casts = [
        'role' => Role::class,
        'nivel' => PermissaoNivel::class,
    ];
}

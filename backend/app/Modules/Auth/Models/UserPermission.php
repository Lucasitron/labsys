<?php

namespace App\Modules\Auth\Models;

use App\Modules\Auth\Enums\Role;
use Illuminate\Database\Eloquent\Model;

/** Papel RBAC do usuário (auth.user_permissions). Sem "responsabilidade" (é do RH). */
class UserPermission extends Model
{
    protected $table = 'auth.user_permissions';

    public $timestamps = false;

    protected $fillable = ['id_user', 'role', 'active'];

    protected $casts = [
        'role' => Role::class,
        'active' => 'boolean',
    ];
}

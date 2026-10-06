<?php

namespace App\Modules\Auth\Http\Resources;

use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;
use Illuminate\Support\Collection;

/** GET /api/auth/me — dados do autenticado + permissões (role/label/active). */
class MeResource extends JsonResource
{
    /** @var Collection<int, UserPermission> */
    public $permissions;

    /** @param Collection<int, UserPermission> $permissions */
    public function __construct(Login $login, $permissions)
    {
        parent::__construct($login);
        $this->permissions = $permissions;
    }

    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        /** @var Login $login */
        $login = $this->resource;

        return [
            'id' => $login->id,
            'idUser' => $login->id_user,
            'email' => $login->email,
            'nomeUsuario' => $login->nome_usuario,
            'setor' => $login->setor,
            'permissions' => $this->permissions->map(fn (UserPermission $p) => [
                'role' => $p->role->value,
                'label' => $p->role->label(),
                'active' => $p->active,
            ])->values()->all(),
        ];
    }
}

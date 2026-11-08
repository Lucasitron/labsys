<?php

namespace App\Http\Resources\Auth;

use App\Enums\Auth\Role;
use App\Models\Auth\Login;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Conta de acesso (GET/POST/PUT/PATCH /api/usuarios). */
class UsuarioResource extends JsonResource
{
    public ?Role $role;

    public function __construct(Login $login, ?Role $role = null)
    {
        parent::__construct($login);
        $this->role = $role;
    }

    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        /** @var Login $login */
        $login = $this->resource;

        return [
            'id' => $login->id,
            'idUser' => $login->id_user,
            'uuid' => $login->uuid,
            'email' => $login->email,
            'nomeUsuario' => $login->nome_usuario,
            'setor' => $login->setor,
            'situacao' => $login->situacao->value,
            'nivel' => $this->role?->value,
            'nivelNome' => $this->role?->name,
        ];
    }
}

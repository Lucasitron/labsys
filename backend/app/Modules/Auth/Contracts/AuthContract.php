<?php

namespace App\Modules\Auth\Contracts;

use App\Modules\Auth\Enums\Role;

/**
 * Fronteira pública do Auth p/ outros módulos (in-process, sem HTTP).
 * Único ponto de consumo permitido — nunca importar Service/Model do Auth.
 */
interface AuthContract
{
    /**
     * Valida o JWT e devolve identidade + papel.
     *
     * @return array{id:int,idUser:int,role:string,setor:?string}|null null se inválido/revogado
     */
    public function verify(string $token): ?array;

    /** true se o usuário tem permissão ADMIN (0) ativa. */
    public function isAdmin(int $idUser): bool;

    /** Papel da permissão ativa do usuário (null sem permissão ativa). */
    public function roleOf(int $idUser): ?Role;

    /** Ids (`id_user`) com permissão ADMIN ativa (broadcasts — ex. Notification). */
    public function adminIds(): array;

    /**
     * Espelha o nível de acesso do RH no papel RBAC ativo do usuário (`id_user`).
     * No-op (false) quando não há credencial cadastrada — nunca cria permissão órfã.
     */
    public function definirRole(int $idUser, Role $role): bool;
}

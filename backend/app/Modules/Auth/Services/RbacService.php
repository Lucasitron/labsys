<?php

namespace App\Modules\Auth\Services;

use App\Modules\Auth\Enums\Modulo;
use App\Modules\Auth\Enums\PermissaoNivel;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\PermissaoMatriz;
use App\Shared\Exceptions\PermissaoInvalidaException;

/**
 * Matriz RBAC: MATRIX em memória (compat) + células persistentes em
 * auth.permissao_matriz (verdade persistente — PUT sobrevive ao restart).
 */
class RbacService
{
    private const MATRIX = [
        'ADMIN' => ['*'],
        'BOLSISTA' => ['catalogo:read', 'projetos:write', 'equipamentos:reserve', 'ponto:write', 'relatorios:export'],
        'VOLUNTARIO' => ['catalogo:read', 'equipamentos:read'],
        'ESTAGIARIO' => ['catalogo:read', 'equipamentos:read', 'ponto:write'],
        'RECRUTANDO' => ['catalogo:read'],
    ];

    /** @return array{role:string,code:int,label:string,permissions:list<string>} */
    public function getMatrix(Role $role): array
    {
        return [
            'role' => $role->name,
            'code' => $role->value,
            'label' => $role->label(),
            'permissions' => self::MATRIX[$role->name] ?? [],
        ];
    }

    public static function defaultNivel(Role $role): PermissaoNivel
    {
        return match ($role) {
            Role::ADMIN => PermissaoNivel::EDITAR,
            Role::RECRUTANDO => PermissaoNivel::NENHUM,
            default => PermissaoNivel::VER,
        };
    }

    /** @return array{roles:list<array>,matriz:list<array>,enums:array} */
    public function getFullMatrix(): array
    {
        $cells = $this->loadCells();

        $roles = array_map(fn (Role $role) => $this->getMatrix($role), Role::cases());

        $matriz = [];
        foreach (Modulo::cases() as $modulo) {
            foreach (Role::cases() as $role) {
                $nivel = $cells[$modulo->value][$role->value];
                $matriz[] = [
                    'modulo' => $modulo->value,
                    'nivel' => $role->name,
                    'code' => $role->value,
                    'valor' => $nivel->label(),
                ];
            }
        }

        return [
            'roles' => $roles,
            'matriz' => $matriz,
            'enums' => [
                'modulos' => array_map(fn (Modulo $m) => $m->value, Modulo::cases()),
                'niveis' => array_map(fn (Role $r) => $r->name, Role::cases()),
                'valores' => array_map(fn (PermissaoNivel $n) => $n->label(), PermissaoNivel::cases()),
            ],
        ];
    }

    /** @return array{modulo:string,nivel:string,code:int,valor:string} */
    public function updateCell(string $moduloRaw, string $nivelRaw, string $valorRaw): array
    {
        try {
            $modulo = Modulo::parse($moduloRaw);
        } catch (\InvalidArgumentException $e) {
            throw new PermissaoInvalidaException($e->getMessage());
        }

        $role = $this->parseNivel($nivelRaw);

        try {
            $valor = PermissaoNivel::parse($valorRaw);
        } catch (\InvalidArgumentException $e) {
            throw new PermissaoInvalidaException($e->getMessage());
        }

        PermissaoMatriz::query()->updateOrCreate(
            ['modulo' => $modulo->value, 'role' => $role->value],
            ['nivel' => $valor->value]
        );

        return [
            'modulo' => $modulo->value,
            'nivel' => $role->name,
            'code' => $role->value,
            'valor' => $valor->label(),
        ];
    }

    /** @return array<string,array<int,PermissaoNivel>> */
    private function loadCells(): array
    {
        $cells = [];
        foreach (Modulo::cases() as $modulo) {
            foreach (Role::cases() as $role) {
                $cells[$modulo->value][$role->value] = self::defaultNivel($role);
            }
        }

        foreach (PermissaoMatriz::all() as $row) {
            try {
                $modulo = Modulo::parse($row->modulo);
            } catch (\InvalidArgumentException) {
                continue;
            }
            $cells[$modulo->value][$row->role->value] = $row->nivel;
        }

        return $cells;
    }

    private function parseNivel(?string $nivelRaw): Role
    {
        $message = 'Nível inválido: informe 0, 1, 2, 3, 4 ou Admin, Bolsista, Voluntário, Estagiário, Recrutando';

        if ($nivelRaw === null || trim($nivelRaw) === '') {
            throw new PermissaoInvalidaException($message);
        }

        $normalized = trim($nivelRaw);

        if (is_numeric($normalized)) {
            $role = Role::tryFrom((int) $normalized);
            if ($role !== null) {
                return $role;
            }
        }

        foreach (Role::cases() as $role) {
            if (strcasecmp($role->name, $normalized) === 0 || strcasecmp($role->label(), $normalized) === 0) {
                return $role;
            }
        }

        throw new PermissaoInvalidaException($message);
    }
}

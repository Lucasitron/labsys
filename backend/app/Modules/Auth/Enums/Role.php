<?php

namespace App\Modules\Auth\Enums;

/** Níveis de acesso RBAC (user_permissions.role 0-4). */
enum Role: int
{
    case ADMIN = 0;
    case BOLSISTA = 1;
    case VOLUNTARIO = 2;
    case ESTAGIARIO = 3;
    case RECRUTANDO = 4;

    public function label(): string
    {
        return match ($this) {
            self::ADMIN => 'Admin',
            self::BOLSISTA => 'Bolsista',
            self::VOLUNTARIO => 'Voluntário',
            self::ESTAGIARIO => 'Estagiário',
            self::RECRUTANDO => 'Recrutando',
        };
    }

    public static function parse(int|string $value): self
    {
        if ($value instanceof self) {
            return $value;
        }
        if (is_string($value) && ($found = self::tryFromName(mb_strtoupper(trim($value)))) !== null) {
            return $found;
        }

        return self::tryFrom((int) $value)
            ?? throw new \InvalidArgumentException("Papel inválido: {$value}");
    }

    private static function tryFromName(string $name): ?self
    {
        foreach (self::cases() as $case) {
            if ($case->name === $name) {
                return $case;
            }
        }

        return null;
    }
}

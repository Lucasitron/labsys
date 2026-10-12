<?php

namespace App\Modules\Rh\Enums;

/** Nível de acesso do funcionário (0-Admin … 4-Recrutando). Espelha Auth Role. */
enum NivelAcesso: int
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

    public static function parse(int|string|self $value): self
    {
        if ($value instanceof self) {
            return $value;
        }
        if (is_string($value)) {
            $trim = trim($value);
            if (($found = self::tryFromName(mb_strtoupper($trim))) !== null) {
                return $found;
            }
            if (! is_numeric($trim)) {
                throw new \InvalidArgumentException("Nível de acesso inválido: {$value}");
            }
        }

        return self::tryFrom((int) $value)
            ?? throw new \InvalidArgumentException("Nível de acesso inválido: {$value}");
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

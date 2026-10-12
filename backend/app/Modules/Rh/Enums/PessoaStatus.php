<?php

namespace App\Modules\Rh\Enums;

/** Status da pessoa (0-Ativo, 1-Inativo, 2-Recrutando). */
enum PessoaStatus: int
{
    case ATIVO = 0;
    case INATIVO = 1;
    case RECRUTANDO = 2;

    public function label(): string
    {
        return match ($this) {
            self::ATIVO => 'Ativo',
            self::INATIVO => 'Inativo',
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
                throw new \InvalidArgumentException("Status de pessoa inválido: {$value}");
            }
        }

        return self::tryFrom((int) $value)
            ?? throw new \InvalidArgumentException("Status de pessoa inválido: {$value}");
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

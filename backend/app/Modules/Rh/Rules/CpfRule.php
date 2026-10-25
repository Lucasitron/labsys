<?php

namespace App\Modules\Rh\Rules;

use Closure;
use Illuminate\Contracts\Validation\ValidationRule;

/**
 * CPF opcional com dígitos verificadores (port de CpfUtil; sem equivalente nativo).
 * Persistido só com dígitos; a API responde sempre mascarado (LGPD).
 */
class CpfRule implements ValidationRule
{
    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        if ($value === null || trim((string) $value) === '') {
            return;
        }

        if (! self::valido((string) $value)) {
            $fail('CPF inválido.');
        }
    }

    /** Normaliza para só-dígitos (nulo quando ausente/em branco). */
    public static function normalizar(?string $cpf): ?string
    {
        if ($cpf === null || trim($cpf) === '') {
            return null;
        }

        $digitos = (string) preg_replace('/\D/', '', $cpf);

        return $digitos === '' ? null : $digitos;
    }

    /** 11 dígitos com check-digits válidos (aceita formatado ou não). */
    public static function valido(string $cpf): bool
    {
        $digitos = self::normalizar($cpf);

        if ($digitos === null || strlen($digitos) !== 11) {
            return false;
        }

        // Rejeita sequências triviais (000…, 111…, …).
        if (count(array_unique(str_split($digitos))) === 1) {
            return false;
        }

        return substr($digitos, 9, 1) === (string) self::digito($digitos, 9, 10)
            && substr($digitos, 10, 1) === (string) self::digito($digitos, 10, 11);
    }

    private static function digito(string $digitos, int $tamanho, int $pesoInicial): int
    {
        $soma = 0;
        for ($i = 0; $i < $tamanho; $i++) {
            $soma += ((int) $digitos[$i]) * ($pesoInicial - $i);
        }
        $resto = $soma % 11;

        return $resto < 2 ? 0 : 11 - $resto;
    }

    /** Máscara LGPD `***.***.***-XX` (nulo quando ausente). */
    public static function mascarar(?string $cpfNormalizado): ?string
    {
        if ($cpfNormalizado === null || strlen($cpfNormalizado) !== 11) {
            return null;
        }

        return '***.***.***-'.substr($cpfNormalizado, 9);
    }
}

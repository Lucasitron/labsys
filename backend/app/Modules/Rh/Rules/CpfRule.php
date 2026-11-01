<?php

namespace App\Modules\Rh\Rules;

use App\Rules\CpfCnpjRule;
use Closure;
use Illuminate\Contracts\Validation\ValidationRule;

/**
 * CPF opcional com dígitos verificadores (port de CpfUtil; sem equivalente nativo).
 * Persistido só com dígitos; a API responde sempre mascarado (LGPD).
 * Delega o algoritmo a App\Rules\CpfCnpjRule (sem duplicação).
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
        return CpfCnpjRule::normalizar($cpf);
    }

    /** 11 dígitos com check-digits válidos (aceita formatado ou não). Rejeita CNPJ. */
    public static function valido(string $cpf): bool
    {
        $digitos = CpfCnpjRule::normalizar($cpf);

        if ($digitos === null || strlen($digitos) !== 11) {
            return false;
        }

        return CpfCnpjRule::cpfValido($digitos);
    }

    /** Máscara LGPD `***.***.***-XX` (nulo quando ausente). */
    public static function mascarar(?string $cpfNormalizado): ?string
    {
        return CpfCnpjRule::mascararCpf($cpfNormalizado);
    }
}

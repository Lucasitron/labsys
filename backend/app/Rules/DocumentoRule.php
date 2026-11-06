<?php

namespace App\Rules;

use App\Rules\CpfCnpjRule;
use Closure;
use Illuminate\Contracts\Validation\ValidationRule;

/**
 * CPF **e** CNPJ com dígitos verificadores. Persistido só com dígitos; a API
 * responde sempre mascarado (LGPD, na Resource). Delega a App\Rules\CpfCnpjRule.
 */
class DocumentoRule implements ValidationRule
{
    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        if ($value === null || trim((string) $value) === '') {
            return;
        }

        if (! self::valido((string) $value)) {
            $fail('CPF ou CNPJ inválido.');
        }
    }

    /** Normaliza para só-dígitos (nulo quando ausente/em branco). */
    public static function normalizar(?string $documento): ?string
    {
        return CpfCnpjRule::normalizar($documento);
    }

    /** 11 (CPF) ou 14 (CNPJ) dígitos com check-digits válidos. */
    public static function valido(string $documento): bool
    {
        return CpfCnpjRule::valido($documento);
    }

    /**
     * Máscara LGPD: só os limites do documento
     * (`***.000.000-**` / `**. 000.000/0000-**`).
     */
    public static function mascarar(?string $documento): ?string
    {
        return CpfCnpjRule::mascararDocumento($documento);
    }
}

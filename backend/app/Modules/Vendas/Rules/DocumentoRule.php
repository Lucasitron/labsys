<?php

namespace App\Modules\Vendas\Rules;

use Closure;
use Illuminate\Contracts\Validation\ValidationRule;

/**
 * CPF **e** CNPJ com dígitos verificadores (port de DocumentoUtil; o CpfRule
 * do Rh não cobre CNPJ — create justificado). Persistido só com dígitos; a
 * API responde sempre mascarado (LGPD, na Resource).
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
        if ($documento === null || trim($documento) === '') {
            return null;
        }

        $digitos = (string) preg_replace('/\D/', '', $documento);

        return $digitos === '' ? null : $digitos;
    }

    /** 11 (CPF) ou 14 (CNPJ) dígitos com check-digits válidos. */
    public static function valido(string $documento): bool
    {
        $digitos = self::normalizar($documento);

        if ($digitos === null) {
            return false;
        }

        if (strlen($digitos) === 11) {
            return self::cpfValido($digitos);
        }

        if (strlen($digitos) === 14) {
            return self::cnpjValido($digitos);
        }

        return false;
    }

    /**
     * Máscara LGPD: só os limites do documento
     * (`***.000.000-**` / `**. 000.000/0000-**`).
     */
    public static function mascarar(?string $documento): ?string
    {
        $digitos = self::normalizar($documento);

        if ($digitos === null) {
            return null;
        }

        if (strlen($digitos) === 11) {
            return '***.'.substr($digitos, 3, 3).'.'.substr($digitos, 6, 3).'-**';
        }

        if (strlen($digitos) === 14) {
            return '**. '.substr($digitos, 2, 3).'.'.substr($digitos, 5, 3)
                .'/'.substr($digitos, 8, 4).'-**';
        }

        return '***';
    }

    private static function todosIguais(string $digitos): bool
    {
        return count(array_unique(str_split($digitos))) === 1;
    }

    private static function cpfValido(string $cpf): bool
    {
        if (self::todosIguais($cpf)) {
            return false;
        }

        $soma = 0;
        for ($i = 0; $i < 9; $i++) {
            $soma += ((int) $cpf[$i]) * (10 - $i);
        }
        $dv1 = 11 - ($soma % 11);
        $dv1 = $dv1 >= 10 ? 0 : $dv1;
        if ($dv1 !== (int) $cpf[9]) {
            return false;
        }

        $soma = 0;
        for ($i = 0; $i < 10; $i++) {
            $soma += ((int) $cpf[$i]) * (11 - $i);
        }
        $dv2 = 11 - ($soma % 11);
        $dv2 = $dv2 >= 10 ? 0 : $dv2;

        return $dv2 === (int) $cpf[10];
    }

    private static function cnpjValido(string $cnpj): bool
    {
        if (self::todosIguais($cnpj)) {
            return false;
        }

        $peso1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
        $peso2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

        $soma = 0;
        for ($i = 0; $i < 12; $i++) {
            $soma += ((int) $cnpj[$i]) * $peso1[$i];
        }
        $dv1 = $soma % 11;
        $dv1 = $dv1 < 2 ? 0 : 11 - $dv1;
        if ($dv1 !== (int) $cnpj[12]) {
            return false;
        }

        $soma = 0;
        for ($i = 0; $i < 13; $i++) {
            $soma += ((int) $cnpj[$i]) * $peso2[$i];
        }
        $dv2 = $soma % 11;
        $dv2 = $dv2 < 2 ? 0 : 11 - $dv2;

        return $dv2 === (int) $cnpj[13];
    }
}

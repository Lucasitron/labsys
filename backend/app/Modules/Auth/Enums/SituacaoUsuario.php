<?php

namespace App\Modules\Auth\Enums;

/** Situação da conta de acesso (login.situacao). */
enum SituacaoUsuario: string
{
    case ATIVO = 'ATIVO';
    case PENDENTE = 'PENDENTE';
    case DESATIVADO = 'DESATIVADO';

    public static function parse(?string $value): self
    {
        return self::tryFrom(mb_strtoupper(trim((string) $value)))
            ?? throw new \InvalidArgumentException("Situação inválida: {$value}");
    }
}

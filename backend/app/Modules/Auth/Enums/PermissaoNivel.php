<?php

namespace App\Modules\Auth\Enums;

/** Nível de uma célula da matriz RBAC (permissao_matriz.nivel). */
enum PermissaoNivel: string
{
    case VER = 'VER';
    case EDITAR = 'EDITAR';
    case NENHUM = 'NENHUM';

    public function label(): string
    {
        return match ($this) {
            self::VER => 'Visualizar',
            self::EDITAR => 'Editar',
            self::NENHUM => 'Sem acesso',
        };
    }

    public static function parse(?string $value): self
    {
        return self::tryFrom(mb_strtoupper(trim((string) $value)))
            ?? throw new \InvalidArgumentException("Nível de permissão inválido: {$value}");
    }
}

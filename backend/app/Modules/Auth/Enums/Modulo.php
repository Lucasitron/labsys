<?php

namespace App\Modules\Auth\Enums;

/** Módulos do sistema na matriz RBAC (espelha o front: auth.ts). */
enum Modulo: string
{
    case DASHBOARD = 'dashboard';
    case RH = 'rh';
    case ESTOQUE = 'estoque';
    case VENDAS = 'vendas';
    case FINANCEIRO = 'financeiro';
    case PRODUCAO = 'producao';
    case NOTIFICACOES = 'notificacoes';
    case CONFIGURACOES = 'configuracoes';

    public static function parse(?string $value): self
    {
        $normalized = mb_strtolower(trim((string) $value));
        foreach (self::cases() as $case) {
            if ($case->value === $normalized || mb_strtolower($case->name) === $normalized) {
                return $case;
            }
        }

        throw new \InvalidArgumentException(
            'Módulo inválido: informe dashboard, rh, estoque, vendas, financeiro, producao, notificacoes ou configuracoes'
        );
    }
}

<?php

namespace App\Modules\Producao\Enums;

/** Colunas do Kanban de produção. */
enum KanbanStatus: string
{
    case FILA = 'FILA';
    case PRODUCAO = 'PRODUCAO';
    case ACABAMENTO = 'ACABAMENTO';
    case PRONTO = 'PRONTO';
    case ENTREGUE = 'ENTREGUE';

    /** Rótulo do Vendas (`StatusKanban`) — sync via `producao.status.alterado.event`. */
    public function rotuloVendas(): string
    {
        return match ($this) {
            self::FILA => 'Fila',
            self::PRODUCAO => 'Produção',
            self::ACABAMENTO => 'Acabamento',
            self::PRONTO => 'Pronto',
            self::ENTREGUE => 'Entregue',
        };
    }
}

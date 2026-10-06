<?php

namespace App\Modules\Vendas\Enums;

/** Colunas do Kanban de encomendas (ck_encomenda_status_kanban). */
enum StatusKanban: string
{
    case FILA = 'Fila';
    case PRODUCAO = 'Produção';
    case ACABAMENTO = 'Acabamento';
    case PRONTO = 'Pronto';
    case ENTREGUE = 'Entregue';
}

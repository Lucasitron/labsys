<?php

namespace App\Modules\Estoque\Models;

use App\Modules\Estoque\Enums\StatusEmprestimo;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Empréstimo de equipamento/ferramenta (estoque.emprestimo). */
class Emprestimo extends Model
{
    protected $table = 'estoque.emprestimo';

    protected $primaryKey = 'id_emprestimo';

    public $timestamps = false;

    protected $fillable = [
        'id_item',
        'id_pessoa',
        'quantidade',
        'data_emprestimo',
        'data_devolucao_prevista',
        'data_devolucao_real',
        'status',
        'observacao',
        'responsavel',
    ];

    protected $casts = [
        'quantidade' => 'decimal:2',
        'data_emprestimo' => 'date',
        'data_devolucao_prevista' => 'date',
        'data_devolucao_real' => 'date',
        'status' => StatusEmprestimo::class,
    ];

    public function item(): BelongsTo
    {
        return $this->belongsTo(Item::class, 'id_item', 'id_item');
    }
}

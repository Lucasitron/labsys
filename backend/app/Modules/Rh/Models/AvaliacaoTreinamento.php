<?php

namespace App\Modules\Rh\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Avaliação de treinamento — nota 0-10 + feedback. */
class AvaliacaoTreinamento extends Model
{
    protected $table = 'rh.avaliacao_treinamento';

    public $timestamps = false;

    protected $fillable = [
        'id_treinamento',
        'id_funcionario',
        'nota',
        'feedback',
        'data_avaliacao',
    ];

    protected $casts = [
        'nota' => 'decimal:2',
        'data_avaliacao' => 'date',
    ];

    public function treinamento(): BelongsTo
    {
        return $this->belongsTo(Treinamento::class, 'id_treinamento');
    }

    public function funcionario(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_funcionario');
    }
}

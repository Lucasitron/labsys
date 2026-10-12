<?php

namespace App\Modules\Rh\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Tutor: responde por grupo/PS/ensaio (distinto de instrutor, que ministra). */
class Tutor extends Model
{
    protected $table = 'rh.tutor';

    public $timestamps = false;

    protected $fillable = ['id_funcionario', 'turno', 'qualificacao'];

    protected $casts = [
        'qualificacao' => 'integer',
    ];

    public function funcionario(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_funcionario');
    }
}

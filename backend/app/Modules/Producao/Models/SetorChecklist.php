<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Item de checklist do setor (producao.setor_checklist). Sem peso/ordem (sem coluna upstream). */
class SetorChecklist extends Model
{
    protected $table = 'producao.setor_checklist';

    protected $primaryKey = 'id_checklist';

    public $timestamps = false;

    protected $fillable = [
        'id_setor',
        'item',
        'ativo',
    ];

    protected $casts = [
        'ativo' => 'boolean',
    ];

    public function setor(): BelongsTo
    {
        return $this->belongsTo(Setor::class, 'id_setor', 'id_setor');
    }
}

<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Material esperado do setor (producao.setor_material). */
class SetorMaterial extends Model
{
    protected $table = 'producao.setor_material';

    protected $primaryKey = 'id_material';

    public $timestamps = false;

    protected $fillable = [
        'id_setor',
        'descricao',
        'quantidade',
    ];

    protected $casts = [
        'quantidade' => 'decimal:2',
    ];

    public function setor(): BelongsTo
    {
        return $this->belongsTo(Setor::class, 'id_setor', 'id_setor');
    }
}

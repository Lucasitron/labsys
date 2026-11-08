<?php

namespace App\Models\Rh;

use App\Enums\Rh\NivelAcesso;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Vínculo pessoa → nível de acesso (rh.funcionario). */
class Funcionario extends Model
{
    protected $table = 'rh.funcionario';

    public $timestamps = false;

    protected $fillable = ['id_pessoa', 'nivel_acesso', 'departamento'];

    protected $casts = [
        'nivel_acesso' => NivelAcesso::class,
    ];

    public function pessoa(): BelongsTo
    {
        return $this->belongsTo(Pessoa::class, 'id_pessoa');
    }
}

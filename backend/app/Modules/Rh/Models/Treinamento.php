<?php

namespace App\Modules\Rh\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Treinamento do LMS (ministrado por tutor — vínculo, não texto livre). */
class Treinamento extends Model
{
    protected $table = 'rh.treinamento';

    public $timestamps = false;

    protected $fillable = ['titulo', 'descricao', 'url_conteudo', 'id_tutor'];

    public function tutor(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_tutor');
    }
}

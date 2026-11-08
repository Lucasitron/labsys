<?php

namespace App\Models\Rh;

use App\Enums\Rh\PessoaStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasOne;

/** Pessoa do FabLab (rh.pessoa). `id` = referência externa do Auth (`id_user`). */
class Pessoa extends Model
{
    protected $table = 'rh.pessoa';

    public $timestamps = false;

    // `id` é a referência externa do Auth (`id_user`): vínculo explícito
    // permitido (a API nunca o recebe — controllers não o passam).
    protected $fillable = [
        'id',
        'nome_completo',
        'matricula',
        'data_admissao',
        'contato',
        'turno',
        'status',
        'cpf',
    ];

    protected $casts = [
        'data_admissao' => 'date',
        'status' => PessoaStatus::class,
    ];

    public function funcionario(): HasOne
    {
        return $this->hasOne(Funcionario::class, 'id_pessoa');
    }
}

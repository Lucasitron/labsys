<?php

namespace App\Modules\Notification\Models;

use App\Modules\Notification\Enums\StatusEntrega;
use Illuminate\Database\Eloquent\Model;

/** Notificação ativa (notification.notificacao — Java V1+V2 + lifecycle docs/07 §4). */
class Notificacao extends Model
{
    protected $table = 'notification.notificacao';

    public $timestamps = false;

    protected $fillable = [
        'id_usuario',
        'titulo',
        'mensagem',
        'tipo',
        'canal',
        'link',
        'lida',
        'criada_em',
        'status',
        'data_envio',
        'data_leitura',
        'id_referencia',
    ];

    protected $casts = [
        'lida' => 'boolean',
        'criada_em' => 'datetime',
        'status' => StatusEntrega::class,
        'data_envio' => 'datetime',
        'data_leitura' => 'datetime',
    ];
}

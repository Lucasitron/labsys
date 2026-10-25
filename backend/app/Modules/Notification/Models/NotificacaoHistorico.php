<?php

namespace App\Modules\Notification\Models;

use App\Modules\Notification\Enums\StatusEntrega;
use Illuminate\Database\Eloquent\Model;

/** Notificação revisada pelo Admin (notification.notificacao_historico — expurgo >90d). */
class NotificacaoHistorico extends Model
{
    protected $table = 'notification.notificacao_historico';

    protected $primaryKey = 'id_historico';

    public $timestamps = false;

    protected $fillable = [
        'id_notificacao_original',
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
        'data_revisao_admin',
        'id_admin_revisor',
    ];

    protected $casts = [
        'lida' => 'boolean',
        'criada_em' => 'datetime',
        'status' => StatusEntrega::class,
        'data_envio' => 'datetime',
        'data_leitura' => 'datetime',
        'data_revisao_admin' => 'datetime',
    ];
}

<?php

namespace App\Modules\Notification\Models;

use Illuminate\Database\Eloquent\Model;

/** Canal de envio (notification.configuracao_canal — docs/07 §4 1:1). */
class ConfiguracaoCanal extends Model
{
    protected $table = 'notification.configuracao_canal';

    protected $primaryKey = 'id_configuracao';

    public $timestamps = false;

    protected $fillable = ['canal', 'habilitado', 'parametros'];

    protected $casts = [
        'habilitado' => 'boolean',
        'parametros' => 'array',
    ];
}

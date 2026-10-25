<?php

namespace App\Modules\Notification\Models;

use Illuminate\Database\Eloquent\Model;

/** Preferência tipo×canal (notification.preferencia_notificacao — matriz Java persistida). */
class PreferenciaNotificacao extends Model
{
    protected $table = 'notification.preferencia_notificacao';

    public $timestamps = false;

    protected $fillable = ['tipo', 'canal', 'habilitado'];

    protected $casts = [
        'habilitado' => 'boolean',
    ];
}

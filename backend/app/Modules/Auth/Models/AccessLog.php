<?php

namespace App\Modules\Auth\Models;

use App\Modules\Auth\Enums\AccessLogType;
use Illuminate\Database\Eloquent\Model;

/** Registro de acesso físico / ponto (auth.access_log). */
class AccessLog extends Model
{
    protected $table = 'auth.access_log';

    public $timestamps = false;

    protected $fillable = ['id_user', 'uuid_rfid', 'timestamp', 'type'];

    protected $casts = [
        'timestamp' => 'datetime',
        'type' => AccessLogType::class,
    ];
}

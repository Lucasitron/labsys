<?php

namespace App\Modules\Auth\Models;

use Illuminate\Database\Eloquent\Model;

/** Token JWT revogado (auth.token_blacklist). */
class TokenBlacklist extends Model
{
    protected $table = 'auth.token_blacklist';

    public $timestamps = false;

    protected $fillable = ['token', 'expiry_date'];

    protected $casts = [
        'expiry_date' => 'datetime',
    ];
}

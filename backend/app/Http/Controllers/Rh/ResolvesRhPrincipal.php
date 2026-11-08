<?php

namespace App\Http\Controllers\Rh;

use App\Models\Auth\Login;
use App\Services\Rh\RhPrincipal;
use Illuminate\Http\Request;

trait ResolvesRhPrincipal
{
    protected function principal(Request $request): RhPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return RhPrincipal::from($login);
    }
}

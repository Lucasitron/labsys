<?php

namespace App\Modules\Rh\Http\Controllers\Concerns;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\RhPrincipal;
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

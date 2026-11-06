<?php

namespace App\Http\Controllers;

use App\Models\Auth\Login;
use App\Shared\Auth\Caller;
use Illuminate\Http\Request;

abstract class Controller
{
    protected function principal(Request $request): Caller
    {
        $login = $request->user();

        if (! $login instanceof Login) {
            abort(401);
        }

        return Caller::from($login);
    }
}

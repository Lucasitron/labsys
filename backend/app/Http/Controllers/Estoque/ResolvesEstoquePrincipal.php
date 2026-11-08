<?php

namespace App\Http\Controllers\Estoque;

use App\Models\Auth\Login;
use App\Services\Estoque\EstoquePrincipal;
use Illuminate\Http\Request;

trait ResolvesEstoquePrincipal
{
    protected function principal(Request $request): EstoquePrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return EstoquePrincipal::from($login);
    }
}

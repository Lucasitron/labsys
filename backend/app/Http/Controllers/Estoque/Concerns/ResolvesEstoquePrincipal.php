<?php

namespace App\Http\Controllers\Estoque\Concerns;

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

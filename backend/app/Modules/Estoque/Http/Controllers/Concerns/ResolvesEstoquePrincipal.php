<?php

namespace App\Modules\Estoque\Http\Controllers\Concerns;

use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\EstoquePrincipal;
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

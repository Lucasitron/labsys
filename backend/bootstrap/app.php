<?php

use App\Modules\Auth\Http\Middleware\CheckBlacklist;
use App\Shared\Exceptions\ConfiguracaoInvalidaException;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ErrorResponse;
use App\Shared\Exceptions\ForbiddenException;
use App\Shared\Exceptions\InvalidCredentialsException;
use App\Shared\Exceptions\PermissaoInvalidaException;
use App\Shared\Exceptions\ResourceNotFoundException;
use App\Shared\Exceptions\SaldoInsuficienteException;
use App\Shared\Exceptions\TokenBlacklistedException;
use App\Shared\Exceptions\TokenInvalidException;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Auth\AuthenticationException;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->alias([
            'blacklist' => CheckBlacklist::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $map = [
            InvalidCredentialsException::class => 401,
            TokenInvalidException::class => 401,
            TokenBlacklistedException::class => 401,
            AuthenticationException::class => 401,
            ForbiddenException::class => 403,
            AuthorizationException::class => 403,
            AccessDeniedHttpException::class => 403,
            ResourceNotFoundException::class => 404,
            NotFoundHttpException::class => 404,
            InvalidArgumentException::class => 400,
            SaldoInsuficienteException::class => 409,
            ConflitoException::class => 409,
            ConfiguracaoInvalidaException::class => 422,
            PermissaoInvalidaException::class => 422,
        ];

        foreach ($map as $class => $status) {
            $exceptions->render(function (Throwable $e, $request) use ($class, $status) {
                if (! $e instanceof $class) {
                    return null;
                }

                return ErrorResponse::make($status, $e->getMessage(), $request->path());
            });
        }

        $exceptions->render(function (ValidationException $e, $request) {
            return ErrorResponse::make(422, 'Dados inválidos', $request->path(), $e->errors());
        });
    })->create();

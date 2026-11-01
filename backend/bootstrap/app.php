<?php

use App\Shared\Exceptions\ApiException;
use App\Shared\Exceptions\ErrorResponse;
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
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->alias([
            'blacklist' => \App\Modules\Auth\Http\Middleware\CheckBlacklist::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->render(function (ApiException $e, $request) {
            return ErrorResponse::make($e->status, $e->getMessage(), $request->path());
        });

        $exceptions->render(function (AuthenticationException $e, $request) {
            return ErrorResponse::make(401, $e->getMessage(), $request->path());
        });

        $exceptions->render(function (AuthorizationException $e, $request) {
            return ErrorResponse::make(403, $e->getMessage(), $request->path());
        });

        $exceptions->render(function (AccessDeniedHttpException $e, $request) {
            return ErrorResponse::make(403, $e->getMessage(), $request->path());
        });

        $exceptions->render(function (NotFoundHttpException $e, $request) {
            return ErrorResponse::make(404, $e->getMessage(), $request->path());
        });

        $exceptions->render(function (\InvalidArgumentException $e, $request) {
            return ErrorResponse::make(422, $e->getMessage(), $request->path());
        });

        $exceptions->render(function (ValidationException $e, $request) {
            return ErrorResponse::make(422, 'Dados inválidos', $request->path(), $e->errors());
        });
    })->create();
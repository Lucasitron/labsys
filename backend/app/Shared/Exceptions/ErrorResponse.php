<?php

namespace App\Shared\Exceptions;

use Illuminate\Http\JsonResponse;

/**
 * Corpo de erro JSON padronizado da API (equivale ao ErrorResponse +
 * GlobalExceptionHandler do auth-service: 401 credenciais/token,
 * 403 RBAC, 404 recurso, 400/422 validação).
 */
class ErrorResponse
{
    public static function make(int $status, string $message, string $path, array $errors = []): JsonResponse
    {
        $body = [
            'timestamp' => now()->toIso8601String(),
            'status' => $status,
            'error' => JsonResponse::$statusTexts[$status] ?? 'Erro',
            'message' => $message,
            'path' => $path,
        ];

        if ($errors !== []) {
            $body['errors'] = $errors;
        }

        return response()->json($body, $status);
    }
}

<?php

namespace App\Shared\Exceptions;

class InvalidCredentialsException extends ApiException
{
    public function __construct(string $message = 'Credenciais inválidas')
    {
        parent::__construct($message, 401);
    }
}

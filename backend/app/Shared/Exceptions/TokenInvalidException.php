<?php

namespace App\Shared\Exceptions;

class TokenInvalidException extends \RuntimeException
{
    public function __construct(string $message = 'Token inválido ou expirado')
    {
        parent::__construct($message);
    }
}

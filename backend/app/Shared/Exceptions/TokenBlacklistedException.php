<?php

namespace App\Shared\Exceptions;

class TokenBlacklistedException extends ApiException
{
    public function __construct(string $message = 'Token revogado')
    {
        parent::__construct($message, 401);
    }
}

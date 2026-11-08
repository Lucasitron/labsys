<?php

namespace App\Exceptions;

class TokenBlacklistedException extends ApiException
{
    public function __construct(string $message = 'Token revogado')
    {
        parent::__construct($message, 401);
    }
}

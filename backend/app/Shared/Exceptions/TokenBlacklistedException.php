<?php

namespace App\Shared\Exceptions;

class TokenBlacklistedException extends \RuntimeException
{
    public function __construct(string $message = 'Token revogado')
    {
        parent::__construct($message);
    }
}

<?php

namespace App\Shared\Exceptions;

class PermissaoInvalidaException extends ApiException
{
    public function __construct(string $message = '')
    {
        parent::__construct($message, 422);
    }
}

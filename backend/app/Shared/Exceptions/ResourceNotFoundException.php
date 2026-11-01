<?php

namespace App\Shared\Exceptions;

class ResourceNotFoundException extends ApiException
{
    public function __construct(string $message = 'Recurso não encontrado')
    {
        parent::__construct($message, 404);
    }
}

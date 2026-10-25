<?php

namespace App\Shared\Exceptions;

class ResourceNotFoundException extends \RuntimeException
{
    public function __construct(string $message = 'Recurso não encontrado')
    {
        parent::__construct($message);
    }
}

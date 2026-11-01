<?php

namespace App\Shared\Exceptions;

/** Estoque insuficiente para a baixa (saída/consumo/empréstimo nunca negativam). */
class SaldoInsuficienteException extends ApiException
{
    public function __construct(string $message = '')
    {
        parent::__construct($message, 409);
    }
}

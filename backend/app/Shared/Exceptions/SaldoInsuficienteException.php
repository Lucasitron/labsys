<?php

namespace App\Shared\Exceptions;

/** Estoque insuficiente para a baixa (saída/consumo/empréstimo nunca negativam). */
class SaldoInsuficienteException extends \RuntimeException {}

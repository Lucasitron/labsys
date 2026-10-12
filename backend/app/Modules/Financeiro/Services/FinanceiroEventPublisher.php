<?php

namespace App\Modules\Financeiro\Services;

use App\Modules\Financeiro\Events\CompraSolicitadaEvent;
use App\Modules\Financeiro\Events\CustoCalculadoEvent;
use App\Modules\Financeiro\Events\LancamentoVencidoEvent;
use Illuminate\Support\Facades\Log;

/**
 * Publicação fail-soft dos eventos do Financeiro (fila `database`, nomes
 * preservados): falha de fila nunca estoura 500 na operação — equivale ao
 * try/catch do `FinanceiroEventPublisher` com `RabbitTemplate` (descartado).
 */
final class FinanceiroEventPublisher
{
    public function lancamentoVencido(int $id, string $valor, string $vencimento, ?string $referencia): void
    {
        $this->publicar(
            fn () => event(new LancamentoVencidoEvent($id, $valor, $vencimento, $referencia)),
            'lancamento.vencido',
        );
    }

    public function custoCalculado(int $encomenda, string $total, string $margem, string $data): void
    {
        $this->publicar(
            fn () => event(new CustoCalculadoEvent($encomenda, $total, $margem, $data)),
            'custo.calculado',
        );
    }

    public function compraSolicitada(int $id): void
    {
        $this->publicar(fn () => event(new CompraSolicitadaEvent($id, null)), 'compra.solicitada');
    }

    private function publicar(callable $dispatch, string $nome): void
    {
        try {
            $dispatch();
        } catch (\Throwable $e) {
            Log::warning("Falha ao publicar {$nome}.event: {$e->getMessage()}");
        }
    }
}

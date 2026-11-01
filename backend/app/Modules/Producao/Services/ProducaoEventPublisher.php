<?php

namespace App\Modules\Producao\Services;

use App\Events\ProducaoConcluidaEvent;
use App\Events\ProducaoStatusAlteradoEvent;
use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Events\AdvertenciaLimiteAtingidoEvent;
use App\Modules\Producao\Events\AdvertenciaRegistradaEvent;
use App\Modules\Producao\Events\KanbanStatusAlteradoEvent;
use App\Modules\Producao\Events\ProjetoMesaAbandonadoEvent;
use Illuminate\Support\Facades\Log;

/**
 * Publicação fail-soft dos eventos do Producao (fila `database`, nomes
 * preservados): falha de fila nunca estoura 500 na operação — equivale ao
 * try/catch do `ProducaoEventPublisher` com `RabbitTemplate` (descartado).
 *
 * Os eventos transversais (`producao.status.alterado.event` /
 * `producao.concluida.event`) vivem em `App\Events` — um consumidor
 * (Estoque/Vendas/Financeiro/Notification) por classe, um único dispatch.
 */
final class ProducaoEventPublisher
{
    public function statusAlterado(int $idEncomenda, KanbanStatus $novo, ?int $idUsuario, ?string $observacao): void
    {
        $this->publicar(
            function () use ($idEncomenda, $novo, $idUsuario, $observacao): void {
                event(new ProducaoStatusAlteradoEvent($idEncomenda, $novo->value, $idUsuario, $observacao, $novo->rotuloVendas()));
            },
            'producao.status.alterado',
        );
    }

    public function kanbanStatusAlterado(int $idEncomenda, ?KanbanStatus $anterior, KanbanStatus $novo): void
    {
        $this->publicar(
            fn () => event(new KanbanStatusAlteradoEvent(
                $idEncomenda, $anterior?->value, $novo->value, now()->toDateTimeString(),
            )),
            'kanban.status.alterado',
        );
    }

    /**
     * @param list<array{idItem:int,quantidadeConsumida:string}> $itens BOM final
     */
    public function producaoConcluida(int $idEncomenda, array $itens): void
    {
        $this->publicar(
            function () use ($idEncomenda, $itens): void {
                event(new ProducaoConcluidaEvent($idEncomenda, null, $itens, today()->toDateString()));
            },
            'producao.concluida',
        );
    }

    public function advertencia(int $idFuncionario, int $contador, string $motivo): void
    {
        $this->publicar(
            fn () => event(new AdvertenciaRegistradaEvent($idFuncionario, $contador, $motivo)),
            'advertencia.registrada',
        );
    }

    public function advertenciaLimite(int $idFuncionario, int $contador, string $motivo): void
    {
        $this->publicar(
            fn () => event(new AdvertenciaLimiteAtingidoEvent($idFuncionario, $contador, $motivo)),
            'advertencia.limite.atingido',
        );
    }

    public function mesaAbandonada(int $idProjetoMesa, int $idFuncionario, ?string $acaoTomada): void
    {
        $this->publicar(
            fn () => event(new ProjetoMesaAbandonadoEvent($idProjetoMesa, $idFuncionario, $acaoTomada)),
            'projeto.mesa.abandonado',
        );
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

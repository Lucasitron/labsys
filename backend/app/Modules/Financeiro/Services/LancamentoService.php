<?php

namespace App\Modules\Financeiro\Services;

use App\Modules\Financeiro\Enums\StatusLancamento;
use App\Modules\Financeiro\Enums\TipoLancamento;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Models\CategoriaFinanceira;
use App\Modules\Financeiro\Models\LancamentoFinanceiro;
use App\Modules\Financeiro\Policies\FinanceiroPolicy;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

/**
 * Contas a pagar/receber: criação, listagem com filtros (D-4/D-8) e shapes
 * servidos (`counts`+`resumo`, D-2), liquidação e varredura de vencidos.
 * Aritmética em centavos (int); `valor_total` nunca confiado do payload.
 */
class LancamentoService
{
    public function __construct(private FinanceiroEventPublisher $publisher) {}

    public function criar(array $dados, FinanceiroPrincipal $principal): LancamentoFinanceiro
    {
        FinanceiroPolicy::exigeAdmin($principal);

        CategoriaFinanceira::find($dados['idCategoria'])
            ?? throw new ResourceNotFoundException("Categoria não encontrada: {$dados['idCategoria']}");

        return DB::transaction(function () use ($dados) {
            $vencido = $dados['dataVencimento'] < today()->toDateString();

            $lancamento = LancamentoFinanceiro::create([
                'id_categoria' => $dados['idCategoria'],
                'tipo' => TipoLancamento::from($dados['tipo']),
                'valor' => number_format((float) $dados['valor'], 2, '.', ''),
                'data_vencimento' => $dados['dataVencimento'],
                'status' => $vencido ? StatusLancamento::ATRASADO : StatusLancamento::PENDENTE,
                'id_referencia_externa' => $dados['idReferenciaExterna'] ?? null,
                'observacao' => $dados['observacao'] ?? null,
            ]);

            if ($vencido) {
                $this->publisher->lancamentoVencido(
                    (int) $lancamento->getKey(),
                    (string) $lancamento->valor,
                    substr((string) $lancamento->data_vencimento, 0, 10),
                    $lancamento->id_referencia_externa,
                );
            }

            return $lancamento;
        });
    }

    /**
     * @return array{lancamentos:list<LancamentoFinanceiro>,counts:array<string,int>,resumo:array<string,string>}
     */
    public function listar(array $filtros, FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);

        $inicio = $filtros['dataInicio'] ?? $filtros['vencimentoDe'] ?? null;
        $fim = $filtros['dataFim'] ?? $filtros['vencimentoAte'] ?? null;
        $origem = isset($filtros['origem']) && trim((string) $filtros['origem']) !== ''
            ? trim((string) $filtros['origem']) : null;

        $filtrados = LancamentoFinanceiro::query()->with('categoria')
            ->when(isset($filtros['status']), fn ($q) => $q->where('status', $filtros['status']))
            ->when(isset($filtros['tipo']), fn ($q) => $q->where('tipo', $filtros['tipo']))
            ->when(isset($filtros['idCategoria']), fn ($q) => $q->where('id_categoria', $filtros['idCategoria']))
            ->when($inicio !== null, fn ($q) => $q->where('data_vencimento', '>=', $inicio))
            ->when($fim !== null, fn ($q) => $q->where('data_vencimento', '<=', $fim))
            ->when($origem !== null, fn ($q) => $q->where('id_referencia_externa', 'like', "%{$origem}%"))
            ->orderByDesc('id_lancamento')
            ->get();

        $counts = [];
        foreach (StatusLancamento::cases() as $caso) {
            $counts[$caso->value] = 0;
        }
        $entradas = 0;
        $saidas = 0;
        $pendente = 0;
        foreach ($filtrados as $lancamento) {
            $counts[$lancamento->status->value]++;
            $centavos = (int) round((float) $lancamento->valor * 100);
            if ($lancamento->status !== StatusLancamento::CANCELADO) {
                if ($lancamento->tipo === TipoLancamento::ENTRADA) {
                    $entradas += $centavos;
                } else {
                    $saidas += $centavos;
                }
            }
            if ($lancamento->status === StatusLancamento::PENDENTE
                || $lancamento->status === StatusLancamento::ATRASADO) {
                $pendente += $centavos;
            }
        }

        $fmt = fn (int $c) => number_format($c / 100, 2, '.', '');

        return [
            'lancamentos' => $filtrados->all(),
            'counts' => $counts,
            'resumo' => [
                'entradas' => $fmt($entradas),
                'saidas' => $fmt($saidas),
                'pendente' => $fmt($pendente),
                'saldo' => $fmt($entradas - $saidas),
            ],
        ];
    }

    public function registrarPagamento(int $id, array $dados, FinanceiroPrincipal $principal): LancamentoFinanceiro
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return DB::transaction(function () use ($id, $dados, $principal) {
            $lancamento = LancamentoFinanceiro::find($id)
                ?? throw new ResourceNotFoundException("Lançamento não encontrado: {$id}");

            if ($lancamento->status === StatusLancamento::PAGO
                || $lancamento->status === StatusLancamento::CANCELADO) {
                throw new ConflitoException('Lançamento já liquidado ou cancelado não pode ser pago');
            }

            $lancamento->status = StatusLancamento::PAGO;
            $lancamento->data_pagamento = $dados['dataPagamento'] ?? today()->toDateString();
            if (($dados['observacao'] ?? null) !== null) {
                $lancamento->observacao = $dados['observacao'];
            }
            $lancamento->save();

            // Ex-`ManualPaymentProcessor`: auditoria preservada, abstração descartada.
            Log::info("Pagamento manual registrado: lançamento {$id} (usuário {$principal->idPessoa})");

            return $lancamento->refresh();
        });
    }

    /**
     * Varredura idempotente de vencidos: PENDENTE→ATRASADO + 1 evento por
     * vencido (PENDENTE ou ATRASADO), sem liquidar. Retorna a contagem.
     */
    public function emitirVencidos(): int
    {
        $vencidos = LancamentoFinanceiro::query()
            ->whereIn('status', [StatusLancamento::PENDENTE, StatusLancamento::ATRASADO])
            ->where('data_vencimento', '<', today()->toDateString())
            ->orderBy('id_lancamento')
            ->get();

        foreach ($vencidos as $lancamento) {
            if ($lancamento->status === StatusLancamento::PENDENTE) {
                $lancamento->status = StatusLancamento::ATRASADO;
                $lancamento->save();
            }
            $this->publisher->lancamentoVencido(
                (int) $lancamento->getKey(),
                (string) $lancamento->valor,
                substr((string) $lancamento->data_vencimento, 0, 10),
                $lancamento->id_referencia_externa,
            );
        }

        return $vencidos->count();
    }
}

<?php

namespace App\Modules\Financeiro\Services;

use App\Modules\Financeiro\Enums\StatusLancamento;
use App\Modules\Financeiro\Enums\TipoDoacao;
use App\Modules\Financeiro\Enums\TipoLancamento;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Models\CustoEncomenda;
use App\Modules\Financeiro\Models\DoacaoRecurso;
use App\Modules\Financeiro\Models\LancamentoFinanceiro;
use App\Modules\Financeiro\Policies\FinanceiroPolicy;
use Carbon\CarbonImmutable;

/**
 * Relatórios de saúde financeira (D-2: shapes servidos; front nunca deriva).
 * Período via `dataInicio`/`dataFim` (precedência) ou `periodo` (`yyyy-MM`,
 * allowlist no Request). Cancelados fora das somas. Aritmética em centavos.
 */
class RelatorioService
{
    /** @return array{inicio:?string,fim:?string} */
    public function resolverPeriodo(?string $inicio, ?string $fim, ?string $periodo): array
    {
        if ($inicio !== null || $fim !== null) {
            return ['inicio' => $inicio, 'fim' => $fim];
        }
        if ($periodo !== null && trim($periodo) !== '') {
            $mes = CarbonImmutable::createFromFormat('Y-m', trim($periodo))->startOfDay();

            return [
                'inicio' => $mes->startOfMonth()->toDateString(),
                'fim' => $mes->endOfMonth()->toDateString(),
            ];
        }

        return ['inicio' => null, 'fim' => null];
    }

    /** @return array<string,string|list<array<string,string>>> */
    public function fluxoCaixa(?string $inicio, ?string $fim, ?string $periodo, FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);
        $per = $this->resolverPeriodo($inicio, $fim, $periodo);
        $lista = $this->lancamentosNoPeriodo($per['inicio'], $per['fim']);

        $entradas = $this->somar($lista, TipoLancamento::ENTRADA);
        $saidas = $this->somar($lista, TipoLancamento::SAIDA);

        $porSemana = [];
        foreach ($lista as $lancamento) {
            $semana = CarbonImmutable::parse(substr((string) $lancamento->data_vencimento, 0, 10))
                ->format('o-\SW');
            $porSemana[$semana][] = $lancamento;
        }
        ksort($porSemana);
        $semanas = [];
        foreach ($porSemana as $semana => $itens) {
            $en = $this->somar($itens, TipoLancamento::ENTRADA);
            $sa = $this->somar($itens, TipoLancamento::SAIDA);
            $semanas[] = [
                'semana' => $semana,
                'entradas' => $this->fmt($en),
                'saidas' => $this->fmt($sa),
                'liquido' => $this->fmt($en - $sa),
            ];
        }

        return [
            'entradas' => $this->fmt($entradas),
            'saidas' => $this->fmt($saidas),
            'saldo' => $this->fmt($entradas - $saidas),
            'semanas' => $semanas,
        ];
    }

    /** @return array<string,string> */
    public function dre(?string $inicio, ?string $fim, ?string $periodo, FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);
        $per = $this->resolverPeriodo($inicio, $fim, $periodo);
        $lista = $this->lancamentosNoPeriodo($per['inicio'], $per['fim']);

        $entradas = $this->somar($lista, TipoLancamento::ENTRADA);
        $despesas = $this->somar($lista, TipoLancamento::SAIDA);
        $doacoes = $this->somarDoacoes($per['inicio'], $per['fim']);

        return [
            'receitaOperacional' => $this->fmt($entradas),
            'custosDiretos' => $this->fmt(0),
            'maoDeObra' => $this->fmt(0),
            'overhead' => $this->fmt(0),
            'despesasOperacionais' => $this->fmt($despesas),
            'doacoesRecursos' => $this->fmt($doacoes),
            'resultado' => $this->fmt($entradas + $doacoes - $despesas),
        ];
    }

    /** @return list<array<string,mixed>> último custo por encomenda, com margem % */
    public function lucratividade(FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);

        $ultimos = [];
        foreach (CustoEncomenda::orderBy('id_custo')->get() as $custo) {
            $id = (int) $custo->id_encomenda;
            $atual = $ultimos[$id] ?? null;
            if ($atual === null || substr((string) $custo->data_calculo, 0, 10) > substr((string) $atual->data_calculo, 0, 10)) {
                $ultimos[$id] = $custo;
            }
        }
        ksort($ultimos);

        $itens = [];
        foreach ($ultimos as $id => $custo) {
            $venda = (int) round((float) $custo->valor_venda * 100);
            $margem = (int) round((float) $custo->margem_lucro * 100);
            $itens[] = [
                'idEncomenda' => $id,
                'valorVenda' => $this->fmt($venda),
                'custoTotal' => (string) $custo->custo_total,
                'margem' => $this->fmt($margem),
                'margemPercentual' => $venda === 0 ? '0.00'
                    : number_format($margem / $venda * 100, 2, '.', ''),
            ];
        }

        return $itens;
    }

    /** @return list<array<string,mixed>> só ENTRADA ATRASADO, com dias em atraso */
    public function inadimplencia(FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);
        $hoje = today()->toDateString();

        $itens = [];
        foreach (LancamentoFinanceiro::query()
            ->where('status', StatusLancamento::ATRASADO)
            ->where('tipo', TipoLancamento::ENTRADA)
            ->where('data_vencimento', '<=', $hoje)
            ->orderBy('id_lancamento')->get() as $lancamento) {
            $venc = substr((string) $lancamento->data_vencimento, 0, 10);
            $itens[] = [
                'idLancamento' => (int) $lancamento->getKey(),
                'referencia' => $lancamento->id_referencia_externa,
                'valor' => (string) $lancamento->valor,
                'dataVencimento' => $venc,
                'diasEmAtraso' => (int) CarbonImmutable::parse($venc)->diffInDays(CarbonImmutable::parse($hoje)),
            ];
        }

        return $itens;
    }

    /** @return array<string,mixed> */
    public function doacoesDespesas(?string $inicio, ?string $fim, ?string $periodo, FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);
        $per = $this->resolverPeriodo($inicio, $fim, $periodo);

        $doacoes = DoacaoRecurso::query()
            ->when($per['inicio'] !== null, fn ($q) => $q->where('data_recebimento', '>=', $per['inicio']))
            ->when($per['fim'] !== null, fn ($q) => $q->where('data_recebimento', '<=', $per['fim']))
            ->orderBy('id_doacao')->get();

        $totalDoacoes = 0;
        $recursos = 0;
        $porMes = [];
        foreach ($doacoes as $doacao) {
            $centavos = (int) round((float) $doacao->valor * 100);
            if ($doacao->tipo === TipoDoacao::DOACAO) {
                $totalDoacoes += $centavos;
            } else {
                $recursos += $centavos;
            }
            $mes = substr((string) $doacao->data_recebimento, 0, 7);
            $porMes[$mes] = ($porMes[$mes] ?? 0) + $centavos;
        }
        ksort($porMes);

        $despesasPorMes = [];
        $despesas = 0;
        foreach ($this->lancamentosNoPeriodo($per['inicio'], $per['fim']) as $lancamento) {
            if ($lancamento->tipo !== TipoLancamento::SAIDA || $lancamento->status === StatusLancamento::CANCELADO) {
                continue;
            }
            $centavos = (int) round((float) $lancamento->valor * 100);
            $despesas += $centavos;
            $mes = substr((string) $lancamento->data_vencimento, 0, 7);
            $despesasPorMes[$mes] = ($despesasPorMes[$mes] ?? 0) + $centavos;
        }

        $meses = [];
        foreach ($porMes as $mes => $doa) {
            $des = $despesasPorMes[$mes] ?? 0;
            $meses[] = [
                'mes' => $mes,
                'doacoes' => $this->fmt($doa),
                'despesas' => $this->fmt($des),
                'saldo' => $this->fmt($doa - $des),
            ];
        }

        $arrecadado = $totalDoacoes + $recursos;

        return [
            'doacoes' => $this->fmt($totalDoacoes),
            'recursosProjeto' => $this->fmt($recursos),
            'despesas' => $this->fmt($despesas),
            'saldo' => $this->fmt($arrecadado - $despesas),
            'coberturaPercentual' => $despesas === 0 ? '0.00'
                : number_format($arrecadado / $despesas * 100, 2, '.', ''),
            'meses' => $meses,
        ];
    }

    /** @return list<array<string,string>> custo agregado por máquina (referência externa) */
    public function custoMaquina(FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);

        $porMaquina = [];
        foreach (LancamentoFinanceiro::query()
            ->where('tipo', TipoLancamento::SAIDA)
            ->where('status', '!=', StatusLancamento::CANCELADO->value)
            ->whereNotNull('id_referencia_externa')
            ->orderBy('id_lancamento')->get() as $lancamento) {
            $ref = (string) $lancamento->id_referencia_externa;
            $porMaquina[$ref] = ($porMaquina[$ref] ?? 0) + (int) round((float) $lancamento->valor * 100);
        }
        arsort($porMaquina);
        $total = array_sum($porMaquina);

        $itens = [];
        foreach ($porMaquina as $ref => $centavos) {
            $itens[] = [
                'idMaquina' => $ref,
                'custo' => $this->fmt($centavos),
                'percentualTotal' => $total === 0 ? '0.00'
                    : number_format($centavos / $total * 100, 2, '.', ''),
            ];
        }

        return $itens;
    }

    /** @return list<LancamentoFinanceiro> */
    private function lancamentosNoPeriodo(?string $inicio, ?string $fim): array
    {
        return LancamentoFinanceiro::query()
            ->when($inicio !== null, fn ($q) => $q->where('data_vencimento', '>=', $inicio))
            ->when($fim !== null, fn ($q) => $q->where('data_vencimento', '<=', $fim))
            ->orderBy('id_lancamento')->get()->all();
    }

    /** @param list<LancamentoFinanceiro> $lista */
    private function somar(array $lista, TipoLancamento $tipo): int
    {
        $soma = 0;
        foreach ($lista as $lancamento) {
            if ($lancamento->tipo === $tipo && $lancamento->status !== StatusLancamento::CANCELADO) {
                $soma += (int) round((float) $lancamento->valor * 100);
            }
        }

        return $soma;
    }

    private function somarDoacoes(?string $inicio, ?string $fim): int
    {
        $soma = 0;
        foreach (DoacaoRecurso::query()
            ->when($inicio !== null, fn ($q) => $q->where('data_recebimento', '>=', $inicio))
            ->when($fim !== null, fn ($q) => $q->where('data_recebimento', '<=', $fim))->get() as $doacao) {
            $soma += (int) round((float) $doacao->valor * 100);
        }

        return $soma;
    }

    private function fmt(int $centavos): string
    {
        return number_format($centavos / 100, 2, '.', '');
    }
}

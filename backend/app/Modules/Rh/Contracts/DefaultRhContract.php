<?php

namespace App\Modules\Rh\Contracts;

use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\Pessoa;
use App\Modules\Rh\Models\RegistroPontoDiario;

class DefaultRhContract implements RhContract
{
    public function funcionarioExiste(int $idFuncionario): bool
    {
        return Funcionario::whereKey($idFuncionario)->exists();
    }

    public function horasValidadasNoPeriodo(int $idFuncionario, string $tipo, string $inicio, string $fim): string
    {
        $total = ApontamentoHoras::where('id_funcionario', $idFuncionario)
            ->where('tipo', TipoApontamento::from($tipo))
            ->where('status', StatusApontamento::VALIDADO)
            ->whereBetween('data', [$inicio, $fim])
            ->sum('horas_trabalhadas');

        return number_format((float) $total, 2, '.', '');
    }

    public function incoerencias(int $idFuncionario): array
    {
        $presenca = RegistroPontoDiario::where('id_funcionario', $idFuncionario)
            ->whereNotNull('total_horas')
            ->pluck('total_horas', 'data');

        $apontado = ApontamentoHoras::where('id_funcionario', $idFuncionario)
            ->whereIn('status', [StatusApontamento::PENDENTE, StatusApontamento::VALIDADO])
            ->selectRaw('data, SUM(horas_trabalhadas) as total')
            ->groupBy('data')
            ->pluck('total', 'data');

        $saida = [];
        foreach ($apontado as $data => $horas) {
            $dia = substr((string) $data, 0, 10);
            $pres = (float) ($presenca[$data] ?? $presenca[$dia] ?? 0);
            if ($pres < (float) $horas) {
                $saida[] = [
                    'data' => $dia,
                    'horasPresenca' => number_format($pres, 2, '.', ''),
                    'horasApontadas' => number_format((float) $horas, 2, '.', ''),
                ];
            }
        }

        return $saida;
    }

    public function nomePessoa(int $idPessoa): ?string
    {
        $nome = Pessoa::whereKey($idPessoa)->value('nome_completo');

        return $nome === null ? null : (string) $nome;
    }
}

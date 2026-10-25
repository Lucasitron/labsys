<?php

namespace App\Modules\Rh\Contracts;

use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\CertificadoEmitido;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\Pessoa;
use App\Modules\Rh\Models\RegistroPontoDiario;
use App\Modules\Rh\Models\SolicitacaoCertificado;

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

    public function dadosDestinatario(int $idFuncionario): ?array
    {
        $funcionario = Funcionario::whereKey($idFuncionario)->first()
            ?? Funcionario::where('id_pessoa', $idFuncionario)->first();

        if ($funcionario === null) {
            return null;
        }

        $pessoa = Pessoa::whereKey($funcionario->id_pessoa)->first();

        if ($pessoa === null) {
            return null;
        }

        $contato = is_string($pessoa->contato) ? trim($pessoa->contato) : null;

        return [
            'idUsuario' => (int) $pessoa->getKey(),
            'nome' => (string) $pessoa->nome_completo,
            'email' => $contato !== null && filter_var($contato, FILTER_VALIDATE_EMAIL) !== false ? $contato : null,
        ];
    }

    public function solicitacoesCertificado(?string $status = null, ?int $idUsuario = null): array
    {
        return SolicitacaoCertificado::query()
            ->with('funcionario.pessoa')
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->when($idUsuario !== null,
                fn ($q) => $q->whereHas('funcionario', fn ($f) => $f->where('id_pessoa', $idUsuario)))
            ->orderByDesc('id')
            ->get()
            ->map(fn (SolicitacaoCertificado $s) => [
                'idSolicitacao' => (int) $s->getKey(),
                'nomeFuncionario' => (string) $s->funcionario->pessoa->nome_completo,
                'tipoCertificado' => $s->tipo_certificado->value,
                'horasSolicitadas' => number_format((float) $s->horas_solicitadas, 2, '.', ''),
                'dataSolicitacao' => $s->data_solicitacao->toIso8601String(),
                'status' => $s->status->value,
            ])
            ->all();
    }

    public function certificadosEmitidos(): array
    {
        return CertificadoEmitido::query()
            ->with('funcionario.pessoa')
            ->orderByDesc('id')
            ->get()
            ->map(fn (CertificadoEmitido $c) => [
                'idCertificado' => (int) $c->getKey(),
                'nomeFuncionario' => (string) $c->funcionario->pessoa->nome_completo,
                'dataEmissao' => $c->data_emissao->toIso8601String(),
            ])
            ->all();
    }
}

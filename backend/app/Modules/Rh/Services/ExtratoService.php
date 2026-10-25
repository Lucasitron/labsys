<?php

namespace App\Modules\Rh\Services;

use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Events\ExtratoMensalHorasEvent;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\RegistroPontoDiario;
use App\Modules\Rh\Policies\RhPolicy;
use App\Modules\Rh\RhPrincipal;
use App\Shared\Exceptions\ForbiddenException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Carbon\Carbon;

/**
 * Extrato mensal (F9): mês fechado, 1 evento por ativo exceto nível 4.
 * Agendado em routes/console.php (`0 6 1 * *`); leitura via GET.
 */
class ExtratoService
{
    /** Gera o extrato do mês anterior (job) ou do mês informado (manual/teste). */
    public function gerarExtratoMensal(?string $mesReferencia = null): int
    {
        $mes = $mesReferencia !== null
            ? Carbon::createFromFormat('Y-m', $mesReferencia)->startOfMonth()
            : today()->startOfMonth()->subMonthNoOverflow();

        $inicio = $mes->copy()->startOfMonth()->toDateString();
        $fim = $mes->copy()->endOfMonth()->toDateString();
        $referencia = $mes->format('m/Y');

        $ativos = Funcionario::with('pessoa')
            ->where('nivel_acesso', '!=', NivelAcesso::RECRUTANDO)
            ->get()
            ->filter(fn (Funcionario $f) => $f->pessoa !== null && $f->pessoa->status === PessoaStatus::ATIVO);

        foreach ($ativos as $funcionario) {
            $id = (int) $funcionario->getKey();
            event(new ExtratoMensalHorasEvent(
                $id,
                $funcionario->pessoa->nome_completo,
                $referencia,
                $this->somaPresenca($id, $inicio, $fim),
                $this->somaValidadasPorTipo($id, TipoApontamento::ENCOMENDA, $inicio, $fim),
                $this->somaValidadasPorTipo($id, TipoApontamento::PROJETO, $inicio, $fim),
                $this->somaDisponiveis($id),
            ));
        }

        return $ativos->count();
    }

    /** Leitura do extrato de um funcionário/mês (GET). Admin vê todos; demais, o próprio. */
    public function leitura(int $idFuncionario, string $mesReferencia, RhPrincipal $principal): array
    {
        $funcionario = Funcionario::with('pessoa')->find($idFuncionario);

        if ($funcionario === null) {
            throw new ResourceNotFoundException("Funcionário não encontrado: {$idFuncionario}");
        }

        if (! RhPolicy::ehAdminOuProprio($principal, $funcionario)) {
            throw new ForbiddenException('Acesso apenas aos seus próprios dados');
        }

        $mes = Carbon::createFromFormat('Y-m', $mesReferencia)->startOfMonth();
        $inicio = $mes->copy()->startOfMonth()->toDateString();
        $fim = $mes->copy()->endOfMonth()->toDateString();

        return [
            'idFuncionario' => $idFuncionario,
            'nome' => $funcionario->pessoa->nome_completo,
            'mesReferencia' => $mes->format('m/Y'),
            'horasPresenca' => $this->somaPresenca($idFuncionario, $inicio, $fim),
            'horasEncomenda' => $this->somaValidadasPorTipo($idFuncionario, TipoApontamento::ENCOMENDA, $inicio, $fim),
            'horasProjeto' => $this->somaValidadasPorTipo($idFuncionario, TipoApontamento::PROJETO, $inicio, $fim),
            'horasDisponiveis' => $this->somaDisponiveis($idFuncionario),
        ];
    }

    private function somaPresenca(int $id, string $inicio, string $fim): string
    {
        $total = RegistroPontoDiario::where('id_funcionario', $id)
            ->whereBetween('data', [$inicio, $fim])
            ->sum('total_horas');

        return number_format((float) $total, 2, '.', '');
    }

    private function somaValidadasPorTipo(int $id, TipoApontamento $tipo, string $inicio, string $fim): string
    {
        $total = ApontamentoHoras::where('id_funcionario', $id)
            ->where('tipo', $tipo)
            ->where('status', StatusApontamento::VALIDADO)
            ->whereBetween('data', [$inicio, $fim])
            ->sum('horas_trabalhadas');

        return number_format((float) $total, 2, '.', '');
    }

    private function somaDisponiveis(int $id): string
    {
        $total = ApontamentoHoras::where('id_funcionario', $id)
            ->where('status', StatusApontamento::VALIDADO)
            ->where('consolidado', false)
            ->sum('horas_trabalhadas');

        return number_format((float) $total, 2, '.', '');
    }
}

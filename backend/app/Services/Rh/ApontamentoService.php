<?php

namespace App\Services\Rh;

use App\Enums\Rh\StatusApontamento;
use App\Enums\Rh\TipoApontamento;
use App\Events\HorasValidadasEvent;
use App\Exceptions\ForbiddenException;
use App\Exceptions\ResourceNotFoundException;
use App\Models\Rh\ApontamentoHoras;
use App\Models\Rh\RegistroPontoDiario;
use App\Policies\RhPolicy;
use Carbon\Carbon;

/**
 * Apontamentos (F4): nasce PENDENTE; horário recalcula horas; validar publica
 * evento. A fachada `HorasController` usa este mesmo service (sem classe extra).
 */
class ApontamentoService
{
    public function __construct(private FuncionarioService $funcionarios) {}

    public function registrar(array $dados, RhPrincipal $principal): ApontamentoHoras
    {
        $funcionario = $this->funcionarios->obter((int) $dados['idFuncionario']);

        if (! $principal->isAdmin() && $principal->idFuncionario !== (int) $funcionario->getKey()) {
            throw new ForbiddenException('Apenas o próprio funcionário pode registrar suas horas');
        }

        $dados['horas_trabalhadas'] = (string) $this->resolverHoras($dados);
        $dados['id_funcionario'] = (int) $funcionario->getKey();
        $dados['status'] = StatusApontamento::PENDENTE;
        $dados['consolidado'] = false;

        return ApontamentoHoras::create($this->somenteColunas($dados));
    }

    /** Admin vê todos; demais, só os próprios. Exige vínculo (ou Admin geral). */
    public function listar(?StatusApontamento $status, ?string $periodo, RhPrincipal $principal): array
    {
        $eu = RhPolicy::exigeVinculoOuAdmin($principal);
        [$inicio, $fim] = $this->intervaloPeriodo($periodo);

        return ApontamentoHoras::query()
            ->when(! $principal->isAdmin(), fn ($q) => $q->where('id_funcionario', $eu))
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->when($inicio !== null, fn ($q) => $q->whereBetween('data', [$inicio, $fim]))
            ->orderBy('data')
            ->orderBy('id')
            ->get()
            ->all();
    }

    /**
     * Valida (Admin ou tutor) ou rejeita (motivo obrigatório). Coerência
     * presença×apontado checada na validação.
     */
    public function validar(int $id, StatusApontamento $status, ?string $motivo, RhPrincipal $principal): ApontamentoHoras
    {
        $apontamento = $this->obter($id);

        if ($apontamento->status !== StatusApontamento::PENDENTE) {
            throw new \InvalidArgumentException("Apontamento já validado ou rejeitado: {$id}");
        }

        if ($status === StatusApontamento::PENDENTE) {
            throw new \InvalidArgumentException('Status deve ser VALIDADO ou REJEITADO');
        }

        RhPolicy::exigeVinculoOuAdmin($principal);

        if (! RhPolicy::podeValidarHoras($principal)) {
            throw new ForbiddenException('Apenas Admin ou tutor podem validar horas');
        }

        if ($status === StatusApontamento::REJEITADO && trim((string) $motivo) === '') {
            throw new \InvalidArgumentException('Motivo da rejeição é obrigatório');
        }

        $admin = $this->funcionarios->obter($principal->idFuncionario);

        if ($status === StatusApontamento::VALIDADO) {
            $this->exigeCoerencia($apontamento);
            event(new HorasValidadasEvent(
                (int) $apontamento->id_funcionario,
                $apontamento->tipo,
                (int) $apontamento->id_referencia,
                number_format((float) $apontamento->horas_trabalhadas, 2, '.', ''),
                substr((string) $apontamento->data, 0, 10),
            ));
        }

        $apontamento->status = $status;
        $apontamento->motivo_rejeicao = $status === StatusApontamento::REJEITADO ? trim((string) $motivo) : null;
        $apontamento->id_admin_validador = (int) $admin->getKey();
        $apontamento->data_validacao = now();
        $apontamento->save();

        return $apontamento->refresh();
    }

    public function obter(int $id): ApontamentoHoras
    {
        return ApontamentoHoras::find($id)
            ?? throw new ResourceNotFoundException("Apontamento não encontrado: {$id}");
    }

    /**
     * Com início+fim, recalcula no servidor (ignora o enviado); sem eles,
     * exige horasTrabalhadas > 0.
     */
    public function resolverHoras(array $dados): float
    {
        $inicio = $dados['horaInicio'] ?? null;
        $fim = $dados['horaFim'] ?? null;

        if ($inicio !== null && $fim !== null) {
            $minutos = Carbon::parse($inicio)->diffInMinutes(Carbon::parse($fim), false);

            if ($minutos <= 0) {
                throw new \InvalidArgumentException('Hora fim deve ser posterior à hora início');
            }

            return round($minutos / 60, 2);
        }

        if ($inicio !== null || $fim !== null) {
            throw new \InvalidArgumentException('Informe hora início e hora fim juntos');
        }

        if (! isset($dados['horasTrabalhadas']) || (float) $dados['horasTrabalhadas'] <= 0) {
            throw new \InvalidArgumentException('Informe horasTrabalhadas ou hora início e hora fim');
        }

        return (float) $dados['horasTrabalhadas'];
    }

    /** Encomenda+projeto do dia não podem exceder a presença (p/ Financeiro). */
    private function exigeCoerencia(ApontamentoHoras $apontamento): void
    {
        $data = substr((string) $apontamento->data, 0, 10);

        $presenca = (float) (RegistroPontoDiario::where('id_funcionario', $apontamento->id_funcionario)
            ->where('data', $data)
            ->value('total_horas') ?? 0);

        $apontado = (float) ApontamentoHoras::where('id_funcionario', $apontamento->id_funcionario)
            ->where('data', $data)
            ->whereIn('status', [StatusApontamento::PENDENTE, StatusApontamento::VALIDADO])
            ->sum('horas_trabalhadas');

        if ($presenca <= 0) {
            throw new \InvalidArgumentException("Não há horas de presença registradas para validar horas em {$data}");
        }

        if ($apontado > $presenca) {
            throw new \InvalidArgumentException("Horas apontadas (encomenda + projeto) excedem as horas de presença do dia {$data}");
        }
    }

    /** @return array{0:?string,1:?string} */
    private function intervaloPeriodo(?string $periodo): array
    {
        if ($periodo === null || trim($periodo) === '') {
            return [null, null];
        }

        try {
            $base = Carbon::createFromFormat('!Y-m', trim($periodo));
        } catch (\Throwable $e) {
            throw new \InvalidArgumentException('Período deve estar no formato yyyy-MM');
        }

        return [$base->copy()->startOfMonth()->toDateString(), $base->copy()->endOfMonth()->toDateString()];
    }

    private function somenteColunas(array $dados): array
    {
        return [
            'id_funcionario' => $dados['id_funcionario'],
            'tipo' => $dados['tipo'] instanceof TipoApontamento ? $dados['tipo'] : TipoApontamento::from($dados['tipo']),
            'id_referencia' => $dados['idReferencia'],
            'data' => $dados['data'],
            'horas_trabalhadas' => $dados['horas_trabalhadas'],
            'hora_inicio' => $dados['horaInicio'] ?? null,
            'hora_fim' => $dados['horaFim'] ?? null,
            'descricao_atividade' => $dados['descricaoAtividade'] ?? null,
            'status' => $dados['status'],
            'consolidado' => $dados['consolidado'],
        ];
    }
}

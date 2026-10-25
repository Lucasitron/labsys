<?php

namespace App\Modules\Rh\Services;

use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\StatusSolicitacao;
use App\Modules\Rh\Events\CertificadoAprovadoEvent;
use App\Modules\Rh\Events\CertificadoRejeitadoEvent;
use App\Modules\Rh\Events\CertificadoSolicitadoEvent;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\CertificadoEmitido;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\HoraConsolidada;
use App\Modules\Rh\Models\SolicitacaoCertificado;
use App\Modules\Rh\Policies\RhPolicy;
use App\Modules\Rh\RhPrincipal;
use App\Shared\Exceptions\ForbiddenException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Carbon\CarbonImmutable;
use Illuminate\Support\Str;

/**
 * Certificados (F8): solicitar (próprio) → aprovar (Admin: emite UUID +
 * consolida TODAS as disponíveis, sem parcial no MVP) / rejeitar (mantém).
 */
class CertificadoService
{
    public function solicitar(array $dados, RhPrincipal $principal): SolicitacaoCertificado
    {
        $eu = RhPolicy::exigeVinculoOuAdmin($principal);

        if ($eu === 0) {
            throw new ForbiddenException('Operação restrita a usuários vinculados a um funcionário');
        }

        $disponiveis = $this->horasDisponiveisPara($eu);

        if ((float) $dados['horasSolicitadas'] > (float) $disponiveis) {
            throw new \InvalidArgumentException("Horas solicitadas excedem as horas disponíveis ({$disponiveis}h)");
        }

        $solicitacao = SolicitacaoCertificado::create([
            'id_funcionario' => $eu,
            'tipo_certificado' => $dados['tipoCertificado'],
            'data_solicitacao' => now(),
            'horas_solicitadas' => number_format((float) $dados['horasSolicitadas'], 2, '.', ''),
            'status' => StatusSolicitacao::PENDENTE,
        ]);

        event(new CertificadoSolicitadoEvent(
            (int) $solicitacao->getKey(), $eu,
            $solicitacao->tipo_certificado,
            number_format((float) $solicitacao->horas_solicitadas, 2, '.', ''),
            CarbonImmutable::instance($solicitacao->data_solicitacao),
        ));

        return $solicitacao;
    }

    /** Admin vê todas; funcionário, só as suas. */
    public function listarSolicitacoes(?StatusSolicitacao $status, RhPrincipal $principal): array
    {
        $eu = $principal->isAdmin() ? null : RhPolicy::exigeVinculoOuAdmin($principal);

        return SolicitacaoCertificado::with('funcionario.pessoa')
            ->when($eu !== null, fn ($q) => $q->where('id_funcionario', $eu))
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->orderByDesc('data_solicitacao')
            ->get()
            ->all();
    }

    /** Aprova (só Admin): emite com UUID, consolida tudo, publica evento. */
    public function aprovar(int $id, ?string $observacao, RhPrincipal $principal): CertificadoEmitido
    {
        $this->exigeAdmin($principal);
        $solicitacao = $this->obterPendenteComLock($id);
        $admin = $this->funcionarioDoPrincipal($principal);

        $disponiveis = $this->horasDisponiveisPara((int) $solicitacao->id_funcionario);

        if ((float) $solicitacao->horas_solicitadas > (float) $disponiveis) {
            throw new \InvalidArgumentException("Horas disponíveis insuficientes para aprovar a solicitação {$id} (disponíveis: {$disponiveis}h)");
        }

        $solicitacao->status = StatusSolicitacao::APROVADO;
        $solicitacao->id_admin_aprovador = (int) $admin->getKey();
        $solicitacao->data_decisao = now();
        if ($observacao !== null) {
            $solicitacao->observacao = $observacao;
        }
        $solicitacao->save();

        $agora = now();
        $certificado = CertificadoEmitido::create([
            'id_solicitacao' => (int) $solicitacao->getKey(),
            'id_funcionario' => (int) $solicitacao->id_funcionario,
            'tipo_certificado' => $solicitacao->tipo_certificado,
            'horas_certificadas' => number_format((float) $solicitacao->horas_solicitadas, 2, '.', ''),
            'data_emissao' => $agora,
            'codigo_verificacao' => (string) Str::uuid(),
        ]);

        $disponiveisLista = ApontamentoHoras::where('id_funcionario', $solicitacao->id_funcionario)
            ->where('status', StatusApontamento::VALIDADO)
            ->where('consolidado', false)
            ->orderBy('id')
            ->get();

        foreach ($disponiveisLista as $apontamento) {
            $apontamento->consolidado = true;
            $apontamento->save();

            HoraConsolidada::create([
                'id_certificado' => (int) $certificado->getKey(),
                'id_apontamento' => (int) $apontamento->getKey(),
                'horas' => number_format((float) $apontamento->horas_trabalhadas, 2, '.', ''),
                'data_consolidacao' => $agora,
            ]);
        }

        $certificado->load('funcionario.pessoa');

        event(new CertificadoAprovadoEvent(
            (int) $solicitacao->getKey(),
            (int) $certificado->getKey(),
            (int) $certificado->id_funcionario,
            $certificado->funcionario->pessoa->nome_completo,
            $certificado->tipo_certificado,
            number_format((float) $certificado->horas_certificadas, 2, '.', ''),
            CarbonImmutable::instance($certificado->data_emissao),
        ));

        return $certificado;
    }

    /** Rejeita (só Admin): mantém as disponíveis, publica evento. */
    public function rejeitar(int $id, ?string $observacao, RhPrincipal $principal): SolicitacaoCertificado
    {
        $this->exigeAdmin($principal);
        $solicitacao = $this->obterPendenteComLock($id);
        $admin = $this->funcionarioDoPrincipal($principal);

        $solicitacao->status = StatusSolicitacao::REJEITADO;
        $solicitacao->id_admin_aprovador = (int) $admin->getKey();
        $solicitacao->data_decisao = now();
        if ($observacao !== null) {
            $solicitacao->observacao = $observacao;
        }
        $solicitacao->save();
        $solicitacao->load('funcionario.pessoa');

        event(new CertificadoRejeitadoEvent(
            (int) $solicitacao->getKey(),
            (int) $solicitacao->id_funcionario,
            $solicitacao->funcionario->pessoa->nome_completo,
            $solicitacao->tipo_certificado,
            $solicitacao->observacao,
            CarbonImmutable::instance($solicitacao->data_decisao),
        ));

        return $solicitacao;
    }

    /** Admin vê todos; funcionário, só os seus. */
    public function listarEmitidos(RhPrincipal $principal): array
    {
        $eu = $principal->isAdmin() ? null : RhPolicy::exigeVinculoOuAdmin($principal);

        return CertificadoEmitido::with('funcionario.pessoa')
            ->when($eu !== null, fn ($q) => $q->where('id_funcionario', $eu))
            ->orderByDesc('data_emissao')
            ->get()
            ->all();
    }

    public function obterEmitido(int $id, RhPrincipal $principal): CertificadoEmitido
    {
        if (! $principal->isAdmin()) {
            RhPolicy::exigeVinculoOuAdmin($principal);
        }

        $certificado = CertificadoEmitido::with('funcionario.pessoa')->find($id)
            ?? throw new ResourceNotFoundException("Certificado não encontrado: {$id}");

        if (! $principal->isAdmin() && (int) $certificado->id_funcionario !== $principal->idFuncionario) {
            throw new ForbiddenException('Acesso apenas aos seus próprios certificados');
        }

        return $certificado;
    }

    /** Disponíveis do usuário = VALIDADOS ∧ ¬consolidado. */
    public function horasDisponiveis(RhPrincipal $principal): array
    {
        $eu = RhPolicy::exigeVinculoOuAdmin($principal);

        if ($eu === 0) {
            throw new ForbiddenException('Operação restrita a usuários vinculados a um funcionário');
        }

        return ['idFuncionario' => $eu, 'totalHorasDisponiveis' => $this->horasDisponiveisPara($eu)];
    }

    public function horasDisponiveisPara(int $idFuncionario): string
    {
        $total = ApontamentoHoras::where('id_funcionario', $idFuncionario)
            ->where('status', StatusApontamento::VALIDADO)
            ->where('consolidado', false)
            ->sum('horas_trabalhadas');

        return number_format((float) $total, 2, '.', '');
    }

    /** @return list<HoraConsolidada> */
    public function consolidadasDe(int $idCertificado): array
    {
        return HoraConsolidada::where('id_certificado', $idCertificado)->orderBy('id')->get()->all();
    }

    private function obterPendenteComLock(int $id): SolicitacaoCertificado
    {
        $solicitacao = SolicitacaoCertificado::whereKey($id)->lockForUpdate()->first()
            ?? throw new ResourceNotFoundException("Solicitação não encontrada: {$id}");

        if ($solicitacao->status !== StatusSolicitacao::PENDENTE) {
            throw new \InvalidArgumentException("Solicitação já decidida: {$id}");
        }

        return $solicitacao;
    }

    private function exigeAdmin(RhPrincipal $principal): void
    {
        if (! $principal->isAdmin()) {
            throw new ForbiddenException('Apenas Admin pode decidir certificados');
        }
    }

    private function funcionarioDoPrincipal(RhPrincipal $principal): Funcionario
    {
        if ($principal->idFuncionario === null) {
            throw new ForbiddenException('Operação restrita a usuários vinculados a um funcionário');
        }

        return Funcionario::find($principal->idFuncionario)
            ?? throw new ResourceNotFoundException("Funcionário não encontrado: {$principal->idFuncionario}");
    }
}

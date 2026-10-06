<?php

namespace App\Modules\Rh\Services;

use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Events\NivelAlteradoEvent;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\HistoricoNivel;
use App\Modules\Rh\Models\RegistroPontoDiario;
use App\Modules\Rh\Policies\RhPolicy;
use App\Modules\Rh\RhPrincipal;
use App\Shared\Exceptions\ForbiddenException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Carbon\CarbonImmutable;

/**
 * Funcionários (F2/F7): vínculo, listagem, nível (só Admin) + totais/coerência.
 */
class FuncionarioService
{
    public function vincular(array $dados): Funcionario
    {
        if (Funcionario::where('id_pessoa', $dados['id_pessoa'])->exists()) {
            throw new \InvalidArgumentException("Pessoa já vinculada a um funcionário: {$dados['id_pessoa']}");
        }

        // Garante que a pessoa existe (404 antes do FK estourar).
        app(PessoaService::class)->obter((int) $dados['id_pessoa']);

        $dados['nivel_acesso'] ??= NivelAcesso::BOLSISTA;

        return Funcionario::create($dados);
    }

    /** @return list<Funcionario> */
    public function listar(?NivelAcesso $nivel, ?string $departamento): array
    {
        return Funcionario::with('pessoa')
            ->when($nivel !== null, fn ($q) => $q->where('nivel_acesso', $nivel))
            ->when($departamento !== null && trim($departamento) !== '', fn ($q) => $q->where('departamento', 'ilike', trim((string) $departamento)))
            ->orderBy('id')
            ->get()
            ->all();
    }

    /**
     * Altera o nível (só Admin — rota `can:admin`): grava histórico + evento.
     * Exige vínculo do autor (id_admin_alterou); sem mudança, não grava nem emite.
     */
    public function alterarNivel(int $id, NivelAcesso $nivelNovo, RhPrincipal $principal): array
    {
        $funcionario = $this->obter($id);

        if ($principal->idFuncionario === null) {
            throw new ForbiddenException('Operação restrita a usuários vinculados a um funcionário');
        }

        $nivelAntigo = $funcionario->nivel_acesso;

        if ($nivelAntigo === $nivelNovo) {
            return [
                'idFuncionario' => $id, 'nivelAntigo' => $nivelAntigo->value,
                'nivelNovo' => $nivelNovo->value, 'dataAlteracao' => now()->toIso8601String(),
            ];
        }

        $admin = $this->obter($principal->idFuncionario);

        $funcionario->nivel_acesso = $nivelNovo;
        $funcionario->save();

        $data = CarbonImmutable::now();
        HistoricoNivel::create([
            'id_funcionario' => $id,
            'nivel_antigo' => $nivelAntigo,
            'nivel_novo' => $nivelNovo,
            'id_admin_alterou' => (int) $admin->getKey(),
            'data_alteracao' => $data,
        ]);

        event(new NivelAlteradoEvent($id, $nivelAntigo, $nivelNovo, $data));

        return [
            'idFuncionario' => $id, 'nivelAntigo' => $nivelAntigo->value,
            'nivelNovo' => $nivelNovo->value, 'dataAlteracao' => $data->toIso8601String(),
        ];
    }

    /** Totais presença×(encomenda+projeto) + incoerências do dia (p/ Financeiro). */
    public function totalHoras(int $id, RhPrincipal $principal): array
    {
        $funcionario = $this->obter($id);

        if (! RhPolicy::ehAdminOuProprio($principal, $funcionario)) {
            throw new ForbiddenException('Acesso apenas aos seus próprios dados');
        }

        $registros = RegistroPontoDiario::where('id_funcionario', $id)->get();

        $apontamentos = ApontamentoHoras::where('id_funcionario', $id)
            ->whereIn('status', [StatusApontamento::PENDENTE, StatusApontamento::VALIDADO])
            ->get();

        $totalPresenca = (string) $registros->sum(fn ($r) => (float) ($r->total_horas ?? 0));
        $totalEncomenda = (string) $apontamentos->where('tipo', TipoApontamento::ENCOMENDA)->sum(fn ($a) => (float) $a->horas_trabalhadas);
        $totalProjeto = (string) $apontamentos->where('tipo', TipoApontamento::PROJETO)->sum(fn ($a) => (float) $a->horas_trabalhadas);

        $porData = $apontamentos->groupBy(fn ($a) => substr((string) $a->data, 0, 10));
        $presencaPorData = $registros->keyBy(fn ($r) => substr((string) $r->data, 0, 10));

        $datas = $porData->keys()->merge($presencaPorData->keys())->unique()->sort()->values();

        $incoerencias = [];
        foreach ($datas as $data) {
            $pres = (float) ($presencaPorData->get($data)?->total_horas ?? 0);
            $apont = (float) $porData->get($data, collect())->sum(fn ($a) => (float) $a->horas_trabalhadas);
            if ($pres < $apont) {
                $incoerencias[] = [
                    'data' => $data,
                    'horasPresenca' => number_format($pres, 2, '.', ''),
                    'horasApontadas' => number_format($apont, 2, '.', ''),
                ];
            }
        }

        return [
            'idFuncionario' => $id,
            'totalHorasPresenca' => number_format((float) $totalPresenca, 2, '.', ''),
            'totalHorasEncomenda' => number_format((float) $totalEncomenda, 2, '.', ''),
            'totalHorasProjeto' => number_format((float) $totalProjeto, 2, '.', ''),
            'incoerencias' => $incoerencias,
        ];
    }

    public function obter(int $id): Funcionario
    {
        return Funcionario::find($id)
            ?? throw new ResourceNotFoundException("Funcionário não encontrado: {$id}");
    }
}

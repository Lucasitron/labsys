<?php

namespace App\Modules\Rh\Services;

use App\Modules\Rh\Models\AvaliacaoTreinamento;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\Treinamento;
use App\Modules\Rh\Models\Tutor;
use App\Modules\Rh\Policies\RhPolicy;
use App\Modules\Rh\RhPrincipal;
use App\Shared\Exceptions\ForbiddenException;
use App\Shared\Exceptions\ResourceNotFoundException;

/**
 * Treinamentos LMS (F6): criar por Admin (idTutor obrigatório) ou tutor
 * (vira o próprio); avaliar por Admin ou tutor.
 */
class TreinamentoService
{
    public function criar(array $dados, RhPrincipal $principal): Treinamento
    {
        $tutor = $this->resolverTutor($dados['idTutor'] ?? null, $principal);

        return Treinamento::create([
            'titulo' => $dados['titulo'],
            'descricao' => $dados['descricao'] ?? null,
            'url_conteudo' => $dados['urlConteudo'] ?? null,
            'id_tutor' => (int) $tutor->getKey(),
        ]);
    }

    public function avaliar(int $idTreinamento, array $dados, RhPrincipal $principal): AvaliacaoTreinamento
    {
        if (! $principal->isAdmin() && ! RhPolicy::ehTutor($principal)) {
            throw new ForbiddenException('Apenas Admin ou tutor podem avaliar treinamentos');
        }

        $treinamento = $this->obter($idTreinamento);
        $aluno = Funcionario::find($dados['idFuncionario'])
            ?? throw new ResourceNotFoundException("Funcionário não encontrado: {$dados['idFuncionario']}");

        return AvaliacaoTreinamento::create([
            'id_treinamento' => (int) $treinamento->getKey(),
            'id_funcionario' => (int) $aluno->getKey(),
            'nota' => number_format((float) $dados['nota'], 2, '.', ''),
            'feedback' => $dados['feedback'] ?? null,
            'data_avaliacao' => today()->toDateString(),
        ]);
    }

    /** @return list<AvaliacaoTreinamento> */
    public function listarAvaliacoes(int $idTreinamento): array
    {
        $this->obter($idTreinamento);

        return AvaliacaoTreinamento::where('id_treinamento', $idTreinamento)
            ->orderBy('id')
            ->get()
            ->all();
    }

    public function obter(int $id): Treinamento
    {
        return Treinamento::find($id)
            ?? throw new ResourceNotFoundException("Treinamento não encontrado: {$id}");
    }

    private function resolverTutor(?int $idTutor, RhPrincipal $principal): Funcionario
    {
        if ($principal->isAdmin()) {
            if ($idTutor === null) {
                throw new \InvalidArgumentException('idTutor é obrigatório quando criado por Admin');
            }

            return Funcionario::find($idTutor)
                ?? throw new ResourceNotFoundException("Tutor não encontrado: {$idTutor}");
        }

        if ($principal->idFuncionario === null || ! RhPolicy::ehTutor($principal)) {
            throw new ForbiddenException('Apenas Admin ou tutor podem criar treinamentos');
        }

        return Funcionario::find($principal->idFuncionario)
            ?? throw new ForbiddenException('Funcionário não encontrado');
    }
}

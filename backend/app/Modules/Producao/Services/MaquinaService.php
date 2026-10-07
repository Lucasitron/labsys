<?php

namespace App\Modules\Producao\Services;

use App\Modules\Producao\Enums\MaquinaStatus;
use App\Modules\Producao\Models\HistoricoUsoMaquina;
use App\Modules\Producao\Models\Maquina;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * Máquinas (F4): cadastro/status Admin; uso abre/fecha com `horas_uso`.
 * `iniciarUso` exige `DISPONIVEL` (409 senão — plan P12).
 */
class MaquinaService
{
    /** @return list<Maquina> */
    public function listar(?MaquinaStatus $status, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return Maquina::query()
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->orderBy('id_maquina')
            ->get()
            ->all();
    }

    public function detalhar(int $id, ProducaoPrincipal $principal): Maquina
    {
        ProducaoPolicy::exigeLeitura($principal);

        return $this->obter($id);
    }

    public function criar(array $dados, ProducaoPrincipal $principal): Maquina
    {
        ProducaoPolicy::exigeAdmin($principal);

        return DB::transaction(fn () => Maquina::create([
            'nome' => $dados['nome'],
            'descricao' => $dados['descricao'] ?? null,
            'status' => $dados['status'] ?? MaquinaStatus::DISPONIVEL->value,
            'localizacao' => $dados['localizacao'] ?? null,
        ]));
    }

    public function atualizar(int $id, array $dados, ProducaoPrincipal $principal): Maquina
    {
        ProducaoPolicy::exigeAdmin($principal);
        $maquina = $this->obter($id);

        return DB::transaction(function () use ($maquina, $dados) {
            $maquina->update([
                'nome' => $dados['nome'],
                'descricao' => $dados['descricao'] ?? null,
                'status' => $dados['status'] ?? MaquinaStatus::DISPONIVEL->value,
                'localizacao' => $dados['localizacao'] ?? null,
            ]);

            return $maquina->refresh();
        });
    }

    public function alterarStatus(int $id, MaquinaStatus $status, ProducaoPrincipal $principal): Maquina
    {
        ProducaoPolicy::exigeAdmin($principal);
        $maquina = $this->obter($id);

        return DB::transaction(function () use ($maquina, $status) {
            $maquina->status = $status;
            $maquina->save();

            return $maquina->refresh();
        });
    }

    public function remover(int $id, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeAdmin($principal);

        $this->obter($id)->delete();
    }

    public function iniciarUso(int $idMaquina, array $dados, ProducaoPrincipal $principal): HistoricoUsoMaquina
    {
        ProducaoPolicy::exigeUsoMaquina($principal);
        $maquina = $this->obter($idMaquina);

        return DB::transaction(function () use ($maquina, $dados) {
            if ($maquina->status !== MaquinaStatus::DISPONIVEL) {
                throw new ConflitoException("Máquina {$maquina->getKey()} indisponível para uso ({$maquina->status->value})");
            }

            $uso = HistoricoUsoMaquina::create([
                'id_maquina' => $maquina->getKey(),
                'id_funcionario' => $dados['idFuncionario'],
                'data_inicio' => $dados['dataInicio'] ?? now()->toDateTimeString(),
                'observacao' => $dados['observacao'] ?? null,
            ]);

            $maquina->status = MaquinaStatus::EM_USO;
            $maquina->save();

            return $uso->refresh();
        });
    }

    public function encerrarUso(int $idMaquina, int $idUso, array $dados, ProducaoPrincipal $principal): HistoricoUsoMaquina
    {
        ProducaoPolicy::exigeUsoMaquina($principal);
        $maquina = $this->obter($idMaquina);

        return DB::transaction(function () use ($maquina, $idMaquina, $idUso, $dados) {
            $uso = HistoricoUsoMaquina::find($idUso)
                ?? throw new ResourceNotFoundException("Registro de uso não encontrado: {$idUso}");

            if ((int) $uso->id_maquina !== $idMaquina) {
                throw ValidationException::withMessages([
                    'idUso' => ["O uso {$idUso} não pertence à máquina {$idMaquina}"],
                ]);
            }

            $fim = isset($dados['dataFim'])
                ? \Carbon\Carbon::parse($dados['dataFim'])
                : now();

            if ($fim->lt($uso->data_inicio)) {
                throw ValidationException::withMessages([
                    'dataFim' => ['A data de fim não pode ser anterior à data de início'],
                ]);
            }

            $uso->data_fim = $fim;
            $uso->horas_uso = number_format($uso->data_inicio->diffInMinutes($fim) / 60, 2, '.', '');
            if (array_key_exists('observacao', $dados)) {
                $uso->observacao = $dados['observacao'];
            }
            $uso->save();

            $maquina->status = MaquinaStatus::DISPONIVEL;
            $maquina->save();

            return $uso->refresh();
        });
    }

    /** @return list<HistoricoUsoMaquina> */
    public function historico(int $idMaquina, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);
        $this->obter($idMaquina);

        return HistoricoUsoMaquina::where('id_maquina', $idMaquina)
            ->orderByDesc('data_inicio')
            ->get()
            ->all();
    }

    public function obter(int $id): Maquina
    {
        return Maquina::find($id)
            ?? throw new ResourceNotFoundException("Máquina não encontrada: {$id}");
    }
}

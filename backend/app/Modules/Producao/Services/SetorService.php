<?php

namespace App\Modules\Producao\Services;

use App\Modules\Producao\Models\Setor;
use App\Modules\Producao\Models\SetorChecklist;
use App\Modules\Producao\Models\SetorMaterial;
use App\Modules\Producao\Models\SetorResponsavel;
use App\Modules\Producao\Models\SetorSinalizacao;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * Setores 5S (F5): CRUD+detalhe, materiais, sinalizações, checklist (edita só
 * `item`+`ativo`) e responsáveis com rotação (ativo novo desativa o anterior).
 * Sem checagem cruzada de Recrutando (ids opacos, precedente M4/M5).
 */
class SetorService
{
    /** @return list<Setor> */
    public function listar(?bool $ativo, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return Setor::query()->with(['materiais', 'sinalizacoes', 'checklists', 'responsaveis'])
            ->when($ativo === true, fn ($q) => $q->where('ativo', true))
            ->orderBy('id_setor')
            ->get()
            ->all();
    }

    public function detalhar(int $id, ProducaoPrincipal $principal): Setor
    {
        ProducaoPolicy::exigeLeitura($principal);

        return $this->obter($id)->load(['materiais', 'sinalizacoes', 'checklists', 'responsaveis']);
    }

    public function criar(array $dados, ProducaoPrincipal $principal): Setor
    {
        ProducaoPolicy::exigeCriarSetor($principal);

        return DB::transaction(fn () => $this->detalharNovo(Setor::create([
            'numero' => $dados['numero'],
            'nome' => $dados['nome'],
            'descricao' => $dados['descricao'] ?? null,
            'observacoes' => $dados['observacoes'] ?? null,
            'foto_correto_url' => $dados['fotoCorretoUrl'] ?? null,
            'foto_incorreto_url' => $dados['fotoIncorretoUrl'] ?? null,
            'ativo' => $dados['ativo'] ?? true,
        ])));
    }

    public function atualizar(int $id, array $dados, ProducaoPrincipal $principal): Setor
    {
        $setor = $this->obter($id);
        ProducaoPolicy::exigeEscritaSetor((int) $setor->getKey(), $principal);

        return DB::transaction(function () use ($setor, $dados) {
            $setor->update([
                'numero' => $dados['numero'],
                'nome' => $dados['nome'],
                'descricao' => $dados['descricao'] ?? null,
                'observacoes' => $dados['observacoes'] ?? null,
                'foto_correto_url' => $dados['fotoCorretoUrl'] ?? null,
                'foto_incorreto_url' => $dados['fotoIncorretoUrl'] ?? null,
                'ativo' => $dados['ativo'] ?? true,
            ]);

            return $this->detalharNovo($setor->refresh());
        });
    }

    public function remover(int $id, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeLeitura($principal);
        $setor = $this->obter($id);
        ProducaoPolicy::exigeEscritaSetor((int) $setor->getKey(), $principal);

        // Agregado 5S: remove os filhos antes do setor (equivale ao
        // `orphanRemoval`/cascade do Java; sem isso a FK 500).
        DB::transaction(function () use ($setor): void {
            $key = $setor->getKey();
            SetorMaterial::where('id_setor', $key)->delete();
            SetorSinalizacao::where('id_setor', $key)->delete();
            SetorChecklist::where('id_setor', $key)->delete();
            SetorResponsavel::where('id_setor', $key)->delete();
            $setor->delete();
        });
    }

    public function adicionarMaterial(int $idSetor, array $dados, ProducaoPrincipal $principal): SetorMaterial
    {
        $this->obter($idSetor);
        ProducaoPolicy::exigeEscritaSetor($idSetor, $principal);

        return SetorMaterial::create([
            'id_setor' => $idSetor,
            'descricao' => $dados['descricao'],
            'quantidade' => number_format((float) $dados['quantidade'], 2, '.', ''),
        ]);
    }

    public function removerMaterial(int $idSetor, int $idMaterial, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeEscritaSetor($idSetor, $principal);
        $material = SetorMaterial::find($idMaterial)
            ?? throw new ResourceNotFoundException("Material não encontrado: {$idMaterial}");
        $this->pertenceAoSetor((int) $material->id_setor, $idSetor, 'Material');

        $material->delete();
    }

    public function adicionarSinalizacao(int $idSetor, array $dados, ProducaoPrincipal $principal): SetorSinalizacao
    {
        $this->obter($idSetor);
        ProducaoPolicy::exigeEscritaSetor($idSetor, $principal);

        return SetorSinalizacao::create([
            'id_setor' => $idSetor,
            'texto' => $dados['texto'],
        ]);
    }

    public function removerSinalizacao(int $idSetor, int $idSinalizacao, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeEscritaSetor($idSetor, $principal);
        $sinalizacao = SetorSinalizacao::find($idSinalizacao)
            ?? throw new ResourceNotFoundException("Sinalização não encontrada: {$idSinalizacao}");
        $this->pertenceAoSetor((int) $sinalizacao->id_setor, $idSetor, 'Sinalização');

        $sinalizacao->delete();
    }

    public function adicionarChecklist(int $idSetor, array $dados, ProducaoPrincipal $principal): SetorChecklist
    {
        $this->obter($idSetor);
        ProducaoPolicy::exigeEscritaSetor($idSetor, $principal);

        return SetorChecklist::create([
            'id_setor' => $idSetor,
            'item' => $dados['item'],
            'ativo' => $dados['ativo'] ?? true,
        ]);
    }

    public function atualizarChecklist(int $idSetor, int $idItem, array $dados, ProducaoPrincipal $principal): SetorChecklist
    {
        ProducaoPolicy::exigeEscritaSetor($idSetor, $principal);
        $checklist = SetorChecklist::find($idItem)
            ?? throw new ResourceNotFoundException("Item de checklist não encontrado: {$idItem}");
        $this->pertenceAoSetor((int) $checklist->id_setor, $idSetor, 'Item de checklist');

        return DB::transaction(function () use ($checklist, $dados) {
            $checklist->item = $dados['item'];
            if (array_key_exists('ativo', $dados)) {
                $checklist->ativo = $dados['ativo'];
            }
            $checklist->save();

            return $checklist->refresh();
        });
    }

    public function removerChecklist(int $idSetor, int $idItem, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeEscritaSetor($idSetor, $principal);
        $checklist = SetorChecklist::find($idItem)
            ?? throw new ResourceNotFoundException("Item de checklist não encontrado: {$idItem}");
        $this->pertenceAoSetor((int) $checklist->id_setor, $idSetor, 'Item de checklist');

        $checklist->delete();
    }

    public function adicionarResponsavel(int $idSetor, array $dados, ProducaoPrincipal $principal): SetorResponsavel
    {
        ProducaoPolicy::exigeAdmin($principal);
        $this->obter($idSetor);

        return DB::transaction(function () use ($idSetor, $dados) {
            $ativo = $dados['ativo'] ?? true;

            if ($ativo) {
                SetorResponsavel::where('id_setor', $idSetor)->where('ativo', true)->update(['ativo' => false]);
            }

            return SetorResponsavel::create([
                'id_setor' => $idSetor,
                'id_funcionario' => $dados['idFuncionario'],
                'data_inicio' => $dados['dataInicio'],
                'data_fim' => $dados['dataFim'] ?? null,
                'ativo' => $ativo,
            ]);
        });
    }

    /** @return list<SetorResponsavel> */
    public function listarResponsaveis(int $idSetor, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);
        $this->obter($idSetor);

        return SetorResponsavel::where('id_setor', $idSetor)->orderBy('id_responsavel')->get()->all();
    }

    public function removerResponsavel(int $idSetor, int $idResponsavel, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeEscritaSetor($idSetor, $principal);
        $responsavel = SetorResponsavel::find($idResponsavel)
            ?? throw new ResourceNotFoundException("Responsável não encontrado: {$idResponsavel}");
        $this->pertenceAoSetor((int) $responsavel->id_setor, $idSetor, 'Responsável');

        $responsavel->delete();
    }

    public function obter(int $id): Setor
    {
        return Setor::find($id)
            ?? throw new ResourceNotFoundException("Setor não encontrado: {$id}");
    }

    private function detalharNovo(Setor $setor): Setor
    {
        return $setor->load(['materiais', 'sinalizacoes', 'checklists', 'responsaveis']);
    }

    private function pertenceAoSetor(int $dono, int $esperado, string $recurso): void
    {
        if ($dono !== $esperado) {
            throw ValidationException::withMessages([
                'id' => ["{$recurso} não pertence ao setor {$esperado}"],
            ]);
        }
    }

}

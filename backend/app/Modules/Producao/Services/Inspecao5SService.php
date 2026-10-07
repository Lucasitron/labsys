<?php

namespace App\Modules\Producao\Services;

use App\Modules\Producao\Enums\StatusInspecao;
use App\Modules\Producao\Models\Inspecao5S;
use App\Modules\Producao\Models\ItemInspecao5S;
use App\Modules\Producao\Models\SetorChecklist;
use App\Modules\Producao\Models\SetorResponsavel;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * Inspeções 5S (F6): itens só do checklist do setor (422 senão), status
 * recalculado no backend e auto-advertência VERBAL ao responsável ativo em
 * `NAO_CONFORME`.
 */
class Inspecao5SService
{
    public function __construct(
        private AdvertenciaService $advertencias,
        private SetorService $setores,
    ) {}

    /** @return list<Inspecao5S> */
    public function listar(?int $idSetor, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return Inspecao5S::query()->with(['setor', 'itens'])
            ->when($idSetor !== null, fn ($q) => $q->where('id_setor', $idSetor))
            ->orderByDesc('data_inspecao')
            ->get()
            ->all();
    }

    public function detalhar(int $id, ProducaoPrincipal $principal): Inspecao5S
    {
        ProducaoPolicy::exigeLeitura($principal);

        return $this->obter($id)->load(['setor', 'itens']);
    }

    public function registrar(array $dados, ProducaoPrincipal $principal): Inspecao5S
    {
        ProducaoPolicy::exigeResponsavel((int) $dados['idInspetor'], $principal);

        return DB::transaction(function () use ($dados) {
            $setor = $this->setores->obter((int) $dados['idSetor']);

            $inspecao = Inspecao5S::create([
                'id_setor' => $setor->getKey(),
                'id_inspetor' => $dados['idInspetor'],
                'data_inspecao' => $dados['dataInspecao'],
                'turno' => $dados['turno'],
                'status' => StatusInspecao::OK->value,
                'observacoes' => $dados['observacoes'] ?? null,
            ]);

            foreach ($dados['itens'] as $item) {
                $checklist = SetorChecklist::find($item['idChecklist'])
                    ?? throw new ResourceNotFoundException("Item de checklist não encontrado: {$item['idChecklist']}");

                if ((int) $checklist->id_setor !== (int) $setor->getKey()) {
                    throw ValidationException::withMessages([
                        'itens' => ["O item de checklist {$checklist->getKey()} não pertence ao setor {$setor->getKey()}"],
                    ]);
                }

                ItemInspecao5S::create([
                    'id_inspecao' => $inspecao->getKey(),
                    'id_checklist' => $checklist->getKey(),
                    'conforme' => $item['conforme'],
                    'observacao' => $item['observacao'] ?? null,
                ]);
            }

            $inspecao->load('itens');
            $inspecao->status = $inspecao->recalculado();
            $inspecao->save();

            if ($inspecao->status === StatusInspecao::NAO_CONFORME) {
                $responsavel = SetorResponsavel::where('id_setor', $setor->getKey())
                    ->where('ativo', true)
                    ->orderBy('id_responsavel')
                    ->first();

                if ($responsavel !== null) {
                    $this->advertencias->registrarAutomatica(
                        (int) $responsavel->id_funcionario,
                        $inspecao,
                        "Não conformidade 5S no setor {$setor->nome} em {$inspecao->data_inspecao->toDateString()}",
                    );
                }
            }

            return $inspecao->load(['setor', 'itens']);
        });
    }

    public function remover(int $id, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeAdmin($principal);

        DB::transaction(function () use ($id): void {
            $inspecao = $this->obter($id);
            ItemInspecao5S::where('id_inspecao', $inspecao->getKey())->delete();
            $inspecao->delete();
        });
    }

    public function obter(int $id): Inspecao5S
    {
        return Inspecao5S::find($id)
            ?? throw new ResourceNotFoundException("Inspeção 5S não encontrada: {$id}");
    }
}

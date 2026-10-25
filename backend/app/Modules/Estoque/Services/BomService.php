<?php

namespace App\Modules\Estoque\Services;

use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Models\Item;
use App\Modules\Estoque\Models\ItemBom;
use App\Modules\Estoque\Models\ListaMateriais;
use App\Modules\Estoque\Policies\EstoquePolicy;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;

/** Listas de Materiais — BOM (regras só aqui). Sem tabela de reserva. */
class BomService
{
    public function __construct(private ItemService $itens) {}

    public function criar(array $dados, EstoquePrincipal $principal): ListaMateriais
    {
        EstoquePolicy::exigeEdicaoBom($principal);

        return DB::transaction(function () use ($dados) {
            $bom = ListaMateriais::create([
                'id_produto_servico' => (int) $dados['idProdutoServico'],
                'nome' => $dados['nome'],
                'versao' => isset($dados['versao']) ? (int) $dados['versao'] : 1,
                'editavel' => $dados['editavel'] ?? true,
            ]);

            foreach ($dados['itens'] as $item) {
                $bom->itens()->create([
                    'id_item' => $this->obterItem((int) $item['idItem'])->getKey(),
                    'quantidade_prevista' => number_format((float) $item['quantidadePrevista'], 2, '.', ''),
                ]);
            }

            return $bom->refresh()->load('itens');
        });
    }

    /** Sem `idProdutoServico` retorna todas (E-2). */
    public function listar(?int $idProdutoServico, EstoquePrincipal $principal): array
    {
        EstoquePolicy::exigeLeitura($principal);

        return ListaMateriais::query()
            ->with('itens')
            ->when($idProdutoServico !== null, fn ($q) => $q->where('id_produto_servico', $idProdutoServico))
            ->orderBy('id_bom')
            ->get()
            ->all();
    }

    public function buscar(int $id, EstoquePrincipal $principal): ListaMateriais
    {
        EstoquePolicy::exigeLeitura($principal);

        return $this->obter($id);
    }

    /** Cada atualizar incrementa `versao` (default `atual+1`) e reconcilia os itens. */
    public function atualizar(int $id, array $dados, EstoquePrincipal $principal): ListaMateriais
    {
        EstoquePolicy::exigeEdicaoBom($principal);

        return DB::transaction(function () use ($id, $dados) {
            $bom = $this->obter($id);
            $bom->nome = $dados['nome'];
            $bom->versao = isset($dados['versao']) ? (int) $dados['versao'] : ((int) $bom->versao + 1);
            $bom->editavel = $dados['editavel'] ?? $bom->editavel;
            $bom->save();

            $novos = [];
            foreach ($dados['itens'] as $item) {
                $idItem = (int) $item['idItem'];
                $existente = $bom->itens->firstWhere('id_item', $idItem);

                if ($existente !== null) {
                    $existente->quantidade_prevista = number_format((float) $item['quantidadePrevista'], 2, '.', '');
                    $existente->save();
                } else {
                    $this->obterItem($idItem);
                    $novos[] = new ItemBom([
                        'id_item' => $idItem,
                        'quantidade_prevista' => number_format((float) $item['quantidadePrevista'], 2, '.', ''),
                    ]);
                }
            }

            $desejados = array_map(fn ($i) => (int) $i['idItem'], $dados['itens']);
            $bom->itens()->whereNotIn('id_item', $desejados)->delete();
            $bom->itens()->saveMany($novos);

            return $bom->refresh()->load('itens');
        });
    }

    /**
     * Consumo real → preenche `quantidade_real` + baixa via ItemService com
     * `id_referencia` = id da BOM. Item estranho → 400 sem baixa parcial.
     */
    public function registrarConsumo(int $id, array $itens, EstoquePrincipal $principal): ListaMateriais
    {
        EstoquePolicy::exigeEdicaoBom($principal);

        return DB::transaction(function () use ($id, $itens) {
            $bom = $this->obter($id);
            $pertencem = $bom->itens->pluck('id_item')->map(fn ($v) => (int) $v)->all();

            foreach ($itens as $consumo) {
                if (! in_array((int) $consumo['idItem'], $pertencem, true)) {
                    throw new \InvalidArgumentException("Item {$consumo['idItem']} não pertence à BOM {$id}");
                }
            }

            foreach ($itens as $consumo) {
                $itemBom = $bom->itens->firstWhere('id_item', (int) $consumo['idItem']);
                $itemBom->quantidade_real = number_format((float) $consumo['quantidadeConsumida'], 2, '.', '');
                $itemBom->save();

                $this->itens->registrarConsumo(
                    (int) $consumo['idItem'],
                    (string) $consumo['quantidadeConsumida'],
                    $id,
                );
            }

            return $bom->refresh()->load('itens');
        });
    }

    private function obter(int $id): ListaMateriais
    {
        return ListaMateriais::with('itens')->find($id)
            ?? throw new ResourceNotFoundException("BOM não encontrada: {$id}");
    }

    private function obterItem(int $idItem): Item
    {
        return Item::find($idItem)
            ?? throw new ResourceNotFoundException("Item não encontrado: {$idItem}");
    }
}

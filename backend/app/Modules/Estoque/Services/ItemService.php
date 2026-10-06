<?php

namespace App\Modules\Estoque\Services;

use App\Modules\Estoque\Enums\Categoria;
use App\Modules\Estoque\Enums\TipoSaida;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Events\EstoqueBaixoEvent;
use App\Modules\Estoque\Models\Item;
use App\Modules\Estoque\Models\Localizacao;
use App\Modules\Estoque\Models\SaidaEstoque;
use App\Modules\Estoque\Policies\EstoquePolicy;
use App\Shared\Exceptions\ResourceNotFoundException;
use App\Shared\Exceptions\SaldoInsuficienteException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

/** Gestão de itens, consumo via BOM e import/export CSV (regras só aqui). */
class ItemService
{
    public function criar(array $dados, EstoquePrincipal $principal): Item
    {
        EstoquePolicy::exigeEdicaoMovimentacao($principal);

        $item = Item::create([
            'nome' => $dados['nome'],
            'descricao' => $dados['descricao'] ?? null,
            'categoria' => Categoria::from($dados['categoria']),
            'unidade_medida' => $dados['unidadeMedida'],
            'quantidade_atual' => $this->decimal($dados['quantidadeAtual']),
            'estoque_minimo' => $this->decimal($dados['estoqueMinimo']),
            'versao' => 0,
            'localizacao_id' => $this->resolverLocalizacao($dados['idLocalizacao'] ?? null)?->getKey(),
        ]);

        $this->verificarEstoqueBaixo($item->refresh());

        return $item;
    }

    /** @return list<Item> */
    public function listar(
        ?Categoria $categoria,
        ?int $idLocalizacao,
        ?bool $baixo,
        EstoquePrincipal $principal,
    ): array {
        EstoquePolicy::exigeLeitura($principal);

        return Item::query()
            ->with('localizacao')
            ->when($categoria !== null, fn ($q) => $q->where('categoria', $categoria))
            ->when($idLocalizacao !== null, fn ($q) => $q->where('localizacao_id', $idLocalizacao))
            ->when($baixo === true, fn ($q) => $q->whereColumn('quantidade_atual', '<=', 'estoque_minimo'))
            ->orderBy('id_item')
            ->get()
            ->all();
    }

    public function buscar(int $id, EstoquePrincipal $principal): Item
    {
        EstoquePolicy::exigeLeitura($principal);

        return $this->obter($id);
    }

    public function atualizar(int $id, array $dados, EstoquePrincipal $principal): Item
    {
        EstoquePolicy::exigeEdicaoMovimentacao($principal);

        $item = $this->obter($id);
        $item->fill([
            'nome' => $dados['nome'],
            'descricao' => $dados['descricao'] ?? null,
            'categoria' => Categoria::from($dados['categoria']),
            'unidade_medida' => $dados['unidadeMedida'],
            'quantidade_atual' => $this->decimal($dados['quantidadeAtual']),
            'estoque_minimo' => $this->decimal($dados['estoqueMinimo']),
            'localizacao_id' => $this->resolverLocalizacao($dados['idLocalizacao'] ?? null)?->getKey(),
        ]);
        $item->save();

        return $item->refresh();
    }

    /**
     * Baixa pelos itens consumidos (BOM/produção). Idempotente por
     * `idReferencia` (retry/duplo dispatch = 1 baixa).
     *
     * @param list<array{idItem:int,quantidadeConsumida:string|float|int}> $itens
     * @return list<SaidaEstoque>
     */
    public function baixarPorConsumo(array $itens, ?int $idReferencia): array
    {
        if ($idReferencia !== null && SaidaEstoque::where('tipo_saida', TipoSaida::CONSUMO)
            ->where('id_referencia', $idReferencia)->exists()) {
            Log::info("Baixa por consumo já registrada para a referência {$idReferencia}; ignorando duplicado");

            return [];
        }

        $saidas = [];
        foreach ($itens as $consumo) {
            $saidas[] = $this->registrarConsumo(
                (int) $consumo['idItem'],
                (string) $consumo['quantidadeConsumida'],
                $idReferencia,
            );
        }

        return $saidas;
    }

    /** Consumo efetivo de um item (saída CONSUMO, nunca negativa). */
    public function registrarConsumo(int $idItem, string|float|int $quantidade, ?int $idReferencia): SaidaEstoque
    {
        return DB::transaction(function () use ($idItem, $quantidade, $idReferencia) {
            $item = Item::whereKey($idItem)->lockForUpdate()->first()
                ?? throw new ResourceNotFoundException("Item não encontrado: {$idItem}");

            $this->subtrair($item, $this->decimal($quantidade));

            $saida = SaidaEstoque::create([
                'id_item' => $item->getKey(),
                'quantidade' => $this->decimal($quantidade),
                'tipo_saida' => TipoSaida::CONSUMO,
                'id_referencia' => $idReferencia,
                'data_saida' => now(),
                'observacao' => 'Consumo registrado via BOM',
            ]);

            $this->verificarEstoqueBaixo($item);

            return $saida;
        });
    }

    /**
     * CSV `;` com cabeçalho PT (download). Células sanitizadas anti-injection:
     * `;` vira `,`, prefixos de fórmula (`= + - @` + tab) ganham `'`.
     */
    public function exportarCsv(EstoquePrincipal $principal): string
    {
        EstoquePolicy::exigeLeitura($principal);

        $csv = "id_item;nome;descricao;categoria;unidade_medida;quantidade_atual;estoque_minimo;localizacao\n";

        foreach (Item::with('localizacao')->orderBy('id_item')->get() as $item) {
            $loc = $item->localizacao === null ? '' : implode('/', array_filter([
                $item->localizacao->armario,
                $item->localizacao->prateleira,
                $item->localizacao->caixa,
            ]));

            $csv .= implode(';', [
                $item->getKey(),
                $this->celula($item->nome),
                $this->celula($item->descricao),
                $item->categoria->value,
                $this->celula($item->unidade_medida),
                $item->quantidade_atual,
                $item->estoque_minimo,
                $this->celula($loc),
            ])."\n";
        }

        return $csv;
    }

    /**
     * Importa itens do CSV (`nome;descricao;categoria;unidade;atual;minimo[;idLocalizacao]`,
     * sem cabeçalho). Reaproveita as validações do criar.
     *
     * @return list<Item>
     */
    public function importarCsv(string $conteudo, EstoquePrincipal $principal): array
    {
        EstoquePolicy::exigeEdicaoMovimentacao($principal);

        $importados = [];
        foreach (preg_split('/\r\n|\n|\r/', $conteudo) ?: [] as $linha) {
            $linha = trim($linha);
            if ($linha === '') {
                continue;
            }

            $colunas = explode(';', $linha);
            if (count($colunas) < 6) {
                throw new \InvalidArgumentException(
                    'Linha inválida no CSV de importação (esperado: nome;descricao;categoria;unidade_medida;quantidade_atual;estoque_minimo): '.$linha,
                );
            }

            $importados[] = $this->criar([
                'nome' => trim($colunas[0]),
                'descricao' => trim($colunas[1]),
                'categoria' => mb_strtoupper(trim($colunas[2])),
                'unidadeMedida' => trim($colunas[3]),
                'quantidadeAtual' => str_replace(',', '.', trim($colunas[4])),
                'estoqueMinimo' => str_replace(',', '.', trim($colunas[5])),
                'idLocalizacao' => isset($colunas[6]) && trim($colunas[6]) !== ''
                    ? (int) trim($colunas[6]) : null,
            ], $principal);
        }

        return $importados;
    }

    /** Publica `estoque.baixo.event` quando `atual <= minimo` (fail-soft). */
    public function verificarEstoqueBaixo(Item $item): void
    {
        if ($this->centavos($item->quantidade_atual) > $this->centavos($item->estoque_minimo)) {
            return;
        }

        try {
            event(new EstoqueBaixoEvent(
                (int) $item->getKey(),
                (string) $item->nome,
                number_format((float) $item->quantidade_atual, 2, '.', ''),
                number_format((float) $item->estoque_minimo, 2, '.', ''),
            ));
        } catch (\Throwable $e) {
            Log::warning("Falha ao publicar estoque.baixo.event do item {$item->getKey()}: {$e->getMessage()}");
        }
    }

    public function obter(int $id): Item
    {
        return Item::with('localizacao')->find($id)
            ?? throw new ResourceNotFoundException("Item não encontrado: {$id}");
    }

    private function resolverLocalizacao(?int $idLocalizacao): ?Localizacao
    {
        if ($idLocalizacao === null) {
            return null;
        }

        return Localizacao::find($idLocalizacao)
            ?? throw new ResourceNotFoundException("Localização não encontrada: {$idLocalizacao}");
    }

    private function subtrair(Item $item, string $quantidade): void
    {
        $novoSaldo = $this->centavos($item->quantidade_atual) - $this->centavos($quantidade);

        if ($novoSaldo < 0) {
            throw new SaldoInsuficienteException(
                "Estoque insuficiente para o item {$item->nome}: saldo atual {$item->quantidade_atual}, necessário {$quantidade}",
            );
        }

        $item->quantidade_atual = $this->fmt($novoSaldo);
        $item->save();
    }

    private function decimal(string|float|int $valor): string
    {
        return $this->fmt($this->centavos($valor));
    }

    /** Centavos inteiros (aritmética exata sem bcmath, ausente na imagem). */
    private function centavos(string|float|int $valor): int
    {
        return (int) round((float) $valor * 100);
    }

    private function fmt(int $centavos): string
    {
        return number_format($centavos / 100, 2, '.', '');
    }

    private function celula(?string $valor): string
    {
        if ($valor === null) {
            return '';
        }

        $v = str_replace(';', ',', $valor);

        if ($v !== '' && str_contains("=+-@\t", $v[0])) {
            $v = "'".$v;
        }

        if (strpbrk($v, ",\"\n\r") !== false) {
            $v = '"'.str_replace('"', '""', $v).'"';
        }

        return $v;
    }
}

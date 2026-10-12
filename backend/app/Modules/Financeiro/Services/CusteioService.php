<?php

namespace App\Modules\Financeiro\Services;

use App\Modules\Estoque\Contracts\EstoqueContract;
use App\Modules\Financeiro\Enums\StatusLancamento;
use App\Modules\Financeiro\Enums\TipoCategoria;
use App\Modules\Financeiro\Enums\TipoLancamento;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Models\CategoriaFinanceira;
use App\Modules\Financeiro\Models\CustoEncomenda;
use App\Modules\Financeiro\Models\FechamentoEncomenda;
use App\Modules\Financeiro\Models\HorasEncomenda;
use App\Modules\Financeiro\Models\LancamentoFinanceiro;
use App\Modules\Financeiro\Policies\FinanceiroPolicy;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;

/**
 * Custeio por ordem (Job Order Costing): materiais (Σ SAIDA não canceladas
 * por referência = id da encomenda) + mão de obra (Σ horas × valor/hora
 * vigente, default nível 2) + overhead (taxa vigente × total de horas).
 * Aritmética em centavos (int); `margem = valor_fechado − custo_total`.
 */
class CusteioService
{
    /** Nível default para horas sem nível informado (premissa de MVP). */
    public const NIVEL_DEFAULT = 2;

    /** Categoria DESPESA usada nos lançamentos automáticos de consumo de material. */
    public const CATEGORIA_MATERIAIS = 'Materiais de produção';

    public function __construct(
        private ParametroService $parametros,
        private FinanceiroEventPublisher $publisher,
        private EstoqueContract $estoque,
    ) {}

    public function calcular(int $idEncomenda, FinanceiroPrincipal $principal): CustoEncomenda
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return $this->executar($idEncomenda);
    }

    /** Variante para o listener de produção concluída (sem principal: sistema). */
    public function calcularDoEvento(int $idEncomenda): CustoEncomenda
    {
        return $this->executar($idEncomenda);
    }

    /**
     * Registra o lançamento de SAIDA do material consumido (BOM) da encomenda,
     * para que `executar()` o compute em `custo_materiais`. **Idempotente** por
     * `id_referencia_externa` (retry/evento duplicado não duplica o lançamento);
     * cria a categoria DESPESA sob demanda. Custo 0 (sem preço de compra) → nada grava.
     *
     * @param  list<array{idItem:int,quantidadeConsumida:string|float|int}>  $itens
     */
    public function registrarMateriais(int $idEncomenda, array $itens): void
    {
        if ($itens === []) {
            return;
        }

        $referencia = (string) $idEncomenda;

        $jaRegistrado = LancamentoFinanceiro::where('id_referencia_externa', $referencia)
            ->where('tipo', TipoLancamento::SAIDA)
            ->where('status', '!=', StatusLancamento::CANCELADO->value)
            ->exists();

        if ($jaRegistrado) {
            return;
        }

        $total = $this->estoque->custoConsumo($itens);

        if ((float) $total <= 0) {
            return;
        }

        $categoria = CategoriaFinanceira::firstOrCreate(
            ['nome' => self::CATEGORIA_MATERIAIS],
            ['tipo' => TipoCategoria::DESPESA, 'descricao' => 'Custo de material consumido na produção (BOM)'],
        );

        LancamentoFinanceiro::create([
            'id_categoria' => $categoria->getKey(),
            'tipo' => TipoLancamento::SAIDA,
            'valor' => $total,
            'data_vencimento' => today()->toDateString(),
            'status' => StatusLancamento::PENDENTE,
            'id_referencia_externa' => $referencia,
            'observacao' => "Consumo de materiais (BOM) — encomenda {$idEncomenda}",
        ]);
    }

    private function executar(int $idEncomenda): CustoEncomenda
    {
        return DB::transaction(function () use ($idEncomenda) {
            $fechamento = FechamentoEncomenda::where('id_encomenda', $idEncomenda)->first()
                ?? throw new \InvalidArgumentException(
                    "Fechamento da encomenda {$idEncomenda} não encontrado. Feche valor e horas antes do custeio.",
                );

            $materiais = 0;
            foreach ($this->saidasPorReferencia((string) $idEncomenda) as $valor) {
                $materiais += (int) round((float) $valor * 100);
            }

            $maoObra = 0;
            $minutos = 0;
            foreach (HorasEncomenda::where('id_encomenda', $idEncomenda)->get() as $linha) {
                $nivel = $linha->nivel_acesso ?? self::NIVEL_DEFAULT;
                $valorHora = $this->parametros->obterValorHoraVigente((int) $nivel);
                $maoObra += (int) round((float) $linha->horas * (float) $valorHora * 100);
                $minutos += (int) round((float) $linha->horas * 100);
            }

            $taxa = $this->parametros->obterTaxaVigente();
            $overhead = (int) round($minutos / 100 * (float) $taxa * 100);

            $total = $materiais + $maoObra + $overhead;
            $fechado = (int) round((float) $fechamento->valor_fechado * 100);
            $margem = $fechado - $total;

            $fmt = fn (int $c) => number_format($c / 100, 2, '.', '');

            $custo = CustoEncomenda::create([
                'id_encomenda' => $idEncomenda,
                'custo_materiais' => $fmt($materiais),
                'custo_mao_obra' => $fmt($maoObra),
                'custo_overhead' => $fmt($overhead),
                'custo_total' => $fmt($total),
                'valor_venda' => $fmt($fechado),
                'margem_lucro' => $fmt($margem),
                'data_calculo' => today()->toDateString(),
            ]);

            $this->publisher->custoCalculado(
                $idEncomenda, $fmt($total), $fmt($margem), today()->toDateString(),
            );

            return $custo;
        });
    }

    /** Último cálculo da encomenda (congelado). */
    public function consultar(int $idEncomenda, FinanceiroPrincipal $principal): CustoEncomenda
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return CustoEncomenda::where('id_encomenda', $idEncomenda)
            ->orderByDesc('data_calculo')->orderByDesc('id_custo')->first()
            ?? throw new ResourceNotFoundException("Custo da encomenda {$idEncomenda} não encontrado");
    }

    /** @return list<string> valores das SAIDA não canceladas com a referência exata */
    private function saidasPorReferencia(string $referencia): array
    {
        return LancamentoFinanceiro::query()
            ->where('tipo', TipoLancamento::SAIDA)
            ->where('status', '!=', StatusLancamento::CANCELADO->value)
            ->where('id_referencia_externa', $referencia)
            ->pluck('valor')->all();
    }
}

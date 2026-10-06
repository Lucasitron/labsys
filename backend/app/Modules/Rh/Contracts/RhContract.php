<?php

namespace App\Modules\Rh\Contracts;

/**
 * Fronteira pública do RH p/ Financeiro (coerência/horas) e Produção
 * (funcionários/responsáveis) — in-process, sem HTTP. Só o consumido.
 */
interface RhContract
{
    public function funcionarioExiste(int $idFuncionario): bool;

    /** Horas VALIDADAS do funcionário no período (Y-m-d), por tipo. Decimal `string`. */
    public function horasValidadasNoPeriodo(int $idFuncionario, string $tipo, string $inicio, string $fim): string;

    /** Dias em que encomenda+projeto excedeu a presença. */
    public function incoerencias(int $idFuncionario): array;

    /**
     * Nome completo da pessoa (E-4/Estoque, rótulo `tomador` fail-soft).
     * Null quando ausente — o chamador usa `Pessoa #id`.
     */
    public function nomePessoa(int $idPessoa): ?string;
}

<?php

namespace App\Modules\Financeiro\Services;

use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Models\ParametroOverhead;
use App\Modules\Financeiro\Models\ValorHoraNivel;
use App\Modules\Financeiro\Policies\FinanceiroPolicy;
use App\Shared\Exceptions\ConfiguracaoInvalidaException;
use Illuminate\Support\Facades\Log;

/**
 * Parâmetros de custeio: valor/hora por nível (0-3) e taxa de overhead, ambos
 * com vigência e auditoria em log (usuário/data — D-5). Append-only: definir
 * cria nova linha, nunca edita.
 */
class ParametroService
{
    public function definirValorHora(array $dados, FinanceiroPrincipal $principal): ValorHoraNivel
    {
        FinanceiroPolicy::exigeAdmin($principal);

        $valor = ValorHoraNivel::create([
            'nivel_acesso' => (int) $dados['nivelAcesso'],
            'valor_hora' => number_format((float) $dados['valorHora'], 2, '.', ''),
            'data_vigencia' => $dados['dataVigencia'],
        ]);

        Log::info("Auditoria: usuário {$principal->idPessoa} definiu valor/hora do nível {$dados['nivelAcesso']} = {$dados['valorHora']} (vigência {$dados['dataVigencia']})");

        return $valor;
    }

    /** @return list<ValorHoraNivel> vigentes: o mais recente de cada nível (0-3) */
    public function valoresVigentes(FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);

        $vigentes = [];
        for ($nivel = 0; $nivel <= 3; $nivel++) {
            $valor = ValorHoraNivel::where('nivel_acesso', $nivel)->orderByDesc('data_vigencia')->first();
            if ($valor !== null) {
                $vigentes[] = $valor;
            }
        }

        return $vigentes;
    }

    public function obterValorHoraVigente(int $nivel): string
    {
        $valor = ValorHoraNivel::where('nivel_acesso', $nivel)->orderByDesc('data_vigencia')->first();

        if ($valor === null) {
            throw new ConfiguracaoInvalidaException(
                "Valor/hora do nível {$nivel} não configurado. Defina em /valores-hora antes do custeio.",
            );
        }

        return (string) $valor->valor_hora;
    }

    public function definirOverhead(array $dados, FinanceiroPrincipal $principal): ParametroOverhead
    {
        FinanceiroPolicy::exigeAdmin($principal);

        $overhead = ParametroOverhead::create([
            'valor_taxa_hora' => number_format((float) $dados['valorTaxaHora'], 4, '.', ''),
            'data_vigencia' => $dados['dataVigencia'],
        ]);

        Log::info("Auditoria: usuário {$principal->idPessoa} definiu taxa de overhead = {$dados['valorTaxaHora']} (vigência {$dados['dataVigencia']})");

        return $overhead;
    }

    public function overheadVigente(FinanceiroPrincipal $principal): ParametroOverhead
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return ParametroOverhead::orderByDesc('data_vigencia')->first()
            ?? throw new ConfiguracaoInvalidaException(
                'Taxa de overhead não configurada. Defina em /parametros-overhead antes do custeio.',
            );
    }

    public function obterTaxaVigente(): string
    {
        $overhead = ParametroOverhead::orderByDesc('data_vigencia')->first();

        if ($overhead === null) {
            throw new ConfiguracaoInvalidaException(
                'Taxa de overhead não configurada. Defina em /parametros-overhead antes do custeio.',
            );
        }

        return (string) $overhead->valor_taxa_hora;
    }
}

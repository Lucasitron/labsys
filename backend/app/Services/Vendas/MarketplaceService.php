<?php

namespace App\Services\Vendas;

use App\Models\Vendas\Encomenda;
use App\Models\Vendas\RegistroMarketplace;
use App\Policies\VendasPolicy;
use App\Shared\Auth\Caller;
use App\Exceptions\ConflitoException;
use App\Exceptions\ResourceNotFoundException;

/** Registro manual de vendas externas (regras só aqui). Escrita = Admin. */
class MarketplaceService
{
    public function registrar(array $dados, Caller $principal): RegistroMarketplace
    {
        VendasPolicy::exigeEscrita($principal);

        Encomenda::find($dados['encomendaId'])
            ?? throw new ResourceNotFoundException("Encomenda não encontrada: {$dados['encomendaId']}");

        $plataforma = trim((string) $dados['plataforma']);
        $codigo = trim((string) $dados['codigoExterno']);

        if (RegistroMarketplace::where('plataforma', $plataforma)->where('codigo_externo', $codigo)->exists()) {
            throw new ConflitoException('Pedido já registrado para esta plataforma');
        }

        return RegistroMarketplace::create([
            'id_encomenda' => $dados['encomendaId'],
            'plataforma' => $plataforma,
            'codigo_externo' => $codigo,
            'data_venda' => $dados['dataVenda'],
            'valor_taxa' => number_format((float) $dados['valorTaxa'], 2, '.', ''),
        ]);
    }

    /** @return array{registros:list<RegistroMarketplace>,totais:array<string,string>} */
    public function listar(?int $encomendaId, ?string $plataforma, Caller $principal): array
    {
        VendasPolicy::exigeLeitura($principal);

        $plataforma = $plataforma !== null && trim($plataforma) !== '' ? trim($plataforma) : null;

        $registros = RegistroMarketplace::query()->with('encomenda')
            ->when($encomendaId !== null, fn ($q) => $q->where('id_encomenda', $encomendaId))
            ->when($plataforma !== null, fn ($q) => $q->where('plataforma', 'ilike', $plataforma))
            ->orderByDesc('id_registro')
            ->get();

        $bruto = 0.0;
        $taxas = 0.0;
        foreach ($registros as $registro) {
            $bruto += (float) ($registro->encomenda?->valor_final ?? 0);
            $taxas += (float) $registro->valor_taxa;
        }

        return [
            'registros' => $registros->all(),
            'totais' => [
                'bruto' => number_format($bruto, 2, '.', ''),
                'taxas' => number_format($taxas, 2, '.', ''),
                'liquido' => $this->liquido(
                    number_format($bruto, 2, '.', ''),
                    number_format($taxas, 2, '.', ''),
                ),
            ],
        ];
    }

    /** Líquido = valor final da encomenda − taxa (em R$). */
    public function liquido(string $valorBruto, string $valorTaxa): string
    {
        return number_format((float) $valorBruto - (float) $valorTaxa, 2, '.', '');
    }
}

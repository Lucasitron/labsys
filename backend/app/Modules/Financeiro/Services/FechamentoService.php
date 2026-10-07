<?php

namespace App\Modules\Financeiro\Services;

use App\Modules\Financeiro\Enums\StatusFechamento;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Models\FechamentoEncomenda;
use App\Modules\Financeiro\Models\HorasEncomenda;
use App\Modules\Financeiro\Policies\FinanceiroPolicy;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

/**
 * Fechamento de encomenda (congelamento — D-5): abertura manual ou automática
 * idempotente via `encomenda.criada.event`, consulta e acúmulo de horas
 * validadas do RH. Imutável após criar (sem PUT; `cancelar` não existe —
 * método Java sem rota). `id_encomenda` opaco, sem checagem via VendasContract.
 */
class FechamentoService
{
    public function criar(array $dados, FinanceiroPrincipal $principal): FechamentoEncomenda
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return DB::transaction(function () use ($dados, $principal) {
            if (FechamentoEncomenda::where('id_encomenda', $dados['idEncomenda'])->exists()) {
                throw new ConflitoException('Encomenda já possui fechamento. Alterações geram nova ordem.');
            }

            $fechamento = FechamentoEncomenda::create([
                'id_encomenda' => $dados['idEncomenda'],
                'horas_estimadas' => number_format((float) $dados['horasEstimadas'], 2, '.', ''),
                'valor_fechado' => number_format((float) $dados['valorFechado'], 2, '.', ''),
                'data_fechamento' => $dados['dataFechamento'] ?? today()->toDateString(),
                'status' => StatusFechamento::ABERTA,
                'horas_validadas' => '0.00',
            ]);

            Log::info("Auditoria: usuário {$principal->idPessoa} criou fechamento da encomenda {$dados['idEncomenda']} (valor {$dados['valorFechado']}, horas estimadas {$dados['horasEstimadas']})");

            return $fechamento;
        });
    }

    /** Abertura automática idempotente a partir de `encomenda.criada.event`. */
    public function criarDoEvento(int $idEncomenda, ?string $valorFechado, ?string $dataCriacao): FechamentoEncomenda
    {
        return FechamentoEncomenda::where('id_encomenda', $idEncomenda)->first()
            ?? FechamentoEncomenda::create([
                'id_encomenda' => $idEncomenda,
                'horas_estimadas' => '0.00',
                'valor_fechado' => $valorFechado ?? '0.00',
                'data_fechamento' => $dataCriacao ?? today()->toDateString(),
                'status' => StatusFechamento::ABERTA,
                'horas_validadas' => '0.00',
            ]);
    }

    public function consultar(int $idEncomenda, FinanceiroPrincipal $principal): FechamentoEncomenda
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return $this->obter($idEncomenda);
    }

    /** @return list<FechamentoEncomenda> */
    public function listar(FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return FechamentoEncomenda::orderByDesc('id_fechamento')->get()->all();
    }

    /**
     * Acumula horas validadas: upsert em `horas_encomenda` por (encomenda,
     * funcionário, data) + incremento de `horas_validadas`. Horas sem nível
     * seguem para o custeio com default nível 2 (Voluntário).
     */
    public function registrarHorasValidadas(
        int $idEncomenda,
        int $idFuncionario,
        ?int $nivel,
        string $horas,
        string $data,
    ): void {
        DB::transaction(function () use ($idEncomenda, $idFuncionario, $nivel, $horas, $data): void {
            $fechamento = $this->obter($idEncomenda);

            $linha = HorasEncomenda::where('id_encomenda', $idEncomenda)
                ->where('id_funcionario', $idFuncionario)
                ->where('data_registro', $data)
                ->first();

            if ($linha === null) {
                $linha = new HorasEncomenda([
                    'id_encomenda' => $idEncomenda,
                    'id_funcionario' => $idFuncionario,
                    'nivel_acesso' => $nivel,
                    'horas' => '0.00',
                    'data_registro' => $data,
                ]);
            }
            $linha->horas = number_format((float) $linha->horas + (float) $horas, 2, '.', '');
            if ($nivel !== null) {
                $linha->nivel_acesso = $nivel;
            }
            $linha->save();

            $soma = (int) round((float) $fechamento->horas_validadas * 100) + (int) round((float) $horas * 100);
            $fechamento->horas_validadas = number_format($soma / 100, 2, '.', '');
            $fechamento->save();
        });
    }

    /**
     * Alteração de encomenda (D-5): encerra a ordem atual (custo congelado) e
     * abre nova ordem com novas estimativas e valor — nunca edita o congelado.
     */
    public function novaOrdem(int $idEncomenda, array $dados, FinanceiroPrincipal $principal): FechamentoEncomenda
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return DB::transaction(function () use ($idEncomenda, $dados, $principal) {
            $atual = $this->obter($idEncomenda);
            $atual->status = StatusFechamento::CONCLUIDA;
            $atual->save();

            Log::info("Auditoria: usuário {$principal->idPessoa} encerrou a ordem da encomenda {$idEncomenda} e abriu nova ordem");

            if (FechamentoEncomenda::where('id_encomenda', $dados['idEncomenda'])->exists()) {
                throw new ConflitoException('Encomenda já possui fechamento. Alterações geram nova ordem.');
            }

            return FechamentoEncomenda::create([
                'id_encomenda' => $dados['idEncomenda'],
                'horas_estimadas' => number_format((float) $dados['horasEstimadas'], 2, '.', ''),
                'valor_fechado' => number_format((float) $dados['valorFechado'], 2, '.', ''),
                'data_fechamento' => $dados['dataFechamento'] ?? today()->toDateString(),
                'status' => StatusFechamento::ABERTA,
                'horas_validadas' => '0.00',
            ]);
        });
    }

    public function obter(int $idEncomenda): FechamentoEncomenda
    {
        return FechamentoEncomenda::where('id_encomenda', $idEncomenda)->first()
            ?? throw new ResourceNotFoundException("Fechamento da encomenda não encontrado: {$idEncomenda}");
    }
}

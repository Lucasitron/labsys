<?php

namespace App\Modules\Producao\Services;

use App\Modules\Producao\Enums\TipoAdvertencia;
use App\Modules\Producao\Models\AdvertenciaMembro;
use App\Modules\Producao\Models\Inspecao5S;
use App\Modules\Producao\Models\Parametro5S;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;

/**
 * Advertências e penalidades (F7/F10): contador sequencial por membro; a
 * partir da 3ª, `advertencia.limite.atingido.event`. Com
 * `periodoExperimentalAtivo=true`, a auto-advertência é registrada mas não
 * conta p/ o limite (sem evento crítico).
 */
class AdvertenciaService
{
    public const LIMITE_PENALIDADE = 3;

    public function __construct(private ProducaoEventPublisher $eventos) {}

    /** @return list<AdvertenciaMembro> não-Admin vê só as próprias (server-side). */
    public function listar(?int $idFuncionario, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        $alvo = $principal->isAdmin() ? $idFuncionario : $principal->idPessoa;

        return AdvertenciaMembro::query()
            ->when($alvo !== null, fn ($q) => $q->where('id_funcionario', $alvo))
            ->orderByDesc('data')
            ->get()
            ->all();
    }

    public function registrar(array $dados, ProducaoPrincipal $principal): AdvertenciaMembro
    {
        ProducaoPolicy::exigeAdmin($principal);

        return DB::transaction(function () use ($dados, $principal) {
            $inspecao = ($dados['idInspecao'] ?? null) === null
                ? null
                : Inspecao5S::find($dados['idInspecao'])
                    ?? throw new ResourceNotFoundException("Inspeção 5S não encontrada: {$dados['idInspecao']}");

            return $this->registrarInterno(
                (int) $dados['idFuncionario'],
                $inspecao,
                $dados['data'] ?? today()->toDateString(),
                $dados['motivo'],
                TipoAdvertencia::from($dados['tipo']),
                $principal->idPessoa,
                false,
            );
        });
    }

    /** Fluxo da inspeção 5S em não conformidade (VERBAL ao responsável ativo). */
    public function registrarAutomatica(int $idFuncionario, Inspecao5S $inspecao, string $motivo): AdvertenciaMembro
    {
        return DB::transaction(fn () => $this->registrarInterno(
            $idFuncionario, $inspecao, today()->toDateString(), $motivo, TipoAdvertencia::VERBAL, null, true,
        ));
    }

    private function registrarInterno(
        int $idFuncionario,
        ?Inspecao5S $inspecao,
        string $data,
        string $motivo,
        TipoAdvertencia $tipo,
        ?int $idAdminRegistrou,
        bool $automatica,
    ): AdvertenciaMembro {
        $advertencia = AdvertenciaMembro::create([
            'id_funcionario' => $idFuncionario,
            'id_inspecao' => $inspecao?->getKey(),
            'data' => $data,
            'motivo' => $motivo,
            'tipo' => $tipo->value,
            'contador' => AdvertenciaMembro::where('id_funcionario', $idFuncionario)->count() + 1,
            'id_admin_registrou' => $idAdminRegistrou,
        ]);

        $this->eventos->advertencia($idFuncionario, (int) $advertencia->contador, $motivo);

        $experimental = $automatica && Parametro5S::experimentalAtivo();

        if ((int) $advertencia->contador >= self::LIMITE_PENALIDADE && ! $experimental) {
            $this->eventos->advertenciaLimite($idFuncionario, (int) $advertencia->contador, $motivo);
        }

        return $advertencia->refresh();
    }
}

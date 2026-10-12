<?php

namespace App\Modules\Producao\Services;

use App\Modules\Producao\Enums\StatusProjetoMesa;
use App\Modules\Producao\Models\AuditoriaProjetoMesa;
use App\Modules\Producao\Models\ProjetoMesa;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ResourceNotFoundException;
use Carbon\CarbonInterface;
use Illuminate\Support\Facades\DB;

/**
 * Projetos de mesa, QR e auditorias (F8/F12). Auditoria espelha o `status` e
 * só publica `projeto.mesa.abandonado.event` quando `ABANDONADO`.
 */
class ProjetoMesaService
{
    public function __construct(private ProducaoEventPublisher $eventos) {}

    /** @return list<ProjetoMesa> */
    public function listar(?StatusProjetoMesa $status, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return ProjetoMesa::query()
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->orderByDesc('id_projeto_mesa')
            ->get()
            ->all();
    }

    public function detalhar(int $id, ProducaoPrincipal $principal): ProjetoMesa
    {
        ProducaoPolicy::exigeLeitura($principal);

        return $this->obter($id);
    }

    public function criar(array $dados, ProducaoPrincipal $principal): ProjetoMesa
    {
        ProducaoPolicy::exigeResponsavel((int) $dados['idFuncionario'], $principal);

        return DB::transaction(function () use ($dados) {
            $hoje = today()->toDateString();

            $projeto = ProjetoMesa::create([
                'id_funcionario' => $dados['idFuncionario'],
                'id_mesa' => $dados['idMesa'],
                'nome_projeto' => $dados['nomeProjeto'],
                'tipo_projeto' => $dados['tipoProjeto'] ?? null,
                'prazo_execucao' => $dados['prazoExecucao'] ?? null,
                'data_inicio' => $hoje,
                'data_ultima_evolucao' => $hoje,
                'status' => StatusProjetoMesa::ATIVO->value,
            ]);

            $projeto->qr_code_totem = "fablab://projeto-mesa/{$projeto->getKey()}";
            $projeto->save();

            return $projeto->refresh();
        });
    }

    public function atualizar(int $id, array $dados, ProducaoPrincipal $principal): ProjetoMesa
    {
        ProducaoPolicy::exigeLeitura($principal);
        $projeto = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $projeto->id_funcionario === null ? null : (int) $projeto->id_funcionario, $principal,
        );

        return DB::transaction(function () use ($projeto, $dados) {
            $projeto->update([
                'id_funcionario' => $dados['idFuncionario'],
                'id_mesa' => $dados['idMesa'],
                'nome_projeto' => $dados['nomeProjeto'],
                'tipo_projeto' => $dados['tipoProjeto'] ?? null,
                'prazo_execucao' => $dados['prazoExecucao'] ?? null,
            ]);

            return $projeto->refresh();
        });
    }

    /** Evolução reativa `ABANDONADO → ATIVO`. */
    public function registrarEvolucao(int $id, ProducaoPrincipal $principal): ProjetoMesa
    {
        ProducaoPolicy::exigeLeitura($principal);
        $projeto = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $projeto->id_funcionario === null ? null : (int) $projeto->id_funcionario, $principal,
        );

        return DB::transaction(function () use ($projeto) {
            $projeto->data_ultima_evolucao = today()->toDateString();
            if ($projeto->status === StatusProjetoMesa::ABANDONADO) {
                $projeto->status = StatusProjetoMesa::ATIVO;
            }
            $projeto->save();

            return $projeto->refresh();
        });
    }

    public function remover(int $id, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeLeitura($principal);
        $projeto = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $projeto->id_funcionario === null ? null : (int) $projeto->id_funcionario, $principal,
        );

        $projeto->delete();
    }

    /** Conteúdo do QR do totem (o PNG é inviável sem GD — serve-se a string, P9). */
    public function conteudoQrCode(int $id, ProducaoPrincipal $principal): string
    {
        ProducaoPolicy::exigeLeitura($principal);

        return (string) $this->obter($id)->qr_code_totem;
    }

    public function auditar(array $dados, ProducaoPrincipal $principal): AuditoriaProjetoMesa
    {
        ProducaoPolicy::exigeAdmin($principal);

        return DB::transaction(function () use ($dados, $principal) {
            $projeto = $this->obter((int) $dados['idProjetoMesa']);
            $resultado = StatusProjetoMesa::from($dados['resultado']);

            $auditoria = AuditoriaProjetoMesa::create([
                'id_projeto_mesa' => $projeto->getKey(),
                'data_auditoria' => today()->toDateString(),
                'resultado' => $resultado->value,
                'acao_tomada' => $dados['acaoTomada'] ?? null,
                'id_admin_responsavel' => $principal->idPessoa,
            ]);

            $projeto->status = $resultado;
            $projeto->data_ultima_evolucao = today()->toDateString();
            $projeto->save();

            if ($resultado === StatusProjetoMesa::ABANDONADO) {
                $this->eventos->mesaAbandonada(
                    (int) $projeto->getKey(),
                    (int) $projeto->id_funcionario,
                    $dados['acaoTomada'] ?? null,
                );
            }

            return $auditoria->refresh();
        });
    }

    /** @return list<AuditoriaProjetoMesa> */
    public function listarAuditorias(int $idProjetoMesa, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);
        $this->obter($idProjetoMesa);

        return AuditoriaProjetoMesa::where('id_projeto_mesa', $idProjetoMesa)
            ->orderByDesc('data_auditoria')
            ->get()
            ->all();
    }

    /** Ativos sem evolução desde a data (leitura p/ o scheduler P15). */
    public function projetosSemEvolucao(CarbonInterface $desde): array
    {
        return ProjetoMesa::where('status', StatusProjetoMesa::ATIVO)
            ->where('data_ultima_evolucao', '<', $desde->toDateString())
            ->orderBy('id_projeto_mesa')
            ->get()
            ->all();
    }

    public function obter(int $id): ProjetoMesa
    {
        return ProjetoMesa::find($id)
            ?? throw new ResourceNotFoundException("Projeto de mesa não encontrado: {$id}");
    }
}
